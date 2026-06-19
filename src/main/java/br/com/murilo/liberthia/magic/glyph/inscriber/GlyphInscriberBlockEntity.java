package br.com.murilo.liberthia.magic.glyph.inscriber;

import br.com.murilo.liberthia.magic.glyph.SpiritGlyphRecipes;
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

/**
 * v0.1.150 r118: <b>GlyphInscriberBlockEntity</b> — bloco com GUI 4 input + 1 output.
 * Auto-craft: quando os 4 inputs casam uma {@link SpiritGlyphRecipes.Recipe}, o
 * output é gerado e VFX rico spawna no top.
 *
 * <h2>Crafting flow</h2>
 * <ol>
 *   <li>Player abre GUI (right-click — sem sneak)</li>
 *   <li>Drag-drop reagents nos 4 slots input</li>
 *   <li>Quando {@code checkRecipe} encontra match, output preenchido</li>
 *   <li>Player tira output do slot → consome inputs proporcionalmente</li>
 *   <li>Durante o crafting (output preenchido): HelixSpawner spin no top</li>
 * </ol>
 *
 * <p>Inventory NBT save/load + Forge ItemHandler capability pra hoppers.
 */
public class GlyphInscriberBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_COUNT = 5;
    public static final int OUTPUT_SLOT = 4;
    private static final String NBT_INVENTORY = "Inventory";

    /** ItemStackHandler 5 slots: 0..3 inputs, 4 output. */
    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // Re-check recipe quando inputs mudam (não o output)
            if (slot != OUTPUT_SLOT && !level.isClientSide) {
                checkRecipe();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == OUTPUT_SLOT) return false; // não dá pra inserir manualmente
            return stack.getItem() instanceof br.com.murilo.liberthia.magic.glyph.SpiritReagentItem;
        }
    };

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> items);
    private int tickCounter = 0;
    private boolean recipeReady = false;

    public GlyphInscriberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GLYPH_INSCRIBER.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return items;
    }

    /** Conta total de items em cada slot do tipo specific. */
    private int countItem(net.minecraft.world.item.Item want) {
        int n = 0;
        for (int i = 0; i < OUTPUT_SLOT; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.is(want)) n += s.getCount();
        }
        return n;
    }

    private void checkRecipe() {
        // Já tem output: não faz nada
        if (!items.getStackInSlot(OUTPUT_SLOT).isEmpty()) return;

        // Procura match
        for (SpiritGlyphRecipes.Recipe r : SpiritGlyphRecipes.all()) {
            boolean match = true;
            for (SpiritGlyphRecipes.Ingredient ing : r.ingredients) {
                if (countItem(ing.item.get()) < ing.count) { match = false; break; }
            }
            if (match) {
                // Consome reagents + spawna output
                java.util.Map<net.minecraft.world.item.Item, Integer> needed = new java.util.HashMap<>();
                for (SpiritGlyphRecipes.Ingredient ing : r.ingredients) {
                    needed.merge(ing.item.get(), ing.count, Integer::sum);
                }
                for (int i = 0; i < OUTPUT_SLOT && !needed.isEmpty(); i++) {
                    ItemStack s = items.getStackInSlot(i);
                    if (s.isEmpty()) continue;
                    Integer want = needed.get(s.getItem());
                    if (want == null || want <= 0) continue;
                    int take = Math.min(want, s.getCount());
                    s.shrink(take);
                    int remaining = want - take;
                    if (remaining <= 0) needed.remove(s.getItem());
                    else needed.put(s.getItem(), remaining);
                }
                items.setStackInSlot(OUTPUT_SLOT, r.output.get());
                recipeReady = true;
                setChanged();
                return;
            }
        }
        recipeReady = false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GlyphInscriberBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        be.tickCounter++;
        // VFX no top se output cheio (visual de glyph pronto)
        if (!be.items.getStackInSlot(OUTPUT_SLOT).isEmpty() && be.tickCounter % 4 == 0) {
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5);
            double phase = (be.tickCounter * 0.15) % (Math.PI * 2);
            HelixSpawner.spawnRing(sl, center,
                    br.com.murilo.liberthia.magic.school.SpellSchool.HOLY,
                    0.35, 8, phase);
        }
        // Sub-VFX quando inputs presentes mas sem output (waiting state)
        else if (be.hasAnyInput() && be.tickCounter % 10 == 0) {
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    center.x, center.y, center.z, 2, 0.2, 0.1, 0.2, 0.02);
        }
    }

    private boolean hasAnyInput() {
        for (int i = 0; i < OUTPUT_SLOT; i++) {
            if (!items.getStackInSlot(i).isEmpty()) return true;
        }
        return false;
    }

    /** Drop inventory quando bloco quebra. */
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
        return Component.translatable("container.liberthia.glyph_inscriber");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new GlyphInscriberMenu(id, inv, this);
    }
}
