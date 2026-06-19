package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.cosmic.idol.PhotoStore;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * r180: visualizador da foto da {@code CameraItem}. Mostra a FOTO REAL capturada
 * (PNG via {@link PhotoStore}) em tamanho grande, com moldura + legenda. Fotos
 * amaldiçoadas aparecem corrompidas (glitch). Aberto por right-click na Fotografia.
 */
public class PhotographViewScreen extends Screen {

    private final String photoId;
    private final boolean cursed;
    private final String caption;

    public PhotographViewScreen(String photoId, boolean cursed, String caption) {
        super(Component.literal("Fotografia"));
        this.photoId = photoId == null ? "" : photoId;
        this.cursed = cursed;
        this.caption = caption == null ? "" : caption;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);

        int size = (int) (Math.min(this.width, this.height) * 0.62);
        int x = (this.width - size) / 2;
        int y = (this.height - size) / 2 - 6;

        ResourceLocation tex = PhotoStore.texture(photoId);
        int texW = PhotoStore.texWidth(photoId);
        int texH = PhotoStore.texHeight(photoId);

        // moldura (vermelha se amaldiçoada)
        int frame = cursed ? 0xFF7A0000 : 0xFFEDEDED;
        g.fill(x - 7, y - 7, x + size + 7, y + size + 7, 0xFF0C0C10);
        g.fill(x - 4, y - 4, x + size + 4, y + size + 4, frame);

        // a foto em si
        RenderSystem.enableBlend();
        g.blit(tex, x, y, size, size, 0F, 0F, texW, texH, texW, texH);
        RenderSystem.disableBlend();

        // corrupção (glitch) nas amaldiçoadas
        if (cursed) {
            long t = System.currentTimeMillis() / 80L;
            for (int i = 0; i < 6; i++) {
                int ly = y + (int) ((t + i * 13L) % size);
                g.fill(x, ly, x + size, ly + 2, (i % 2 == 0) ? 0x99FF0033 : 0x99000000);
            }
            if ((t % 9L) < 3L) g.fill(x, y + size / 3, x + size, y + size / 3 + size / 8, 0x5530FF40);
        }

        // títulos
        g.drawCenteredString(this.font,
                cursed ? "§4§lFotografia Amaldiçoada" : "§7Fotografia",
                this.width / 2, y - 22, 0xFFFFFFFF);
        if (!caption.isEmpty()) {
            g.drawCenteredString(this.font, "§o\"" + caption + "\"", this.width / 2, y + size + 12, 0xFFE0E0E0);
        }
        g.drawCenteredString(this.font, "§8[ESC para fechar]", this.width / 2, y + size + 26, 0xFF808080);

        super.render(g, mouseX, mouseY, partial);
    }

    @Override public boolean isPauseScreen() { return false; }
}
