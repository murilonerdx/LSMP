package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.computer.ComputerData;
import br.com.murilo.liberthia.item.computer.ComputerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.network.NetworkEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/** C2S — salva os arquivos do Computador no item (NBT) + exporta JSON no disco. */
public class SaveComputerC2SPacket {

    private final CompoundTag data;

    public SaveComputerC2SPacket(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public static SaveComputerC2SPacket decode(FriendlyByteBuf buf) {
        return new SaveComputerC2SPacket(buf.readNbt());
    }

    public static void handle(SaveComputerC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;

            ItemStack held = sp.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(held.getItem() instanceof ComputerItem)) {
                held = sp.getItemInHand(InteractionHand.OFF_HAND);
            }
            if (!(held.getItem() instanceof ComputerItem)) return;

            ListTag files = ComputerData.unwrap(msg.data);
            ComputerItem.writeFiles(held, files);

            // Export JSON pro disco do servidor (não perder dados / lore / enigmas).
            try {
                String id = ComputerItem.getOrCreateId(held);
                MinecraftServer server = sp.getServer();
                if (server != null) {
                    Path dir = server.getWorldPath(LevelResource.ROOT).resolve("liberthia_computers");
                    Files.createDirectories(dir);
                    Files.writeString(dir.resolve(id + ".json"),
                            ComputerData.toJson(ComputerData.fromList(files)));
                }
            } catch (Exception e) {
                LiberthiaMod.LOGGER.debug("[Computador] export JSON falhou: {}", e.toString());
            }

            sp.displayClientMessage(Component.literal("§aComputador salvo §7(" + files.size() + " arquivos)")
                    , true);
        });
        ctx.get().setPacketHandled(true);
    }
}
