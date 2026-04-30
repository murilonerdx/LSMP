package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.event.YellowMatterInstabilityEvents;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class YellowMatterSwordItem extends SwordItem {

    private static final String TAG_CHANNEL_TICKS = "YellowMatterChannelTicks";

    private static final int MAX_USE_DURATION = 72000;

    private static final double BEAM_RANGE = 18.0D;
    private static final double BEAM_RADIUS = 1.15D;

    private static final int BEAM_DAMAGE_INTERVAL_TICKS = 20;
    private static final int DURABILITY_COST_PER_SECOND = 2;

    public YellowMatterSwordItem(Properties properties) {
        super(YellowMatterToolMaterial.INSTANCE, 8, -2.4F, properties);
    }

    /**
     * Passiva enquanto a espada está na mão.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        if (level.isClientSide()) {
            return;
        }

        if (!(entity instanceof Player player)) {
            return;
        }

        boolean holding =
                player.getMainHandItem() == stack ||
                        player.getOffhandItem() == stack;

        if (!holding) {
            return;
        }

        if (player.tickCount % 40 != 0) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 80, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));

        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.WITHER);
    }

    /**
     * Ataque normal da espada.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);

        if (!result) {
            return false;
        }

        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        target.setSecondsOnFire(4);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
        target.hurt(attacker.damageSources().magic(), 3.0F);

        spawnHitParticles(serverLevel, target);

        serverLevel.playSound(
                null,
                target.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.7F,
                1.4F
        );

        return true;
    }

    /**
     * Botão direito: começa canalização do raio.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player instanceof ServerPlayer serverPlayer) {
            int instability = YellowMatterInstabilityEvents.getInstability(serverPlayer);

            if (instability >= YellowMatterInstabilityEvents.BLOCK_USE_LEVEL) {
                return InteractionResultHolder.fail(stack);
            }
        }

        player.startUsingItem(hand);

        if (!level.isClientSide()) {
            setChannelTicks(stack, 0);
        }

        return InteractionResultHolder.consume(stack);
    }

    /**
     * Executa todo tick enquanto o player segura botão direito.
     */
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        int usedTicks = getUseDuration(stack) - remainingUseDuration;
        setChannelTicks(stack, usedTicks);

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = getBeamStartPosition(player, look);
        Vec3 end = start.add(look.scale(BEAM_RANGE));

        spawnBeamParticles(serverLevel, start, look, usedTicks);

        Optional<LivingEntity> targetOptional = findBeamTarget(serverLevel, player, start, end);

        targetOptional.ifPresent(target -> {
            spawnImpactParticles(serverLevel, target);

            if (usedTicks > 0 && usedTicks % BEAM_DAMAGE_INTERVAL_TICKS == 0) {
                int seconds = usedTicks / 20;

                float damage = seconds * 10.0F;
                damage = Math.min(damage, 36.0F);

                target.hurt(player.damageSources().magic(), damage);
                target.setSecondsOnFire(3 + Math.min(seconds, 8));

                int slowAmplifier = Math.min(4, seconds / 3);

                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        80,
                        slowAmplifier
                ));

                target.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING,
                        60,
                        0
                ));

                serverLevel.playSound(
                        null,
                        target.blockPosition(),
                        SoundEvents.BLAZE_SHOOT,
                        SoundSource.PLAYERS,
                        0.6F,
                        1.6F
                );

                stack.hurtAndBreak(
                        DURABILITY_COST_PER_SECOND,
                        player,
                        p -> p.broadcastBreakEvent(player.getUsedItemHand())
                );
            }
        });

        if (usedTicks % 30 == 0) {
            serverLevel.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BEACON_AMBIENT,
                    SoundSource.PLAYERS,
                    0.25F,
                    1.8F
            );
        }
    }

    /**
     * Quando solta o botão direito.
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        setChannelTicks(stack, 0);

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.playSound(
                null,
                livingEntity.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS,
                0.35F,
                1.6F
        );
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return MAX_USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    /**
     * Faz o raio sair da mão/espada, não do meio da câmera.
     */
    private Vec3 getBeamStartPosition(Player player, Vec3 look) {
        Vec3 eye = player.getEyePosition();

        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));

        if (right.lengthSqr() < 0.0001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        return eye
                .add(look.scale(1.25D))
                .add(right.scale(0.45D))
                .add(0.0D, -0.45D, 0.0D);
    }

    private Optional<LivingEntity> findBeamTarget(ServerLevel level, Player player, Vec3 start, Vec3 end) {
        Vec3 beamVector = end.subtract(start);
        double beamLength = beamVector.length();
        Vec3 beamDirection = beamVector.normalize();

        AABB searchArea = player.getBoundingBox()
                .expandTowards(beamDirection.scale(BEAM_RANGE))
                .inflate(BEAM_RADIUS);

        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator()
        );

        return entities.stream()
                .filter(entity -> isEntityInsideBeam(entity, start, beamDirection, beamLength))
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
    }

    private boolean isEntityInsideBeam(LivingEntity entity, Vec3 start, Vec3 direction, double maxDistance) {
        Vec3 entityCenter = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        Vec3 toEntity = entityCenter.subtract(start);

        double projectionLength = toEntity.dot(direction);

        if (projectionLength < 0.0D || projectionLength > maxDistance) {
            return false;
        }

        Vec3 closestPoint = start.add(direction.scale(projectionLength));
        double distanceToBeam = entityCenter.distanceTo(closestPoint);

        return distanceToBeam <= BEAM_RADIUS;
    }

    /**
     * Partículas do raio, versão reduzida para não poluir a visão.
     */
    private void spawnBeamParticles(ServerLevel level, Vec3 start, Vec3 direction, int usedTicks) {
        int particleSteps = 9;

        for (int i = 2; i < particleSteps; i++) {
            double distance = (BEAM_RANGE / particleSteps) * i;
            Vec3 pos = start.add(direction.scale(distance));

            double wave = Math.sin((usedTicks + i) * 0.35D) * 0.05D;

            level.sendParticles(
                    ParticleTypes.END_ROD,
                    pos.x,
                    pos.y + wave,
                    pos.z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );

            if (usedTicks % 4 == 0 && i % 3 == 0) {
                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        pos.x,
                        pos.y,
                        pos.z,
                        1,
                        0.005D,
                        0.005D,
                        0.005D,
                        0.0D
                );
            }
        }
    }

    /**
     * Partículas no alvo, também reduzidas.
     */
    private void spawnImpactParticles(ServerLevel level, LivingEntity target) {
        level.sendParticles(
                ParticleTypes.END_ROD,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(),
                5,
                0.25D,
                0.3D,
                0.25D,
                0.015D
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(),
                3,
                0.2D,
                0.25D,
                0.2D,
                0.015D
        );

        level.sendParticles(
                ParticleTypes.FLAME,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.4D,
                target.getZ(),
                2,
                0.15D,
                0.2D,
                0.15D,
                0.01D
        );
    }

    private void spawnHitParticles(ServerLevel level, LivingEntity target) {
        level.sendParticles(
                ParticleTypes.END_ROD,
                target.getX(),
                target.getY() + 1.0D,
                target.getZ(),
                16,
                0.4D,
                0.45D,
                0.4D,
                0.025D
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                target.getX(),
                target.getY() + 1.0D,
                target.getZ(),
                7,
                0.25D,
                0.3D,
                0.25D,
                0.015D
        );
    }

    public static int getChannelTicks(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt(TAG_CHANNEL_TICKS);
    }

    public static void setChannelTicks(ItemStack stack, int ticks) {
        stack.getOrCreateTag().putInt(TAG_CHANNEL_TICKS, Math.max(0, ticks));
    }

    public static boolean isChanneling(ItemStack stack) {
        return stack.getItem() instanceof YellowMatterSwordItem && getChannelTicks(stack) > 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.liberthia.yellow_matter_sword.title")
                .withStyle(ChatFormatting.YELLOW));

        tooltip.add(Component.translatable("tooltip.liberthia.yellow_matter_sword.description")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());

        tooltip.add(Component.translatable("tooltip.liberthia.yellow_matter_sword.right_click")
                .withStyle(ChatFormatting.GOLD));

        tooltip.add(Component.translatable("tooltip.liberthia.yellow_matter_sword.damage_scaling")
                .withStyle(ChatFormatting.YELLOW));

        tooltip.add(Component.translatable("tooltip.liberthia.yellow_matter_sword.interrupt")
                .withStyle(ChatFormatting.RED));

        tooltip.add(Component.translatable("tooltip.liberthia.yellow_matter_sword.instability")
                .withStyle(ChatFormatting.RED));
    }
}