package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22: Boss Crown (Coroa do Boss) — item OP toggleável.
 *
 * <p>Right-click alterna o NBT {@code CrownActive}. Quando ATIVO E no
 * inventário do player, {@link br.com.murilo.liberthia.event.BossCrownHandler}
 * aplica TODOS os efeitos:
 *
 * <ul>
 *   <li>HP cap 3600 (1800 corações) via attribute modifier.</li>
 *   <li>Regeneration III constante (~1.5 HP/s).</li>
 *   <li>Aura damage: players a ≤ 3 blocos levam dano + Blood Infection + Weakness.</li>
 *   <li>Pulso a cada 10s: Blindness + Poison II em todos players num raio 8.</li>
 *   <li>Gaze: player olhando pro dono ganha Blindness + Blood Infection.</li>
 *   <li>30% damage reflect contra atacante.</li>
 *   <li>Panic explosion @ HP ≤ 20%: pushback 12 blocos + Slowness IV + Hunger III
 *       (cooldown 5 min).</li>
 * </ul>
 *
 * <p>Inativo = nenhum efeito roda (HP volta a 20, regen para). Toggle via
 * right-click no AR.
 */
public class BossCrownItem extends Item {

    public static final String NBT_ACTIVE = "CrownActive";

    public BossCrownItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isActive(stack);
    }

    public static boolean isActive(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(NBT_ACTIVE);
    }

    public static void setActive(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean(NBT_ACTIVE, active);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // v0.1.22 r3: SEMPRE abre a tela unificada (toggle + rename num lugar
        // só). Antes split entre shift e não-shift confundia o user.
        if (!level.isClientSide
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            String current = getBossBarName(stack);
            boolean activeNow = isActive(stack);
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                    "[BossCrown use] {} right-clicked — sending screen open packet name='{}' active={}",
                    sp.getName().getString(), current, activeNow);
            br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                    new br.com.murilo.liberthia.network.packet.OpenBossCrownNameScreenS2CPacket(
                            current, activeNow));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** v0.1.22: nome custom da bossbar (NBT). Vazio se nunca foi setado. */
    public static String getBossBarName(ItemStack stack) {
        if (!stack.hasTag()) return "";
        return stack.getTag().getString(
                br.com.murilo.liberthia.network.packet.SetBossCrownNameC2SPacket.NBT_BOSS_BAR_NAME);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        boolean active = isActive(stack);
        tooltip.add(Component.literal(active ? "✦ DESPERTA" : "✦ Adormecida")
                .withStyle(active ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GRAY,
                        ChatFormatting.BOLD));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§7Right-click§r§o: abrir tela (toggle + nome)")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        String custom = getBossBarName(stack);
        if (!custom.isEmpty()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Bossbar: ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(custom)));
        }
        if (active) {
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("• 1800 corações (HP cap)").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("• Regeneração III constante").withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.literal("• Aura: dano + infecção + fraqueza em quem se aproxima").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.literal("• Pulso 10s: cegueira + veneno raio 8").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.literal("• Olhar pra mim = cegueira + infecção").withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.add(Component.literal("• Reflete 30% do dano recebido").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal("• Pânico HP ≤ 20%: explosão de pushback (5min cd)").withStyle(ChatFormatting.GOLD));
        }
    }
}
