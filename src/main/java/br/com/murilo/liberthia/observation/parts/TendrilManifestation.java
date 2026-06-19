package br.com.murilo.liberthia.observation.parts;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.api.Manifestation;
import br.com.murilo.liberthia.observation.api.ObservationContext;
import br.com.murilo.liberthia.observation.api.ObservationStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * v0.1.22 r60: <b>Tendril Manifestation</b> — tentáculos emergem do hit
 * point. Damage = intensity. Knockback. Particles soul.
 */
public class TendrilManifestation extends Manifestation {

    public TendrilManifestation() {
        super(new ResourceLocation(LiberthiaMod.MODID, "manifest/tendril"), "Tentáculos");
    }

    @Override public int sanityCost() { return 4; }

    @Override
    public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                          ObservationStats stats, ObservationContext ctx) {
        Vec3 epicenter;
        if (hit instanceof EntityHitResult ehr) {
            epicenter = ehr.getEntity().position();
        } else {
            epicenter = hit.getLocation();
        }

        // Damage entities in reach
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(0).move(
                        epicenter.subtract(caster.position())).inflate(stats.reach))) {
            if (le == caster) continue;
            le.hurt(caster.damageSources().magic(), (float) stats.intensity * 4F);
            // Knockback radial
            Vec3 away = le.position().subtract(epicenter).normalize().scale(0.4);
            le.setDeltaMovement(away.x, 0.3, away.z);
            le.hurtMarked = true;
        }

        // Tendrils particles — N rings up + down
        int rings = 8;
        for (int i = 0; i < rings; i++) {
            double a = (i / (double)rings) * Math.PI * 2;
            for (double r = 0; r < stats.reach; r += 0.3) {
                level.sendParticles(ParticleTypes.SOUL,
                        epicenter.x + Math.cos(a) * r,
                        epicenter.y + 0.2,
                        epicenter.z + Math.sin(a) * r,
                        1, 0.05, 0.05, 0.05, 0);
            }
        }
        // Central burst
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                epicenter.x, epicenter.y + 0.5, epicenter.z,
                (int)(20 * stats.intensity), 0.5, 0.8, 0.5, 0.05);

        caster.displayClientMessage(Component.literal(
                "§5§o✦ algo sai do chão."), true);
    }

    @Override
    public List<Component> lore() {
        return List.of(
            Component.literal("§5§oO que dorme abaixo te observa."),
            Component.literal("§8§oCusto: 4 sanity base.")
        );
    }
}
