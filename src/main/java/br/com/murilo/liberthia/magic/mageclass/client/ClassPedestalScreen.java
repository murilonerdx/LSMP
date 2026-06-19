package br.com.murilo.liberthia.magic.mageclass.client;

import br.com.murilo.liberthia.magic.mageclass.MageClass;
import br.com.murilo.liberthia.magic.mageclass.SelectClassC2SPacket;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * r164: <b>ClassPedestalScreen</b> — UI grid com as 13 classes de mago.
 *
 * <p>Substitui o chat-menu antigo (que dava trabalho ler). Layout:
 * <ul>
 *   <li>Header com classe atual + level</li>
 *   <li>Grid 3 colunas × 5 linhas (15 slots, 13 classes ocupam)</li>
 *   <li>Cada card mostra: cor + nome + base damage + step</li>
 *   <li>Click no card seleciona via packet → fecha auto</li>
 *   <li>Botão "Remover Classe" pra limpar</li>
 *   <li>ESC fecha sem mudar</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class ClassPedestalScreen extends Screen {

    private final String currentClassName;
    private final int currentLevel;
    private int hoveredIndex = -1;

    private static final int CARD_W = 110;
    private static final int CARD_H = 28;
    private static final int CARD_GAP = 4;
    private static final int COLS = 3;

    public ClassPedestalScreen(String currentClass, int level) {
        super(Component.literal("Pedestal das Classes"));
        this.currentClassName = currentClass;
        this.currentLevel = level;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private int rowCount() {
        return (MageClass.values().length + COLS - 1) / COLS;
    }

    /** Topo do bloco de conteúdo, centralizado verticalmente na tela.
     *  Inclui header + grid + botão + linha de ESC (tudo num bloco só, nada
     *  fixado no rodapé — assim não estoura nem sobrepõe). */
    private int contentTop() {
        int contentH = 24 + rowCount() * (CARD_H + CARD_GAP) + 24 + 14;
        return Math.max(4, (this.height - contentH) / 2);
    }

    /** Y onde o grid de cards começa (abaixo do header). */
    private int gridTop() {
        return contentTop() + 24;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Background semi-opaque
        g.fillGradient(0, 0, width, height, 0xCC000020, 0xEE100020);

        // Header
        String title = "§l§dPedestal das Classes Mágicas";
        int tw = font.width(title.replaceAll("§.", ""));
        g.drawString(font, title, (width - tw) / 2, contentTop(), 0xFFFFFFFF, true);

        // Subheader — classe atual
        String sub;
        if (currentClassName.isEmpty()) {
            sub = "§7Atual: §8nenhuma classe escolhida";
        } else {
            MageClass cur = safeValue(currentClassName);
            if (cur != null) {
                sub = "§7Atual: " + cur.colorCode + "§l" + cur.displayName + "§r §7lv §e" + currentLevel
                        + "§7/10 (§a+" + cur.dmgBonusAt(currentLevel) + "%§7 dano)";
            } else {
                sub = "§7Atual: §8" + currentClassName;
            }
        }
        int sw = font.width(sub.replaceAll("§.", ""));
        g.drawString(font, sub, (width - sw) / 2, contentTop() + 12, 0xFFCCCCCC, true);

        // Grid das 13 classes (3 colunas)
        MageClass[] classes = MageClass.values();
        int gridStartX = (width - (COLS * CARD_W + (COLS - 1) * CARD_GAP)) / 2;
        int gridStartY = gridTop();

        hoveredIndex = -1;
        for (int i = 0; i < classes.length; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = gridStartX + col * (CARD_W + CARD_GAP);
            int cy = gridStartY + row * (CARD_H + CARD_GAP);

            boolean hovered = mouseX >= cx && mouseX <= cx + CARD_W
                          && mouseY >= cy && mouseY <= cy + CARD_H;
            if (hovered) hoveredIndex = i;
            boolean isSelected = !currentClassName.isEmpty() && classes[i].name().equals(currentClassName);

            drawClassCard(g, cx, cy, classes[i], hovered, isSelected);
        }

        // Remove class button (centralizado abaixo do grid)
        int rows = (classes.length + COLS - 1) / COLS;
        int btnY = gridStartY + rows * (CARD_H + CARD_GAP) + 4;
        int btnW = 160, btnH = 18;
        int btnX = (width - btnW) / 2;
        boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW
                         && mouseY >= btnY && mouseY <= btnY + btnH;
        g.fill(btnX, btnY, btnX + btnW, btnY + btnH,
                btnHovered ? 0xFFAA3333 : 0xCC551111);
        g.fill(btnX, btnY, btnX + btnW, btnY + 1, 0xFFFF6666);
        g.fill(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, 0xFFFF6666);
        String rmText = "§c✗ Remover Classe";
        int rmw = font.width("✗ Remover Classe");
        g.drawString(font, rmText, btnX + (btnW - rmw) / 2, btnY + 5, 0xFFFFFFFF, true);

        // ESC hint no rodapé
        String esc = "§8[ESC] fechar  §7|  §8[click] selecionar";
        int ew = font.width(esc.replaceAll("§.", ""));
        g.drawString(font, esc, (width - ew) / 2, btnY + btnH + 6, 0xFFAAAAAA, true);

        super.render(g, mouseX, mouseY, partial);
    }

    private void drawClassCard(GuiGraphics g, int x, int y,
                               MageClass cls, boolean hovered, boolean selected) {
        // Background
        int bg = selected ? 0xCC332266 : (hovered ? 0xCC2A1A4A : 0xAA1A0830);
        g.fill(x, y, x + CARD_W, y + CARD_H, bg);

        // Border (cor da classe)
        int border = colorFromCode(cls.colorCode);
        if (selected) border = 0xFFFFDD33;  // gold border pra selecionada
        else if (hovered) border = 0xFFFFFFFF;
        g.fill(x, y, x + CARD_W, y + 1, border);
        g.fill(x, y + CARD_H - 1, x + CARD_W, y + CARD_H, border);
        g.fill(x, y, x + 1, y + CARD_H, border);
        g.fill(x + CARD_W - 1, y, x + CARD_W, y + CARD_H, border);

        // Selected indicator
        if (selected) {
            g.drawString(font, "§e✓", x + CARD_W - 12, y + 3, 0xFFFFDD33, true);
        }

        // Class name (top, com cor)
        g.drawString(font, cls.colorCode + "§l" + cls.displayName,
                x + 6, y + 2, 0xFFFFFFFF, true);

        // Stats line: base dmg + step
        String stats = "§7+§a" + cls.baseDmgPct + "%§7 base · §a+" + (int)cls.stepPct + "%§7/lv";
        g.drawString(font, stats, x + 6, y + 11, 0xFFCCCCCC, true);

        // Effects line
        String fx = "§8" + cls.primaryEffect + " + " + cls.secondaryEffect;
        g.drawString(font, fx, x + 6, y + 20, 0xFF888888, true);
    }

    private int colorFromCode(String code) {
        return switch (code) {
            case "§5" -> 0xFFAA00AA;
            case "§c" -> 0xFFFF5555;
            case "§6" -> 0xFFFFAA00;
            case "§a" -> 0xFF55FF55;
            case "§b" -> 0xFF55FFFF;
            case "§e" -> 0xFFFFFF55;
            case "§4" -> 0xFFAA0000;
            case "§d" -> 0xFFFF55FF;
            case "§7" -> 0xFFAAAAAA;
            case "§3" -> 0xFF00AAAA;
            case "§f" -> 0xFFFFFFFF;
            case "§2" -> 0xFF00AA00;
            default -> 0xFFAAAAAA;
        };
    }

    private MageClass safeValue(String name) {
        try { return MageClass.valueOf(name); }
        catch (Exception e) { return null; }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        MageClass[] classes = MageClass.values();
        int gridStartX = (width - (COLS * CARD_W + (COLS - 1) * CARD_GAP)) / 2;
        int gridStartY = gridTop();

        // Click num card?
        for (int i = 0; i < classes.length; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = gridStartX + col * (CARD_W + CARD_GAP);
            int cy = gridStartY + row * (CARD_H + CARD_GAP);
            if (mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H) {
                ModNetwork.sendToServer(new SelectClassC2SPacket(classes[i].name()));
                this.onClose();
                return true;
            }
        }

        // Click no botão remover?
        int rows = (classes.length + COLS - 1) / COLS;
        int btnY = gridStartY + rows * (CARD_H + CARD_GAP) + 4;
        int btnW = 160, btnH = 18;
        int btnX = (width - btnW) / 2;
        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
            ModNetwork.sendToServer(new SelectClassC2SPacket(""));  // empty = remove
            this.onClose();
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
