package br.com.murilo.liberthia.magic.spell.mutator;

import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellLibrary;
import br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.162 r138: <b>SpellMutatorBlockEntity</b> — 3 slots:
 * <ul>
 *   <li>Slot 0: Scroll A (input)</li>
 *   <li>Slot 1: Scroll B (input)</li>
 *   <li>Slot 2: Output (híbrido)</li>
 * </ul>
 *
 * <p>Quando ambos inputs preenchidos, gera output híbrido automaticamente.
 * Take output consome os 2 inputs.
 */
public class SpellMutatorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_A = 0;
    public static final int SLOT_B = 1;
    public static final int SLOT_OUT = 2;
    public static final int SLOT_COUNT = 3;
    private static final String NBT_INV = "Inventory";

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot != SLOT_OUT && level != null && !level.isClientSide) {
                regenerateOutput();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_OUT) return false;
            return stack.getItem() instanceof UniversalSpellScrollItem;
        }

        @Override
        public @org.jetbrains.annotations.NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack extracted = super.extractItem(slot, amount, simulate);
            if (!simulate && slot == SLOT_OUT && !extracted.isEmpty()) {
                consumeInputs();
            }
            return extracted;
        }
    };

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> items);
    private int tickCounter = 0;

    public SpellMutatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPELL_MUTATOR.get(), pos, state);
    }

    public ItemStackHandler getItems() { return items; }

    /** Gera o output híbrido baseado nos 2 inputs. */
    private void regenerateOutput() {
        if (!items.getStackInSlot(SLOT_OUT).isEmpty()) return;

        ItemStack a = items.getStackInSlot(SLOT_A);
        ItemStack b = items.getStackInSlot(SLOT_B);
        if (a.isEmpty() || b.isEmpty()) return;
        if (!(a.getItem() instanceof UniversalSpellScrollItem usiA)) return;
        if (!(b.getItem() instanceof UniversalSpellScrollItem usiB)) return;

        String idA = usiA.spellId(a);
        String idB = usiB.spellId(b);
        if (idA == null || idB == null) return;
        if (idA.equals(idB)) return; // não mutar o mesmo spell consigo mesmo

        SpellDef defA = SpellLibrary.get(idA);
        SpellDef defB = SpellLibrary.get(idB);
        if (defA == null || defB == null) return;

        // r138: cria um SpellDef híbrido em runtime e registra com id custom.
        // Pra simplicidade: cria scroll cópia do A com NBT custom indicando override stats
        ItemStack output = new ItemStack(a.getItem());
        output.setCount(1);
        CompoundTag tag = output.getOrCreateTag();
        // Hybrid marker
        tag.putString("liberthia.hybrid_a", idA);
        tag.putString("liberthia.hybrid_b", idB);
        // Override stats (overlay applied by HybridSpellResolver no read)
        tag.putFloat("liberthia.hybrid_damage", (defA.damage + defB.damage) / 2F);
        tag.putInt("liberthia.hybrid_mana", Math.max(defA.manaCost, defB.manaCost) + 5);
        tag.putInt("liberthia.hybrid_cooldown", (defA.cooldownTicks + defB.cooldownTicks) / 2);
        tag.putFloat("liberthia.hybrid_range", Math.max(defA.range, defB.range));
        // Use spell A's id como o que vai resolver — efeito visual e cast logic
        // Display name = "A ⨯ B"
        tag.putString("liberthia.hybrid_name",
                defA.name + " ⨯ " + defB.name);

        items.setStackInSlot(SLOT_OUT, output);
        setChanged();
    }

    /** Quando player extrai o output, consome 1 de cada input. */
    public void consumeInputs() {
        ItemStack a = items.getStackInSlot(SLOT_A);
        if (!a.isEmpty()) a.shrink(1);
        ItemStack b = items.getStackInSlot(SLOT_B);
        if (!b.isEmpty()) b.shrink(1);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpellMutatorBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        be.tickCounter++;
        // VFX: 1 partícula DRAGON_BREATH no topo quando ambos inputs presentes
        if (be.tickCounter % 4 == 0
                && !be.items.getStackInSlot(SLOT_A).isEmpty()
                && !be.items.getStackInSlot(SLOT_B).isEmpty()) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    2, 0.2, 0.1, 0.2, 0.02);
        }
    }

    public void drops() {
        if (level == null) return;
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level,
                        worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), s);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INV, items.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NBT_INV)) items.deserializeNBT(tag.getCompound(NBT_INV));
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.liberthia.spell_mutator");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SpellMutatorMenu(id, inv, this);
    }
}
