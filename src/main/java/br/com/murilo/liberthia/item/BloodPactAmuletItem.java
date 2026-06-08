package br.com.murilo.liberthia.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Blood Pact Amulet — enquanto segurado (OFFHAND), concede +3 ATK_DAMAGE e -4 MAX_HEALTH.
 * Forge honra getAttributeModifiers por slot.
 */
public class BloodPactAmuletItem extends Item {

    private static final UUID ATK_UUID  = UUID.fromString("0c2d5e3f-2b6d-4d8a-9f12-b3a7c9d8e01a");
    private static final UUID HP_UUID   = UUID.fromString("0c2d5e3f-2b6d-4d8a-9f12-b3a7c9d8e01b");

    private final Multimap<Attribute, AttributeModifier> offhand;

    public BloodPactAmuletItem(Properties p) {
        super(p.stacksTo(1));
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(ATK_UUID, "Blood Pact", 3.0D, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.MAX_HEALTH,
                new AttributeModifier(HP_UUID, "Blood Pact", -4.0D, AttributeModifier.Operation.ADDITION));
        this.offhand = b.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.OFFHAND) return offhand;
        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
                                java.util.List<net.minecraft.network.chat.Component> tip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tip.add(net.minecraft.network.chat.Component.literal("§4Pacto de Sangue"));
        tip.add(net.minecraft.network.chat.Component.literal("§7Equipe no slot de §ccolar§7 (Curios) §oou§r§7 segure na §coff-hand§7:"));
        tip.add(net.minecraft.network.chat.Component.literal("  §a+3§7 de ataque · §c−4 vida máx§7 (−2 corações)"));
        tip.add(net.minecraft.network.chat.Component.literal("§8§oUm pacto exige sacrifício."));
    }
}
