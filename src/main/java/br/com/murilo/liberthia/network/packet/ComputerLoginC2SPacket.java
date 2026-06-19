package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.item.computer.ComputerData;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.storage.ComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S — tentativa de login no Computador (bloco). */
public class ComputerLoginC2SPacket {

    private final BlockPos pos;
    private final String pass;

    public ComputerLoginC2SPacket(BlockPos pos, String pass) {
        this.pos = pos;
        this.pass = pass == null ? "" : pass;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(pass, 64);
    }

    public static ComputerLoginC2SPacket decode(FriendlyByteBuf buf) {
        return new ComputerLoginC2SPacket(buf.readBlockPos(), buf.readUtf(64));
    }

    public static void handle(ComputerLoginC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            if (sp.distanceToSqr(msg.pos.getCenter()) > 64) return;
            if (sp.level().getBlockEntity(msg.pos) instanceof ComputerBlockEntity cbe) {
                if (cbe.tryLogin(sp, msg.pass)) {
                    ModNetwork.sendToPlayer(sp, new OpenComputerBlockS2CPacket(
                            msg.pos, false, cbe.isOwner(sp), cbe.isLoginEnabled(),
                            cbe.getEnergyStored(), cbe.getMaxEnergy(), ComputerData.wrap(cbe.getFiles())));
                } else {
                    sp.displayClientMessage(Component.literal("§cSenha incorreta."), true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
