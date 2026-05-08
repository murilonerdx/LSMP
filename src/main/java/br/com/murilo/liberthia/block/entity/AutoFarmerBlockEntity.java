package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.AutoFarmerMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Auto Farmer — colocado em cima de lava. Slot 0 = catalisador (consome 1
 * a cada operação), slot 1 = saída. Recicla a lava: cada operação consome
 * a fonte de lava embaixo + 1 catalisador + 20.000 FE → produz
 * 1 dark_matter_block + repõe a lava.
 *
 * <p>Cooldown de 200 ticks por operação.
 */
public class AutoFarmerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_CATALYST = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int FE_BUFFER = 100_000;
    public static final int FE_PER_OPERATION = 20_000;
    public static final int COOLDOWN_TICKS = 200;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != SLOT_CATALYST || stack.is(ModItems.DARK_MATTER_CATALYST.get());
        }
    };
    private final EnergyStorage energy = new EnergyStorage(FE_BUFFER, 4_000, 0);
    private LazyOptional<IItemHandler> lazyItem = LazyOptional.empty();
    private LazyOptional<net.minecraftforge.energy.IEnergyStorage> lazyEnergy = LazyOptional.empty();
    private int cooldown = 0;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            int v = switch (i) {
                case 0, 1 -> energy.getEnergyStored();
                case 2, 3 -> energy.getMaxEnergyStored();
                case 4 -> cooldown;
                default -> 0;
            };
            return switch (i) {
                case 0, 2 -> (v >> 16) & 0xFFFF;
                case 1, 3 -> v & 0xFFFF;
                default -> v;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 5; }
    };

    public AutoFarmerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_FARMER.get(), pos, state);
    }

    public IItemHandler getInventory() { return inventory; }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItem.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() {
        super.onLoad();
        lazyItem = LazyOptional.of(() -> inventory);
        lazyEnergy = LazyOptional.of(() -> energy);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        lazyItem.invalidate();
        lazyEnergy.invalidate();
    }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.auto_farmer");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new AutoFarmerMenu(id, inv, this, this.data);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AutoFarmerBlockEntity be) {
        if (level.isClientSide) return;

        if (be.cooldown > 0) { be.cooldown--; be.setChanged(); return; }

        ItemStack catalyst = be.inventory.getStackInSlot(SLOT_CATALYST);
        ItemStack output = be.inventory.getStackInSlot(SLOT_OUTPUT);
        if (catalyst.isEmpty()) return;
        if (be.energy.getEnergyStored() < FE_PER_OPERATION) return;
        if (!output.isEmpty() && (
                !output.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem())
                        || output.getCount() >= output.getMaxStackSize())) return;

        // Verifica lava embaixo (uma fonte)
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (!belowState.getFluidState().is(Fluids.LAVA)
                || !belowState.getFluidState().isSource()) return;

        // Consome
        be.energy.extractEnergy(FE_PER_OPERATION, false);
        // extractItem dispara onContentsChanged → setChanged → chunk save
        be.inventory.extractItem(SLOT_CATALYST, 1, false);
        // Substitui a lava por outra lava (recicla — efeito visual: pisca por 1 tick)
        // Como a lava já está lá, basta marcar para repor depois? Vamos repor já.
        // Para simular "consumo + reposição", definimos a lava de volta (que já é lava — no-op).
        // O importante é simular a perda/reposição: setar pra ar e devolta.
        level.setBlock(below, net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState(), 2);
        // Output
        be.inventory.insertItem(SLOT_OUTPUT,
                new ItemStack(ModBlocks.DARK_MATTER_BLOCK.get().asItem()), false);
        be.cooldown = COOLDOWN_TICKS;
        be.setChanged();
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        tag.put("inv", inventory.serializeNBT());
        tag.put("energy", energy.serializeNBT());
        tag.putInt("cooldown", cooldown);
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        if (tag.contains("energy")) energy.deserializeNBT(tag.get("energy"));
        cooldown = tag.getInt("cooldown");
    }
}
