package br.com.murilo.liberthia.magic.scribe;

import br.com.murilo.liberthia.magic.school.SpellSchool;
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
 * r165: <b>Scroll Forge BlockEntity</b> — 2 slots:
 * <ul>
 *   <li>0 = Focus input (Focus_Fire / Focus_Ice / …)</li>
 *   <li>1 = Output scroll (auto-preenchido conforme o Focus colocado)</li>
 * </ul>
 *
 * <p>Quando o player coloca um Focus no slot 0, o slot 1 auto-preenche com
 * o Scroll correspondente. O player então tira o scroll (slot 1) e o forge
 * consome o Focus (slot 0).
 */
public class ScrollForgeBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_FOCUS  = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int TOTAL_SLOTS = 2;

    /** Tick counter — client-side animation pulse. */
    public int animTick = 0;

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide() && slot == SLOT_FOCUS) {
                updateOutput();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == SLOT_OUTPUT) return false;
            if (slot == SLOT_FOCUS)  return identifySchool(stack) != null;
            return true;
        }
    };

    private LazyOptional<IItemHandler> lazyHandler = LazyOptional.empty();

    public ScrollForgeBlockEntity(BlockPos pos, BlockState state) {
        super(br.com.murilo.liberthia.registry.ModBlockEntities.SCROLL_FORGE.get(), pos, state);
    }

    public IItemHandler getItemHandler() { return inventory; }

    // ── Output update ────────────────────────────────────────────────────────

    /** Auto-generates output scroll based on Focus in slot 0. */
    public void updateOutput() {
        ItemStack focus = inventory.getStackInSlot(SLOT_FOCUS);
        SpellSchool school = identifySchool(focus);
        if (school == null) {
            inventory.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }
        net.minecraft.world.item.Item scrollItem = getScrollForSchool(school);
        if (scrollItem == null) {
            inventory.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }
        inventory.setStackInSlot(SLOT_OUTPUT, new ItemStack(scrollItem));
    }

    /** Called when player takes the output scroll — consume 1 focus. */
    public void onOutputTaken() {
        inventory.getStackInSlot(SLOT_FOCUS).shrink(1);
        updateOutput();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @Nullable
    public static SpellSchool identifySchool(ItemStack stack) {
        if (stack.isEmpty()) return null;
        net.minecraft.world.item.Item item = stack.getItem();
        try {
            if (item == br.com.murilo.liberthia.registry.ModItems.FOCUS_FIRE.get())      return SpellSchool.FIRE;
            if (item == br.com.murilo.liberthia.registry.ModItems.FOCUS_ICE.get())       return SpellSchool.ICE;
            if (item == br.com.murilo.liberthia.registry.ModItems.FOCUS_LIGHTNING.get()) return SpellSchool.LIGHTNING;
            if (item == br.com.murilo.liberthia.registry.ModItems.FOCUS_BLOOD.get())     return SpellSchool.BLOOD;
            if (item == br.com.murilo.liberthia.registry.ModItems.FOCUS_ELDRITCH.get())  return SpellSchool.ELDRITCH;
            if (item == br.com.murilo.liberthia.registry.ModItems.FOCUS_HOLY.get())      return SpellSchool.HOLY;
            if (item == br.com.murilo.liberthia.registry.ModItems.FOCUS_NATURE.get())    return SpellSchool.NATURE;
        } catch (Throwable ignored) {}
        return null;
    }

    @Nullable
    public static net.minecraft.world.item.Item getScrollForSchool(SpellSchool s) {
        try {
            return switch (s) {
                case FIRE      -> br.com.murilo.liberthia.registry.ModItems.SCROLL_FIRE.get();
                case ICE       -> br.com.murilo.liberthia.registry.ModItems.SCROLL_ICE.get();
                case LIGHTNING -> br.com.murilo.liberthia.registry.ModItems.SCROLL_LIGHTNING.get();
                case BLOOD     -> br.com.murilo.liberthia.registry.ModItems.SCROLL_BLOOD.get();
                case ELDRITCH  -> br.com.murilo.liberthia.registry.ModItems.SCROLL_ELDRITCH.get();
                case HOLY      -> br.com.murilo.liberthia.registry.ModItems.SCROLL_HOLY.get();
                case NATURE    -> br.com.murilo.liberthia.registry.ModItems.SCROLL_NATURE.get();
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    // ── MenuProvider ─────────────────────────────────────────────────────────

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.scroll_forge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new ScrollForgeMenu(id, inv, this);
    }

    // ── Capability ───────────────────────────────────────────────────────────

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override public void onLoad()       { super.onLoad(); lazyHandler = LazyOptional.of(() -> inventory); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyHandler.invalidate(); }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    public CompoundTag getUpdateTag() {
        var tag = super.getUpdateTag();
        tag.put("inv", inventory.serializeNBT());
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
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
