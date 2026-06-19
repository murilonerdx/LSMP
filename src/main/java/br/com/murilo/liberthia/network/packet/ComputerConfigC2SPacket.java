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

/** C2S — dono configura login on/off + senha do Computador. */
public class ComputerConfigC2SPacket {

    private final BlockPos pos;
    private final boolean loginEnabled;
    private final String pass;

    public ComputerConfigC2SPacket(BlockPos pos, boolean loginEnabled, String pass) {
        this.pos = pos;
        this.loginEnabled = loginEnabled;
        this.pass = pass == null ? "" : pass;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(loginEnabled);
        buf.writeUtf(pass, 64);
    }

    public static ComputerConfigC2SPacket decode(FriendlyByteBuf buf) {
        return new ComputerConfigC2SPacket(buf.readBlockPos(), buf.readBoolean(), buf.readUtf(64));
    }

    public static void handle(ComputerConfigC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            if (sp.distanceToSqr(msg.pos.getCenter()) > 64) return;
            if (sp.level().getBlockEntity(msg.pos) instanceof ComputerBlockEntity cbe) {
                if (!cbe.isOwner(sp)) {
                    sp.displayClientMessage(Component.literal("§cApenas o dono pode configurar."), true);
                    return;
                }
                cbe.setConfig(msg.loginEnabled, msg.pass);
                // A senha pode ter mudado → invalida sessões: TODOS (inclusive o
                // dono) precisam logar de novo. Sem isto, o dono ficava "auto-logado"
                // e a senha nunca pedia nada ao reabrir.
                cbe.clearAuth();
                boolean locked = cbe.isLocked(sp);
                sp.displayClientMessage(Component.literal("§aConfiguração salva."
                        + (locked ? " §7(login ligado — digite a senha)" : " §7(login desligado)")), true);
                // Reabre já refletindo o lock: se ligou a senha, cai direto na tela de login.
                ModNetwork.sendToPlayer(sp, new OpenComputerBlockS2CPacket(
                        msg.pos, locked, true, cbe.isLoginEnabled(),
                        cbe.getEnergyStored(), cbe.getMaxEnergy(),
                        locked ? new net.minecraft.nbt.CompoundTag() : ComputerData.wrap(cbe.getFiles())));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
