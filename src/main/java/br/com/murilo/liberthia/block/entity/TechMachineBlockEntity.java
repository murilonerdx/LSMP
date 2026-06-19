package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.block.TechMachineBlock;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.menu.TechMachineMenu;
import br.com.murilo.liberthia.registry.ModTech;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * r180c — máquina tech com GUI (Mekanism-style): slot input(0) → output(1),
 * barra de energia + progresso. Auto-processa consumindo FE (recebido das células
 * pela rede). Tipo lido do {@link TechMachineBlock}.
 */
public class TechMachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0, SLOT_OUTPUT = 1;
    private static final int BUFFER = 100_000, FE_PER_TICK = 40, MAX_PROGRESS = 100;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != SLOT_OUTPUT && recipeFor(stack) != null;
        }
    };
    private LazyOptional<IItemHandler> lazyItems = LazyOptional.empty();
    private final TrackedEnergyStorage energy = new TrackedEnergyStorage(this, BUFFER, 10_000, 0);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();
    private int progress = 0;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> MAX_PROGRESS;
                case 2 -> (int) (1000L * energy.getEnergyStored() / Math.max(1, energy.getMaxEnergyStored()));
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 3; }
    };

    public TechMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModTech.TECH_MACHINE_BE.get(), pos, state);
    }

    private TechMachineBlock.Type type() {
        return getBlockState().getBlock() instanceof TechMachineBlock b ? b.getType() : TechMachineBlock.Type.CRUSHER;
    }

    public IItemHandler getInventory() { return inventory; }
    public int getEnergyStored() { return energy.getEnergyStored(); }
    public int getMaxEnergy() { return energy.getMaxEnergyStored(); }

    public static void tick(Level level, BlockPos pos, BlockState state, TechMachineBlockEntity be) {
        if (level.isClientSide) return;
        ItemStack in = be.inventory.getStackInSlot(SLOT_INPUT);
        ItemStack out = be.recipeFor(in);
        if (out == null || out.isEmpty()) {
            if (be.progress != 0) { be.progress = 0; be.setChanged(); }
            return;
        }
        ItemStack cur = be.inventory.getStackInSlot(SLOT_OUTPUT);
        boolean room = cur.isEmpty()
                || (ItemStack.isSameItemSameTags(cur, out) && cur.getCount() + out.getCount() <= cur.getMaxStackSize());
        if (!room || be.energy.getEnergyStored() < FE_PER_TICK) return;

        be.energy.setStored(be.energy.getEnergyStored() - FE_PER_TICK);
        be.progress++;
        if (be.progress >= MAX_PROGRESS) {
            be.progress = 0;
            be.inventory.extractItem(SLOT_INPUT, 1, false);
            be.inventory.insertItem(SLOT_OUTPUT, out.copy(), false);
        }
        be.setChanged();
    }

    /** Resolve o output pro input atual conforme o tipo da máquina (null = inválido). */
    private ItemStack recipeFor(ItemStack in) {
        if (in.isEmpty()) return null;
        switch (type()) {
            case CRUSHER: return crusher(in);
            case COMPRESSOR: return in.is(ModTech.STEEL_INGOT.get()) ? new ItemStack(ModTech.STEEL_PLATE.get()) : null;
            case ALLOY: return in.is(Items.IRON_INGOT) ? new ItemStack(ModTech.STEEL_INGOT.get()) : null;
            case SAWMILL: return sawmill(in);
            case SMELTER:
                if (level == null) return null;
                return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SimpleContainer(in.copy()), level)
                        .map(r -> r.getResultItem(level.registryAccess()).copy()).orElse(null);
            default: return null;
        }
    }

    private ItemStack crusher(ItemStack in) {
        if (in.is(Items.IRON_ORE) || in.is(Items.DEEPSLATE_IRON_ORE) || in.is(Items.RAW_IRON))       return new ItemStack(ModTech.IRON_DUST.get(), 2);
        if (in.is(Items.GOLD_ORE) || in.is(Items.DEEPSLATE_GOLD_ORE) || in.is(Items.RAW_GOLD))       return new ItemStack(ModTech.GOLD_DUST.get(), 2);
        if (in.is(Items.COPPER_ORE) || in.is(Items.DEEPSLATE_COPPER_ORE) || in.is(Items.RAW_COPPER)) return new ItemStack(ModTech.COPPER_DUST.get(), 2);
        if (in.is(Items.IRON_INGOT))   return new ItemStack(ModTech.IRON_DUST.get(), 1);
        if (in.is(Items.GOLD_INGOT))   return new ItemStack(ModTech.GOLD_DUST.get(), 1);
        if (in.is(Items.COPPER_INGOT)) return new ItemStack(ModTech.COPPER_DUST.get(), 1);
        if (in.is(ModTech.STEEL_INGOT.get())) return new ItemStack(ModTech.STEEL_DUST.get(), 1);
        return null;
    }

    private ItemStack sawmill(ItemStack in) {
        if (in.is(Items.OAK_LOG))      return new ItemStack(Items.OAK_PLANKS, 6);
        if (in.is(Items.SPRUCE_LOG))   return new ItemStack(Items.SPRUCE_PLANKS, 6);
        if (in.is(Items.BIRCH_LOG))    return new ItemStack(Items.BIRCH_PLANKS, 6);
        if (in.is(Items.JUNGLE_LOG))   return new ItemStack(Items.JUNGLE_PLANKS, 6);
        if (in.is(Items.ACACIA_LOG))   return new ItemStack(Items.ACACIA_PLANKS, 6);
        if (in.is(Items.DARK_OAK_LOG)) return new ItemStack(Items.DARK_OAK_PLANKS, 6);
        if (in.is(Items.MANGROVE_LOG)) return new ItemStack(Items.MANGROVE_PLANKS, 6);
        if (in.is(Items.CHERRY_LOG))   return new ItemStack(Items.CHERRY_PLANKS, 6);
        if (in.is(Items.CRIMSON_STEM)) return new ItemStack(Items.CRIMSON_PLANKS, 6);
        if (in.is(Items.WARPED_STEM))  return new ItemStack(Items.WARPED_PLANKS, 6);
        return null;
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItems.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazyItems = LazyOptional.of(() -> inventory); lazyEnergy = LazyOptional.of(() -> energy); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyItems.invalidate(); lazyEnergy.invalidate(); }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new TechMachineMenu(id, inv, this, this.data);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inv", inventory.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("Energy", energy.getEnergyStored());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        progress = tag.getInt("progress");
        energy.setStored(tag.getInt("Energy"));
    }
}
