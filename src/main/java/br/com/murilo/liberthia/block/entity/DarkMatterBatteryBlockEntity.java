package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.energy.EnergyNetwork;
import br.com.murilo.liberthia.menu.DarkMatterBatteryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bateria de Matéria Escura — bloco de armazenamento de FE em 3 tiers.
 *
 * <p>Usa {@code int} interno (Forge Energy é int-bound), portanto valores são
 * limitados a {@link Integer#MAX_VALUE} ≈ 2.1 bilhões. Para tiers acima de 1B
 * a capacidade é cap-eada nesse limite.
 *
 * <p>Comportamento:
 * <ul>
 *   <li>Aceita FE de qualquer fonte (canReceive = true)</li>
 *   <li>Empurra ATIVAMENTE FE pela rede de cabos a cada tick — funciona como
 *       fonte autônoma. Se desejar evitar isso, desconecte cabos via right-click.</li>
 *   <li>Persiste energia em NBT.</li>
 * </ul>
 */
public class DarkMatterBatteryBlockEntity extends BlockEntity
        implements MenuProvider, br.com.murilo.liberthia.persistence.Persistable {

    public enum Tier {
        BASIC    (1_000_000,        10_000),       // 1M FE, 10k/tick
        ADVANCED (100_000_000,      100_000),      // 100M FE, 100k/tick
        QUANTUM  (Integer.MAX_VALUE, 1_000_000);   // ~2.1B FE, 1M/tick

        public final int capacity;
        public final int transfer;
        Tier(int cap, int t) { this.capacity = cap; this.transfer = t; }
    }

    public final Tier tier;
    public final br.com.murilo.liberthia.energy.TrackedEnergyStorage energy;
    private LazyOptional<IEnergyStorage> lazy = LazyOptional.empty();

    /** ContainerData pra GUI: 0/1=energy hi/lo, 2/3=max hi/lo, 4=tier ordinal. */
    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            int v = switch (i) {
                case 0, 1 -> energy.getEnergyStored();
                case 2, 3 -> energy.getMaxEnergyStored();
                case 4    -> tier.ordinal();
                default   -> 0;
            };
            return switch (i) {
                case 0, 2 -> (v >> 16) & 0xFFFF;
                case 1, 3 -> v & 0xFFFF;
                default   -> v;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 5; }
    };

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.dm_battery");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new DarkMatterBatteryMenu(id, inv, this, this.data);
    }

    protected DarkMatterBatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Tier tier) {
        super(type, pos, state);
        this.tier = tier;
        this.energy = new br.com.murilo.liberthia.energy.TrackedEnergyStorage(
                this, tier.capacity, tier.transfer, tier.transfer);
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() {
        super.onLoad();
        lazy = LazyOptional.of(() -> energy);
        br.com.murilo.liberthia.persistence.Persistable.LIVE.add(this);
        // Backup-restore se NBT vanilla veio vazio
        if (level instanceof net.minecraft.server.level.ServerLevel sl && isStateEmpty()) {
            CompoundTag snapshot = br.com.murilo.liberthia.persistence.LiberthiaPersistence
                    .get(sl).getSnapshot(sl, worldPosition);
            if (snapshot != null) {
                br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                        "[Liberthia] Restaurando Battery em {} via backup file", worldPosition);
                restoreFromSnapshot(snapshot);
            }
        }
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazy.invalidate(); }
    @Override public void setRemoved() {
        super.setRemoved();
        br.com.murilo.liberthia.persistence.Persistable.LIVE.remove(this);
    }

    @Override public boolean isStateEmpty() { return energy.getEnergyStored() == 0; }
    @Override public void restoreFromSnapshot(CompoundTag tag) { load(tag); setChanged(); }

    public static void tick(Level level, BlockPos pos, BlockState state, DarkMatterBatteryBlockEntity be) {
        if (level.isClientSide) return;

        // PUSH: tenta empurrar pra consumidores que aceitam (canReceive=true).
        // Se laser-only network, sent=0 (laser é pull-based, não aceita push).
        if (be.energy.getEnergyStored() > 0) {
            int budget = Math.min(be.tier.transfer, be.energy.getEnergyStored());
            int sent = EnergyNetwork.pushThroughNetwork(level, pos, budget);
            if (sent > 0) {
                be.energy.extractEnergy(sent, false);
                be.setChanged();
            }
        }

        // GUI sync periódico — INDEPENDENTE de push ter ocorrido.
        if (level.getGameTime() % 10 == 0) {
            level.sendBlockUpdated(pos, state, state, 3);
        }

        // Backup snapshot a cada 600t = 30s
        if (level instanceof net.minecraft.server.level.ServerLevel sl
                && level.getGameTime() % br.com.murilo.liberthia.persistence.PersistenceHandler.SNAPSHOT_PERIOD == 0) {
            br.com.murilo.liberthia.persistence.LiberthiaPersistence.get(sl)
                    .snapshot(sl, pos, be.saveWithFullMetadata());
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        tag.put("energy", energy.serializeNBT());
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("energy")) energy.deserializeNBT(tag.get("energy"));
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
