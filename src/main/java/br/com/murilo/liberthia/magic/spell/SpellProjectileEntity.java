package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import br.com.murilo.liberthia.particle.engine.ConfigurableParticleOptions;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * v0.1.145 r112: <b>SpellProjectileEntity</b> — projétil REAL de feitiço
 * com trail de partículas customizadas (cor da escola), impacto explosivo,
 * sons em camada e status effects.
 *
 * <p>Substitui o padrão preguiçoso "raycast AOE" da SpellLibrary. Spawna
 * como entidade no mundo, viaja em linha reta com aceleração inicial,
 * deixa trail de {@link ConfigurableParticleOptions} emissivos no nosso
 * engine de partículas próprio.
 *
 * <h2>VFX por tick</h2>
 * <ul>
 *   <li>3 partículas "core" emissivas grandes (size 0.6, lifetime 12t)</li>
 *   <li>2 partículas "spark" menores (size 0.25, lifetime 8t) com drift radial</li>
 *   <li>1 partícula vanilla complementar por escola (FLAME, SNOWFLAKE, etc)</li>
 * </ul>
 *
 * <h2>Impacto</h2>
 * <ul>
 *   <li>30 core particles burst em raio 1.5b</li>
 *   <li>60 spark particles em sphere</li>
 *   <li>Sound em 2 camadas (pitch high + low)</li>
 *   <li>Status effect por escola</li>
 *   <li>Bloco hit: vanilla EXPLOSION particle pequena</li>
 * </ul>
 */
public class SpellProjectileEntity extends AbstractHurtingProjectile {

    // DataAccessor pra sincronizar dados pro client (trail VFX usa essas infos)
    private static final EntityDataAccessor<Integer> DATA_SCHOOL =
            SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.FLOAT);

    private int maxLifeTicks = 80; // 4s default
    private boolean piercing = false;
    private float aoeRadius = 0F; // 0 = single target
    private boolean igniteBlocks = false;

    public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> type, Level level) {
        super(type, level);
    }

    public SpellProjectileEntity(Level level, LivingEntity shooter, Vec3 motion,
                                  SpellSchool school, float damage) {
        super(ModEntities.SPELL_PROJECTILE.get(),
                shooter.getX() + 0.0,
                shooter.getEyeY() - 0.1,
                shooter.getZ() + 0.0,
                motion.x, motion.y, motion.z, level);
        setOwner(shooter);
        setSchool(school);
        setDmg(damage);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SCHOOL, SpellSchool.FIRE.ordinal());
        this.entityData.define(DATA_DAMAGE, 6.0F);
    }

    public SpellSchool getSchool() {
        int i = this.entityData.get(DATA_SCHOOL);
        SpellSchool[] vals = SpellSchool.values();
        return vals[Math.max(0, Math.min(vals.length - 1, i))];
    }

    public void setSchool(SpellSchool s) {
        this.entityData.set(DATA_SCHOOL, s.ordinal());
    }

    public float getDmg() { return this.entityData.get(DATA_DAMAGE); }
    public void setDmg(float dmg) { this.entityData.set(DATA_DAMAGE, dmg); }

    /** Configura comportamento extra. */
    public SpellProjectileEntity withConfig(int life, boolean piercing, float aoe, boolean ignite) {
        this.maxLifeTicks = life;
        this.piercing = piercing;
        this.aoeRadius = aoe;
        this.igniteBlocks = ignite;
        return this;
    }

    @Override
    public boolean isOnFire() { return false; }

    @Override
    protected boolean shouldBurn() { return false; }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > maxLifeTicks) {
            if (!level().isClientSide) discard();
            return;
        }

        // Spawn trail particles SERVER-side via sendParticles
        if (!level().isClientSide && level() instanceof ServerLevel sl) {
            spawnTrail(sl);
            // #76 fix: dispara Spell Sensors próximos (antes triggerPulse nunca
            // era chamado → sensor nunca emitia redstone). Scan barato a cada 2t.
            if (tickCount % 2 == 0) scanForSensors();
        }
    }

    /** #76: varre blocos num raio de 3 e pulsa qualquer Spell Sensor encontrado. */
    private void scanForSensors() {
        net.minecraft.core.BlockPos center = blockPosition();
        final int R = 3;
        for (net.minecraft.core.BlockPos bp : net.minecraft.core.BlockPos.betweenClosed(
                center.offset(-R, -R, -R), center.offset(R, R, R))) {
            if (level().getBlockState(bp).getBlock()
                    instanceof br.com.murilo.liberthia.automation.SpellSensorBlock) {
                br.com.murilo.liberthia.automation.SpellSensorBlock.triggerPulse(level(), bp.immutable());
            }
        }
    }

    private void spawnTrail(ServerLevel sl) {
        SpellSchool school = getSchool();

        // r146: trail denso em vôo (antes só burst no impacto).
        // Calcula direção do voo pra "puxar" trail PARA TRÁS — partículas
        // ficam alinhadas com a velocidade em vez de spread radial uniforme.
        Vec3 motion = getDeltaMovement();
        double speed = motion.length();
        Vec3 reverseUnit = speed > 0.001 ? motion.normalize().scale(-1) : Vec3.ZERO;
        // Trail offset ATRÁS do projétil (4 frames de "rastro")
        double trailDist = 0.35;

        // ─── Camada 1: Core trail (3 partículas grandes, alinhadas) ──────
        // Spawnam ligeiramente atrás do projétil pra dar sensação de motion blur
        br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData core =
                new br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData(
                        school, 1.3F, 18);
        for (int i = 0; i < 3; i++) {
            double offset = trailDist * (i + 1) / 3.0;
            sl.sendParticles(core,
                    getX() + reverseUnit.x * offset,
                    getY() + reverseUnit.y * offset,
                    getZ() + reverseUnit.z * offset,
                    1, 0.04, 0.04, 0.04, 0.003);
        }

        // ─── Camada 2: Spark cone (6 sparks, spread maior atrás) ──────
        br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData spark =
                new br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData(
                        school, 0.6F, 13);
        sl.sendParticles(spark,
                getX() + reverseUnit.x * 0.2,
                getY() + reverseUnit.y * 0.2,
                getZ() + reverseUnit.z * 0.2,
                6, 0.18, 0.18, 0.18, 0.025);

        // ─── Camada 3: ENGINE emissive trail (3 chispas brilhantes) ────
        int hex = school.colorHex();
        float r = ((hex >> 16) & 0xFF) / 255F;
        float g = ((hex >> 8) & 0xFF) / 255F;
        float b = (hex & 0xFF) / 255F;
        ConfigurableParticleOptions glow = new ConfigurableParticleOptions(
                ModParticles.ENGINE_PARTICLE.get(),
                r, g, b, 0.65F,
                0.32F, 0.02F,
                10,
                0.0F, 0.88F,
                0.25F,
                false, true, true);
        sl.sendParticles(glow, getX(), getY(), getZ(), 3, 0.05, 0.05, 0.05, 0.012);

        // ─── Camada 4: Vanilla complementar por escola (a cada 2 ticks) ─
        // Adiciona um sabor diferente sem custo de partícula custom
        if (tickCount % 2 == 0) {
            net.minecraft.core.particles.ParticleOptions vanilla = switch (school) {
                case FIRE -> ParticleTypes.FLAME;
                case ICE -> ParticleTypes.SNOWFLAKE;
                case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
                case BLOOD -> ParticleTypes.DAMAGE_INDICATOR;
                case ELDRITCH -> ParticleTypes.PORTAL;
                case HOLY -> ParticleTypes.END_ROD;
                case NATURE -> ParticleTypes.SPORE_BLOSSOM_AIR;
            };
            sl.sendParticles(vanilla, getX(), getY(), getZ(),
                    1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;
        if (!(result.getEntity() instanceof LivingEntity target)) return;
        // Não atinge o caster
        Entity owner = getOwner();
        if (owner != null && target == owner) return;

        applyHit(target);

        if (!piercing) discardWithBurst();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide) return;
        if (aoeRadius > 0) {
            // Splash damage
            for (LivingEntity le : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(aoeRadius))) {
                if (le == getOwner()) continue;
                applyHit(le);
            }
        }
        if (igniteBlocks && level() instanceof ServerLevel sl) {
            net.minecraft.core.BlockPos bp = result.getBlockPos().relative(result.getDirection());
            if (sl.getBlockState(bp).isAir()) {
                sl.setBlock(bp, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 3);
            }
        }
        discardWithBurst();
    }

    private void applyHit(LivingEntity target) {
        SpellSchool school = getSchool();
        Entity owner = getOwner();
        SchoolDamageSource sds = switch (school) {
            case FIRE -> SchoolDamageSource.fire(80);
            case ICE -> SchoolDamageSource.ice(60);
            case BLOOD -> SchoolDamageSource.blood(0.4F);
            case ELDRITCH -> SchoolDamageSource.eldritch();
            default -> SchoolDamageSource.of(school);
        };
        target.hurt(sds.toVanilla(level(), owner != null ? owner : this), getDmg());

        // Status effect por escola
        switch (school) {
            case FIRE -> target.setSecondsOnFire(5);
            case ICE -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                target.setTicksFrozen(target.getTicksFrozen() + 80);
            }
            case LIGHTNING -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            case BLOOD -> {
                if (owner instanceof LivingEntity le) le.heal(getDmg() * 0.4F);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
            }
            case ELDRITCH -> {
                target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            }
            case HOLY -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            case NATURE -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        }
    }

    private void discardWithBurst() {
        if (!(level() instanceof ServerLevel sl)) return;
        SpellSchool school = getSchool();
        int hex = school.colorHex();
        float r = ((hex >> 16) & 0xFF) / 255F;
        float g = ((hex >> 8) & 0xFF) / 255F;
        float b = (hex & 0xFF) / 255F;

        // r113: Impact burst em 3 camadas pra dar feel de explosão real
        // Camada 1: SpellTrail BIG (núcleo da explosão) — 20 partículas grandes
        br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData bigBurst =
                new br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData(
                        school, 2.0F, 22);
        sl.sendParticles(bigBurst, getX(), getY(), getZ(), 20, 0.4, 0.4, 0.4, 0.2);

        // Camada 2: SpellTrail medium — 40 partículas menores espalhadas
        br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData smallBurst =
                new br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData(
                        school, 0.7F, 16);
        sl.sendParticles(smallBurst, getX(), getY(), getZ(), 45, 0.9, 0.9, 0.9, 0.3);

        // Camada 3: ENGINE_PARTICLE emissive sparks (com gravity pra dar volume vertical)
        ConfigurableParticleOptions sparkBurst = new ConfigurableParticleOptions(
                ModParticles.ENGINE_PARTICLE.get(),
                Math.min(1F, r + 0.2F), Math.min(1F, g + 0.2F), Math.min(1F, b + 0.2F), 0.9F,
                0.35F, 0.02F,
                24,
                0.06F, 0.82F,
                0.9F,
                false, true, true);
        sl.sendParticles(sparkBurst, getX(), getY(), getZ(), 50, 0.7, 0.7, 0.7, 0.35);

        // r113: Screen shake S2C pra todos players próximos (raio 8 blocos = 64 sqr)
        for (var sp : sl.getPlayers(p -> p.distanceToSqr(getX(), getY(), getZ()) < 64)) {
            br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                    new br.com.murilo.liberthia.magic.spell.ScreenShakeS2CPacket(0.6F, 8));
        }

        // r115: Ground decal no ponto de impacto — anel expandindo no chão por 30t
        // Raio escala com damage (mais forte = decal maior)
        double decalRadius = 1.5 + Math.min(2.5, getDmg() * 0.15);
        br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx.spawnGroundDecal(
                sl, new Vec3(getX(), getY(), getZ()), school, decalRadius, 25);

        // r146: som agora via MagicSounds helper (2 camadas)
        MagicSounds.playImpact(sl, new Vec3(getX(), getY(), getZ()), school, this);

        discard();
    }
}
