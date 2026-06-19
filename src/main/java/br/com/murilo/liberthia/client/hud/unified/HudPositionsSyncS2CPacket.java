package br.com.murilo.liberthia.client.hud.unified;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r164: server → client com todas as posições de HUD do player. Enviado:
 * <ul>
 *   <li>No login (PlayerLoggedInEvent)</li>
 *   <li>Após {@code /liberthia hud reset} no server</li>
 *   <li>Após drag no editor (server confirma a posição salva)</li>
 * </ul>
 */
public class HudPositionsSyncS2CPacket {

    private final CompoundTag positions;

    public HudPositionsSyncS2CPacket(CompoundTag positions) {
        this.positions = positions;
    }

    public static HudPositionsSyncS2CPacket forPlayer(ServerPlayer sp) {
        return new HudPositionsSyncS2CPacket(HudPositionsData.snapshot(sp));
    }

    public static void encode(HudPositionsSyncS2CPacket pkt, FriendlyByteBuf buf) {
        buf.writeNbt(pkt.positions);
    }

    public static HudPositionsSyncS2CPacket decode(FriendlyByteBuf buf) {
        return new HudPositionsSyncS2CPacket(buf.readNbt());
    }

    public static void handle(HudPositionsSyncS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.apply(pkt)));
        ctx.get().setPacketHandled(true);
    }

    /** Client-only — classe SEPARADA (server-safe). */
    private static final class Client {
        static void apply(HudPositionsSyncS2CPacket pkt) {
            if (Minecraft.getInstance().player != null) {
                ClientHudPositions.load(pkt.positions);
            }
        }
    }
}
