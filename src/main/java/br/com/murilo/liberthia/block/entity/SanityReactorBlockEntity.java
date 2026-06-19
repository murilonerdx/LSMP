package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.energy.EnergyNetwork;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * r180b — <b>Reator de Sanidade</b>. Gerador de FE "metafísico": consome a <b>sanidade</b>
 * ({@link SpiritDimension}) de quem fica COLADO nele (≤2,5b). Pacto faustiano: <b>quanto
 * MENOR a sanidade, mais energia por ponto drenado</b> (1× a 100 → ~5× perto de 0). Ao
 * cair a sanidade, o pipeline de horror existente reage sozinho (alucinações, etc).
 * Para em 0 (mente vazia = nada a consumir). Mesmo padrão FE do Motor de Entropia.
 */
public class SanityReactorBlockEntity extends BlockEntity {

    private static final int BUFFER = 300_000;
    private static final int GEN_RATE = 20_000;
    private static final int PUSH_BUDGET = 8_000;
    private static final int DRAIN_INTERVAL = 40;   // 2s
    private static final double RADIUS = 2.5;       // tem que estar colado (opt-in)
    private static final int FE_BASE = 1_500;

    private final TrackedEnergyStorage energy = new TrackedEnergyStorage(this, BUFFER, GEN_RATE, Integer.MAX_VALUE);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> new SourceOnlyEnergyView(energy));
    private int counter = 0;
    private int lastGen = 0;
    private int lastSanity = 100;
    private boolean active = false;

    public SanityReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SANITY_REACTOR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SanityReactorBlockEntity be) {
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;
        be.counter++;
        if (be.counter % DRAIN_INTERVAL == 0) be.drainMinds(sl, pos);

        if (be.energy.getEnergyStored() > 0) {
            int budget = Math.min(PUSH_BUDGET, be.energy.getEnergyStored());
            int sent = EnergyNetwork.pushThroughNetwork(level, pos, budget);
            if (sent > 0) be.energy.extractEnergy(sent, false);
        }
    }

    private void drainMinds(ServerLevel sl, BlockPos pos) {
        AABB box = new AABB(pos).inflate(RADIUS);
        int gained = 0;
        boolean any = false;
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (!(p instanceof ServerPlayer sp) || sp.isCreative() || sp.isSpectator()) continue;
            int san = SpiritDimension.getSanity(sp);
            if (san <= 0) continue;                       // mente vazia → nada a consumir
            lastSanity = san;
            SpiritDimension.addSanity(sp, -1);
            ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
            int fe = (int) (FE_BASE * (1.0 + (100 - san) / 25.0)); // 1×..~5×
            energy.receiveEnergy(fe, false);
            gained += fe;
            any = true;
            if (counter % 80 == 0) {
                sp.displayClientMessage(Component.literal("§5§oO reator devora um fragmento da sua mente..."), true);
            }
            sl.sendParticles(ParticleTypes.SOUL, sp.getX(), sp.getY() + 1.0, sp.getZ(), 6, 0.3, 0.5, 0.3, 0.02);
        }
        lastGen = gained;
        active = any;
        if (any) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
            if (counter % 100 == 0) {
                sl.playSound(null, pos, SoundEvents.SOUL_ESCAPE, SoundSource.BLOCKS, 0.7F, 0.5F);
            }
        }
        setChanged();
    }

    public int getEnergyStored() { return energy.getEnergyStored(); }
    public int getMaxEnergy()    { return energy.getMaxEnergyStored(); }
    public int getLastGen()      { return lastGen; }
    public int getLastSanity()   { return lastSanity; }
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

    /** Source-only (canReceive=false externamente) — evita loop gerador↔bateria. */
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
