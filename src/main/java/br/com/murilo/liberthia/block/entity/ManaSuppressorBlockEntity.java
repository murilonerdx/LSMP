package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.magic.antimagic.AntiMagic;
import br.com.murilo.liberthia.registry.ModTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
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
 * r182 — <b>Selo Supressor de Mana</b>: enquanto tiver FE, cria um campo (raio
 * {@value #RADIUS}) que IMPEDE conjuração — quem está dentro não consegue gastar mana
 * (cast falha, ver {@link AntiMagic#isSuppressed}). Consome FE/tick. GUI de energia
 * (layout CONSUMER). Recebe FE da rede.
 */
public class ManaSuppressorBlockEntity extends BlockEntity implements MenuProvider, ITechEnergyUI {
    private static final int BUFFER = 300_000, RECEIVE = 10_000, COST = 24, RADIUS = 8;

    private final TrackedEnergyStorage store = new TrackedEnergyStorage(this, BUFFER, RECEIVE, 0);
    private LazyOptional<IEnergyStorage> lazy = LazyOptional.empty();
    private boolean active = false;
    private final TechEnergyData uiData =
            new TechEnergyData(store::getEnergyStored, store::getMaxEnergyStored, () -> active ? 1 : 0, () -> 1);

    public ManaSuppressorBlockEntity(BlockPos pos, BlockState state) { super(ModTech.MANA_SUPPRESSOR_BE.get(), pos, state); }

    public static void tick(Level lvl, BlockPos pos, BlockState st, ManaSuppressorBlockEntity be) {
        if (lvl.isClientSide || !(lvl instanceof ServerLevel sl)) return;
        boolean wasActive = be.active;
        be.active = be.store.getEnergyStored() >= COST;

        if (be.active) {
            AABB box = new AABB(pos).inflate(RADIUS);
            boolean any = false;
            for (Player p : lvl.getEntitiesOfClass(Player.class, box)) {
                if (p.isCreative() || p.isSpectator()) continue;
                if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > (double) RADIUS * RADIUS) continue;
                AntiMagic.suppress(p, 10);
                any = true;
            }
            be.store.setStored(be.store.getEnergyStored() - COST);
            if (any && sl.getGameTime() % 10 == 0)
                sl.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.0);
        }

        if (be.active != wasActive) sl.setBlock(pos, st.setValue(br.com.murilo.liberthia.block.ManaSuppressorBlock.LIT, be.active), 3);
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
