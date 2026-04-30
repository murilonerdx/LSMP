package br.com.murilo.liberthia.sounds;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class DarkMatterSwordSoundEvents {

    private static final int SOUND_INTERVAL_TICKS = 20 * 30;
    private static final Logger log = LoggerFactory.getLogger(DarkMatterSwordSoundEvents.class);

    private DarkMatterSwordSoundEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % SOUND_INTERVAL_TICKS != 0) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean holdingDarkMatterSword =
                mainHand.is(ModItems.DARK_MATTER_SWORD.get()) ||
                        offHand.is(ModItems.DARK_MATTER_SWORD.get());

        if (!holdingDarkMatterSword) {
            return;
        }

        player.playNotifySound(
                ModSounds.DARK_MATTER_SWORD.get(),
                SoundSource.PLAYERS,
                0.7F,
                0.8F
        );

        log.info("PLAY DARK MATTER");


    }
}
