package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.item.MarkingStickItem;
import br.com.murilo.liberthia.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class ExecutionStickCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .then(Commands.literal("execstick")
                        .then(Commands.literal("tp")
                                .then(Commands.argument("uuid", StringArgumentType.string())
                                        .executes(ctx -> tp(ctx.getSource(), StringArgumentType.getString(ctx, "uuid")))))
                        .then(Commands.literal("unmark")
                                .then(Commands.argument("uuid", StringArgumentType.string())
                                        .executes(ctx -> unmark(ctx.getSource(), StringArgumentType.getString(ctx, "uuid")))))
                        .then(Commands.literal("clearall")
                                .executes(ctx -> clearAll(ctx.getSource())))));
    }

    private static ItemStack findMarkingStick(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.getItem() == ModItems.MARKING_STICK.get()) return s;
        }
        return null;
    }

    private static int tp(CommandSourceStack src, String uuidStr) {
        if (!(src.getEntity() instanceof ServerPlayer caller)) return 0;
        ItemStack mark = findMarkingStick(caller);
        if (mark == null || !MarkingStickItem.hasPos(mark)) {
            caller.sendSystemMessage(Component.translatable("chat.liberthia.exec_no_stick").withStyle(ChatFormatting.RED));
            return 0;
        }
        try {
            UUID uuid = UUID.fromString(uuidStr);
            ServerPlayer target = src.getServer().getPlayerList().getPlayer(uuid);
            if (target == null) {
                caller.sendSystemMessage(Component.literal("§cJogador offline.").withStyle(ChatFormatting.RED));
                return 0;
            }
            int[] p = MarkingStickItem.getPos(mark);
            ServerLevel lvl = src.getServer().overworld();
            // Use target's current level
            target.teleportTo(target.serverLevel(), p[0] + 0.5, p[1], p[2] + 0.5, target.getYRot(), target.getXRot());
            target.sendSystemMessage(Component.translatable("chat.liberthia.exec_tped").withStyle(ChatFormatting.AQUA));
            caller.sendSystemMessage(Component.translatable("chat.liberthia.exec_sent",
                    target.getDisplayName()).withStyle(ChatFormatting.GOLD));
            target.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, p[0] + 0.5, p[1] + 1, p[2] + 0.5, 30, 0.3, 1, 0.3, 0.1);
            target.serverLevel().playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        } catch (Exception e) {
            caller.sendSystemMessage(Component.literal("§cUUID inválido.").withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int unmark(CommandSourceStack src, String uuidStr) {
        if (!(src.getEntity() instanceof ServerPlayer caller)) return 0;
        ItemStack mark = findMarkingStick(caller);
        if (mark == null) return 0;
        CompoundTag tag = mark.getOrCreateTag();
        ListTag list = tag.getList("marked", 8);
        ListTag newList = new ListTag();
        for (int i = 0; i < list.size(); i++) {
            if (!list.getString(i).equals(uuidStr)) newList.add(StringTag.valueOf(list.getString(i)));
        }
        tag.put("marked", newList);
        caller.sendSystemMessage(Component.translatable("chat.liberthia.exec_unmarked").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static int clearAll(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer caller)) return 0;
        ItemStack mark = findMarkingStick(caller);
        if (mark == null) return 0;
        MarkingStickItem.clearMarked(mark);
        caller.sendSystemMessage(Component.translatable("chat.liberthia.exec_cleared").withStyle(ChatFormatting.YELLOW));
        return 1;
    }
}
