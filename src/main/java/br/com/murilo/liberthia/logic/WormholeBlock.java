package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wormhole Block — a dark matter portal that teleports players between two linked wormholes.
 *
 * Usage:
 * 1. Place first wormhole → right-click with dark matter shard to start linking
 * 2. Place second wormhole → right-click with dark matter shard to complete link
 * 3. Step into a linked wormhole to teleport to the other one
 *
 * Uses the dark matter shard's NBT to store link position.
 */
public class WormholeBlock extends Block {

    private static final String TAG_LINK_X = "WormholeLinkX";
    private static final String TAG_LINK_Y = "WormholeLinkY";
    private static final String TAG_LINK_Z = "WormholeLinkZ";
    private static final String TAG_LINK_DIM = "WormholeLinkDim";

    // Thin portal shape like nether portal
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);

    public WormholeBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0; // Transparent to light (portal-like)
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        // Link with dark matter shard
        if (held.is(br.com.murilo.liberthia.registry.ModItems.DARK_MATTER_SHARD.get())) {
            if (!level.isClientSide) {
                CompoundTag tag = held.getOrCreateTag();

                if (!tag.contains(TAG_LINK_X)) {
                    // First wormhole — store position
                    tag.putInt(TAG_LINK_X, pos.getX());
                    tag.putInt(TAG_LINK_Y, pos.getY());
                    tag.putInt(TAG_LINK_Z, pos.getZ());
                    tag.putString(TAG_LINK_DIM, level.dimension().location().toString());

                    player.displayClientMessage(
                            Component.literal("Wormhole A linked [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]")
                                    .withStyle(ChatFormatting.DARK_PURPLE), true);
                } else {
                    // Second wormhole — complete link
                    int ax = tag.getInt(TAG_LINK_X);
                    int ay = tag.getInt(TAG_LINK_Y);
                    int az = tag.getInt(TAG_LINK_Z);
                    BlockPos otherPos = new BlockPos(ax, ay, az);

                    // Verify the other wormhole still exists
                    if (level.getBlockState(otherPos).is(ModBlocks.WORMHOLE_BLOCK.get())) {
                        player.displayClientMessage(
                                Component.literal("Wormhole B linked! Portal active.")
                                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
                        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 0.5F);
                    } else {
                        player.displayClientMessage(
                                Component.literal("Wormhole A no longer exists!")
                                        .withStyle(ChatFormatting.RED), true);
                    }

                    // Clear the tag
                    tag.remove(TAG_LINK_X);
                    tag.remove(TAG_LINK_Y);
                    tag.remove(TAG_LINK_Z);
                    tag.remove(TAG_LINK_DIM);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;

        // Cooldown to prevent teleport loops
        if (player.tickCount % 40 != 0) return;

        // Search for linked wormhole in nearby area (max 256 blocks)
        BlockPos linked = findLinkedWormhole(level, pos, 256);
        if (linked != null) {
            player.teleportTo(linked.getX() + 0.5, linked.getY() + 1.0, linked.getZ() + 0.5);
            level.playSound(null, linked, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 0.8F);

            // Apply brief nausea for immersion
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.CONFUSION, 60, 0));

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        linked.getX() + 0.5, linked.getY() + 1.0, linked.getZ() + 0.5,
                        30, 0.5, 1.0, 0.5, 0.05);
            }
        }
    }

    private BlockPos findLinkedWormhole(Level level, BlockPos self, int searchRadius) {
        // Search for the nearest other wormhole block
        int r = Math.min(searchRadius, 64);
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (BlockPos check : BlockPos.betweenClosed(
                self.offset(-r, -r, -r), self.offset(r, r, r))) {
            if (check.equals(self)) continue;
            if (level.getBlockState(check).is(ModBlocks.WORMHOLE_BLOCK.get())) {
                double dist = check.distSqr(self);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = check.immutable();
                }
            }
        }
        return nearest;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Purple/void particle effects
        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z,
                    (random.nextDouble() - 0.5) * 0.2, 0.1, (random.nextDouble() - 0.5) * 0.2);
        }
        if (random.nextFloat() < 0.3f) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            level.addParticle(ParticleTypes.PORTAL, x, y, z, 0, 0.5, 0);
        }
    }
}
