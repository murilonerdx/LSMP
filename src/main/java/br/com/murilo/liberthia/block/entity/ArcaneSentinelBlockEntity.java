package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.magic.antimagic.AntiMagic;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * r182 — <b>Selo Sentinela</b>: enquanto tiver FE (armado), DETECTA quem conjura no raio
 * ({@value #RADIUS}) — qualquer cast marca o player ({@link AntiMagic#castFlash}) — e PUNE:
 * dano + Lentidão + Cegueira + Antimagia. Consome FE por punição. GUI de energia.
 */
public class ArcaneSentinelBlockEntity extends BlockEntity implements MenuProvider, ITechEnergyUI {
    private static final int BUFFER = 300_000, RECEIVE = 10_000, COST = 250, RADIUS = 12;
    private static final float DMG = 6.0F;

    private final TrackedEnergyStorage store = new TrackedEnergyStorage(this, BUFFER, RECEIVE, 0);
    private LazyOptional<IEnergyStorage> lazy = LazyOptional.empty();
    private boolean armed = false;
    private final TechEnergyData uiData =
            new TechEnergyData(store::getEnergyStored, store::getMaxEnergyStored, () -> armed ? 1 : 0, () -> 1);

    public ArcaneSentinelBlockEntity(BlockPos pos, BlockState state) { super(ModTech.ARCANE_SENTINEL_BE.get(), pos, state); }

    public static void tick(Level lvl, BlockPos pos, BlockState st, ArcaneSentinelBlockEntity be) {
        if (lvl.isClientSide || !(lvl instanceof ServerLevel sl)) return;
        boolean wasArmed = be.armed;
        be.armed = be.store.getEnergyStored() >= COST;

        if (be.armed) {
            AABB box = new AABB(pos).inflate(RADIUS);
            for (Player p : lvl.getEntitiesOfClass(Player.class, box)) {
                if (p.isCreative() || p.isSpectator() || !AntiMagic.castFlash(p)) continue;
                if (be.store.getEnergyStored() < COST) break;
                AntiMagic.clearCast(p);
                p.hurt(p.damageSources().magic(), DMG);
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, true));
                p.addEffect(new MobEffectInstance(ModEffects.ANTIMAGIA.get(), 120, 0, false, true));
                be.store.setStored(be.store.getEnergyStored() - COST);
                sl.sendParticles(ParticleTypes.SCULK_CHARGE_POP, p.getX(), p.getY() + 1.0, p.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.02);
                sl.playSound(null, pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 0.5F, 1.4F);
            }
        }

        if (be.armed != wasArmed) sl.setBlock(pos, st.setValue(br.com.murilo.liberthia.block.ArcaneSentinelBlock.LIT, be.armed), 3);
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
