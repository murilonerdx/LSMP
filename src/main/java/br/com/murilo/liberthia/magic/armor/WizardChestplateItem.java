package br.com.murilo.liberthia.magic.armor;

import br.com.murilo.liberthia.magic.school.SchoolResistanceHandler;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.24 r101: <b>Wizard Chestplate</b> — armor temática por school.
 *
 * <p>Cada chestplate:
 * <ul>
 *   <li>+40 resistance da própria school</li>
 *   <li>+15% spell damage da school (NBT bonus)</li>
 *   <li>Bônus passivo único por school (fire-immune, snow-walker, etc)</li>
 * </ul>
 */
public class WizardChestplateItem extends ArmorItem {

    public final SpellSchool school;

    public WizardChestplateItem(Properties props, SpellSchool school) {
        super(ArmorMaterials.NETHERITE, ArmorItem.Type.CHESTPLATE,
                props.stacksTo(1).durability(600).fireResistant());
        this.school = school;
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        super.onArmorTick(stack, level, player);
        if (level.isClientSide) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest != stack) return;

        if (level.getGameTime() % 40 != 0) return;

        // Aplica resistance + spell damage bonus
        SchoolResistanceHandler.setResistance(player, school, 40);
        player.getPersistentData().putInt("liberthia.armor_spell_damage_" + school.name(), 15);

        // Passivo único por school
        switch (school) {
            case FIRE -> {
                if (player.getRemainingFireTicks() > 0) player.setRemainingFireTicks(0);
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, true, false));
            }
            case ICE -> {
                // Snow walker — não slip on ice
                player.setTicksFrozen(0);
            }
            case LIGHTNING -> {
                // Boost de speed quando sprinting
                if (player.isSprinting()) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0, true, false));
                }
            }
            case BLOOD -> {
                // Regen lento quando low HP
                if (player.getHealth() < player.getMaxHealth() * 0.4F) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
                }
            }
            case ELDRITCH -> {
                // Night vision passiva
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false));
            }
            case HOLY -> {
                // Absorption +2 passiva
                if (player.getAbsorptionAmount() < 4) {
                    player.setAbsorptionAmount(Math.min(4, player.getAbsorptionAmount() + 0.2F));
                }
            }
            case NATURE -> {
                // Saturation lenta
                if (player.tickCount % 100 == 0
                        && player.getFoodData().getFoodLevel() < 20) {
                    player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() + 1);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Escola: ").append(school.label()));
        tooltip.add(Component.literal("§b+40 ").append(school.label()).append(" §bResistance"));
        tooltip.add(Component.literal("§b+15% §bSpell Damage"));
        String passive = switch (school) {
            case FIRE -> "Fire Immune";
            case ICE -> "Frost Walker";
            case LIGHTNING -> "Sprint = Speed";
            case BLOOD -> "Low HP → Regen";
            case ELDRITCH -> "Night Vision";
            case HOLY -> "+2 Absorption passive";
            case NATURE -> "Slow Saturation";
        };
        tooltip.add(Component.literal("§d§o" + passive));
    }
}
