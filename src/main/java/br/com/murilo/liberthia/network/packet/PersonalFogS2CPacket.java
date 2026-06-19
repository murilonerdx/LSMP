package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → UM player: liga/desliga a névoa PESSOAL (só esse player vê, e ela
 * segue ele pra onde for). Como um efeito de poção de névoa.
 */
public class PersonalFogS2CPacket {

    public final boolean active;
    public final int color;
    public final float density;

    public PersonalFogS2CPacket(boolean active, int color, float density) {
        this.active = active;
        this.color = color;
        this.density = density;
    }

    public static void encode(PersonalFogS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.active);
        buf.writeInt(p.color);
        buf.writeFloat(p.density);
    }

    public static PersonalFogS2CPacket decode(FriendlyByteBuf buf) {
        return new PersonalFogS2CPacket(buf.readBoolean(), buf.readInt(), buf.readFloat());
    }

    public static void handle(PersonalFogS2CPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        br.com.murilo.liberthia.client.fog.ClientPersonalFog.set(p.active, p.color, p.density)));
        ctx.get().setPacketHandled(true);
    }
}
