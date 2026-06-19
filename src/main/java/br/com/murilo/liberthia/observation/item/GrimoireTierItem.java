package br.com.murilo.liberthia.observation.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r74: <b>Grimoire Tier System</b> — 3 níveis progressivos.
 *
 * <p>Pattern AN's SpellBook tiers (Apprentice/Master/Creative).
 *
 * <h2>Tiers</h2>
 * <ul>
 *   <li>{@link Apprentice} — max 8 glyphs, 200 durability, 1× damage</li>
 *   <li>{@link Master} — max 12 glyphs, 500 durability, 1.25× damage</li>
 *   <li>{@link Archmage} — max 16 glyphs, 1500 durability, 1.5× damage</li>
 * </ul>
 */
public class GrimoireTierItem extends GrimoireOfObservationItem {

    public final int tier;
    public final int maxGlyphs;
    public final float damageMult;
    public final String tierName;

    public GrimoireTierItem(Properties p, int tier, int maxGlyphs, float damageMult, String tierName, int durability) {
        super(p.stacksTo(1).durability(durability).rarity(rarityForTier(tier)));
        this.tier = tier;
        this.maxGlyphs = maxGlyphs;
        this.damageMult = damageMult;
        this.tierName = tierName;
    }

    private static Rarity rarityForTier(int t) {
        return switch (t) {
            case 1 -> Rarity.UNCOMMON;
            case 2 -> Rarity.RARE;
            case 3 -> Rarity.EPIC;
            default -> Rarity.COMMON;
        };
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        super.appendHoverText(s, l, t, f);
        t.add(Component.empty());
        t.add(Component.literal("§6§l⭑ Tier " + tier + " — " + tierName));
        t.add(Component.literal("§7Glyphs máx: §e" + maxGlyphs));
        t.add(Component.literal("§7Damage mult: §e×" + damageMult));
        t.add(Component.literal("§7Durabilidade: §e" + getMaxDamage(s)));
    }

    /** Subclasses pré-criadas. */
    public static class Apprentice extends GrimoireTierItem {
        public Apprentice(Properties p) {
            super(p, 1, 8, 1.0f, "Aprendiz", 200);
        }
    }

    public static class Master extends GrimoireTierItem {
        public Master(Properties p) {
            super(p, 2, 12, 1.25f, "Mestre", 500);
        }
    }

    public static class Archmage extends GrimoireTierItem {
        public Archmage(Properties p) {
            super(p, 3, 16, 1.5f, "Arquimago", 1500);
        }
    }
}
