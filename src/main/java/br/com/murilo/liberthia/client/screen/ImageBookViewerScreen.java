package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.client.texture.ImageUrlTextureCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImageBookViewerScreen extends Screen {

    private final ItemStack bookStack;
    private final List<String> urls = new ArrayList<>();

    private int page = 0;

    private Button previousButton;
    private Button nextButton;

    public ImageBookViewerScreen(ItemStack bookStack) {
        super(getBookTitle(bookStack));
        this.bookStack = bookStack;
        loadUrlsFromStack();
    }

    private static Component getBookTitle(ItemStack stack) {
        if (stack.hasTag() && stack.getOrCreateTag().contains("ImageBookTitle")) {
            String title = stack.getOrCreateTag().getString("ImageBookTitle");

            if (!title.isBlank()) {
                return Component.literal(title);
            }
        }

        if (stack.hasCustomHoverName()) {
            return stack.getHoverName();
        }

        return Component.literal("Livro de Imagens");
    }

    private void loadUrlsFromStack() {
        urls.clear();

        if (!bookStack.hasTag()) {
            return;
        }

        ListTag listTag = bookStack.getOrCreateTag().getList("ImageUrls", Tag.TAG_STRING);

        for (int i = 0; i < listTag.size(); i++) {
            String url = listTag.getString(i);

            if (!url.isBlank()) {
                urls.add(url);
            }
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int bottom = this.height - 36;

        this.previousButton = Button.builder(
                        Component.literal("<"),
                        button -> {
                            if (page > 0) {
                                page--;
                                updateButtons();
                            }
                        }
                )
                .bounds(centerX - 90, bottom, 40, 20)
                .build();

        this.nextButton = Button.builder(
                        Component.literal(">"),
                        button -> {
                            if (page < urls.size() - 1) {
                                page++;
                                updateButtons();
                            }
                        }
                )
                .bounds(centerX + 50, bottom, 40, 20)
                .build();

        this.addRenderableWidget(previousButton);
        this.addRenderableWidget(nextButton);

        updateButtons();
    }

    private void updateButtons() {
        if (previousButton != null) {
            previousButton.active = page > 0;
        }

        if (nextButton != null) {
            nextButton.active = page < urls.size() - 1;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;

        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                centerX,
                15,
                0xFFFFFF
        );

        if (urls.isEmpty()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.literal("Este livro não possui imagens salvas.")
                            .withStyle(ChatFormatting.RED),
                    centerX,
                    this.height / 2,
                    0xFF5555
            );

            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        String currentUrl = urls.get(page);

        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Página " + (page + 1) + " / " + urls.size())
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                centerX,
                32,
                0xFFFFFF
        );

        Optional<ImageUrlTextureCache.LoadedImage> loadedImage = ImageUrlTextureCache.get(currentUrl);

        if (loadedImage.isEmpty()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.literal("Carregando imagem...")
                            .withStyle(ChatFormatting.GRAY),
                    centerX,
                    this.height / 2,
                    0xAAAAAA
            );

            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        ImageUrlTextureCache.LoadedImage image = loadedImage.get();

        int maxWidth = Math.min(360, this.width - 80);
        int maxHeight = Math.min(220, this.height - 110);

        float imageRatio = image.width() / (float) image.height();
        int drawWidth = maxWidth;
        int drawHeight = (int) (drawWidth / imageRatio);

        if (drawHeight > maxHeight) {
            drawHeight = maxHeight;
            drawWidth = (int) (drawHeight * imageRatio);
        }

        int drawX = centerX - drawWidth / 2;
        int drawY = 55;

        guiGraphics.blit(
                image.location(),
                drawX,
                drawY,
                drawWidth,
                drawHeight,
                0,
                0,
                image.width(),
                image.height(),
                image.width(),
                image.height()
        );

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static String shorten(String value, int max) {
        if (value.length() <= max) {
            return value;
        }

        return value.substring(0, max - 3) + "...";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
