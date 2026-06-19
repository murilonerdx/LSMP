package br.com.murilo.liberthia.magic.scroll;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * v0.1.24 r93: <b>School Scroll</b> — single-use scroll que casta um spell
 * direcionado pelo crosshair.
 *
 * <p>Effects por escola:
 * <ul>
 *   <li>FIRE — fireball cone (5 dano + burn 80t)</li>
 *   <li>ICE — frost lance (3 dano + freeze 100t)</li>
 *   <li>LIGHTNING — chain bolt (6 dano lightning)</li>
 *   <li>BLOOD — vampiric strike (5 dano + lifesteal 60%)</li>
 *   <li>ELDRITCH — void rip (4 dano + DARKNESS)</li>
 *   <li>HOLY — divine smite (8 dano vs undead, regen pra player)</li>
 *   <li>NATURE — root spike (3 dano + slowness IV)</li>
 * </ul>
 *
 * <p>Consumido ao usar. Cooldown 20t.
 */
public class SchoolScrollItem extends Item {

    private final SpellSchool school;

    public SchoolScrollItem(Properties props, SpellSchool school) {
        super(props.stacksTo(16));
        this.school = school;
    }

    public SpellSchool getSchool() { return school; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (sp.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        // r164 FIX: entity raycast real — sp.pick() só retorna BlockHitResult
        LivingEntity target = br.com.murilo.liberthia.util.EntityRaycast.pickLiving(sp, 20.0);
        if (target == null) {
            sp.displayClientMessage(Component.literal(
                    "§7§o✦ Aponte pra uma criatura."), true);
            return InteractionResultHolder.fail(stack);
        }

        castEffect(sp, target);

        sp.getCooldowns().addCooldown(this, 20);
        if (!sp.isCreative()) stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }

    private void castEffect(ServerPlayer sp, LivingEntity target) {
        ServerLevel sl = sp.serverLevel();
        switch (school) {
            case FIRE -> {
                target.hurt(SchoolDamageSource.fire(80).toVanilla(sl, sp), 5);
                target.setRemainingFireTicks(80);
                drawBeam(sl, sp, target, ParticleTypes.FLAME);
            }
            case ICE -> {
                target.hurt(SchoolDamageSource.ice(100).toVanilla(sl, sp), 3);
                target.setTicksFrozen(target.getTicksFrozen() + 100);
                drawBeam(sl, sp, target, ParticleTypes.SNOWFLAKE);
            }
            case LIGHTNING -> {
                target.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(sl, sp), 6);
                drawBeam(sl, sp, target, ParticleTypes.ELECTRIC_SPARK);
                var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
                if (bolt != null) {
                    bolt.moveTo(target.getX(), target.getY(), target.getZ());
                    bolt.setVisualOnly(true);
                    sl.addFreshEntity(bolt);
                }
            }
            case BLOOD -> {
                target.hurt(SchoolDamageSource.blood(0.6F).toVanilla(sl, sp), 5);
                drawBeam(sl, sp, target, ParticleTypes.DAMAGE_INDICATOR);
            }
            case ELDRITCH -> {
                target.hurt(SchoolDamageSource.eldritch().toVanilla(sl, sp), 4);
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, true, true));
                drawBeam(sl, sp, target, ParticleTypes.SOUL);
            }
            case HOLY -> {
                float dmg = target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD ? 16 : 8;
                target.hurt(SchoolDamageSource.of(SpellSchool.HOLY).toVanilla(sl, sp), dmg);
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, true, false));
                drawBeam(sl, sp, target, ParticleTypes.END_ROD);
            }
            case NATURE -> {
                target.hurt(SchoolDamageSource.of(SpellSchool.NATURE).toVanilla(sl, sp), 3);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3, true, true));
                drawBeam(sl, sp, target, ParticleTypes.HAPPY_VILLAGER);
            }
        }
    }

    private static void drawBeam(ServerLevel sl, ServerPlayer sp, LivingEntity target,
                                  net.minecraft.core.particles.ParticleOptions particle) {
        Vec3 from = sp.position().add(0, 1.4, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0);
        Vec3 dir = to.subtract(from);
        double dist = dir.length();
        Vec3 norm = dir.normalize();
        int steps = (int) (dist * 2);
        for (int i = 0; i < steps; i++) {
            double f = i / (double) steps;
            sl.sendParticles(particle,
                    from.x + norm.x * dist * f,
                    from.y + norm.y * dist * f,
                    from.z + norm.z * dist * f,
                    1, 0.05, 0.05, 0.05, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Escola: ").append(school.label()));
        tooltip.add(Component.literal("§8§oConsumido ao usar. Cooldown 1s."));
    }
}
