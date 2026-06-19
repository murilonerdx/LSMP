package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.event.VisionSwapManager;
import br.com.murilo.liberthia.matter.MatterProfile;
import br.com.murilo.liberthia.matter.MatterProfileProvider;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.StartVisionSwapS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Vision Swap Lens — item da Feature 1 (White Matter Vision Swap).
 *
 * <p>Ao usar (right-click no ar), procura todos os players online com
 * {@link MatterProfile#getWhite()} ≥ {@link VisionSwapManager#MIN_WHITE_FOR_TARGET}.
 * Se houver pelo menos um, escolhe aleatoriamente e ativa a sessão de
 * {@link VisionSwapManager} por 8 segundos. Durante esse tempo, a câmera
 * do user é redirecionada (via {@code Minecraft.setCameraEntity}) para o
 * alvo, e seus inputs são bloqueados client-side. O corpo físico do user
 * continua exatamente onde estava — outros players o veem normal.
 *
 * <p>Cooldown: 60s. Cooldown é client-driven (via {@code Cooldowns}) e é o
 * único guard contra spam — não há "carga" no item.
 *
 * <p>Edge cases:
 * <ul>
 *   <li>Sem player elegível: mensagem no chat ("Nenhum player infectado por
 *       WM encontrado"), sem cooldown.</li>
 *   <li>User já está em um swap ativo: bloqueado (start retorna false).</li>
 * </ul>
 */
public class VisionSwapLensItem extends Item {

    private static final int COOLDOWN_TICKS = 60 * 20;

    public VisionSwapLensItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // Cliente apenas devolve sided-success para o sound de hand-swing.
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!(player instanceof ServerPlayer user)) {
            return InteractionResultHolder.pass(stack);
        }
        MinecraftServer server = user.server;
        if (server == null) return InteractionResultHolder.pass(stack);

        // Já existe swap ativo desse user? Bloqueia.
        if (VisionSwapManager.isUserSwapping(user.getUUID())) {
            user.displayClientMessage(Component.literal("Você já está observando alguém.")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }

        // Coleta candidatos: player !== user com WM ≥ threshold e na mesma dim
        // (client precisa do target carregado pra resolver setCameraEntity).
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getUUID().equals(user.getUUID())) continue;
            if (p.level() != user.level()) continue;
            float wm = p.getCapability(MatterProfileProvider.CAP)
                    .map(MatterProfile::getWhite)
                    .orElse(0f);
            if (wm >= VisionSwapManager.MIN_WHITE_FOR_TARGET) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty()) {
            user.displayClientMessage(
                    Component.literal("Nenhum player infectado por WM encontrado.")
                            .withStyle(ChatFormatting.GRAY),
                    false);
            return InteractionResultHolder.fail(stack);
        }

        ServerPlayer target = candidates.get(level.getRandom().nextInt(candidates.size()));
        if (!VisionSwapManager.start(user, target)) {
            // start retornou false (ex.: race condition) — sem cooldown.
            return InteractionResultHolder.fail(stack);
        }

        // Tudo certo: manda packet pra cliente do user e aplica cooldown.
        ModNetwork.sendToPlayer(user, new StartVisionSwapS2CPacket(target.getUUID()));
        user.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        // Som ambiente sutil pro user
        level.playSound(null, user.blockPosition(), SoundEvents.SPYGLASS_USE,
                SoundSource.PLAYERS, 0.6f, 1.2f);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Lente da Vidência — White Matter")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Use para ver pelos olhos de outro infectado")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Duração: 8s · Cooldown: 60s")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
