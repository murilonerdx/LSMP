package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.command.ModCommands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CommandEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }
}
