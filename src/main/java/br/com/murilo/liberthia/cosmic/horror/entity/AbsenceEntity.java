package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * r81 — <b>The Absence</b> (Void Horror).
 *
 * <p>Criatura completamente invisível. Detectada APENAS por:
 * <ul>
 *   <li>Particles "comidas" — sons de ambiente sumindo num raio de 5</li>
 *   <li>Mob effects DARKNESS aplicado a players próximos</li>
 *   <li>Block break sounds vindos do nada</li>
 * </ul>
 *
 * <p>Não tem hitbox visível, não renderiza modelo. Player mata por
 * tentativa-erro — atacar a posição onde sons somem.
 */
public class AbsenceEntity extends Monster {

    public AbsenceEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setInvisible(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;
        this.setInvisible(true); // garante invisível sempre

        // Particle "absence" — chamado SQUID_INK que parece sucção
        if (this.tickCount % 5 == 0 && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SQUID_INK,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    1, 0.1, 0.1, 0.1, 0);
        }

        // Aplica DARKNESS em players num raio de 8
        if (this.tickCount % 40 == 0) {
            for (Player p : this.level().getEntitiesOfClass(Player.class,
                    this.getBoundingBox().inflate(8))) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, true, false));
            }
        }

        // Persegue lentamente
        Player target = this.level().getNearestPlayer(this, 24);
        if (target != null) {
            this.getNavigation().moveTo(target, 0.6);
        }
    }
}
