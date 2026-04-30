package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.SwordBrumItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SwordBrumEvents {

    private SwordBrumEvents() {
    }

    /**
     * Defesa com botão direito:
     * Enquanto usa a espada dentro dos 20 segundos,
     * o dano recebido é acumulado na espada e zerado no player.
     */
    @SubscribeEvent
    public static void onPlayerHurtWhileBlocking(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!player.isUsingItem()) {
            return;
        }

        ItemStack usingStack = player.getUseItem();

        if (!(usingStack.getItem() instanceof SwordBrumItem)) {
            return;
        }

        if (!SwordBrumItem.canStillAbsorb(usingStack, player.level())) {
            return;
        }

        float damage = event.getAmount();

        if (damage <= 0.0F) {
            return;
        }

        SwordBrumItem.addStoredDamage(usingStack, damage);

        float total = SwordBrumItem.getStoredDamage(usingStack);
        int mark = SwordBrumItem.getThirstMark(total);

        event.setAmount(0.0F);

        player.displayClientMessage(
                Component.literal("A espada absorveu " + formatDamage(damage) + " de dano. Total: " + formatDamage(total) + " | " + SwordBrumItem.getThirstName(total))
                        .withStyle(SwordBrumItem.getThirstColor(total)),
                true
        );

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SOUL,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    4 + mark * 2,
                    0.25D,
                    0.4D,
                    0.25D,
                    0.02D
            );

            serverLevel.sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    player.getX(),
                    player.getY() + 1.2D,
                    player.getZ(),
                    2 + mark,
                    0.18D,
                    0.25D,
                    0.18D,
                    0.01D
            );

            serverLevel.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS,
                    0.7F,
                    0.6F
            );

            if (mark == 1) {
                serverLevel.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.WITHER_AMBIENT,
                        SoundSource.PLAYERS,
                        0.5F,
                        0.8F
                );
            }

            if (mark == 2) {
                serverLevel.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.WARDEN_HEARTBEAT,
                        SoundSource.PLAYERS,
                        0.6F,
                        1.0F
                );
            }

            if (mark == 3) {
                serverLevel.playSound(
                        null,
                        player.blockPosition(),
                        SoundEvents.WARDEN_ROAR,
                        SoundSource.PLAYERS,
                        1.0F,
                        0.7F
                );
            }
        }
    }

    /**
     * Vampirismo:
     * Quando o player causa dano segurando a Sword Brum,
     * ganha corações extras em absorção.
     *
     * Máximo: 50 corações extras = 100 pontos.
     */
    @SubscribeEvent
    public static void onPlayerDealDamageWithSwordBrum(LivingHurtEvent event) {
        Entity sourceEntity = event.getSource().getEntity();

        if (!(sourceEntity instanceof Player player)) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean hasSwordInMainHand = mainHand.getItem() instanceof SwordBrumItem;
        boolean hasSwordInOffHand = offHand.getItem() instanceof SwordBrumItem;

        if (!hasSwordInMainHand && !hasSwordInOffHand) {
            return;
        }

        float damageDealt = event.getAmount();

        if (damageDealt <= 0.0F) {
            return;
        }

        float currentAbsorption = player.getAbsorptionAmount();
        float maxAbsorption = SwordBrumItem.getMaxExtraAbsorption();

        float updatedAbsorption = Math.min(maxAbsorption, currentAbsorption + damageDealt);

        player.setAbsorptionAmount(updatedAbsorption);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.HEART,
                    player.getX(),
                    player.getY() + 1.2D,
                    player.getZ(),
                    2,
                    0.25D,
                    0.35D,
                    0.25D,
                    0.01D
            );

            serverLevel.sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    event.getEntity().getX(),
                    event.getEntity().getY() + 1.0D,
                    event.getEntity().getZ(),
                    2,
                    0.18D,
                    0.25D,
                    0.18D,
                    0.01D
            );
        }

        player.displayClientMessage(
                Component.literal("Sangue absorvido: +" + formatDamage(damageDealt) + " | Corações extras: " + formatHearts(updatedAbsorption))
                        .withStyle(ChatFormatting.DARK_RED),
                true
        );
    }

    /**
     * Efeito tipo Totem:
     * Se o player morrer segurando a Sword Brum na mão principal ou secundária,
     * a morte é cancelada e ele revive automaticamente.
     */
    @SubscribeEvent
    public static void onPlayerDeathWithSwordBrum(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean hasSwordInMainHand = mainHand.getItem() instanceof SwordBrumItem;
        boolean hasSwordInOffHand = offHand.getItem() instanceof SwordBrumItem;

        if (!hasSwordInMainHand && !hasSwordInOffHand) {
            return;
        }

        ItemStack swordStack = hasSwordInMainHand ? mainHand : offHand;

        if (!SwordBrumItem.canSaveFromDeath(swordStack, player.level())) {
            return;
        }

        event.setCanceled(true);

        SwordBrumItem.markDeathSaveUsed(swordStack, player.level());
        SwordBrumItem.clearStoredDamage(swordStack);

        player.setHealth(1.0F);
        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), 20.0F));

        player.removeAllEffects();

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 2));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 8, 2));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 15, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 6, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 3, 0));

        player.displayClientMessage(
                Component.literal("A Espada Brum recusou sua morte.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                true
        );

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    12,
                    0.35D,
                    0.6D,
                    0.35D,
                    0.04D
            );

            serverLevel.sendParticles(
                    ParticleTypes.SOUL,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    6,
                    0.25D,
                    0.4D,
                    0.25D,
                    0.02D
            );

            serverLevel.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.TOTEM_USE,
                    SoundSource.PLAYERS,
                    1.0F,
                    0.7F
            );

            serverLevel.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.WITHER_AMBIENT,
                    SoundSource.PLAYERS,
                    0.8F,
                    0.5F
            );
        }
    }

    private static String formatDamage(float damage) {
        return String.format("%.1f", damage);
    }

    private static String formatHearts(float absorptionPoints) {
        float hearts = absorptionPoints / 2.0F;
        return String.format("%.1f", hearts);
    }
}