package br.com.murilo.liberthia.cosmic.scare;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * r173: Tela de CRASH FALSA — parece um crash report, mas o jogador fecha no
 * botão (ou ESC). Mind-game puro.
 */
@OnlyIn(Dist.CLIENT)
public class FakeCrashScreen extends Screen {

    private static final String[] LINES = {
            "// Everything's going to plan. No, really, that was supposed to happen.",
            "",
            "Description: Rendering overlay",
            "",
            "java.lang.NullPointerException: Reality",
            "\tat net.minecraft.client.renderer.GameRenderer.render(GameRenderer.java:???)",
            "\tat liberthia.cosmic.█████(Unknown Source)",
            "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)",
            "",
            "-- System Details --",
            "\tMinecraft Version: 1.20.1",
            "\tOperating System: ??? (you are being watched)",
            "\tJava Version: ██.█",
            "\tMemory: 0 bytes / 0 bytes",
            "\tObserver: PRESENT",
    };

    public FakeCrashScreen() { super(Component.literal("Minecraft Crash Report")); }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Voltar ao jogo"), b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 36, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0xFF1A1A1A);
        int y = 18;
        g.drawString(font, "§c---- Minecraft Crash Report ----", 18, y, 0xFFFF5555, false);
        y += 16;
        for (String l : LINES) {
            g.drawString(font, "§7" + l, 18, y, 0xFFBBBBBB, false);
            y += 11;
        }
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return true; }
}
