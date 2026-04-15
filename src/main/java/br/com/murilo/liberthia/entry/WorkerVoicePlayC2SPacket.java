package br.com.murilo.liberthia.entry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

public class WorkerVoicePlayC2SPacket {

    private final UUID targetId; // null = self
    private final String soundId;
    private final float volume;
    private final float pitch;

    public WorkerVoicePlayC2SPacket(UUID targetId, String soundId, float volume, float pitch) {
        this.targetId = targetId;
        this.soundId = soundId == null ? "" : soundId;
        this.volume = volume;
        this.pitch = pitch;
    }

    public static void encode(WorkerVoicePlayC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.targetId != null);
        if (msg.targetId != null) buf.writeUUID(msg.targetId);
        buf.writeUtf(msg.soundId);
        buf.writeFloat(msg.volume);
        buf.writeFloat(msg.pitch);
    }

    public static WorkerVoicePlayC2SPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readBoolean() ? buf.readUUID() : null;
        return new WorkerVoicePlayC2SPacket(id, buf.readUtf(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(WorkerVoicePlayC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            ResourceLocation id = ResourceLocation.tryParse(msg.soundId);
            if (id == null) return;
            SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(id);
            if (ev == null) return;

            ServerPlayer target = msg.targetId != null && sender.server != null
                    ? sender.server.getPlayerList().getPlayer(msg.targetId)
                    : sender;
            if (target == null) target = sender;

            ServerLevel lvl = target.serverLevel();
            float v = Math.max(0.1F, Math.min(8.0F, msg.volume));
            float p = Math.max(0.5F, Math.min(2.0F, msg.pitch));
            lvl.playSound(null, target.blockPosition(), ev, SoundSource.HOSTILE, v, p);
        });
        ctx.setPacketHandled(true);
    }
}
