package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.entity.BlackHoleEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class WhiteLightWand extends Item {
    public WhiteLightWand(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000; // Continuous usage
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (!(living instanceof Player player)) return;

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        double range = 25.0;
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        // Block Raycast
        HitResult blockHit = level.clip(new net.minecraft.world.level.ClipContext(
                eyePos, endPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player
        ));

        Vec3 maxVec = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : endPos;

        // Visuals (Laser trail)
        if (level instanceof ServerLevel serverLevel) {
            double distance = eyePos.distanceTo(maxVec);
            for (double d = 1.0; d < distance; d += 0.5) {
                Vec3 p = eyePos.add(lookVec.scale(d));
                serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0.0);
            }
        }

        if (!level.isClientSide) {
            // Priority: Stabilize Black Hole (Area check near ray)
            AABB laserBox = new AABB(eyePos, maxVec).inflate(2.0);
            List<BlackHoleEntity> blackHoles = level.getEntitiesOfClass(BlackHoleEntity.class, laserBox);
            for (BlackHoleEntity blackHole : blackHoles) {
                blackHole.stabilize(0.05f); // Gradual containment
                level.playSound(null, blackHole.blockPosition(), ModSounds.CLEAR_HUM.get(), SoundSource.PLAYERS, 0.2F, 1.8F);
            }

            // Beam Cleaning: Clear blocks ALONG the path
            double distance = eyePos.distanceTo(maxVec);
            for (double d = 0; d < distance; d += 1.0) {
                Vec3 p = eyePos.add(lookVec.scale(d));
                clearArea(level, new BlockPos((int)p.x, (int)p.y, (int)p.z));
            }

            // Entity Raycast
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    level, player, eyePos, maxVec,
                    laserBox, e -> !e.isSpectator() && e.isPickable() && e instanceof LivingEntity
            );

            if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
                target.hurt(level.damageSources().magic(), 4.0F); // Increased magic damage
            }

            // Durability drain
            if (player.tickCount % 5 == 0) {
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
            }
            
            // Sound
            if (player.tickCount % 5 == 0) {
                level.playSound(null, player.blockPosition(), ModSounds.CLEAR_HUM.get(), SoundSource.PLAYERS, 0.4F, 2.0F);
            }
        }
    }

    private void clearArea(Level level, BlockPos pos) {
        for (BlockPos targetPos : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            BlockState s = level.getBlockState(targetPos);
            if (s.is(ModBlocks.INFECTION_GROWTH.get()) || 
                s.is(ModBlocks.CORRUPTED_SOIL.get()) ||
                s.is(ModBlocks.DARK_MATTER_BLOCK.get()) ||
                s.is(ModBlocks.DARK_MATTER_ORE.get()) ||
                s.is(ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get()) ||
                s.is(ModBlocks.DARK_MATTER_FLUID_BLOCK.get())) {
                level.setBlockAndUpdate(targetPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
        }
    }
}
