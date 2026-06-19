package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.compat.mna.MagicDetect;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
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

import java.util.UUID;

/**
 * r180: <b>Pilar Protetor</b> — bloco anti-magia movido a FE. Quando tem energia, projeta
 * um campo (raio {@value #RADIUS}b). Qualquer player que <b>carregue itens de magia</b>
 * ({@link MagicDetect}) e <b>não seja o dono</b>, ao entrar no campo, leva dano + recebe
 * <b>Antimagia</b> e é empurrado pra fora. Feixe ciano animado. Recebe FE da rede.
 */
public class WardPillarBlockEntity extends BlockEntity {

    private static final int BUFFER = 100_000;
    private static final int FE_IDLE = 40;     // custo/tick ligado
    private static final int FE_REPEL = 200;   // custo extra ao repelir
    private static final double RADIUS = 8.0;
    private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(0.17F, 0.84F, 0.84F), 1.3F);

    private UUID owner;
    private final class ConsumerEnergy extends TrackedEnergyStorage {
        ConsumerEnergy() { super(WardPillarBlockEntity.this, BUFFER, 10_000, 0); } // só recebe
        void consume(int amt) { this.energy = Math.max(0, this.energy - amt); }
    }
    private final ConsumerEnergy energy = new ConsumerEnergy();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> energy);
    private boolean active;

    public WardPillarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WARD_PILLAR.get(), pos, state);
    }

    public void setOwner(UUID o) { this.owner = o; setChanged(); }
    public UUID getOwner() { return owner; }
    public int getEnergyStored() { return energy.getEnergyStored(); }
    public boolean isActive() { return active; }

    public static void tick(Level level, BlockPos pos, BlockState state, WardPillarBlockEntity be) {
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;

        if (be.energy.getEnergyStored() < FE_IDLE) {
            be.active = false;
            return;
        }
        be.active = true;
        int cost = FE_IDLE;

        Vec3 center = Vec3.atCenterOf(pos);
        AABB box = new AABB(pos).inflate(RADIUS);
        boolean repelled = false;
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (be.owner != null && p.getUUID().equals(be.owner)) continue;
            if (!MagicDetect.isMagicUser(p)) continue;          // só intrusos COM magia
            if (p.position().distanceToSqr(center) > RADIUS * RADIUS) continue;

            // intruso! sela + dano + empurra pra fora
            p.addEffect(new MobEffectInstance(ModEffects.ANTIMAGIA.get(), 80, 0, false, true, true));
            if (level.getGameTime() % 20 == 0) {
                p.hurt(p.damageSources().magic(), 3.0F);
            }
            Vec3 away = p.position().subtract(center);
            if (away.lengthSqr() > 0.01) {
                away = away.normalize().scale(0.55);
                p.push(away.x, 0.15, away.z);
                p.hurtMarked = true;
            }
            sl.sendParticles(CYAN, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.3, 0.5, 0.3, 0.0);
            repelled = true;
        }
        if (repelled) {
            cost += FE_REPEL;
            if (level.getGameTime() % 10 == 0) {
                sl.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 0.6F);
            }
        }
        be.energy.consume(cost);

        // feixe + cúpula (VFX)
        if (level.getGameTime() % 4 == 0) {
            for (int y = 1; y <= 6; y++) {
                sl.sendParticles(CYAN, center.x, pos.getY() + y, center.z, 1, 0.05, 0.0, 0.05, 0.0);
            }
            double a = (level.getGameTime() % 360) * (Math.PI / 180.0) * 6;
            sl.sendParticles(CYAN, center.x + Math.cos(a) * RADIUS, pos.getY() + 1.0, center.z + Math.sin(a) * RADIUS, 1, 0, 0.3, 0, 0.0);
        }
        be.setChanged();
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
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.consume(energy.getEnergyStored()); // zera
        int e = tag.getInt("Energy");
        if (e > 0) energy.receiveEnergy(e, false);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
    }
}
