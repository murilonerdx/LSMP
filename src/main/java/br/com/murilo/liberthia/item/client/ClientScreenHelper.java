package br.com.murilo.liberthia.item.client;

import br.com.murilo.liberthia.entry.FieldJournalScreen;
import br.com.murilo.liberthia.entry.TrackerScreen;
import br.com.murilo.liberthia.entry.WorkerVoiceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

/**
 * Client-only helper for opening Screens from item classes.
 * Lives in a separate class so the dedicated server NEVER loads Screen classes
 * (RuntimeDistCleaner guards prevent client-class loading on server dist).
 *
 * Callers MUST route through DistExecutor so this class is only referenced on client.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientScreenHelper {
    private ClientScreenHelper() {}

    public static void openTracker(UUID targetId, String name) {
        Minecraft.getInstance().setScreen(new TrackerScreen(targetId, name));
    }

    public static void openFieldJournal(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new FieldJournalScreen(hand));
    }

    public static void openWorkerVoice() {
        Minecraft.getInstance().setScreen(new WorkerVoiceScreen());
    }
}
