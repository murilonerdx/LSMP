package br.com.murilo.liberthia.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>Punhos Celestiais</b> — arma de soco. Pode ser SEGURADA na mão OU equipada
 * no slot Curios {@code hands} (aí buffa seus socos de mão livre). As mecânicas
 * (combo 3 golpes, dash ofensivo, fúria) vivem em {@code event/CelestialFistsHandler}.
 */
public class CelestialFistsItem extends Item {

    private final Multimap<Attribute, AttributeModifier> mainhand;

    public CelestialFistsItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        // base = 1 (punho) + 2 = 3 (= 1º golpe do combo). Rápido.
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 2.0, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                BASE_ATTACK_SPEED_UUID, "Weapon modifier", -1.6, AttributeModifier.Operation.ADDITION)); // ~2.4/s
        mainhand = b.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return slot == EquipmentSlot.MAINHAND ? mainhand : super.getAttributeModifiers(slot, stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("✦ Punhos Celestiais").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§7Slot Curios: §fmãos §8(ou segure na mão)"));
        tooltip.add(Component.literal("§7Combo 3 golpes: §f3 §7→ §e5 §7→ §c8 §7(+knockback) §8em 1s"));
        tooltip.add(Component.literal("§7Dash Ofensivo: §fcorrendo + golpe → avanço + dano em área"));
        tooltip.add(Component.literal("§7Fúria Celestial: §f10 golpes seguidos → velocidade de ataque 5s"));
    }
}
