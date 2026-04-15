package br.com.murilo.liberthia.entry;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record AdminPlayerEntry(
        UUID uuid,
        String name,
        double x,
        double y,
        double z,
        float health,
        float maxHealth,
        int food,
        int armor,
        String dimension,
        String mainHand,
        String offHand
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUtf(name);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeFloat(health);
        buf.writeFloat(maxHealth);
        buf.writeVarInt(food);
        buf.writeVarInt(armor);
        buf.writeUtf(dimension);
        buf.writeUtf(mainHand);
        buf.writeUtf(offHand);
    }

    public static AdminPlayerEntry decode(FriendlyByteBuf buf) {
        return new AdminPlayerEntry(
                buf.readUUID(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf()
        );
    }
}
