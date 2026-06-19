package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.dimension.DimensionalRiftEntity;
import br.com.murilo.liberthia.energy.EnergyNetwork;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r180b — <b>Sifão de Fenda</b>. Gerador de FE que <b>suga energia dimensional</b> de
 * {@link DimensionalRiftEntity}s num raio de {@value #RANGE}b — quanto mais fendas por
 * perto, mais energia. Limpo (não destrói nada; só dorme se não há fenda). Mesmo padrão
 * FE do Motor de Entropia. Sinergia: abra rifts com o Abridor de Fendas pra alimentá-lo.
 */
public class RiftSiphonBlockEntity extends BlockEntity {

    private static final int BUFFER = 200_000;
    private static final int GEN_RATE = 20_000;
    private static final int PUSH_BUDGET = 8_000;
    private static final int GEN_INTERVAL = 20;   // 1×/s
    private static final int RANGE = 8;
    private static final int FE_PER_RIFT = 2_000; // por fenda por ciclo

    private final TrackedEnergyStorage energy = new TrackedEnergyStorage(this, BUFFER, GEN_RATE, Integer.MAX_VALUE);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> new SourceOnlyEnergyView(energy));
    private int counter = 0;
    private int lastGen = 0;
    private int lastRifts = 0;
    private boolean active = false;

    public RiftSiphonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RIFT_SIPHON.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RiftSiphonBlockEntity be) {
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;
        be.counter++;
        if (be.counter % GEN_INTERVAL == 0) be.siphon(sl, pos);

        if (be.energy.getEnergyStored() > 0) {
            int budget = Math.min(PUSH_BUDGET, be.energy.getEnergyStored());
            int sent = EnergyNetwork.pushThroughNetwork(level, pos, budget);
            if (sent > 0) be.energy.extractEnergy(sent, false);
        }
    }

    private void siphon(ServerLevel sl, BlockPos pos) {
        AABB box = new AABB(pos).inflate(RANGE);
        List<DimensionalRiftEntity> rifts = sl.getEntitiesOfClass(DimensionalRiftEntity.class, box);
        lastRifts = rifts.size();
        if (rifts.isEmpty()) { active = false; lastGen = 0; return; }

        active = true;
        int fe = FE_PER_RIFT * rifts.size();
        energy.receiveEnergy(fe, false);
        lastGen = fe;

        Vec3 c = Vec3.atCenterOf(pos);
        for (DimensionalRiftEntity r : rifts) {
            // partículas fluindo da fenda → sifão
            Vec3 from = r.position().add(0, 1.0, 0);
            for (int i = 0; i < 3; i++) {
                double t = sl.random.nextDouble();
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        from.x + (c.x - from.x) * t, from.y + (c.y + 0.6 - from.y) * t, from.z + (c.z - from.z) * t,
                        1, 0.02, 0.02, 0.02, 0.0);
            }
        }
        if (counter % 80 == 0) {
            sl.playSound(null, pos, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F, 1.4F);
        }
        setChanged();
    }

    public int getEnergyStored() { return energy.getEnergyStored(); }
    public int getMaxEnergy()    { return energy.getMaxEnergyStored(); }
    public int getLastGen()      { return lastGen; }
    public int getLastRifts()    { return lastRifts; }
    public boolean isActive()    { return active; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); lazyEnergy.invalidate(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int e = tag.getInt("Energy");
        if (e > 0) energy.receiveEnergy(e, false);
    }

    private static final class SourceOnlyEnergyView implements IEnergyStorage {
        private final IEnergyStorage delegate;
        SourceOnlyEnergyView(IEnergyStorage d) { this.delegate = d; }
        @Override public int receiveEnergy(int max, boolean sim) { return 0; }
        @Override public int extractEnergy(int max, boolean sim) { return delegate.extractEnergy(max, sim); }
        @Override public int getEnergyStored() { return delegate.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return delegate.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    }
}
