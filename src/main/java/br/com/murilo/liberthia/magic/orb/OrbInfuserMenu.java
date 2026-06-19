package br.com.murilo.liberthia.magic.orb;

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

/**
 * r174: Menu do Infusor de Orbs — 5 slots de input + 1 slot de output (preview).
 * Retirar o output consome 1 de cada input.
 */
public class OrbInfuserMenu extends AbstractContainerMenu {

    private final OrbInfuserBlockEntity be;
    private final Level level;

    public OrbInfuserMenu(int id, Inventory inv, OrbInfuserBlockEntity be) {
        super(ModMenuTypes.ORB_INFUSER.get(), id);
        this.be = be;
        this.level = inv.player.level();

        for (int i = 0; i < OrbInfuserBlockEntity.INPUT_COUNT; i++) {
            addSlot(new SlotItemHandler(be.items, i, 26 + i * 18, 34));
        }
        addSlot(new SlotItemHandler(be.items, OrbInfuserBlockEntity.SLOT_OUTPUT, 134, 34) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
            @Override public void onTake(Player p, ItemStack taken) {
                be.consumeInputs();
                super.onTake(p, taken);
            }
        });

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, 8 + col * 18, 162));
    }

    public OrbInfuserMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (OrbInfuserBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    public net.minecraft.core.BlockPos getBlockPos() { return be.getBlockPos(); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stk = slot.getItem();
        ItemStack orig = stk.copy();
        int invStart = OrbInfuserBlockEntity.TOTAL;
        if (index < invStart) {
            if (!moveItemStackTo(stk, invStart, slots.size(), true)) return ItemStack.EMPTY;
            if (index == OrbInfuserBlockEntity.SLOT_OUTPUT) slot.onTake(player, stk);
        } else {
            if (!moveItemStackTo(stk, 0, OrbInfuserBlockEntity.INPUT_COUNT, false)) return ItemStack.EMPTY;
        }
        if (stk.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return orig;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, be.getBlockPos()), player, ModBlocks.ORB_INFUSER.get());
    }
}
