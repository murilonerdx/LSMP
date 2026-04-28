package br.com.murilo.liberthia.ritual;

import br.com.murilo.liberthia.block.entity.GoldenBloodBowlBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Occultism-inspired generic crafting ritual: consumes ingredients from
 * sacrificial bowls, then drops a single {@link ItemStack} above the central
 * bowl on finish. Subclassing is optional — instantiate directly with the
 * desired result + ingredients.
 */
public class CraftRitual extends BloodRitual {

    private final Supplier<ItemStack> resultSupplier;

    public CraftRitual(String id,
                       List<BlockPos> pentacleOffsets,
                       List<Item> ingredients,
                       Item activator,
                       int durationTicks,
                       Supplier<ItemStack> result) {
        super(id, pentacleOffsets, ingredients, activator, durationTicks);
        this.resultSupplier = result;
    }

    @Override
    public void start(ServerLevel level, BlockPos centre,
                      GoldenBloodBowlBlockEntity ritualBE, @Nullable ServerPlayer caster) {
        super.start(level, centre, ritualBE, caster);
        level.playSound(null, centre, SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.BLOCKS, 0.7F, 1.2F);
    }

    @Override
    public void update(ServerLevel level, BlockPos centre, GoldenBloodBowlBlockEntity ritualBE,
                       @Nullable ServerPlayer caster, int time) {
        if (time % 4 == 0) {
            for (int i = 0; i < 3; i++) {
                double a = (time * 0.06) + (i * Math.PI * 2 / 3);
                level.sendParticles(ParticleTypes.ENCHANT,
                        centre.getX() + 0.5 + Math.cos(a) * 1.0,
                        centre.getY() + 1.2,
                        centre.getZ() + 0.5 + Math.sin(a) * 1.0,
                        1, 0, 0, 0, 0.05);
            }
        }
    }

    @Override
    public void finish(ServerLevel level, BlockPos centre, GoldenBloodBowlBlockEntity ritualBE,
                       @Nullable ServerPlayer caster) {
        ItemStack result = resultSupplier.get();
        if (result.isEmpty()) return;

        // Visual + drop above the bowl.
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                centre.getX() + 0.5, centre.getY() + 1.5, centre.getZ() + 0.5,
                40, 0.6, 0.8, 0.6, 0.1);
        level.playSound(null, centre, SoundEvents.PLAYER_LEVELUP,
                SoundSource.BLOCKS, 0.8F, 1.6F);

        ItemEntity drop = new ItemEntity(level,
                centre.getX() + 0.5, centre.getY() + 1.6, centre.getZ() + 0.5,
                result);
        drop.setDeltaMovement(0, 0.25, 0);
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);

        if (caster != null) {
            caster.displayClientMessage(
                    Component.literal("§6Forjado: §f" + result.getHoverName().getString()),
                    false);
        }
    }
}
