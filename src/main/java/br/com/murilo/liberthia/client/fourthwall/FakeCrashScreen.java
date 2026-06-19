package br.com.murilo.liberthia.client.fourthwall;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * "Crash" FALSO — uma tela que imita um relatório de crash do Minecraft, trava
 * o input por ~2s e some sozinha voltando pro jogo. Pânico puro.
 */
@OnlyIn(Dist.CLIENT)
public class FakeCrashScreen extends Screen {

    private int ticks;

    private static final String[] LINES = {
            "---- Minecraft Crash Report ----",
            "// ele estava aqui o tempo todo",
            "",
            "Time: agora",
            "Description: Observação não tratada",
            "",
            "java.lang.IllegalStateException: você não devia ter visto isto",
            "    at liberthia.observador.olhar(VOCE.java:1)",
            "    at liberthia.presenca.atras_de_voce(Nunca.java:0)",
            "    at net.minecraft.client.main.Main.run(Main.java:###)",
            "",
            "-- System Details --",
            "    Observado por: SIM",
            "    Sozinho: NAO",
            "    Minecraft Version: ??.??.??",
    };

    public FakeCrashScreen() {
        super(Component.literal("crash"));
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void tick() {
        if (++ticks >= 40) onClose(); // ~2 segundos
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, this.width, this.height, 0xFF0A0A0A);
        int y = 16;
        for (String s : LINES) {
            g.drawString(this.font, s, 16, y, 0xFFB0B0B0, false);
            y += 11;
        }
    }
}
