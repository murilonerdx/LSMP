package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wireless Charger — bloco que distribui FE pros itens carregados por
 * jogadores num raio de {@link #RANGE} blocos.
 *
 * <p>Aceita FE via cabo Liberthia. Cada tick, escaneia jogadores próximos
 * e tenta inserir até {@link #PER_TICK_PER_PLAYER} FE em cada item da
 * inventory que aceite a capability {@code ForgeCapabilities.ENERGY}.
 *
 * <p>Itens cobertos: {@code Dark Matter Cell}, espadas/ferramentas FE de
 * outros mods, etc. Tudo que registre {@code IEnergyStorage} no item-stack.
 */
public class WirelessChargerBlockEntity extends BlockEntity
        implements br.com.murilo.liberthia.persistence.Persistable {

    public static final int CAPACITY = 1_000_000;
    public static final int MAX_TRANSFER = 50_000;
    public static final int RANGE = 6;
    public static final int PER_TICK_PER_PLAYER = 200;

    private final br.com.murilo.liberthia.energy.TrackedEnergyStorage energy =
            new br.com.murilo.liberthia.energy.TrackedEnergyStorage(this, CAPACITY, MAX_TRANSFER, MAX_TRANSFER);
    private LazyOptional<IEnergyStorage> lazy = LazyOptional.empty();
    /** Quantos jogadores carregando agora — pra animação. */
    private int activeChargingPlayers = 0;

    public WirelessChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIRELESS_CHARGER.get(), pos, state);
    }

    public int getEnergyStored() { return energy.getEnergyStored(); }
    public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
    public int getActiveChargingPlayers() { return activeChargingPlayers; }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() {
        super.onLoad();
        lazy = LazyOptional.of(() -> energy);
        br.com.murilo.liberthia.persistence.Persistable.LIVE.add(this);
        if (level instanceof net.minecraft.server.level.ServerLevel sl && isStateEmpty()) {
            CompoundTag snap = br.com.murilo.liberthia.persistence.LiberthiaPersistence
                    .get(sl).getSnapshot(sl, worldPosition);
            if (snap != null) restoreFromSnapshot(snap);
        }
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazy.invalidate(); }
    @Override public void setRemoved() {
        super.setRemoved();
        br.com.murilo.liberthia.persistence.Persistable.LIVE.remove(this);
    }

    @Override public boolean isStateEmpty() { return energy.getEnergyStored() == 0; }
    @Override public void restoreFromSnapshot(CompoundTag tag) { load(tag); setChanged(); }

    public static void tick(Level level, BlockPos pos, BlockState state, WirelessChargerBlockEntity be) {
        if (level.isClientSide) return;
        if (be.energy.getEnergyStored() <= 0) {
            if (be.activeChargingPlayers != 0) { be.activeChargingPlayers = 0; be.markUpdated(); }
            return;
        }

        // Backup snapshot a cada 600t
        if (level instanceof net.minecraft.server.level.ServerLevel sl
                && level.getGameTime() % br.com.murilo.liberthia.persistence.PersistenceHandler.SNAPSHOT_PERIOD == 0) {
            br.com.murilo.liberthia.persistence.LiberthiaPersistence.get(sl)
                    .snapshot(sl, pos, be.saveWithFullMetadata());
        }

        AABB box = new AABB(pos).inflate(RANGE);
        int charging = 0;
        int totalDrained = 0;
        for (Player player : level.getEntitiesOfClass(Player.class, box)) {
            if (player.isSpectator() || player.isCreative()) continue;
            int drained = chargePlayerInventory(be, player);
            if (drained > 0) {
                charging++;
                totalDrained += drained;
            }
        }
        if (totalDrained > 0) be.setChanged();

        // Partícula efeito quando ativo
        if (charging > 0 && level instanceof ServerLevel sl && level.getGameTime() % 8 == 0) {
            for (int i = 0; i < 3; i++) {
                double a = level.random.nextDouble() * Math.PI * 2;
                sl.sendParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5 + Math.cos(a) * 0.6,
                        pos.getY() + 0.7 + level.random.nextDouble() * 0.4,
                        pos.getZ() + 0.5 + Math.sin(a) * 0.6,
                        1, 0, 0.05, 0, 0);
            }
        }

        if (be.activeChargingPlayers != charging) {
            be.activeChargingPlayers = charging;
            be.markUpdated();
        }
    }

    /** Carrega itens FE-compatíveis no inventory do jogador. Retorna FE total drenado. */
    private static int chargePlayerInventory(WirelessChargerBlockEntity be, Player player) {
        int budget = Math.min(PER_TICK_PER_PLAYER, be.energy.getEnergyStored());
        if (budget <= 0) return 0;
        int spent = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (budget <= 0) break;
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            int finalBudget = budget;
            int sent = stack.getCapability(ForgeCapabilities.ENERGY).map(es -> {
                if (!es.canReceive()) return 0;
                return es.receiveEnergy(finalBudget, false);
            }).orElse(0);
            if (sent > 0) {
                budget -= sent;
                spent += sent;
                be.energy.extractEnergy(sent, false);
            }
        }
        return spent;
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
