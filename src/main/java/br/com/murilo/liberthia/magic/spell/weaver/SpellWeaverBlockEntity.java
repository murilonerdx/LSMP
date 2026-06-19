package br.com.murilo.liberthia.magic.spell.weaver;

import br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem;
import br.com.murilo.liberthia.magic.spell.composition.SpellComposition;
import br.com.murilo.liberthia.magic.spell.composition.SpellModifier;
import br.com.murilo.liberthia.magic.spell.composition.SpellModifierItem;
import br.com.murilo.liberthia.magic.spell.vfx.HelixSpawner;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * v0.1.151 r119: <b>SpellWeaverBlockEntity</b> — 9 slots:
 * <ul>
 *   <li>Slot 0: Base spell scroll (input)</li>
 *   <li>Slots 1-7: Modifier glyphs</li>
 *   <li>Slot 8: Output (composed scroll)</li>
 * </ul>
 *
 * <p>Auto-craft quando base preenchido. Cada modifier glyph adiciona stack
 * ao SpellComposition. Custo de mana cresce multiplicativamente.
 */
public class SpellWeaverBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_COUNT = 9;
    public static final int BASE_SLOT = 0;
    public static final int OUTPUT_SLOT = 8;
    public static final int FIRST_MOD_SLOT = 1;
    public static final int LAST_MOD_SLOT = 7;
    private static final String NBT_INVENTORY = "Inventory";

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // r137 fix #9: level pode ser null se NBT deserializa antes do BE entrar no mundo
            // (Minecraft chama load() antes de setLevel). NPE crashava no startup.
            if (slot != OUTPUT_SLOT && level != null && !level.isClientSide) {
                checkCraft();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == OUTPUT_SLOT) return false;
            if (slot == BASE_SLOT) return stack.getItem() instanceof UniversalSpellScrollItem;
            if (slot >= FIRST_MOD_SLOT && slot <= LAST_MOD_SLOT) {
                return stack.getItem() instanceof SpellModifierItem;
            }
            return false;
        }

        @Override
        public @org.jetbrains.annotations.NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // r137 fix #15: quando player extrai output via hopper/automation/menu shift,
            // o consume nao acontecia (Menu fazia mas hoppers nao). Hook centralizado.
            ItemStack extracted = super.extractItem(slot, amount, simulate);
            if (!simulate && slot == OUTPUT_SLOT && !extracted.isEmpty()) {
                consumeIngredientsOnExtract();
            }
            return extracted;
        }
    };

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> items);
    private int tickCounter = 0;

    public SpellWeaverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPELL_WEAVER.get(), pos, state);
    }

    public ItemStackHandler getItems() { return items; }

    /** Reconstrói o output baseado no base + modifiers atuais. */
    private void checkCraft() {
        // Output já existe — não regenera
        if (!items.getStackInSlot(OUTPUT_SLOT).isEmpty()) return;

        ItemStack base = items.getStackInSlot(BASE_SLOT);
        if (base.isEmpty() || !(base.getItem() instanceof UniversalSpellScrollItem usi)) return;
        String baseSpellId = usi.spellId(base);
        if (baseSpellId == null) return;

        // Coleta modifiers
        List<SpellModifier> mods = new ArrayList<>();
        for (int i = FIRST_MOD_SLOT; i <= LAST_MOD_SLOT; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.isEmpty() || !(s.getItem() instanceof SpellModifierItem smi)) continue;
            // Cada item do stack conta como 1 stack (count)
            for (int c = 0; c < s.getCount(); c++) {
                mods.add(smi.modifier);
            }
        }

        SpellComposition composition = new SpellComposition(baseSpellId, mods);

        // Valida: cada modifier não pode exceder maxStacks; apolão exige EPIC base
        java.util.Map<SpellModifier, Integer> stackMap = composition.stackCounts();
        for (var entry : stackMap.entrySet()) {
            if (entry.getValue() > entry.getKey().maxStacks) return;
        }
        if (stackMap.containsKey(SpellModifier.APOLAO)) {
            var def = composition.baseDef();
            if (def == null || def.rarity != net.minecraft.world.item.Rarity.EPIC) return;
        }

        // Clona o base + grava composition NBT
        ItemStack output = base.copy();
        output.setCount(1);
        composition.writeToStack(output);
        items.setStackInSlot(OUTPUT_SLOT, output);
        setChanged();
    }

    /** Quando player extrai o output, consumir base e modifier glyphs. */
    public void consumeIngredientsOnExtract() {
        ItemStack base = items.getStackInSlot(BASE_SLOT);
        if (!base.isEmpty()) base.shrink(1);
        for (int i = FIRST_MOD_SLOT; i <= LAST_MOD_SLOT; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) s.shrink(s.getCount());
        }
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpellWeaverBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        be.tickCounter++;
        // VFX: orbital de partículas sobre o bloco quando crafting válido
        if (!be.items.getStackInSlot(be.OUTPUT_SLOT).isEmpty() && be.tickCounter % 3 == 0) {
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5);
            double phase = (be.tickCounter * 0.2) % (Math.PI * 2);
            HelixSpawner.spawnRing(sl, center,
                    br.com.murilo.liberthia.magic.school.SpellSchool.HOLY,
                    0.5, 16, phase);
            HelixSpawner.spawnRing(sl, center.add(0, 0.3, 0),
                    br.com.murilo.liberthia.magic.school.SpellSchool.ELDRITCH,
                    0.4, 12, -phase);
        } else if (be.hasBase() && be.tickCounter % 12 == 0) {
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    center.x, center.y, center.z, 3, 0.2, 0.2, 0.2, 0.05);
        }
    }

    private boolean hasBase() {
        return !items.getStackInSlot(BASE_SLOT).isEmpty();
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
        tag.put(NBT_INVENTORY, items.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NBT_INVENTORY)) items.deserializeNBT(tag.getCompound(NBT_INVENTORY));
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
        return Component.translatable("container.liberthia.spell_weaver");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SpellWeaverMenu(id, inv, this);
    }
}
