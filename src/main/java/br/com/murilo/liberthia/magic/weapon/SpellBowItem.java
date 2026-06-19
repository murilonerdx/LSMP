package br.com.murilo.liberthia.magic.weapon;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.24 r104: <b>Spell Bow</b> — bow que carrega arrows com SchoolDamage.
 *
 * <p>Comportamento idêntico a um bow regular MAS arrows disparadas levam
 * a school configurada (cycle via shift+R-click). Hit deals school damage
 * + particles + status effect.
 */
public class SpellBowItem extends BowItem {

    public static final String NBT_SCHOOL = "Liberthia.Spell.School";
    public static final SpellSchool[] CYCLE_SCHOOLS = {
            SpellSchool.FIRE, SpellSchool.ICE, SpellSchool.LIGHTNING,
            SpellSchool.BLOOD, SpellSchool.ELDRITCH, SpellSchool.HOLY,
            SpellSchool.NATURE
    };

    public SpellBowItem(Properties props) {
        super(props.stacksTo(1).durability(384));
    }

    public static SpellSchool getSchool(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_SCHOOL)) return SpellSchool.FIRE;
        try {
            return SpellSchool.valueOf(tag.getString(NBT_SCHOOL));
        } catch (IllegalArgumentException e) {
            return SpellSchool.FIRE;
        }
    }

    public static void cycleSchool(ItemStack stack) {
        SpellSchool current = getSchool(stack);
        int idx = current.ordinal();
        SpellSchool next = CYCLE_SCHOOLS[(idx + 1) % CYCLE_SCHOOLS.length];
        stack.getOrCreateTag().putString(NBT_SCHOOL, next.name());
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !level.isClientSide && player instanceof ServerPlayer sp) {
            cycleSchool(stack);
            SpellSchool s = getSchool(stack);
            sp.displayClientMessage(Component.literal("§5✦ Bow: ").append(s.label()), true);
            return net.minecraft.world.InteractionResultHolder.success(stack);
        }
        return super.use(level, player, hand);
    }

    /**
     * Hook em arrow → quando arrow é shot por SpellBow, aplica school NBT
     * pra entity arrow pra trackear no impact.
     */
    public static void onArrowShot(ServerLevel sl, Arrow arrow, SpellSchool school) {
        arrow.getPersistentData().putString(NBT_SCHOOL, school.name());
        // Color tracking via NBT only — setFixedColor is private
        try {
            arrow.getPersistentData().putInt("Liberthia.Color", school.colorHex());
        } catch (Throwable ignored) {}
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        SpellSchool s = getSchool(stack);
        tooltip.add(Component.literal("§7Escola: ").append(s.label()));
        tooltip.add(Component.literal("§8§oShift+R-click: cycle school"));
    }
}
