package br.com.murilo.liberthia.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S2C: opens the Command Tablet editor on the client, prefilled with the
 * current commands, label, target settings and verbose flag stored on the
 * held tablet.
 */
public class OpenCommandTabletScreenS2CPacket {
    private final List<String> commands;
    private final String label;
    private final String targetMode;
    private final String targetName;
    private final boolean verbose;

    public OpenCommandTabletScreenS2CPacket(List<String> commands, String label,
                                            String targetMode, String targetName,
                                            boolean verbose) {
        this.commands = commands;
        this.label = label == null ? "" : label;
        this.targetMode = targetMode == null ? "self" : targetMode;
        this.targetName = targetName == null ? "" : targetName;
        this.verbose = verbose;
    }

    public List<String> getCommands() { return commands; }
    public String getLabel() { return label; }
    public String getTargetMode() { return targetMode; }
    public String getTargetName() { return targetName; }
    public boolean isVerbose() { return verbose; }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(commands.size());
        for (String c : commands) buf.writeUtf(c, 256);
        buf.writeUtf(label, 64);
        buf.writeUtf(targetMode, 8);
        buf.writeUtf(targetName, 32);
        buf.writeBoolean(verbose);
    }

    public static OpenCommandTabletScreenS2CPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(buf.readUtf(256));
        return new OpenCommandTabletScreenS2CPacket(list,
                buf.readUtf(64), buf.readUtf(8), buf.readUtf(32),
                buf.readBoolean());
    }

    public static void handle(OpenCommandTabletScreenS2CPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> br.com.murilo.liberthia.client.ClientCommandTabletDispatch.open(msg)));
        ctx.get().setPacketHandled(true);
    }
}
