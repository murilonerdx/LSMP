package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.observation.api.Manifestation;
import br.com.murilo.liberthia.observation.api.ObservationPart;
import br.com.murilo.liberthia.observation.api.ObservationRegistry;
import br.com.murilo.liberthia.observation.item.GlyphItem;
import br.com.murilo.liberthia.observation.item.SpellParchmentItem;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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

import java.util.ArrayList;
import java.util.List;

/**
 * v0.1.22 r68: <b>Scribes Table Block Entity</b> — inventário da mesa de escriba.
 *
 * <p>Pattern AN's {@code ScribesBlockTile}: inventário com slots dedicados:
 * <ul>
 *   <li>Slot 0 — Parchment input (paper blank)</li>
 *   <li>Slot 1-8 — 8 Glyph slots (ordem importa: 1=primeiro glyph da receita)</li>
 *   <li>Slot 9 — Output (parchment com recipe inscrita)</li>
 * </ul>
 *
 * <p>onContentsChanged: re-avalia recipe. Se válida (primeiro glyph é Method),
 * popula output com parchment inscrito. Taking output: consume todos os inputs.
 */
public class ScribesTableBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_PARCHMENT_IN = 0;
    public static final int SLOT_GLYPH_START = 1;
    public static final int SLOT_GLYPH_END = 8;   // inclusive
    public static final int SLOT_OUTPUT = 9;
    public static final int TOTAL_SLOTS = 10;

    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                // Se mudou inputs (não o output), re-avalia recipe
                if (slot != SLOT_OUTPUT) {
                    updateOutput();
                }
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_PARCHMENT_IN -> stack.getItem() == ModItems.SPELL_PARCHMENT.get()
                    && SpellParchmentItem.getRecipe(stack).isEmpty(); // só parchment VAZIO
                case SLOT_OUTPUT -> false; // ninguém coloca no output
                default -> stack.getItem() instanceof GlyphItem;
            };
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public ScribesTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCRIBES_TABLE.get(), pos, state);
    }

    public IItemHandler getItemHandler() { return inventory; }

    /** r164: ID do Method default — adicionado se receita não tem Method próprio. */
    private static final String DEFAULT_METHOD_ID = "liberthia:watch/direct_gaze";
    /** r164: ID da Manifestation default — adicionada se receita não tem Manifestation. */
    private static final String DEFAULT_MANIFEST_ID = "liberthia:manifest/tendril";

    /** r164: nome customizado pro feitiço (escrito pelo client via SetSpellNameC2SPacket). */
    private String customSpellName = "";

    public String getCustomSpellName() { return customSpellName; }

    public void setCustomSpellName(String name) {
        this.customSpellName = (name == null) ? "" : name.trim();
        updateOutput();
        setChanged();
    }

    /**
     * r164 redesign: <b>flexível</b> — qualquer combinação de glyphs gera output.
     *
     * <h2>Regras novas</h2>
     * <ul>
     *   <li>Parchment + ≥1 glyph válido → gera output (mínimo absoluto)</li>
     *   <li>Se não tem Method nos glyphs → adiciona §a"direct_gaze" (projétil)§r implícito</li>
     *   <li>Se não tem Manifestation → adiciona §a"tendril"§r implícito (efeito visível)</li>
     *   <li>Ordem dos slots NÃO importa — o Method (se houver) é movido pro início</li>
     *   <li>Múltiplos Methods são permitidos (o 1º vira principal, os outros viram modificadores)</li>
     * </ul>
     *
     * <p><b>Por quê:</b> o user reclamou que "1 ou 2 glifos era pra funcionar" e
     * "senti falta de projétil no scribes table". Agora qualquer combinação é
     * craftable — o sistema completa automaticamente o que falta.
     */
    private void updateOutput() {
        // 1) Input parchment? Sem isso, output zerado
        ItemStack inputParch = inventory.getStackInSlot(SLOT_PARCHMENT_IN);
        if (inputParch.isEmpty() || inputParch.getItem() != ModItems.SPELL_PARCHMENT.get()) {
            inventory.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }

        // 2) Coleta glyphs em ordem dos slots (sem rejeitar nada ainda)
        List<ObservationPart> rawParts = new ArrayList<>();
        for (int i = SLOT_GLYPH_START; i <= SLOT_GLYPH_END; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.isEmpty()) continue;
            if (!(s.getItem() instanceof GlyphItem glyph)) continue;
            ObservationPart part = glyph.getPart();
            if (part != null) rawParts.add(part);
        }

        // 3) Mínimo: pelo menos 1 glyph válido
        if (rawParts.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }

        // 4) Reorganiza: Method primeiro, depois resto. Auto-prepend default se não tem.
        List<String> recipe = new ArrayList<>();
        boolean hasMethod = false;
        boolean hasManifest = false;
        int color = 0x9d4dd6;  // roxo default

        // Pass 1: acha o PRIMEIRO Method e coloca como entrada zero
        ObservationPart primaryMethod = null;
        for (ObservationPart p : rawParts) {
            if (p.typeIndex() == 1 && primaryMethod == null) {
                primaryMethod = p;
                hasMethod = true;
            }
        }
        if (primaryMethod != null) {
            recipe.add(primaryMethod.id().toString());
        } else {
            // Auto-adiciona Method default (projétil = direct_gaze)
            recipe.add(DEFAULT_METHOD_ID);
        }

        // Pass 2: adiciona resto (skip o primaryMethod, mantem Manifestations + Distortions + outros Methods)
        for (ObservationPart p : rawParts) {
            if (p == primaryMethod) continue;
            recipe.add(p.id().toString());
            if (p instanceof Manifestation) {
                hasManifest = true;
                if (color == 0x9d4dd6) color = p.color();
            }
        }

        // 5) Se não tem Manifestation, adiciona uma default (tendril) pra ter efeito visível
        if (!hasManifest) {
            recipe.add(DEFAULT_MANIFEST_ID);
            ObservationPart def = ObservationRegistry.get(
                    new net.minecraft.resources.ResourceLocation(DEFAULT_MANIFEST_ID));
            if (def != null) color = def.color();
        }

        // 6) Constrói output parchment com recipe inscrita
        ItemStack output = new ItemStack(ModItems.SPELL_PARCHMENT.get());
        SpellParchmentItem.setRecipe(output, recipe);
        output.getOrCreateTag().putInt(SpellParchmentItem.NBT_COLOR, color);

        // 7) Nome: usa customSpellName se setado, senão auto-nome baseado no manifestation/method
        String autoName = null;
        for (String id : recipe) {
            ObservationPart p = ObservationRegistry.get(new net.minecraft.resources.ResourceLocation(id));
            if (p instanceof Manifestation) {
                autoName = p.displayComponent().getString();
                break;
            }
        }
        if (autoName == null && !recipe.isEmpty()) {
            // fallback pro primeiro part
            ObservationPart p = ObservationRegistry.get(new net.minecraft.resources.ResourceLocation(recipe.get(0)));
            if (p != null) autoName = p.displayComponent().getString();
        }
        String finalName = !customSpellName.isEmpty() ? customSpellName
                : (autoName != null ? autoName : "Feitiço");
        output.getOrCreateTag().putString(SpellParchmentItem.NBT_NAME, finalName);
        // Também aplica display name pro item ficar com nome visível
        if (!customSpellName.isEmpty()) {
            output.setHoverName(net.minecraft.network.chat.Component.literal(customSpellName));
        }

        // Marca auto-completados pra debug visual (não afeta gameplay)
        if (!hasMethod) {
            output.getOrCreateTag().putBoolean("liberthia.auto_method", true);
        }
        if (!hasManifest) {
            output.getOrCreateTag().putBoolean("liberthia.auto_manifest", true);
        }

        inventory.setStackInSlot(SLOT_OUTPUT, output);
    }

    /** Chamado quando jogador pega o output — consome inputs. */
    public void onOutputTaken() {
        // Consome 1 parchment input
        inventory.getStackInSlot(SLOT_PARCHMENT_IN).shrink(1);
        // Consome todos os glyph items
        for (int i = SLOT_GLYPH_START; i <= SLOT_GLYPH_END; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty()) s.shrink(1);
        }
        // r164: limpa o nome custom pro próximo craft
        this.customSpellName = "";
        // Re-avalia (provavelmente vai limpar o output já que glyphs foram consumidos)
        updateOutput();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.scribes_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new br.com.murilo.liberthia.menu.ScribesTableMenu(containerId, inventory, this);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> inventory);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    /** Drop tudo quando block é quebrado. */
    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        if (level != null) Containers.dropContents(level, worldPosition, inv);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inv", inventory.serializeNBT());
        if (customSpellName != null && !customSpellName.isEmpty()) {
            tag.putString("CustomSpellName", customSpellName);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        this.customSpellName = tag.contains("CustomSpellName") ? tag.getString("CustomSpellName") : "";
    }
    // getBlockPos() já vem do BlockEntity parent — sem precisar override.
}
