package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.RiftForgeBlockEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * r185 — menu da Forja de Fendas: 12 slots de entrada (grade 6×2), 1 de saída, + inventário do jogador.
 */
public class RiftForgeMenu extends AbstractContainerMenu {
    private final RiftForgeBlockEntity be;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    // índices de slots no menu
    private static final int TE_FIRST = 0, TE_COUNT = 13;          // 0–11 entrada, 12 saída
    private static final int PLAYER_FIRST = TE_COUNT;              // 13
    private static final int PLAYER_END = PLAYER_FIRST + 36;       // 49

    public RiftForgeMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolve(inv, buf), new SimpleContainerData(2));
    }

    private static RiftForgeBlockEntity resolve(Inventory inv, FriendlyByteBuf buf) {
        net.minecraft.world.level.block.entity.BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof RiftForgeBlockEntity rbe) return rbe;
        throw new IllegalStateException("RiftForgeBlockEntity ausente ao abrir o menu");
    }

    public RiftForgeMenu(int id, Inventory inv, RiftForgeBlockEntity be, ContainerData data) {
        super(ModMenuTypes.RIFT_FORGE.get(), id);
        this.be = be;
        this.data = data;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        IItemHandler h = be.getInventory();

        // grade 6×2 de entrada
        for (int row = 0; row < 2; row++)
            for (int col = 0; col < 6; col++)
                addSlot(new SlotItemHandler(h, row * 6 + col, 17 + col * 18, 21 + row * 18));
        // saída (sem inserir)
        addSlot(new SlotItemHandler(h, RiftForgeBlockEntity.OUTPUT_SLOT, 189, 30) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
        });

        // inventário do jogador
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 30 + col * 18, 120 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 30 + col * 18, 178));

        addDataSlots(data);
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return Math.max(1, data.get(1)); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return result;
        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < PLAYER_FIRST) {
            // do bloco → inventário do jogador
            if (!moveItemStackTo(stack, PLAYER_FIRST, PLAYER_END, true)) return ItemStack.EMPTY;
        } else {
            // do jogador → slots de entrada (0–11)
            if (!moveItemStackTo(stack, TE_FIRST, RiftForgeBlockEntity.INPUT_SLOTS, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.RIFT_FORGE.get());
    }
}
