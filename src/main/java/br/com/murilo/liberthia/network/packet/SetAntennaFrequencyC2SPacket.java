package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.block.entity.DimensionalAntennaBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r27: C2S — client envia nova frequência da antena. Server valida
 * (tipo do BE, distância, ownership não checada por simplicidade) e atualiza.
 */
public class SetAntennaFrequencyC2SPacket {

    private final BlockPos pos;
    private final String frequency;

    public SetAntennaFrequencyC2SPacket(BlockPos pos, String frequency) {
        this.pos = pos;
        this.frequency = frequency == null ? "" : frequency;
    }

    public static void encode(SetAntennaFrequencyC2SPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeUtf(p.frequency, 32);
    }

    public static SetAntennaFrequencyC2SPacket decode(FriendlyByteBuf buf) {
        return new SetAntennaFrequencyC2SPacket(buf.readBlockPos(), buf.readUtf(32));
    }

    public static void handle(SetAntennaFrequencyC2SPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            // r27: Valida distância (anti-cheat — 9 blocos, reach vanilla típico)
            if (sender.distanceToSqr(p.pos.getX() + 0.5, p.pos.getY() + 0.5, p.pos.getZ() + 0.5) > 81) {
                return;
            }
            BlockEntity be = sender.level().getBlockEntity(p.pos);
            if (be instanceof DimensionalAntennaBlockEntity antenna) {
                antenna.setFrequency(p.frequency);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
