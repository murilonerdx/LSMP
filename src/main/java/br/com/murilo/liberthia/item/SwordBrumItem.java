package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class SwordBrumItem extends SwordItem {

    public static final String TAG_STORED_DAMAGE = "BrumStoredDamage";
    public static final String TAG_CHARGING = "BrumCharging";
    public static final String TAG_CHARGE_START_TICK = "BrumChargeStartTick";
    public static final String TAG_LAST_DEATH_SAVE_TICK = "BrumLastDeathSaveTick";

    private static final long MAX_CHARGE_TIME_TICKS = 20L * 20L;
    private static final int DASH_COOLDOWN_TICKS = 60 * 20;

    private static final float MAX_EXTRA_ABSORPTION = 100.0F; // 50 corações extras

    private static final float MARK_1_THRESHOLD = 100.0F;
    private static final float MARK_2_THRESHOLD = 400.0F;
    private static final float MARK_3_THRESHOLD = 1000.0F;

    private static final double DASH_STRENGTH = 3.0D;
    private static final double DASH_UPWARD_FORCE = 0.18D;

    private static final DustParticleOptions BLOOD_DUST =
            new DustParticleOptions(new Vector3f(0.75F, 0.0F, 0.0F), 1.5F);

    private static final DustParticleOptions DARK_BLOOD_DUST =
            new DustParticleOptions(new Vector3f(0.25F, 0.0F, 0.0F), 1.8F);

    public SwordBrumItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown() || player.isCrouching()) {
            tryBloodDash(level, player, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        player.startUsingItem(hand);

        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean(TAG_CHARGING, true);
            tag.putLong(TAG_CHARGE_START_TICK, level.getGameTime());

            player.displayClientMessage(
                    Component.literal("A Espada Brum começou a absorver dor...")
                            .withStyle(ChatFormatting.DARK_PURPLE),
                    true
            );

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.SOUL_ESCAPE,
                    SoundSource.PLAYERS,
                    0.8F,
                    0.7F
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();

        if (player != null && (player.isShiftKeyDown() || player.isCrouching())) {
            return this.use(context.getLevel(), player, context.getHand()).getResult();
        }

        return super.useOn(context);
    }

    private void tryBloodDash(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            player.displayClientMessage(
                    Component.literal("Dash de sangue em recarga.")
                            .withStyle(ChatFormatting.RED),
                    true
            );

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.ALLAY_DEATH,
                    SoundSource.PLAYERS,
                    0.7F,
                    0.5F
            );

            return;
        }

        stopCharging(stack);
        player.getCooldowns().addCooldown(this, DASH_COOLDOWN_TICKS);

        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z);

        if (direction.lengthSqr() < 0.001D) {
            direction = player.getForward();
            direction = new Vec3(direction.x, 0.0D, direction.z);
        }

        direction = direction.normalize();

        player.push(
                direction.x * DASH_STRENGTH,
                DASH_UPWARD_FORCE,
                direction.z * DASH_STRENGTH
        );

        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        player.displayClientMessage(
                Component.literal("Dash de sangue!")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                true
        );

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                1.0F,
                0.6F
        );

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.WITHER_SHOOT,
                SoundSource.PLAYERS,
                0.6F,
                1.4F
        );

        if (level instanceof ServerLevel serverLevel) {
            spawnBloodDashParticles(serverLevel, player, direction);
            damageEntitiesInDashPath(serverLevel, player, direction);
        }
    }

    private void spawnBloodDashParticles(ServerLevel serverLevel, Player player, Vec3 direction) {
        Vec3 backwards = direction.scale(-1.0D);

        for (int i = 0; i < 8; i++) {
            double distance = i * 0.35D;

            double x = player.getX() + backwards.x * distance;
            double y = player.getY() + 0.7D + (serverLevel.random.nextDouble() - 0.5D) * 0.4D;
            double z = player.getZ() + backwards.z * distance;

            serverLevel.sendParticles(
                    BLOOD_DUST,
                    x,
                    y,
                    z,
                    2,
                    0.22D,
                    0.15D,
                    0.22D,
                    0.01D
            );
        }

        serverLevel.sendParticles(
                ParticleTypes.EXPLOSION,
                player.getX() - direction.x * 0.9D,
                player.getY() + 0.8D,
                player.getZ() - direction.z * 0.9D,
                1,
                0.2D,
                0.2D,
                0.2D,
                0.01D
        );
    }

    private void damageEntitiesInDashPath(ServerLevel serverLevel, Player player, Vec3 direction) {
        AABB area = player.getBoundingBox()
                .expandTowards(direction.scale(5.0D))
                .inflate(1.4D);

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity.isAlive() && entity != player
        );

        for (LivingEntity target : targets) {
            target.hurt(serverLevel.damageSources().magic(), 14.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));

            serverLevel.sendParticles(
                    DARK_BLOOD_DUST,
                    target.getX(),
                    target.getY() + 1.0D,
                    target.getZ(),
                    4,
                    0.25D,
                    0.35D,
                    0.25D,
                    0.01D
            );
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        super.releaseUsing(stack, level, entity, timeLeft);

        if (level.isClientSide) {
            return;
        }

        stopCharging(stack);

        if (entity instanceof Player player) {
            float storedDamage = getStoredDamage(stack);

            player.displayClientMessage(
                    Component.literal("Dano armazenado: " + formatDamage(storedDamage) + " | " + getThirstName(storedDamage))
                            .withStyle(getThirstColor(storedDamage)),
                    true
            );
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide) {
            return;
        }

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        boolean inMainHand = living.getMainHandItem() == stack;
        boolean inOffHand = living.getOffhandItem() == stack;

        if (!inMainHand && !inOffHand && !isSelected) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        RandomSource random = living.getRandom();

        double x = living.getX() + (random.nextDouble() - 0.5D) * 0.8D;
        double y = living.getY() + 1.0D + random.nextDouble() * 0.9D;
        double z = living.getZ() + (random.nextDouble() - 0.5D) * 0.8D;

        boolean charging = isCharging(stack);
        boolean canAbsorb = canStillAbsorb(stack, level);
        float storedDamage = getStoredDamage(stack);
        int thirstMark = getThirstMark(storedDamage);

        if (charging && !canAbsorb) {
            stopCharging(stack);

            if (living instanceof Player player) {
                player.displayClientMessage(
                        Component.literal("A absorção terminou. " + getThirstName(storedDamage) + " | Dano: " + formatDamage(storedDamage))
                                .withStyle(getThirstColor(storedDamage)),
                        true
                );
            }
        }

        if (canAbsorb) {
            serverLevel.sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    x,
                    y,
                    z,
                    1 + Math.min(thirstMark, 2),
                    0.18D,
                    0.25D,
                    0.18D,
                    0.01D
            );

            serverLevel.sendParticles(
                    BLOOD_DUST,
                    x,
                    y + 0.1D,
                    z,
                    1 + Math.min(thirstMark, 2),
                    0.14D,
                    0.18D,
                    0.14D,
                    0.01D
            );
        } else {
            serverLevel.sendParticles(
                    thirstMark >= 3 ? DARK_BLOOD_DUST : BLOOD_DUST,
                    x,
                    y,
                    z,
                    Math.max(1, Math.min(thirstMark, 2)),
                    0.12D,
                    0.18D,
                    0.12D,
                    0.01D
            );
        }

        if (thirstMark >= 1 && living instanceof Player player) {
            applyThirstPassiveEffects(player, thirstMark);
        }

        if (storedDamage > 0.0F && level.getGameTime() % 40L == 0L && living instanceof Player player) {
            player.displayClientMessage(
                    Component.literal(getThirstName(storedDamage) + " | Dano acumulado: " + formatDamage(storedDamage))
                            .withStyle(getThirstColor(storedDamage)),
                    true
            );
        }
    }

    private static void applyThirstPassiveEffects(Player player, int thirstMark) {
        if (thirstMark >= 1) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, true, false, true));
        }

        if (thirstMark >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false, true));
        }

        if (thirstMark >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, true));
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level();

        if (!level.isClientSide) {
            float storedDamage = getStoredDamage(stack);
            int thirstMark = getThirstMark(storedDamage);

            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));

            if (storedDamage > 0.0F) {
                float releasedDamage = calculateReleasedDamage(storedDamage, thirstMark);

                target.hurt(level.damageSources().magic(), releasedDamage);

                if (attacker instanceof Player player) {
                    applyReleaseEffectsToPlayer(player, thirstMark);

                    player.displayClientMessage(
                            Component.literal("A Espada Brum liberou " + formatDamage(releasedDamage) + " de dano. " + getThirstName(storedDamage))
                                    .withStyle(getThirstColor(storedDamage), ChatFormatting.BOLD),
                            true
                    );
                }

                if (level instanceof ServerLevel serverLevel) {
                    applyReleaseAreaEffects(serverLevel, attacker, target, thirstMark, releasedDamage);
                }

                clearStoredDamage(stack);

                level.playSound(
                        null,
                        target.blockPosition(),
                        thirstMark >= 3 ? SoundEvents.WITHER_SPAWN : SoundEvents.WITHER_HURT,
                        SoundSource.PLAYERS,
                        thirstMark >= 3 ? 1.4F : 1.0F,
                        thirstMark >= 3 ? 0.35F : 0.6F
                );
            }

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        thirstMark >= 3 ? DARK_BLOOD_DUST : BLOOD_DUST,
                        target.getX(),
                        target.getY() + 1.0D,
                        target.getZ(),
                        6 + thirstMark * 4,
                        0.3D,
                        0.5D,
                        0.3D,
                        0.02D
                );
            }
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    private static float calculateReleasedDamage(float storedDamage, int thirstMark) {
        if (thirstMark >= 3) {
            return storedDamage * 3.0F + 5000.0F;
        }

        if (thirstMark == 2) {
            return storedDamage * 3.0F;
        }

        if (thirstMark == 1) {
            return storedDamage * 2.0F;
        }

        return storedDamage;
    }

    private static void applyReleaseEffectsToPlayer(Player player, int thirstMark) {
        if (thirstMark >= 1) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 8, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 8, 0));
        }

        if (thirstMark >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 10, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 1));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 15, 0));
        }

        if (thirstMark >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 20, 4));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 20, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 20, 2));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 2));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 20, 4));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 30, 0));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 30, 0));
        }
    }

    private static void applyReleaseAreaEffects(ServerLevel serverLevel, LivingEntity attacker, LivingEntity target, int thirstMark, float releasedDamage) {
        double radius = switch (thirstMark) {
            case 1 -> 3.0D;
            case 2 -> 5.0D;
            case 3 -> 9.0D;
            default -> 2.0D;
        };

        AABB area = target.getBoundingBox().inflate(radius);

        List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity.isAlive() && entity != attacker && entity != target
        );

        float areaDamage = switch (thirstMark) {
            case 1 -> Math.min(40.0F, releasedDamage * 0.20F);
            case 2 -> Math.min(120.0F, releasedDamage * 0.35F);
            case 3 -> 250.0F;
            default -> 8.0F;
        };

        for (LivingEntity entity : nearby) {
            entity.hurt(serverLevel.damageSources().magic(), areaDamage);
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100 + thirstMark * 40, thirstMark));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100 + thirstMark * 40, Math.min(3, thirstMark)));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100 + thirstMark * 40, Math.min(2, thirstMark)));

            if (thirstMark >= 3) {
                entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0));
            }
        }

        serverLevel.sendParticles(
                DARK_BLOOD_DUST,
                target.getX(),
                target.getY() + 1.0D,
                target.getZ(),
                12 + thirstMark * 8,
                radius * 0.20D,
                0.7D,
                radius * 0.20D,
                0.04D
        );

        serverLevel.sendParticles(
                ParticleTypes.EXPLOSION,
                target.getX(),
                target.getY() + 1.0D,
                target.getZ(),
                thirstMark >= 3 ? 3 : 1,
                radius * 0.10D,
                0.3D,
                radius * 0.10D,
                0.01D
        );

        if (thirstMark >= 3) {
            serverLevel.playSound(
                    null,
                    target.blockPosition(),
                    SoundEvents.WARDEN_SONIC_BOOM,
                    SoundSource.PLAYERS,
                    1.6F,
                    0.5F
            );
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        float storedDamage = getStoredDamage(stack);
        int thirstMark = getThirstMark(storedDamage);

        tooltip.add(Component.literal("Uma lâmina instável de Sangue.")
                .withStyle(ChatFormatting.DARK_PURPLE));

        tooltip.add(Component.literal("Botão direito: absorve dano e concede imunidade por até 20s.")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.literal("SHIFT + botão direito: dash de sangue. Cooldown: 1 minuto.")
                .withStyle(ChatFormatting.DARK_RED));

        tooltip.add(Component.literal("Enquanto estiver na mão, a espada recusa sua morte.")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));

        tooltip.add(Component.literal("Causar dano concede até 50 corações extras.")
                .withStyle(ChatFormatting.GOLD));

        tooltip.add(Component.literal("Marca I: 100 dano acumulado = libera 2x dano.")
                .withStyle(ChatFormatting.RED));

        tooltip.add(Component.literal("Marca II: 400 dano acumulado = libera 3x dano.")
                .withStyle(ChatFormatting.DARK_RED));

        tooltip.add(Component.literal("Marca III: 1000 dano acumulado = 5000 dano + massacre.")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));

        if (storedDamage > 0.0F) {
            tooltip.add(Component.literal("Dano armazenado: " + formatDamage(storedDamage))
                    .withStyle(getThirstColor(storedDamage), ChatFormatting.BOLD));

            tooltip.add(Component.literal("Estado: " + getThirstName(storedDamage))
                    .withStyle(getThirstColor(storedDamage), ChatFormatting.BOLD));
        }

        if (thirstMark >= 3) {
            tooltip.add(Component.literal("A lâmina está faminta.")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD, ChatFormatting.ITALIC));
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }

    public static boolean isCharging(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_CHARGING);
    }

    public static boolean canStillAbsorb(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();

        if (tag == null) {
            return false;
        }

        if (!tag.getBoolean(TAG_CHARGING)) {
            return false;
        }

        long startTick = tag.getLong(TAG_CHARGE_START_TICK);
        long elapsed = level.getGameTime() - startTick;

        return elapsed <= MAX_CHARGE_TIME_TICKS;
    }

    public static float getStoredDamage(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag == null) {
            return 0.0F;
        }

        return tag.getFloat(TAG_STORED_DAMAGE);
    }

    public static void addStoredDamage(ItemStack stack, float damage) {
        if (damage <= 0.0F) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();

        float current = tag.getFloat(TAG_STORED_DAMAGE);
        float updated = current + damage;

        tag.putFloat(TAG_STORED_DAMAGE, updated);
    }

    public static void stopCharging(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();

        tag.putBoolean(TAG_CHARGING, false);
        tag.remove(TAG_CHARGE_START_TICK);
    }

    public static void clearStoredDamage(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();

        tag.putFloat(TAG_STORED_DAMAGE, 0.0F);
        tag.putBoolean(TAG_CHARGING, false);
        tag.remove(TAG_CHARGE_START_TICK);
    }

    public static int getThirstMark(float storedDamage) {
        if (storedDamage >= MARK_3_THRESHOLD) {
            return 3;
        }

        if (storedDamage >= MARK_2_THRESHOLD) {
            return 2;
        }

        if (storedDamage >= MARK_1_THRESHOLD) {
            return 1;
        }

        return 0;
    }

    public static String getThirstName(float storedDamage) {
        int mark = getThirstMark(storedDamage);

        return switch (mark) {
            case 1 -> "Marca I - Lâmina Sedenta";
            case 2 -> "Marca II - Lâmina Voraz";
            case 3 -> "Marca III - Lâmina Faminta";
            default -> "Sem Marca";
        };
    }

    public static ChatFormatting getThirstColor(float storedDamage) {
        int mark = getThirstMark(storedDamage);

        return switch (mark) {
            case 1 -> ChatFormatting.RED;
            case 2 -> ChatFormatting.DARK_RED;
            case 3 -> ChatFormatting.DARK_PURPLE;
            default -> ChatFormatting.GRAY;
        };
    }

    public static float getMaxExtraAbsorption() {
        return MAX_EXTRA_ABSORPTION;
    }

    public static boolean canSaveFromDeath(ItemStack stack, Level level) {
        CompoundTag tag = stack.getOrCreateTag();

        long now = level.getGameTime();
        long last = tag.getLong(TAG_LAST_DEATH_SAVE_TICK);

        return last <= 0L || now - last >= 20L;
    }

    public static void markDeathSaveUsed(ItemStack stack, Level level) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(TAG_LAST_DEATH_SAVE_TICK, level.getGameTime());
    }

    private static String formatDamage(float damage) {
        return String.format("%.1f", damage);
    }
}