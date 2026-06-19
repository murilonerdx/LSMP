package br.com.murilo.liberthia.magic.scribe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r164: client → server. Player clicou num recipe do painel esquerdo da
 * Inscription Table. Server atualiza selectedRecipe do BE + re-avalia output.
 */
public class SelectInscriptionRecipeC2SPacket {

    public final BlockPos pos;
    public final int recipeIdx;

    public SelectInscriptionRecipeC2SPacket(BlockPos pos, int recipeIdx) {
        this.pos = pos;
        this.recipeIdx = recipeIdx;
    }

    public static void encode(SelectInscriptionRecipeC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.recipeIdx);
    }

    public static SelectInscriptionRecipeC2SPacket decode(FriendlyByteBuf buf) {
        return new SelectInscriptionRecipeC2SPacket(buf.readBlockPos(), buf.readVarInt());
    }

    public static void handle(SelectInscriptionRecipeC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            if (sp.distanceToSqr(pkt.pos.getCenter()) > 64) return;
            BlockEntity be = sp.level().getBlockEntity(pkt.pos);
            if (be instanceof InscriptionTableBlockEntity itbe) {
                itbe.setSelectedRecipe(pkt.recipeIdx);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
