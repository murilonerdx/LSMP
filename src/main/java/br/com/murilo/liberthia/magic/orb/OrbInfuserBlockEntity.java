package br.com.murilo.liberthia.magic.orb;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r174: BlockEntity do <b>Infusor de Orbs</b>. 5 slots de input (itens raros) +
 * 1 slot de output (preview do Orb Customizado). Ao retirar o output, consome 1
 * de cada input preenchido.
 */
public class OrbInfuserBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INPUT_COUNT = 5;
    public static final int SLOT_OUTPUT = 5;
    public static final int TOTAL = 6;

    public final ItemStackHandler items = new ItemStackHandler(TOTAL) {
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != SLOT_OUTPUT;
        }
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (slot != SLOT_OUTPUT) recompute();
        }
    };

    public OrbInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORB_INFUSER.get(), pos, state);
    }

    private List<ItemStack> inputs() {
        return List.of(
                items.getStackInSlot(0), items.getStackInSlot(1), items.getStackInSlot(2),
                items.getStackInSlot(3), items.getStackInSlot(4));
    }

    public void recompute() {
        var fx = OrbIngredients.build(inputs());
        items.setStackInSlot(SLOT_OUTPUT, fx.isEmpty() ? ItemStack.EMPTY : CustomOrbItem.create(fx));
    }

    public void consumeInputs() {
        for (int i = 0; i < INPUT_COUNT; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) { s.shrink(1); items.setStackInSlot(i, s); }
        }
    }

    public void dropContents() {
        SimpleContainer c = new SimpleContainer(INPUT_COUNT);
        for (int i = 0; i < INPUT_COUNT; i++) c.setItem(i, items.getStackInSlot(i));
        if (level != null) Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("block.liberthia.orb_infuser");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new OrbInfuserMenu(id, inv, this);
    }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("items", items.serializeNBT());
    }
    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("items")) items.deserializeNBT(tag.getCompound("items"));
    }
}
