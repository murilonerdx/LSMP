package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * r81 — <b>The Observer</b> (Perception Horror).
 *
 * <p>SCP-173 inspired. Move APENAS quando algum player próximo tem inventário
 * aberto. Quando alguém olha pra ele (line of sight + pitch direto), congela.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>Static por padrão (movement_speed = 0)</li>
 *   <li>Quando NENHUM player tem line-of-sight: anda 0.4 speed em direção
 *       ao player mais próximo</li>
 *   <li>Em contato (dist &lt; 1.5): mata instantaneamente</li>
 *   <li>Imune a knockback, dano físico (só dano mágico)</li>
 * </ul>
 */
public class ObserverEntity extends Monster {

    public ObserverEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, 60.0)
                .add(Attributes.ATTACK_DAMAGE, 30.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        Player nearest = this.level().getNearestPlayer(this, 48);
        if (nearest == null) {
            this.getNavigation().stop();
            return;
        }

        // Verifica se ALGUM player tem line of sight + olhando direto pra ele
        boolean watched = false;
        for (Player p : this.level().players()) {
            if (p.distanceTo(this) > 32) continue;
            if (!p.hasLineOfSight(this)) continue;
            // Calcula dot produto entre olhar do player e direção pro observer
            var look = p.getLookAngle();
            double dx = this.getX() - p.getX();
            double dy = this.getY() - p.getY();
            double dz = this.getZ() - p.getZ();
            double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (len < 0.001) continue;
            double dot = (look.x * dx + look.y * dy + look.z * dz) / len;
            if (dot > 0.7) {
                watched = true;
                break;
            }
        }

        if (watched) {
            // CONGELA
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        } else {
            // Move TURBO pro player
            this.getNavigation().moveTo(nearest, 1.2);

            // Em contato — mata
            if (this.distanceTo(nearest) < 1.6) {
                nearest.hurt(this.damageSources().mobAttack(this), 999.0F);
            }
        }
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // r145: removida imunidade que travava ataques fisicos
        // (antes so dano magico/explosao/wither passava, dava sensacao de hitbox bugada).
        // Agora toma dano normal — continua imune a knockback via attribute.
        return super.hurt(source, amount);
    }
}
