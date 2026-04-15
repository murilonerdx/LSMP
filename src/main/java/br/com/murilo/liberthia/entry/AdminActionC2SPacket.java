package br.com.murilo.liberthia.entry;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class AdminActionC2SPacket {

    public enum Action {
        REQUEST_PLAYERS,
        REQUEST_INVENTORY,
        SEND_POSITION,
        APPLY_EFFECT,
        CLEAR_EFFECT,
        GIVE_ITEM,
        REMOVE_ITEM,
        SCARE_SINGLE,
        SCARE_AREA,
        SUMMON_MONSTER
    }

    private final Action action;
    private final UUID targetId;
    private final String effectId;
    private final int durationSeconds;
    private final int amplifier;
    private final String itemId;
    private final int itemCount;

    public AdminActionC2SPacket(
            Action action,
            UUID targetId,
            String effectId,
            int durationSeconds,
            int amplifier,
            String itemId,
            int itemCount
    ) {
        this.action = action;
        this.targetId = targetId;
        this.effectId = effectId == null ? "" : effectId;
        this.durationSeconds = durationSeconds;
        this.amplifier = amplifier;
        this.itemId = itemId == null ? "" : itemId;
        this.itemCount = itemCount;
    }

    public Action getAction() {
        return action;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getEffectId() {
        return effectId;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public String getItemId() {
        return itemId;
    }

    public int getItemCount() {
        return itemCount;
    }

    public static AdminActionC2SPacket requestPlayers() {
        return new AdminActionC2SPacket(Action.REQUEST_PLAYERS, null, "", 0, 0, "", 0);
    }

    public static AdminActionC2SPacket requestInventory(UUID targetId) {
        return new AdminActionC2SPacket(Action.REQUEST_INVENTORY, targetId, "", 0, 0, "", 0);
    }

    public static AdminActionC2SPacket sendPosition(UUID targetId) {
        return new AdminActionC2SPacket(Action.SEND_POSITION, targetId, "", 0, 0, "", 0);
    }

    public static AdminActionC2SPacket applyEffect(UUID targetId, String effectId, int durationSeconds, int amplifier) {
        return new AdminActionC2SPacket(Action.APPLY_EFFECT, targetId, effectId, durationSeconds, amplifier, "", 0);
    }

    public static AdminActionC2SPacket clearEffect(UUID targetId, String effectId) {
        return new AdminActionC2SPacket(Action.CLEAR_EFFECT, targetId, effectId, 0, 0, "", 0);
    }

    public static AdminActionC2SPacket giveItem(UUID targetId, String itemId, int count) {
        return new AdminActionC2SPacket(Action.GIVE_ITEM, targetId, "", 0, 0, itemId, count);
    }

    public static AdminActionC2SPacket removeItem(UUID targetId, String itemId, int count) {
        return new AdminActionC2SPacket(Action.REMOVE_ITEM, targetId, "", 0, 0, itemId, count);
    }

    public static AdminActionC2SPacket scareSingle(UUID targetId) {
        return new AdminActionC2SPacket(Action.SCARE_SINGLE, targetId, "", 0, 0, "", 0);
    }

    public static AdminActionC2SPacket scareArea(UUID targetId) {
        return new AdminActionC2SPacket(Action.SCARE_AREA, targetId, "", 0, 0, "", 0);
    }

    public static AdminActionC2SPacket summonMonster(UUID targetId, String entityId, int count) {
        return new AdminActionC2SPacket(Action.SUMMON_MONSTER, targetId, "", 0, 0, entityId, count);
    }

    public static void encode(AdminActionC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);

        buf.writeBoolean(msg.targetId != null);
        if (msg.targetId != null) {
            buf.writeUUID(msg.targetId);
        }

        buf.writeUtf(msg.effectId);
        buf.writeVarInt(msg.durationSeconds);
        buf.writeVarInt(msg.amplifier);
        buf.writeUtf(msg.itemId);
        buf.writeVarInt(msg.itemCount);
    }

    public static AdminActionC2SPacket decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);

        UUID targetId = null;
        if (buf.readBoolean()) {
            targetId = buf.readUUID();
        }

        String effectId = buf.readUtf();
        int durationSeconds = buf.readVarInt();
        int amplifier = buf.readVarInt();
        String itemId = buf.readUtf();
        int itemCount = buf.readVarInt();

        return new AdminActionC2SPacket(action, targetId, effectId, durationSeconds, amplifier, itemId, itemCount);
    }

    public static void handle(AdminActionC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) {
                AdminActions.execute(sender, msg);
            }
        });

        ctx.setPacketHandled(true);
    }
}
