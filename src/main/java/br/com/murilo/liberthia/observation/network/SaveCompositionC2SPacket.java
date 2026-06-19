package br.com.murilo.liberthia.observation.network;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.item.GrimoireOfObservationItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r65: C2S — cliente envia o preset escolhido na Composition Screen
 * pra ser persistido no Grimório segurado.
 *
 * <p>Pattern AN's PacketUpdateCaster.
 */
public class SaveCompositionC2SPacket {

    private final int preset;

    public SaveCompositionC2SPacket(int preset) {
        this.preset = preset;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(preset);
    }

    public static SaveCompositionC2SPacket decode(FriendlyByteBuf buf) {
        return new SaveCompositionC2SPacket(buf.readInt());
    }

    public static void handle(SaveCompositionC2SPacket msg,
                               Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            // Acha o Grimório na mão (main ou off)
            ItemStack stack = sp.getMainHandItem();
            if (!(stack.getItem() instanceof GrimoireOfObservationItem)) {
                stack = sp.getOffhandItem();
            }
            if (!(stack.getItem() instanceof GrimoireOfObservationItem)) {
                LiberthiaMod.LOGGER.warn("[SaveComposition] {} sem Grimório na mão", sp.getName().getString());
                return;
            }
            int clamped = Math.max(0, Math.min(5, msg.preset));
            stack.getOrCreateTag().putInt(GrimoireOfObservationItem.NBT_PRESET, clamped);
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§5§l✦ §rPreset salvo no Grimório: §e" + clamped), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
