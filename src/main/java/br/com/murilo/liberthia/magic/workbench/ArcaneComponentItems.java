package br.com.murilo.liberthia.magic.workbench;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r159: Componentes necessários pra craftar feitiço no Arcane Workbench.
 *
 * <p>Items genéricos marcadores. Cada um tem uma classe própria pra que o
 * {@link ArcaneSlotType} consiga validar por instanceof.
 */
public final class ArcaneComponentItems {

    private ArcaneComponentItems() {}

    /** Tablet — material base que estabiliza a magia. */
    public static class MagicTablet extends Item {
        public MagicTablet(Properties p) { super(p.rarity(Rarity.UNCOMMON)); }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§7Componente: §dTablet"));
            t.add(Component.literal("§8Slot tablet no Arcane Workbench"));
            t.add(Component.literal("§8§oMaterial de craft — não faz nada na mão."));
        }
    }

    /** Orb genérico — núcleo de energia mágica. */
    public static class ArcaneOrb extends Item {
        public ArcaneOrb(Properties p) { super(p.rarity(Rarity.RARE)); }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§7Componente: §bOrb"));
            t.add(Component.literal("§8Slot orb no Arcane Workbench"));
            t.add(Component.literal("§8§oMaterial de craft — não faz nada na mão."));
        }
    }

    /** Runa de escola — marca a escola do feitiço (7 variantes). */
    public static class SchoolRune extends Item {
        public final String schoolName;
        public SchoolRune(Properties p, String schoolName) {
            super(p.rarity(Rarity.UNCOMMON));
            this.schoolName = schoolName;
        }
        @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§7Componente: §5Runa de Escola"));
            t.add(Component.literal("§dEscola: §f" + schoolName.toUpperCase()));
            t.add(Component.literal("§8Slot escola no Arcane Workbench"));
            t.add(Component.literal("§8§oMaterial de craft — não faz nada na mão."));
        }
    }
}
