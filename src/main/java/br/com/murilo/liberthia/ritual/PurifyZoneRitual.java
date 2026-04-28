package br.com.murilo.liberthia.ritual;

import br.com.murilo.liberthia.block.entity.GoldenBloodBowlBlockEntity;
import br.com.murilo.liberthia.effect.BloodInfectionApplier;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ritual: clears Blood Infection from all entities in a 16-block radius and
 * replaces nearby corrupted_soil/infection_growth with regular dirt.
 *
 * <p>Pentacle: cross of 4 blood_stone at radius 4 (cardinal).
 * <p>Ingredients: purifying_flask × 2 + sanguine_essence + cleansing_salt.
 * <p>Activator: purging_pendant.
 * <p>Duration: 4 seconds (80 ticks).
 */
public class PurifyZoneRitual extends BloodRitual {
    public static final String ID = "purify_zone";
    private static final double CLEANSE_RADIUS = 16.0;

    public PurifyZoneRitual() {
        super(ID,
                List.of(
                        new BlockPos( 4, 0,  0),
                        new BlockPos(-4, 0,  0),
                        new BlockPos( 0, 0,  4),
                        new BlockPos( 0, 0, -4)),
                List.of(
                        ModItems.PURIFYING_FLASK.get(),
                        ModItems.PURIFYING_FLASK.get(),
                        ModItems.SANGUINE_ESSENCE.get(),
                        ModItems.CLEANSING_SALT.get()),
                ModItems.PURGING_PENDANT.get(),
                80);
    }

    @Override
    public void start(ServerLevel level, BlockPos centre,
                      GoldenBloodBowlBlockEntity ritualBE, @Nullable ServerPlayer caster) {
        super.start(level, centre, ritualBE, caster);
        level.playSound(null, centre, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.4F);
        if (caster != null) {
            caster.displayClientMessage(
                    Component.literal("§bA luz purificadora se acumula..."), false);
        }
    }

    @Override
    public void update(ServerLevel level, BlockPos centre, GoldenBloodBowlBlockEntity ritualBE,
                       @Nullable ServerPlayer caster, int time) {
        if (time % 3 == 0) {
            for (int i = 0; i < 6; i++) {
                double a = (time * 0.1) + (i * Math.PI / 3);
                level.sendParticles(ParticleTypes.GLOW,
                        centre.getX() + 0.5 + Math.cos(a) * 2.0,
                        centre.getY() + 0.5 + (time * 0.02 % 2.0),
                        centre.getZ() + 0.5 + Math.sin(a) * 2.0,
                        1, 0, 0.05, 0, 0.01);
            }
        }
    }

    @Override
    public void finish(ServerLevel level, BlockPos centre, GoldenBloodBowlBlockEntity ritualBE,
                       @Nullable ServerPlayer caster) {
        // Cleanse living entities in 16-block radius.
        AABB box = new AABB(centre).inflate(CLEANSE_RADIUS);
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (ModEffects.BLOOD_INFECTION.get() != null
                    && le.hasEffect(ModEffects.BLOOD_INFECTION.get())) {
                le.removeEffect(ModEffects.BLOOD_INFECTION.get());
            }
            BloodInfectionApplier.clear(le);
        }
        // Replace infected blocks in the same radius.
        int r = (int) CLEANSE_RADIUS;
        int restored = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = centre.offset(dx, dy, dz);
                    if (centre.distSqr(p) > r * r) continue;
                    var bs = level.getBlockState(p);
                    if (ModBlocks.CORRUPTED_SOIL != null
                            && ModBlocks.CORRUPTED_SOIL.isPresent()
                            && bs.is(ModBlocks.CORRUPTED_SOIL.get())) {
                        level.setBlockAndUpdate(p, Blocks.DIRT.defaultBlockState());
                        restored++;
                        continue;
                    }
                    if (ModBlocks.INFECTION_GROWTH != null
                            && ModBlocks.INFECTION_GROWTH.isPresent()
                            && bs.is(ModBlocks.INFECTION_GROWTH.get())) {
                        level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                        restored++;
                    }
                }
            }
        }
        // Visual + sound burst.
        level.sendParticles(ParticleTypes.GLOW,
                centre.getX() + 0.5, centre.getY() + 1.5, centre.getZ() + 0.5,
                100, 3.0, 1.5, 3.0, 0.1);
        level.sendParticles(ParticleTypes.HEART,
                centre.getX() + 0.5, centre.getY() + 2.0, centre.getZ() + 0.5,
                10, 2.0, 0.5, 2.0, 0.05);
        level.playSound(null, centre, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.2F, 1.6F);

        if (caster != null) {
            caster.displayClientMessage(
                    Component.literal("§bZona purificada. §7" + restored
                            + " blocos restaurados."), false);
        }
    }
}
