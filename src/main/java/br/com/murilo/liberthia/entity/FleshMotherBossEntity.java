package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.entity.projectile.HemoBoltEntity;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModEntities;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/**
 * Final boss.
 *
 * Agora a entidade:
 * - tem vida alta;
 * - anda;
 * - persegue jogadores;
 * - ataca corpo a corpo;
 * - segura item na mão;
 * - muda velocidade por fase;
 * - mantém os ataques especiais antigos.
 */
public class FleshMotherBossEntity extends Monster {

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal("§4§lA Mãe da Carne"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS
    );

    private int wormCooldown = 280;
    private int boltCooldown = 300;
    private int pulseCooldown = 400;
    private int sonicCooldown = 100;
    private int poundCooldown = 1900;
    private int teleportCooldown = 1200;
    private int shieldHealTick = 0;


    private int meleeComboHits = 0;
    private int weaknessTicks = 0;
    private int heavyPunchCooldown = 0;

    private static final int MAX_MELEE_COMBO_HITS = 4;
    private static final int WEAKNESS_DURATION_TICKS = 100; // 5 segundos
    private static final int HEAVY_PUNCH_COOLDOWN_TICKS = 40; // 2 segundos

    public FleshMotherBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 250;

        /*
         * Arma na mão.
         * Troque Items.NETHERITE_AXE por um item do seu mod se quiser.
         *
         * Exemplo:
         * this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BLOOD_SCYTHE.get()));
         */
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_AXE));

        /*
         * 0.0F = não dropa a arma.
         * 1.0F = sempre dropa.
         */
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 22.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95D);
    }

    @Override
    protected void registerGoals() {
        /*
         * Permite nadar/subir na água.
         */
        this.goalSelector.addGoal(0, new FloatGoal(this));

        /*
         * Ataque corpo a corpo.
         * 1.15D = velocidade enquanto persegue para bater.
         * true = continua perseguindo mesmo se o alvo sair um pouco da visão.
         */
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true));

        /*
         * Aproxima do alvo se estiver distante.
         */
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 1.0D, 48.0F));

        /*
         * Anda quando não tem alvo.
         */
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.75D));

        /*
         * Olha para jogadores próximos.
         */
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));

        /*
         * Olhar aleatório.
         */
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        /*
         * Revida quem bater nele.
         */
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));

        /*
         * Procura jogadores para atacar.
         */
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (effect.getEffect() == MobEffects.POISON) {
            return false;
        }

        if (effect.getEffect() == MobEffects.WITHER) {
            return false;
        }

        if (effect.getEffect() == MobEffects.MOVEMENT_SLOWDOWN) {
            return false;
        }

        if (effect.getEffect() == ModEffects.BLOOD_INFECTION.get()) {
            return false;
        }

        return super.canBeAffected(effect);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            return false;
        }

        if (source.is(net.minecraft.world.damagesource.DamageTypes.DROWN)) {
            return false;
        }

        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) {
            return false;
        }

        float hpFrac = getHealth() / getMaxHealth();

        /*
         * Antes da fase final, ele reduz parte do dano recebido.
         */
        if (hpFrac > 0.33F) {
            amount *= 0.5F;
        }

        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);

        if (!hit) {
            return false;
        }

        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));

            if (ModEffects.BLOOD_INFECTION.get() != null) {
                living.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 160, 1));
            }

            double dx = living.getX() - this.getX();
            double dz = living.getZ() - this.getZ();
            double dist = Math.max(0.5D, Math.sqrt(dx * dx + dz * dz));

            living.push(dx / dist * 0.7D, 0.35D, dz / dist * 0.7D);
            living.hurtMarked = true;
        }

        this.level().playSound(
                null,
                this.blockPosition(),
                SoundEvents.RAVAGER_ATTACK,
                SoundSource.HOSTILE,
                1.4F,
                0.65F
        );

        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        bossEvent.setProgress(getHealth() / getMaxHealth());

        if (level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                level().addParticle(
                        ParticleTypes.DAMAGE_INDICATOR,
                        getX() + (random.nextDouble() - 0.5D) * 2.5D,
                        getY() + 1.0D + random.nextDouble() * 1.8D,
                        getZ() + (random.nextDouble() - 0.5D) * 2.5D,
                        0.0D,
                        0.05D,
                        0.0D
                );
            }

            return;
        }

        float hpFrac = getHealth() / getMaxHealth();
        boolean phase2 = hpFrac <= 0.66F;
        boolean phase3 = hpFrac <= 0.33F;

        /*
         * Controla o cooldown do socão forte.
         * Enquanto esse cooldown estiver acima de 0,
         * o boss ainda pode bater, mas não joga o player longe.
         */
        if (heavyPunchCooldown > 0) {
            heavyPunchCooldown--;
        }

        /*
         * Controla o ponto fraco.
         * Durante weaknessTicks, ele fica mais lento,
         * não usa socão forte e pode receber mais dano no método hurt().
         */
        if (weaknessTicks > 0) {
            weaknessTicks--;

            if (getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.12D);
            }

            if (level() instanceof ServerLevel serverLevel && tickCount % 10 == 0) {
                serverLevel.sendParticles(
                        ParticleTypes.CRIT,
                        getX(),
                        getY() + 1.5D,
                        getZ(),
                        12,
                        0.8D,
                        0.8D,
                        0.8D,
                        0.05D
                );

                serverLevel.playSound(
                        null,
                        blockPosition(),
                        SoundEvents.RAVAGER_STUNNED,
                        SoundSource.HOSTILE,
                        1.0F,
                        0.8F
                );
            }
        } else {
            /*
             * Só atualiza a velocidade normal de fase quando ele NÃO está cansado.
             * Isso evita sobrescrever a lentidão do ponto fraco.
             */
            updateSpeedByPhase(phase2, phase3);
        }

        Player nearest = findNearestValidPlayer(64.0D);

        if (nearest != null) {
            setTarget(nearest);
        }

        if (--wormCooldown <= 0) {
            wormCooldown = phase3 ? 50 : phase2 ? 100 : 140;

            summonWorm();

            if (phase3) {
                summonWorm();
                summonWorm();
            }
        }

        if (--pulseCooldown <= 0) {
            pulseCooldown = phase3 ? 30 : 70;
            bloodPulse(phase3 ? 9.0D : 7.0D, phase3 ? 8.0F : 6.0F);
        }

        if (phase2 && nearest != null && --boltCooldown <= 0) {
            boltCooldown = phase3 ? 25 : 45;
            fireBolt(nearest);
        }

        if (nearest != null && --sonicCooldown <= 0) {
            sonicCooldown = phase3 ? 80 : phase2 ? 120 : 180;
            sonicBoom(nearest);
        }

        if (phase2 && --poundCooldown <= 0) {
            poundCooldown = phase3 ? 120 : 200;
            groundPound();
        }

        if (phase2 && nearest != null && --teleportCooldown <= 0) {
            teleportCooldown = phase3 ? 140 : 240;
            tryTeleportNearPlayer(nearest);
        }

        /*
         * Cura leve antes da fase final.
         * Mantido igual ao seu código original.
         */
        if (!phase3) {
            shieldHealTick++;

            if (shieldHealTick >= 60) {
                shieldHealTick = 0;
                heal(1.5F);
            }
        }
    }

    private void updateSpeedByPhase(boolean phase2, boolean phase3) {
        if (getAttribute(Attributes.MOVEMENT_SPEED) == null) {
            return;
        }

        if (phase3) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36D);
        } else if (phase2) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.31D);
        } else {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.26D);
        }
    }

    private Player findNearestValidPlayer(double range) {
        AABB box = this.getBoundingBox().inflate(range);

        return level()
                .getEntitiesOfClass(Player.class, box, player ->
                        !player.isCreative()
                                && !player.isSpectator()
                                && player.isAlive()
                                && player.distanceToSqr(this) <= range * range
                )
                .stream()
                .min(Comparator.comparingDouble(player -> player.distanceToSqr(this)))
                .orElse(null);
    }

    private void summonWorm() {
        EntityType<?> type = random.nextInt(3) == 0
                ? ModEntities.GORE_WORM.get()
                : ModEntities.BLOOD_WORM.get();

        Entity worm = type.create(level());

        if (worm == null) {
            return;
        }

        double ox = (random.nextDouble() - 0.5D) * 4.0D;
        double oz = (random.nextDouble() - 0.5D) * 4.0D;

        worm.moveTo(
                getX() + ox,
                getY(),
                getZ() + oz,
                random.nextFloat() * 360.0F,
                0.0F
        );

        level().addFreshEntity(worm);

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    worm.getX(),
                    worm.getY() + 0.5D,
                    worm.getZ(),
                    18,
                    0.4D,
                    0.4D,
                    0.4D,
                    0.1D
            );
        }
    }

    private void bloodPulse(double radius, float damage) {
        AABB box = new AABB(blockPosition()).inflate(radius);

        for (Player player : level().getEntitiesOfClass(Player.class, box)) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }

            player.hurt(damageSources().indirectMagic(this, this), damage);

            double dx = player.getX() - getX();
            double dz = player.getZ() - getZ();
            double dist = Math.max(0.5D, Math.sqrt(dx * dx + dz * dz));

            player.push(dx / dist * 0.6D, 0.3D, dz / dist * 0.6D);
            player.hurtMarked = true;

            if (ModEffects.BLOOD_INFECTION.get() != null) {
                player.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 120, 1));
            }
        }

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    getX(),
                    getY() + 0.5D,
                    getZ(),
                    80,
                    radius / 2.0D,
                    0.4D,
                    radius / 2.0D,
                    0.3D
            );

            serverLevel.playSound(
                    null,
                    blockPosition(),
                    SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.HOSTILE,
                    1.0F,
                    0.6F
            );
        }
    }

    private void fireBolt(Player target) {
        double dx = target.getX() - getX();
        double dy = target.getY(0.5D) - (getY() + 1.5D);
        double dz = target.getZ() - getZ();

        HemoBoltEntity bolt = new HemoBoltEntity(level(), this, dx, dy, dz);
        bolt.setPos(getX(), getY() + 2.0D, getZ());

        level().addFreshEntity(bolt);

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(
                    null,
                    blockPosition(),
                    SoundEvents.EVOKER_CAST_SPELL,
                    SoundSource.HOSTILE,
                    1.2F,
                    0.5F
            );
        }
    }

    private void sonicBoom(Player target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 origin = new Vec3(getX(), getY() + 1.5D, getZ());
        Vec3 direction = target.getEyePosition().subtract(origin).normalize();

        double range = 16.0D;

        for (int i = 1; i <= range; i++) {
            Vec3 particlePos = origin.add(direction.scale(i));

            serverLevel.sendParticles(
                    ParticleTypes.SONIC_BOOM,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }

        serverLevel.playSound(
                null,
                blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE,
                2.0F,
                1.0F
        );

        AABB lineBox = new AABB(origin, origin.add(direction.scale(range))).inflate(2.5D);

        for (Player player : serverLevel.getEntitiesOfClass(Player.class, lineBox)) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }

            Vec3 toPlayer = player.position().add(0.0D, 1.0D, 0.0D).subtract(origin);
            double along = toPlayer.dot(direction);

            if (along < 0.0D || along > range) {
                continue;
            }

            Vec3 perpendicular = toPlayer.subtract(direction.scale(along));

            if (perpendicular.length() > 2.0D) {
                continue;
            }

            player.hurt(damageSources().sonicBoom(this), 14.0F);
            player.push(direction.x * 1.5D, 0.4D, direction.z * 1.5D);
            player.hurtMarked = true;

            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        }
    }

    private void groundPound() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB box = new AABB(blockPosition()).inflate(8.0D);

        for (Player player : serverLevel.getEntitiesOfClass(Player.class, box)) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }

            double distance = player.position().distanceTo(this.position());
            float damage = (float) Math.max(2.0D, 8.0D - distance * 0.5D);

            player.hurt(damageSources().mobAttack(this), damage);

            double dx = player.getX() - getX();
            double dz = player.getZ() - getZ();
            double dist = Math.max(0.5D, Math.sqrt(dx * dx + dz * dz));

            player.push(dx / dist * 0.9D, 1.6D, dz / dist * 0.9D);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;

            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
        }

        for (int i = 0; i < 32; i++) {
            double angle = (i / 32.0D) * Math.PI * 2.0D;
            double radius = 6.0D;

            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION,
                    getX() + Math.cos(angle) * radius,
                    getY() + 0.2D,
                    getZ() + Math.sin(angle) * radius,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }

        serverLevel.sendParticles(
                ParticleTypes.DAMAGE_INDICATOR,
                getX(),
                getY() + 0.4D,
                getZ(),
                120,
                4.0D,
                0.4D,
                4.0D,
                0.3D
        );

        serverLevel.playSound(
                null,
                blockPosition(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE,
                2.5F,
                0.7F
        );

        serverLevel.playSound(
                null,
                blockPosition(),
                SoundEvents.RAVAGER_ROAR,
                SoundSource.HOSTILE,
                2.0F,
                0.6F
        );
    }

    private void tryTeleportNearPlayer(Player target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        double horizontalDistance = Math.hypot(
                target.getX() - getX(),
                target.getZ() - getZ()
        );

        if (horizontalDistance <= 12.0D) {
            return;
        }

        BlockPos best = null;

        for (int i = 0; i < 8; i++) {
            double angle = serverLevel.random.nextDouble() * Math.PI * 2.0D;
            double radius = 5.0D + serverLevel.random.nextDouble() * 3.0D;

            int dx = (int) (Math.cos(angle) * radius);
            int dz = (int) (Math.sin(angle) * radius);

            BlockPos candidate = target.blockPosition().offset(dx, 0, dz);

            for (int dy = -3; dy <= 3; dy++) {
                BlockPos checkedPos = candidate.offset(0, dy, 0);

                boolean hasSpace =
                        serverLevel.getBlockState(checkedPos).isAir()
                                && serverLevel.getBlockState(checkedPos.above()).isAir()
                                && serverLevel.getBlockState(checkedPos.above().above()).isAir()
                                && !serverLevel.getBlockState(checkedPos.below()).isAir();

                if (hasSpace) {
                    best = checkedPos;
                    break;
                }
            }

            if (best != null) {
                break;
            }
        }

        if (best == null) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.PORTAL,
                getX(),
                getY() + 1.5D,
                getZ(),
                40,
                1.0D,
                1.5D,
                1.0D,
                0.5D
        );

        serverLevel.playSound(
                null,
                blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE,
                2.0F,
                0.5F
        );

        teleportTo(
                best.getX() + 0.5D,
                best.getY() + 0.1D,
                best.getZ() + 0.5D
        );

        serverLevel.sendParticles(
                ParticleTypes.PORTAL,
                getX(),
                getY() + 1.5D,
                getZ(),
                40,
                1.0D,
                1.5D,
                1.0D,
                0.5D
        );

        serverLevel.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                getX(),
                getY() + 1.5D,
                getZ(),
                30,
                1.2D,
                1.2D,
                1.2D,
                0.05D
        );
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);

        spawnAtLocation(new ItemStack(ModItems.HEART_OF_THE_MOTHER.get(), 1));
        spawnAtLocation(new ItemStack(ModItems.SANGUINE_CORE.get(), 1));
        spawnAtLocation(new ItemStack(ModItems.SANGUINE_ESSENCE.get(), 4 + random.nextInt(5)));
        spawnAtLocation(new ItemStack(ModItems.TOME_OF_THE_MOTHER.get(), 1));

        if (random.nextFloat() < 0.5F && ModItems.CURSED_IDOL != null && ModItems.CURSED_IDOL.isPresent()) {
            spawnAtLocation(new ItemStack(ModItems.CURSED_IDOL.get(), 1));
        }

        if (random.nextFloat() < 0.35F && ModItems.VEILED_LANTERN != null && ModItems.VEILED_LANTERN.isPresent()) {
            spawnAtLocation(new ItemStack(ModItems.VEILED_LANTERN.get(), 1));
        }

        if (random.nextFloat() < 0.25F && ModItems.PULSING_HEART != null && ModItems.PULSING_HEART.isPresent()) {
            spawnAtLocation(new ItemStack(ModItems.PULSING_HEART.get(), 1));
        }

        if (source.getEntity() instanceof Player player) {
            int orderPieces = 0;

            for (ItemStack stack : player.getArmorSlots()) {
                if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.ArmorItem armorItem) {
                    if (armorItem.getMaterial().getName().toLowerCase().contains("order")) {
                        orderPieces++;
                    }
                }
            }

            if (orderPieces >= 2) {
                spawnAtLocation(new ItemStack(ModItems.DESECRATED_HOLY_RELIC.get(), 1));
            }
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

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }
}