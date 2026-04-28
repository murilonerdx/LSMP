package br.com.murilo.liberthia.ritual;

import br.com.murilo.liberthia.block.entity.GoldenBloodBowlBlockEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Hardcoded ritual: summons {@code FleshMotherBoss} above the central bowl.
 *
 * <p>Pentacle pattern (relative to the central bowl, all on the same Y plane):
 * <pre>
 *      .  .  .  .  .  .  .
 *      .  .  .  X  .  .  .
 *      .  .  .  .  .  .  .
 *      .  X  .  C  .  X  .   ← X = blood_stone, C = central golden bowl
 *      .  .  .  .  .  .  .
 *      .  .  .  X  .  .  .
 *      .  .  .  .  .  .  .
 * </pre>
 * Plus 4 ingredient bowls, one per cardinal corner (NE/NW/SE/SW radius 2),
 * each holding the matching ingredient.
 */
public class SummonFleshMotherRitual extends BloodRitual {

    public static final String ID = "summon_flesh_mother";
    /** 200 ticks = 10 seconds total. */
    public static final int DURATION = 200;

    public SummonFleshMotherRitual() {
        super(ID,
                List.of(
                        new BlockPos( 3, 0,  0),
                        new BlockPos(-3, 0,  0),
                        new BlockPos( 0, 0,  3),
                        new BlockPos( 0, 0, -3)),
                List.of(
                        ModItems.HEART_OF_THE_MOTHER.get(),
                        ModItems.SANGUINE_ESSENCE.get(),
                        ModItems.PRIEST_SIGIL.get(),
                        ModItems.CONGEALED_BLOOD.get()),
                ModItems.BLOOD_RITUAL_DAGGER.get(),
                DURATION);
    }

    @Override
    public void start(ServerLevel level, BlockPos centre,
                      GoldenBloodBowlBlockEntity ritualBE,
                      @Nullable ServerPlayer caster) {
        super.start(level, centre, ritualBE, caster);
        level.playSound(null, centre, SoundEvents.WITHER_SPAWN,
                SoundSource.BLOCKS, 0.6F, 0.4F);
        if (caster != null) {
            caster.displayClientMessage(
                    Component.literal("§4§lA Mãe responde ao chamado..."), false);
        }
    }

    @Override
    public void update(ServerLevel level, BlockPos centre,
                       GoldenBloodBowlBlockEntity ritualBE,
                       @Nullable ServerPlayer caster, int time) {
        // Atmospheric particles every 5 ticks: spinning red dust ring.
        if (time % 5 == 0) {
            double angle = (time * 0.05);
            for (int i = 0; i < 8; i++) {
                double a = angle + (i * Math.PI / 4);
                double r = 1.6;
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        centre.getX() + 0.5 + Math.cos(a) * r,
                        centre.getY() + 1.0 + Math.sin(time * 0.1) * 0.3,
                        centre.getZ() + 0.5 + Math.sin(a) * r,
                        1, 0, 0, 0, 0.005);
            }
        }
        // Heartbeat every 25 ticks.
        if (time % 25 == 0) {
            level.playSound(null, centre, SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.BLOCKS, 0.8F, 0.6F);
        }
    }

    @Override
    public void finish(ServerLevel level, BlockPos centre,
                       GoldenBloodBowlBlockEntity ritualBE,
                       @Nullable ServerPlayer caster) {
        // Big visual + sound burst.
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                centre.getX() + 0.5, centre.getY() + 1.5, centre.getZ() + 0.5,
                80, 1.5, 1.5, 1.5, 0.15);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                centre.getX() + 0.5, centre.getY() + 1.5, centre.getZ() + 0.5,
                40, 1.0, 1.0, 1.0, 0.1);
        level.playSound(null, centre, SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 1.5F, 0.7F);

        // Spawn the boss above the centre.
        if (ModEntities.FLESH_MOTHER_BOSS.get() != null) {
            Entity boss = ModEntities.FLESH_MOTHER_BOSS.get().create(level);
            if (boss != null) {
                boss.moveTo(centre.getX() + 0.5, centre.getY() + 2.0, centre.getZ() + 0.5,
                        level.random.nextFloat() * 360F, 0F);
                if (boss instanceof net.minecraft.world.entity.Mob mob) {
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(centre),
                            MobSpawnType.MOB_SUMMONED, null, null);
                }
                level.addFreshEntity(boss);
            }
        }

        if (caster != null) {
            caster.displayClientMessage(
                    Component.literal("§4§lA Mãe da Carne foi invocada!"), false);
        }
    }
}
