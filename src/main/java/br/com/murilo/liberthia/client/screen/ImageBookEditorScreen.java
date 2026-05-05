package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.packet.CreateImageBookPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageBookEditorScreen extends Screen {

    private static final int MAX_IMAGES = 50;

    private final List<String> urls = new ArrayList<>();

    private EditBox titleInput;
    private EditBox urlInput;

    private Button addButton;
    private Button removeButton;
    private Button moveUpButton;
    private Button moveDownButton;
    private Button clearButton;
    private Button confirmButton;

    private int selectedIndex = -1;

    public ImageBookEditorScreen() {
        super(Component.literal("Criador de Livro com Imagens"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = 32;

        this.titleInput = new EditBox(
                this.font,
                centerX - 170,
                top,
                340,
                20,
                Component.literal("Nome do livro")
        );
        this.titleInput.setMaxLength(64);
        this.titleInput.setValue("Livro de Imagens");
        this.titleInput.setHint(Component.literal("Nome do livro"));

        this.urlInput = new EditBox(
                this.font,
                centerX - 170,
                top + 35,
                340,
                20,
                Component.literal("Link da imagem")
        );
        this.urlInput.setMaxLength(2048);
        this.urlInput.setHint(Component.literal("Cole um link .png, .jpg, .jpeg ou .webp"));

        this.addRenderableWidget(this.titleInput);
        this.addRenderableWidget(this.urlInput);

        this.addButton = Button.builder(
                        Component.literal("Adicionar página"),
                        button -> addCurrentUrl()
                )
                .bounds(centerX - 170, top + 63, 120, 20)
                .build();

        this.moveUpButton = Button.builder(
                        Component.literal("Subir"),
                        button -> moveSelectedUp()
                )
                .bounds(centerX - 45, top + 63, 55, 20)
                .build();

        this.moveDownButton = Button.builder(
                        Component.literal("Descer"),
                        button -> moveSelectedDown()
                )
                .bounds(centerX + 15, top + 63, 60, 20)
                .build();

        this.removeButton = Button.builder(
                        Component.literal("Remover"),
                        button -> removeSelected()
                )
                .bounds(centerX + 80, top + 63, 70, 20)
                .build();

        this.clearButton = Button.builder(
                        Component.literal("Limpar"),
                        button -> {
                            urls.clear();
                            selectedIndex = -1;
                            updateButtons();
                        }
                )
                .bounds(centerX + 155, top + 63, 65, 20)
                .build();

        this.confirmButton = Button.builder(
                        Component.literal("Confirmar livro"),
                        button -> confirmBook()
                )
                .bounds(centerX - 70, this.height - 32, 140, 20)
                .build();

        this.addRenderableWidget(addButton);
        this.addRenderableWidget(moveUpButton);
        this.addRenderableWidget(moveDownButton);
        this.addRenderableWidget(removeButton);
        this.addRenderableWidget(clearButton);
        this.addRenderableWidget(confirmButton);

        this.setInitialFocus(this.urlInput);

        updateButtons();
    }

    private void addCurrentUrl() {
        String value = this.urlInput.getValue().trim();

        if (value.isBlank()) {
            return;
        }

        if (urls.size() >= MAX_IMAGES) {
            return;
        }

        urls.add(value);
        selectedIndex = urls.size() - 1;

        this.urlInput.setValue("");
        updateButtons();
    }

    private void removeSelected() {
        if (!isValidSelectedIndex()) {
            return;
        }

        urls.remove(selectedIndex);

        if (urls.isEmpty()) {
            selectedIndex = -1;
        } else if (selectedIndex >= urls.size()) {
            selectedIndex = urls.size() - 1;
        }

        updateButtons();
    }

    private void moveSelectedUp() {
        if (!isValidSelectedIndex()) {
            return;
        }

        if (selectedIndex <= 0) {
            return;
        }

        Collections.swap(urls, selectedIndex, selectedIndex - 1);
        selectedIndex--;

        updateButtons();
    }

    private void moveSelectedDown() {
        if (!isValidSelectedIndex()) {
            return;
        }

        if (selectedIndex >= urls.size() - 1) {
            return;
        }

        Collections.swap(urls, selectedIndex, selectedIndex + 1);
        selectedIndex++;

        updateButtons();
    }

    private void confirmBook() {
        if (urls.isEmpty()) {
            return;
        }

        String title = this.titleInput.getValue().trim();

        if (title.isBlank()) {
            title = "Livro de Imagens";
        }

        ModNetwork.CHANNEL.sendToServer(new CreateImageBookPacket(title, new ArrayList<>(urls)));

        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private boolean isValidSelectedIndex() {
        return selectedIndex >= 0 && selectedIndex < urls.size();
    }

    private void updateButtons() {
        if (removeButton != null) {
            removeButton.active = isValidSelectedIndex();
        }

        if (moveUpButton != null) {
            moveUpButton.active = isValidSelectedIndex() && selectedIndex > 0;
        }

        if (moveDownButton != null) {
            moveDownButton.active = isValidSelectedIndex() && selectedIndex < urls.size() - 1;
        }

        if (clearButton != null) {
            clearButton.active = !urls.isEmpty();
        }

        if (confirmButton != null) {
            confirmButton.active = !urls.isEmpty();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int listX = centerX - 170;
        int listY = 135;
        int rowHeight = 14;
        int visible = Math.min(urls.size(), getMaxVisibleRows());

        for (int i = 0; i < visible; i++) {
            int rowY = listY + i * rowHeight;

            boolean insideX = mouseX >= listX && mouseX <= listX + 340;
            boolean insideY = mouseY >= rowY && mouseY <= rowY + rowHeight;

            if (insideX && insideY) {
                selectedIndex = i;
                updateButtons();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean enter = keyCode == 257 || keyCode == 335;

        if (enter && this.getFocused() == this.urlInput) {
            addCurrentUrl();
            return true;
        }

        boolean upArrow = keyCode == 265;
        boolean downArrow = keyCode == 264;
        boolean delete = keyCode == 261;

        if (upArrow && isValidSelectedIndex()) {
            selectedIndex = Math.max(0, selectedIndex - 1);
            updateButtons();
            return true;
        }

        if (downArrow && isValidSelectedIndex()) {
            selectedIndex = Math.min(urls.size() - 1, selectedIndex + 1);
            updateButtons();
            return true;
        }

        if (delete && isValidSelectedIndex()) {
            removeSelected();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;

        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                centerX,
                12,
                0xFFFFFF
        );

        guiGraphics.drawString(
                this.font,
                Component.literal("Nome do livro")
                        .withStyle(ChatFormatting.GRAY),
                centerX - 170,
                22,
                0xAAAAAA,
                false
        );

        guiGraphics.drawString(
                this.font,
                Component.literal("Link da imagem")
                        .withStyle(ChatFormatting.GRAY),
                centerX - 170,
                57,
                0xAAAAAA,
                false
        );

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderPageList(guiGraphics, centerX);

        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Clique em uma página para selecionar. ENTER adiciona. DEL remove.")
                        .withStyle(ChatFormatting.DARK_GRAY),
                centerX,
                this.height - 50,
                0x777777
        );
    }

    private void renderPageList(GuiGraphics guiGraphics, int centerX) {
        int listX = centerX - 170;
        int listY = 135;
        int rowHeight = 14;
        int listWidth = 340;

        guiGraphics.drawString(
                this.font,
                Component.literal("Páginas: " + urls.size() + "/" + MAX_IMAGES)
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                listX,
                listY - 16,
                0xFFFFFF,
                false
        );

        if (urls.isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.literal("Nenhuma página adicionada.")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    listX,
                    listY,
                    0x888888,
                    false
            );
            return;
        }

        int visible = Math.min(urls.size(), getMaxVisibleRows());

        for (int i = 0; i < visible; i++) {
            int rowY = listY + i * rowHeight;

            if (i == selectedIndex) {
                guiGraphics.fill(listX - 3, rowY - 2, listX + listWidth, rowY + rowHeight, 0x663B1F5C);
            }

            String text = (i + 1) + ". Página " + (i + 1);

            guiGraphics.drawString(
                    this.font,
                    Component.literal(text)
                            .withStyle(i == selectedIndex ? ChatFormatting.YELLOW : ChatFormatting.WHITE),
                    listX,
                    rowY,
                    0xDDDDDD,
                    false
            );
        }

        if (urls.size() > visible) {
            guiGraphics.drawString(
                    this.font,
                    Component.literal("... +" + (urls.size() - visible) + " página(s)")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    listX,
                    listY + visible * rowHeight + 4,
                    0x888888,
                    false
            );
        }
    }

    private int getMaxVisibleRows() {
        int availableHeight = this.height - 190;
        return Math.max(4, availableHeight / 14);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}