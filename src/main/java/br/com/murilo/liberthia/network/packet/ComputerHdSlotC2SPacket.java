package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.item.HardDriveItem;
import br.com.murilo.liberthia.item.computer.ComputerData;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.storage.ComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S — insere (da mão) ou ejeta o HD do slot do Computador. */
public class ComputerHdSlotC2SPacket {

    private final BlockPos pos;
    private final boolean eject;

    public ComputerHdSlotC2SPacket(BlockPos pos, boolean eject) {
        this.pos = pos;
        this.eject = eject;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(eject);
    }

    public static ComputerHdSlotC2SPacket decode(FriendlyByteBuf buf) {
        return new ComputerHdSlotC2SPacket(buf.readBlockPos(), buf.readBoolean());
    }

    public static void handle(ComputerHdSlotC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            if (sp.distanceToSqr(msg.pos.getCenter()) > 64) return;
            if (!(sp.level().getBlockEntity(msg.pos) instanceof ComputerBlockEntity cbe)) return;
            if (cbe.isLocked(sp)) {
                sp.displayClientMessage(Component.literal("§cAcesso negado."), true);
                return;
            }

            if (msg.eject) {
                ItemStack hd = cbe.getHd();
                if (hd.isEmpty()) return;
                cbe.setHd(ItemStack.EMPTY);
                if (!sp.getInventory().add(hd)) sp.drop(hd, false);
                sp.displayClientMessage(Component.literal("§7HD removido."), true);
            } else {
                if (!cbe.getHd().isEmpty()) {
                    sp.displayClientMessage(Component.literal("§eJá tem um HD no slot."), true);
                    return;
                }
                ItemStack hand = sp.getMainHandItem();
                if (!(hand.getItem() instanceof HardDriveItem)) {
                    sp.displayClientMessage(Component.literal("§cSegure um HD na mão pra inserir."), true);
                    return;
                }
                ItemStack one = hand.copy();
                one.setCount(1);
                cbe.setHd(one);
                hand.shrink(1);
                sp.displayClientMessage(Component.literal("§aHD inserido."), true);
            }

            // Reabre a tela já refletindo o HD presente/ausente — os relatórios
            // aparecem ao inserir e somem ao retirar (o HD É o armazenamento).
            ModNetwork.sendToPlayer(sp, new OpenComputerBlockS2CPacket(
                    msg.pos, false, cbe.isOwner(sp), cbe.isLoginEnabled(),
                    cbe.getEnergyStored(), cbe.getMaxEnergy(),
                    ComputerData.wrap(cbe.getFiles())));
        });
        ctx.get().setPacketHandled(true);
    }
}
