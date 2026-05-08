package br.com.murilo.liberthia.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Helper para o sistema de pureza dos itens de matéria escura.
 *
 * <p>NBT key: {@code "Purity"} — int 0..MAX. Quanto maior, mais energia o
 * bloco gera quando queimado no {@link br.com.murilo.liberthia.block.entity.DarkMatterGeneratorBlockEntity}.
 *
 * <p>Multiplicador FE: {@code 1 + purity * 0.5} → purity 0 = 1×, purity 5 = 3.5×.
 */
public final class Purity {
    public static final String KEY = "Purity";
    public static final int MAX = 5;

    private Purity() {}

    public static int getPurity(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(KEY)) return 0;
        int p = tag.getInt(KEY);
        return Math.max(0, Math.min(MAX, p));
    }

    public static void setPurity(ItemStack stack, int purity) {
        if (stack.isEmpty()) return;
        purity = Math.max(0, Math.min(MAX, purity));
        if (purity == 0) {
            CompoundTag tag = stack.getTag();
            if (tag != null) tag.remove(KEY);
            return;
        }
        stack.getOrCreateTag().putInt(KEY, purity);
    }

    /** Multiplicador FE pro queima. purity=0→1.0, purity=5→3.5. */
    public static double feMultiplier(int purity) {
        return 1.0 + Math.max(0, Math.min(MAX, purity)) * 0.5;
    }

    /** Renderização de estrelas pra tooltip. */
    public static String stars(int purity) {
        StringBuilder sb = new StringBuilder();
        purity = Math.max(0, Math.min(MAX, purity));
        for (int i = 0; i < MAX; i++) {
            sb.append(i < purity ? "★" : "☆");
        }
        return sb.toString();
    }

    /** Cor pra mostrar na tooltip baseado em pureza. */
    public static String colorCode(int purity) {
        return switch (Math.max(0, Math.min(MAX, purity))) {
            case 0 -> "§7";
            case 1 -> "§e";
            case 2 -> "§6";
            case 3 -> "§d";
            case 4 -> "§5";
            case 5 -> "§4§l";
            default -> "§7";
        };
    }
}
