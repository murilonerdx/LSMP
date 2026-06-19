package br.com.murilo.liberthia.magic.accessory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * r164: Helper estático que varre inventário + slots Curios do player atrás de
 * {@link MagicAccessoryItem}, somando os bônus de cada efeito.
 *
 * <p>Sempre faz fallback graceful se Curios não estiver carregado — só lê
 * inventário regular. Não cacheia (a cada tick recalcula; é barato).
 */
public final class MagicAccessoryEffects {

    private MagicAccessoryEffects() {}

    /** Soma de magnitudes de todos itens com efeito {@code target} equipados/no inv. */
    public static float sumMagnitude(Player p, MagicAccessoryItem.Effect target) {
        if (p == null) return 0F;
        final float[] acc = { 0F };
        forEachAccessory(p, stack -> {
            if (stack.getItem() instanceof MagicAccessoryItem mai && mai.effect() == target) {
                acc[0] += mai.magnitude();
            }
        });
        return acc[0];
    }

    /** Source/s passivo do player (soma de MANA_REGEN). */
    public static float manaRegenPerSec(Player p) {
        return sumMagnitude(p, MagicAccessoryItem.Effect.MANA_REGEN);
    }

    /** Sanidade/s passivo do player (soma de SANITY_REGEN). */
    public static float sanityRegenPerSec(Player p) {
        return sumMagnitude(p, MagicAccessoryItem.Effect.SANITY_REGEN);
    }

    /**
     * Multiplicador de dano dos feitiços. Base 1.0 + soma de magnitudes
     * (ex: 2 items SPELL_POWER 0.05 cada = 1.10 → +10% dano).
     */
    public static float damageMultiplier(Player p) {
        return 1.0F + sumMagnitude(p, MagicAccessoryItem.Effect.SPELL_POWER);
    }

    /** Itera por todos os accessory items relevantes (inventário + curios). */
    private static void forEachAccessory(Player p, Consumer<ItemStack> visitor) {
        // 1) Main + offhand
        visitor.accept(p.getMainHandItem());
        visitor.accept(p.getOffhandItem());

        // 2) Inventário inteiro (hotbar + storage)
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            visitor.accept(p.getInventory().getItem(i));
        }

        // 3) Curios slots (se mod presente). findCurios retorna List<SlotResult>
        // de todos os slots Curios cujo stack passa no predicate.
        try {
            top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                    .findCurios(p, s -> s.getItem() instanceof MagicAccessoryItem)
                    .forEach(slotResult -> visitor.accept(slotResult.stack()));
        } catch (Throwable ignored) {
            // Curios não instalado ou erro — só usa inventário regular
        }
    }
}
