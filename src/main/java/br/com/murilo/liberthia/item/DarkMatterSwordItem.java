package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class DarkMatterSwordItem extends SwordItem {

    private static final String TAG_KNOCKBACK_ENABLED = "KnockbackEnabled";

    private static final int RIGHT_CLICK_COOLDOWN_TICKS = 20 * 15;
    private static final double RIGHT_CLICK_RADIUS = 7.0D;
    private static final float RIGHT_CLICK_DAMAGE = 8.0F;
    private static final int RIGHT_CLICK_DURABILITY_COST = 8;

    public DarkMatterSwordItem(Properties properties) {
        super(DarkMatterToolMaterial.INSTANCE, 20, -2.4F, properties);
    }

    /**
     * Efeito ao bater em uma entidade.
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);

        if (!result) {
            return false;
        }

        // Cura o portador.
        attacker.heal(4.0F);

        // Aplica infecção e efeitos no alvo.
        target.addEffect(new MobEffectInstance(ModEffects.DARK_INFECTION.get(), 200, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));

        // Aumenta a infecção do player que usa a espada.
        if (attacker instanceof Player player) {
            player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                data.setInfection(data.getInfection() + 1);
                data.setDirty(true);
            });
        }

        // Se knockback estiver ativo, joga para longe.
        // Se estiver desativado, puxa para perto.
        if (isKnockbackEnabled(stack)) {
            applyKnockback(target, attacker, 1.6D, 0.25D);
        } else {
            pullEntityToPosition(target, attacker.position(), 0.45D, 0.12D);
        }

        spawnHitEffects(target, attacker);

        return true;
    }

    /**
     * Efeito ao clicar com botão direito.
     *
     * Shift + botão direito: ativa/desativa knockback.
     * Botão direito normal: usa a habilidade da espada.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        // SHIFT + BOTÃO DIREITO = alterna o modo knockback.
        if (player.isShiftKeyDown()) {
            boolean newValue = !isKnockbackEnabled(stack);
            setKnockbackEnabled(stack, newValue);

            player.displayClientMessage(
                    Component.literal(newValue
                            ? "Knockback da Matéria Escura: ATIVADO"
                            : "Knockback da Matéria Escura: DESATIVADO"
                    ).withStyle(newValue ? ChatFormatting.DARK_PURPLE : ChatFormatting.GRAY),
                    true
            );

            serverLevel.playSound(
                    null,
                    player.blockPosition(),
                    newValue ? SoundEvents.SCULK_SENSOR_HIT : SoundEvents.SCULK_BLOCK_FALL,
                    SoundSource.PLAYERS,
                    0.8F,
                    newValue ? 0.7F : 0.5F
            );

            serverLevel.sendParticles(
                    newValue ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.SMOKE,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    40,
                    0.7D,
                    0.8D,
                    0.7D,
                    0.05D
            );

            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        // Botão direito normal: habilidade em área.
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        player.getCooldowns().addCooldown(this, RIGHT_CLICK_COOLDOWN_TICKS);

        AABB area = player.getBoundingBox().inflate(RIGHT_CLICK_RADIUS);

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != player && entity.isAlive()
        );

        for (LivingEntity entity : targets) {
            // Habilidade normal continua puxando todos para o portador.
            pullEntityToPosition(entity, player.position(), 1.15D, 0.35D);

            entity.hurt(player.damageSources().magic(), RIGHT_CLICK_DAMAGE);
            entity.addEffect(new MobEffectInstance(ModEffects.DARK_INFECTION.get(), 260, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
        }

        player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
            data.setInfection(data.getInfection() + 3);
            data.setDirty(true);
        });

        spawnRightClickEffects(serverLevel, player);

        stack.hurtAndBreak(
                RIGHT_CLICK_DURABILITY_COST,
                player,
                p -> p.broadcastBreakEvent(hand)
        );

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private boolean isKnockbackEnabled(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();

        // Por padrão, deixei ATIVADO.
        // Se quiser começar desligado, troque para false.
        if (!tag.contains(TAG_KNOCKBACK_ENABLED)) {
            tag.putBoolean(TAG_KNOCKBACK_ENABLED, true);
        }

        return tag.getBoolean(TAG_KNOCKBACK_ENABLED);
    }

    private void setKnockbackEnabled(ItemStack stack, boolean enabled) {
        stack.getOrCreateTag().putBoolean(TAG_KNOCKBACK_ENABLED, enabled);
    }

    private void applyKnockback(LivingEntity target, LivingEntity attacker, double strength, double verticalStrength) {
        Vec3 direction = target.position()
                .subtract(attacker.position())
                .normalize();

        target.setDeltaMovement(
                target.getDeltaMovement().add(
                        direction.x * strength,
                        verticalStrength,
                        direction.z * strength
                )
        );

        target.hurtMarked = true;
    }

    private void pullEntityToPosition(LivingEntity entity, Vec3 position, double horizontalStrength, double verticalStrength) {
        Vec3 direction = position
                .subtract(entity.position())
                .normalize();

        entity.setDeltaMovement(
                entity.getDeltaMovement().add(
                        direction.x * horizontalStrength,
                        verticalStrength,
                        direction.z * horizontalStrength
                )
        );

        entity.hurtMarked = true;
    }

    private void spawnHitEffects(LivingEntity target, LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.PORTAL,
                target.getX(),
                target.getY() + 1.0D,
                target.getZ(),
                20,
                0.5D,
                0.7D,
                0.5D,
                0.05D
        );

        serverLevel.sendParticles(
                ParticleTypes.SCULK_SOUL,
                target.getX(),
                target.getY() + 1.0D,
                target.getZ(),
                10,
                0.4D,
                0.5D,
                0.4D,
                0.03D
        );

        serverLevel.playSound(
                null,
                target.blockPosition(),
                SoundEvents.WARDEN_ATTACK_IMPACT,
                SoundSource.PLAYERS,
                0.8F,
                0.6F
        );
    }

    private void spawnRightClickEffects(ServerLevel serverLevel, Player player) {
        serverLevel.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                180,
                3.0D,
                1.5D,
                3.0D,
                0.2D
        );

        serverLevel.sendParticles(
                ParticleTypes.SCULK_SOUL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                90,
                2.2D,
                1.0D,
                2.2D,
                0.08D
        );

        serverLevel.sendParticles(
                ParticleTypes.SMOKE,
                player.getX(),
                player.getY() + 0.8D,
                player.getZ(),
                70,
                2.8D,
                0.8D,
                2.8D,
                0.04D
        );

        serverLevel.playSound(
                null,
                player.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                1.5F,
                0.4F
        );

        serverLevel.playSound(
                null,
                player.blockPosition(),
                SoundEvents.SCULK_SHRIEKER_SHRIEK,
                SoundSource.PLAYERS,
                1.0F,
                0.6F
        );
    }

    /**
     * Tooltip/descrição da espada.
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        boolean knockbackEnabled = isKnockbackEnabled(stack);

        tooltip.add(Component.literal("Uma lâmina instável feita de Matéria Escura.")
                .withStyle(ChatFormatting.DARK_PURPLE));

        tooltip.add(Component.literal("Ela não corta carne. Ela desloca existência.")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());

        tooltip.add(Component.literal("Botão direito: Colapso Gravitacional")
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        tooltip.add(Component.literal("Shift + botão direito: alterna o knockback")
                .withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.literal("Knockback: " + (knockbackEnabled ? "ATIVADO" : "DESATIVADO"))
                .withStyle(knockbackEnabled ? ChatFormatting.GREEN : ChatFormatting.RED));

        tooltip.add(Component.empty());

        tooltip.add(Component.literal("+ Infecta o alvo")
                .withStyle(ChatFormatting.DARK_GREEN));

        tooltip.add(Component.literal("+ Rouba vida")
                .withStyle(ChatFormatting.RED));

        tooltip.add(Component.literal("+ Puxa ou arremessa entidades")
                .withStyle(ChatFormatting.AQUA));
    }
}