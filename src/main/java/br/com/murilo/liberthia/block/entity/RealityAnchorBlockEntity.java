package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import br.com.murilo.liberthia.data.ChunkInfectionData;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
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
 * r180b — <b>Âncora de Realidade</b>. O oposto CALMO do {@code EntropyEngineBlockEntity}:
 * consome FE pra <b>estabilizar</b> a região — reduz a corrupção ({@link ChunkInfectionData})
 * dos chunks à volta e <b>acalma a mente</b> dos players por perto (regen de sanidade +
 * reduz paranoia/insanidade + limpa Escuridão/Náusea). Um porto seguro contra a matéria
 * escura. Recebe FE da rede (ex.: do próprio Motor de Entropia).
 */
public class RealityAnchorBlockEntity extends BlockEntity {

    private static final int BUFFER = 200_000;
    private static final int RADIUS_CHUNKS = 1;       // 3×3 chunks
    private static final int FE_PER_CHUNK = 400;      // custo por chunk estabilizado/ciclo
    private static final int FE_PER_PLAYER = 300;     // custo por player acalmado/ciclo
    private static final double PLAYER_RADIUS = 24.0;

    private final class ConsumerEnergy extends TrackedEnergyStorage {
        ConsumerEnergy() { super(RealityAnchorBlockEntity.this, BUFFER, 10_000, 0); } // só recebe
        void consume(int amt) { this.energy = Math.max(0, this.energy - amt); }
    }
    private final ConsumerEnergy energy = new ConsumerEnergy();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> energy);

    private int counter = 0;
    private boolean active = false;
    private int lastContamination = 0;

    public RealityAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REALITY_ANCHOR.get(), pos, state);
    }

    public int getEnergyStored()  { return energy.getEnergyStored(); }
    public int getMaxEnergy()     { return energy.getMaxEnergyStored(); }
    public boolean isActive()     { return active; }
    public int getLastContamination() { return lastContamination; }

    public static void tick(Level level, BlockPos pos, BlockState state, RealityAnchorBlockEntity be) {
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;
        be.counter++;

        ChunkPos center = new ChunkPos(pos);
        be.lastContamination = ChunkInfectionData.get(sl).getContamination(center);

        if (be.energy.getEnergyStored() < FE_PER_CHUNK) { be.active = false; return; }
        be.active = true;

        // 1×/s: estabiliza os chunks à volta (reduz corrupção até onde a energia paga)
        if (be.counter % 20 == 0) be.stabilizeChunks(sl, pos);
        // 1×/2s: acalma os players no raio
        if (be.counter % 40 == 0) be.calmPlayers(sl, pos);

        // VFX: aura branca estável + anel
        if (be.counter % 4 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    2, 0.25, 0.4, 0.25, 0.005);
            double a = (level.getGameTime() % 360) * (Math.PI / 180.0) * 4;
            sl.sendParticles(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5 + Math.cos(a) * 6, pos.getY() + 1.0, pos.getZ() + 0.5 + Math.sin(a) * 6,
                    1, 0, 0.2, 0, 0.0);
        }
        be.setChanged();
    }

    private void stabilizeChunks(ServerLevel sl, BlockPos pos) {
        ChunkInfectionData data = ChunkInfectionData.get(sl);
        ChunkPos cp = new ChunkPos(pos);
        boolean did = false;
        for (int dx = -RADIUS_CHUNKS; dx <= RADIUS_CHUNKS; dx++) {
            for (int dz = -RADIUS_CHUNKS; dz <= RADIUS_CHUNKS; dz++) {
                if (energy.getEnergyStored() < FE_PER_CHUNK) break;
                ChunkPos c = new ChunkPos(cp.x + dx, cp.z + dz);
                int v = data.getContamination(c);
                if (v <= 0) continue;
                data.setContamination(c, Math.max(0, v - 1));
                energy.consume(FE_PER_CHUNK);
                did = true;
            }
        }
        if (did && counter % 100 == 0) {
            sl.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.5F, 1.6F);
        }
    }

    private void calmPlayers(ServerLevel sl, BlockPos pos) {
        AABB box = new AABB(pos).inflate(PLAYER_RADIUS);
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (!(p instanceof ServerPlayer sp) || sp.isSpectator()) continue;
            if (energy.getEnergyStored() < FE_PER_PLAYER) break;
            energy.consume(FE_PER_PLAYER);
            // a realidade é estável aqui → a mente acalma
            SpiritDimension.addSanity(sp, 2);
            ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
            try {
                InsanityData.addParanoia(sp, -1);
                InsanityData.addInsanity(sp, -1);
            } catch (Throwable ignored) {}
            sp.removeEffect(MobEffects.DARKNESS);
            sp.removeEffect(MobEffects.CONFUSION);
            sp.removeEffect(MobEffects.WITHER);
        }
    }

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
        energy.consume(energy.getEnergyStored());
        int e = tag.getInt("Energy");
        if (e > 0) energy.receiveEnergy(e, false);
    }
}
