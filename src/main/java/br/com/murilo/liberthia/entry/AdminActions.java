package br.com.murilo.liberthia.entry;


import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AdminActions {

    private AdminActions() {}

    public static void execute(ServerPlayer admin, AdminActionC2SPacket packet) {
        if (!admin.hasPermissions(2)) return;

        switch (packet.getAction()) {
            case REQUEST_PLAYERS -> sendPlayerList(admin);
            case REQUEST_INVENTORY -> sendInventory(admin, getTarget(admin, packet));
            case SEND_POSITION -> sendPosition(admin, getTarget(admin, packet));
            case APPLY_EFFECT -> applyEffect(admin, getTarget(admin, packet), packet.getEffectId(), packet.getDurationSeconds(), packet.getAmplifier());
            case CLEAR_EFFECT -> clearEffect(admin, getTarget(admin, packet), packet.getEffectId());
            case GIVE_ITEM -> giveItem(admin, getTarget(admin, packet), packet.getItemId(), packet.getItemCount());
            case REMOVE_ITEM -> removeItem(admin, getTarget(admin, packet), packet.getItemId(), packet.getItemCount());
            case SCARE_SINGLE -> scareSingle(admin, getTarget(admin, packet));
            case SCARE_AREA -> scareArea(admin, getTarget(admin, packet));
            case SUMMON_MONSTER -> summonMonster(admin, getTarget(admin, packet), packet.getItemId(), packet.getItemCount());
            case PLACE_BLOCK -> placeBlock(admin, getTarget(admin, packet), packet.getItemId(), packet.getItemCount());
        }
    }

    /**
     * Places any block/fluid around the target player.
     * - id can be a block id ("minecraft:water", "liberthia:dark_matter_block") OR a fluid id ("minecraft:water", "liberthia:dark_matter").
     * - radius 0 = single block at feet, 1..8 = filled cube around the target.
     */
    private static void placeBlock(ServerPlayer admin, ServerPlayer target, String blockIdText, int radius) {
        if (target == null) return;
        ResourceLocation id = ResourceLocation.tryParse(blockIdText);
        if (id == null) return;

        net.minecraft.world.level.block.state.BlockState state = null;

        // Try as block first
        net.minecraft.world.level.block.Block block = ForgeRegistries.BLOCKS.getValue(id);
        if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
            state = block.defaultBlockState();
        }

        // Fall back to fluid (store as fluid source block)
        if (state == null) {
            net.minecraft.world.level.material.Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
            if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                state = fluid.defaultFluidState().createLegacyBlock();
            }
        }

        if (state == null) return;

        ServerLevel level = target.serverLevel();
        int r = Mth.clamp(radius, 0, 8);
        net.minecraft.core.BlockPos origin = target.blockPosition();

        if (r == 0) {
            level.setBlockAndUpdate(origin, state);
            return;
        }

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    net.minecraft.core.BlockPos p = origin.offset(dx, dy, dz);
                    // skip the target's own body blocks so we don't suffocate instantly
                    if (dx == 0 && dz == 0 && (dy == 0 || dy == 1)) continue;
                    level.setBlockAndUpdate(p, state);
                }
            }
        }
    }

    private static void summonMonster(ServerPlayer admin, ServerPlayer target, String entityIdText, int count) {
        if (target == null) return;
        ResourceLocation id = ResourceLocation.tryParse(entityIdText);
        if (id == null) return;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) return;
        ServerLevel level = target.serverLevel();
        int n = Mth.clamp(count, 1, 32);
        for (int i = 0; i < n; i++) {
            net.minecraft.world.entity.Entity e = type.create(level);
            if (e == null) continue;
            double ox = (level.random.nextDouble() - 0.5) * 3.0;
            double oz = (level.random.nextDouble() - 0.5) * 3.0;
            e.moveTo(target.getX() + ox, target.getY(), target.getZ() + oz, level.random.nextFloat() * 360F, 0F);
            level.addFreshEntity(e);
        }
    }

    private static ServerPlayer getTarget(ServerPlayer admin, AdminActionC2SPacket packet) {
        if (packet.getTargetId() == null || admin.server == null) return null;
        return admin.server.getPlayerList().getPlayer(packet.getTargetId());
    }

    private static void sendPlayerList(ServerPlayer admin) {
        if (admin.server == null) return;

        List<AdminPlayerEntry> players = admin.server.getPlayerList().getPlayers().stream()
                .sorted(Comparator.comparing(ServerPlayer::getScoreboardName, String.CASE_INSENSITIVE_ORDER))
                .map(AdminActions::toEntry)
                .toList();

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> admin),
                new AdminPlayerListS2CPacket(players)
        );
    }

    private static AdminPlayerEntry toEntry(ServerPlayer player) {
        return new AdminPlayerEntry(
                player.getUUID(),
                player.getName().getString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getHealth(),
                player.getMaxHealth(),
                player.getFoodData().getFoodLevel(),
                player.getArmorValue(),
                player.level().dimension().location().toString(),
                readableStack(player.getMainHandItem()),
                readableStack(player.getOffhandItem())
        );
    }

    private static void sendInventory(ServerPlayer admin, ServerPlayer target) {
        if (target == null) return;

        AdminInventorySnapshot snapshot = new AdminInventorySnapshot(
                target.getUUID(),
                target.getName().getString(),
                copyStacks(target.getInventory().items),
                copyStacks(target.getInventory().armor),
                target.getOffhandItem().copy()
        );

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> admin),
                new AdminInventoryS2CPacket(snapshot)
        );
    }

    private static void sendPosition(ServerPlayer admin, ServerPlayer target) {
        if (target == null) return;
        // silencioso — posição visível na GUI
    }

    private static void applyEffect(ServerPlayer admin, ServerPlayer target, String effectIdText, int durationSeconds, int amplifier) {
        if (target == null) return;
        ResourceLocation effectId = ResourceLocation.tryParse(effectIdText);
        if (effectId == null) return;
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        if (effect == null) return;

        int clampedSeconds = Mth.clamp(durationSeconds, 1, 3600);
        int clampedAmplifier = Mth.clamp(amplifier, 0, 255);
        target.addEffect(new MobEffectInstance(effect, clampedSeconds * 20, clampedAmplifier, false, true, true));
    }

    private static void clearEffect(ServerPlayer admin, ServerPlayer target, String effectIdText) {
        if (target == null) return;
        ResourceLocation effectId = ResourceLocation.tryParse(effectIdText);
        if (effectId == null) return;
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        if (effect == null) return;
        target.removeEffect(effect);
    }

    private static void giveItem(ServerPlayer admin, ServerPlayer target, String itemIdText, int count) {
        if (target == null) return;
        ResourceLocation itemId = ResourceLocation.tryParse(itemIdText);
        if (itemId == null) return;
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) return;

        int remaining = Mth.clamp(count, 1, 4096);
        int maxStack = Math.max(1, item.getMaxStackSize());

        while (remaining > 0) {
            int toGive = Math.min(maxStack, remaining);
            ItemStack stack = new ItemStack(item, toGive);
            boolean added = target.getInventory().add(stack);
            if (!added && !stack.isEmpty()) {
                target.drop(stack, false);
            }
            remaining -= toGive;
        }

        target.getInventory().setChanged();
        target.inventoryMenu.broadcastChanges();
        target.containerMenu.broadcastChanges();
    }

    private static void removeItem(ServerPlayer admin, ServerPlayer target, String itemIdText, int count) {
        if (target == null) return;
        ResourceLocation itemId = ResourceLocation.tryParse(itemIdText);
        if (itemId == null) return;
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) return;

        int remaining = Mth.clamp(count, 1, 4096);
        remaining = removeFromList(target.getInventory().items, item, remaining);
        remaining = removeFromList(target.getInventory().armor, item, remaining);
        remaining = removeFromList(target.getInventory().offhand, item, remaining);

        target.getInventory().setChanged();
        target.inventoryMenu.broadcastChanges();
        target.containerMenu.broadcastChanges();
    }

    private static int removeFromList(NonNullList<ItemStack> stacks, Item targetItem, int remaining) {
        for (int i = 0; i < stacks.size() && remaining > 0; i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty() && stack.is(targetItem)) {
                int removeNow = Math.min(remaining, stack.getCount());
                stack.shrink(removeNow);
                if (stack.isEmpty()) {
                    stacks.set(i, ItemStack.EMPTY);
                }
                remaining -= removeNow;
            }
        }
        return remaining;
    }

    private static void scareSingle(ServerPlayer admin, ServerPlayer target) {
        if (target == null) return;

        ServerLevel level = target.serverLevel();
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 20 * 5, 0, false, true, true));
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 20 * 4, 0, false, true, true));
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, 1, false, true, true));

        level.playSound(null, target.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, target.blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 0.7F, 1.0F);
        level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5F, 1.2F);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(target.getX(), target.getY(), target.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    private static void scareArea(ServerPlayer admin, ServerPlayer target) {
        if (target == null) return;

        ServerLevel level = target.serverLevel();
        List<ServerPlayer> nearby = level.getEntitiesOfClass(
                ServerPlayer.class,
                target.getBoundingBox().inflate(8.0D),
                p -> p != null && p.isAlive()
        );

        for (ServerPlayer p : nearby) {
            p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 20 * 4, 0, false, true, true));
            p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, 1, false, true, true));
            p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 20 * 5, 0, false, true, true));
            level.playSound(null, p.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.PLAYERS, 1.0F, 0.7F);
            level.playSound(null, p.blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 0.7F, 1.0F);
        }

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(target.getX(), target.getY(), target.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    private static List<ItemStack> copyStacks(NonNullList<ItemStack> source) {
        List<ItemStack> copy = new ArrayList<>(source.size());
        for (ItemStack stack : source) {
            copy.add(stack.copy());
        }
        return copy;
    }

    private static String readableStack(ItemStack stack) {
        if (stack.isEmpty()) return "vazio";
        return stack.getCount() + "x " + stack.getHoverName().getString();
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
