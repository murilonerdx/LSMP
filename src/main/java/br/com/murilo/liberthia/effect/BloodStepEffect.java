package br.com.murilo.liberthia.effect;

import com.google.common.collect.Maps;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Occultism {@code StepHeightEffect} port. While active, adds +1.0 to the
 * forge step-height attribute so the wearer walks up full blocks like a
 * staircase. Stack with amplifier: each level adds 1 block.
 *
 * <p>Uses the standard MobEffect attribute-modifier mechanism so it cleans
 * up automatically when the effect expires.
 */
public class BloodStepEffect extends MobEffect {

    private static final UUID STEP_UUID = UUID.fromString("1f5d3ab1-2f7c-44b9-86b6-2d6e3b1c4a2f");

    public BloodStepEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xa83244);
        // Register the attribute modifier — Forge's STEP_HEIGHT_ADDITION attribute
        // is added below via static initializer once it's available.
    }

    /** Forge's STEP_HEIGHT_ADDITION attribute is wrapped in a Supplier (ForgeRegistries init order). */
    private static final Supplier<Attribute> STEP_ATTR = () -> ForgeMod.STEP_HEIGHT_ADDITION.get();

    @Override
    public Map<Attribute, AttributeModifier> getAttributeModifiers() {
        // We override addAttributeModifiers/removeAttributeModifiers to use the
        // dynamic Forge attribute reference instead of the static map.
        return Map.of();
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap map, int amplifier) {
        super.addAttributeModifiers(entity, map, amplifier);
        var attr = STEP_ATTR.get();
        if (attr == null) return;
        var inst = map.getInstance(attr);
        if (inst != null) {
            inst.removeModifier(STEP_UUID);
            inst.addPermanentModifier(new AttributeModifier(STEP_UUID,
                    "liberthia_blood_step",
                    1.0D + amplifier,                  // amp 0 = +1, amp 1 = +2 ...
                    AttributeModifier.Operation.ADDITION));
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap map, int amplifier) {
        super.removeAttributeModifiers(entity, map, amplifier);
        var attr = STEP_ATTR.get();
        if (attr == null) return;
        var inst = map.getInstance(attr);
        if (inst != null) inst.removeModifier(STEP_UUID);
    }

    @Override
    public boolean isInstantenous() { return false; }
//
//    @Override
//    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return false; }
}
