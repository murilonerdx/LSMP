package br.com.murilo.liberthia.cosmic.horror.entity;

import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * <b>A Mulher do Horizonte</b> — mob de terror que aparece SEMPRE AO LONGE.
 *
 * <p>r180b (report #72): redesenhada. Antes ficava PARADA (speed 0) e morria em
 * 1 tapa (HP 1 + hurt→discard). Agora:
 * <ul>
 *   <li><b>FOGE</b> do player ({@link AvoidEntityGoal}) — mantém distância, nunca
 *       deixa chegar perto.</li>
 *   <li><b>Intocável de perto</b>: qualquer golpe melee a faz <b>piscar pra longe</b>
 *       (16-28 blocos) em vez de morrer.</li>
 *   <li><b>Aura de horror</b>: quem chega perto fica <b>lento + cego</b> e ouve
 *       <b>sussurros</b> (sugestão do tester).</li>
 * </ul>
 * Continua persistente (não some) — spawn por ovo é totalmente testável.
 */
public class MulherDoHorizonteEntity extends Monster {

    /** r180: spawnada por OVO → persistente. Mantido p/ compat de NBT. */
    private boolean manualSpawn = false;

    public MulherDoHorizonteEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // foge de qualquer player num raio de 18b; anda 1.0x longe, 1.5x (corrida) perto
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 18.0F, 1.0D, 1.5D));
        // quando parada/longe, encara o player (clima de "ela está te observando")
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 64.0F));
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance diff,
            net.minecraft.world.entity.MobSpawnType reason, net.minecraft.world.entity.SpawnGroupData data,
            net.minecraft.nbt.CompoundTag tag) {
        if (reason == net.minecraft.world.entity.MobSpawnType.SPAWN_EGG) this.manualSpawn = true;
        return super.finalizeSpawn(level, diff, reason, data, tag);
    }

    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag t) {
        super.addAdditionalSaveData(t); t.putBoolean("ManualSpawn", manualSpawn);
    }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag t) {
        super.readAdditionalSaveData(t); manualSpawn = t.getBoolean("ManualSpawn");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)   // rápida o bastante pra escapar
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;

        Player nearest = this.level().getNearestPlayer(this, 32);
        if (nearest == null || nearest.isCreative() || nearest.isSpectator()) return;
        double d = nearest.distanceTo(this);

        // sussurros conforme você se aproxima
        if (d < 16 && this.tickCount % 50 == 0) {
            this.level().playSound(null, nearest.getX(), nearest.getY(), nearest.getZ(),
                    ModSounds.COSMIC_DISTANT_WHISPERS.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
        }
        // se CONSEGUIU chegar perto → lentidão + cegueira (sugestão do tester)
        if (d < 8) {
            nearest.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
            nearest.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        }
    }

    /** Pisca pra um ponto distante (16-28b) do alvo — usada ao tomar hit. */
    private void blinkAway(Entity threat) {
        RandomSource r = this.getRandom();
        double ox = threat != null ? threat.getX() : this.getX();
        double oz = threat != null ? threat.getZ() : this.getZ();
        for (int i = 0; i < 16; i++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double dist = 16 + r.nextDouble() * 12;
            double nx = ox + Math.cos(ang) * dist;
            double nz = oz + Math.sin(ang) * dist;
            int ny = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(nx), Mth.floor(nz));
            if (this.randomTeleport(nx + 0.5, ny, nz + 0.5, true)) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F, 0.6F);
                break;
            }
        }
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity e) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // intocável de perto: golpe de qualquer ser vivo → pisca pra longe (não morre)
        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            this.blinkAway(attacker);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.COSMIC_DISTANT_WHISPERS.get(), SoundSource.HOSTILE, 1.0F, 0.7F);
            return false;
        }
        // /kill, void, etc (fonte não-viva) → some de verdade (admin consegue limpar)
        return super.hurt(source, amount);
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
        return false;
    }
}
