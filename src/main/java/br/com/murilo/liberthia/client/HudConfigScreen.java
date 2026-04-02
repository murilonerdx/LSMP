package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.config.LiberthiaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class HudConfigScreen extends Screen {
    private boolean draggingInfection = false;
    private boolean draggingExposure = false;
    private boolean draggingDna = false;

    public HudConfigScreen() {
        super(Component.literal("Configuração de HUD"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal("Clique e arraste os elementos para reposicionar"), this.width / 2, 22, 0xAAAAAA);

        int infX = LiberthiaConfig.CLIENT.infectionX.get();
        int infY = LiberthiaConfig.CLIENT.infectionY.get();
        int expX = LiberthiaConfig.CLIENT.exposureX.get();
        int expY = LiberthiaConfig.CLIENT.exposureY.get();
        int dnaX = LiberthiaConfig.CLIENT.dnaX.get();
        int dnaY = LiberthiaConfig.CLIENT.dnaY.get();

        // Render dummy HUDs for preview (matching premium look)
        renderDummyInfection(guiGraphics, infX, infY);
        renderDummyExposure(guiGraphics, expX, expY);
        renderDummyDna(guiGraphics, dnaX, dnaY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderDummyInfection(GuiGraphics guiGraphics, int x, int y) {
        int width = 110;
        int height = 10;
        guiGraphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xCC1A1A1A);
        guiGraphics.fill(x, y, x + width, y + height, 0xFF2D2D2D);
        guiGraphics.fill(x, y, x + 60, y + height, 0xFF3D1F5B);
        
        guiGraphics.drawString(this.font, "Nível: 50% (Exemplo)", x, y + 14, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, "Estado: CONTAMINAÇÃO", x, y + 22, 0xD9B3FF, true);
        guiGraphics.drawString(this.font, "Tipo: Viral", x, y + 30, 0xAAAAAA, true);
    }

    private void renderDummyExposure(GuiGraphics guiGraphics, int x, int y) {
        int width = 140;
        int height = 30;
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xCC000000);
        guiGraphics.fill(x, y + height - 2, x + 40, y + height, 0xFFE6D36C);
        guiGraphics.drawString(this.font, "RADIAÇÃO: 25 Sps", x + 4, y + 6, 0xFFE6D36C, true);
        guiGraphics.drawString(this.font, "DEFESA: 50%", x + 4, y + 16, 0xAAAAAA, true);
    }

    private void renderDummyDna(GuiGraphics guiGraphics, int x, int y) {
        int width = 140;
        int height = 56;
        guiGraphics.fill(x - 4, y - 6, x + width + 6, y + height, 0xAA0A0A0A);
        guiGraphics.drawString(this.font, "DNA Mutation", x, y - 2, 0xFFE0E0E0, false);
        guiGraphics.drawString(this.font, "Escura: 45%", x + 4, y + 12, 0xFF7B2CBF, false);
        guiGraphics.drawString(this.font, "Clara: 30%", x + 4, y + 24, 0xFF4CC9F0, false);
        guiGraphics.drawString(this.font, "Amarela: 25%", x + 4, y + 36, 0xFFF4B400, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int infX = LiberthiaConfig.CLIENT.infectionX.get();
        int infY = LiberthiaConfig.CLIENT.infectionY.get();
        // Area covers the bar + 3 lines of text
        if (mouseX >= infX - 5 && mouseX <= infX + 115 && mouseY >= infY - 5 && mouseY <= infY + 45) {
            draggingInfection = true;
            return true;
        }

        int expX = LiberthiaConfig.CLIENT.exposureX.get();
        int expY = LiberthiaConfig.CLIENT.exposureY.get();
        if (mouseX >= expX - 5 && mouseX <= expX + 145 && mouseY >= expY - 5 && mouseY <= expY + 35) {
            draggingExposure = true;
            return true;
        }

        int dnaX = LiberthiaConfig.CLIENT.dnaX.get();
        int dnaY = LiberthiaConfig.CLIENT.dnaY.get();
        if (mouseX >= dnaX - 8 && mouseX <= dnaX + 150 && mouseY >= dnaY - 8 && mouseY <= dnaY + 60) {
            draggingDna = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingInfection = false;
        draggingExposure = false;
        draggingDna = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingInfection) {
            LiberthiaConfig.CLIENT.infectionX.set((int) mouseX);
            LiberthiaConfig.CLIENT.infectionY.set((int) mouseY);
            return true;
        }
        if (draggingExposure) {
            LiberthiaConfig.CLIENT.exposureX.set((int) mouseX);
            LiberthiaConfig.CLIENT.exposureY.set((int) mouseY);
            return true;
        }
        if (draggingDna) {
            LiberthiaConfig.CLIENT.dnaX.set((int) mouseX);
            LiberthiaConfig.CLIENT.dnaY.set((int) mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public void onClose() {
        super.onClose();
        LiberthiaConfig.CLIENT_SPEC.save();
    }
}
