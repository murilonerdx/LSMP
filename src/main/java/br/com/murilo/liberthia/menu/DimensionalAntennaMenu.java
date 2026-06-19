package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.DimensionalAntennaBlockEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * v0.1.22 r27: Menu da Dimensional Antenna. Slots:
 * <ul>
 *   <li>0: amethyst (fuel)</li>
 *   <li>1: dark matter shard (catalyst)</li>
 * </ul>
 * Plus 36 player slots.
 *
 * <p>ContainerData (read-only sync pro client renderizar status):
 * <ul>
 *   <li>0: active (0/1)</li>
 *   <li>1: tuned count</li>
 *   <li>2-3: energy current (split low/high short)</li>
 *   <li>4-5: energy max</li>
 * </ul>
 */
public class DimensionalAntennaMenu extends AbstractContainerMenu {

    private final DimensionalAntennaBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Client constructor (vem do extraData)
    public DimensionalAntennaMenu(int containerId, Inventory inv, FriendlyByteBuf extra) {
        this(containerId, inv,
                (DimensionalAntennaBlockEntity) inv.player.level().getBlockEntity(extra.readBlockPos()),
                new SimpleContainerData(8));
    }

    /** r28: tuning % (0-100) com a melhor antena tunada na mesma freq. */
    public int getTuningPercent() {
        return data.get(6);
    }

    /** r28: facing da antena (Direction.from3DDataValue). */
    public net.minecraft.core.Direction getFacing() {
        return net.minecraft.core.Direction.from3DDataValue(data.get(7));
    }

    public DimensionalAntennaMenu(int containerId, Inventory inv,
                                  DimensionalAntennaBlockEntity be, ContainerData data) {
        super(ModMenuTypes.DIMENSIONAL_ANTENNA.get(), containerId);
        this.blockEntity = be;
        this.level = inv.player.level();
        this.data = data;

        IItemHandler h = be.getInventory();
        // Slot AMETHYST (esquerda) — r29: nova posição na linha de insumos
        this.addSlot(new SlotItemHandler(h, DimensionalAntennaBlockEntity.SLOT_AMETHYST, 44, 36));
        // Slot DARK MATTER (direita)
        this.addSlot(new SlotItemHandler(h, DimensionalAntennaBlockEntity.SLOT_DARK_MATTER, 116, 36));

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
        addDataSlots(data);
    }

    public boolean isActive() {
        return data.get(0) == 1;
    }

    public int getTunedCount() {
        return data.get(1);
    }

    public int getEnergyStored() {
        return (data.get(2) & 0xFFFF) | ((data.get(3) & 0xFFFF) << 16);
    }

    public int getEnergyMax() {
        return (data.get(4) & 0xFFFF) | ((data.get(5) & 0xFFFF) << 16);
    }

    public String getFrequency() {
        return blockEntity == null ? "" : blockEntity.getFrequency();
    }

    public BlockPos getPos() {
        return blockEntity == null ? BlockPos.ZERO : blockEntity.getBlockPos();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        Slot slot = this.slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack orig = stack.copy();

        int beSlots = 2;          // 0-1
        int playerStart = beSlots; // 2
        int playerEnd = playerStart + 36; // 38

        if (idx < beSlots) {
            // BE → player
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            // Player → BE (tenta amethyst então DM)
            if (!moveItemStackTo(stack, 0, beSlots, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == orig.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return orig;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && stillValid(
                ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.DIMENSIONAL_ANTENNA.get());
    }

    // r37: GUI cresceu pra 232 de altura — inventário desce mais 8px pra
    // garantir que nem o energy bar nem o separator overlapem o player inv.
    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 148 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 206));
        }
    }
}
