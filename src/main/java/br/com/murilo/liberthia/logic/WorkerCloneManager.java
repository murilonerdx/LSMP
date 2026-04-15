package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entity.ClonePlayerEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorkerCloneManager {

    public enum State { IDLE, RECORDING, REPLAYING }

    private static final int MAX_RECORD_TICKS = 20 * 60 * 5; // 5 min cap
    private static final Map<UUID, CloneRecord> CLONES = new ConcurrentHashMap<>();

    private WorkerCloneManager() {}

    public static boolean isActive(UUID ownerId) {
        CloneRecord r = CLONES.get(ownerId);
        return r != null && r.entity != null && r.entity.isAlive();
    }

    public static State getState(UUID ownerId) {
        CloneRecord r = CLONES.get(ownerId);
        return r == null ? null : r.state;
    }

    public static void spawn(ServerPlayer owner) {
        removeFor(owner.getUUID());

        ServerLevel level = owner.serverLevel();
        ClonePlayerEntity clone = ModEntities.CLONE_PLAYER.get().create(level);
        if (clone == null) return;

        clone.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        clone.setOwnerUuid(owner.getUUID());
        clone.setOwnerName(owner.getName().getString());
        clone.setCustomNameVisible(false);

        clone.setItemSlot(EquipmentSlot.MAINHAND, owner.getMainHandItem().copy());
        clone.setItemSlot(EquipmentSlot.OFFHAND, owner.getOffhandItem().copy());
        clone.setItemSlot(EquipmentSlot.HEAD, owner.getItemBySlot(EquipmentSlot.HEAD).copy());
        clone.setItemSlot(EquipmentSlot.CHEST, owner.getItemBySlot(EquipmentSlot.CHEST).copy());
        clone.setItemSlot(EquipmentSlot.LEGS, owner.getItemBySlot(EquipmentSlot.LEGS).copy());
        clone.setItemSlot(EquipmentSlot.FEET, owner.getItemBySlot(EquipmentSlot.FEET).copy());

        level.addFreshEntity(clone);

        CLONES.put(owner.getUUID(), new CloneRecord(clone));
    }

    public static void startRecording(UUID ownerId) {
        CloneRecord r = CLONES.get(ownerId);
        if (r == null) return;
        r.state = State.RECORDING;
        r.recorded.clear();
        r.replayIdx = 0;
    }

    public static void startReplay(UUID ownerId) {
        CloneRecord r = CLONES.get(ownerId);
        if (r == null || r.recorded.isEmpty()) return;
        r.state = State.REPLAYING;
        r.replayIdx = 0;
    }

    public static void removeFor(UUID ownerId) {
        CloneRecord existing = CLONES.remove(ownerId);
        if (existing != null && existing.entity != null && existing.entity.isAlive()) {
            existing.entity.discard();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (CLONES.isEmpty()) return;

        Iterator<Map.Entry<UUID, CloneRecord>> it = CLONES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, CloneRecord> entry = it.next();
            CloneRecord c = entry.getValue();

            if (c.entity == null || !c.entity.isAlive()) {
                it.remove();
                continue;
            }

            c.entity.setDeltaMovement(0, 0, 0);

            if (c.state == State.RECORDING) {
                ServerPlayer owner = findOwner(c, entry.getKey());
                if (owner == null) continue;
                if (c.recorded.size() < MAX_RECORD_TICKS) {
                    c.recorded.add(new PoseData(
                            owner.getX(), owner.getY(), owner.getZ(),
                            owner.getYRot(), owner.getXRot(),
                            owner.isCrouching(),
                            owner.swinging
                    ));
                }
            } else if (c.state == State.REPLAYING) {
                if (c.replayIdx >= c.recorded.size()) {
                    c.state = State.IDLE;
                    continue;
                }
                PoseData pose = c.recorded.get(c.replayIdx++);
                c.entity.setPos(pose.x(), pose.y(), pose.z());
                c.entity.setYRot(pose.yaw());
                c.entity.setXRot(pose.pitch());
                c.entity.setYBodyRot(pose.yaw());
                c.entity.setYHeadRot(pose.yaw());
                c.entity.setPose(pose.crouching() ? Pose.CROUCHING : Pose.STANDING);
                if (pose.swinging()) {
                    c.entity.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
        }
    }

    private static ServerPlayer findOwner(CloneRecord c, UUID id) {
        if (c.entity.level().getServer() == null) return null;
        for (ServerLevel lvl : c.entity.level().getServer().getAllLevels()) {
            Player p = lvl.getPlayerByUUID(id);
            if (p instanceof ServerPlayer sp) return sp;
        }
        return null;
    }

    private static final class CloneRecord {
        final ClonePlayerEntity entity;
        State state = State.IDLE;
        final List<PoseData> recorded = new ArrayList<>();
        int replayIdx = 0;

        CloneRecord(ClonePlayerEntity entity) {
            this.entity = entity;
        }
    }

    private record PoseData(double x, double y, double z, float yaw, float pitch, boolean crouching, boolean swinging) {}
}
