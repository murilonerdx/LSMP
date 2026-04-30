package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.YellowMatterSwordItem;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class YellowMatterInstabilityEvents {

    public static final int MAX_INSTABILITY = 1000;
    public static final int BLOCK_USE_LEVEL = 950;

    private static final int HOLDING_GAIN_PER_SECOND = 8;
    private static final int CHANNELING_GAIN_PER_SECOND = 35;
    private static final int RECOVERY_PER_SECOND = 25;

    private static final int WARNING_LEVEL = 650;
    private static final int DANGER_LEVEL = 850;
    private static final int COLLAPSE_LEVEL = MAX_INSTABILITY;

    private static final Map<UUID, Integer> INSTABILITY_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, ServerBossEvent> BOSS_BAR_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Boolean> COLLAPSED_BY_PLAYER = new HashMap<>();

    private YellowMatterInstabilityEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        UUID playerId = player.getUUID();

        boolean holdingSword = isHoldingYellowMatterSword(player);
        boolean channelingSword = isChannelingYellowMatterSword(player);

        int current = INSTABILITY_BY_PLAYER.getOrDefault(playerId, 0);

        if (player.tickCount % 20 == 0) {
            if (channelingSword) {
                current += CHANNELING_GAIN_PER_SECOND;
            } else if (holdingSword) {
                current += HOLDING_GAIN_PER_SECOND;
            } else {
                current -= RECOVERY_PER_SECOND;
            }

            current = clamp(current, 0, MAX_INSTABILITY);
            INSTABILITY_BY_PLAYER.put(playerId, current);

            if (current < DANGER_LEVEL) {
                COLLAPSED_BY_PLAYER.remove(playerId);
            }
        }

        /*
         * Se não estiver portando a espada:
         * - não mostra bossbar;
         * - não mostra mensagens;
         * - continua recuperando internamente;
         * - remove tudo quando chegar em 0.
         */
        if (!holdingSword) {
            removeBossBar(player);

            if (current <= 0) {
                INSTABILITY_BY_PLAYER.remove(playerId);
                COLLAPSED_BY_PLAYER.remove(playerId);
            }

            return;
        }

        updateBossBar(player, current);
        applyInstabilityEffects(serverLevel, player, current);

        if (current >= COLLAPSE_LEVEL) {
            boolean alreadyCollapsed = COLLAPSED_BY_PLAYER.getOrDefault(playerId, false);

            if (!alreadyCollapsed) {
                triggerEmotionalCollapse(serverLevel, player);
                COLLAPSED_BY_PLAYER.put(playerId, true);
            }

            current = MAX_INSTABILITY;
            INSTABILITY_BY_PLAYER.put(playerId, current);
            updateBossBar(player, current);
        }
    }

    /**
     * Tomar dano durante canalização rompe o fluxo.
     */
    @SubscribeEvent
    public static void onPlayerHurtWhileChanneling(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!isHoldingYellowMatterSword(player)) {
            removeBossBar(player);
            return;
        }

        if (!isChannelingYellowMatterSword(player)) {
            return;
        }

        UUID playerId = player.getUUID();

        ItemStack useItem = player.getUseItem();
        int channelTicks = YellowMatterSwordItem.getChannelTicks(useItem);
        int seconds = Math.max(1, channelTicks / 20);

        int current = INSTABILITY_BY_PLAYER.getOrDefault(playerId, 0);
        current += 180;
        current = clamp(current, 0, MAX_INSTABILITY);

        INSTABILITY_BY_PLAYER.put(playerId, current);

        if (useItem.getItem() instanceof YellowMatterSwordItem) {
            YellowMatterSwordItem.setChannelTicks(useItem, 0);
        }

        player.stopUsingItem();

        float backlashDamage = 3.0F + seconds * 1.25F;
        backlashDamage = Math.min(backlashDamage, 14.0F);

        event.setAmount(event.getAmount() + backlashDamage);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    25,
                    0.55D,
                    0.55D,
                    0.55D,
                    0.06D
            );

            serverLevel.sendParticles(
                    ParticleTypes.FLASH,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );

            serverLevel.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS,
                    0.8F,
                    0.7F
            );
        }
    }

    public static int getInstability(ServerPlayer player) {
        return INSTABILITY_BY_PLAYER.getOrDefault(player.getUUID(), 0);
    }

    private static boolean isHoldingYellowMatterSword(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        return mainHand.is(ModItems.YELLOW_MATTER_SWORD.get())
                || offHand.is(ModItems.YELLOW_MATTER_SWORD.get());
    }

    private static boolean isChannelingYellowMatterSword(ServerPlayer player) {
        if (!player.isUsingItem()) {
            return false;
        }

        ItemStack useItem = player.getUseItem();

        return useItem.getItem() instanceof YellowMatterSwordItem;
    }

    private static void applyInstabilityEffects(ServerLevel level, ServerPlayer player, int instability) {
        if (instability >= WARNING_LEVEL && player.tickCount % 80 == 0) {
            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    8,
                    0.35D,
                    0.45D,
                    0.35D,
                    0.035D
            );

            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS,
                    0.45F,
                    0.8F
            );
        }

        if (instability >= DANGER_LEVEL && player.tickCount % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 0));
        }
    }

    private static void triggerEmotionalCollapse(ServerLevel level, ServerPlayer player) {
        player.stopUsingItem();

        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 220, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 2));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));

        player.hurt(player.damageSources().magic(), 6.0F);

        AABB area = player.getBoundingBox().inflate(5.0D);

        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                entity -> entity != player && entity.isAlive()
        )) {
            entity.hurt(player.damageSources().magic(), 5.0F);
            entity.setSecondsOnFire(3);
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }

        level.sendParticles(
                ParticleTypes.FLASH,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );

        level.sendParticles(
                ParticleTypes.END_ROD,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                45,
                1.8D,
                1.0D,
                1.8D,
                0.05D
        );

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                40,
                1.6D,
                1.0D,
                1.6D,
                0.08D
        );

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS,
                1.1F,
                0.5F
        );

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                0.5F,
                1.4F
        );
    }

    private static void updateBossBar(ServerPlayer player, int instability) {
        UUID playerId = player.getUUID();

        ServerBossEvent bossBar = BOSS_BAR_BY_PLAYER.computeIfAbsent(playerId, id -> {
            ServerBossEvent bar = new ServerBossEvent(
                    Component.translatable("bossbar.liberthia.yellow_matter_instability")
                            .withStyle(ChatFormatting.YELLOW),
                    BossEvent.BossBarColor.YELLOW,
                    BossEvent.BossBarOverlay.PROGRESS
            );

            bar.addPlayer(player);
            return bar;
        });

        float progress = instability / (float) MAX_INSTABILITY;
        bossBar.setProgress(progress);

        if (instability >= DANGER_LEVEL) {
            bossBar.setName(Component.translatable("bossbar.liberthia.yellow_matter_instability.critical")
                    .withStyle(ChatFormatting.RED));
            bossBar.setColor(BossEvent.BossBarColor.RED);
        } else if (instability >= WARNING_LEVEL) {
            bossBar.setName(Component.translatable("bossbar.liberthia.yellow_matter_instability.unstable")
                    .withStyle(ChatFormatting.GOLD));
            bossBar.setColor(BossEvent.BossBarColor.YELLOW);
        } else {
            bossBar.setName(Component.translatable("bossbar.liberthia.yellow_matter_instability")
                    .withStyle(ChatFormatting.YELLOW));
            bossBar.setColor(BossEvent.BossBarColor.YELLOW);
        }
    }

    private static void removeBossBar(ServerPlayer player) {
        UUID playerId = player.getUUID();

        ServerBossEvent bossBar = BOSS_BAR_BY_PLAYER.remove(playerId);

        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}