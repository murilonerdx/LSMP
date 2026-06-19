package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * r191 — <b>O Astrônomo Cego</b>: chefe cósmico de 3 fases (Observador / Cartógrafo do Vazio /
 * Horizonte dos Sonhos Mortos). Posicionamento, gravidade e efeitos cósmicos. Tudo server-side.
 */
public class BlindAstronomerBossEntity extends Monster {
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§9§lO Astrônomo Cego"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);

    private int phase = 1;
    // cooldowns
    private int gazeCd = 100, meteorCd = 200, starCd = 160;     // P1
    private int gravityCd = 220, eclipseCd = 500; // P2
    private int collapseCd = 400, supernovaCd = 500, deadStarCd = 360; // P3
    private float cosmicMadness = 0f;
    private boolean eclipseActive = false; private int eclipseTicks = 0;
    private int supernovaCharging = 0;
    private boolean singularityActive = false;
    private final List<int[]> markedGround = new ArrayList<>();        // x,y,z,landTick
    private final List<Object[]> deadStars = new ArrayList<>();        // {UUID, expireTick}

    public BlindAstronomerBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public boolean canBeAffected(MobEffectInstance e) {
        if (e.getEffect() == MobEffects.POISON || e.getEffect() == MobEffects.WITHER
                || e.getEffect() == MobEffects.MOVEMENT_SLOWDOWN) return false;
        return super.canBeAffected(e);
    }
    @Override public boolean fireImmune() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override protected void pushEntities() {}

    @Override public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.IN_WALL)) return false;
        float hp = getHealth() / getMaxHealth();
        if (eclipseActive) amount *= 0.25F;
        else if (hp > 0.66F) amount *= 0.65F;
        else if (hp > 0.33F) amount *= 0.55F;
        return super.hurt(source, amount);
    }

    private Player nearestValid(double r) {
        Player p = level().getNearestPlayer(this, r);
        return (p == null || p.isCreative() || p.isSpectator() || !p.isAlive()) ? null : p;
    }

    @Override public void aiStep() {
        super.aiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (level().isClientSide) {
            for (int i = 0; i < 3; i++)
                level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble()-0.5)*2, getY()+1.2+random.nextDouble()*1.6, getZ()+(random.nextDouble()-0.5)*2, 0, 0.02, 0);
            return;
        }
        if (!(level() instanceof ServerLevel sl)) return;

        int np = getHealth()/getMaxHealth() > 0.66F ? 1 : getHealth()/getMaxHealth() > 0.33F ? 2 : 3;
        if (np != phase) { phase = np; onPhaseChange(sl); }

        Player t = nearestValid(48.0);
        if (t != null) setTarget(t);

        // orbiting star ambience
        if (tickCount % 6 == 0) {
            double ang = tickCount * 0.15;
            for (int i = 0; i < 3; i++) {
                double a = ang + i * 2.094;
                sl.sendParticles(ParticleTypes.END_ROD, getX()+Math.cos(a)*1.8, getY()+1.4+Math.sin(ang*2)*0.4, getZ()+Math.sin(a)*1.8, 1, 0,0,0,0);
            }
        }

        if (phase >= 2) tickCosmicMadness(sl, t);
        tickShared(sl, t);                 // gaze + meteor + starVolley (1 timer cada)
        if (phase >= 2) tickP2Excl(sl, t); // gravity + eclipse
        if (phase == 3) tickP3Excl(sl, t); // collapse + supernova + deadStar + gravidade reduzida + singularidade

        tickMeteors(sl);
        tickDeadStars(sl);
        tickSupernova(sl, t);
        if (eclipseActive && --eclipseTicks <= 0) { eclipseActive = false; setInvisible(false); }
        if (singularityActive) singularityPull(sl);
    }

    private void onPhaseChange(ServerLevel sl) {
        if (phase == 2) {
            bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
            announce(sl, "§5O Astrônomo levita — o céu se abre num vazio impossível.");
            sl.players().forEach(p -> p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0)));
            sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2f, 0.7f);
        } else if (phase == 3) {
            bossEvent.setColor(BossEvent.BossBarColor.WHITE);
            bossEvent.setCreateWorldFog(true); bossEvent.setDarkenScreen(true);
            announce(sl, "§f§lO corpo se quebra. As estrelas saem de dentro dele.");
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2f, 0.8f);
        }
    }

    private void announce(ServerLevel sl, String msg) { sl.players().forEach(p -> p.displayClientMessage(Component.literal(msg), false)); }

    // ── Habilidades compartilhadas (P1+, mais rápidas em P2+) ──
    private void tickShared(ServerLevel sl, Player t) {
        if (t == null) return;
        if (--gazeCd <= 0) { stellarGaze(sl, t); gazeCd = phase == 1 ? 180 : 120; }
        if (--meteorCd <= 0) { meteorFall(sl, t); meteorCd = phase == 1 ? 280 : 200; }
        if (--starCd <= 0) { starVolley(sl, t); starCd = phase >= 2 ? 220 : 300; }
    }
    private void stellarGaze(ServerLevel sl, Player t) {
        Vec3 eye = getEyePosition(); Vec3 dir = t.getEyePosition().subtract(eye).normalize();
        for (int i = 1; i <= 20; i++) {
            Vec3 p = eye.add(dir.scale(i));
            sl.sendParticles(ParticleTypes.SOUL, p.x, p.y, p.z, 1, 0,0,0,0);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0,0,0,0);
        }
        if (t.getEyePosition().subtract(eye).length() < 24) {
            t.hurt(damageSources().indirectMagic(this, this), 8f);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        }
        sl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.4f, 0.4f);
    }
    private void meteorFall(ServerLevel sl, Player t) {
        int n = 3 + random.nextInt(3);
        for (int i = 0; i < n; i++) {
            int x = (int)(t.getX() + (random.nextDouble()-0.5)*24);
            int z = (int)(t.getZ() + (random.nextDouble()-0.5)*24);
            int y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            markedGround.add(new int[]{x, y, z, tickCount + 60});
            sl.sendParticles(ParticleTypes.FLAME, x+0.5, y+0.2, z+0.5, 20, 0.3, 0.1, 0.3, 0.01);
        }
    }
    private void starVolley(ServerLevel sl, Player t) {
        if (t == null) return;
        // "constelação viva": rajada de projéteis lentos = dano em área marcada ao redor do alvo
        sl.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY()+0.5, t.getZ(), 30, 1.5,1,1.5,0.1);
        for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(t.blockPosition()).inflate(3))) {
            if (p.isCreative()||p.isSpectator()) continue;
            p.hurt(damageSources().indirectMagic(this, this), 4f);
        }
    }
    private void tickMeteors(ServerLevel sl) {
        markedGround.removeIf(m -> {
            if (tickCount < m[3]) {
                if (tickCount % 5 == 0) sl.sendParticles(ParticleTypes.SMOKE, m[0]+0.5, m[1]+1.0, m[2]+0.5, 2, 0.2,0.4,0.2,0.01);
                return false;
            }
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, m[0]+0.5, m[1]+0.5, m[2]+0.5, 1, 0,0,0,0);
            sl.playSound(null, new BlockPos(m[0],m[1],m[2]), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2f, 0.8f);
            for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(new BlockPos(m[0],m[1],m[2])).inflate(3.5))) {
                if (p.isCreative()||p.isSpectator()) continue;
                p.hurt(damageSources().explosion(this, this), 14f);
            }
            return true;
        });
    }

    // ── Exclusivas da Fase 2+ ──
    private void tickP2Excl(ServerLevel sl, Player t) {
        if (--gravityCd <= 0) { gravityHole(sl); gravityCd = 220; }
        if (--eclipseCd <= 0 && !eclipseActive) { eclipse(sl); eclipseCd = 600; }
    }
    private void tickCosmicMadness(ServerLevel sl, Player t) {
        if (t != null) {
            Vec3 look = t.getViewVector(1f).normalize();
            Vec3 to = position().subtract(t.getEyePosition()).normalize();
            if (look.dot(to) > 0.85) cosmicMadness = Math.min(1f, cosmicMadness + 0.003f);
        }
        cosmicMadness = Math.max(0f, cosmicMadness - 0.001f);
        if (cosmicMadness >= 1f && t != null) {
            cosmicMadness = 0f;
            switch (random.nextInt(3)) {
                case 0 -> t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                case 1 -> t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
                default -> { if (t instanceof ServerPlayer) tpPlayerRandom(sl, t); }
            }
            sl.playSound(null, blockPosition(), SoundEvents.AMBIENT_NETHER_WASTES_MOOD.value(), SoundSource.HOSTILE, 1.4f, 0.6f);
        }
    }
    private void tpPlayerRandom(ServerLevel sl, Player t) {
        double a = random.nextDouble()*Math.PI*2, r = 5 + random.nextDouble()*5;
        int tx = (int)(t.getX()+Math.cos(a)*r), tz = (int)(t.getZ()+Math.sin(a)*r);
        int ty = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz);
        t.teleportTo(tx + 0.5, ty, tz + 0.5);
        sl.sendParticles(ParticleTypes.PORTAL, tx + 0.5, ty + 1, tz + 0.5, 30, 0.4,0.8,0.4,0.3);
    }
    private void gravityHole(ServerLevel sl) {
        AABB box = new AABB(blockPosition()).inflate(16);
        for (var e : sl.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box)) {
            if (e == this) continue;
            if (e instanceof Player p && (p.isCreative()||p.isSpectator())) continue;
            Vec3 to = position().subtract(e.position());
            if (to.lengthSqr() < 1e-3) continue;
            e.setDeltaMovement(e.getDeltaMovement().add(to.normalize().scale(0.35)));
            e.hurtMarked = true;
        }
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY()+1, getZ(), 80, 6,6,6,0.1);
        sl.playSound(null, blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1.5f, 0.5f);
    }
    private void eclipse(ServerLevel sl) {
        eclipseActive = true; eclipseTicks = 200; setInvisible(true);
        addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        announce(sl, "§8Um eclipse cobre a arena. Apenas o brilho dele o denuncia.");
        for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(20))) {
            if (p.isCreative() || p.isSpectator()) continue;
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
        }
    }

    // ── Exclusivas da Fase 3 ──
    private void tickP3Excl(ServerLevel sl, Player t) {
        if (--collapseCd <= 0) { realityCollapse(sl); collapseCd = 400; }
        if (--supernovaCd <= 0 && supernovaCharging <= 0 && t != null) { startSupernova(sl); supernovaCd = 700; }
        if (--deadStarCd <= 0 && t != null) { deadStar(sl, t); deadStarCd = 500; }
        if (!singularityActive && getHealth()/getMaxHealth() < 0.10F) {
            singularityActive = true;
            announce(sl, "§5§l[SINGULARIDADE FINAL] — tudo é puxado para o vazio.");
        }
        // gravidade reduzida
        for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(20))) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (p.getDeltaMovement().y < -0.2) p.setDeltaMovement(p.getDeltaMovement().add(0, 0.06, 0));
        }
    }
    private void realityCollapse(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY()+0.2, getZ(), 120, 8,0.5,8,0.05);
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2f, 0.4f);
    }
    private void startSupernova(ServerLevel sl) {
        supernovaCharging = 160;
        announce(sl, "§c§l[SUPERNOVA] — escondam-se atrás de algo!");
    }
    private void tickSupernova(ServerLevel sl, Player t) {
        if (supernovaCharging <= 0) return;
        supernovaCharging--;
        double rad = (160 - supernovaCharging) / 160.0 * 6.0;
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY()+1, getZ(), 6, rad,rad,rad,0);
        if (supernovaCharging == 0) {
            for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(20))) {
                if (p.isCreative()||p.isSpectator()) continue;
                boolean cover = !p.hasLineOfSight(this);
                p.hurt(damageSources().explosion(this, this), cover ? 8f : 35f);
                if (!cover) p.setSecondsOnFire(4);
            }
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY()+1, getZ(), 30, 8,4,8,0);
            sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4f, 0.3f);
        }
    }
    private void deadStar(ServerLevel sl, Player t) {
        deadStars.add(new Object[]{ t.getUUID(), tickCount + 200 });
        if (t instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("§4[ESTRELA MORTA] — afaste-se dos outros!"), true);
    }
    private void tickDeadStars(ServerLevel sl) {
        deadStars.removeIf(ds -> {
            UUID id = (UUID) ds[0]; int exp = (int) ds[1];
            Player p = sl.getPlayerByUUID(id);
            if (p == null) return true;
            sl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, p.getX(), p.getY()+2, p.getZ(), 4, 0.6,0.6,0.6,0.02);
            if (tickCount >= exp) {
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY()+0.5, p.getZ(), 4, 1,1,1,0);
                for (Player q : sl.getEntitiesOfClass(Player.class, new AABB(p.blockPosition()).inflate(5))) {
                    if (q.isCreative()||q.isSpectator()) continue;
                    q.hurt(damageSources().indirectMagic(this, this), 20f);
                }
                return true;
            }
            return false;
        });
    }
    private void singularityPull(ServerLevel sl) {
        AABB box = new AABB(blockPosition()).inflate(24);
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (p.isCreative()||p.isSpectator()) continue;
            Vec3 to = position().subtract(p.position());
            if (to.lengthSqr() < 4) continue;
            p.setDeltaMovement(p.getDeltaMovement().add(to.normalize().scale(0.22)));
            p.hurtMarked = true;
        }
        if (tickCount % 3 == 0) sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY()+1, getZ(), 30, 1,1,1,0.2);
    }

    @Override public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        }
        return ok;
    }

    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new net.minecraft.world.item.ItemStack(ModItems.CONSTELLATION_CORE.get(), 5 + random.nextInt(3)));
    }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }

    @Override public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("bossPhase", phase);
        t.putBoolean("eclipseActive", eclipseActive); t.putInt("eclipseTicks", eclipseTicks);
        t.putBoolean("singularityActive", singularityActive);
    }
    @Override public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        phase = t.getInt("bossPhase");
        eclipseActive = t.getBoolean("eclipseActive"); eclipseTicks = t.getInt("eclipseTicks");
        singularityActive = t.getBoolean("singularityActive");
        if (eclipseActive) setInvisible(true);
    }
}
