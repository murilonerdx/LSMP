package br.com.murilo.liberthia.entry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.command.SpiritualCommand;
import br.com.murilo.liberthia.init.SpiritualState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = LiberthiaMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ModCommonEvents {

    private ModCommonEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SpiritualCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SpiritualState.syncTo(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }

        ArrayList<UUID> activePlayers = new ArrayList<>(SpiritualState.snapshot().keySet());

        for (UUID uuid : activePlayers) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);

            if (player == null) {
                continue;
            }

            if (SpiritualState.findValidConnectionItem(player).isEmpty()) {
                SpiritualState.forceDisable(player);
            }
        }
    }
}