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

        // Acumula matéria escura no perfil do jogador (mesmo com luva — só
        // o DANO é suprimido pela luva, não a impregnação dimensional).
        final int finalDmCount = dmCount;
        player.getCapability(br.com.murilo.liberthia.matter.MatterProfileProvider.CAP).ifPresent(profile -> {
            profile.addDark(0.5f + finalDmCount * 0.1f + handBonus);
            br.com.murilo.liberthia.matter.MatterProfileEvents.syncTo(player);
        });

        if (dmCount <= 0) return;  // se só tinha em mão, sai aqui (sem dano padrão)

        // Tem luva? Suprime dano e consome durabilidade.
        ItemStack glove = findGlove(player);
        if (!glove.isEmpty()) {
            // 1 dano de durabilidade por tick de exposição (consome devagar)
            if (player.level().random.nextInt(2) == 0) {
                glove.hurtAndBreak(1, player, p -> {
                    p.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
                });
            }
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

    /** Conta cubos de matéria escura no inventário (incluindo offhand). */
    private static int countDarkMatter(Player player) {
        int count = 0;
        var dmItem = ModBlocks.DARK_MATTER_BLOCK.get().asItem();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(dmItem)) count += s.getCount();
        }
        return count;
    }

    /** Procura uma Containment Glove em qualquer slot do inventário. */
    private static ItemStack findGlove(Player player) {
        var gloveItem = ModItems.CONTAINMENT_GLOVE.get();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(gloveItem)) return s;
        }
        return ItemStack.EMPTY;
    }
}
