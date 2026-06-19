package br.com.murilo.liberthia.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C — abre a tela do Computador (bloco). locked → tela de senha. */
public class OpenComputerBlockS2CPacket {

    private final BlockPos pos;
    private final boolean locked;
    private final boolean owner;
    private final boolean loginEnabled;
    private final int energy;
    private final int maxEnergy;
    private final CompoundTag data;

    public OpenComputerBlockS2CPacket(BlockPos pos, boolean locked, boolean owner, boolean loginEnabled,
                                      int energy, int maxEnergy, CompoundTag data) {
        this.pos = pos;
        this.locked = locked;
        this.owner = owner;
        this.loginEnabled = loginEnabled;
        this.energy = energy;
        this.maxEnergy = maxEnergy;
        this.data = data == null ? new CompoundTag() : data;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(locked);
        buf.writeBoolean(owner);
        buf.writeBoolean(loginEnabled);
        buf.writeVarInt(energy);
        buf.writeVarInt(maxEnergy);
        buf.writeNbt(data);
    }

    public static OpenComputerBlockS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenComputerBlockS2CPacket(buf.readBlockPos(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readNbt());
    }

    public static void handle(OpenComputerBlockS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        br.com.murilo.liberthia.client.gui.ComputerBlockScreen.open(
                                msg.pos, msg.locked, msg.owner, msg.loginEnabled,
                                msg.energy, msg.maxEnergy, msg.data)));
        ctx.get().setPacketHandled(true);
    }
}
