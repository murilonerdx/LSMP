package br.com.murilo.liberthia.magic.boss;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
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
 * v0.1.24 r98: <b>Abyssal Lich</b> — boss em 3 fases.
 *
 * <h2>Fases</h2>
 * <ul>
 *   <li><b>Phase 1 (100%-66% HP)</b> — Caster: lança void bolts em raio 20</li>
 *   <li><b>Phase 2 (66%-33%)</b> — Soma 2 Stalkers + 1 Hunter, ataques mais
 *       rápidos, blindness aura raio 8</li>
 *   <li><b>Phase 3 (33%-0%)</b> — Berserk: damage +50%, teleport agressivo,
 *       lightning storm a cada 100t</li>
 * </ul>
 *
 * <p>BossEvent visível no HUD. Music handler placeholder (uses warden
 * ambient).
 */
public class AbyssalLichEntity extends Monster {

    private final ServerBossEvent bossEvent;
    private int phase = 1;
    private int attackCooldown = 0;
    private boolean phase2SpawnedAdds = false;

    public AbyssalLichEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(
                Component.literal("§5§lLich do Abismo"),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS);
        this.bossEvent.setDarkenScreen(true);
        this.bossEvent.setCreateWorldFog(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (attackCooldown > 0) attackCooldown--;
        if (level().isClientSide) return;

        // Update boss bar
        bossEvent.setProgress(getHealth() / getMaxHealth());

        // Phase transitions
        float pct = getHealth() / getMaxHealth();
        int newPhase = pct > 0.66F ? 1 : pct > 0.33F ? 2 : 3;
        if (newPhase != phase) {
            phase = newPhase;
            onPhaseChange();
        }

        // Phase-specific behavior
        if (level() instanceof ServerLevel sl) {
            switch (phase) {
                case 1 -> phase1(sl);
                case 2 -> phase2(sl);
                case 3 -> phase3(sl);
            }
        }
    }

    private void onPhaseChange() {
        if (!(level() instanceof ServerLevel sl)) return;
        // Visual explosion of phase shift
        for (int i = 0; i < 50; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * 3;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    getX() + Math.cos(a) * r, getY() + 1, getZ() + Math.sin(a) * r,
                    2, 0.2, 0.5, 0.2, 0.1);
        }
        // Phase announcement
        bossEvent.setName(Component.literal(
                "§5§lLich do Abismo §7- §dFase " + phase));
        if (phase == 2) {
            bossEvent.setColor(BossEvent.BossBarColor.RED);
        } else if (phase == 3) {
            bossEvent.setColor(BossEvent.BossBarColor.WHITE);
            bossEvent.setName(Component.literal("§4§l§oLich do Abismo §c§l- BERSERK"));
        }
        // Sound
        sl.playSound(null, blockPosition(),
                net.minecraft.sounds.SoundEvents.WARDEN_ROAR,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    private void phase1(ServerLevel sl) {
        LivingEntity target = getTarget();
        if (attackCooldown == 0 && target != null) {
            castVoidBolt(sl, target);
            attackCooldown = 60;
        }
    }

    private void phase2(ServerLevel sl) {
        // Spawn adds (1x)
        if (!phase2SpawnedAdds) {
            spawnMinions(sl);
            phase2SpawnedAdds = true;
        }
        // Blindness aura
        if (tickCount % 60 == 0) {
            var players = sl.getEntitiesOfClass(Player.class,
                    getBoundingBox().inflate(8));
            for (var p : players) {
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.BLINDNESS, 80, 0, true, false));
            }
        }
        // Mais ataques
        LivingEntity target2 = getTarget();
        if (attackCooldown == 0 && target2 != null) {
            castVoidBolt(sl, target2);
            attackCooldown = 40;
        }
    }

    private void phase3(ServerLevel sl) {
        LivingEntity target = getTarget();
        // Lightning storm cada 100t
        if (tickCount % 100 == 0 && target != null) {
            for (int i = 0; i < 4; i++) {
                Vec3 strike = target.position().add(
                        (Math.random() - 0.5) * 6, 0, (Math.random() - 0.5) * 6);
                var bolt = EntityType.LIGHTNING_BOLT.create(sl);
                if (bolt != null) {
                    bolt.moveTo(strike.x, strike.y, strike.z);
                    sl.addFreshEntity(bolt);
                }
            }
        }
        // Teleport agressivo
        if (tickCount % 80 == 0 && target != null) {
            Vec3 behind = target.position().subtract(target.getLookAngle().scale(2));
            randomTeleport(behind.x, behind.y, behind.z, true);
        }
        if (attackCooldown == 0 && target != null) {
            castVoidBolt(sl, target);
            attackCooldown = 25;
        }
    }

    private void castVoidBolt(ServerLevel sl, LivingEntity target) {
        // Beam particles
        Vec3 from = position().add(0, 1.5, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0);
        Vec3 dir = to.subtract(from);
        double dist = dir.length();
        Vec3 norm = dir.normalize();
        int steps = (int) (dist * 2);
        for (int i = 0; i < steps; i++) {
            double f = i / (double) steps;
            sl.sendParticles(ParticleTypes.SOUL,
                    from.x + norm.x * dist * f,
                    from.y + norm.y * dist * f,
                    from.z + norm.z * dist * f,
                    1, 0.1, 0.1, 0.1, 0);
        }
        // Damage
        float dmg = phase == 3 ? 12 : phase == 2 ? 9 : 7;
        target.hurt(SchoolDamageSource.eldritch().toVanilla(sl, this), dmg);
    }

    private void spawnMinions(ServerLevel sl) {
        // 2 Stalker + 1 Hunter
        for (int i = 0; i < 2; i++) {
            var stalker = br.com.murilo.liberthia.registry.ModEntities.LICH_STALKER.get().create(sl);
            if (stalker != null) {
                stalker.moveTo(getX() + (Math.random() - 0.5) * 4,
                        getY(),
                        getZ() + (Math.random() - 0.5) * 4, 0, 0);
                sl.addFreshEntity(stalker);
            }
        }
        var hunter = br.com.murilo.liberthia.registry.ModEntities.LICH_HUNTER.get().create(sl);
        if (hunter != null) {
            hunter.moveTo(getX() + 2, getY(), getZ() + 2, 0, 0);
            sl.addFreshEntity(hunter);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }
}
