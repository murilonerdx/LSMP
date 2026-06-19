package br.com.murilo.liberthia.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.List;

/**
 * v0.1.24 r92: <b>Warp Scroll</b> — bind GlobalPos + teleport.
 *
 * <h2>Uso</h2>
 * <ol>
 *   <li>Shift+right-click → bind posição atual + dimensão</li>
 *   <li>Right-click sem shift → teleporta. Consome scroll (variant stable não).</li>
 * </ol>
 */
public class WarpScrollItem extends Item {

    private final boolean stable;

    public WarpScrollItem(Properties props, boolean stable) {
        super(stable ? props.stacksTo(1) : props.stacksTo(16));
        this.stable = stable;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }

        if (sp.isShiftKeyDown()) {
            // BIND
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString("Dim", sp.level().dimension().location().toString());
            tag.putInt("X", (int) sp.getX());
            tag.putInt("Y", (int) sp.getY());
            tag.putInt("Z", (int) sp.getZ());
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5Scroll vinculado a §b" + (int) sp.getX() + ", "
                    + (int) sp.getY() + ", " + (int) sp.getZ()), false);
            return InteractionResultHolder.success(stack);
        }

        // TELEPORT
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Dim")) {
            sp.displayClientMessage(Component.literal(
                    "§7§oScroll não vinculado. Shift+R-click pra bindar."), true);
            return InteractionResultHolder.fail(stack);
        }
        if (sp.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        ResourceLocation rl = ResourceLocation.tryParse(tag.getString("Dim"));
        if (rl == null) return InteractionResultHolder.fail(stack);
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, rl);
        ServerLevel target = sp.server.getLevel(dimKey);
        if (target == null) {
            sp.displayClientMessage(Component.literal("§4§o✦ Dimensão não encontrada."), true);
            return InteractionResultHolder.fail(stack);
        }
        int x = tag.getInt("X");
        int y = tag.getInt("Y");
        int z = tag.getInt("Z");

        sp.teleportTo(target, x + 0.5, y, z + 0.5,
                EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                sp.getYRot(), sp.getXRot());

        // Particle + sound
        if (sp.level() instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    sp.getX(), sp.getY() + 1, sp.getZ(), 30, 0.5, 1.0, 0.5, 0.1);
        }
        sp.level().playSound(null, sp.blockPosition(),
                net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);

        sp.getCooldowns().addCooldown(this, 60);
        if (!stable && !sp.isCreative()) {
            stack.shrink(1);
        }
        sp.displayClientMessage(Component.literal("§5§l✦ §r§5Tu chegou."), false);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Dim")) {
            tooltip.add(Component.literal("§5Vinculado: §b" + tag.getInt("X") + ", " + tag.getInt("Y") + ", " + tag.getInt("Z")));
            tooltip.add(Component.literal("§7Dim: §8" + tag.getString("Dim")));
        } else {
            tooltip.add(Component.literal("§7§oShift+R-click: bindar"));
        }
        tooltip.add(Component.literal(stable ? "§a§oReutilizável" : "§c§oConsumido ao usar"));
    }
}
