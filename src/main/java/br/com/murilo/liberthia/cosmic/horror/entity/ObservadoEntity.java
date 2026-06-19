package br.com.murilo.liberthia.cosmic.horror.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * r180: <b>O Observado</b> — terror que se alimenta da atenção.
 *
 * <ul>
 *   <li><b>Quanto mais o encaras → maior e mais forte ele fica</b> (growth sobe enquanto
 *       algum player olha direto pra ele; HP/ataque/velocidade/escala crescem).</li>
 *   <li>Quando ninguém olha, ele encolhe/enfraquece lentamente.</li>
 *   <li><b>Como derrotar:</b> lutar SEM olhar direto (golpes "às cegas" dão dano cheio,
 *       e ele não cresce) — OU usar um item de <b>espelho</b> nele (ele se vê e se desfaz).</li>
 * </ul>
 */
public class ObservadoEntity extends Monster {

    private static final EntityDataAccessor<Float> GROWTH =
            SynchedEntityData.defineId(ObservadoEntity.class, EntityDataSerializers.FLOAT);

    private static final UUID HP_UUID  = UUID.fromString("b1a7c0de-0001-4a1a-9f10-0b0b0b0b0001");
    private static final UUID ATK_UUID = UUID.fromString("b1a7c0de-0002-4a1a-9f10-0b0b0b0b0002");
    private static final UUID SPD_UUID = UUID.fromString("b1a7c0de-0003-4a1a-9f10-0b0b0b0b0003");

    public ObservadoEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 1.1D, 32.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GROWTH, 0.0F);
    }

    /** 0..1 — quanto ele já "cresceu" de ser observado. O renderer escala por isso. */
    public float getGrowth() { return this.entityData.get(GROWTH); }
    private void setGrowth(float g) { this.entityData.set(GROWTH, Math.max(0F, Math.min(1F, g))); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        boolean stared = isBeingStaredAt();
        float g = getGrowth();
        if (stared) g += 0.012F;       // ~7s de encarada até o máximo
        else        g -= 0.004F;       // recua devagar quando ninguém olha
        setGrowth(g);

        if (this.tickCount % 20 == 0) applyGrowthAttributes();

        if (stared && level() instanceof ServerLevel sl && this.tickCount % 4 == 0) {
            sl.sendParticles(ParticleTypes.SCULK_SOUL,
                    getX(), getY() + getBbHeight() * 0.6, getZ(), 2, 0.3, 0.4, 0.3, 0.0);
        }
    }

    /** Algum player olhando direto pra mim (cone + linha de visão)? */
    private boolean isBeingStaredAt() {
        for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(40.0),
                pl -> pl.isAlive() && !pl.isSpectator() && !pl.isCreative())) {
            Vec3 toMe = position().add(0, getBbHeight() * 0.5, 0).subtract(p.getEyePosition()).normalize();
            if (p.getLookAngle().dot(toMe) > 0.55 && p.hasLineOfSight(this)) return true;
        }
        return false;
    }

    private void applyGrowthAttributes() {
        float g = getGrowth();
        setMod(Attributes.MAX_HEALTH, HP_UUID, g * 40.0);     // +0..40 HP
        setMod(Attributes.ATTACK_DAMAGE, ATK_UUID, g * 9.0);  // +0..9 dano
        setMod(Attributes.MOVEMENT_SPEED, SPD_UUID, g * 0.10);
    }

    private void setMod(Attribute attr, UUID id, double amount) {
        AttributeInstance inst = getAttribute(attr);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (cur != null) {
            if (cur.getAmount() == amount) return;
            inst.removeModifier(id);
        }
        if (amount != 0.0) {
            inst.addTransientModifier(new AttributeModifier(id, "observado_growth", amount,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float g = getGrowth();
        float dmg = amount * (1.0F - g * 0.75F);   // crescido = blindado
        // Golpe "às cegas" (atacante NÃO encara) → dano cheio + bônus
        if (source.getEntity() instanceof Player p) {
            Vec3 toMe = position().add(0, getBbHeight() * 0.5, 0).subtract(p.getEyePosition()).normalize();
            if (p.getLookAngle().dot(toMe) < 0.4) dmg = amount * 1.5F;
        }
        return super.hurt(source, Math.max(0.5F, dmg));
    }

    /** Usar um ESPELHO nele → ele se vê e se desfaz. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        String id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.getItem()).getPath();
        if (id.contains("mirror") && !level().isClientSide) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.0, getZ(), 40, 0.5, 0.8, 0.5, 0.05);
                sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2F, 0.6F);
            }
            this.hurt(level().damageSources().magic(), 100000.0F); // garante a morte apesar da redução por growth
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§5O Observado encara a si mesmo... e desmorona."), true);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putFloat("Growth", getGrowth());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); setGrowth(tag.getFloat("Growth"));
    }
}
