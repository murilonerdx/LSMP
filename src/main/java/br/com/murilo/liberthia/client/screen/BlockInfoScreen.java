package br.com.murilo.liberthia.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * r164: Tela genérica pra mostrar info de blocos que não têm GUI dedicada.
 *
 * <p>Substituto pragmático pras GUIs completas dos blocos Mage Cauldron,
 * Auto Miner, Scroll Forge — em vez de Menu+Container+Slots, mostra um
 * painel modal com:
 * <ul>
 *   <li>Título grande</li>
 *   <li>Descrição (1-2 linhas)</li>
 *   <li>Stats lines (chave: valor)</li>
 *   <li>Instruções de uso ("Right-click com item pra ...")</li>
 * </ul>
 *
 * <p>Não é "GUI completa" como AN tem, mas é VISÍVEL e EXPLICATIVO — o user
 * passa a entender o que o bloco faz, em vez de só ver mensagem efêmera no chat.
 */
@OnlyIn(Dist.CLIENT)
public class BlockInfoScreen extends Screen {

    private final String headerTitle;
    private final List<String> statLines;
    private final List<String> instructionLines;
    private final int accentColor;

    public BlockInfoScreen(String title, List<String> stats, List<String> instructions, int accentColor) {
        super(Component.literal(title));
        this.headerTitle = title;
        this.statLines = stats;
        this.instructionLines = instructions;
        this.accentColor = accentColor | 0xFF000000;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fillGradient(0, 0, width, height, 0xCC000020, 0xEE000018);

        int panelW = 320;
        int panelH = Math.min(280, 60 + statLines.size() * 12 + instructionLines.size() * 12 + 30);
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;

        // Panel background
        g.fill(px, py, px + panelW, py + panelH, 0xEE1a0d2e);
        // Top accent strip
        g.fill(px, py, px + panelW, py + 3, accentColor);
        // Borders
        g.fill(px, py + panelH - 1, px + panelW, py + panelH, 0xFF555555);
        g.fill(px, py, px + 1, py + panelH, 0xFF666666);
        g.fill(px + panelW - 1, py, px + panelW, py + panelH, 0xFF666666);

        // Title
        int tw = font.width(headerTitle);
        g.drawString(font, "§l" + headerTitle, px + (panelW - tw) / 2, py + 10, 0xFFFFFFFF, true);

        // Separator
        g.fill(px + 20, py + 26, px + panelW - 20, py + 27, accentColor);

        // Stats lines (left column)
        int cursorY = py + 36;
        for (String stat : statLines) {
            g.drawString(font, stat, px + 14, cursorY, 0xFFCCCCCC, false);
            cursorY += 12;
        }

        // Instructions section
        cursorY += 8;
        g.fill(px + 20, cursorY, px + panelW - 20, cursorY + 1, 0x66FFFFFF);
        cursorY += 6;
        g.drawString(font, "§e§lComo usar:", px + 14, cursorY, 0xFFFFEE66, false);
        cursorY += 12;
        for (String instr : instructionLines) {
            g.drawString(font, instr, px + 14, cursorY, 0xFFCCCCFF, false);
            cursorY += 12;
        }

        // Footer
        String footer = "§8[ESC] Fechar";
        int fw = font.width(footer.replaceAll("§.", ""));
        g.drawString(font, footer, px + (panelW - fw) / 2, py + panelH - 14, 0xFFAAAAAA, true);

        super.render(g, mouseX, mouseY, partial);
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
