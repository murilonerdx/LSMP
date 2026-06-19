package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * r184 — <b>Torre Arcana</b>: defesa tecnológica. Enquanto tiver FE, atira um feixe nos
 * monstros hostis no raio ({@value #RADIUS}) — dano + Antimagia (forte contra cósmicos/magos).
 * Consome FE por disparo. GUI de energia.
 */
public class ArcaneTurretBlockEntity extends BlockEntity implements MenuProvider, ITechEnergyUI {
    private static final int BUFFER = 300_000, RECEIVE = 10_000, COST = 150, RADIUS = 14;
    private static final float DMG = 6.0F;

    private final TrackedEnergyStorage store = new TrackedEnergyStorage(this, BUFFER, RECEIVE, 0);
    private LazyOptional<IEnergyStorage> lazy = LazyOptional.empty();
    private boolean armed = false;
    private final TechEnergyData uiData =
            new TechEnergyData(store::getEnergyStored, store::getMaxEnergyStored, () -> armed ? 1 : 0, () -> 1);

    public ArcaneTurretBlockEntity(BlockPos pos, BlockState state) { super(ModTech.ARCANE_TURRET_BE.get(), pos, state); }

    public static void tick(Level lvl, BlockPos pos, BlockState st, ArcaneTurretBlockEntity be) {
        if (lvl.isClientSide || !(lvl instanceof ServerLevel sl)) return;
        boolean wasArmed = be.armed;
        be.armed = be.store.getEnergyStored() >= COST;
        if (be.armed && sl.getGameTime() % 15 == 0) {
            Vec3 from = Vec3.atCenterOf(pos).add(0, 0.4, 0);
            Monster target = null; double best = (double) RADIUS * RADIUS;
            for (Monster m : sl.getEntitiesOfClass(Monster.class, new AABB(pos).inflate(RADIUS))) {
                if (!m.isAlive()) continue;
                double d = m.distanceToSqr(from);
                if (d < best) { best = d; target = m; }
            }
            if (target != null) {
                target.hurt(sl.damageSources().magic(), DMG);
                target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
                if (ModEffects.ANTIMAGIA.get() != null) target.addEffect(new MobEffectInstance(ModEffects.ANTIMAGIA.get(), 80, 0, false, true));
                be.store.setStored(be.store.getEnergyStored() - COST);
                Vec3 to = target.getEyePosition();
                int steps = (int) (from.distanceTo(to) * 3);
                for (int i = 0; i <= steps; i++) {
                    Vec3 pt = from.lerp(to, i / (double) Math.max(1, steps));
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pt.x, pt.y, pt.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
                sl.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.5F, 1.8F);
            }
        }
        if (be.armed != wasArmed) sl.setBlock(pos, st.setValue(br.com.murilo.liberthia.block.ArcaneTurretBlock.LIT, be.armed), 3);
    }

    public int getEnergyStored() { return store.getEnergyStored(); }
    public int getMaxEnergy() { return store.getMaxEnergyStored(); }

    @Override public ContainerData getEnergyData() { return uiData; }
    @Override public IItemHandler getMenuSlots() { return null; }
    @Override public int getLayoutId() { return LAYOUT_CONSUMER; }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
        return new br.com.murilo.liberthia.menu.TechEnergyMenu(id, inv, this, uiData);
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazy = LazyOptional.of(() -> store); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazy.invalidate(); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("Energy", store.getEnergyStored()); }
    @Override public void load(CompoundTag tag) { super.load(tag); store.setStored(tag.getInt("Energy")); }
}
