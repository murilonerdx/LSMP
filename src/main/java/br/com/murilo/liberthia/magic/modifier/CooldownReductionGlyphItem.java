package br.com.murilo.liberthia.magic.modifier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r154: <b>Cooldown Reduction Glyph</b> — diminui cooldown de feitiços em troca de mana.
 *
 * <p>Cada glyph deste no <b>inventário</b> reduz o cooldown de todos os spells em
 * <b>10% (multiplicativo)</b> mas aumenta o custo de mana em <b>15%</b>.
 *
 * <p>Stackable até 5× (máximo de redução prática):
 * <ul>
 *   <li>1 glyph: CD ×0.90 (10% off), custo ×1.15 (15% mais)</li>
 *   <li>2 glyphs: CD ×0.81 (19% off), custo ×1.32</li>
 *   <li>3 glyphs: CD ×0.73 (27% off), custo ×1.52</li>
 *   <li>5 glyphs: CD ×0.59 (41% off), custo ×2.01 — break-even</li>
 * </ul>
 *
 * <p>Não tem cap rígido — você pode usar 10+ glyphs mas o custo de mana
 * fica proibitivo. Spell barata bombea, spell cara fica impossível.
 */
public class CooldownReductionGlyphItem extends Item {

    public static final float CDR_PER_GLYPH = 0.10F;     // 10%
    public static final float COST_PER_GLYPH = 0.15F;    // 15%

    public CooldownReductionGlyphItem(Properties props) {
        super(props.stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    /** Conta quantos glyphs estão no inventário do player (qualquer slot). */
    public static int countInInventory(Player p) {
        int count = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.getItem() instanceof CooldownReductionGlyphItem) {
                count += s.getCount();
            }
        }
        return count;
    }

    /** Multiplicador de cooldown (1.0 sem reduce, < 1.0 com reduce). */
    public static float cooldownMultiplier(Player p) {
        int n = countInInventory(p);
        if (n <= 0) return 1.0F;
        return (float) Math.pow(1.0 - CDR_PER_GLYPH, n);
    }

    /** Multiplicador de custo de mana (1.0 base, > 1.0 com penalty). */
    public static float manaMultiplier(Player p) {
        int n = countInInventory(p);
        if (n <= 0) return 1.0F;
        return 1.0F + (COST_PER_GLYPH * n);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Cada glyph reduz CD em §a-10% §7e aumenta custo em §c+15%"));
        tooltip.add(Component.literal("§7Multiplicativo, stackável."));
        tooltip.add(Component.literal("§o§8Mantenha no inventário pra ativar."));
        // Status do player LOCAL — isolado (DistExecutor + classe aninhada) pra NUNCA
        // carregar Minecraft/LocalPlayer no servidor dedicado (crashava o registro do item).
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> ClientTip.append(tooltip));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    /** Client-only: status do player local. Só carrega Minecraft quando chamado no client. */
    private static final class ClientTip {
        static void append(List<Component> tooltip) {
            Player p = net.minecraft.client.Minecraft.getInstance().player;
            if (p == null) return;
            int n = countInInventory(p);
            if (n <= 0) return;
            float cdMult = cooldownMultiplier(p);
            float manaMult = manaMultiplier(p);
            int cdReduce = Math.round((1F - cdMult) * 100);
            int manaInc = Math.round((manaMult - 1F) * 100);
            tooltip.add(Component.literal(""));
            tooltip.add(Component.literal("§7Ativos: §f" + n + "×").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("§7CD: §a-" + cdReduce + "% §7| Custo: §c+" + manaInc + "%"));
        }
    }
}
