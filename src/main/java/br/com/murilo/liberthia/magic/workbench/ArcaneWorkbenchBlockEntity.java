package br.com.murilo.liberthia.magic.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
 * r159: BlockEntity do Arcane Workbench com 11 slots tipados.
 *
 * <p>Cada slot só aceita o tipo correspondente em {@link ArcaneSlotType}. Quando
 * TODOS os 10 inputs estão preenchidos com items válidos, o output (slot 10) é
 * gerado via {@link SpellMutationLogic}.
 */
public class ArcaneWorkbenchBlockEntity extends BlockEntity {

    public static final int SLOT_BASE = ArcaneSlotType.SLOT_SCROLL; // legacy alias
    public static final int SLOT_OUTPUT = ArcaneSlotType.SLOT_OUTPUT;
    public static final int TOTAL_SLOTS = ArcaneSlotType.TOTAL_SLOTS;

    public final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot < 0 || slot >= ArcaneSlotType.ORDER.length) return false;
            return ArcaneSlotType.ORDER[slot].accepts(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot != SLOT_OUTPUT) recalcOutput();
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) return stack;
            return super.insertItem(slot, stack, simulate);
        }
    };

    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> items);

    public ArcaneWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(br.com.murilo.liberthia.registry.ModBlockEntities.ARCANE_WORKBENCH.get(), pos, state);
    }

    /** Nome customizado pro feitiço — escrito pelo client via SetSpellNameC2SPacket (r164). */
    private String customSpellName = "";

    public String getCustomSpellName() { return customSpellName; }

    public void setCustomSpellName(String name) {
        this.customSpellName = (name == null) ? "" : name.trim();
        recalcOutput();  // re-aplica o nome no output existente
        setChanged();
    }

    /**
     * r164: Recomputa o output baseado nos inputs disponíveis.
     *
     * <p><b>Requisitos mínimos relaxados</b> (user reclamou que precisar dos 10 slots
     * é absurdo):
     * <ul>
     *   <li>SCROLL é obrigatório (define o feitiço base)</li>
     *   <li>PARCHMENT é obrigatório (é o "pergaminho final" que o user mencionou)</li>
     *   <li>Pelo menos 1 outro slot preenchido (tablet/weave/thread/orb/focus/school/glyph/modifier) — qualquer um</li>
     * </ul>
     *
     * <p>Todos os slots preenchidos são "lidos" como modifiers (cada um adiciona algo
     * à composição). Quanto mais slots, mais poderoso. Mas o mínimo de 3 items
     * (SCROLL + PARCHMENT + 1 outro) já gera output.
     */
    private void recalcOutput() {
        // r166: SEM obrigatoriedade de Scroll/Parchment. Gera o feitiço a partir de
        // QUALQUER item de input preenchido. Se houver um scroll base, ele define o
        // feitiço; senão, o combine sintetiza um base a partir dos próprios items
        // (focus/school/glyph). Basta 1 item pra gerar.
        ItemStack base = items.getStackInSlot(ArcaneSlotType.SLOT_SCROLL);

        // Coleta TODOS os outros slots preenchidos como "componentes" (incl. parchment).
        java.util.List<ItemStack> modList = new java.util.ArrayList<>();
        for (int i : ArcaneSlotType.INPUT_SLOTS) {
            if (i == ArcaneSlotType.SLOT_SCROLL) continue;
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) modList.add(s);
        }

        // Nada preenchido em lugar nenhum → sem output.
        if (base.isEmpty() && modList.isEmpty()) {
            items.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }

        ItemStack out = SpellMutationLogic.combine(base, modList.toArray(new ItemStack[0]));
        if (!out.isEmpty() && !customSpellName.isEmpty()) {
            out.setHoverName(net.minecraft.network.chat.Component.literal(customSpellName));
        }
        items.setStackInSlot(SLOT_OUTPUT, out);
    }

    /** Chamado pelo Menu quando o player retira o output — consome inputs que existem. */
    public void consumeAllInputs() {
        for (int i : ArcaneSlotType.INPUT_SLOTS) {
            ItemStack s = items.getStackInSlot(i);
            if (!s.isEmpty()) {
                s.shrink(1);
                items.setStackInSlot(i, s);
            }
        }
        // r164: limpa nome customizado pro próximo craft
        this.customSpellName = "";
    }

    public void dropContents(Level level, BlockPos pos) {
        NonNullList<ItemStack> list = NonNullList.create();
        for (int i : ArcaneSlotType.INPUT_SLOTS) {
            list.add(items.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, list);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        if (customSpellName != null && !customSpellName.isEmpty()) {
            tag.putString("CustomSpellName", customSpellName);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) {
            items.deserializeNBT(tag.getCompound("Items"));
        }
        this.customSpellName = tag.contains("CustomSpellName") ? tag.getString("CustomSpellName") : "";
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap,
                                                       @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemHandlerCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
    }
}
