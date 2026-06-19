package br.com.murilo.liberthia.magic.boss;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * v0.1.24 r98: <b>Lich Hunter</b> — minion ranged do boss. Mantém distância
 * e atira eldritch bolts.
 */
public class LichHunterEntity extends Monster {

    private int castCooldown = 0;

    public LichHunterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 25.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (castCooldown > 0) castCooldown--;
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel sl)) return;

        LivingEntity target = getTarget();
        if (target == null) return;

        double dist = distanceTo(target);

        // Mantém distância 8-14
        if (dist < 8) {
            Vec3 away = position().subtract(target.position()).normalize().scale(4);
            getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, 1.0);
        } else if (dist > 14) {
            getNavigation().moveTo(target, 0.8);
        } else {
            getNavigation().stop();
        }

        // Cast eldritch bolt
        if (castCooldown == 0 && hasLineOfSight(target) && dist <= 18) {
            castBolt(sl, target);
            castCooldown = 50;
        }
    }

    private void castBolt(ServerLevel sl, LivingEntity target) {
        Vec3 from = position().add(0, 1.5, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0);
        Vec3 dir = to.subtract(from);
        double dist = dir.length();
        Vec3 norm = dir.normalize();
        int steps = (int) (dist * 2);
        for (int i = 0; i < steps; i++) {
            double f = i / (double) steps;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    from.x + norm.x * dist * f,
                    from.y + norm.y * dist * f,
                    from.z + norm.z * dist * f,
                    1, 0.1, 0.1, 0.1, 0);
        }
        target.hurt(SchoolDamageSource.of(SpellSchool.ELDRITCH).toVanilla(sl, this), 4F);
    }
}
