package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.client.screen.ImageBookEditorScreen;
import br.com.murilo.liberthia.client.screen.ImageBookViewerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientBookHooks {

    private ClientBookHooks() {
    }

    public static void openEditor() {
        Minecraft.getInstance().setScreen(new ImageBookEditorScreen());
    }

    public static void openViewer(ItemStack stack) {
        Minecraft.getInstance().setScreen(new ImageBookViewerScreen(stack));
    }
}