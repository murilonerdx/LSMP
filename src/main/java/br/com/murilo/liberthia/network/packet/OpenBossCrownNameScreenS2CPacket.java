package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r3: S2C — abre tela {@code BossCrownNameScreen} com nome + estado
 * ativo. Tela unificada permite togglar e renomear no mesmo lugar.
 */
public class OpenBossCrownNameScreenS2CPacket {
    private final String currentName;
    private final boolean active;

    public OpenBossCrownNameScreenS2CPacket(String currentName, boolean active) {
        this.currentName = currentName == null ? "" : currentName;
        this.active = active;
    }

    public String getCurrentName() { return currentName; }
    public boolean isActive() { return active; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(currentName, 64);
        buf.writeBoolean(active);
    }

    public static OpenBossCrownNameScreenS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenBossCrownNameScreenS2CPacket(buf.readUtf(64), buf.readBoolean());
    }

    public static void handle(OpenBossCrownNameScreenS2CPacket msg,
                               Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            LiberthiaMod.LOGGER.info("[BossCrown S2C] open screen — name='{}' active={}",
                    msg.currentName, msg.active);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> br.com.murilo.liberthia.client.gui.BossCrownNameScreen.openNow(msg.currentName, msg.active));
        });
        ctx.get().setPacketHandled(true);
    }
}
