package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.effect.BloodInfectionApplier;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Sanctify Orb — AoE cleanse: converts flesh/infection blocks within radius 6 to stone,
 * removes Blood/Dark Infection from allies, applies Regeneration II.
 */
public class SanctifyOrbItem extends Item {
    public SanctifyOrbItem(Properties p) { super(p.stacksTo(8).rarity(Rarity.EPIC)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        BlockPos center = player.blockPosition();
        int r = 6;
        if (level instanceof ServerLevel sl) {
            int converted = 0;
            for (int dx = -r; dx <= r; dx++)
                for (int dy = -r; dy <= r; dy++)
                    for (int dz = -r; dz <= r; dz++) {
                        BlockPos bp = center.offset(dx, dy, dz);
                        BlockState bs = level.getBlockState(bp);
                        if (bs.is(ModBlocks.LIVING_FLESH.get()) || bs.is(ModBlocks.ATTACKING_FLESH.get())
                                || bs.is(ModBlocks.FLESH_MOTHER.get()) || bs.is(ModBlocks.INFECTION_GROWTH.get())
                                || bs.is(ModBlocks.CORRUPTED_SOIL.get())) {
                            level.setBlockAndUpdate(bp, Blocks.STONE.defaultBlockState());
                            converted++;
                        }
                    }
            // burst
            sl.sendParticles(ParticleTypes.END_ROD, center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
                    120, r, 2, r, 0.1);
            sl.sendParticles(ParticleTypes.FLASH, center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
                    1, 0, 0, 0, 0);
            level.playSound(null, center, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.8F);
            // heal allies
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, new AABB(center).inflate(r))) {
                if (le instanceof Player) {
                    le.removeEffect(ModEffects.BLOOD_INFECTION.get());
                    BloodInfectionApplier.clear(le);
                    try { le.removeEffect(ModEffects.DARK_INFECTION.get()); } catch (Exception ignored) {}
                    le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1, false, true, true));
                    le.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1, false, true, true));
                }
            }
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.getCooldowns().addCooldown(this, 200);
        return InteractionResultHolder.consume(stack);
    }
}
