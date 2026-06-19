package br.com.murilo.liberthia.magic.scribe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
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
 * r164: BlockEntity da Inscription Table. 3 slots:
 * <ul>
 *   <li>0 = Input (Tome/Grimoire/SpellBook)</li>
 *   <li>1 = Ink/Catalyst (item raro, depende do recipe selecionado)</li>
 *   <li>2 = Output (Spell Parchment ou item especial)</li>
 * </ul>
 *
 * <p>Selected recipe is stored as integer index. Default 0.
 */
public class InscriptionTableBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_INK = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int TOTAL_SLOTS = 3;

    /** Índice do recipe selecionado no panel esquerdo. */
    public int selectedRecipe = 0;

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                if (slot != SLOT_OUTPUT) updateOutput();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != SLOT_OUTPUT;  // input + ink livres, output só read
        }
    };

    private LazyOptional<IItemHandler> lazyHandler = LazyOptional.empty();

    public InscriptionTableBlockEntity(BlockPos pos, BlockState state) {
        super(br.com.murilo.liberthia.registry.ModBlockEntities.INSCRIPTION_TABLE.get(), pos, state);
    }

    public IItemHandler getItemHandler() { return inventory; }

    public void setSelectedRecipe(int idx) {
        var recipes = InscriptionRecipes.LIST;
        if (idx >= 0 && idx < recipes.size()) {
            this.selectedRecipe = idx;
            updateOutput();
            setChanged();
            // Re-sync pro client — senão o painel não atualiza highlight
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag() {
        var tag = super.getUpdateTag();
        tag.put("inv", inventory.serializeNBT());
        tag.putInt("selectedRecipe", selectedRecipe);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /** Tenta produzir output baseado no recipe selecionado e ingredients. */
    public void updateOutput() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        ItemStack ink = inventory.getStackInSlot(SLOT_INK);
        int idx = Math.max(0, Math.min(selectedRecipe, InscriptionRecipes.LIST.size() - 1));
        var recipe = InscriptionRecipes.LIST.get(idx);
        if (recipe.matches(input, ink)) {
            inventory.setStackInSlot(SLOT_OUTPUT, recipe.result().copy());
        } else {
            inventory.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
        }
    }

    /** Consome 1 de cada input quando retira output. */
    public void onOutputTaken() {
        inventory.getStackInSlot(SLOT_INPUT).shrink(1);
        inventory.getStackInSlot(SLOT_INK).shrink(1);
        updateOutput();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.inscription_table");
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new br.com.murilo.liberthia.magic.scribe.InscriptionTableMenu(id, inv, this);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyHandler.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazyHandler = LazyOptional.of(() -> inventory); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyHandler.invalidate(); }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inv", inventory.serializeNBT());
        tag.putInt("selectedRecipe", selectedRecipe);
    }
    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        if (tag.contains("selectedRecipe")) selectedRecipe = tag.getInt("selectedRecipe");
    }
}
