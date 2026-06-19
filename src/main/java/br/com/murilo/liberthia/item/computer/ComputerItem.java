package br.com.murilo.liberthia.item.computer;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenComputerScreenS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * <b>Computador</b> — dispositivo portátil de dados. Botão direito abre um
 * terminal (GUI) onde você cria/lê/edita "arquivos" de texto: anotações,
 * pesquisas, códigos de enigma, dumps do Matter Analyzer, etc.
 *
 * <p>Os dados ficam no NBT do item (persistem com o mundo) e o servidor também
 * exporta um JSON em {@code <mundo>/liberthia_computers/<id>.json} pra não
 * perder dados e permitir edição externa (lore/enigmas).
 */
public class ComputerItem extends Item {

    public ComputerItem(Properties props) {
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

    /** Anexa um arquivo de texto ao computador. Retorna false se cheio. */
    public static boolean appendFile(ItemStack computer, String name, String body) {
        ListTag files = readFiles(computer);
        if (files.size() >= ComputerData.MAX_FILES) return false;
        CompoundTag c = new CompoundTag();
        c.putString("n", name == null ? "" : name);
        c.putString("b", body == null ? "" : body);
        c.putInt("t", ComputerData.TYPE_TEXT);
        files.add(c);
        writeFiles(computer, files);
        return true;
    }

    /** Exporta os arquivos do computador pra JSON no disco do servidor. */
    public static void exportJson(MinecraftServer server, ItemStack computer) {
        if (server == null) return;
        try {
            String id = getOrCreateId(computer);
            Path dir = server.getWorldPath(LevelResource.ROOT).resolve("liberthia_computers");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(id + ".json"),
                    ComputerData.toJson(ComputerData.fromList(readFiles(computer))));
        } catch (Exception ignored) {}
    }

    /** Acha o primeiro Computador no inventário do player (mão ou mochila). */
    public static ItemStack findInInventory(net.minecraft.world.entity.player.Player p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.getItem() instanceof ComputerItem) return s;
        }
        return ItemStack.EMPTY;
    }

    /** ID estável por computador (gera na primeira vez), usado no arquivo JSON. */
    public static String getOrCreateId(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(ComputerData.NBT_ID)) {
            tag.putString(ComputerData.NBT_ID, UUID.randomUUID().toString().substring(0, 8));
        }
        return tag.getString(ComputerData.NBT_ID);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            getOrCreateId(stack); // garante ID
            ModNetwork.sendToPlayer(sp,
                    new OpenComputerScreenS2CPacket(ComputerData.wrap(readFiles(stack))));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        int count = readFiles(stack).size();
        tip.add(Component.literal("§7Arquivos: §b" + count));
        tip.add(Component.translatable("item.liberthia.computador.hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !readFiles(stack).isEmpty();
    }
}
