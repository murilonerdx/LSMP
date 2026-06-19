package br.com.murilo.liberthia.cosmic.observatory;

import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * v0.1.22 r48: <b>Reflection Entity</b> — clone "vivo" do player target,
 * spawned pelo Reflection Seed. Diferente do ClonePlayerEntity (no-AI worker),
 * este tem AI natural: anda, olha, pode mimicar atividades.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>Random walk em torno do spawn (raio 8b)</li>
 *   <li>Look at nearest player (até 32b)</li>
 *   <li><b>Despawn invisible</b> quando observed (player olha direto, dot &gt; 0.85)</li>
 *   <li>Lifetime cap: 5 minutos — depois despawn silencioso</li>
 * </ul>
 *
 * <h2>Rendering</h2>
 * Reusa o renderer do {@link br.com.murilo.liberthia.entity.ClonePlayerEntity}
 * via {@code ownerUuid} — pega skin do player real.
 */
public class ReflectionEntity extends PathfinderMob {

    /**
     * r49: Copia ESTADO completo do player target: armadura nos 4 slots,
     * main hand, off hand. Chamado no spawn pelo Reflection Seed / MimicHeart.
     */
    public void copyEquipmentFrom(net.minecraft.server.level.ServerPlayer target) {
        // Copia armor de TODOS os slots
        for (net.minecraft.world.entity.EquipmentSlot slot :
                net.minecraft.world.entity.EquipmentSlot.values()) {
            net.minecraft.world.item.ItemStack stack = target.getItemBySlot(slot).copy();
            this.setItemSlot(slot, stack);
            // Drop chance = 0 (não dropa equipment ao morrer/vanish)
            this.setDropChance(slot, 0F);
        }
        // Custom name = nome do player (aparece como nametag)
        this.setCustomName(net.minecraft.network.chat.Component.literal(
                target.getName().getString()));
        this.setCustomNameVisible(true);
    }


    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(ReflectionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(ReflectionEntity.class, EntityDataSerializers.STRING);

    /** Tick em que foi spawned (server-side, used pra lifetime). */
    public long spawnedTick = 0;

    /** Max lifetime — 5 minutos = 6000 ticks. */
    public static final int MAX_LIFETIME = 6000;

    /** Pra quem o clone é "visível" — só esse player vê. null = todos. */
    public java.util.UUID targetViewerId = null;

    public ReflectionEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    /** r179: cópias do Mirror Pulse são HOSTIS (atacam o player) e NÃO somem ao serem observadas. */
    private boolean hostile = false;

    public void setHostile(boolean h) {
        this.hostile = h;
        if (h) {
            this.setInvulnerable(false);   // pode ser morta na luta
            this.setPersistenceRequired();
        }
    }
    public boolean isHostile() { return hostile; }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        // r48: AI natural — anda, olha, fica idle
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // r179: ataque (só ativa quando hostile, via predicate no target goal)
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false, (living) -> this.hostile));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(OWNER_NAME, "");
    }

    public void setOwnerUuid(UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public UUID getOwnerUuid() {
        return this.entityData.get(OWNER_UUID).orElse(this.getUUID());
    }

    public void setOwnerName(String name) {
        this.entityData.set(OWNER_NAME, name);
    }

    public String getOwnerName() {
        return this.entityData.get(OWNER_NAME);
    }

    /** Despawn silencioso, sem item drop. */
    public void vanish() {
        if (this.level() instanceof ServerLevel sl) {
            // Quick puff of smoke pra "desaparecer"
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 1, this.getZ(),
                    10, 0.3, 0.5, 0.3, 0.02);
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        // r49: short-lifetime override (MimicHeart usa 10s)
        long mimicExpire = this.getPersistentData().getLong("liberthia.mimic_expire");
        if (mimicExpire > 0 && this.level().getGameTime() >= mimicExpire) {
            vanish();
            return;
        }

        // Lifetime check (default 5 min do Reflection Seed)
        if (spawnedTick > 0 && this.level().getGameTime() - spawnedTick > MAX_LIFETIME) {
            vanish();
            return;
        }

        // Check if observed by anyone — fade if so.
        // r179: cópias HOSTIS (Mirror Pulse) NÃO somem ao serem observadas — elas atacam.
        if (!hostile && this.tickCount % 10 == 0 && checkObserved()) {
            vanish();
        }
    }

    /**
     * Retorna true se algum player está olhando direto pro clone
     * (dot > 0.85 do look angle + within 24 blocos + line of sight).
     */
    private boolean checkObserved() {
        if (!(this.level() instanceof ServerLevel sl)) return false;
        // Apenas pode ser observado pelo targetViewer (se definido)
        // ou por qualquer player se aberto
        for (ServerPlayer sp : sl.getPlayers(p -> p.distanceToSqr(this) < 24 * 24)) {
            if (targetViewerId != null && !sp.getUUID().equals(targetViewerId)) continue;
            Vec3 toMe = this.position().add(0, this.getBbHeight() * 0.5, 0)
                    .subtract(sp.getEyePosition()).normalize();
            double dot = sp.getLookAngle().dot(toMe);
            if (dot > 0.85 && this.hasLineOfSight(sp)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        this.entityData.get(OWNER_UUID).ifPresent(u -> tag.putUUID("OwnerUuid", u));
        tag.putString("OwnerName", getOwnerName());
        tag.putLong("SpawnedTick", spawnedTick);
        tag.putBoolean("Hostile", hostile);
        if (targetViewerId != null) tag.putUUID("TargetViewer", targetViewerId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("OwnerUuid")) setOwnerUuid(tag.getUUID("OwnerUuid"));
        if (tag.contains("OwnerName")) setOwnerName(tag.getString("OwnerName"));
        spawnedTick = tag.getLong("SpawnedTick");
        if (tag.contains("Hostile")) setHostile(tag.getBoolean("Hostile"));
        if (tag.hasUUID("TargetViewer")) targetViewerId = tag.getUUID("TargetViewer");
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void pushEntities() {}

    /** Damage-immune via setInvulnerable, mas pra safety. */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // r179: cópia HOSTIL toma dano normal (pode ser morta na luta), não some.
        if (hostile) {
            return super.hurt(source, amount);
        }
        // Quando atacado, vanish dramático
        if (!this.level().isClientSide) {
            vanish();
            // Bonus: trigger hallucination no attacker
            if (source.getEntity() instanceof ServerPlayer sp) {
                br.com.murilo.liberthia.cosmic.hallucination.HallucinationManager.force(
                        sp, br.com.murilo.liberthia.cosmic.hallucination.HallucinationType.SCREEN_GLITCH_BURST,
                        1.0F, 30, "");
                sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§8§o*sua arma passa direto*"), true);
            }
        }
        return false;
    }
}
