package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Right-click a living entity → runs {@code /scale add 0.1 <target>} at OP level.
 * Falls back gracefully if {@code /scale} is not registered (e.g. Pehkui not
 * installed) — no crash, just a chat hint.
 *
 * Op-only: non-ops get a "permission denied" message and no command is run.
 */
public class GrowthRodItem extends Item {
    private final double delta;

    public GrowthRodItem(Properties props, double delta) {
        super(props);
        this.delta = delta;
    }

    public GrowthRodItem(Properties props) {
        this(props, 0.1);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        if (!CommandRunner.isOp(sp)) {
            sp.displayClientMessage(
                    Component.literal("Apenas operadores (OP) podem usar este item.")
                            .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        // Pehkui's /scale syntax is: `scale add <amount> [<type>] [<targets>]`.
        // Putting the target name without a type confuses the parser, so we
        // run the command AS the target entity — `@s` then resolves to them
        // and the type defaults to `pehkui:base`.
        String cmd = "scale add " + delta;
        int rc = CommandRunner.runAs(target, cmd);

        if (rc <= 0) {
            // Try fallback variants in case the user's scale mod uses a
            // different command root.
            int fb = CommandRunner.runAs(target, "pehkui:scale add " + delta);
            if (fb <= 0) {
                sp.displayClientMessage(
                        Component.literal("Comando /scale não respondeu. Verifique se Pehkui (ou outro mod de escala) está instalado e se o alvo é compatível.")
                                .withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.FAIL;
            }
        }

        sp.level().playSound(null, target.blockPosition(),
                delta >= 0 ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.SOUL_ESCAPE,
                SoundSource.PLAYERS, 1.0F, delta >= 0 ? 1.4F : 0.8F);
        sp.displayClientMessage(
                Component.literal((delta >= 0 ? "+" : "") + delta + " escala em "
                                + target.getName().getString())
                        .withStyle(delta >= 0 ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE),
                true);
        sp.getCooldowns().addCooldown(this, 10);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal((delta >= 0 ? "§a+" : "§d") + delta + " §7escala no alvo (OP)"));
        tip.add(Component.literal("§7Requer mod de escala como Pehkui"));
    }
}
