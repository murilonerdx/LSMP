package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bloco de matéria escura é radioativo. Quando jogador tem o bloco no
 * inventário (qualquer slot, exceto se estiver carregando uma
 * {@link br.com.murilo.liberthia.registry.ModItems#CONTAINMENT_GLOVE}),
 * sofre 1 dano de cada {@link #DAMAGE_PERIOD} ticks. A luva consome
 * durabilidade enquanto suprime o dano.
 *
 * <p>Mensagem de aviso aparece a cada {@link #WARNING_PERIOD} ticks na
 * primeira exposição.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class DarkMatterRadiationHandler {

    private static final Logger LOG = LoggerFactory.getLogger("Liberthia/DarkMatterRadiation");

    public static final int DAMAGE_PERIOD = 60;        // 3s
    public static final int WARNING_PERIOD = 200;      // 10s
    public static final float DAMAGE_PER_TICK = 1.0f;

    private DarkMatterRadiationHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        // Só processa de tempos em tempos
        if (player.tickCount % DAMAGE_PERIOD != 0) return;

        int dmCount = countDarkMatter(player);
        // Bonus pra item segurado na mão principal — impregnação direta
        boolean inHand = player.getMainHandItem().is(
                br.com.murilo.liberthia.registry.ModBlocks.DARK_MATTER_BLOCK.get().asItem());
        float handBonus = inHand ? 1.5f : 0f;

        if (dmCount <= 0 && handBonus == 0f) return;

        // v0.1.30: Refined Containment artifacts SUPRIMEM totalmente o dano
        // E o ganho de DM via radiação. Cheked PRIMEIRO porque tem prioridade
        // sobre a glove crua. Drena durab proporcional à exposição.
        if (br.com.murilo.liberthia.compat.CuriosCompat.isRefinedPendantActive(player)) {
            br.com.murilo.liberthia.compat.CuriosCompat.damageRefinedPendant(player, 1);
            return;
        }
        if (br.com.murilo.liberthia.compat.CuriosCompat.isRefinedGloveActive(player)
                && handBonus > 0) {
            // Glove refinada cobre o caso "segurando bloco de DM na mão" (handBonus)
            br.com.murilo.liberthia.compat.CuriosCompat.damageRefinedGlove(player, 1);
            return;
        }

        // Tem luva crua? Suprime TODO o efeito (dano + impregnação dimensional)
        // e consome durabilidade. Comportamento anterior preservado pra quem
        // não tem os refined artifacts.
        ItemStack glove = findGlove(player);
        if (!glove.isEmpty()) {
            // Consome durabilidade enquanto absorve a radiação (lento)
            if (player.level().random.nextInt(2) == 0) {
                glove.hurtAndBreak(1, player, p -> {
                    p.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                });
            }
            return;
        }

        // Sem luva — acumula matéria escura no perfil do jogador.
        // v0.1.51: skip se player tomou Dark Matter Pill (resistente).
        final int finalDmCount = dmCount;
        if (!br.com.murilo.liberthia.matter.MatterResistance.blocked(
                player, br.com.murilo.liberthia.matter.MatterResistance.Type.DARK)) {
            player.getCapability(br.com.murilo.liberthia.matter.MatterProfileProvider.CAP).ifPresent(profile -> {
                profile.addDark(0.5f + finalDmCount * 0.1f + handBonus);
                br.com.murilo.liberthia.matter.MatterProfileEvents.syncTo(player);
            });
        }

        if (dmCount <= 0) return;  // se só tinha em mão, sai aqui (sem dano padrão)

        // v0.1.51: pílula também bloqueia o dano de radiação
        if (br.com.murilo.liberthia.matter.MatterResistance.blocked(
                player, br.com.murilo.liberthia.matter.MatterResistance.Type.DARK)) {
            return;
        }

        // Sem luva → toma dano. Escala com quantidade.
        float dmg = DAMAGE_PER_TICK + Math.min(dmCount * 0.25f, 4f);
        player.hurt(player.damageSources().magic(), dmg);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.4f, 0.3f);

        // Aviso ocasional
        if (player.tickCount % WARNING_PERIOD == 0) {
            player.displayClientMessage(
                    Component.literal("⚠ Matéria escura no inventário — radiação tóxica! Use a Luva de Contenção.")
                            .withStyle(ChatFormatting.RED), true);
        }
    }

    /**
     * Conta cubos de matéria escura no inventário (incluindo offhand) MAIS
     * items com tag NBT {@code MatterInfected} (produzidos pelo Dark Matter
     * Alchemizer). Cada item infectado conta como +1 source — empilha com
     * outros blocos DM pro dano por radiação escalonar igual.
     */
    private static int countDarkMatter(Player player) {
        int dmBlocks = 0;
        int infected = 0;
        var dmItem = ModBlocks.DARK_MATTER_BLOCK.get().asItem();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(dmItem)) dmBlocks += s.getCount();
            // Items infectados (tag MatterInfected) também irradiam — cada um
            // conta como 1 source. Não usa getCount() porque rare loot é stack
            // size 1 sempre e é mais coerente: 1 item = 1 fonte.
            else if (s.getTag() != null && s.getTag().getBoolean("MatterInfected")) {
                infected += 1;
            }
        }
        int total = dmBlocks + infected;
        if (total > 0) {
            LOG.info("countDarkMatter: dm blocks={}, infected items={}, total={}",
                    dmBlocks, infected, total);
        }
        return total;
    }

    /**
     * Procura uma Containment Glove em qualquer slot do inventário OU equipada
     * num slot Curios (hands, bracelet, ring, charm — se o mod Curios estiver
     * instalado). Curios é checada primeiro pois é o "slot oficial" da glove.
     */
    private static ItemStack findGlove(Player player) {
        // 1) Curios slot (se mod presente)
        ItemStack curioGlove = br.com.murilo.liberthia.compat.CuriosCompat.findEquippedGlove(player);
        if (!curioGlove.isEmpty()) return curioGlove;

        // 2) Inventário comum (fallback se Curios não está instalado)
        var gloveItem = ModItems.CONTAINMENT_GLOVE.get();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(gloveItem)) return s;
        }
        return ItemStack.EMPTY;
    }
}
