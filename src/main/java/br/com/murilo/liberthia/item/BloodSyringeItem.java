package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.BloodKin;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Blood Syringe — inspired by EvilCraft's {@code ItemBloodExtractor}. A single
 * item with two logical states encoded in NBT:
 * <ul>
 *   <li>Empty: attack a living entity with it (left click) OR right-click an entity
 *       to extract 2 HP of blood. The syringe becomes Filled.</li>
 *   <li>Filled: right-click in air to self-inject. Heals 4 HP, applies BLOOD_FRENZY
 *       for 10s, and leaves a 40-tick BLOOD_INFECTION trickle. Syringe returns to Empty.</li>
 * </ul>
 *
 * Blood-kin (cultists, priests, worms, mother) cannot be extracted from — their
 * blood is already tainted. Undead yield nothing but consume the action.
 */
public class BloodSyringeItem extends Item {

    private static final String TAG_FILLED = "liberthia_syringe_filled";
    private static final String TAG_SOURCE_TAINTED = "liberthia_syringe_tainted";

    public BloodSyringeItem(Properties properties) {
        super(properties);
    }

    /** Convenience: is this stack currently holding a sample? */
    public static boolean isFilled(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_FILLED);
    }

    private static boolean isTainted(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(TAG_SOURCE_TAINTED);
    }

    private static void setFilled(ItemStack stack, boolean tainted) {
        stack.getOrCreateTag().putBoolean(TAG_FILLED, true);
        stack.getOrCreateTag().putBoolean(TAG_SOURCE_TAINTED, tainted);
    }

    private static void setEmpty(ItemStack stack) {
        if (stack.hasTag()) {
            stack.getTag().remove(TAG_FILLED);
            stack.getTag().remove(TAG_SOURCE_TAINTED);
        }
    }

    // --- Extract from another entity on right-click ---
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (isFilled(stack)) {
            player.displayClientMessage(
                    Component.literal("A seringa já está cheia.").withStyle(ChatFormatting.YELLOW),
                    true);
            return InteractionResult.FAIL;
        }
        if (BloodKin.is(target)) {
            player.displayClientMessage(
                    Component.literal("Esse sangue já está amaldiçoado — inútil.").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResult.FAIL;
        }
        // Extract: 2 HP from target, mark syringe. Tainted if target has Blood Infection.
        target.hurt(target.damageSources().magic(), 2.0F);
        boolean tainted = target.hasEffect(ModEffects.BLOOD_INFECTION.get());
        setFilled(stack, tainted);

        if (target.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    10, 0.2, 0.2, 0.2, 0.05);
            sl.playSound(null, target.blockPosition(),
                    SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
        return InteractionResult.SUCCESS;
    }

    // --- Self-inject when filled (right-click in air) ---
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);

        if (!isFilled(stack)) {
            player.displayClientMessage(
                    Component.literal("Seringa vazia — clique-direito em um ser vivo para extrair sangue.")
                            .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.pass(stack);
        }

        boolean tainted = isTainted(stack);
        // Cost: the act of injecting yourself deals 1 magic damage to steady the hand.
        player.hurt(player.damageSources().magic(), 1.0F);
        // Always heal 4 HP — that's the "good" part.
        player.heal(4.0F);
        // Bonus effects depend on purity of the source blood.
        if (tainted) {
            // Tainted: light BLOOD_INFECTION tick.
            player.addEffect(new MobEffectInstance(ModEffects.BLOOD_INFECTION.get(), 20 * 30, 0, false, true, true));
        } else {
            // Clean: short BLOOD_FRENZY buff (+strength/speed, small self-damage).
            player.addEffect(new MobEffectInstance(ModEffects.BLOOD_FRENZY.get(), 20 * 10, 0, false, true, true));
        }

        setEmpty(stack);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.5, player.getZ(),
                    4, 0.3, 0.3, 0.3, 0.01);
            sl.playSound(null, player.blockPosition(),
                    SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.8F, 1.4F);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isFilled(stack)) {
            boolean tainted = isTainted(stack);
            tooltip.add(Component.literal("Cheia" + (tainted ? " (contaminada)" : ""))
                    .withStyle(tainted ? ChatFormatting.DARK_RED : ChatFormatting.RED));
            tooltip.add(Component.literal("Clique direito no ar: auto-injeção").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal("Vazia").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Clique direito em ser vivo: extrair sangue").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
