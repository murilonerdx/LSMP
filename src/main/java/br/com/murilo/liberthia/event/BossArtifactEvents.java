package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.BossArtifactItem;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Per-tick aura pulses for boss artifacts carried in any player inventory
 * slot (main, hotbar, off-hand, armor — anywhere). Pulses every 40 ticks (2s).
 *
 * <p>Effect is applied to all OTHER players within the artifact's configured
 * radius (the holder is exempt — they are the "anchor"). If the holder
 * carries all 3 distinct artifacts simultaneously, an extra "Trinity Wrath"
 * pulse deals 2 damage and applies Weakness II.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class BossArtifactEvents {
    private BossArtifactEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer holder)) return;
        if (holder.tickCount % 40 != 0) return;
        if (!(holder.level() instanceof ServerLevel sl)) return;

        Set<BossArtifactItem> carried = new HashSet<>();
        for (ItemStack s : holder.getInventory().items) {
            if (s.getItem() instanceof BossArtifactItem ba) carried.add(ba);
        }
        for (ItemStack s : holder.getInventory().offhand) {
            if (s.getItem() instanceof BossArtifactItem ba) carried.add(ba);
        }
        if (carried.isEmpty()) return;

        // Maximum radius among carried artifacts is the scan box.
        double maxRadius = 0;
        for (BossArtifactItem a : carried) maxRadius = Math.max(maxRadius, a.radius);

        AABB box = new AABB(holder.position(), holder.position()).inflate(maxRadius);
        boolean trinity = countDistinct(carried) >= 3;

        for (Player victim : sl.getEntitiesOfClass(Player.class, box)) {
            if (victim == holder) continue;
            if (victim.isCreative() || victim.isSpectator()) continue;

            for (BossArtifactItem art : carried) {
                if (art.effect.get() == null) continue;
                double d = victim.position().distanceTo(holder.position());
                if (d > art.radius) continue;
                victim.addEffect(new MobEffectInstance(art.effect.get(),
                        art.durationTicks, art.amplifier, false, true, true));
            }

            if (trinity) {
                victim.hurt(victim.damageSources().indirectMagic(holder, holder), 2.0F);
                victim.addEffect(new MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.WEAKNESS,
                        100, 1, false, true, true));
            }
        }

        // Visual indicator on the holder.
        sl.sendParticles(ParticleTypes.SCULK_SOUL,
                holder.getX(), holder.getY() + 1.0, holder.getZ(),
                trinity ? 8 : 3, 0.4, 0.6, 0.4, 0.02);
    }

    /** Trinity = 3 distinct artifact types: cursed_idol + veiled_lantern + pulsing_heart. */
    private static int countDistinct(Set<BossArtifactItem> carried) {
        boolean idol = false, lantern = false, heart = false;
        for (BossArtifactItem a : carried) {
            if (ModItems.CURSED_IDOL != null && ModItems.CURSED_IDOL.isPresent()
                    && a == ModItems.CURSED_IDOL.get()) idol = true;
            if (ModItems.VEILED_LANTERN != null && ModItems.VEILED_LANTERN.isPresent()
                    && a == ModItems.VEILED_LANTERN.get()) lantern = true;
            if (ModItems.PULSING_HEART != null && ModItems.PULSING_HEART.isPresent()
                    && a == ModItems.PULSING_HEART.get()) heart = true;
        }
        return (idol ? 1 : 0) + (lantern ? 1 : 0) + (heart ? 1 : 0);
    }
}
