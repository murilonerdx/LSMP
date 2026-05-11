package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.command.BookRedKirikoCommand;
import br.com.murilo.liberthia.command.ModCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber
public class CommandEvents {

    // /liberthia admin           -> sub vazio
    // /liberthia admin link      -> sub = link
    // /liberthia admin status    -> sub = status
    private static final Pattern ADMIN_PATTERN =
            Pattern.compile("^/?liberthia\\s+admin(?:\\s+(\\S+))?\\s*$", Pattern.CASE_INSENSITIVE);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
        br.com.murilo.liberthia.command.CultCampCommand.register(event.getDispatcher());
        BookRedKirikoCommand.register(event.getDispatcher());
    }

    /**
     * Intercepta /liberthia admin antes do Brigadier vomitar "comando desconhecido".
     * O subcomando "admin" não é registrado de propósito — assim some do autocomplete.
     * OPs digitam manualmente; aqui a gente captura, cancela o evento e roda nosso handler.
     */
    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        try {
            String input = event.getParseResults().getReader().getString();
            Matcher m = ADMIN_PATTERN.matcher(input);
            if (!m.matches()) return;

            event.setCanceled(true);
            CommandSourceStack source = event.getParseResults().getContext().getSource();
            String sub = m.group(1);
            ModCommands.handleSecretAdmin(source, sub);
        } catch (Throwable ignored) {
            // se algo quebrar, deixa Brigadier seguir o curso normal
        }
    }
}
