package br.com.murilo.liberthia.magic.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r164: client → server. Player digitou um nome customizado no campo "Nome do
 * Feitiço" do Arcane Workbench — server salva no BE pra ser aplicado no output.
 */
public class SetSpellNameC2SPacket {

    public final BlockPos pos;
    public final String name;

    public SetSpellNameC2SPacket(BlockPos pos, String name) {
        this.pos = pos;
        this.name = name == null ? "" : name;
    }

    public static void encode(SetSpellNameC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeUtf(pkt.name, 64);
    }

    public static SetSpellNameC2SPacket decode(FriendlyByteBuf buf) {
        return new SetSpellNameC2SPacket(buf.readBlockPos(), buf.readUtf(64));
    }

    public static void handle(SetSpellNameC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            // Sanity check distance pra prevenir tampering remoto
            if (sp.distanceToSqr(pkt.pos.getCenter()) > 64) return;
            BlockEntity be = sp.level().getBlockEntity(pkt.pos);
            // Sanitize: limita a 40 chars + remove formatação maliciosa
            String clean = pkt.name.replaceAll("§.", "");
            if (clean.length() > 40) clean = clean.substring(0, 40);

            // r164: o packet serve TANTO Arcane Workbench quanto Scribes Table —
            // o BlockEntity sabe lidar com setCustomSpellName.
            if (be instanceof ArcaneWorkbenchBlockEntity awb) {
                awb.setCustomSpellName(clean);
            } else if (be instanceof br.com.murilo.liberthia.block.entity.ScribesTableBlockEntity stb) {
                stb.setCustomSpellName(clean);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
