package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.matter.MatterProfileEvents;
import br.com.murilo.liberthia.matter.MatterProfileProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Matter Cure — cura "de emergência" para players com matter profile alto.
 *
 * <p>Ao consumir (segurar right-click, animação de bebida):
 * <ul>
 *   <li>Zera os 3 valores de matter (DM, WM, YM) do player → estado limpo</li>
 *   <li>Aplica Regen II por 10s + Resistance I por 10s (compensa o "trauma" do reset)</li>
 *   <li>Sincroniza profile pro client (HUD atualiza imediatamente)</li>
 * </ul>
 *
 * <p>Pré-requisito: o player precisa TER matter profile (>0 em qualquer
 * tipo). Se não tem matter pra curar, a cura é desperdiçada e o item NÃO é
 * consumido — feedback no actionbar.
 *
 * <p>Receita: 3x Purified Essence + 1x Glass Bottle + 1x Glowstone Dust
 * (na MatterAnalyzer ou crafting normal).
 */
public class MatterCureItem extends Item {

    public MatterCureItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Se player não tem matter algum, não consome (evita desperdício)
        var capOpt = player.getCapability(MatterProfileProvider.CAP);
        if (capOpt.isPresent()) {
            var profile = capOpt.orElseThrow(IllegalStateException::new);
            if (profile.getDark() <= 0.5f && profile.getWhite() <= 0.5f && profile.getYellow() <= 0.5f) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(
                            Component.literal("Você não tem matéria contaminada pra purificar.")
                                    .withStyle(ChatFormatting.GRAY),
                            true);
                }
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            // Zera matter profile + aplica effects de recuperação
            player.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
                profile.setDark(0f);
                profile.setWhite(0f);
                profile.setYellow(0f);
            });
            if (player instanceof ServerPlayer sp) {
                MatterProfileEvents.syncTo(sp);
            }

            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1)); // Regen II
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0)); // Resistance I
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, player.getSoundSource(), 1.0f, 1.2f);

            player.displayClientMessage(
                    Component.literal("✨ Matéria purificada. Você está limpo.")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);
        }

        // Consome 1 do stack (a não ser que player esteja em criativo)
        if (user instanceof Player p && !p.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32; // ~1.5s — mesma duração de poção
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Purifica matéria contaminada do corpo")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("• Zera DM/WM/YM")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("• Regen II + Resistance I (10s)")
                .withStyle(ChatFormatting.GREEN));
    }
}
