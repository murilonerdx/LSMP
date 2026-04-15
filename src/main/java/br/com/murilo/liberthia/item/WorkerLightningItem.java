package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Worker Lightning — fires lightning at whatever the player is looking at.
 * Shift+right-click cycles mode: LOOK / CHAIN / STORM / SILENT (no-damage visual).
 */
public class WorkerLightningItem extends Item {
    private static final String NBT_MODE = "WorkerLightningMode";
    private static final String[] MODE_NAMES = {"LOOK", "CHAIN", "STORM", "SILENT"};

    public WorkerLightningItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        CompoundTag tag = stack.getOrCreateTag();
        int mode = tag.getInt(NBT_MODE);

        if (player.isShiftKeyDown()) {
            mode = (mode + 1) % MODE_NAMES.length;
            tag.putInt(NBT_MODE, mode);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 target = rayTargetPos(sp, 128.0);

        switch (mode) {
            case 0 -> strike(serverLevel, target, false);
            case 1 -> chainStrike(serverLevel, sp, target);
            case 2 -> stormStrike(serverLevel, target);
            case 3 -> strike(serverLevel, target, true);
            default -> strike(serverLevel, target, false);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private Vec3 rayTargetPos(ServerPlayer sp, double reach) {
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(reach));
        BlockHitResult hit = sp.level().clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        if (hit.getType() != HitResult.Type.MISS) {
            return hit.getLocation();
        }
        // Try to hit entity
        List<LivingEntity> entities = sp.level().getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().expandTowards(look.scale(reach)).inflate(2.0),
                e -> e != sp && e.isAlive());
        LivingEntity closest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : entities) {
            double d = e.distanceToSqr(sp);
            if (d < best) { best = d; closest = e; }
        }
        return closest != null ? closest.position() : end;
    }

    private void strike(ServerLevel level, Vec3 pos, boolean visualOnly) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(pos.x, pos.y, pos.z);
        bolt.setVisualOnly(visualOnly);
        level.addFreshEntity(bolt);
    }

    private void chainStrike(ServerLevel level, ServerPlayer owner, Vec3 center) {
        strike(level, center, false);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(10.0),
                e -> e != owner && e.isAlive());
        int count = 0;
        for (LivingEntity e : nearby) {
            strike(level, e.position(), false);
            if (++count >= 4) break;
        }
    }

    private void stormStrike(ServerLevel level, Vec3 center) {
        for (int i = 0; i < 8; i++) {
            double ox = (level.random.nextDouble() - 0.5) * 16.0;
            double oz = (level.random.nextDouble() - 0.5) * 16.0;
            BlockPos bp = BlockPos.containing(center.x + ox, center.y, center.z + oz);
            bp = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, bp);
            strike(level, Vec3.atBottomCenterOf(bp), false);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int mode = stack.getOrCreateTag().getInt(NBT_MODE);
        tooltip.add(Component.literal("Modo: " + MODE_NAMES[mode % MODE_NAMES.length])
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Shift+Clique: trocar modo").withStyle(ChatFormatting.DARK_GRAY));
    }
}
