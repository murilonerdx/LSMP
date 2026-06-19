package br.com.murilo.liberthia.magic.weapon;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.24 r104: Hook em ProjectileImpactEvent — quando uma Arrow com NBT
 * "Liberthia.Spell.School" bate em LivingEntity, aplica SchoolDamageSource
 * extra + efeito do elemento + partículas.
 *
 * <p>v0.1.224 FIX: arrows disparadas pelo Spell Bow agora são TAGUEADAS no
 * spawn ({@link #onArrowSpawn}). Antes ninguém chamava {@code onArrowShot},
 * então as flechas saíam sem escola e nenhum efeito (fogo/gelo/etc) aplicava.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SpellArrowHandler {

    private SpellArrowHandler() {}

    /**
     * Tagueia a flecha com a escola do Spell Bow no momento em que ela nasce.
     * Durante {@code BowItem.releaseUsing} (que dá o {@code addFreshEntity}), o
     * {@code useItem} do player ainda é o arco — então lemos a escola dele.
     */
    @SubscribeEvent
    public static void onArrowSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.level() instanceof ServerLevel sl)) return;
        if (arrow.getPersistentData().contains(SpellBowItem.NBT_SCHOOL)) return; // já tagueada
        if (!(arrow.getOwner() instanceof Player player)) return;

        ItemStack using = player.getUseItem();
        if (using.getItem() instanceof SpellBowItem) {
            SpellBowItem.onArrowShot(sl, arrow, SpellBowItem.getSchool(using));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onArrowImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!(arrow.level() instanceof ServerLevel sl)) return;
        var tag = arrow.getPersistentData();
        if (!tag.contains(SpellBowItem.NBT_SCHOOL)) return;
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof LivingEntity target)) return;

        try {
            SpellSchool school = SpellSchool.valueOf(tag.getString(SpellBowItem.NBT_SCHOOL));
            SchoolDamageSource sds = switch (school) {
                case FIRE -> SchoolDamageSource.fire(120);   // ~6s de fogo
                case ICE -> SchoolDamageSource.ice(140);     // freeze acumulado
                case BLOOD -> SchoolDamageSource.blood(0.3F);
                case ELDRITCH -> SchoolDamageSource.eldritch();
                default -> SchoolDamageSource.of(school);
            };
            LivingEntity owner = arrow.getOwner() instanceof LivingEntity le ? le : null;
            target.hurt(sds.toVanilla(sl, owner), 3.0F);

            // Efeitos extras por elemento (os que não têm side-effect no DamageSource)
            applyElementExtra(sl, target, school, owner);

            sl.sendParticles(getParticleForSchool(school),
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    14, 0.3, 0.3, 0.3, 0.05);
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.debug("[SpellBow] impacto falhou: {}", t.toString());
        }
    }

    /**
     * Garante que TODO elemento faça algo visível, não só dano tipado:
     * Lightning = raio visual, Nature = veneno, Holy = brilho + smite em undead.
     * (Fire/Ice/Blood/Eldritch já agem via {@code SchoolResistanceHandler}.)
     */
    private static void applyElementExtra(ServerLevel sl, LivingEntity target,
                                          SpellSchool school, LivingEntity owner) {
        switch (school) {
            case LIGHTNING -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                if (bolt != null) {
                    bolt.moveTo(target.getX(), target.getY(), target.getZ());
                    bolt.setVisualOnly(true); // só flash+trovão; o dano vem da flecha
                    if (owner instanceof net.minecraft.server.level.ServerPlayer sp) {
                        bolt.setCause(sp);
                    }
                    sl.addFreshEntity(bolt);
                }
            }
            case NATURE -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
            case HOLY -> {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
                if (target.getMobType() == MobType.UNDEAD) {
                    target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 40));
                }
            }
            default -> { /* Fire/Ice/Blood/Eldritch já tratados pelo meta */ }
        }
    }

    private static net.minecraft.core.particles.ParticleOptions getParticleForSchool(SpellSchool s) {
        return switch (s) {
            case FIRE -> net.minecraft.core.particles.ParticleTypes.FLAME;
            case ICE -> net.minecraft.core.particles.ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK;
            case BLOOD -> net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR;
            case ELDRITCH -> net.minecraft.core.particles.ParticleTypes.SOUL;
            case HOLY -> net.minecraft.core.particles.ParticleTypes.END_ROD;
            case NATURE -> net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER;
        };
    }
}
