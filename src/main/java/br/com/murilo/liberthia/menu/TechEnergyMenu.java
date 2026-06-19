package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.ITechEnergyUI;
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
import net.minecraftforge.items.SlotItemHandler;

/**
 * r182 — GUI de energia unificada dos blocos tech. Mostra energia (barra + número exato)
 * e, conforme o layout do bloco ({@link ITechEnergyUI}), um slot de combustível/carga +
 * barra de atividade (queima/geração). Mesmo padrão da TechMachineMenu.
 */
public class TechEnergyMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final BlockEntity be;
    private final Level level;
    private final int layout;
    private final boolean hasSlot;

    public TechEnergyMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(6));
    }

    public TechEnergyMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModMenuTypes.TECH_ENERGY.get(), id);
        this.be = be;
        this.level = inv.player.level();
        this.data = data;
        // r183: guarda contra BE null (chunk não carregado no cliente) ou tipo errado (desync) → sem crash
        if (be instanceof ITechEnergyUI ui) {
            this.layout = ui.getLayoutId();
            this.hasSlot = layout != ITechEnergyUI.LAYOUT_PASSIVE && ui.getMenuSlots() != null;
            addPlayer(inv);
            if (hasSlot) addSlot(new SlotItemHandler(ui.getMenuSlots(), 0, 80, 35));
        } else {
            this.layout = ITechEnergyUI.LAYOUT_PASSIVE;
            this.hasSlot = false;
            addPlayer(inv);
        }
        addDataSlots(data);
    }

    private void addPlayer(Inventory inv) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    public int layout() { return layout; }
    public int energy() { return (data.get(1) << 16) | (data.get(0) & 0xFFFF); }
    public int maxEnergy() { int m = (data.get(3) << 16) | (data.get(2) & 0xFFFF); return m <= 0 ? 1 : m; }
    public float energyFrac() { return Math.min(1f, (float) energy() / maxEnergy()); }
    public int activity() { return data.get(4); }
    public int activityTotal() { return data.get(5); }
    public float activityFrac() { int t = activityTotal(); return t <= 0 ? 0 : Math.min(1f, (float) activity() / t); }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        Slot slot = slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int end = slots.size(); // 36 (passive) ou 37 (com slot)
        if (idx < 36) {
            if (!hasSlot || !moveItemStackTo(stack, 36, end, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 36, true)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(p, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player p) {
        if (be == null) return false;
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), p, be.getBlockState().getBlock());
    }
}
