package br.com.murilo.liberthia.cutscene;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * r190 — S2C: dispara/para/reinicia a cutscene/vídeo no cliente. Carrega só dados (ação + url + modo).
 */
public class CutsceneS2CPacket {
    public static final byte MODE_BROWSER   = 0x01; // abre o link no navegador real
    public static final byte MODE_CINEMATIC = 0x02; // overlay cinematográfico no jogo
    public static final byte MODE_LOCK_ESC  = 0x04; // trava ESC nos primeiros 3s

    public final CutsceneAction action;
    public final String url;
    public final byte mode;

    public CutsceneS2CPacket(CutsceneAction action, String url, byte mode) {
        this.action = action; this.url = url == null ? "" : url; this.mode = mode;
    }

    public static void encode(CutsceneS2CPacket p, FriendlyByteBuf buf) {
        buf.writeByte(p.action.ordinal());
        buf.writeUtf(p.url, 1024);
        buf.writeByte(p.mode);
    }

    public static CutsceneS2CPacket decode(FriendlyByteBuf buf) {
        CutsceneAction action = CutsceneAction.values()[buf.readByte() & 0xFF];
        String url = buf.readUtf(1024);
        byte mode = buf.readByte();
        return new CutsceneS2CPacket(action, url, mode);
    }

    public static void handle(CutsceneS2CPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> br.com.murilo.liberthia.cutscene.client.CutsceneManager.receive(pkt)));
        ctx.get().setPacketHandled(true);
    }
}
