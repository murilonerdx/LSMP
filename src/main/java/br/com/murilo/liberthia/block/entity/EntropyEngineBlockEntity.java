package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.data.ChunkInfectionData;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * r180: <b>Motor de Entropia</b> — o coração do sistema de energia metafísica.
 *
 * <p>Não queima combustível: <b>desfaz a realidade</b>. A cada ciclo "consome" (deleta,
 * sem drops) blocos num raio → gera FE. O output ESCALA com a <b>corrupção local</b>
 * (feedback positivo), e rodar o motor <b>SOBE a corrupção do chunk</b> ({@link ChunkInfectionData}).
 * Corrupção alta = mais energia, mas o sistema de infecção/horror reage (mutações,
 * densidade ≥0.9 → buraco negro via {@code InfectionLogic}). Energia = aposta contra o colapso.
 */
public class EntropyEngineBlockEntity extends BlockEntity {

    private static final int BUFFER = 400_000;
    private static final int GEN_RATE = 20_000;        // teto de receive interno
    private static final int PUSH_BUDGET = 8_000;      // FE/tick empurrado pra rede
    private static final int CONSUME_INTERVAL = 20;    // 1×/s consome realidade
    private static final int RADIUS = 4;
    private static final int FE_PER_BLOCK = 600;       // base por bloco consumido

    private final TrackedEnergyStorage energy = new TrackedEnergyStorage(this, BUFFER, GEN_RATE, Integer.MAX_VALUE);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> new SourceOnlyEnergyView(energy));
    private int counter = 0;
    private int lastCorruption = 0;
    private int lastGen = 0;

    public EntropyEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENTROPY_ENGINE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EntropyEngineBlockEntity be) {
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;
        be.counter++;
        if (be.counter % CONSUME_INTERVAL == 0) be.consumeReality(sl, pos);

        // empurra energia pra rede
        if (be.energy.getEnergyStored() > 0) {
            int budget = Math.min(PUSH_BUDGET, be.energy.getEnergyStored());
            int sent = EnergyNetwork.pushThroughNetwork(level, pos, budget);
            if (sent > 0) be.energy.extractEnergy(sent, false);
        }
    }

    private void consumeReality(ServerLevel sl, BlockPos pos) {
        ChunkInfectionData data = ChunkInfectionData.get(sl);
        ChunkPos cp = new ChunkPos(pos);
        int corruption = data.getContamination(cp);   // 0..100
        lastCorruption = corruption;
        float mult = 1.0F + corruption / 50.0F;        // até 3× a 100 de corrupção

        int consumed = 0;
        int gained = 0;
        for (int attempt = 0; attempt < 10 && consumed < 2; attempt++) {
            BlockPos t = pos.offset(
                    sl.random.nextInt(RADIUS * 2 + 1) - RADIUS,
                    sl.random.nextInt(RADIUS * 2 + 1) - RADIUS,
                    sl.random.nextInt(RADIUS * 2 + 1) - RADIUS);
            if (t.equals(pos)) continue;
            BlockState bs = sl.getBlockState(t);
            if (bs.isAir() || bs.hasBlockEntity()) continue;
            float hard = bs.getDestroySpeed(sl, t);
            if (hard < 0 || hard > 50) continue;        // pula bedrock/inquebrável
            sl.setBlock(t, Blocks.AIR.defaultBlockState(), 3);   // UNMAKE (sem drop)
            int fe = (int) ((FE_PER_BLOCK + hard * 120) * mult);
            energy.receiveEnergy(fe, false);
            gained += fe;
            consumed++;
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, t.getX() + 0.5, t.getY() + 0.5, t.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
        }
        lastGen = gained;

        if (consumed > 0) {
            // RISCO: rodar o motor espalha corrupção (alimenta o pipeline de perigo existente)
            data.setContamination(cp, corruption + 2);
            sl.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
            if (counter % 100 == 0) {
                sl.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 0.6F, 0.4F);
            }
            // corrupção crítica → aciona os efeitos de região existentes (mutações/buraco negro)
            if (corruption >= 80 && counter % 60 == 0) {
                try { br.com.murilo.liberthia.logic.InfectionLogic.evaluateDarkMatterRegion(sl, pos); }
                catch (Throwable ignored) {}
            }
        }
        setChanged();
    }

    public int getEnergyStored()  { return energy.getEnergyStored(); }
    public int getMaxEnergy()     { return energy.getMaxEnergyStored(); }
    public int getLastCorruption(){ return lastCorruption; }
    public int getLastGen()       { return lastGen; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate();
    }

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

    /** Wrapper source-only (canReceive=false externamente) — evita loop gerador↔bateria. */
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
