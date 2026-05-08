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

/**
 * Livro Vermelho de Kiriko — fully responsive layout.
 *
 * <p>The panel is sized as {@code min(MAX_W, screenW - 32) × min(MAX_H, screenH - 16)}
 * and centred. The list area scales to whatever vertical space remains after
 * reserving fixed regions (header, coord section, close), so the GUI never
 * overflows even on GUI scale 4.
 *
 * <p>Sections, top to bottom:
 * <ol>
 *   <li>Title — 14 px</li>
 *   <li>Column headers ("Dimensões" / "Players Online") — 16 px</li>
 *   <li>Scroll buttons (▲▼) — 22 px</li>
 *   <li>List rows (dynamic; 22 px each)</li>
 *   <li>Coord section — fixed 84 px (selected line + X/Y/Z inputs + 2 action buttons)</li>
 *   <li>Close button — 24 px</li>
 * </ol>
 */
public class KirikoBookScreen extends Screen {

    private final List<OpenKirikoBookScreenPacket.DimensionEntry> dimensions;
    private final List<OpenKirikoBookScreenPacket.PlayerEntry> players;

    private int dimensionScroll = 0;
    private int playerScroll = 0;

    private static final int MAX_PANEL_WIDTH  = 360;
    private static final int MAX_PANEL_HEIGHT = 280;
    private static final int MIN_PANEL_HEIGHT = 200;

    private static final int TITLE_H        = 14;
    private static final int HEADERS_H      = 16;
    private static final int SCROLL_H       = 22;
    private static final int ROW_H          = 22;
    private static final int COORD_BLOCK_H  = 84;
    private static final int CLOSE_H        = 24;
    private static final int OUTER_PADDING  = 10;
    private static final int COLUMN_GAP     = 8;

    private String selectedDimensionId;
    private double selectedDefaultX;
    private double selectedDefaultY;
    private double selectedDefaultZ;

    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;

    private String lastX = "0";
    private String lastY = "100";
    private String lastZ = "0";

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
        }
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    public void rebuildWidgets() {
        saveCurrentTextBoxValues();
        clearWidgets();

        Layout L = layout();
        buildScrollButtons(L);
        buildDimensionButtons(L);
        buildPlayerButtons(L);
        buildCoordinateInputs(L);
        buildCloseButton(L);
    }

    private Layout layout() {
        int panelW = Math.min(MAX_PANEL_WIDTH, this.width - 32);
        int panelH = Math.max(MIN_PANEL_HEIGHT, Math.min(MAX_PANEL_HEIGHT, this.height - 16));
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        if (panelY < 4) panelY = 4;

        int titleY = panelY + 8;
        int headerY = titleY + TITLE_H;
        int scrollY = headerY + HEADERS_H;
        int listY = scrollY + SCROLL_H;

        int closeY = panelY + panelH - CLOSE_H + 2;
        int coordY = closeY - COORD_BLOCK_H;

        int listEndY = coordY - 4;
        int listH = Math.max(ROW_H, listEndY - listY);
        int visibleRows = Math.max(1, listH / ROW_H);

        int innerW = panelW - 2 * OUTER_PADDING - COLUMN_GAP;
        int columnW = innerW / 2;
        int leftX = panelX + OUTER_PADDING;
        int rightX = leftX + columnW + COLUMN_GAP;

        return new Layout(panelX, panelY, panelW, panelH,
                titleY, headerY, scrollY, listY,
                coordY, closeY, visibleRows,
                leftX, rightX, columnW);
    }

    private void buildScrollButtons(Layout L) {
        int arrowW = 16, arrowH = 18, gap = 4;
        int pairWidth = arrowW * 2 + gap;
        int leftPairX  = L.leftX + (L.columnW - pairWidth) / 2;
        int rightPairX = L.rightX + (L.columnW - pairWidth) / 2;

        addRenderableWidget(Button.builder(Component.literal("▲"),
                b -> { if (dimensionScroll > 0) { dimensionScroll--; rebuildWidgets(); } })
                .bounds(leftPairX, L.scrollY, arrowW, arrowH).build());
        addRenderableWidget(Button.builder(Component.literal("▼"),
                b -> {
                    int max = Math.max(0, dimensions.size() - L.visibleRows);
                    if (dimensionScroll < max) { dimensionScroll++; rebuildWidgets(); }
                })
                .bounds(leftPairX + arrowW + gap, L.scrollY, arrowW, arrowH).build());

        addRenderableWidget(Button.builder(Component.literal("▲"),
                b -> { if (playerScroll > 0) { playerScroll--; rebuildWidgets(); } })
                .bounds(rightPairX, L.scrollY, arrowW, arrowH).build());
        addRenderableWidget(Button.builder(Component.literal("▼"),
                b -> {
                    int max = Math.max(0, players.size() - L.visibleRows);
                    if (playerScroll < max) { playerScroll++; rebuildWidgets(); }
                })
                .bounds(rightPairX + arrowW + gap, L.scrollY, arrowW, arrowH).build());
    }

    private void buildDimensionButtons(Layout L) {
        int end = Math.min(dimensions.size(), dimensionScroll + L.visibleRows);
        for (int i = dimensionScroll; i < end; i++) {
            OpenKirikoBookScreenPacket.DimensionEntry e = dimensions.get(i);
            int row = i - dimensionScroll;
            int y = L.listY + row * ROW_H;
            boolean selected = e.dimensionId().equals(selectedDimensionId);
            String label = (selected ? "▶ " : "  ") + shortDimensionName(e.dimensionId())
                    + "  " + (int) e.x() + "," + (int) e.y() + "," + (int) e.z();
            addRenderableWidget(Button.builder(
                    Component.literal(truncateToFit(label, L.columnW - 8)),
                    btn -> {
                        selectedDimensionId = e.dimensionId();
                        selectedDefaultX = e.x();
                        selectedDefaultY = e.y();
                        selectedDefaultZ = e.z();
                        lastX = String.valueOf((int) e.x());
                        lastY = String.valueOf((int) e.y());
                        lastZ = String.valueOf((int) e.z());
                        setCoordinateValues(lastX, lastY, lastZ);
                    }
            ).bounds(L.leftX, y, L.columnW, ROW_H - 2).build());
        }
    }

    private void buildPlayerButtons(Layout L) {
        int end = Math.min(players.size(), playerScroll + L.visibleRows);
        for (int i = playerScroll; i < end; i++) {
            OpenKirikoBookScreenPacket.PlayerEntry e = players.get(i);
            int row = i - playerScroll;
            int y = L.listY + row * ROW_H;
            addRenderableWidget(Button.builder(
                    Component.literal(truncateToFit("→ " + e.name(), L.columnW - 8)),
                    btn -> {
                        KirikoBookNetworking.CHANNEL.sendToServer(
                                new KirikoBookTeleportPacket(
                                        KirikoBookTeleportPacket.TargetType.PLAYER,
                                        e.name(), 0, 0, 0));
                        this.minecraft.setScreen(null);
                    }
            ).bounds(L.rightX, y, L.columnW, ROW_H - 2).build());
        }
    }

    private void buildCoordinateInputs(Layout L) {
        int sectionY = L.coordY;
        int inputW = (L.panelW - 2 * OUTER_PADDING - 16) / 3;
        int xX = L.panelX + OUTER_PADDING;
        int yX = xX + inputW + 8;
        int zX = yX + inputW + 8;
        int inputY = sectionY + 26;

        this.xBox = new EditBox(this.font, xX, inputY, inputW, 18, Component.literal("X"));
        this.yBox = new EditBox(this.font, yX, inputY, inputW, 18, Component.literal("Y"));
        this.zBox = new EditBox(this.font, zX, inputY, inputW, 18, Component.literal("Z"));
        for (EditBox e : new EditBox[]{xBox, yBox, zBox}) {
            e.setMaxLength(12);
            e.setFilter(this::isCoordinateTextValid);
        }
        setCoordinateValues(lastX, lastY, lastZ);
        addRenderableWidget(this.xBox);
        addRenderableWidget(this.yBox);
        addRenderableWidget(this.zBox);

        int btnY = sectionY + 52;
        int btnW = (L.panelW - 2 * OUTER_PADDING - 8) / 2;
        addRenderableWidget(Button.builder(
                Component.literal("Escolher coordenadas"),
                b -> teleportToTypedCoordinates()
        ).bounds(L.panelX + OUTER_PADDING, btnY, btnW, 20).build());
        addRenderableWidget(Button.builder(
                Component.literal("Usar padrão"),
                b -> {
                    lastX = String.valueOf((int) selectedDefaultX);
                    lastY = String.valueOf((int) selectedDefaultY);
                    lastZ = String.valueOf((int) selectedDefaultZ);
                    setCoordinateValues(lastX, lastY, lastZ);
                    teleportToTypedCoordinates();
                }
        ).bounds(L.panelX + OUTER_PADDING + btnW + 8, btnY, btnW, 20).build());
    }

    private void buildCloseButton(Layout L) {
        int btnW = 100;
        addRenderableWidget(Button.builder(
                Component.literal("Fechar"),
                b -> this.minecraft.setScreen(null)
        ).bounds(L.panelX + (L.panelW - btnW) / 2, L.closeY, btnW, 20).build());
    }

    private void teleportToTypedCoordinates() {
        saveCurrentTextBoxValues();
        double x = parseCoordinate(lastX, selectedDefaultX);
        double y = parseCoordinate(lastY, selectedDefaultY);
        double z = parseCoordinate(lastZ, selectedDefaultZ);
        KirikoBookNetworking.CHANNEL.sendToServer(
                new KirikoBookTeleportPacket(
                        KirikoBookTeleportPacket.TargetType.DIMENSION,
                        selectedDimensionId, x, y, z));
        this.minecraft.setScreen(null);
    }

    private void saveCurrentTextBoxValues() {
        if (xBox != null) lastX = xBox.getValue();
        if (yBox != null) lastY = yBox.getValue();
        if (zBox != null) lastZ = zBox.getValue();
    }

    private void setCoordinateValues(String x, String y, String z) {
        if (xBox != null) xBox.setValue(x);
        if (yBox != null) yBox.setValue(y);
        if (zBox != null) zBox.setValue(z);
    }

    private boolean isCoordinateTextValid(String value) {
        if (value == null || value.isEmpty() || value.equals("-")) return true;
        try { Double.parseDouble(value); return true; } catch (NumberFormatException e) { return false; }
    }

    private double parseCoordinate(String value, double fallback) {
        if (value == null || value.isBlank() || value.equals("-")) return fallback;
        try { return Double.parseDouble(value); } catch (NumberFormatException e) { return fallback; }
    }

    private String shortDimensionName(String id) {
        if (id == null) return "?";
        int i = id.indexOf(':');
        return (i >= 0 && i + 1 < id.length()) ? id.substring(i + 1) : id;
    }

    private String truncateToFit(String text, int pixelW) {
        if (this.font.width(text) <= pixelW) return text;
        String ell = "…";
        while (text.length() > 1 && this.font.width(text + ell) > pixelW) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ell;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout L = layout();
        int listEndY = L.listY + L.visibleRows * ROW_H;
        if (mouseY >= L.listY && mouseY <= listEndY) {
            if (mouseX >= L.leftX && mouseX <= L.leftX + L.columnW) {
                if (delta > 0 && dimensionScroll > 0) dimensionScroll--;
                else if (delta < 0)
                    dimensionScroll = Math.min(Math.max(0, dimensions.size() - L.visibleRows),
                            dimensionScroll + 1);
                rebuildWidgets();
                return true;
            }
            if (mouseX >= L.rightX && mouseX <= L.rightX + L.columnW) {
                if (delta > 0 && playerScroll > 0) playerScroll--;
                else if (delta < 0)
                    playerScroll = Math.min(Math.max(0, players.size() - L.visibleRows),
                            playerScroll + 1);
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        Layout L = layout();

        // Panel background — solid wine-red with a thin border.
        g.fill(L.panelX, L.panelY, L.panelX + L.panelW, L.panelY + L.panelH, 0xEE0E0010);
        g.fill(L.panelX + 1, L.panelY + 1, L.panelX + L.panelW - 1, L.panelY + L.panelH - 1, 0xEE220018);
        int borderColor = 0xFF8B0000;
        g.fill(L.panelX, L.panelY, L.panelX + L.panelW, L.panelY + 1, borderColor);
        g.fill(L.panelX, L.panelY + L.panelH - 1, L.panelX + L.panelW, L.panelY + L.panelH, borderColor);
        g.fill(L.panelX, L.panelY, L.panelX + 1, L.panelY + L.panelH, borderColor);
        g.fill(L.panelX + L.panelW - 1, L.panelY, L.panelX + L.panelW, L.panelY + L.panelH, borderColor);

        // Title.
        g.drawCenteredString(this.font,
                Component.literal("Livro Vermelho de Kiriko")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                L.panelX + L.panelW / 2, L.titleY, 0xFFFFFF);

        // Column headers.
        g.drawString(this.font,
                Component.literal("Dimensões").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                L.leftX, L.headerY, 0xFFFFFF, false);
        g.drawString(this.font,
                Component.literal("Players Online").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                L.rightX, L.headerY, 0xFFFFFF, false);

        // Empty-state hint.
        if (dimensions.isEmpty()) {
            g.drawString(this.font, "Nenhuma dimensão.", L.leftX, L.listY + 4, 0xAAAAAA, false);
        }
        if (players.isEmpty()) {
            g.drawString(this.font, "Nenhum player.", L.rightX, L.listY + 4, 0xAAAAAA, false);
        }

        // Coord section header.
        g.drawString(this.font,
                Component.literal("Selecionada: " + shortDimensionName(selectedDimensionId))
                        .withStyle(ChatFormatting.GOLD),
                L.panelX + OUTER_PADDING, L.coordY + 2, 0xFFD700, false);

        // X/Y/Z labels above their input boxes.
        int inputW = (L.panelW - 2 * OUTER_PADDING - 16) / 3;
        int xX = L.panelX + OUTER_PADDING;
        int yX = xX + inputW + 8;
        int zX = yX + inputW + 8;
        g.drawString(this.font, "X", xX + inputW / 2 - 3, L.coordY + 16, 0xDDDDDD, false);
        g.drawString(this.font, "Y", yX + inputW / 2 - 3, L.coordY + 16, 0xDDDDDD, false);
        g.drawString(this.font, "Z", zX + inputW / 2 - 3, L.coordY + 16, 0xDDDDDD, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record Layout(
            int panelX, int panelY, int panelW, int panelH,
            int titleY, int headerY, int scrollY, int listY,
            int coordY, int closeY, int visibleRows,
            int leftX, int rightX, int columnW
    ) {}
}
