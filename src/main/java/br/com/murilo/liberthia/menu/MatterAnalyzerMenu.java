package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.MatterAnalyzerBlockEntity;
import br.com.murilo.liberthia.matter.MatterContent;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;

public class MatterAnalyzerMenu extends AbstractContainerMenu {
    private final MatterAnalyzerBlockEntity be;
    private final Level level;

    public MatterAnalyzerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (MatterAnalyzerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public MatterAnalyzerMenu(int id, Inventory inv, MatterAnalyzerBlockEntity be) {
        super(ModMenuTypes.MATTER_ANALYZER.get(), id);
        this.be = be;
        this.level = inv.player.level();

        addPlayer(inv);
        // Slot único de input — sem auto-extract
        addSlot(new SlotItemHandler(be.getInventory(), MatterAnalyzerBlockEntity.SLOT_INPUT, 24, 35));
    }

    private void addPlayer(Inventory inv) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    /** Conteúdo de matéria atual — lido de ambos lados (cliente + server) já
     *  que o BE expõe o item via update packet. */
    public MatterContent currentContent() {
        return be.currentContent();
    }

    public ItemStack getInputStack() { return be.getInputStack(); }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        Slot slot = slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (idx >= 36) {
            if (!moveItemStackTo(stack, 0, 36, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 36, 37, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), p, ModBlocks.MATTER_ANALYZER.get());
    }
}
