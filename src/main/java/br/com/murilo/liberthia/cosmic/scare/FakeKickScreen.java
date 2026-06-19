package br.com.murilo.liberthia.cosmic.scare;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * r173: Tela de KICK FALSA — imita "Connection Lost / Disconnected". O botão
 * só fecha (o jogador continua no mundo — pânico controlado).
 */
@OnlyIn(Dist.CLIENT)
public class FakeKickScreen extends Screen {

    private final String reason;

    public FakeKickScreen(String reason) {
        super(Component.literal("Connection Lost"));
        this.reason = (reason == null || reason.isEmpty()) ? "Você foi removido do servidor" : reason;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Voltar ao título"), b -> onClose())
                .bounds(this.width / 2 - 100, this.height / 2 + 40, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0xFF0D0D0D);
        g.drawCenteredString(font, "§f§lConexão Perdida", this.width / 2, this.height / 2 - 46, 0xFFFFFFFF);
        g.drawCenteredString(font, "§7" + reason, this.width / 2, this.height / 2 - 24, 0xFFAAAAAA);
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return true; }
}
