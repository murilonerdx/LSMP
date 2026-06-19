package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.registry.ModItems;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * r192 — <b>A Colmeia Rainha do Abismo</b>: chefe biológico de 3 fases (Mãe das Larvas / Mente
 * Coletiva / Verdadeiro Organismo). Sobrevivência, debuffs, infecção, summons. Autocontido
 * (invoca mobs vanilla como larvas/enxame). Tudo server-side.
 */
public class HiveQueenBossEntity extends Monster {
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§2§lA Colmeia Rainha do Abismo"), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);

    private int phase = 1;
    private int sporeCd = 120, larvaeCd = 200, screamCd = 240, callCd = 300, dominatorCd = 260;
    private int rootsCd = 220, swarmCd = 300, infestCd = 60;
    private boolean apocalypse = false; private int apocalypseTicks = 0;
    // consciência compartilhada (aprende)
    private Item lastWeapon = null; private int sameWeaponHits = 0;

    public HiveQueenBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 320;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 360.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 13.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, 12.0)
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
        if (apocalypse) amount *= 1.25F;          // fase "tudo ou nada": vulnerável
        else if (hp > 0.66F) amount *= 0.65F;
        else if (hp > 0.33F) amount *= 0.55F;
        // Consciência Compartilhada: arma repetida causa menos dano (força variar)
        if (source.getEntity() instanceof LivingEntity le) {
            Item w = le.getMainHandItem().getItem();
            if (w == lastWeapon) { sameWeaponHits = Math.min(8, sameWeaponHits + 1); }
            else { lastWeapon = w; sameWeaponHits = 0; }
            amount *= (1.0F - Math.min(0.6F, sameWeaponHits * 0.1F));
        }
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
                level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, getX()+(random.nextDouble()-0.5)*2.4, getY()+1.0+random.nextDouble()*1.6, getZ()+(random.nextDouble()-0.5)*2.4, 0,0,0);
            return;
        }
        if (!(level() instanceof ServerLevel sl)) return;

        int np = getHealth()/getMaxHealth() > 0.66F ? 1 : getHealth()/getMaxHealth() > 0.33F ? 2 : 3;
        if (np != phase) { phase = np; onPhaseChange(sl); }
        Player t = nearestValid(40.0);
        if (t != null) setTarget(t);

        if (phase == 1) tickP1(sl, t);
        else if (phase == 2) tickP2(sl, t);
        else tickP3(sl, t);
    }

    private void onPhaseChange(ServerLevel sl) {
        if (phase == 2) {
            bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
            announce(sl, "§eA Rainha rompe a carapaça — a Mente Coletiva desperta.");
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2f, 1.2f);
        } else if (phase == 3) {
            bossEvent.setColor(BossEvent.BossBarColor.RED);
            bossEvent.setCreateWorldFog(true);
            announce(sl, "§4A Rainha era só um corpo. O Verdadeiro Organismo emerge.");
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2f, 0.6f);
        }
    }
    private void announce(ServerLevel sl, String m) { sl.players().forEach(p -> p.displayClientMessage(Component.literal(m), false)); }

    // ── FASE 1 ──
    private void tickP1(ServerLevel sl, Player t) {
        if (--sporeCd <= 0) { sporeCloud(sl); sporeCd = 140; }
        if (--larvaeCd <= 0 && t != null) { summonSwarm(sl, t, 2 + random.nextInt(2)); larvaeCd = 220; }
    }
    private void sporeCloud(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY()+0.5, getZ(), 60, 6, 1, 6, 0.02);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY()+0.5, getZ(), 30, 6, 1, 6, 0.0);
        for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(8))) {
            if (p.isCreative()||p.isSpectator()) continue;
            p.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
            p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 160, 1));
        }
    }
    /** larvas/enxame = silverfish vanilla buffados (sem entidade nova). Cap 12 vivos p/ não lagar. */
    private void summonSwarm(ServerLevel sl, Player t, int n) {
        int live = sl.getEntitiesOfClass(net.minecraft.world.entity.monster.Silverfish.class, new AABB(blockPosition()).inflate(32)).size();
        n = Math.max(0, Math.min(n, 12 - live));
        for (int i = 0; i < n; i++) {
            Mob m = EntityType.SILVERFISH.create(sl);
            if (m == null) continue;
            double a = random.nextDouble()*Math.PI*2, r = 2 + random.nextDouble()*4;
            m.moveTo(getX()+Math.cos(a)*r, getY(), getZ()+Math.sin(a)*r, random.nextFloat()*360, 0);
            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 6000, 1));
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            sl.addFreshEntity(m);
            if (m.getTarget() == null && t != null) m.setTarget(t);
        }
        sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY()+0.5, getZ(), 20, 1,0.5,1,0.05);
    }

    // ── FASE 2 ──
    private void tickP2(ServerLevel sl, Player t) {
        tickP1(sl, t);
        if (--screamCd <= 0) { hiveScream(sl); screamCd = 260; }
        if (--callCd <= 0) { queensCall(sl); callCd = 360; }
        if (--dominatorCd <= 0 && t != null) { dominator(sl, t); dominatorCd = 320; }
    }
    private void hiveScream(ServerLevel sl) {
        sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5f, 1.4f);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY()+1, getZ(), 3, 0,0,0,0);
        for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(14))) {
            if (p.isCreative()||p.isSpectator()) continue;
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 1));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 0));
        }
    }
    private void queensCall(ServerLevel sl) {
        announce(sl, "§eO Chamado da Rainha — a colmeia se enfurece.");
        for (Mob m : sl.getEntitiesOfClass(Mob.class, new AABB(blockPosition()).inflate(24))) {
            if (!(m instanceof net.minecraft.world.entity.monster.Silverfish)) continue; // só o enxame da colmeia
            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
        }
    }
    private void dominator(ServerLevel sl, Player t) {
        // "parasita dominador": controle perdido ~ escuridão + lentidão extrema 5s
        t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4));
        if (t instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("§2Um parasita dominador te alcançou!"), true);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, t.getX(), t.getY()+1, t.getZ(), 20, 0.3,0.5,0.3,0.05);
    }

    // ── FASE 3 ──
    private void tickP3(ServerLevel sl, Player t) {
        tickP2(sl, t);
        if (--rootsCd <= 0) { livingRoots(sl); rootsCd = 240; }
        if (--swarmCd <= 0 && t != null) { summonSwarm(sl, t, 6 + random.nextInt(3)); swarmCd = 320; }
        if (--infestCd <= 0) { infestation(sl); infestCd = 60; }
        if (!apocalypse && getHealth()/getMaxHealth() < 0.15F) { apocalypse = true; apocalypseTicks = 600; announce(sl, "§4§l[APOCALIPSE BIOLÓGICO] — a arena vira um organismo vivo!"); }
        if (apocalypse) tickApocalypse(sl);
    }
    private void livingRoots(ServerLevel sl) {
        for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(16))) {
            if (p.isCreative()||p.isSpectator()) continue;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 6));   // preso
            p.addEffect(new MobEffectInstance(MobEffects.JUMP, 80, 128));              // jump negativo = não pula (convenção do mod)
            sl.sendParticles(ParticleTypes.SCULK_CHARGE_POP, p.getX(), p.getY(), p.getZ(), 10, 0.3,0.1,0.3,0);
        }
    }
    /** Infestação Global: efeito cresce com o tempo (lentidão→cegueira→náusea). */
    private void infestation(ServerLevel sl) {
        for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(24))) {
            if (p.isCreative()||p.isSpectator()) continue;
            int lvl = apocalypse ? 3 : 2; // P3 sempre tem náusea; apocalipse adiciona cegueira
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
            if (lvl >= 2) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            if (lvl >= 3) p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        }
    }
    private void tickApocalypse(ServerLevel sl) {
        if (--apocalypseTicks <= 0) return; // para os pulsos após ~30s (latch de vulnerabilidade permanece)
        if (tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY()+1, getZ(), 40, 10, 3, 10, 0.02);
            for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(20))) {
                if (p.isCreative()||p.isSpectator()) continue;
                p.hurt(damageSources().magic(), 2f);
            }
        }
    }

    @Override public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) {
            // "Infecção": dreno de vida + fome 30s
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 600, 0));
            le.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 1));
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0));
        }
        return ok;
    }

    @Override protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        spawnAtLocation(new net.minecraft.world.item.ItemStack(ModItems.HIVE_HEART.get(), 5 + random.nextInt(3)));
    }

    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bossEvent.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bossEvent.removePlayer(p); }
    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putInt("bossPhase", phase); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); phase = t.getInt("bossPhase"); }
}
