package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.TryEscapeSealPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CapturedBySealScreen extends Screen {

    public CapturedBySealScreen() {
        super(Component.literal("Aprisionado"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Tentar fugir"),
                                button -> ModNetwork.CHANNEL.sendToServer(new TryEscapeSealPacket())
                        )
                        .bounds(centerX - 60, centerY + 35, 120, 20)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xCC555555);

        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("VOCÊ FOI APRISIONADO")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                this.width / 2,
                this.height / 2 - 45,
                0xFF2222
        );

        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Um selo espiritual contém sua presença.")
                        .withStyle(ChatFormatting.GRAY),
                this.width / 2,
                this.height / 2 - 20,
                0xDDDDDD
        );

        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Sua chance de fuga depende da sua vida máxima e do melhor dano no inventário.")
                        .withStyle(ChatFormatting.DARK_GRAY),
                this.width / 2,
                this.height / 2 - 5,
                0xAAAAAA
        );

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}