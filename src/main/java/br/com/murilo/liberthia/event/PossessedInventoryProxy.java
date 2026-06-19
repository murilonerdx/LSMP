package br.com.murilo.liberthia.event;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * v0.1.22 r22: Container proxy que expõe o inventário do PLAYER POSSUÍDO
 * pro possessor abrir/mexer via {@code ChestMenu} vanilla.
 *
 * <h2>Layout (45 slots, 9×5 — formato ChestMenu)</h2>
 * <pre>
 * Row 0 (slots 0-8):   Hotbar do possuído (slots 0-8 do inv vanilla)
 * Row 1 (slots 9-17):  Main inv linha 0 (slots 9-17)
 * Row 2 (slots 18-26): Main inv linha 1 (slots 18-26)
 * Row 3 (slots 27-35): Main inv linha 2 (slots 27-35)
 * Row 4 (slots 36-44): [Helmet][Chest][Legs][Boots][Offhand][empty×4]
 * </pre>
 *
 * <h2>Sincronização</h2>
 * Cada {@code setItem(slot, stack)} escreve direto no {@code Player.getInventory()}
 * do target — server-side sync automático. Vanilla quickMoveStack funciona
 * porque seguimos a interface Container correta.
 */
public class PossessedInventoryProxy implements Container {

    private final Player target;
    public static final int SIZE = 45; // 9×5 — encaixa em ChestMenu 5-row

    public PossessedInventoryProxy(Player target) {
        this.target = target;
    }

    /** Mapping slot UI → posição real no inventory do target. */
    private int mapSlot(int slot) {
        return slot; // 0-35 main inv, 36-40 ainda mapeáveis via getItem custom
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SIZE; i++) {
            if (!getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        Inventory inv = target.getInventory();
        if (slot < 0) return ItemStack.EMPTY;
        if (slot < 36) {
            return inv.items.get(slot);
        }
        // Row 4: armor + offhand
        if (slot == 36) return inv.armor.get(3); // HELMET (vanilla slot 3 = head)
        if (slot == 37) return inv.armor.get(2); // CHEST
        if (slot == 38) return inv.armor.get(1); // LEGS
        if (slot == 39) return inv.armor.get(0); // BOOTS
        if (slot == 40) return inv.offhand.get(0); // OFFHAND
        // 41-44: filler vazio (visualmente ocupado mas sem item)
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        Inventory inv = target.getInventory();
        if (slot < 36) {
            ItemStack stack = inv.items.get(slot);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack split = stack.split(count);
            if (stack.isEmpty()) inv.items.set(slot, ItemStack.EMPTY);
            setChanged();
            return split;
        }
        // armor/offhand: removeItemNoUpdate equivalente
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack split = stack.split(count);
        setItem(slot, stack);
        setChanged();
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        Inventory inv = target.getInventory();
        if (slot < 0 || slot > 44) return;
        if (slot < 36) {
            inv.items.set(slot, stack);
        } else if (slot == 36) {
            inv.armor.set(3, stack);
        } else if (slot == 37) {
            inv.armor.set(2, stack);
        } else if (slot == 38) {
            inv.armor.set(1, stack);
        } else if (slot == 39) {
            inv.armor.set(0, stack);
        } else if (slot == 40) {
            inv.offhand.set(0, stack);
        }
        // 41-44: ignora (filler)
        setChanged();
    }

    @Override
    public void setChanged() {
        target.getInventory().setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        // Permite o possessor manter o menu aberto se ainda há posse ativa.
        if (target == null || !target.isAlive()) return false;
        PossessionSession s = PossessionManager.getSession(player.getUUID());
        return s != null && s.target.equals(target.getUUID());
    }

    @Override
    public void clearContent() {
        // Não permitido — não queremos /clear acidental do inv do target.
    }
}
