package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.item.RiftCutterItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r185 — C2S: o jogador confirmou X/Y/Z + dimensão na GUI da Adaga Corta-Fendas. O servidor
 * valida a dimensão (deve existir) e os limites, então grava no NBT da adaga em mão.
 */
public class SaveRiftCutterCoordsC2SPacket {
    private final int x, y, z;
    private final String dim;

    public SaveRiftCutterCoordsC2SPacket(int x, int y, int z, String dim) {
        this.x = x; this.y = y; this.z = z; this.dim = dim;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z); buf.writeUtf(dim);
    }

    public static SaveRiftCutterCoordsC2SPacket decode(FriendlyByteBuf buf) {
        return new SaveRiftCutterCoordsC2SPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf());
    }

    public static void handle(SaveRiftCutterCoordsC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            // valida limites do mundo
            int cx = Math.max(-30_000_000, Math.min(30_000_000, msg.x));
            int cz = Math.max(-30_000_000, Math.min(30_000_000, msg.z));
            int cy = Math.max(-256, Math.min(2048, msg.y));
            // valida que a dimensão existe
            ResourceLocation dimLoc;
            try { dimLoc = new ResourceLocation(msg.dim); } catch (Exception e) {
                sp.displayClientMessage(Component.literal("§cDimensão inválida."), true); return;
            }
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimLoc);
            boolean exists = false;
            for (ResourceKey<Level> k : sp.server.levelKeys()) if (k.equals(key)) { exists = true; break; }
            if (!exists) { sp.displayClientMessage(Component.literal("§cEssa dimensão não existe no servidor."), true); return; }

            ItemStack stack = sp.getMainHandItem();
            if (!(stack.getItem() instanceof RiftCutterItem)) stack = sp.getOffhandItem();
            if (!(stack.getItem() instanceof RiftCutterItem rc)) {
                sp.displayClientMessage(Component.literal("§cSegure a Adaga Corta-Fendas."), true); return;
            }
            // valida que o NÍVEL da adaga alcança a dimensão escolhida (anti-cheat)
            if (!rc.riftTier().canTeleportTo(key)) {
                sp.displayClientMessage(Component.literal("§cEste nível da adaga não alcança essa dimensão."), true); return;
            }
            var tag = stack.getOrCreateTag();
            tag.putInt(RiftCutterItem.KEY_X, cx);
            tag.putInt(RiftCutterItem.KEY_Y, cy);
            tag.putInt(RiftCutterItem.KEY_Z, cz);
            tag.putString(RiftCutterItem.KEY_DIM, msg.dim);
            sp.displayClientMessage(Component.literal("§aAlvo salvo: §f" + cx + ", " + cy + ", " + cz + " §7(" + msg.dim + ")"), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
