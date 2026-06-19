package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

/**
 * r81 — <b>The Remembered</b> (Memetic Horror).
 *
 * <p>Só existe enquanto players falam dela. Spawnada pelo {@link
 * br.com.murilo.liberthia.cosmic.systems.MemeticSystem} quando o contador
 * global de menções passa o threshold.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>Spawn invisível, particles SOUL flutuam ao redor</li>
 *   <li>Quando players param de mencioná-la, despawna em 60s</li>
 *   <li>Ataque suicídio: explode com soul flame ao tocar player</li>
 * </ul>
 */
public class RememberedEntity extends Monster {

    private int lastMentionTick = 0;

    public RememberedEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.lastMentionTick = this.tickCount;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        // Particle aura
        if (this.tickCount % 10 == 0 && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL,
                    this.getX(), this.getY() + 0.5, this.getZ(),
                    3, 0.3, 0.5, 0.3, 0.01);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(), this.getY() + 1, this.getZ(),
                    1, 0.1, 0.1, 0.1, 0);
        }

        // Despawn após 1200t (60s) sem ser "lembrada"
        if (this.tickCount - this.lastMentionTick > 1200) {
            this.discard();
            return;
        }

        Player target = this.level().getNearestPlayer(this, 16);
        if (target != null) {
            this.getNavigation().moveTo(target, 0.7);
        }
    }

    /** Chamado pelo MemeticSystem quando algum player menciona triggers. */
    public void onMentioned() {
        this.lastMentionTick = this.tickCount;
    }
}
