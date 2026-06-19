package br.com.murilo.liberthia.magic.accessory;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r164: <b>MagicAccessoryItem</b> — base de todos os 9 acessórios mágicos
 * (3 tipos de slot × 3 efeitos).
 *
 * <p>Os bônus são aplicados automaticamente quando o item está:
 * <ul>
 *   <li>Em qualquer slot do inventário (hotbar/storage), OU</li>
 *   <li>Equipado em slot Curios compatível ({@code ring}, {@code hands}, {@code belt})</li>
 * </ul>
 *
 * <p>Rings também valem pro slot {@code hands} por pedido do user
 * (data/curios/tags/items/hands.json inclui os 3 rings).
 */
public class MagicAccessoryItem extends Item {

    public enum SlotKind {
        RING("§7Anel — slot Curios: §dring §7+ §dhands"),
        GLOVE("§7Luva — slot Curios: §dhands"),
        BELT("§7Cinto — slot Curios: §dbelt");

        public final String tooltipLine;
        SlotKind(String l) { this.tooltipLine = l; }
    }

    public enum Effect {
        MANA_REGEN("§b♦ Regeneração de Mana", 0xFF55C8FF),
        SANITY_REGEN("§5☽ Regeneração de Sanidade", 0xFFC080FF),
        SPELL_POWER("§c✦ Poder dos Feitiços", 0xFFFF7766);

        public final String tooltipLine;
        public final int textColor;
        Effect(String l, int c) { this.tooltipLine = l; this.textColor = c; }
    }

    private final SlotKind slotKind;
    private final Effect effect;
    private final float magnitude;

    /**
     * @param slotKind tipo do slot Curios (RING/GLOVE/BELT)
     * @param effect   tipo de bônus (MANA_REGEN/SANITY_REGEN/SPELL_POWER)
     * @param magnitude valor:
     *                  <ul>
     *                    <li>MANA_REGEN: source por segundo (ex: 0.5, 1.0)</li>
     *                    <li>SANITY_REGEN: sanidade por segundo (ex: 0.2, 0.5)</li>
     *                    <li>SPELL_POWER: multiplicador adicional (ex: 0.05 = +5%)</li>
     *                  </ul>
     */
    public MagicAccessoryItem(Properties props, SlotKind slotKind, Effect effect, float magnitude) {
        super(props.stacksTo(1).rarity(Rarity.UNCOMMON));
        this.slotKind = slotKind;
        this.effect = effect;
        this.magnitude = magnitude;
    }

    public SlotKind slotKind() { return slotKind; }
    public Effect effect() { return effect; }
    public float magnitude() { return magnitude; }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(slotKind.tooltipLine));
        tip.add(Component.literal(""));
        tip.add(Component.literal(effect.tooltipLine).withStyle(ChatFormatting.BOLD));
        switch (effect) {
            case MANA_REGEN -> tip.add(Component.literal("§7+§b" + formatPerSec(magnitude) + " §7Source/s"));
            case SANITY_REGEN -> tip.add(Component.literal("§7+§d" + formatPerSec(magnitude) + " §7Sanidade/s"));
            case SPELL_POWER -> tip.add(Component.literal("§7+§c" + Math.round(magnitude * 100) + "% §7dano de feitiços"));
        }
        tip.add(Component.literal(""));
        tip.add(Component.literal("§8§oFunciona equipado §nou§r§8§o no inventário."));
    }

    private static String formatPerSec(float f) {
        if (f == Math.floor(f)) return Integer.toString((int) f);
        return String.format("%.1f", f);
    }
}
