package br.com.murilo.liberthia.event;


import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.init.ClientSpiritualState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = LiberthiaMod.MODID,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT
)
public final class ClientRenderEvents {

    private ClientRenderEvents() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer viewer = minecraft.player;

        if (viewer == null) {
            return;
        }

        Player renderedPlayer = event.getEntity();

        if (renderedPlayer.getUUID().equals(viewer.getUUID())) {
            return;
        }

        String channelId = ClientSpiritualState.getChannelFor(renderedPlayer.getUUID());

        if (channelId == null) {
            return;
        }

        boolean canSee = ClientSpiritualState.canLocalPlayerSee(
                viewer,
                renderedPlayer.getUUID(),
                channelId
        );

        if (!canSee) {
            event.setCanceled(true);
        }
    }
}
