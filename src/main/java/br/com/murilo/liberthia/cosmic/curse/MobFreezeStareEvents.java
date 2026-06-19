package br.com.murilo.liberthia.cosmic.curse;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r45: Quando player tem corruption/insanity alta OU segura item
 * amaldiçoado, mobs hostis próximos FREEZE e ENCARAM (silent stare).
 *
 * <h2>Trigger conditions</h2>
 * Ativo enquanto player tem:
 * <ul>
 *   <li>insanity ≥ 50 OU</li>
 *   <li>corruption ≥ 40 OU</li>
 *   <li>algum item amaldiçoado segurado</li>
 * </ul>
 *
 * <h2>Effect</h2>
 * Cada mob hostil num raio de 12b vira a cabeça pro player, congela navigation,
 * e fica olhando 3s. Re-pode atacar depois.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MobFreezeStareEvents {

    public static final int STARE_DURATION_TICKS = 60; // 3s
    public static final String NBT_FROZEN_UNTIL = "liberthia.frozen_until";

    private MobFreezeStareEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 40 != 0) return; // checa a cada 2s

        boolean trigger = InsanityData.getInsanity(sp) >= 50
                || InsanityData.getCorruption(sp) >= 40
                || hasAnyCursedItem(sp);

        if (!trigger) return;

        long now = sp.level().getGameTime();
        long freezeUntil = now + STARE_DURATION_TICKS;

        // Acha mobs hostis num raio 12
        for (LivingEntity e : sp.serverLevel().getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(12))) {
            if (!(e instanceof Mob m)) continue;
            if (m.getMobType() == net.minecraft.world.entity.MobType.UNDEFINED) continue;

            // Faz mob olhar pro player
            Vec3 toPlayer = sp.position().subtract(m.position());
            double horizDist = Math.sqrt(toPlayer.x * toPlayer.x + toPlayer.z * toPlayer.z);
            float yaw = (float) Math.toDegrees(Math.atan2(-toPlayer.x, toPlayer.z));
            float pitch = (float) -Math.toDegrees(Math.atan2(toPlayer.y - 1, horizDist));
            m.setYRot(yaw);
            m.setXRot(pitch);
            m.setYHeadRot(yaw);
            m.setYBodyRot(yaw);
            m.getLookControl().setLookAt(sp.getX(), sp.getY() + 1.5, sp.getZ(), 180F, 180F);
            // Para navigation
            m.getNavigation().stop();
            // Tag pra Goal AI saber respeitar
            m.getPersistentData().putLong(NBT_FROZEN_UNTIL, freezeUntil);
        }
    }

    /**
     * Chamado por entity tick (subscriber a LivingTickEvent) pra forçar
     * mobs frozen a permanecer parados.
     */
    @SubscribeEvent
    public static void onMobTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob m)) return;
        if (m.level().isClientSide) return;
        long until = m.getPersistentData().getLong(NBT_FROZEN_UNTIL);
        if (until == 0) return;
        long now = m.level().getGameTime();
        if (now >= until) {
            m.getPersistentData().remove(NBT_FROZEN_UNTIL);
            return;
        }
        // Mantém parado
        m.setDeltaMovement(0, m.getDeltaMovement().y, 0);
        m.getNavigation().stop();
    }

    private static boolean hasAnyCursedItem(Player p) {
        for (int i = 0; i < 9; i++) {
            if (CursedItemRegistry.isCursed(p.getInventory().getItem(i))) return true;
        }
        if (CursedItemRegistry.isCursed(p.getOffhandItem())) return true;
        return false;
    }
}
