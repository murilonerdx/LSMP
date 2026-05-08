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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SwordBrumEvents {

    private static final float MIN_HEALTH = 1.0F;

    private static final String BRUM_ATTACK_COUNTER_TAG = "BrumAttackCounter";
    private static final String BRUM_NEXT_SPECIAL_ATTACK_TAG = "BrumNextSpecialAttack";

    private static final int BRUM_MIN_ATTACKS_TO_SPECIAL = 8;
    private static final int BRUM_MAX_ATTACKS_TO_SPECIAL = 37;

    private static final float BRUM_HEALTH_DAMAGE_PERCENT = 0.80F;
    private static final float BRUM_ARMOR_DAMAGE_PERCENT = 0.80F;

    private SwordBrumEvents() {
    }

    /**
     * Regras defensivas da Sword Brum.
     *
     * 1. Se estiver segurando botão direito com a espada:
     *    bloqueia QUALQUER dano.
     *
     * 2. Se estiver apenas com a espada na mão:
     *    toma dano normal, mas não morre.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDamageWithSwordBrum(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack swordStack = getSwordBrumInHands(player);

        if (swordStack.isEmpty()) {
            return;
        }

        float damage = event.getAmount();

        if (damage <= 0.0F) {
            return;
        }

        /*
         * MODO DEFESA ABSOLUTA:
         *
         * Se está segurando botão direito com a Sword Brum,
         * cancela qualquer dano.
         */
        if (isDefendingWithSwordBrum(player)) {
            event.setCanceled(true);
            event.setAmount(0.0F);

            SwordBrumItem.addStoredDamage(swordStack, damage);

            player.displayClientMessage(
                    Component.literal("A Espada Brum bloqueou " + formatDamage(damage) + " de dano.")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                    true
            );

            showBlockingEffect(player);

            return;
        }

        /*
         * MODO IMORTALIDADE PASSIVA:
         *
         * Se não está segurando botão direito,
         * o dano comum passa normalmente.
         *
         * Só cancela se o dano for fatal.
         */
        boolean fatalDamage = damage >= player.getHealth();

        if (!fatalDamage) {
            return;
        }

        event.setCanceled(true);
        event.setAmount(0.0F);

        keepAlive(player);

        SwordBrumItem.addStoredDamage(swordStack, damage);

        player.displayClientMessage(
                Component.literal("A Espada Brum recusou o golpe fatal.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                true
        );

        showDeathDeniedEffect(player);
    }

    /**
     * Trava final contra morte direta.
     *
     * Isso cobre /kill ou mods que tentam matar diretamente.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeathWithSwordBrum(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack swordStack = getSwordBrumInHands(player);

        if (swordStack.isEmpty()) {
            return;
        }

        event.setCanceled(true);

        keepAlive(player);

        player.displayClientMessage(
                Component.literal("A morte foi cancelada pela Espada Brum.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                true
        );

        showDeathDeniedEffect(player);
    }

    /**
     * Segurança extra.
     *
     * Não bloqueia dano comum.
     * Só impede que algum mod deixe o player com vida 0 enquanto segura a espada.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTickWithSwordBrum(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        if (player.level().isClientSide()) {
            return;
        }

        ItemStack swordStack = getSwordBrumInHands(player);

        if (swordStack.isEmpty()) {
            return;
        }

        if (player.getHealth() <= 0.0F) {
            keepAlive(player);
        }
    }

    /**
     * Golpe especial da Sword Brum.
     *
     * A espada sorteia um número entre BRUM_MIN_ATTACKS_TO_SPECIAL
     * e BRUM_MAX_ATTACKS_TO_SPECIAL.
     *
     * Quando o contador chega nesse número, o super dano ativa.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSwordBrumSpecialDamage(LivingHurtEvent event) {
        Entity sourceEntity = event.getSource().getEntity();

        if (!(sourceEntity instanceof Player player)) {
            return;
        }

        ItemStack swordStack = getSwordBrumInHands(player);

        if (swordStack.isEmpty()) {
            return;
        }

        LivingEntity target = event.getEntity();

        if (target == player) {
            return;
        }

        float originalDamage = event.getAmount();

        if (originalDamage <= 0.0F) {
            return;
        }

        int currentCounter = getAttackCounter(swordStack);
        int nextSpecialAttack = getOrCreateNextSpecialAttack(swordStack, player);

        currentCounter++;

        boolean shouldActivateSpecialDamage = currentCounter >= nextSpecialAttack;

        if (!shouldActivateSpecialDamage) {
            setAttackCounter(swordStack, currentCounter);

            int remaining = Math.max(0, nextSpecialAttack - currentCounter);

            player.displayClientMessage(
                    Component.literal("A Espada Brum pulsa... faltam " + remaining + " ataque(s).")
                            .withStyle(ChatFormatting.DARK_PURPLE),
                    true
            );

            return;
        }

        resetSpecialAttackCounter(swordStack, player);

        float finalDamage = calculateSpecialDamage(target, originalDamage);

        event.setAmount(finalDamage);

        SwordBrumItem.addStoredDamage(swordStack, finalDamage);

        showSpecialDamageEffect(player, target);

        player.displayClientMessage(
                Component.literal("Golpe Brum ativado: " + formatDamage(finalDamage) + " de dano.")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                true
        );

        if (target instanceof Player targetPlayer) {
            targetPlayer.displayClientMessage(
                    Component.literal("A Espada Brum rasgou sua essência.")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                    true
            );
        }
    }

    /**
     * Vampirismo.
     *
     * Quando o player causa dano segurando a Sword Brum,
     * ganha absorção baseada no dano final do evento.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerDealDamageWithSwordBrum(LivingHurtEvent event) {
        Entity sourceEntity = event.getSource().getEntity();

        if (!(sourceEntity instanceof Player player)) {
            return;
        }

        ItemStack swordStack = getSwordBrumInHands(player);

        if (swordStack.isEmpty()) {
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
                Component.literal("Sangue absorvido: +" + formatDamage(damageDealt)
                                + " | Corações extras: " + formatHearts(updatedAbsorption))
                        .withStyle(ChatFormatting.DARK_RED),
                true
        );
    }

    private static boolean isDefendingWithSwordBrum(Player player) {
        if (!player.isUsingItem()) {
            return false;
        }

        ItemStack usingStack = player.getUseItem();

        if (usingStack.isEmpty()) {
            return false;
        }

        /*
         * Não compara NBT aqui.
         *
         * A espada pode alterar NBT enquanto bloqueia dano.
         * Se comparar tags, pode falhar no meio do uso.
         */
        return usingStack.getItem() instanceof SwordBrumItem;
    }

    private static float calculateSpecialDamage(LivingEntity target, float originalDamage) {
        float targetMaxHealth = target.getMaxHealth();

        double armorValue = target.getAttributeValue(Attributes.ARMOR);
        double armorToughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        float healthDamage = targetMaxHealth * BRUM_HEALTH_DAMAGE_PERCENT;
        float armorDamage = (float) ((armorValue + armorToughness) * BRUM_ARMOR_DAMAGE_PERCENT);

        float specialDamage = healthDamage + armorDamage;

        return Math.max(originalDamage, specialDamage);
    }

    private static ItemStack getSwordBrumInHands(Player player) {
        ItemStack mainHand = player.getMainHandItem();

        if (mainHand.getItem() instanceof SwordBrumItem) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();

        if (offHand.getItem() instanceof SwordBrumItem) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static void keepAlive(Player player) {
        player.setHealth(MIN_HEALTH);
        player.setSecondsOnFire(0);
        player.fallDistance = 0.0F;

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 3, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 3, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 5, 0, false, false, true));
    }

    private static int getAttackCounter(ItemStack swordStack) {
        return swordStack.getOrCreateTag().getInt(BRUM_ATTACK_COUNTER_TAG);
    }

    private static void setAttackCounter(ItemStack swordStack, int value) {
        swordStack.getOrCreateTag().putInt(BRUM_ATTACK_COUNTER_TAG, Math.max(0, value));
    }

    private static int getOrCreateNextSpecialAttack(ItemStack swordStack, Player player) {
        int nextSpecialAttack = swordStack.getOrCreateTag().getInt(BRUM_NEXT_SPECIAL_ATTACK_TAG);

        if (nextSpecialAttack <= 0) {
            nextSpecialAttack = rollNextSpecialAttack(player);
            swordStack.getOrCreateTag().putInt(BRUM_NEXT_SPECIAL_ATTACK_TAG, nextSpecialAttack);
        }

        return nextSpecialAttack;
    }

    private static void resetSpecialAttackCounter(ItemStack swordStack, Player player) {
        swordStack.getOrCreateTag().putInt(BRUM_ATTACK_COUNTER_TAG, 0);
        swordStack.getOrCreateTag().putInt(BRUM_NEXT_SPECIAL_ATTACK_TAG, rollNextSpecialAttack(player));
    }

    private static int rollNextSpecialAttack(Player player) {
        int min = BRUM_MIN_ATTACKS_TO_SPECIAL;
        int max = BRUM_MAX_ATTACKS_TO_SPECIAL;

        if (max <= min) {
            return min;
        }

        int range = max - min + 1;
        return min + player.getRandom().nextInt(range);
    }

    private static void showBlockingEffect(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.SOUL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                8,
                0.25D,
                0.45D,
                0.25D,
                0.02D
        );

        serverLevel.sendParticles(
                ParticleTypes.DAMAGE_INDICATOR,
                player.getX(),
                player.getY() + 1.2D,
                player.getZ(),
                4,
                0.20D,
                0.30D,
                0.20D,
                0.01D
        );

        serverLevel.playSound(
                null,
                player.blockPosition(),
                SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS,
                0.8F,
                0.55F
        );
    }

    private static void showDeathDeniedEffect(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                18,
                0.35D,
                0.7D,
                0.35D,
                0.04D
        );

        serverLevel.sendParticles(
                ParticleTypes.SOUL,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                8,
                0.25D,
                0.45D,
                0.25D,
                0.02D
        );

        serverLevel.playSound(
                null,
                player.blockPosition(),
                SoundEvents.TOTEM_USE,
                SoundSource.PLAYERS,
                1.0F,
                0.75F
        );

        serverLevel.playSound(
                null,
                player.blockPosition(),
                SoundEvents.WITHER_AMBIENT,
                SoundSource.PLAYERS,
                0.7F,
                0.55F
        );
    }

    private static void showSpecialDamageEffect(Player player, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(),
                target.getY() + 1.0D,
                target.getZ(),
                18,
                0.35D,
                0.65D,
                0.35D,
                0.04D
        );

        serverLevel.sendParticles(
                ParticleTypes.DAMAGE_INDICATOR,
                target.getX(),
                target.getY() + 1.0D,
                target.getZ(),
                10,
                0.25D,
                0.45D,
                0.25D,
                0.02D
        );

        serverLevel.sendParticles(
                ParticleTypes.SOUL,
                player.getX(),
                player.getY() + 1.2D,
                player.getZ(),
                8,
                0.25D,
                0.45D,
                0.25D,
                0.02D
        );

        serverLevel.playSound(
                null,
                target.blockPosition(),
                SoundEvents.WITHER_HURT,
                SoundSource.PLAYERS,
                1.0F,
                0.55F
        );

        serverLevel.playSound(
                null,
                player.blockPosition(),
                SoundEvents.WARDEN_ATTACK_IMPACT,
                SoundSource.PLAYERS,
                0.8F,
                0.75F
        );
    }

    private static String formatDamage(float damage) {
        return String.format("%.1f", damage);
    }

    private static String formatHearts(float absorptionPoints) {
        float hearts = absorptionPoints / 2.0F;
        return String.format("%.1f", hearts);
    }
}