package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.item.computer.ComputerData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * <b>HD</b> — disco de armazenamento. No Computador (bloco) você pode gravar os
 * dados nele ("→ HD") ou carregar os dados de volta ("← HD"). Permite carregar
 * relatórios entre computadores ou guardar backups.
 */
public class HardDriveItem extends Item {

    public HardDriveItem(Properties props) {
        super(props);
    }

    public static ListTag readFiles(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return new ListTag();
        return tag.getList(ComputerData.NBT_FILES, Tag.TAG_COMPOUND);
    }

    public static void writeFiles(ItemStack stack, ListTag files) {
        stack.getOrCreateTag().put(ComputerData.NBT_FILES, files);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        int count = readFiles(stack).size();
        tip.add(Component.literal("§7Arquivos: §b" + count + "§7/§b" + ComputerData.MAX_FILES));
        tip.add(Component.literal("Grave/leia dados num Computador").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !readFiles(stack).isEmpty();
    }
}
