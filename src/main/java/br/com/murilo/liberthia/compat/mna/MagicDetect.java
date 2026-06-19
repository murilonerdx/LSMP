package br.com.murilo.liberthia.compat.mna;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * r180: detecta se um item/player é "de magia" — sem depender de nenhum mod de magia
 * no classpath. Reconhece por NAMESPACE do registro (mana-and-artifice, ars_nouveau,
 * irons_spellbooks…) e pelo PACOTE da classe do item ({@code com.mna.*} etc).
 */
public final class MagicDetect {

    private static final String[] MAGIC_NAMESPACES = {
            "mana-and-artifice", "mana_and_artifice", "mna",
            "ars_nouveau", "arsnouveau", "ars_elemental",
            "irons_spellbooks", "irons_spells", "spell_engine", "spellbookcore"
    };
    private static final String[] MAGIC_PACKAGES = {
            "com.mna", "com.hollingsworth.arsnouveau", "io.redspace.ironsspellbooks"
    };

    private MagicDetect() {}

    /** True se o item é de um mod de magia (pelo namespace OU pacote da classe). */
    public static boolean isMagicItem(ItemStack s) {
        if (s == null || s.isEmpty()) return false;
        var key = ForgeRegistries.ITEMS.getKey(s.getItem());
        if (key != null) {
            String ns = key.getNamespace();
            for (String m : MAGIC_NAMESPACES) if (ns.equals(m)) return true;
        }
        String cn = s.getItem().getClass().getName();
        for (String p : MAGIC_PACKAGES) if (cn.startsWith(p)) return true;
        return false;
    }

    /** Quantos itens de magia o player tem no CORPO (armadura + mãos + hotbar). */
    public static int magicItemCount(Player p) {
        int n = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (isMagicItem(p.getItemBySlot(slot))) n++;
        }
        for (int i = 0; i < 9; i++) {
            if (isMagicItem(p.getInventory().getItem(i))) n++;
        }
        return n;
    }

    /** True se o player usa magia (tem ao menos 1 item de magia no corpo/hotbar). */
    public static boolean isMagicUser(Player p) {
        return magicItemCount(p) > 0;
    }
}
