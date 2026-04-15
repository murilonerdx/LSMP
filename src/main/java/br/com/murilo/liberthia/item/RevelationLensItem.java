package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Revelation Lens — reveals all invisible entities within 20 blocks
 * by drawing particle wireframes around them.
 */
public class RevelationLensItem extends Item {

    private static final double RANGE = 20.0;

    public RevelationLensItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        ServerLevel sl = (ServerLevel) level;
        AABB area = player.getBoundingBox().inflate(RANGE);
        List<Entity> entities = level.getEntities(player, area, e -> e instanceof LivingEntity le && le.isInvisible());

        int found = 0;
        for (Entity e : entities) {
            found++;
            LivingEntity le = (LivingEntity) e;

            // Remove invisibility effect
            le.removeEffect(MobEffects.INVISIBILITY);

            // Draw wireframe outline with particles
            drawWireframe(sl, le);

            // Glowing effect so they stay visible
            le.setGlowingTag(true);

            if (le instanceof ServerPlayer target) {
                target.sendSystemMessage(Component.translatable("chat.liberthia.revealed").withStyle(ChatFormatting.RED));
            }
        }

        if (found > 0) {
            player.sendSystemMessage(Component.translatable("chat.liberthia.lens_found", found).withStyle(ChatFormatting.AQUA));
            sl.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.0F, 1.5F);
        } else {
            player.sendSystemMessage(Component.translatable("chat.liberthia.lens_clear").withStyle(ChatFormatting.GREEN));
        }

        player.getCooldowns().addCooldown(this, 60);
        return InteractionResultHolder.success(stack);
    }

    private void drawWireframe(ServerLevel sl, LivingEntity entity) {
        AABB bb = entity.getBoundingBox();
        double minX = bb.minX, minY = bb.minY, minZ = bb.minZ;
        double maxX = bb.maxX, maxY = bb.maxY, maxZ = bb.maxZ;

        // 12 edges of a bounding box
        drawLine(sl, minX, minY, minZ, maxX, minY, minZ);
        drawLine(sl, minX, minY, minZ, minX, maxY, minZ);
        drawLine(sl, minX, minY, minZ, minX, minY, maxZ);
        drawLine(sl, maxX, maxY, maxZ, minX, maxY, maxZ);
        drawLine(sl, maxX, maxY, maxZ, maxX, minY, maxZ);
        drawLine(sl, maxX, maxY, maxZ, maxX, maxY, minZ);
        drawLine(sl, minX, maxY, minZ, maxX, maxY, minZ);
        drawLine(sl, minX, maxY, minZ, minX, maxY, maxZ);
        drawLine(sl, maxX, minY, minZ, maxX, minY, maxZ);
        drawLine(sl, maxX, minY, minZ, maxX, maxY, minZ);
        drawLine(sl, minX, minY, maxZ, maxX, minY, maxZ);
        drawLine(sl, minX, minY, maxZ, minX, maxY, maxZ);

        // Head marker
        sl.sendParticles(ParticleTypes.END_ROD,
                (minX + maxX) / 2, maxY + 0.3, (minZ + maxZ) / 2, 5, 0.1, 0.1, 0.1, 0.01);
    }

    private void drawLine(ServerLevel sl, double x1, double y1, double z1, double x2, double y2, double z2) {
        int steps = 6;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double x = x1 + (x2 - x1) * t;
            double y = y1 + (y2 - y1) * t;
            double z = z1 + (z2 - z1) * t;
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.liberthia.revelation_lens.desc1").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.liberthia.revelation_lens.desc2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
