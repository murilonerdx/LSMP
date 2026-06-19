package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.ItemPipeBlockEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu da GUI do Pipe Filter (acessada via shift+right-click com pipe_filter
 * ou pipe_filter_not numa face de Item Pipe).
 *
 * <p>Mostra 5 slots correspondentes aos 5 entries do filtro daquela face
 * específica do BE. Inserir item num slot = adicionar ao filtro. Tirar = remover.
 *
 * <p>O modo (WHITELIST/BLACKLIST) é exposto via {@link DataSlot} (sincroniza
 * pro client) e mudado via botão na Screen (sends packet pelo system de
 * setData/clickMenuButton).
 *
 * <p>Container é {@code SimpleContainer} (5 slots, "ghost slots" do filtro).
 * Quando o player coloca/tira items, sincronizamos com o {@code filter[face]}
 * do BE via {@link #setChanged()}.
 *
 * <p><b>v0.1.26 fix do bug "pisca e fecha"</b>: o cliente fechava o menu
 * imediatamente porque {@code resolveBE} retornava null em race de chunk sync,
 * e o {@code stillValid} tinha {@code if (be == null) return false}. Fix:
 * {@code pipePosCached} é guardado SEMPRE (mesmo quando be vem null), e
 * {@code stillValid} usa só a pos + o level do player — não depende do BE.
 */
public class PipeFilterMenu extends AbstractContainerMenu {

    public static final int FILTER_SIZE = ItemPipeBlockEntity.FILTER_SIZE;

    private final ItemPipeBlockEntity be;
    private final Direction face;
    private final BlockPos pipePosCached;
    private final Container filterContainer;
    /** 0 = WHITELIST, 1 = BLACKLIST. */
    private final DataSlot filterModeData = DataSlot.standalone();

    /**
     * Constructor lado client — recebe BlockPos+face via FriendlyByteBuf.
     * Lê a pos PRIMEIRO e passa pro master, em vez de tentar resolver o BE
     * (que pode estar null em race de sync e travar o stillValid).
     */
    public PipeFilterMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                buf.readBlockPos(),
                Direction.from3DDataValue(buf.readByte()),
                /* be: */ null);
    }

    /**
     * Constructor lado server — chamado de
     * {@link br.com.murilo.liberthia.item.PipeFilterNotItem#openFilterGui}
     * com o BE já resolvido.
     */
    public PipeFilterMenu(int id, Inventory inv, ItemPipeBlockEntity be, Direction face) {
        this(id, inv, be.getBlockPos(), face, be);
    }

    /** Constructor master — captura pos explícita E be (que pode ser null). */
    private PipeFilterMenu(int id, Inventory inv, BlockPos pipePos, Direction face, ItemPipeBlockEntity be) {
        super(ModMenuTypes.PIPE_FILTER.get(), id);
        this.pipePosCached = pipePos;
        this.face = face;
        // No client, tenta resolver o BE se não foi passado. Se ainda assim
        // vier null (chunk em sync), guarda null e segue — stillValid não
        // depende disso, só o write-back de items (que roda no server).
        ItemPipeBlockEntity resolved = be;
        if (resolved == null) {
            var foundBe = inv.player.level().getBlockEntity(pipePos);
            if (foundBe instanceof ItemPipeBlockEntity p) resolved = p;
        }
        this.be = resolved;

        final ItemPipeBlockEntity beRef = this.be; // pra usar dentro da inner class
        final Direction faceRef = this.face;
        this.filterContainer = new SimpleContainer(FILTER_SIZE) {
            @Override
            public int getMaxStackSize() { return 1; }

            @Override
            public void setChanged() {
                super.setChanged();
                // Propaga pro BE: copia 5 slots pro filter[face] do BE.
                // Só roda no server side (write authority).
                if (beRef != null && !beRef.getLevel().isClientSide()) {
                    var f = beRef.getFilter(faceRef);
                    for (int i = 0; i < FILTER_SIZE; i++) {
                        f[i] = this.getItem(i).copy();
                        if (!f[i].isEmpty()) f[i].setCount(1);
                    }
                    beRef.setChanged();
                }
            }
        };

        // Carrega state inicial do BE pro container (se tiver BE)
        if (this.be != null) {
            var f = this.be.getFilter(face);
            for (int i = 0; i < FILTER_SIZE; i++) {
                this.filterContainer.setItem(i, f[i].copy());
            }
            filterModeData.set(this.be.getFilterMode(face) == ItemPipeBlockEntity.FilterMode.BLACKLIST ? 1 : 0);
        }

        // 5 slots horizontais centralizados — pos calculada pra match
        // exatamente com o que a Screen desenha (centered, espaçados 18px).
        int slotsStartX = 26;
        int slotsY = 36;
        for (int i = 0; i < FILTER_SIZE; i++) {
            this.addSlot(new GhostSlot(filterContainer, i, slotsStartX + i * 18, slotsY));
        }

        // Player inventory (linhas 1-3) + hotbar
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; ++col)
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));

        // DataSlot pra sync de filterMode
        this.addDataSlot(filterModeData);
    }

    /** True se a face está em BLACKLIST (sync via DataSlot). */
    public boolean isBlacklist() {
        return filterModeData.get() == 1;
    }

    /** Toca o botão de toggle. Roda no server pelo flow padrão de clickMenuButton. */
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == 0) {
            // Toggle WHITELIST <-> BLACKLIST
            if (be != null && !be.getLevel().isClientSide()) {
                var newMode = be.cycleFilterMode(face);
                filterModeData.set(newMode == ItemPipeBlockEntity.FilterMode.BLACKLIST ? 1 : 0);
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        Slot slot = slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (idx < FILTER_SIZE) {
            // Shift-click no slot do filtro → move pro inventário do player
            // (= remove do filtro)
            if (!moveItemStackTo(stack, FILTER_SIZE, FILTER_SIZE + 36, true)) return ItemStack.EMPTY;
        } else {
            // Shift-click no inventário → tenta colocar como filtro (1 item)
            // Mas não consome do inventário — só copia o item type
            ItemStack copy = stack.copy();
            copy.setCount(1);
            for (int i = 0; i < FILTER_SIZE; i++) {
                if (filterContainer.getItem(i).isEmpty()) {
                    filterContainer.setItem(i, copy);
                    filterContainer.setChanged();
                    break;
                }
            }
            return ItemStack.EMPTY; // não consome do player
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player p) {
        // v0.1.26: usa só pipePosCached + level do PLAYER. ANTES tinha
        // `if (be == null) return false` que travava o menu em race de sync
        // do BE no cliente (chunk reload, etc), causando "pisca e fecha".
        if (pipePosCached == null) return false;
        if (!p.level().getBlockState(pipePosCached).is(ModBlocks.ITEM_PIPE.get())) return false;
        return p.distanceToSqr(pipePosCached.getX() + 0.5,
                                pipePosCached.getY() + 0.5,
                                pipePosCached.getZ() + 0.5) <= 64.0;
    }

    /** Ghost slot: aceita 1 item de cada, drop/take sem consumir. */
    private static class GhostSlot extends Slot {
        GhostSlot(Container container, int idx, int x, int y) {
            super(container, idx, x, y);
        }
        @Override public int getMaxStackSize() { return 1; }
        @Override public int getMaxStackSize(ItemStack stack) { return 1; }
        // Ghost: jogador pode setar e tirar livre — o item NÃO sai do inventário
        // dele quando coloca no filtro. Pra isso interceptamos no clicked.
    }

    /**
     * Override do click pra fazer "ghost behavior":
     * - Clique normal: copia o item segurado pra ghost slot SEM consumir.
     * - Clique em slot ocupado: limpa.
     * - Clique com shift de slot ocupado: limpa.
     */
    @Override
    public void clicked(int slotId, int dragType, net.minecraft.world.inventory.ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < FILTER_SIZE) {
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                // Limpa o slot
                filterContainer.setItem(slotId, ItemStack.EMPTY);
            } else {
                // Copia 1 unidade do item pro slot SEM consumir do carried
                ItemStack copy = carried.copy();
                copy.setCount(1);
                filterContainer.setItem(slotId, copy);
            }
            filterContainer.setChanged();
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }
}
