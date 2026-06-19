package br.com.murilo.liberthia.magic.affinity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * r179: Helper central dos Affinity Rings.
 *
 * <p>Antes, cada anel aplicava seu efeito via {@link AffinityRingItem#inventoryTick}.
 * Problema: {@code inventoryTick} <b>NÃO roda</b> quando o item está num slot
 * Curios (Curios usa {@code curioTick}). Como os anéis estão tagueados como
 * {@code curios:ring}, o tester equipava no slot e <b>nada acontecia</b>.
 *
 * <p>Solução: {@link #isWorn} varre mão + inventário inteiro + slots Curios.
 * Os efeitos passam a ser dirigidos por handlers centrais
 * ({@link AffinityRingHandler}, {@link BloodbornHandler}, SourceData.recomputeMax)
 * que consultam este helper — funcionando igual no inventário OU equipado.
 */
public final class AffinityRings {

    private AffinityRings() {}

    /**
     * True se o player tem um Affinity Ring do {@code type} na mão principal,
     * mão secundária, qualquer slot do inventário, OU equipado num slot Curios.
     */
    public static boolean isWorn(Player p, AffinityRingItem.Type type) {
        if (p == null) return false;

        // 1) Mãos
        if (matches(p.getMainHandItem(), type) || matches(p.getOffhandItem(), type)) return true;

        // 2) Inventário inteiro (hotbar + storage)
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (matches(inv.getItem(i), type)) return true;
        }

        // 3) Slots Curios (se o mod estiver presente)
        try {
            boolean found = !top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                    .findCurios(p, s -> s.getItem() instanceof AffinityRingItem ari && ari.getType() == type)
                    .isEmpty();
            if (found) return true;
        } catch (Throwable ignored) {
            // Curios não instalado — só conta inventário
        }
        return false;
    }

    private static boolean matches(ItemStack s, AffinityRingItem.Type type) {
        return s != null && !s.isEmpty()
                && s.getItem() instanceof AffinityRingItem ari && ari.getType() == type;
    }
}
