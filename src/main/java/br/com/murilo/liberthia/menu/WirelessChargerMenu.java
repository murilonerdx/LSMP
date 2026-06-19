package br.com.murilo.liberthia.menu;

import br.com.murilo.liberthia.block.entity.WirelessChargerBlockEntity;
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
import net.minecraft.world.level.Level;

public class WirelessChargerMenu extends AbstractContainerMenu {
    private final WirelessChargerBlockEntity be;
    private final Level level;
    private final ContainerData data;

    public WirelessChargerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                (WirelessChargerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(7));
    }

    public WirelessChargerMenu(int id, Inventory inv, WirelessChargerBlockEntity be, ContainerData data) {
        super(ModMenuTypes.WIRELESS_CHARGER.get(), id);
        this.be = be;
        this.level = inv.player.level();
        this.data = data;
        addPlayer(inv);
        // Slot de upgrade — aceita Nether Star OU dark_matter_block 5★
        addSlot(new net.minecraftforge.items.SlotItemHandler(be.getUpgradeInventory(),
                WirelessChargerBlockEntity.SLOT_UPGRADE, 152, 56) {
            @Override public int getMaxStackSize() { return 1; }
        });
        addDataSlots(data);
    }

    private void addPlayer(Inventory inv) {
        for (int row = 0; row < 3; ++row)
            for (int col = 0; col < 9; ++col)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 110 + row * 18));
        for (int col = 0; col < 9; ++col)
            addSlot(new Slot(inv, col, 8 + col * 18, 168));
    }

    private int read32(int hi, int lo) { return ((data.get(hi) & 0xFFFF) << 16) | (data.get(lo) & 0xFFFF); }
    public int rawEnergy()    { return read32(0, 1); }
    public int rawEnergyMax() { return read32(2, 3); }
    public int chargingPlayers() { return data.get(4); }
    public int range() { return data.get(5); }
    public int upgradeKind() { return data.get(6); } // 0=NONE, 1=NETHER_STAR, 2=MAX_PURITY
    public boolean isCrossDim() { return upgradeKind() == 2; }
    public float energyFrac() { return Math.min(1f, rawEnergy() / (float) Math.max(1, rawEnergyMax())); }

    public WirelessChargerBlockEntity getBlockEntity() { return be; }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), p,
                ModBlocks.WIRELESS_CHARGER.get());
    }
}
