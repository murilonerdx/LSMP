package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.LightningStaffItem;
import br.com.murilo.liberthia.item.MagneticWandItem;
import br.com.murilo.liberthia.item.StaffActiveLogic;
import br.com.murilo.liberthia.item.ThornStaffItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Central per-tick handler for the active-staff aura toggles. Subscribes to
 * {@link LivingEvent.LivingTickEvent} on players holding a staff. Each frame
 * checks whether main- or off-hand stack is currently active and applies the
 * staff-specific effect to nearby valid mobs (never players).
 *
 * Durability is consumed every 20 ticks (1s) of active state.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class StaffAuraEvents {
    private StaffAuraEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p)) return;
        if (!(p.level() instanceof ServerLevel sl)) return;

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = p.getItemInHand(hand);
            if (s.isEmpty()) continue;

            if (s.getItem() instanceof ThornStaffItem) {
                tickThorn(sl, p, s);
            } else if (s.getItem() instanceof LightningStaffItem) {
                tickLightning(sl, p, s);
            } else if (s.getItem() instanceof MagneticWandItem) {
                tickMagnetic(sl, p, s);
            }
        }
    }

    // ---------------------------------------------------------------- thorn
    private static void tickThorn(ServerLevel sl, ServerPlayer p, ItemStack stack) {
        if (!StaffActiveLogic.isActive(stack, sl)) return;
        // Damage pulse every 20 ticks; effects refreshed every 40.
        boolean dmgTick = p.tickCount % 20 == 0;
        boolean fxTick  = p.tickCount % 40 == 0;
        if (!dmgTick && !fxTick) return;

        AABB box = new AABB(p.position(), p.position()).inflate(5.0);
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!StaffActiveLogic.isValidVictim(le, p, true)) continue;
            if (dmgTick) le.hurt(le.damageSources().thorns(p), 2.0F);
            if (fxTick) {
                le.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1, false, true, true));
                if (br.com.murilo.liberthia.registry.ModEffects.BLOOD_INFECTION.get() != null) {
                    le.addEffect(new MobEffectInstance(
                            br.com.murilo.liberthia.registry.ModEffects.BLOOD_INFECTION.get(),
                            120, 0, false, true, true));
                }
            }
        }

        if (p.tickCount % 5 == 0) {
            sl.sendParticles(ParticleTypes.CRIT,
                    p.getX(), p.getY() + 1.0, p.getZ(),
                    8, 1.5, 1.0, 1.5, 0.05);
        }
        consumeDurability(stack, p, 20);
    }

    // ---------------------------------------------------------------- lightning
    private static void tickLightning(ServerLevel sl, ServerPlayer p, ItemStack stack) {
        if (!StaffActiveLogic.isActive(stack, sl)) return;
        if (p.tickCount % 30 != 0) return;

        // Pick the nearest valid mob in 12-block radius and zap it.
        AABB box = new AABB(p.position(), p.position()).inflate(12.0);
        LivingEntity nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!StaffActiveLogic.isValidVictim(le, p, true)) continue;
            double d = le.position().distanceToSqr(p.position());
            if (d < bestDist) { bestDist = d; nearest = le; }
        }
        if (nearest == null) return;

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            Vec3 pos = nearest.position();
            bolt.moveTo(pos.x, pos.y, pos.z);
            bolt.setCause(p);
            sl.addFreshEntity(bolt);
        }
        nearest.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, true, true));
        consumeDurability(stack, p, 30);
    }

    // ---------------------------------------------------------------- magnetic
    private static void tickMagnetic(ServerLevel sl, ServerPlayer p, ItemStack stack) {
        if (!StaffActiveLogic.isActive(stack, sl)) return;

        Vec3 c = p.position().add(0, 0.8, 0);
        AABB box = new AABB(p.position(), p.position()).inflate(6.0);
        boolean any = false;
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!StaffActiveLogic.isValidVictim(le, p, true)) continue;
            Vec3 to = c.subtract(le.position()).normalize();
            double dist = c.distanceTo(le.position());
            double pull = Math.min(0.6, 0.4 / (dist + 1.0));
            le.setDeltaMovement(le.getDeltaMovement().add(to.scale(pull)));
            le.hurtMarked = true;
            if (dist <= 1.6 && p.tickCount % 10 == 0) {
                le.hurt(le.damageSources().magic(), 2.0F);
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, true, true));
            }
            any = true;
        }
        if (any && p.tickCount % 10 == 0) {
            for (int i = 0; i < 8; i++) {
                double a = (p.tickCount * 0.2) + (i * Math.PI / 4);
                sl.sendParticles(ParticleTypes.PORTAL,
                        c.x + Math.cos(a) * 1.5, c.y, c.z + Math.sin(a) * 1.5,
                        1, 0, 0.3, 0, 0.05);
            }
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 0.4F, 0.7F);
        }
        consumeDurability(stack, p, 20);
    }

    /** Consumes 1 durability every {@code period} ticks (anchored on tickCount). */
    private static void consumeDurability(ItemStack stack, ServerPlayer p, int period) {
        if (p.tickCount % period != 0) return;
        if (stack.isDamageableItem()) {
            stack.hurtAndBreak(1, p, sp -> sp.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
        // If durability ran out, mark as deactivated so cooldown messaging is clean.
        if (stack.isEmpty() || stack.getDamageValue() >= stack.getMaxDamage()) {
            stack.getOrCreateTag().putLong(StaffActiveLogic.NBT_ACTIVE_UNTIL, 0L);
        }
    }
}
