package br.com.murilo.liberthia.observation.block;

import br.com.murilo.liberthia.observation.item.GrimoireOfObservationItem;
import br.com.murilo.liberthia.observation.item.ObservationTomeItem;
import br.com.murilo.liberthia.observation.item.PrebuiltTomeItem;
import br.com.murilo.liberthia.observation.item.SpellParchmentItem;
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
 * v0.1.22 r77: <b>Spell Binding Pedestal</b> — bloco que combina Tome/Grimório
 * + Spell Parchment, gerando Tome/Grimório com a recipe imprintada.
 *
 * <h2>3 slots</h2>
 * <ul>
 *   <li>0: Book (Tome or Grimoire) input</li>
 *   <li>1: Spell Parchment (com recipe válido)</li>
 *   <li>2: Output (Book com recipe imprintada)</li>
 * </ul>
 */
public class SpellBindingPedestalBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_BOOK = 0;
    public static final int SLOT_PARCHMENT = 1;
    public static final int SLOT_OUTPUT = 2;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
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
                // r164: também aceita PrebuiltTomeItem (livros prontos) no slot Book —
                // antes só ObservationTome/Grimoire eram aceitos, o que confundia o user.
                case SLOT_BOOK -> stack.getItem() instanceof ObservationTomeItem
                                  || stack.getItem() instanceof GrimoireOfObservationItem
                                  || stack.getItem() instanceof PrebuiltTomeItem;
                // r164: aceita parchment vazio OU com recipe — antes só com recipe.
                // Validação do output continua exigindo recipe (updateOutput).
                case SLOT_PARCHMENT -> stack.getItem() instanceof SpellParchmentItem;
                case SLOT_OUTPUT -> false;
                default -> false;
            };
        }
    };

    private LazyOptional<IItemHandler> lazyHandler = LazyOptional.empty();

    public SpellBindingPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(br.com.murilo.liberthia.registry.ModBlockEntities.SPELL_BINDING_PEDESTAL.get(), pos, state);
    }

    public IItemHandler getItemHandler() { return inventory; }

    private void updateOutput() {
        ItemStack book = inventory.getStackInSlot(SLOT_BOOK);
        ItemStack parch = inventory.getStackInSlot(SLOT_PARCHMENT);

        if (book.isEmpty() || parch.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }
        if (!(parch.getItem() instanceof SpellParchmentItem)) {
            inventory.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }
        var spell = SpellParchmentItem.buildSpell(parch);
        if (spell == null || spell.validate() != null) {
            inventory.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }

        // Constrói output: clone do book com recipe imprintada
        ItemStack output = book.copy();
        output.setCount(1);

        if (book.getItem() instanceof GrimoireOfObservationItem) {
            // Usa NBT do Grimório
            var tag = output.getOrCreateTag();
            var list = new net.minecraft.nbt.ListTag();
            for (String id : SpellParchmentItem.getRecipe(parch))
                list.add(net.minecraft.nbt.StringTag.valueOf(id));
            tag.put(GrimoireOfObservationItem.NBT_CUSTOM_RECIPE, list);
            tag.putString(GrimoireOfObservationItem.NBT_CUSTOM_NAME, spell.name());
            tag.putInt(GrimoireOfObservationItem.NBT_CUSTOM_COLOR, spell.color());
        } else if (book.getItem() instanceof ObservationTomeItem) {
            ObservationTomeItem.imprintFromParchment(output, parch);
        } else if (book.getItem() instanceof PrebuiltTomeItem) {
            // r164: PrebuiltTome aceita imprint custom — substitui spell built-in pelo
            // parchment recipe via custom NBT (mesmo schema do Grimoire).
            // O PrebuiltTomeItem.use() pode ser estendido pra ler esse NBT, mas
            // por ora preservamos a spell built-in e ADICIONAMOS o recipe alternativo.
            var tag = output.getOrCreateTag();
            var list = new net.minecraft.nbt.ListTag();
            for (String id : SpellParchmentItem.getRecipe(parch))
                list.add(net.minecraft.nbt.StringTag.valueOf(id));
            tag.put("tome.custom_recipe", list);
            tag.putString("tome.custom_name", spell.name());
            tag.putInt("tome.custom_color", spell.color());
        }

        inventory.setStackInSlot(SLOT_OUTPUT, output);
    }

    /** Chamado quando jogador pega o output — consome book + parchment. */
    public void onOutputTaken() {
        inventory.getStackInSlot(SLOT_BOOK).shrink(1);
        inventory.getStackInSlot(SLOT_PARCHMENT).shrink(1);
        updateOutput();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.spell_binding_pedestal");
    }

    @Nullable @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player player) {
        return new br.com.murilo.liberthia.menu.SpellBindingPedestalMenu(containerId, inv, this);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyHandler.cast();
        return super.getCapability(cap, side);
    }
    @Override
    public void onLoad() { super.onLoad(); lazyHandler = LazyOptional.of(() -> inventory); }
    @Override
    public void invalidateCaps() { super.invalidateCaps(); lazyHandler.invalidate(); }

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
