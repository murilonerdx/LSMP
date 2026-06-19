package br.com.murilo.liberthia.cosmic.scare;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * r175: <b>Tela Azul da Morte (BSOD) FALSA</b> — imita o crash de sistema do
 * Windows. O botão só fecha (o jogo continua). Puro susto de 4ª parede.
 */
@OnlyIn(Dist.CLIENT)
public class FakeBsodScreen extends Screen {

    private static final int BSOD_BLUE = 0xFF0A2A8C;

    public FakeBsodScreen() {
        super(Component.literal("BSOD"));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal(" "), b -> onClose())
                .bounds(this.width - 24, this.height - 18, 16, 12).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, BSOD_BLUE);
        int x = this.width / 8;
        int y = this.height / 4;
        g.pose().pushPose();
        g.pose().translate(x, y - 30, 0);
        g.pose().scale(4F, 4F, 1F);
        g.drawString(font, ":(", 0, 0, 0xFFFFFFFF, false);
        g.pose().popPose();
        g.drawString(font, "Seu dispositivo encontrou um problema e precisa reiniciar.", x, y + 10, 0xFFFFFFFF, false);
        g.drawString(font, "Estamos coletando algumas informações de erro e então", x, y + 24, 0xFFD8E0FF, false);
        g.drawString(font, "reiniciaremos por você.", x, y + 34, 0xFFD8E0FF, false);
        g.drawString(font, "0% concluído", x, y + 58, 0xFFFFFFFF, false);
        g.drawString(font, "Código de parada: REALITY_FAULT_NOT_HANDLED", x, y + 86, 0xFFD8E0FF, false);
        g.drawString(font, "§8(pressione ESC)", x, this.height - 24, 0xFF8899DD, false);
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return true; }
}
