package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.block.entity.QuantumTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r28: C2S — ação do Quantum Terminal (set freq OU send msg).
 *
 * <p>Ação codada em 1 byte:
 * <ul>
 *   <li>0 = SET_FREQ — payload = frequência string</li>
 *   <li>1 = SEND_MSG — payload = mensagem string</li>
 * </ul>
 */
public class TerminalActionC2SPacket {

    public static final byte ACTION_SET_FREQ = 0;
    public static final byte ACTION_SEND_MSG = 1;

    private final BlockPos pos;
    private final byte action;
    private final String payload;

    public TerminalActionC2SPacket(BlockPos pos, byte action, String payload) {
        this.pos = pos;
        this.action = action;
        this.payload = payload == null ? "" : payload;
    }

    public static void encode(TerminalActionC2SPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeByte(p.action);
        buf.writeUtf(p.payload, 256);
    }

    public static TerminalActionC2SPacket decode(FriendlyByteBuf buf) {
        return new TerminalActionC2SPacket(buf.readBlockPos(), buf.readByte(), buf.readUtf(256));
    }

    public static void handle(TerminalActionC2SPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            if (sender.distanceToSqr(p.pos.getX() + 0.5, p.pos.getY() + 0.5, p.pos.getZ() + 0.5) > 81) return;
            BlockEntity be = sender.level().getBlockEntity(p.pos);
            if (!(be instanceof QuantumTerminalBlockEntity term)) return;
            switch (p.action) {
                case ACTION_SET_FREQ -> term.setFrequency(p.payload);
                case ACTION_SEND_MSG -> {
                    if (!p.payload.trim().isEmpty()) {
                        term.sendMessage(sender, p.payload);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
