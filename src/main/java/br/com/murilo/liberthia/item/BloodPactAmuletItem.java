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
}
