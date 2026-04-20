package br.com.murilo.liberthia.effect;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Applies HP-drain modifier for Blood Infection.
 *
 * For players, the drain is persisted in a server-level SavedData
 * ({@link BloodInfectionData}) keyed by UUID, so it survives server
 * restart, crash, dimension change, and even player data loss.
 *
 * For non-players, drain is stored only in entity NBT (persistent tag).
 */
public final class BloodInfectionApplier {

    private static final UUID MODIFIER_UUID = UUID.fromString("e5a2c3f1-7b4d-4a9e-8f1c-2a3b4c5d6e7f");

    private BloodInfectionApplier() {}

    /** Apply drain value, updating modifier and (for players) SavedData. */
    public static void apply(LivingEntity entity, double drain) {
        AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        AttributeModifier existing = attr.getModifier(MODIFIER_UUID);
        if (existing != null) attr.removeModifier(existing);
        if (drain > 0) {
            attr.addPermanentModifier(new AttributeModifier(
                    MODIFIER_UUID,
                    "liberthia_blood_infection_drain",
                    -drain,
                    AttributeModifier.Operation.ADDITION
            ));
            if (entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
        }
        // Persist
        entity.getPersistentData().putDouble(BloodInfectionEffect.NBT_DRAIN, Math.max(0D, drain));
        if (entity instanceof Player p && !p.level().isClientSide) {
            MinecraftServer server = p.getServer();
            if (server != null) {
                BloodInfectionData.get(server).setDrain(p.getUUID(), drain);
            }
        }
    }

    /** Fully cure: remove modifier, zero NBT, remove from SavedData. */
    public static void clear(LivingEntity entity) {
        AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            AttributeModifier existing = attr.getModifier(MODIFIER_UUID);
            if (existing != null) attr.removeModifier(existing);
        }
        entity.getPersistentData().putDouble(BloodInfectionEffect.NBT_DRAIN, 0D);
        if (entity instanceof Player p && !p.level().isClientSide) {
            MinecraftServer server = p.getServer();
            if (server != null) {
                BloodInfectionData.get(server).clear(p.getUUID());
            }
        }
    }

    /** Current drain — prefers SavedData for players. */
    public static double getDrain(LivingEntity entity) {
        if (entity instanceof Player p && !p.level().isClientSide) {
            MinecraftServer server = p.getServer();
            if (server != null) {
                double saved = BloodInfectionData.get(server).getDrain(p.getUUID());
                if (saved > 0) return saved;
            }
        }
        return entity.getPersistentData().getDouble(BloodInfectionEffect.NBT_DRAIN);
    }

    /** Restore drain modifier on login from SavedData. */
    public static void restore(LivingEntity entity) {
        double saved = getDrain(entity);
        if (saved > 0) {
            // Re-apply without re-writing to SavedData (it's already there).
            AttributeInstance attr = entity.getAttribute(Attributes.MAX_HEALTH);
            if (attr == null) return;
            AttributeModifier existing = attr.getModifier(MODIFIER_UUID);
            if (existing != null) attr.removeModifier(existing);
            attr.addPermanentModifier(new AttributeModifier(
                    MODIFIER_UUID,
                    "liberthia_blood_infection_drain",
                    -saved,
                    AttributeModifier.Operation.ADDITION
            ));
            if (entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
            entity.getPersistentData().putDouble(BloodInfectionEffect.NBT_DRAIN, saved);
        }
    }
}
