package br.com.murilo.liberthia.observation.entity;

import br.com.murilo.liberthia.observation.particle.GlowData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * v0.1.22 r67: <b>EntityObservationProjectile</b> — projétil de spell custom,
 * agora com <b>4-stage emitter pattern</b> idêntico ao AN's EntityProjectileSpell.
 *
 * <h2>Pattern AN (lido direto: EntityProjectileSpell.java + ProjectileTimeline.java)</h2>
 * <pre>
 *  TimelineEntryData trailEffect;      ─→ partículas durante o vôo (interpoladas)
 *  TimelineEntryData onSpawnEffect;    ─→ burst no primeiro frame
 *  TimelineEntryData flairEffect;      ─→ ambient/halo contínuo
 *  TimelineEntryData onResolvingEffect;─→ burst no impacto
 * </pre>
 *
 * <h2>Trail Interpolation (pattern TrailMotion.java)</h2>
 * AN's {@code TrailMotion.tick(prev, cur)} cria N pontos interpolados entre
 * {@code prevPos} e {@code curPos}. Replicado aqui em {@link #emitTrail()} —
 * trail visual contínuo mesmo em alta velocidade.
 *
 * <h2>SynchedEntityData</h2>
 * COLOR + SCALE sync server→client em 1 int + 1 float.
 */
public class EntityObservationProjectile extends Projectile {

    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(
        EntityObservationProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(
        EntityObservationProjectile.class, EntityDataSerializers.FLOAT);

    /** Damage no impacto. Setado pelo caster via setDamage(). */
    private float damage = 4.0F;
    /** Lifetime máximo em ticks (despawn auto). */
    private int maxAge = 100;
    /** Pierce — quantos hits aguenta antes de discard. */
    private int piercing = 0;

    /** <b>previousPosition</b> — pattern AN ParticleEmitter. Atualiza no fim do tick(). */
    private Vec3 prevTrailPos = null;
    /** Flag spawn-once (igual playedSpawnParticle do AN). */
    private boolean spawnEmitted = false;
    /** Quantos pontos interpolar entre prev→cur no trail. Mais = mais denso. */
    private static final int TRAIL_DENSITY = 4;

    public EntityObservationProjectile(EntityType<? extends EntityObservationProjectile> type, Level level) {
        super(type, level);
    }

    public EntityObservationProjectile(Level level, LivingEntity caster, int color, float scale) {
        super(br.com.murilo.liberthia.registry.ModEntities.OBSERVATION_PROJECTILE.get(), level);
        this.setOwner(caster);
        this.setPos(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        this.entityData.set(COLOR, color);
        this.entityData.set(SCALE, scale);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR, 0x9d4dd6);
        this.entityData.define(SCALE, 0.6F);
    }

    public int getColor() { return this.entityData.get(COLOR); }
    public float getScale() { return this.entityData.get(SCALE); }
    public void setColor(int c) { this.entityData.set(COLOR, c); }
    public void setDamage(float d) { this.damage = d; }
    public void setMaxAge(int a) { this.maxAge = a; }
    public void setPiercing(int p) { this.piercing = p; }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > maxAge) {
            this.discard();
            return;
        }

        Vec3 start = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 end = start.add(motion);

        // Block raycast
        BlockHitResult blockHit = this.level().clip(new ClipContext(
            start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }

        // Entity raycast (manual)
        EntityHitResult entityHit = this.getEntityHit(start, end);
        HitResult hitResult = entityHit != null ? entityHit : blockHit;

        if (hitResult.getType() != HitResult.Type.MISS && !this.isRemoved()) {
            this.onHit(hitResult);
        }

        // Apply movement (after hit check pra não passar pelo bloco)
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        // r78: SEM gravidade, drag mínimo — projétil FLAT e RÁPIDO igual AN
        // (antes: drag 0.96 + gravity 0.025 = arco de bola lento)
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.x * 0.99, v.y * 0.99, v.z * 0.99);

        // ====================== 4-STAGE VFX EMITTERS (AN pattern) ======================
        if (this.level() instanceof ServerLevel sl) {
            // 1) ON-SPAWN — burst único no primeiro frame (igual onSpawnEffect AN)
            if (!spawnEmitted) {
                emitSpawnBurst(sl);
                spawnEmitted = true;
                this.prevTrailPos = this.position();
            }

            // 2) TRAIL — interpola N pontos entre prevPos e curPos (igual TrailMotion AN)
            emitTrail(sl);

            // 3) FLAIR — halo ambient (a cada 2 ticks pra não saturar)
            if (this.tickCount % 2 == 0) {
                emitFlair(sl);
            }

            // Update prevPos pro próximo trail tick (igual ParticleEmitter.previousPosition)
            this.prevTrailPos = this.position();
        }
    }

    /**
     * <b>STAGE 1: onSpawnEffect</b> — burst pequeno na origem.
     * Pattern AN's onSpawnEmitter — emite uma vez no frame inicial.
     */
    private void emitSpawnBurst(ServerLevel sl) {
        int color = getColor();
        float scale = getScale();
        for (int i = 0; i < 6; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = Math.random() * 0.3;
            sl.sendParticles(new GlowData(color, scale * 0.7F, 0.9F, 18),
                this.getX() + Math.cos(a) * r,
                this.getY() + (Math.random() - 0.5) * 0.3,
                this.getZ() + Math.sin(a) * r,
                1, 0.02, 0.02, 0.02, 0.01);
        }
    }

    /**
     * <b>STAGE 2: trailEffect</b> — interpola TRAIL_DENSITY pontos
     * entre {@code prevTrailPos} e {@code currentPos}.
     *
     * <p>Pattern AN's {@link com.hollingsworth.arsnouveau.api.particle.configurations.TrailMotion#tick}:
     * <pre>
     *  for (int i = 0; i < density; i++) {
     *      double t = i / (density - 1.0);
     *      double px = prevX + deltaX * t;
     *      spawn(px, py, pz);
     *  }
     * </pre>
     */
    private void emitTrail(ServerLevel sl) {
        Vec3 cur = this.position();
        Vec3 prev = this.prevTrailPos != null ? this.prevTrailPos : cur;
        double dx = cur.x - prev.x;
        double dy = cur.y - prev.y;
        double dz = cur.z - prev.z;

        int color = getColor();
        float scale = getScale();
        GlowData trailData = new GlowData(color, scale * 0.55F, 0.85F, 14);

        for (int i = 0; i < TRAIL_DENSITY; i++) {
            double t = (TRAIL_DENSITY == 1) ? 0.5 : (double) i / (double) (TRAIL_DENSITY - 1);
            double px = prev.x + dx * t;
            double py = prev.y + dy * t;
            double pz = prev.z + dz * t;
            // Pequeno jitter pra orgânico (AN's spawnType.SPHERE)
            double jitter = 0.04;
            sl.sendParticles(trailData,
                px + (Math.random() - 0.5) * jitter,
                py + (Math.random() - 0.5) * jitter,
                pz + (Math.random() - 0.5) * jitter,
                1, 0.005, 0.005, 0.005, 0.0);
        }
    }

    /**
     * <b>STAGE 3: flairEffect</b> — halo ambient ao redor do projétil.
     * Particles maiores, mais lentas, contínuas durante o vôo.
     */
    private void emitFlair(ServerLevel sl) {
        int color = getColor();
        float scale = getScale();
        double a = Math.random() * Math.PI * 2;
        double r = 0.15 + Math.random() * 0.2;
        sl.sendParticles(new GlowData(color, scale * 0.4F, 0.6F, 22),
            this.getX() + Math.cos(a) * r,
            this.getY() + (Math.random() - 0.5) * 0.2,
            this.getZ() + Math.sin(a) * r,
            1, 0.0, 0.01, 0.0, 0.0);
    }

    /**
     * <b>STAGE 4: onResolvingEffect</b> — burst grande no impacto.
     * Pattern AN's resolveEmitter — chamado em {@link #sendResolveParticles}.
     */
    private void emitResolveBurst(ServerLevel sl) {
        int color = getColor();
        float scale = getScale();
        // Burst principal — 22 partículas em esfera
        for (int i = 0; i < 22; i++) {
            double a = Math.random() * Math.PI * 2;
            double phi = (Math.random() - 0.5) * Math.PI;
            double r = 0.3 + Math.random() * 1.0;
            double px = this.getX() + Math.cos(a) * Math.cos(phi) * r;
            double py = this.getY() + Math.sin(phi) * r;
            double pz = this.getZ() + Math.sin(a) * Math.cos(phi) * r;
            sl.sendParticles(new GlowData(color, scale * 1.1F, 1.0F, 24),
                px, py, pz, 1, 0.05, 0.05, 0.05, 0.04);
        }
        // Anel adicional horizontal (pattern AN BurstMotion + Sphere shape)
        int ringCount = 12;
        for (int i = 0; i < ringCount; i++) {
            double a = (double) i / ringCount * Math.PI * 2;
            double rr = 0.8;
            sl.sendParticles(new GlowData(color, scale * 0.9F, 1.0F, 20),
                this.getX() + Math.cos(a) * rr,
                this.getY(),
                this.getZ() + Math.sin(a) * rr,
                1, 0.0, 0.02, 0.0, 0.06);
        }
    }

    private EntityHitResult getEntityHit(Vec3 from, Vec3 to) {
        Entity owner = this.getOwner();
        List<Entity> nearby = this.level().getEntities(this, this.getBoundingBox()
            .expandTowards(this.getDeltaMovement()).inflate(1.0));
        Entity closest = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : nearby) {
            if (e == owner) continue;
            if (!(e instanceof LivingEntity)) continue;
            if (!e.isAlive()) continue;
            var opt = e.getBoundingBox().inflate(0.3).clip(from, to);
            if (opt.isPresent()) {
                double dist = from.distanceToSqr(opt.get());
                if (dist < bestDist) {
                    bestDist = dist;
                    closest = e;
                }
            }
        }
        return closest != null ? new EntityHitResult(closest) : null;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide) return;
        ServerLevel sl = (ServerLevel) this.level();

        if (result instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
            le.hurt(this.damageSources().magic(), damage);
            sendResolveParticles(sl);
            if (piercing-- <= 0) {
                this.discard();
            }
        } else if (result instanceof BlockHitResult) {
            sendResolveParticles(sl);
            this.discard();
        }
    }

    /**
     * Pattern AN's {@code sendResolveParticles()} — chamado uma vez no impacto.
     * Emite STAGE 4 (resolve burst).
     */
    public void sendResolveParticles(ServerLevel sl) {
        emitResolveBurst(sl);
    }

    /** Disable gravity (depth-controlled em tick()). */
    @Override
    public boolean isNoGravity() { return true; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("damage");
        this.maxAge = tag.getInt("maxAge");
        this.piercing = tag.getInt("piercing");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putInt("maxAge", maxAge);
        tag.putInt("piercing", piercing);
    }
}
