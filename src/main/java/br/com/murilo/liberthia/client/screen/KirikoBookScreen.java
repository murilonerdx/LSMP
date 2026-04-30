package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.network.KirikoBookNetworking;
import br.com.murilo.liberthia.packet.KirikoBookTeleportPacket;
import br.com.murilo.liberthia.packet.OpenKirikoBookScreenPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class KirikoBookScreen extends Screen {

    private final List<OpenKirikoBookScreenPacket.DimensionEntry> dimensions;
    private final List<OpenKirikoBookScreenPacket.PlayerEntry> players;

    private int dimensionScroll = 0;
    private int playerScroll = 0;

    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 250;

    private static final int COLUMN_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 5;
    private static final int VISIBLE_ROWS = 5;

    private String selectedDimensionId;
    private double selectedDefaultX;
    private double selectedDefaultY;
    private double selectedDefaultZ;

    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;

    private String lastX = "";
    private String lastY = "";
    private String lastZ = "";

    public KirikoBookScreen(
            List<OpenKirikoBookScreenPacket.DimensionEntry> dimensions,
            List<OpenKirikoBookScreenPacket.PlayerEntry> players
    ) {
        super(Component.literal("Livro Vermelho de Kiriko"));

        this.dimensions = dimensions;
        this.players = players;

        if (!dimensions.isEmpty()) {
            OpenKirikoBookScreenPacket.DimensionEntry first = dimensions.get(0);

            this.selectedDimensionId = first.dimensionId();
            this.selectedDefaultX = first.x();
            this.selectedDefaultY = first.y();
            this.selectedDefaultZ = first.z();

            this.lastX = String.valueOf((int) first.x());
            this.lastY = String.valueOf((int) first.y());
            this.lastZ = String.valueOf((int) first.z());
        } else {
            this.selectedDimensionId = "minecraft:overworld";
            this.selectedDefaultX = 0;
            this.selectedDefaultY = 100;
            this.selectedDefaultZ = 0;

            this.lastX = "0";
            this.lastY = "100";
            this.lastZ = "0";
        }
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    public void rebuildWidgets() {
        saveCurrentTextBoxValues();

        clearWidgets();

        Layout layout = layout();

        buildScrollButtons(layout);
        buildDimensionButtons(layout);
        buildPlayerButtons(layout);
        buildCoordinateInputs(layout);

        addRenderableWidget(Button.builder(
                Component.literal("Fechar"),
                button -> this.minecraft.setScreen(null)
        ).bounds(layout.panelX + PANEL_WIDTH / 2 - 50, layout.panelY + PANEL_HEIGHT - 32, 100, 20).build());
    }

    private Layout layout() {
        int panelX = this.width / 2 - PANEL_WIDTH / 2;
        int panelY = this.height / 2 - PANEL_HEIGHT / 2;

        if (panelY < 10) {
            panelY = 10;
        }

        int titleY = panelY + 14;

        int leftX = panelX + 28;
        int rightX = panelX + PANEL_WIDTH - 28 - COLUMN_WIDTH;

        int headerY = panelY + 48;
        int scrollY = headerY + 16;
        int listY = scrollY + 24;

        int coordinateY = listY + VISIBLE_ROWS * (BUTTON_HEIGHT + ROW_GAP) + 24;

        return new Layout(
                panelX,
                panelY,
                titleY,
                leftX,
                rightX,
                headerY,
                scrollY,
                listY,
                coordinateY
        );
    }

    private void buildScrollButtons(Layout layout) {
        addRenderableWidget(Button.builder(
                Component.literal("▲"),
                button -> {
                    if (dimensionScroll > 0) {
                        dimensionScroll--;
                        rebuildWidgets();
                    }
                }
        ).bounds(layout.leftX, layout.scrollY, 34, 18).build());

        addRenderableWidget(Button.builder(
                Component.literal("▼"),
                button -> {
                    int max = Math.max(0, dimensions.size() - VISIBLE_ROWS);

                    if (dimensionScroll < max) {
                        dimensionScroll++;
                        rebuildWidgets();
                    }
                }
        ).bounds(layout.leftX + 40, layout.scrollY, 34, 18).build());

        addRenderableWidget(Button.builder(
                Component.literal("▲"),
                button -> {
                    if (playerScroll > 0) {
                        playerScroll--;
                        rebuildWidgets();
                    }
                }
        ).bounds(layout.rightX, layout.scrollY, 34, 18).build());

        addRenderableWidget(Button.builder(
                Component.literal("▼"),
                button -> {
                    int max = Math.max(0, players.size() - VISIBLE_ROWS);

                    if (playerScroll < max) {
                        playerScroll++;
                        rebuildWidgets();
                    }
                }
        ).bounds(layout.rightX + 40, layout.scrollY, 34, 18).build());
    }

    private void buildDimensionButtons(Layout layout) {
        int dimensionEnd = Math.min(dimensions.size(), dimensionScroll + VISIBLE_ROWS);

        for (int i = dimensionScroll; i < dimensionEnd; i++) {
            OpenKirikoBookScreenPacket.DimensionEntry entry = dimensions.get(i);

            int row = i - dimensionScroll;
            int y = layout.listY + row * (BUTTON_HEIGHT + ROW_GAP);

            boolean selected = entry.dimensionId().equals(selectedDimensionId);

            String label = (selected ? "➤ " : "")
                    + shortDimensionName(entry.dimensionId())
                    + "  "
                    + (int) entry.x()
                    + ", "
                    + (int) entry.y()
                    + ", "
                    + (int) entry.z();

            addRenderableWidget(Button.builder(
                    Component.literal(label),
                    button -> {
                        selectedDimensionId = entry.dimensionId();

                        selectedDefaultX = entry.x();
                        selectedDefaultY = entry.y();
                        selectedDefaultZ = entry.z();

                        lastX = String.valueOf((int) entry.x());
                        lastY = String.valueOf((int) entry.y());
                        lastZ = String.valueOf((int) entry.z());

                        setCoordinateValues(lastX, lastY, lastZ);
                    }
            ).bounds(layout.leftX, y, COLUMN_WIDTH, BUTTON_HEIGHT).build());
        }
    }

    private void buildPlayerButtons(Layout layout) {
        int playerEnd = Math.min(players.size(), playerScroll + VISIBLE_ROWS);

        for (int i = playerScroll; i < playerEnd; i++) {
            OpenKirikoBookScreenPacket.PlayerEntry entry = players.get(i);

            int row = i - playerScroll;
            int y = layout.listY + row * (BUTTON_HEIGHT + ROW_GAP);

            addRenderableWidget(Button.builder(
                    Component.literal(entry.name()),
                    button -> {
                        KirikoBookNetworking.CHANNEL.sendToServer(
                                new KirikoBookTeleportPacket(
                                        KirikoBookTeleportPacket.TargetType.PLAYER,
                                        entry.name(),
                                        0,
                                        0,
                                        0
                                )
                        );

                        this.minecraft.setScreen(null);
                    }
            ).bounds(layout.rightX, y, COLUMN_WIDTH, BUTTON_HEIGHT).build());
        }
    }

    private void buildCoordinateInputs(Layout layout) {
        int labelY = layout.coordinateY;
        int inputY = labelY + 26;
        int buttonY = inputY + 28;

        int inputWidth = 74;
        int inputGap = 10;

        int inputStartX = layout.leftX;

        this.xBox = new EditBox(this.font, inputStartX, inputY, inputWidth, 18, Component.literal("X"));
        this.yBox = new EditBox(this.font, inputStartX + inputWidth + inputGap, inputY, inputWidth, 18, Component.literal("Y"));
        this.zBox = new EditBox(this.font, inputStartX + (inputWidth + inputGap) * 2, inputY, inputWidth, 18, Component.literal("Z"));

        this.xBox.setMaxLength(12);
        this.yBox.setMaxLength(12);
        this.zBox.setMaxLength(12);

        this.xBox.setFilter(this::isCoordinateTextValid);
        this.yBox.setFilter(this::isCoordinateTextValid);
        this.zBox.setFilter(this::isCoordinateTextValid);

        setCoordinateValues(lastX, lastY, lastZ);

        addRenderableWidget(this.xBox);
        addRenderableWidget(this.yBox);
        addRenderableWidget(this.zBox);

        addRenderableWidget(Button.builder(
                Component.literal("Escolher coordenadas"),
                button -> teleportToTypedCoordinates()
        ).bounds(layout.leftX, buttonY, 145, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Usar padrão"),
                button -> {
                    lastX = String.valueOf((int) selectedDefaultX);
                    lastY = String.valueOf((int) selectedDefaultY);
                    lastZ = String.valueOf((int) selectedDefaultZ);

                    setCoordinateValues(lastX, lastY, lastZ);
                    teleportToTypedCoordinates();
                }
        ).bounds(layout.leftX + 155, buttonY, 145, 20).build());
    }

    private void teleportToTypedCoordinates() {
        saveCurrentTextBoxValues();

        double x = parseCoordinate(lastX, selectedDefaultX);
        double y = parseCoordinate(lastY, selectedDefaultY);
        double z = parseCoordinate(lastZ, selectedDefaultZ);

        KirikoBookNetworking.CHANNEL.sendToServer(
                new KirikoBookTeleportPacket(
                        KirikoBookTeleportPacket.TargetType.DIMENSION,
                        selectedDimensionId,
                        x,
                        y,
                        z
                )
        );

        this.minecraft.setScreen(null);
    }

    private void saveCurrentTextBoxValues() {
        if (xBox != null) {
            lastX = xBox.getValue();
        }

        if (yBox != null) {
            lastY = yBox.getValue();
        }

        if (zBox != null) {
            lastZ = zBox.getValue();
        }
    }

    private void setCoordinateValues(String x, String y, String z) {
        if (xBox != null) {
            xBox.setValue(x);
        }

        if (yBox != null) {
            yBox.setValue(y);
        }

        if (zBox != null) {
            zBox.setValue(z);
        }
    }

    private boolean isCoordinateTextValid(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        if (value.equals("-")) {
            return true;
        }

        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private double parseCoordinate(String value, double fallback) {
        if (value == null || value.isBlank() || value.equals("-")) {
            return fallback;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String shortDimensionName(String id) {
        if (id == null) {
            return "unknown";
        }

        int index = id.indexOf(":");

        if (index >= 0 && index + 1 < id.length()) {
            return id.substring(index + 1);
        }

        return id;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = layout();

        int listBottomY = layout.listY + VISIBLE_ROWS * (BUTTON_HEIGHT + ROW_GAP);

        if (mouseY >= layout.listY && mouseY <= listBottomY) {
            if (mouseX >= layout.leftX && mouseX <= layout.leftX + COLUMN_WIDTH) {
                if (delta > 0 && dimensionScroll > 0) {
                    dimensionScroll--;
                } else if (delta < 0) {
                    dimensionScroll = Math.min(
                            Math.max(0, dimensions.size() - VISIBLE_ROWS),
                            dimensionScroll + 1
                    );
                }

                rebuildWidgets();
                return true;
            }

            if (mouseX >= layout.rightX && mouseX <= layout.rightX + COLUMN_WIDTH) {
                if (delta > 0 && playerScroll > 0) {
                    playerScroll--;
                } else if (delta < 0) {
                    playerScroll = Math.min(
                            Math.max(0, players.size() - VISIBLE_ROWS),
                            playerScroll + 1
                    );
                }

                rebuildWidgets();
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        Layout layout = layout();

        graphics.fill(
                layout.panelX,
                layout.panelY,
                layout.panelX + PANEL_WIDTH,
                layout.panelY + PANEL_HEIGHT,
                0xAA120010
        );

        graphics.fill(
                layout.panelX + 2,
                layout.panelY + 2,
                layout.panelX + PANEL_WIDTH - 2,
                layout.panelY + PANEL_HEIGHT - 2,
                0xAA220018
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal("Livro Vermelho de Kiriko")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                layout.panelX + PANEL_WIDTH / 2,
                layout.titleY,
                0xFFFFFF
        );

        graphics.drawString(
                this.font,
                Component.literal("Dimensões")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                layout.leftX,
                layout.headerY,
                0xFFFFFF
        );

        graphics.drawString(
                this.font,
                Component.literal("Players Online")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                layout.rightX,
                layout.headerY,
                0xFFFFFF
        );

        graphics.drawString(
                this.font,
                Component.literal("Selecionada: " + shortDimensionName(selectedDimensionId))
                        .withStyle(ChatFormatting.GRAY),
                layout.leftX,
                layout.coordinateY,
                0xAAAAAA
        );

        graphics.drawString(
                this.font,
                Component.literal("Coordenadas manuais")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                layout.leftX,
                layout.coordinateY + 12,
                0xFFFFFF
        );

        int inputLabelY = layout.coordinateY + 50;

        graphics.drawString(this.font, Component.literal("X"), layout.leftX, inputLabelY, 0xDDDDDD);
        graphics.drawString(this.font, Component.literal("Y"), layout.leftX + 84, inputLabelY, 0xDDDDDD);
        graphics.drawString(this.font, Component.literal("Z"), layout.leftX + 168, inputLabelY, 0xDDDDDD);

        graphics.drawString(
                this.font,
                Component.literal("Nomes iniciados com LNPC não aparecem.")
                        .withStyle(ChatFormatting.DARK_GRAY),
                layout.rightX,
                layout.coordinateY,
                0xAAAAAA
        );

        if (dimensions.isEmpty()) {
            graphics.drawString(
                    this.font,
                    Component.literal("Nenhuma dimensão disponível.")
                            .withStyle(ChatFormatting.GRAY),
                    layout.leftX,
                    layout.listY,
                    0xAAAAAA
            );
        }

        if (players.isEmpty()) {
            graphics.drawString(
                    this.font,
                    Component.literal("Nenhum player disponível.")
                            .withStyle(ChatFormatting.GRAY),
                    layout.rightX,
                    layout.listY,
                    0xAAAAAA
            );
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Layout(
            int panelX,
            int panelY,
            int titleY,
            int leftX,
            int rightX,
            int headerY,
            int scrollY,
            int listY,
            int coordinateY
    ) {
    }
}