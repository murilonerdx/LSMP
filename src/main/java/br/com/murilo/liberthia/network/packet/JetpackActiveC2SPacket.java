package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.item.tech.JetpackItem;
import br.com.murilo.liberthia.item.tech.TechEnergy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r180c — C2S: o cliente avisa que o jetpack está ATIVO neste tick (segurando pular).
 * O servidor (autoritativo do NBT) drena o FE do peitoral. Sem payload. Handler é
 * 100% server-side (nenhuma classe de cliente referida) → seguro p/ servidor dedicado.
 */
public class JetpackActiveC2SPacket {
    public JetpackActiveC2SPacket() {}

    public static void encode(JetpackActiveC2SPacket msg, FriendlyByteBuf buf) {}
    public static JetpackActiveC2SPacket decode(FriendlyByteBuf buf) { return new JetpackActiveC2SPacket(); }

    public static void handle(JetpackActiveC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            ItemStack chest = sender.getItemBySlot(EquipmentSlot.CHEST);
            if (chest.getItem() instanceof JetpackItem) TechEnergy.drain(chest, JetpackItem.COST_PER_TICK, JetpackItem.CAP);
        });
        ctx.get().setPacketHandled(true);
    }
}
