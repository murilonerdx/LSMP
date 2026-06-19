package br.com.murilo.liberthia.observation.source;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v0.1.22 r62: <b>Source Jar Tile</b> — armazena 10000 Source max.
 *
 * <h2>Pattern AN's SourceJarTile</h2>
 * <ul>
 *   <li>Capacity: 10000</li>
 *   <li>maxReceive: 1000/tick (= 1000)</li>
 *   <li>maxExtract: 1000/tick</li>
 *   <li>{@code onContentsChanged()} atualiza blockstate FILL (0-11)</li>
 * </ul>
 */
public class SourceJarTile extends BlockEntity {

    public static final int CAPACITY = 10000;
    private int stored = 0;

    public SourceJarTile(BlockPos pos, BlockState state) {
        super(br.com.murilo.liberthia.registry.ModBlockEntities.SOURCE_JAR.get(), pos, state);
    }

    public int getStored() { return stored; }
    public int getCapacity() { return CAPACITY; }
    public int getRoom() { return Math.max(0, CAPACITY - stored); }

    /** Tenta receber. Returns true se aceitou. */
    public boolean tryReceive(int amount) {
        if (stored + amount > CAPACITY) return false;
        stored += amount;
        onContentsChanged();
        return true;
    }

    /** Tenta extract. Returns true se conseguiu. */
    public boolean tryExtract(int amount) {
        if (stored < amount) return false;
        stored -= amount;
        onContentsChanged();
        return true;
    }

    /** Simulated transaction — não modifica. */
    public int receiveSim(int amount) {
        return Math.min(amount, getRoom());
    }

    public int extractSim(int amount) {
        return Math.min(amount, stored);
    }

    private void onContentsChanged() {
        // Atualiza blockstate FILL (0-11 baseado em stored)
        if (level != null) {
            BlockState state = getBlockState();
            int newFill = Math.min(11, stored / (CAPACITY / 11));
            if (state.getValue(SourceJarBlock.FILL) != newFill) {
                level.setBlock(worldPosition, state.setValue(SourceJarBlock.FILL, newFill), 3);
            }
            setChanged();
        }
    }

    /** Tick — tenta auto-extract Source pro player próximo se source baixa. */
    public void serverTick() {
        if (level == null || level.isClientSide) return;
        if (level.getGameTime() % 40 != 0) return; // 0.5Hz

        // Auto-recarga subtle: se há player próximo (< 4b) com source baixa,
        // transfer 1 Source/segundo (sem precisar rclick). Cosmic ambient.
        for (var player : level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                new net.minecraft.world.phys.AABB(worldPosition).inflate(4))) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                int cur = SourceData.get(sp);
                int max = SourceData.getMax(sp);
                if (cur < max && stored > 0) {
                    int amt = Math.min(1, Math.min(max - cur, stored));
                    if (amt > 0 && tryExtract(amt)) {
                        SourceData.add(sp, amt);
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("stored", stored);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stored = tag.getInt("stored");
    }
}
