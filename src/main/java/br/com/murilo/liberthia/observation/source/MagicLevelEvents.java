package br.com.murilo.liberthia.observation.source;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r71: Hooks que dão XP de magia ao matar mobs.
 *
 * <p>Cast XP é dado direto no {@code ObservationResolver.cast()} ao final.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MagicLevelEvents {

    private MagicLevelEvents() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        if (killer.level().isClientSide) return;

        // Verifica se o player segura item mágico (proxy pra "kill mágico")
        if (!holdsMagicItem(killer)) return;

        int xp = isBoss(event.getEntity()) ? 50 : 5;
        awardXp(killer, xp, "kill");
    }

    /** Verifica se o player segura item do sistema mágico. */
    private static boolean holdsMagicItem(ServerPlayer p) {
        var main = p.getMainHandItem();
        var off = p.getOffhandItem();
        return main.getItem() instanceof br.com.murilo.liberthia.observation.item.GrimoireOfObservationItem
            || main.getItem() instanceof br.com.murilo.liberthia.observation.item.SpellswordItem
            || main.getItem() instanceof br.com.murilo.liberthia.observation.item.ObservationTomeItem
            || off.getItem() instanceof br.com.murilo.liberthia.observation.item.GrimoireOfObservationItem;
    }

    private static boolean isBoss(net.minecraft.world.entity.LivingEntity entity) {
        // Mark mod bosses + vanilla bosses
        return entity instanceof br.com.murilo.liberthia.entity.FleshMotherBossEntity
            || entity instanceof br.com.murilo.liberthia.entity.BloodWardenBossEntity
            || entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss
            || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
    }

    /** Award XP + notify se subir level. Chamado de ObservationResolver e events. */
    public static void awardXp(ServerPlayer p, int delta, String source) {
        int oldLevel = MagicLevelData.getLevel(p);
        boolean leveled = MagicLevelData.addXp(p, delta);
        if (leveled) {
            int newLevel = MagicLevelData.getLevel(p);
            p.displayClientMessage(Component.literal(
                "§5§l✦ Magic Level Up! §r§eLv " + newLevel
                + " §7(+5 max Source, +1% luck)"), false);
            p.level().playSound(null, p.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
    }
}
