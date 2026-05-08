package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.DarkMatterGeneratorBlockEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class DarkMatterGeneratorMenu extends AbstractContainerMenu {
    private final DarkMatterGeneratorBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public DarkMatterGeneratorMenu(int id, Inventory inv, FriendlyByteBuf extra) {
        this(id, inv, inv.player.level().getBlockEntity(extra.readBlockPos()), new SimpleContainerData(8));
    }

    public DarkMatterGeneratorMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.DARK_MATTER_GENERATOR.get(), id);
        this.blockEntity = (DarkMatterGeneratorBlockEntity) be;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        IItemHandler handler = this.blockEntity.getItemHandler();
        // Slot de combustível à esquerda
        this.addSlot(new SlotItemHandler(handler, DarkMatterGeneratorBlockEntity.SLOT_FUEL,        44, 36));
        // Linha de upgrades à direita do combustível
        this.addSlot(new UpgradeSlot(handler, DarkMatterGeneratorBlockEntity.SLOT_SPEED,           102, 18, ModItems.SPEED_UPGRADE.get()));
        this.addSlot(new UpgradeSlot(handler, DarkMatterGeneratorBlockEntity.SLOT_EFFICIENCY,      102, 36, ModItems.EFFICIENCY_UPGRADE.get()));
        this.addSlot(new UpgradeSlot(handler, DarkMatterGeneratorBlockEntity.SLOT_CAPACITY,        102, 54, ModItems.CAPACITY_UPGRADE.get()));

        addDataSlots(data);
    }

    /** Slot de upgrade — só aceita um tipo específico de item. */
    private static class UpgradeSlot extends SlotItemHandler {
        private final net.minecraft.world.item.Item allowed;
        UpgradeSlot(IItemHandler h, int idx, int x, int y, net.minecraft.world.item.Item allowed) {
            super(h, idx, x, y);
            this.allowed = allowed;
        }
        @Override public boolean mayPlace(ItemStack stack) { return stack.is(allowed); }
        @Override public int getMaxStackSize() { return 4; }
    }

    /** Reconstrói int (32 bits) a partir de dois shorts (alto, baixo) sincronizados. */
    private int readInt(int hiIdx, int loIdx) {
        int hi = data.get(hiIdx) & 0xFFFF;
        int lo = data.get(loIdx) & 0xFFFF;
        return (hi << 16) | lo;
    }

    public int rawEnergy()        { return readInt(0, 1); }
    public int rawEnergyMax()     { return readInt(2, 3); }
    public int rawBurnFuel()      { return readInt(4, 5); }
    public int rawBurnInitial()   { int v = readInt(6, 7); return v == 0 ? 1 : v; }

    public float energyFrac() {
        int e = rawEnergy(), max = rawEnergyMax();
        return max <= 0 ? 0f : Math.min(1f, e / (float) max);
    }
    public float burnFrac() {
        int cur = rawBurnFuel(), init = rawBurnInitial();
        return init <= 0 ? 0f : Math.min(1f, cur / (float) init);
    }
    public boolean isBurning() { return rawBurnFuel() > 0; }

    /** Acesso ao BE (lado servidor) — usado pra ler contagem de upgrades no Screen. */
    public DarkMatterGeneratorBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack slotStack = slot.getItem();
        ItemStack original = slotStack.copy();

        // Slots: 0..35 player inv, 36=fuel, 37=speed, 38=eff, 39=cap
        if (slotIndex >= 36) {
            // Do gerador → player
            if (!this.moveItemStackTo(slotStack, 0, 36, true)) return ItemStack.EMPTY;
        } else {
            // Player → gerador. Tenta achar o slot certo:
            // - dark_matter_block → SLOT_FUEL (36)
            // - speed_upgrade → 37, eff → 38, cap → 39
            int target = -1;
            if (slotStack.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem())) target = 36;
            else if (slotStack.is(ModItems.SPEED_UPGRADE.get())) target = 37;
            else if (slotStack.is(ModItems.EFFICIENCY_UPGRADE.get())) target = 38;
            else if (slotStack.is(ModItems.CAPACITY_UPGRADE.get())) target = 39;
            if (target < 0) return ItemStack.EMPTY;
            if (!this.moveItemStackTo(slotStack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (slotStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (slotStack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, slotStack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.DARK_MATTER_GENERATOR.get());
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int col = 0; col < 9; ++col)
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }
}
