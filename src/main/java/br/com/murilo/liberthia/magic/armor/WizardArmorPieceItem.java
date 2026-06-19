package br.com.murilo.liberthia.magic.armor;

import br.com.murilo.liberthia.magic.school.SchoolResistanceHandler;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.24 r107: <b>Wizard Armor Piece</b> — helmet/legs/boots por school.
 * Cada peça da +10 resistance cumulativo. Set completo (helmet+chest+legs+boots)
 * dá +20 SET bonus + spell damage doubled.
 */
public class WizardArmorPieceItem extends ArmorItem {

    public final SpellSchool school;

    public WizardArmorPieceItem(Properties props, SpellSchool school, ArmorItem.Type type) {
        super(ArmorMaterials.NETHERITE, type, props.stacksTo(1).durability(500).fireResistant());
        this.school = school;
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        super.onArmorTick(stack, level, player);
        if (level.isClientSide) return;
        if (level.getGameTime() % 40 != 0) return;

        // Soma resistance de todas as armor pieces da school equipadas
        int piecesEquipped = countEquippedPieces(player);
        if (piecesEquipped == 0) return;

        // Apply resistance — cada peça contribui
        SchoolResistanceHandler.setResistance(player, school, piecesEquipped * 10);

        // SET BONUS: 4 peças = +30 extra resistance + double spell damage
        if (piecesEquipped == 4) {
            SchoolResistanceHandler.setResistance(player, school, 70);
            player.getPersistentData().putInt("liberthia.armor_spell_damage_" + school.name(), 30);
            // Aplica buff sutil — strength brief
            if (level.getGameTime() % 200 == 0) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 80, 0, true, false));
            }
        }
    }

    private int countEquippedPieces(Player p) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = p.getItemBySlot(slot);
            if (s.getItem() instanceof WizardArmorPieceItem wp && wp.school == this.school) {
                count++;
            } else if (s.getItem() instanceof WizardChestplateItem wc && wc.school == this.school) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Escola: ").append(school.label()));
        tooltip.add(Component.literal("§b+10 ").append(school.label()).append(" §bResistance"));
        tooltip.add(Component.literal("§5§o4 peças: §d§oSET BONUS"));
    }
}
