package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.item.HardDriveItem;
import br.com.murilo.liberthia.item.computer.ComputerData;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.storage.ComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S — grava (computer→HD) ou lê (HD→computer) dados, usando o HD na mão. */
public class ComputerHdC2SPacket {

    private final BlockPos pos;
    private final boolean write;

    public ComputerHdC2SPacket(BlockPos pos, boolean write) {
        this.pos = pos;
        this.write = write;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(write);
    }

    public static ComputerHdC2SPacket decode(FriendlyByteBuf buf) {
        return new ComputerHdC2SPacket(buf.readBlockPos(), buf.readBoolean());
    }

    public static void handle(ComputerHdC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            if (sp.distanceToSqr(msg.pos.getCenter()) > 64) return;
            if (!(sp.level().getBlockEntity(msg.pos) instanceof ComputerBlockEntity cbe)) return;
            // Obsoleto: o HD agora É o próprio armazenamento (os relatórios já
            // ficam gravados nele). Não há mais "gravar/ler" — basta inserir ou
            // retirar o HD do slot. Mantido só pra compatibilidade de rede.
            sp.displayClientMessage(Component.literal(
                    "§7O HD agora é o próprio armazenamento — os relatórios já ficam nele."), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
