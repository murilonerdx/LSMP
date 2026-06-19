package br.com.murilo.liberthia.cosmic.scare;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;

/**
 * r173: <b>/liberthia scare</b> — quebra de 4ª parede (OP, silencioso).
 *
 * <pre>
 * /liberthia scare alert [alvos] [texto]
 * /liberthia scare crash [alvos]
 * /liberthia scare kick  [alvos] [texto]
 * /liberthia scare shake [alvos] [texto]
 * /liberthia scare static [alvos]
 * /liberthia scare flash [alvos] [face 1-6]
 * </pre>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ScareCommand {

    private ScareCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("liberthia")
                .then(Commands.literal("scare")
                        .requires(s -> s.hasPermission(2))
                        .then(typeWithText("alert", ScareType.ALERT, 100))
                        .then(typeSimple("crash", ScareType.CRASH, 1))
                        .then(typeWithText("kick", ScareType.KICK, 1))
                        .then(typeWithText("shake", ScareType.SHAKE, 60))
                        .then(typeSimple("static", ScareType.STATIC, 60))
                        // ── r175: novas opções de terror / 4ª parede / loucura ──
                        .then(typeWithText("whisper", ScareType.WHISPER, 90))
                        .then(typeSimple("blackout", ScareType.BLACKOUT, 60))
                        .then(typeSimple("eyes", ScareType.EYES, 120))
                        .then(typeSimple("heartbeat", ScareType.HEARTBEAT, 160))
                        .then(typeSimple("glitch", ScareType.GLITCH, 50))
                        .then(typeWithText("fakedeath", ScareType.FAKEDEATH, 1))
                        .then(typeSimple("bsod", ScareType.BSOD, 1))
                        .then(typeWithText("fakechat", ScareType.FAKECHAT, 1))
                        // ── r177: +8 sustos ──
                        .then(typeSimple("crack", ScareType.CRACK, 120))
                        .then(typeWithText("countdown", ScareType.COUNTDOWN, 200))
                        .then(typeWithText("fakejoin", ScareType.FAKEJOIN, 1))
                        .then(typeWithText("watermark", ScareType.WATERMARK, 120))
                        .then(typeSimple("eye", ScareType.EYE, 160))
                        .then(typeSimple("tunnel", ScareType.TUNNEL, 100))
                        .then(typeSimple("screenshot", ScareType.SCREENSHOT, 14))
                        .then(typeSimple("scanroll", ScareType.SCANROLL, 140))
                        // ── r177: Grupo A ──
                        .then(typeSimple("freeze", ScareType.NOTRESPONDING, 50))
                        .then(typeSimple("reverse", ScareType.REVERSE, 100))
                        .then(typeSimple("corrupthud", ScareType.CORRUPTHUD, 80))
                        .then(typeWithText("narrator", ScareType.NARRATOR, 120))
                        .then(typeSimple("cursor", ScareType.CURSOR, 140))
                        .then(typeSimple("falsecoords", ScareType.FALSECOORDS, 120))
                        .then(typeSimple("blink", ScareType.BLINK, 14))
                        // ── r178: +4 ──
                        .then(typeSimple("lowbattery", ScareType.LOWBATTERY, 90))
                        .then(typeSimple("discordping", ScareType.DISCORDPING, 70))
                        .then(typeSimple("mirror", ScareType.MIRROR, 120))
                        .then(typeWithText("typetext", ScareType.TYPETEXT, 130))
                        // r177: liga/desliga o "piscar no escuro" automático (a cada N min)
                        .then(Commands.literal("blinkmode")
                                .then(Commands.argument("alvos", EntityArgument.players())
                                        .then(Commands.literal("off").executes(c -> setBlink(c, false, 10)))
                                        .then(Commands.literal("on")
                                                .executes(c -> setBlink(c, true, 10))
                                                .then(Commands.argument("minutos", IntegerArgumentType.integer(1, 120))
                                                        .executes(c -> setBlink(c, true,
                                                                IntegerArgumentType.getInteger(c, "minutos")))))))
                        .then(Commands.literal("reloadfaces")
                                .executes(c -> fire(c, List.of(self(c)), ScareType.RELOADFACES, 1, 0, "")))
                        .then(Commands.literal("flash")
                                .executes(c -> fire(c, List.of(self(c)), ScareType.FLASH, 10, 0, ""))
                                .then(Commands.argument("alvos", EntityArgument.players())
                                        .executes(c -> fire(c, players(c), ScareType.FLASH, 10, 0, ""))
                                        .then(Commands.argument("face", IntegerArgumentType.integer(1, 64))
                                                .executes(c -> fire(c, players(c), ScareType.FLASH, 10,
                                                        IntegerArgumentType.getInteger(c, "face") - 1, "")))))));
    }

    /** Subcomando com alvos opcionais + texto opcional. */
    private static LiteralArgumentBuilder<CommandSourceStack> typeWithText(String name, ScareType type, int dur) {
        return Commands.literal(name)
                .executes(c -> fire(c, List.of(self(c)), type, dur, 0, ""))
                .then(Commands.argument("alvos", EntityArgument.players())
                        .executes(c -> fire(c, players(c), type, dur, 0, ""))
                        .then(Commands.argument("texto", StringArgumentType.greedyString())
                                .executes(c -> fire(c, players(c), type, dur, 0,
                                        StringArgumentType.getString(c, "texto")))));
    }

    /** Subcomando com alvos opcionais, sem texto. */
    private static LiteralArgumentBuilder<CommandSourceStack> typeSimple(String name, ScareType type, int dur) {
        return Commands.literal(name)
                .executes(c -> fire(c, List.of(self(c)), type, dur, 0, ""))
                .then(Commands.argument("alvos", EntityArgument.players())
                        .executes(c -> fire(c, players(c), type, dur, 0, "")));
    }

    private static ServerPlayer self(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return c.getSource().getPlayerOrException();
    }

    private static Collection<ServerPlayer> players(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return EntityArgument.getPlayers(c, "alvos");
    }

    /** Liga/desliga o "piscar no escuro" automático pros alvos. */
    private static int setBlink(CommandContext<CommandSourceStack> c, boolean on, int minutes) {
        Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(c, "alvos");
        } catch (Exception e) {
            return 0;
        }
        for (ServerPlayer p : targets) {
            BlinkInDarkManager.setEnabled(p, on, minutes);
        }
        final int n = targets.size();
        c.getSource().sendSuccess(() -> Component.literal("§5✦ piscar-no-escuro "
                + (on ? "§aON" : "§cOFF") + " §7→ §f" + n + " §7player(s)"
                + (on ? " §8(cada " + minutes + " min)" : "")), false);
        return n;
    }

    private static int fire(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> targets,
                            ScareType type, int dur, int variant, String text) {
        for (ServerPlayer p : targets) {
            ModNetwork.sendToPlayer(p, new ScareS2CPacket(type, dur, variant, text));
            // r175: barulho de caverna em CADA susto (menos no reload de imagens)
            if (type != ScareType.RELOADFACES) {
                p.serverLevel().playSound(null, p.blockPosition(),
                        SoundEvents.AMBIENT_CAVE.value(), SoundSource.MASTER,
                        1.2F, 0.6F + p.getRandom().nextFloat() * 0.3F);
            }
            if (type == ScareType.STATIC) {
                // barulho de estática ATRÁS do player (real, posicional)
                Vec3 behind = p.position().subtract(p.getLookAngle().scale(2.5)).add(0, 1, 0);
                p.serverLevel().playSound(null, BlockPos.containing(behind),
                        SoundEvents.SOUL_ESCAPE, SoundSource.MASTER, 1.3F, 0.35F);
            }
            if (type == ScareType.CRACK) {
                p.serverLevel().playSound(null, p.blockPosition(),
                        SoundEvents.GLASS_BREAK, SoundSource.MASTER, 1.5F, 0.7F);
            }
            if (type == ScareType.SCREENSHOT) {
                p.serverLevel().playSound(null, p.blockPosition(),
                        SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.MASTER, 1.0F, 2.0F);
            }
            if (type == ScareType.DISCORDPING) {
                p.serverLevel().playSound(null, p.blockPosition(),
                        SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.MASTER, 0.8F, 1.6F);
            }
        }
        final int n = targets.size();
        // silencioso: só o autor vê o feedback
        c.getSource().sendSuccess(() -> Component.literal("§5✦ scare §d" + type.name().toLowerCase()
                + " §7→ §f" + n + " §7player(s)"), false);
        return n;
    }
}
