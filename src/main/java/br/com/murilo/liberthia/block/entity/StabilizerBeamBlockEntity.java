package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.data.ChunkInfectionData;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
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
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * r180b — <b>Raio Estabilizador</b> (trio de contenção). Dispara um FEIXE direcional
 * (na direção em que foi colocado, até {@value #RANGE} blocos, parando em paredes) que:
 * <ul>
 *   <li><b>limpa a corrupção</b> ({@link ChunkInfectionData}) dos chunks na linha;</li>
 *   <li><b>dispersa o "irreal"</b> — entidades cósmicas/corrompidas no feixe levam dano
 *       de realidade (ignora armadura) e são empurradas.</li>
 * </ul>
 * Movido a FE. Variante ativa/ofensiva da {@code Âncora de Realidade} (que é passiva/área).
 */
public class StabilizerBeamBlockEntity extends BlockEntity {

    private static final int BUFFER = 150_000;
    private static final int FE_IDLE = 80;
    private static final int FE_PER_CHUNK = 300;
    private static final int FE_PER_HIT = 200;
    private static final int RANGE = 16;
    private static final DustParticleOptions BEAM =
            new DustParticleOptions(new Vector3f(0.55F, 0.95F, 1.0F), 1.0F);

    private final class ConsumerEnergy extends TrackedEnergyStorage {
        ConsumerEnergy() { super(StabilizerBeamBlockEntity.this, BUFFER, 10_000, 0); }
        void consume(int amt) { this.energy = Math.max(0, this.energy - amt); }
    }
    private final ConsumerEnergy energy = new ConsumerEnergy();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> energy);

    private Direction facing = Direction.NORTH;
    private int counter = 0;
    private boolean active = false;

    public StabilizerBeamBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STABILIZER_BEAM.get(), pos, state);
    }

    public void setFacing(Direction d) { this.facing = d.getAxis().isVertical() ? Direction.NORTH : d; setChanged(); }
    public Direction getFacing() { return facing; }
    public int getEnergyStored() { return energy.getEnergyStored(); }
    public int getMaxEnergy() { return energy.getMaxEnergyStored(); }
    public boolean isActive() { return active; }

    public static void tick(Level level, BlockPos pos, BlockState state, StabilizerBeamBlockEntity be) {
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;
        be.counter++;

        if (be.energy.getEnergyStored() < FE_IDLE) { be.active = false; return; }
        be.active = true;
        be.energy.consume(FE_IDLE);

        // traça o feixe (para na primeira parede)
        int reached = 0;
        for (int i = 1; i <= RANGE; i++) {
            BlockPos p = pos.relative(be.facing, i);
            if (sl.getBlockState(p).blocksMotion()) break;
            reached = i;
            if (be.counter % 2 == 0) {
                sl.sendParticles(BEAM, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
        if (reached == 0) { be.setChanged(); return; }
        BlockPos end = pos.relative(be.facing, reached);

        // 1×/s: limpa corrupção dos chunks ao longo do feixe
        if (be.counter % 20 == 0) {
            ChunkInfectionData data = ChunkInfectionData.get(sl);
            Set<Long> done = new HashSet<>();
            for (int i = 1; i <= reached; i++) {
                if (be.energy.getEnergyStored() < FE_PER_CHUNK) break;
                ChunkPos c = new ChunkPos(pos.relative(be.facing, i));
                if (!done.add(c.toLong())) continue;
                int v = data.getContamination(c);
                if (v <= 0) continue;
                data.setContamination(c, Math.max(0, v - 1));
                be.energy.consume(FE_PER_CHUNK);
            }
        }

        // 1×/0.5s: dispersa o irreal no feixe
        if (be.counter % 10 == 0) {
            AABB box = new AABB(pos).minmax(new AABB(end)).inflate(1.0);
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
                if (!isUnreal(le) || be.energy.getEnergyStored() < FE_PER_HIT) continue;
                be.energy.consume(FE_PER_HIT);
                le.hurt(le.damageSources().magic(), 4.0F); // dano de realidade (ignora armadura)
                Vec3 push = le.position().subtract(Vec3.atCenterOf(pos));
                if (push.lengthSqr() > 0.01) {
                    push = push.normalize().scale(0.4);
                    le.push(push.x, 0.1, push.z);
                    le.hasImpulse = true;
                }
                sl.sendParticles(BEAM, le.getX(), le.getY() + 0.8, le.getZ(), 6, 0.3, 0.5, 0.3, 0.0);
            }
        }
        be.setChanged();
    }

    /** "Irreal" = entidade cósmica/corrompida (pelo pacote da classe ou nome). */
    private static boolean isUnreal(Entity e) {
        String cn = e.getClass().getName();
        return cn.contains(".cosmic.") || cn.contains("Corrupted") || cn.contains("Infected");
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
        tag.putInt("Facing", facing.get3DDataValue());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.consume(energy.getEnergyStored());
        int e = tag.getInt("Energy");
        if (e > 0) energy.receiveEnergy(e, false);
        facing = Direction.from3DDataValue(tag.getInt("Facing"));
        if (facing.getAxis().isVertical()) facing = Direction.NORTH;
    }
}
