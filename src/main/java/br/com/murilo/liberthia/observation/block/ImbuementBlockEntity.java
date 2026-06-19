package br.com.murilo.liberthia.observation.block;

import br.com.murilo.liberthia.observation.item.ImbuedSwordHandler;
import br.com.murilo.liberthia.observation.item.SpellParchmentItem;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r73: <b>Imbuement Block Entity</b>.
 *
 * <h2>4 slots</h2>
 * <ul>
 *   <li>0: Sword input (qualquer SwordItem)</li>
 *   <li>1: Spell Parchment com recipe</li>
 *   <li>2: Soul Fragment (catalisador)</li>
 *   <li>3: Output (sword imbued)</li>
 * </ul>
 *
 * <p>Quando os 3 inputs estão presentes E válidos, output é populated automático.
 * Taking output consume todos os inputs.
 */
public class ImbuementBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_SWORD = 0;
    public static final int SLOT_PARCHMENT = 1;
    public static final int SLOT_FRAGMENT = 2;
    public static final int SLOT_OUTPUT = 3;

    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                if (slot != SLOT_OUTPUT) updateOutput();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_SWORD -> stack.getItem() instanceof SwordItem && !ImbuedSwordHandler.isImbued(stack);
                case SLOT_PARCHMENT -> stack.getItem() instanceof SpellParchmentItem && !SpellParchmentItem.getRecipe(stack).isEmpty();
                case SLOT_FRAGMENT -> stack.is(ModItems.SOUL_FRAGMENT.get());
                case SLOT_OUTPUT -> false;
                default -> false;
            };
        }
    };

    private LazyOptional<IItemHandler> lazyHandler = LazyOptional.empty();

    public ImbuementBlockEntity(BlockPos pos, BlockState state) {
        super(br.com.murilo.liberthia.registry.ModBlockEntities.IMBUEMENT_TABLE.get(), pos, state);
    }

    public IItemHandler getItemHandler() { return inventory; }

    private void updateOutput() {
        ItemStack sword = inventory.getStackInSlot(SLOT_SWORD);
        ItemStack parch = inventory.getStackInSlot(SLOT_PARCHMENT);
        ItemStack frag = inventory.getStackInSlot(SLOT_FRAGMENT);

        if (sword.isEmpty() || parch.isEmpty() || frag.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }
        if (!(sword.getItem() instanceof SwordItem)) {
            inventory.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }
        if (ImbuedSwordHandler.isImbued(sword)) {
            inventory.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }
        var spell = SpellParchmentItem.buildSpell(parch);
        if (spell == null || spell.validate() != null) {
            inventory.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }

        // Constrói output
        ItemStack output = sword.copy();
        output.setCount(1);
        ImbuedSwordHandler.imbueSword(output, parch);
        inventory.setStackInSlot(SLOT_OUTPUT, output);
    }

    /** Chamado quando jogador pega o output — consome inputs. */
    public void onOutputTaken() {
        inventory.getStackInSlot(SLOT_SWORD).shrink(1);
        inventory.getStackInSlot(SLOT_PARCHMENT).shrink(1);
        inventory.getStackInSlot(SLOT_FRAGMENT).shrink(1);
        updateOutput();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.imbuement_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player player) {
        return new br.com.murilo.liberthia.menu.ImbuementMenu(containerId, inv, this);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyHandler = LazyOptional.of(() -> inventory);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyHandler.invalidate();
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) inv.setItem(i, inventory.getStackInSlot(i));
        if (level != null) Containers.dropContents(level, worldPosition, inv);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inv", inventory.serializeNBT());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
    }
}
