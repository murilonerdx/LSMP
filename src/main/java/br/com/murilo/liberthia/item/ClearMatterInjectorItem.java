package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.InfectionLogic;
import br.com.murilo.liberthia.matter.MatterProfile;
import br.com.murilo.liberthia.matter.MatterProfileEvents;
import br.com.murilo.liberthia.matter.MatterProfileProvider;
import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Clear Matter Injector — seringa de essência clara.
 *
 * <h2>Uso (v0.1.22 r12 — reescrita)</h2>
 * <ul>
 *   <li><b>Right-click no AR</b> (com ou sem shift): aplica em si mesmo.
 *       Antes era no-op sem shift; user reportou "clico com direito e nada
 *       acontece". Agora é o caminho default.</li>
 *   <li><b>Right-click em outro player/mob</b>: aplica no alvo
 *       ({@link #interactLivingEntity}).</li>
 * </ul>
 *
 * <h2>Efeito da cura</h2>
 * <ul>
 *   <li><b>+50 White Matter</b> (50% do cap=100) — user pediu: "é pra curar
 *       50% da materia clara".</li>
 *   <li><b>-50 Dark Matter</b> (purifica matéria escura proporcionalmente).</li>
 *   <li>Reduz Infection em 30, remove mutações e efeitos negativos.</li>
 *   <li>Aplica Clear Shield (immunity) por 3 minutos.</li>
 *   <li>Cooldown de 5s.</li>
 * </ul>
 */
public class ClearMatterInjectorItem extends Item {

    /** 50% do cap (MAX=100). User: "curar 50% da materia clara". */
    private static final float WHITE_HEAL = 50f;
    private static final float DARK_REDUCE = 50f;
    private static final int COOLDOWN_TICKS = 100; // 5s

    public ClearMatterInjectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        // v0.1.22 r12: right-click no AR aplica em SI MESMO (com ou sem shift).
        // Antes exigia shift, mas user reportou que nada acontecia. Agora é
        // direto. Right-click em OUTRO player ainda passa pelo
        // interactLivingEntity (que dispara primeiro pela ordem do vanilla).
        if (level.isClientSide) {
            // Client devolve SUCCESS pra animação da mão (sem fazer lógica).
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            boolean success = applyCure(serverPlayer, serverPlayer);
            if (success) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
                serverPlayer.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§b✦ Essência clara aplicada em você"),
                        true);
                return InteractionResultHolder.success(stack);
            } else {
                serverPlayer.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§7Nada para curar"),
                        true);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        // Right-click em OUTRO player/mob → cura nele.
        if (target == player) return InteractionResult.PASS;
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS; // animação
        }

        boolean success = applyCure(player, target);
        if (success) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§b✦ Essência clara aplicada em §a"
                                        + target.getName().getString()),
                        true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Aplica a cura completa em {@code target}. Tenta cada componente
     * independentemente — qualquer SUCESSO conta como cura aplicada. Retorna
     * {@code true} se ALGUMA coisa foi modificada.
     */
    private boolean applyCure(Player source, LivingEntity target) {
        boolean anyEffectApplied = false;

        // (1) MATTER PROFILE — primary heal (50% WM, -50 DM). User pediu:
        // "é pra curar 50% da materia clara". MAX = 100, então +50 = 50%.
        if (target instanceof ServerPlayer serverTarget) {
            var profileOpt = serverTarget.getCapability(MatterProfileProvider.CAP).resolve();
            if (profileOpt.isPresent()) {
                MatterProfile profile = profileOpt.get();
                float beforeWhite = profile.getWhite();
                float beforeDark = profile.getDark();
                profile.addWhite(WHITE_HEAL);
                profile.addDark(-DARK_REDUCE);
                if (profile.getWhite() != beforeWhite || profile.getDark() != beforeDark) {
                    anyEffectApplied = true;
                }
                MatterProfileEvents.syncTo(serverTarget);
            }
        }

        // (2) INFECTION — reduz infecção, max-infection, permanent HP penalty
        // e LIMPA todas mutações. Opcional — só se target tem capability.
        var infOpt = target.getCapability(ModCapabilities.INFECTION).resolve();
        if (infOpt.isPresent()) {
            var data = infOpt.get();
            int beforeInf = data.getInfection();
            data.reduceInfection(30);
            data.reducePermanentHealthPenalty(2);
            data.setMaxInfectionReached(Math.max(0, data.getMaxInfectionReached() - 40));
            data.setMutations("");
            data.setDirty(true);
            if (data.getInfection() != beforeInf) {
                anyEffectApplied = true;
            }
            if (target instanceof ServerPlayer serverTarget) {
                InfectionLogic.applyDerivedEffects(serverTarget, data);
                InfectionLogic.sync(serverTarget, data);
            }
        }

        // (3) Remove negative effects (sempre aplicado — não conta pro
        // "anyEffectApplied" porque é cleanup defensivo).
        target.removeEffect(ModEffects.DARK_INFECTION.get());
        target.removeEffect(ModEffects.RADIATION_SICKNESS.get());
        target.removeEffect(net.minecraft.world.effect.MobEffects.HUNGER);
        target.removeEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN);
        target.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
        target.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        target.removeEffect(net.minecraft.world.effect.MobEffects.CONFUSION);
        target.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        target.removeEffect(net.minecraft.world.effect.MobEffects.WITHER);

        // (4) Clear Shield — 3 min de immunity. Conta como effect aplicado
        // mesmo que outras curas não tenham mexido nada.
        boolean hadShield = target.hasEffect(ModEffects.CLEAR_SHIELD.get());
        target.addEffect(new MobEffectInstance(
                ModEffects.CLEAR_SHIELD.get(), 3600, 0, false, true, true));
        if (!hadShield) anyEffectApplied = true;

        // (5) Som — sempre, pra feedback.
        source.level().playSound(null, target.blockPosition(),
                ModSounds.CLEAR_HUM.get(), SoundSource.PLAYERS, 0.9F, 1.25F);

        return anyEffectApplied;
    }
}
