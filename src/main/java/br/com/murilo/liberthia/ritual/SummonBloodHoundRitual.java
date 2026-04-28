package br.com.murilo.liberthia.ritual;

import br.com.murilo.liberthia.block.entity.GoldenBloodBowlBlockEntity;
import br.com.murilo.liberthia.entity.BloodHoundEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ritual: spawns a TAMED Blood Hound bonded to the caster. Smaller pentacle
 * than the Mother ritual (corners at radius 2).
 *
 * <p>Pentacle: 4 blood_stone at NW/NE/SW/SE corners radius 2.
 * <p>Ingredients: 2 × bloody_rag + 2 × congealed_blood (in 4 bowls).
 * <p>Activator: blood_pact_amulet.
 * <p>Duration: 6 seconds (120 ticks).
 */
public class SummonBloodHoundRitual extends BloodRitual {
    public static final String ID = "summon_blood_hound";

    public SummonBloodHoundRitual() {
        super(ID,
                List.of(
                        new BlockPos( 2, 0,  2),
                        new BlockPos( 2, 0, -2),
                        new BlockPos(-2, 0,  2),
                        new BlockPos(-2, 0, -2)),
                List.of(
                        ModItems.BLOODY_RAG.get(),
                        ModItems.BLOODY_RAG.get(),
                        ModItems.CONGEALED_BLOOD.get(),
                        ModItems.CONGEALED_BLOOD.get()),
                ModItems.BLOOD_PACT_AMULET.get(),
                120);
    }

    @Override
    public void start(ServerLevel level, BlockPos centre,
                      GoldenBloodBowlBlockEntity ritualBE, @Nullable ServerPlayer caster) {
        super.start(level, centre, ritualBE, caster);
        level.playSound(null, centre, SoundEvents.WOLF_GROWL, SoundSource.BLOCKS, 1.0F, 0.5F);
        if (caster != null) {
            caster.displayClientMessage(
                    Component.literal("§cUm uivo distante responde ao pacto..."), false);
        }
    }

    @Override
    public void update(ServerLevel level, BlockPos centre, GoldenBloodBowlBlockEntity ritualBE,
                       @Nullable ServerPlayer caster, int time) {
        if (time % 4 == 0) {
            for (int i = 0; i < 4; i++) {
                double a = (time * 0.08) + (i * Math.PI / 2);
                level.sendParticles(ParticleTypes.DRIPPING_LAVA,
                        centre.getX() + 0.5 + Math.cos(a) * 1.4,
                        centre.getY() + 0.8,
                        centre.getZ() + 0.5 + Math.sin(a) * 1.4,
                        1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public void finish(ServerLevel level, BlockPos centre, GoldenBloodBowlBlockEntity ritualBE,
                       @Nullable ServerPlayer caster) {
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                centre.getX() + 0.5, centre.getY() + 1.5, centre.getZ() + 0.5,
                40, 0.8, 0.8, 0.8, 0.1);
        level.playSound(null, centre, SoundEvents.WOLF_HOWL, SoundSource.HOSTILE, 1.0F, 0.6F);

        BloodHoundEntity hound = ModEntities.BLOOD_HOUND.get().create(level);
        if (hound == null) return;
        hound.moveTo(centre.getX() + 0.5, centre.getY() + 1.0, centre.getZ() + 0.5,
                level.random.nextFloat() * 360F, 0F);
        hound.finalizeSpawn(level, level.getCurrentDifficultyAt(centre),
                MobSpawnType.MOB_SUMMONED, null, null);
        if (caster != null) {
            hound.tame(caster);
            hound.setHealth(hound.getMaxHealth());
            hound.setOrderedToSit(false);
        }
        level.addFreshEntity(hound);

        if (caster != null) {
            caster.displayClientMessage(
                    Component.literal("§cO pacto foi selado. O cão obedece."), false);
        }
    }
}
