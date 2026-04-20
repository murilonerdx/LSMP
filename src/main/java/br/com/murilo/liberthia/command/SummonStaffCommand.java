package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class SummonStaffCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .then(Commands.literal("summonstaff")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("entityid", StringArgumentType.string())
                                        .executes(ctx -> set(ctx.getSource(), StringArgumentType.getString(ctx, "entityid"), "", ""))
                                        .then(Commands.argument("tag", StringArgumentType.string())
                                                .executes(ctx -> set(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "entityid"),
                                                        StringArgumentType.getString(ctx, "tag"), ""))
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(ctx -> set(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "entityid"),
                                                                StringArgumentType.getString(ctx, "tag"),
                                                                StringArgumentType.getString(ctx, "name")))))))
                        .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource())))));
    }

    private static ItemStack findStaff(ServerPlayer p) {
        ItemStack main = p.getMainHandItem();
        if (main.getItem() == ModItems.SUMMON_STAFF.get()) return main;
        ItemStack off = p.getOffhandItem();
        if (off.getItem() == ModItems.SUMMON_STAFF.get()) return off;
        return null;
    }

    private static int set(CommandSourceStack src, String entityId, String tag, String name) {
        if (!(src.getEntity() instanceof ServerPlayer p)) return 0;
        ItemStack staff = findStaff(p);
        if (staff == null) {
            p.sendSystemMessage(Component.literal("§cSegure o Summon Staff.").withStyle(ChatFormatting.RED));
            return 0;
        }
        CompoundTag t = staff.getOrCreateTag();
        t.putString("entity", entityId);
        t.putString("tag", tag);
        t.putString("name", name);
        p.sendSystemMessage(Component.literal("§aSummon Staff configurado: §f" + entityId +
                (tag.isEmpty() ? "" : " §7[tag: " + tag + "]") +
                (name.isEmpty() ? "" : " §7[name: " + name + "]")));
        return 1;
    }

    private static int clear(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer p)) return 0;
        ItemStack staff = findStaff(p);
        if (staff == null) return 0;
        if (staff.hasTag()) {
            staff.getTag().remove("entity");
            staff.getTag().remove("tag");
            staff.getTag().remove("name");
        }
        p.sendSystemMessage(Component.literal("§eSummon Staff limpo."));
        return 1;
    }
}
