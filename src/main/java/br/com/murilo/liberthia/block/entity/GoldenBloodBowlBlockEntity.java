package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.ritual.BloodRitual;
import br.com.murilo.liberthia.ritual.BloodRitualRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Central ritual block. When activated by a ritual's activator item, it ticks
 * a {@link BloodRitual} to completion or interrupt.
 *
 * <p>One ritual at a time. Persisted via NBT so the ritual survives chunk
 * unload as long as the chunk is loaded again (it freezes when unloaded —
 * vanilla BE behaviour).
 */
public class GoldenBloodBowlBlockEntity extends BlockEntity {

    private static final String NBT_RITUAL_ID = "ActiveRitualId";
    private static final String NBT_TIME = "RitualTime";
    private static final String NBT_NEXT_INGREDIENT = "NextIngredientIdx";
    private static final String NBT_CASTER = "Caster";

    private BloodRitual activeRitual;
    private int time;
    private int nextIngredientIdx;
    private UUID casterUuid;

    public GoldenBloodBowlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOLDEN_BLOOD_BOWL.get(), pos, state);
    }

    public boolean isActive() { return activeRitual != null; }
    public BloodRitual getActiveRitual() { return activeRitual; }
    public int getTime() { return time; }

    /** Tries to start the ritual matched by the given activator item. */
    public boolean tryStart(ServerLevel level, Item activator, ServerPlayer caster) {
        if (isActive()) return false;
        BloodRitual r = BloodRitualRegistry.findValid(level, worldPosition, activator);
        if (r == null) return false;
        activeRitual = r;
        time = 0;
        nextIngredientIdx = 0;
        casterUuid = caster == null ? null : caster.getUUID();
        r.start(level, worldPosition, this, caster);
        sync();
        return true;
    }

    public void interrupt(ServerLevel level, String reason) {
        if (!isActive()) return;
        ServerPlayer caster = resolveCaster(level);
        activeRitual.interrupt(level, worldPosition, this, caster, reason);
        clearActive();
    }

    private ServerPlayer resolveCaster(ServerLevel level) {
        return casterUuid == null ? null : level.getServer().getPlayerList().getPlayer(casterUuid);
    }

    private void clearActive() {
        activeRitual = null;
        time = 0;
        nextIngredientIdx = 0;
        casterUuid = null;
        sync();
    }

    public void serverTick(ServerLevel level) {
        if (!isActive()) return;

        // Re-validate pentacle every 20 ticks; if broken, interrupt.
        if (time % 20 == 0 && !activeRitual.isPentacleValid(level, worldPosition)) {
            interrupt(level, "pentacle quebrado");
            return;
        }

        ServerPlayer caster = resolveCaster(level);
        activeRitual.update(level, worldPosition, this, caster, time);

        // Consume one ingredient at evenly-spaced moments through the duration.
        int total = activeRitual.ingredients().size();
        int interval = Math.max(1, activeRitual.durationTicks() / Math.max(1, total + 1));
        int targetIdx = Math.min(total, time / interval);
        while (nextIngredientIdx < targetIdx && nextIngredientIdx < total) {
            Item want = activeRitual.ingredients().get(nextIngredientIdx);
            if (!activeRitual.consumeOneIngredient(level, worldPosition, want)) {
                interrupt(level, "ingrediente faltando: " + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(want));
                return;
            }
            nextIngredientIdx++;
            setChanged();
        }

        time++;
        if (time >= activeRitual.durationTicks()) {
            activeRitual.finish(level, worldPosition, this, caster);
            clearActive();
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (activeRitual != null) {
            tag.putString(NBT_RITUAL_ID, activeRitual.id());
            tag.putInt(NBT_TIME, time);
            tag.putInt(NBT_NEXT_INGREDIENT, nextIngredientIdx);
            if (casterUuid != null) tag.putUUID(NBT_CASTER, casterUuid);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NBT_RITUAL_ID)) {
            String id = tag.getString(NBT_RITUAL_ID);
            for (BloodRitual r : BloodRitualRegistry.all()) {
                if (r.id().equals(id)) { activeRitual = r; break; }
            }
            time = tag.getInt(NBT_TIME);
            nextIngredientIdx = tag.getInt(NBT_NEXT_INGREDIENT);
            casterUuid = tag.hasUUID(NBT_CASTER) ? tag.getUUID(NBT_CASTER) : null;
        } else {
            activeRitual = null;
            time = 0;
            nextIngredientIdx = 0;
            casterUuid = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        if (pkt.getTag() != null) load(pkt.getTag());
    }
}
