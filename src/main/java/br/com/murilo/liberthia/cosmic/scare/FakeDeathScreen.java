package br.com.murilo.liberthia.cosmic.scare;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * r175: Tela "Você Morreu" FALSA — imita a death screen, mas o botão só fecha
 * (o jogador continua vivo no mundo). Pânico controlado.
 */
@OnlyIn(Dist.CLIENT)
public class FakeDeathScreen extends Screen {

    private final String msg;

    public FakeDeathScreen(String msg) {
        super(Component.literal("You Died!"));
        this.msg = (msg == null || msg.isEmpty()) ? "Algo te encontrou." : msg;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Reaparecer"), b -> onClose())
                .bounds(this.width / 2 - 100, this.height / 4 + 72, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Tela de título"), b -> onClose())
                .bounds(this.width / 2 - 100, this.height / 4 + 96, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, this.width, this.height, 0x60500000, 0x90000000);
        g.pose().pushPose();
        g.pose().translate(this.width / 2.0, this.height / 4.0 + 10, 0);
        g.pose().scale(2.0F, 2.0F, 1F);
        g.drawCenteredString(font, "§4§lVocê Morreu!", 0, 0, 0xFFFFFFFF);
        g.pose().popPose();
        g.drawCenteredString(font, "§7" + msg, this.width / 2, this.height / 4 + 50, 0xFFE0E0E0);
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return true; }
}
