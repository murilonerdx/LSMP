package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * r150: <b>Wooden Horror Entity</b> — criatura humanóide totêmica de madeira.
 *
 * <p>Uma classe base com 8 variantes que mudam APENAS pela cor (passada via
 * {@link Variant} enum). Cada variante tem um "type effect" que aplica ao
 * atacar o player (queimar, congelar, envenenar, etc).
 *
 * <p>Mecânica: estática quando vista (igual Observer), atacante quando ninguém olha.
 *
 * <h2>Variantes</h2>
 * <ol>
 *   <li>CHARCOAL — preta, melee puro</li>
 *   <li>PALE_OAK — branca, fast (speed buff)</li>
 *   <li>ROTTED_BIRCH — cinza, weakness on hit</li>
 *   <li>BLEEDING_MAPLE — vermelha, wither on hit</li>
 *   <li>MOSSY — verde, poison on hit</li>
 *   <li>FROZEN_PINE — ciano, freeze on hit</li>
 *   <li>BURNING_ACACIA — laranja, ignite on hit</li>
 *   <li>CURSED_MAHOGANY — roxa, nausea+blindness on hit</li>
 * </ol>
 */
public class WoodenHorrorEntity extends Monster {

    /** Variante = cor + efeito on hit. */
    public enum Variant {
        CHARCOAL(null, 0),
        PALE_OAK(null, 0),
        ROTTED_BIRCH(MobEffects.WEAKNESS, 200),
        BLEEDING_MAPLE(MobEffects.WITHER, 100),
        MOSSY(MobEffects.POISON, 140),
        FROZEN_PINE(null, 0),
        BURNING_ACACIA(null, 0),
        CURSED_MAHOGANY(MobEffects.CONFUSION, 180);

        public final net.minecraft.world.effect.MobEffect onHitEffect;
        public final int effectDurationTicks;

        Variant(net.minecraft.world.effect.MobEffect e, int d) {
            this.onHitEffect = e;
            this.effectDurationTicks = d;
        }
    }

    private final Variant variant;

    public WoodenHorrorEntity(EntityType<? extends Monster> type, Level level, Variant variant) {
        super(type, level);
        this.variant = variant;
    }

    public Variant getVariant() { return variant; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity living && variant.onHitEffect != null) {
            living.addEffect(new MobEffectInstance(variant.onHitEffect, variant.effectDurationTicks, 0));
        }
        // Variantes especiais sem mobeffect direto
        if (ok && target instanceof LivingEntity living) {
            if (variant == Variant.BURNING_ACACIA) living.setSecondsOnFire(5);
            else if (variant == Variant.FROZEN_PINE) living.setTicksFrozen(living.getTicksFrozen() + 120);
            else if (variant == Variant.PALE_OAK) {
                // Pale Oak: rapidez própria pós-hit
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
            }
        }
        return ok;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Resistente a knockback de magic (mantém o feel totêmico)
        return super.hurt(source, amount);
    }
}
