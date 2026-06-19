package br.com.murilo.liberthia.cosmic.horror.entity;

import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * r180b — <b>O Pastor do Silêncio</b>. Figura alta e lenta que caminha "respeitosa",
 * envolta numa <b>aura de silêncio expansiva</b> (esfera onde todo o som é ENGOLIDO —
 * ver {@code SilenceAuraClient}). Mecânicas:
 * <ul>
 *   <li><b>Aura</b>: dentro dela o som some; animais fogem; partículas drenam pra dentro.</li>
 *   <li><b>Detecção invertida</b>: ele não vê — <b>ouve a AUSÊNCIA de som</b>. Quem faz
 *       barulho (corre, ataca, pula) some pra ele; ficar <b>parado/quieto</b> é um farol.</li>
 *   <li><b>Sino partido</b>: toca SEM som, mas o impacto psíquico é real e
 *       <b>proporcional à proximidade</b> (ignora armadura + dreno de sanidade).</li>
 *   <li><b>A agressão alimenta-o</b>: atacá-lo trinca mais o sino → +aura, +dano,
 *       +resistência (cura-se e fica mais forte).</li>
 *   <li><b>Chamado Cósmico</b>: mobs cósmicos formam o seu rebanho — seguem-no como
 *       sombras, nunca o atacam, e ao tocar o sino entram em <b>fúria silenciosa</b>.</li>
 * </ul>
 */
public class SilenceShepherdEntity extends Monster {

    private static final EntityDataAccessor<Float> AURA =
            SynchedEntityData.defineId(SilenceShepherdEntity.class, EntityDataSerializers.FLOAT);

    /** Barulho suavizado por player (UUID → nível). Quanto MENOR, mais detectável. */
    private static final Map<UUID, Float> NOISE = new ConcurrentHashMap<>();
    /** Acima disto o player é "barulhento demais" e o Pastor perde-o. */
    private static final float LOUD_THRESHOLD = 6.0F;
    private static final float AURA_BASE = 14.0F, AURA_MAX = 26.0F;

    private int tollCooldown = 0;
    private int cracks = 0;

    public SilenceShepherdEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
        this.setSilent(true); // nunca faz som — o silêncio
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AURA, AURA_BASE);
    }

    /** Raio da aura de silêncio (sincronizado p/ o cliente engolir sons). */
    public float getAuraRadius() { return this.entityData.get(AURA); }
    private void recomputeAura() {
        this.entityData.set(AURA, Math.min(AURA_MAX, AURA_BASE + cracks * 1.5F));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)   // lento e solene
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        // alvo é definido por AUDIÇÃO (tickHearing), não por NearestAttackableTargetGoal
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.85D, 40.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;
        if (tollCooldown > 0) tollCooldown--;
        float aura = getAuraRadius();

        // partículas: o silêncio "puxa" o ar pra dentro
        if (this.tickCount % 6 == 0) {
            sl.sendParticles(ParticleTypes.ASH, getX(), getY() + getBbHeight() * 0.5, getZ(),
                    8, aura * 0.18, 0.9, aura * 0.18, 0.0);
        }
        // detecção invertida (ouve a ausência de som)
        if (this.tickCount % 10 == 0) tickHearing(sl);
        // r183: limpa o mapa NOISE de players offline (evita leak do static map)
        if (this.tickCount % 600 == 0 && sl.getServer() != null)
            NOISE.keySet().removeIf(u -> sl.getServer().getPlayerList().getPlayer(u) == null);
        // animais fogem da aura
        if (this.tickCount % 10 == 0) scareAnimals(sl, aura);
        // rebanho cósmico segue como sombras
        if (this.tickCount % 30 == 0) tickFlock(sl, false);

        // toca o sino mudo: dano psíquico proporcional à distância
        if (tollCooldown <= 0) toll(sl, aura);
    }

    // ──────────── audição (alvo = mais silencioso) ────────────
    private void tickHearing(ServerLevel sl) {
        double range = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        Player quietest = null;
        float minNoise = Float.MAX_VALUE;
        for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(range),
                pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative())) {
            float loud = currentLoudness(p);
            float n = NOISE.merge(p.getUUID(), loud, (old, nw) -> old * 0.85F + nw * 0.15F);
            if (n < minNoise) { minNoise = n; quietest = p; }
        }
        if (quietest != null && minNoise < LOUD_THRESHOLD) {
            this.setTarget(quietest); // trava no farol de silêncio
        } else if (this.getTarget() instanceof Player gp
                && NOISE.getOrDefault(gp.getUUID(), 0F) >= LOUD_THRESHOLD) {
            this.setTarget(null);     // alvo ficou barulhento → some pra ele
        }
    }

    private static float currentLoudness(Player p) {
        Vec3 dm = p.getDeltaMovement();
        float loud = (float) Math.sqrt(dm.x * dm.x + dm.z * dm.z) * 40.0F;
        if (p.isSprinting()) loud += 7.0F;
        if (p.swinging) loud += 4.0F;
        if (dm.y > 0.1) loud += 2.0F;
        return loud;
    }

    // ──────────── sino mudo ────────────
    private void toll(ServerLevel sl, float aura) {
        tollCooldown = 10; // r183: evita o scan a 20Hz quando não há player na aura
        boolean tolled = false;
        for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(aura),
                pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative())) {
            double d = p.distanceTo(this);
            if (d > aura) continue;
            float t = (float) (1.0 - Math.min(1.0, d / aura)); // 1 perto, 0 longe
            p.hurt(level().damageSources().magic(), 1.0F + t * 5.0F); // psíquico (ignora armadura)
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
            if (p instanceof ServerPlayer sp) {
                SpiritDimension.addSanity(sp, -(1 + Math.round(t * 4)));
                ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
            }
            p.displayClientMessage(Component.literal(
                    "§8O sino partido toca... e o silêncio rasga-te por dentro."), true);
            tolled = true;
        }
        if (tolled) {
            tollCooldown = 140; // ~7s
            tickFlock(sl, true); // fúria silenciosa do rebanho
            sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + getBbHeight() * 0.75, getZ(),
                    18, 0.4, 0.5, 0.4, 0.01);
        }
    }

    // ──────────── agressão alimenta o Pastor ────────────
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isInvulnerableTo(source)
                && source.getEntity() instanceof LivingEntity attacker) {
            crackBell(attacker);
            float reduction = Math.min(0.6F, cracks * 0.08F); // a agressão endurece-o (até 60% DR)
            amount *= (1.0F - reduction);
        }
        return super.hurt(source, amount);
    }

    /** O sino trinca mais → +aura, +dano, cura. */
    private void crackBell(LivingEntity attacker) {
        if (cracks < 8) cracks++;
        recomputeAura();
        this.heal(4.0F);
        AttributeInstance atk = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(Math.min(12.0, atk.getBaseValue() + 0.8));
        if (attacker instanceof Player) this.setTarget(attacker);
        this.tollCooldown = 0; // retaliação psíquica imediata no próximo tick
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + getBbHeight() * 0.7, getZ(),
                    14, 0.4, 0.5, 0.4, 0.02);
        }
    }

    // ──────────── rebanho cósmico ────────────
    private void tickFlock(ServerLevel sl, boolean enrage) {
        LivingEntity tgt = this.getTarget();
        for (Mob m : sl.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(30.0),
                e -> e != this && isCosmic(e))) {
            if (m.getTarget() == this) m.setTarget(null); // nunca atacam o Pastor
            if (enrage) {
                m.setSilent(true);                         // fúria SILENCIOSA
                if (tgt != null) m.setTarget(tgt);
            } else if (m.getTarget() == null && m.getNavigation().isDone()) {
                Vec3 behind = this.position().subtract(this.getLookAngle().scale(2.5)); // sombras atrás
                m.getNavigation().moveTo(behind.x, behind.y, behind.z, 1.0);
            }
        }
    }

    /** Mob cósmico = está no pacote {@code .cosmic.} (Visitante, Espreitador, Ídolo, ...). */
    private static boolean isCosmic(Entity e) {
        return e.getClass().getName().contains(".cosmic.");
    }

    // ──────────── animais fogem ────────────
    private void scareAnimals(ServerLevel sl, float aura) {
        for (Animal a : sl.getEntitiesOfClass(Animal.class, getBoundingBox().inflate(aura))) {
            if (!a.getNavigation().isDone() && a.getTarget() == null) continue;
            Vec3 away = a.position().subtract(this.position());
            if (away.lengthSqr() < 1.0e-3) continue;
            away = away.normalize();
            a.getNavigation().moveTo(a.getX() + away.x * 8, a.getY(), a.getZ() + away.z * 8, 1.5);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Cracks", cracks);
        tag.putInt("TollCd", tollCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        cracks = tag.getInt("Cracks");
        tollCooldown = tag.getInt("TollCd");
        recomputeAura();
    }

    /** Sem sons de passos/idle — reforça o silêncio. */
    @Override protected void playStepSound(BlockPos pos, BlockState state) {}
    @Override public boolean isSilent() { return true; }
}
