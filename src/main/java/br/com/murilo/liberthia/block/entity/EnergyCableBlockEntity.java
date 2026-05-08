package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Cabo de energia — relay "transparente" para Forge Energy.
 *
 * <p>Cada face pode estar em um de 2 modos (versão atual):
 * <ul>
 *   <li><b>DEFAULT</b> — conecta normalmente (passa energia em ambas as direções)</li>
 *   <li><b>DISABLED</b> — face desligada, BFS ignora, visual sem braço</li>
 * </ul>
 *
 * <p>Right-click numa face do cabo cicla o modo daquela face. Sync explícito
 * pro cliente quando muda.
 */
public class EnergyCableBlockEntity extends BlockEntity {

    public static final int THROUGHPUT_PER_TICK = 1_000;

    public enum FaceMode { DEFAULT, DISABLED }

    private LazyOptional<IEnergyStorage> lazy = LazyOptional.empty();
    /** Faces marcadas como DISABLED. Default = DEFAULT (não está no set). */
    private final EnumSet<Direction> disabledFaces = EnumSet.noneOf(Direction.class);

    public EnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_CABLE.get(), pos, state);
    }

    public FaceMode getMode(Direction d) {
        return disabledFaces.contains(d) ? FaceMode.DISABLED : FaceMode.DEFAULT;
    }

    /** Cicla o modo daquela face (DEFAULT ↔ DISABLED). Retorna o novo modo. */
    public FaceMode cycleMode(Direction d) {
        FaceMode next;
        if (disabledFaces.contains(d)) { disabledFaces.remove(d); next = FaceMode.DEFAULT; }
        else { disabledFaces.add(d); next = FaceMode.DISABLED; }
        setChanged();
        if (level != null && !level.isClientSide()) {
            // Log INFO (não debug) pra aparecer sem precisar habilitar debug logs
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                    "[Liberthia] Cable {} face {} -> {}, disabledFaces={}",
                    worldPosition, d.getName(), next, disabledFaces);

            // 1. Recomputa BlockState (BooleanProperties) — arms visuais somem/aparecem
            br.com.murilo.liberthia.block.EnergyCableBlock.recomputeConnections(level, worldPosition);

            // 2. Sync explícito BE NBT pro client
            BlockState s = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, s, s, 3);

            // 3. Notifica vizinhos pra invalidarem capabilities cacheadas
            level.updateNeighborsAt(worldPosition, s.getBlock());

            // 4. Re-aciona shouldConnect dos VIZINHOS via updateShape forçado.
            //    Garante que o braço do cabo VIZINHO (que aponta pra nós) também
            //    seja recomputado.
            for (Direction nd : Direction.values()) {
                BlockPos npos = worldPosition.relative(nd);
                if (level.getBlockState(npos).getBlock() instanceof br.com.murilo.liberthia.block.EnergyCableBlock) {
                    br.com.murilo.liberthia.block.EnergyCableBlock.recomputeConnections(level, npos);
                }
            }
        }
        return next;
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            // Se a face de onde vem o pedido está DISABLED, não expõe capability.
            if (side != null && disabledFaces.contains(side)) return LazyOptional.empty();
            return lazy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override public void onLoad() {
        super.onLoad();
        lazy = LazyOptional.of(() -> new ProxyEnergy(this));
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        lazy.invalidate();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergyCableBlockEntity be) {
        // Cabo não precisa tick — toda lógica é reativa via getCapability/proxy.
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        int mask = 0;
        for (Direction d : disabledFaces) mask |= 1 << d.get3DDataValue();
        tag.putInt("disabledFaces", mask);
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        disabledFaces.clear();
        int mask = tag.getInt("disabledFaces");
        for (Direction d : Direction.values()) {
            if ((mask & (1 << d.get3DDataValue())) != 0) disabledFaces.add(d);
        }
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    /**
     * IEnergyStorage virtual exposto pelo cabo. Qualquer FE empurrado é
     * redistribuído via BFS imediatamente para consumidores reais.
     */
    private static final class ProxyEnergy implements IEnergyStorage {
        private final EnergyCableBlockEntity owner;
        ProxyEnergy(EnergyCableBlockEntity owner) { this.owner = owner; }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (owner.level == null || owner.level.isClientSide) return 0;
            int budget = Math.min(THROUGHPUT_PER_TICK, maxReceive);
            if (budget <= 0) return 0;
            if (simulate) {
                return br.com.murilo.liberthia.energy.EnergyNetwork
                        .simulatePush(owner.level, owner.getBlockPos(), budget);
            }
            return br.com.murilo.liberthia.energy.EnergyNetwork
                    .pushThroughNetwork(owner.level, owner.getBlockPos(), budget);
        }

        @Override public int extractEnergy(int max, boolean sim) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return THROUGHPUT_PER_TICK; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    }
}
