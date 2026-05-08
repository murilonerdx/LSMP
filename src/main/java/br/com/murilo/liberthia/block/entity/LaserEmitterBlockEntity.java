package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Laser Emitter — pode atirar em VÁRIAS direções simultaneamente.
 *
 * <p>Cada face do bloco pode ser ligada/desligada via shift+right-click. Cada
 * direção ativa consome {@link #FE_PER_TICK} FE/tick enquanto está disparando.
 *
 * <p>Estado é sincronizado pro cliente toda vez que muda (NBT no
 * BlockEntityDataPacket) — o renderer lê {@link #getActiveBeams} pra desenhar.
 */
public class LaserEmitterBlockEntity extends BlockEntity {

    public static final int MAX_RANGE = 8;
    /** Consumo POR FACE ATIVA — 500 FE/tick. 1 gerador padrão (1000/tick) =
     *  2 lasers single-face simultâneos, ou 1 laser com 2 faces.
     *  PARTIAL FIRE: cada face puxa independentemente — se faltar energia,
     *  algumas faces firam, outras não. */
    public static final int FE_PER_TICK = 500;

    /** Cache para Block.onRemove poder usar. */
    public boolean isAnyBeamActive() {
        return !beamLengths.isEmpty();
    }

    /** Faces escolhidas pelo jogador (persiste no NBT, sincroniza). */
    private final EnumSet<Direction> activeFacings = EnumSet.noneOf(Direction.class);
    /** Comprimento atual do feixe em cada direção ativa (0 = sem energia, >0 = visível). */
    private final EnumMap<Direction, Integer> beamLengths = new EnumMap<>(Direction.class);

    public LaserEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LASER_EMITTER.get(), pos, state);
    }

    public Set<Direction> getActiveFacings() { return activeFacings; }

    /** Map direção → comprimento do feixe atual. Ler do cliente pra renderizar. */
    public Map<Direction, Integer> getActiveBeams() { return beamLengths; }

    /** Toggle uma face. Retorna true se passou a estar ATIVA. */
    public boolean toggleFacing(Direction d) {
        boolean active;
        if (activeFacings.contains(d)) { activeFacings.remove(d); active = false; }
        else { activeFacings.add(d); active = true; }
        beamLengths.remove(d);
        markUpdated();
        return active;
    }

    /** Sync explícito: salva BE e dispara update aos clientes. */
    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // PURE PULL — laser puxa via pullThroughNetwork; não aceita push.
    // Mas EXPÕE uma capability NO-OP pra que cabos detectem ele como vizinho
    // FE válido e mostrem o braço de conexão visual. canReceive/canExtract
    // ambos = false → BFS não tenta empurrar nem puxar diretamente.
    private static final IEnergyStorage NULL_STORAGE = new IEnergyStorage() {
        @Override public int receiveEnergy(int max, boolean sim) { return 0; }
        @Override public int extractEnergy(int max, boolean sim) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return 0; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return false; }
    };
    private LazyOptional<IEnergyStorage> lazy = LazyOptional.empty();

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazy = LazyOptional.of(() -> NULL_STORAGE); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazy.invalidate(); }

    /** AABB grande pro renderer cobrir feixe inteiro. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(MAX_RANGE + 1);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaserEmitterBlockEntity be) {
        if (level.isClientSide) return;
        if (be.activeFacings.isEmpty()) {
            if (!be.beamLengths.isEmpty()) { be.beamLengths.clear(); be.markUpdated(); }
            return;
        }

        // PARTIAL FIRE: cada face independentemente.
        // 1. Simula quanto poderia ser puxado pra cada face
        // 2. Pra cada face onde simulação >= FE_PER_TICK, faz o pull real e fira
        // Como cada simulate/pull é uma BFS independente, as fontes estão sempre
        // atualizadas — não há leak de energia.
        boolean changed = false;
        for (Direction d : be.activeFacings) {
            int avail = br.com.murilo.liberthia.energy.EnergyNetwork
                    .simulatePull(level, pos, FE_PER_TICK);
            if (avail < FE_PER_TICK) {
                // Sem energia suficiente — apaga essa face
                if (be.beamLengths.containsKey(d)) {
                    be.beamLengths.remove(d);
                    changed = true;
                }
                continue;
            }
            // Tem energia — pull real e fira
            int pulled = br.com.murilo.liberthia.energy.EnergyNetwork
                    .pullThroughNetwork(level, pos, FE_PER_TICK);
            if (pulled < FE_PER_TICK) {
                // Race: outra coisa pegou no meio. Apaga essa face nesta tick.
                if (be.beamLengths.containsKey(d)) {
                    be.beamLengths.remove(d);
                    changed = true;
                }
                continue;
            }
            int hitDist = raycast(level, pos, d);
            Integer prev = be.beamLengths.get(d);
            if (prev == null || prev != hitDist) {
                be.beamLengths.put(d, hitDist);
                changed = true;
            }
            BlockEntity hitBe = level.getBlockEntity(pos.relative(d, hitDist));
            if (hitBe instanceof CrystallizerBlockEntity c) {
                c.onLaserHit(pos, level.getGameTime());
            }
            damageEntitiesInBeam(level, pos, d, hitDist);
        }

        if (changed) be.markUpdated();
    }

    /** Raycast pelo ar até MAX_RANGE — retorna distância em blocos do bloco emissor. */
    private static int raycast(Level level, BlockPos pos, Direction facing) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int hitDist = MAX_RANGE;
        for (int i = 1; i <= MAX_RANGE; i++) {
            m.set(pos.getX() + facing.getStepX() * i,
                    pos.getY() + facing.getStepY() * i,
                    pos.getZ() + facing.getStepZ() * i);
            BlockState bs = level.getBlockState(m);
            if (bs.isAir()) { hitDist = i; continue; }
            BlockEntity nbe = level.getBlockEntity(m);
            if (nbe instanceof CrystallizerBlockEntity) { hitDist = i; break; }
            if (!bs.canBeReplaced()) { hitDist = i; break; }
        }
        return hitDist;
    }

    /**
     * Dano extremo no feixe + AURA DE CALOR ao redor (1 bloco de raio extra).
     * <ul>
     *   <li>Dentro do feixe: 14 dmg + 12s de fogo</li>
     *   <li>Aura de calor (1 bloco em torno do trajeto): 4 dmg + 6s de fogo</li>
     * </ul>
     */
    private static void damageEntitiesInBeam(Level level, BlockPos pos, Direction facing, int length) {
        if (length <= 0) return;
        double sx = pos.getX() + 0.5, sy = pos.getY() + 0.5, sz = pos.getZ() + 0.5;
        double ex = sx + facing.getStepX() * length;
        double ey = sy + facing.getStepY() * length;
        double ez = sz + facing.getStepZ() * length;

        // Feixe direto (radius 0.4)
        AABB beamBox = new AABB(
                Math.min(sx, ex) - 0.4, Math.min(sy, ey) - 0.4, Math.min(sz, ez) - 0.4,
                Math.max(sx, ex) + 0.4, Math.max(sy, ey) + 0.4, Math.max(sz, ez) + 0.4);
        // Aura de calor (radius 1.5)
        AABB heatBox = new AABB(
                Math.min(sx, ex) - 1.5, Math.min(sy, ey) - 1.5, Math.min(sz, ez) - 1.5,
                Math.max(sx, ex) + 1.5, Math.max(sy, ey) + 1.5, Math.max(sz, ez) + 1.5);

        var inHeat = level.getEntitiesOfClass(LivingEntity.class, heatBox);
        for (LivingEntity e : inHeat) {
            if (beamBox.contains(e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ())) {
                e.hurt(level.damageSources().magic(), 14.0f);
                e.setSecondsOnFire(12);
            } else {
                // Aura: dano menor mas ainda forte
                e.hurt(level.damageSources().onFire(), 4.0f);
                e.setSecondsOnFire(6);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        // facings → bitmap
        int mask = 0;
        for (Direction d : activeFacings) mask |= (1 << d.get3DDataValue());
        tag.putInt("facings", mask);
        // beam lengths
        CompoundTag bl = new CompoundTag();
        for (var e : beamLengths.entrySet()) bl.putInt(e.getKey().getName(), e.getValue());
        tag.put("beams", bl);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        activeFacings.clear();
        int mask = tag.getInt("facings");
        for (Direction d : Direction.values()) {
            if ((mask & (1 << d.get3DDataValue())) != 0) activeFacings.add(d);
        }
        beamLengths.clear();
        if (tag.contains("beams")) {
            CompoundTag bl = tag.getCompound("beams");
            for (Direction d : Direction.values()) {
                if (bl.contains(d.getName())) beamLengths.put(d, bl.getInt(d.getName()));
            }
        }
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
