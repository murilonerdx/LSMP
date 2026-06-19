package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.event.PossessionManager;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.StartPossessionS2CPacket;
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

import java.util.List;

/**
 * Possession Amulet — item da Feature 2 ("Possessão").
 *
 * <p>Right-click numa {@link LivingEntity} ativa controle remoto: a câmera do
 * possessor passa a renderizar pelos olhos do alvo, e inputs WASD do possessor
 * são re-encaminhados pra mover o alvo via {@code PossessionMoveC2SPacket}.
 *
 * <p>Para terminar: o possessor aperta shift (cliente envia
 * {@code EndPossessionC2SPacket}).
 *
 * <h2>EPIC, foil, sem recipe</h2>
 * O item não tem recipe: só pode ser obtido via {@code /give} ou drop raro
 * (não implementado). Foil ativo por override de {@link #isFoil(ItemStack)}.
 */
public class PossessionAmuletItem extends Item {

    public PossessionAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide) {
            // client devolve SUCCESS pra animação da mão.
            return InteractionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayer possessor)) {
            return InteractionResult.PASS;
        }
        // Self-target: no-op.
        if (target.getUUID().equals(possessor.getUUID())) {
            possessor.displayClientMessage(Component.literal("Você não pode possuir a si mesmo.")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        // v0.1.22 r22: Mind Ward bloqueia tentativa de posse. Feedback rico:
        // partículas SOUL no target + som de "rejeição" + mensagem.
        if (br.com.murilo.liberthia.item.MindWardItem.hasWard(target)) {
            net.minecraft.server.level.ServerLevel sl = possessor.serverLevel();
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    20, 0.4, 0.6, 0.4, 0.05);
            sl.playSound(null, target.blockPosition(),
                    net.minecraft.sounds.SoundEvents.SOUL_ESCAPE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.5F);
            possessor.displayClientMessage(Component.literal(
                    "§5✦ A mente de §d" + target.getName().getString()
                            + "§5 está protegida — o amuleto não tem efeito.")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            // Cooldown leve pra evitar spam (1s)
            possessor.getCooldowns().addCooldown(this, 20);
            return InteractionResult.FAIL;
        }
        // v0.1.22 r8: se já possuindo, ENCERRA implicitamente antes de iniciar
        // nova posse. Antes user ficava preso em "Você já está possuindo outro
        // ser" — state stale após primeira posse impedia segunda tentativa.
        if (PossessionManager.getPossessed(possessor.getUUID()) != null) {
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                    "[Possession] {} já estava possuindo — encerrando posse anterior antes de iniciar nova",
                    possessor.getName().getString());
            PossessionManager.end(possessor.server, possessor.getUUID());
        }
        // Target já possuído por outra pessoa? Força liberação.
        if (PossessionManager.isPossessed(target.getUUID())) {
            java.util.UUID otherPossessor = PossessionManager.getPossessor(target.getUUID());
            if (otherPossessor != null) {
                br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                        "[Possession] target {} estava possuído por {} — força end",
                        target.getName().getString(), otherPossessor);
                PossessionManager.end(possessor.server, otherPossessor);
            }
        }
        if (!PossessionManager.start(possessor, target)) {
            possessor.displayClientMessage(Component.literal("Não foi possível iniciar a posse — tente novamente.")
                    .withStyle(ChatFormatting.RED), true);
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.warn(
                    "[Possession] start({} → {}) falhou mesmo após cleanup",
                    possessor.getName().getString(), target.getName().getString());
            return InteractionResult.FAIL;
        }
        br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                "[Possession] start: {} → {}",
                possessor.getName().getString(), target.getName().getString());
        // Manda S2C pro possessor pra redirecionar a câmera. Usa entity ID
        // (não UUID) porque é mais rápido pro client resolver.
        ModNetwork.sendToPlayer(possessor, new StartPossessionS2CPacket(target.getId()));

        // Som ambient sutil.
        possessor.level().playSound(null, possessor.blockPosition(),
                SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.4f, 1.6f);
        possessor.displayClientMessage(Component.literal("Possessão iniciada — segure shift para liberar.")
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Foil sempre ativo (efeito visual de glint).
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Amuleto da Possessão")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Clique-direito em um ser vivo para controlá-lo")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Shift para liberar")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Item proibido — obtido apenas por meios sombrios")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
