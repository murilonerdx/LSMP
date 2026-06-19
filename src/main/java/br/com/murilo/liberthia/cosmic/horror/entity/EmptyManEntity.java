package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

/**
 * r81 — <b>Empty Man</b> (Uncanny Valley horror).
 *
 * <p>Aparece humanóide na distância (~30-50 blocos). Quando o player tenta
 * se aproximar a &lt; 8 blocos, <b>desaparece silenciosamente</b>. Não persegue,
 * não ataca. Sorri.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>HP 1 (qualquer dano mata)</li>
 *   <li>{@link #aiStep} verifica se player próximo &lt; 8 blocos — se sim, despawn</li>
 *   <li>Sem ataques, sem pathing AI</li>
 *   <li>Imune a fogo/lava/queda (não é tangível)</li>
 * </ul>
 */
public class EmptyManEntity extends Monster {

    public EmptyManEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        // Verifica player próximo
        Player nearest = this.level().getNearestPlayer(this, 32);
        if (nearest != null && nearest.distanceTo(this) < 8) {
            // Despawn silencioso — sem som/particle
            this.discard();
        }

        // A cada 40 ticks, lentamente rotaciona pra olhar pro player
        if (this.tickCount % 5 == 0 && nearest != null) {
            double dx = nearest.getX() - this.getX();
            double dz = nearest.getZ() - this.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            this.setYHeadRot(yaw);
            this.setYBodyRot(yaw);
        }
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity e) {
        return true; // não ataca nada
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // Morre instantaneamente — mas sem particle ou som
        this.discard();
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // só removido quando player chega perto
    }
}
