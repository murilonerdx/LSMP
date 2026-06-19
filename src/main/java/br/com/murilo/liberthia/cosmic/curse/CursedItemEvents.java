package br.com.murilo.liberthia.cosmic.curse;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager;
import br.com.murilo.liberthia.cosmic.hallucination.HallucinationType;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r45: Event hooks para items amaldiçoados.
 *
 * <ul>
 *   <li>{@link ItemTossEvent}: cancela drop manual (Q tecla)</li>
 *   <li>{@code PlayerTickEvent}: acumula corruption 0.5/sec por item amaldiçoado</li>
 *   <li>{@code PlayerTickEvent}: trigger hallucination periódica</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CursedItemEvents {

    private CursedItemEvents() {}

    /**
     * Player tenta soltar item com Q → se for amaldiçoado, cancela e
     * faz item voltar pro inventário + mensagem.
     */
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemEntity entity = event.getEntity();
        ItemStack stack = entity.getItem();
        Player player = event.getPlayer();
        if (!CursedItemRegistry.isCursed(stack)) return;

        // Cancela: retorna item ao inventário do player + mensagem
        event.setCanceled(true);
        if (!player.getInventory().add(stack)) {
            // Sem espaço — força inserir num slot vazio ou drop normal
            player.drop(stack, false);
        }

        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(
                    "§4§o§lA peça se cola à sua mão. Não a solta.§r"), true);
            // Damage de fundo + horror
            InsanityData.addCorruption(sp, 5);
            InsanityData.addParanoia(sp, 3);
            HallucinationManager.force(sp, HallucinationType.FAKE_WHISPER, 1.0F, 30, "");
            // Som distorcido
            sp.level().playSound(null, sp.blockPosition(),
                    SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 0.5F);
        }
    }

    /**
     * Tick por player: pra cada item amaldiçoado segurado/equipado, drena
     * sanity gradualmente + spawn partículas cursed.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        ServerLevel level = sp.serverLevel();

        // A cada 40 ticks (2s) chama o checker
        if (sp.tickCount % 40 != 0) return;

        int cursedCount = countCursedHeld(sp);
        if (cursedCount == 0) return;

        // Acumula corruption + insanity escalonado pelo nº de cursed items
        InsanityData.addCorruption(sp, cursedCount);
        if (sp.tickCount % 200 == 0) {
            InsanityData.addInsanity(sp, cursedCount);
        }

        // Spawn cursed pulse particles ao redor do player (visual feedback)
        try {
            for (int i = 0; i < cursedCount; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = 0.8 + Math.random() * 0.5;
                level.sendParticles(ModParticles.CURSED_PULSE.get(),
                        sp.getX() + Math.cos(angle) * r,
                        sp.getY() + 1.0 + Math.random() * 0.5,
                        sp.getZ() + Math.sin(angle) * r,
                        1, 0, 0, 0, 0);
            }
        } catch (Throwable ignored) {}

        // 8% chance/2s de hallucination de whisper — só com sanidade < 40% (r183)
        if (br.com.murilo.liberthia.dimension.SpiritDimension.getSanity(sp) < 40 && Math.random() < 0.08 * cursedCount) {
            HallucinationType[] options = {
                    HallucinationType.FAKE_WHISPER,
                    HallucinationType.SHADOW_MOVEMENT,
                    HallucinationType.HEARTBEAT_PULSE,
                    HallucinationType.NAME_WHISPER
            };
            HallucinationType pick = options[(int)(Math.random() * options.length)];
            HallucinationManager.force(sp, pick, 0.7F, 40, "");
        }
    }

    private static int countCursedHeld(Player p) {
        int count = 0;
        // Verifica hotbar + offhand (slots 0-8 + 40)
        for (int i = 0; i < 9; i++) {
            if (CursedItemRegistry.isCursed(p.getInventory().getItem(i))) count++;
        }
        if (CursedItemRegistry.isCursed(p.getOffhandItem())) count++;
        return count;
    }
}
