package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.DarkMatterBatteryBlockEntity;
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

/**
 * Menu read-only da bateria — sincroniza energia + capacidade + tier.
 *
 * <p>Não tem slots interativos — é só pra exibir.
 */
public class DarkMatterBatteryMenu extends AbstractContainerMenu {

    private final DarkMatterBatteryBlockEntity be;
    private final Level level;
    private final ContainerData data;

    public DarkMatterBatteryMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (DarkMatterBatteryBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(5));
    }

    public DarkMatterBatteryMenu(int id, Inventory inv, DarkMatterBatteryBlockEntity be, ContainerData data) {
        super(ModMenuTypes.DARK_MATTER_BATTERY.get(), id);
        this.be = be;
        this.level = inv.player.level();
        this.data = data;

        // Player inv (sem ser-be slots interativos)
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, 142));

        addDataSlots(data);
    }

    /** Reconstrói int (32 bits) a partir de (alto, baixo) shorts. */
    private int read32(int hi, int lo) { return ((data.get(hi) & 0xFFFF) << 16) | (data.get(lo) & 0xFFFF); }

    public int rawEnergy()    { return read32(0, 1); }
    public int rawEnergyMax() { return read32(2, 3); }
    public int tierOrdinal()  { return data.get(4); }
    public DarkMatterBatteryBlockEntity.Tier tier() {
        return DarkMatterBatteryBlockEntity.Tier.values()[Math.max(0, Math.min(2, tierOrdinal()))];
    }
    public float energyFrac() {
        return rawEnergyMax() <= 0 ? 0f : Math.min(1f, rawEnergy() / (float) rawEnergyMax());
    }

    @Override public ItemStack quickMoveStack(Player p, int idx) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), p,
                be.getBlockState().getBlock());
    }
}
