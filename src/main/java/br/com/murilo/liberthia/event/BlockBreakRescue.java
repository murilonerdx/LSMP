package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.capture.CapturedPlayerManager;
import br.com.murilo.liberthia.freeze.FreezeManager;
import br.com.murilo.liberthia.item.PlayerLockItem;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;

/**
 * r178 HOTFIX — "não consigo quebrar blocos com a picareta".
 *
 * <p><b>Causa raiz:</b> a Fadiga de Mineração (DIG_SLOWDOWN) em amplitude ≥2 multiplica
 * a velocidade de quebra por ~0,003 — ou seja, o bloco fica <b>praticamente inquebrável</b>.
 * E como efeitos de poção são <b>salvos no player.dat</b>, um efeito grudado por uma versão
 * antiga do código (ou por uma magia/armadilha de longa duração) <b>continua no personagem
 * mesmo depois de rebuildar o mod</b> — por isso "consertar e não adiantar". Há ainda 4
 * sistemas que cancelam {@code BlockEvent.BreakEvent} quando ativos: {@link PlayerLockEvents}
 * (NBT {@code liberthia_locked}), {@code FreezeEvents}, {@code CaptureEvents}, {@code PossessionManager}.
 *
 * <p><b>Correção:</b>
 * <ul>
 *   <li><b>Rede de segurança</b> (a cada 1s, sobrevivência): se houver Mining Fatigue de
 *       amplitude ≥2, remove. A infecção legítima usa amp 0 (slow leve, ainda mineável),
 *       então o jogo normal NÃO é afetado — só o estado "inquebrável" é desfeito.</li>
 *   <li><b>Login</b>: limpa todos os estados de trava (auto-recuperação ao entrar).</li>
 *   <li><b>{@code /liberthia unlock}</b>: alívio imediato sem precisar deslogar.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class BlockBreakRescue {

    private BlockBreakRescue() {}

    /** Remove TODO estado que impede o jogador de quebrar blocos. */
    public static void rescue(ServerPlayer sp) {
        sp.removeEffect(MobEffects.DIG_SLOWDOWN);                 // Mining Fatigue grudada
        sp.getPersistentData().putBoolean(PlayerLockItem.NBT_LOCKED, false); // lock op-only
        FreezeManager.unfreeze(sp.getUUID());                    // Relógio/Freeze
        if (CapturedPlayerManager.isCaptured(sp)) {
            CapturedPlayerManager.release(sp);                   // captura/prisão
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) rescue(sp);
    }

    /**
     * Rede de segurança contínua: nunca deixa um player de sobrevivência preso com
     * Mining Fatigue que atrapalhe minerar. amp 0 (slow leve da infecção) é mantido;
     * amp ≥1 (Fadiga II+, já deixa pedra lentíssima) é removido. Roda 1×/s, custo desprezível.
     */
    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return;
        if (sp.isCreative() || sp.isSpectator()) return;
        MobEffectInstance fatigue = sp.getEffect(MobEffects.DIG_SLOWDOWN);
        if (fatigue != null && fatigue.getAmplifier() >= 1) {
            sp.removeEffect(MobEffects.DIG_SLOWDOWN);
        }
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("liberthia")
                .then(Commands.literal("unlock")
                        .executes(c -> unlock(c, List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("alvos", EntityArgument.players())
                                .requires(s -> s.hasPermission(2))
                                .executes(c -> unlock(c, EntityArgument.getPlayers(c, "alvos"))))));
    }

    private static int unlock(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> targets)
            throws CommandSyntaxException {
        for (ServerPlayer sp : targets) rescue(sp);
        final int n = targets.size();
        c.getSource().sendSuccess(() -> Component.literal(
                "§a✦ Destravado (" + n + "): Fadiga de Mineração / Freeze / Lock / Captura removidos — pode quebrar blocos de novo."), false);
        return n;
    }
}
