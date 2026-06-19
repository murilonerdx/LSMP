package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.block.entity.QuantumTerminalBlockEntity;
import br.com.murilo.liberthia.menu.QuantumTerminalMenu;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.TerminalActionC2SPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * v0.1.22 r39: GUI do Quantum Terminal — log + send.
 *
 * <h2>r39 BUG FIXES</h2>
 * <ul>
 *   <li><b>Typing G/T/E close GUI:</b> override {@code keyPressed} pra
 *       interceptar TODAS teclas quando EditBox tá focado — antes os hotkeys
 *       de inventário (E) chat (T) etc. fechavam o screen.</li>
 *   <li><b>ENTER manda mensagem</b> quando msgInput tá focado.</li>
 *   <li><b>ENTER tuna freq</b> quando freqInput tá focado.</li>
 *   <li><b>Layout:</b> mantém 256×192 com mais espaço pro log (newest no topo).</li>
 * </ul>
 */
public class QuantumTerminalScreen extends AbstractContainerScreen<QuantumTerminalMenu> {

    private static final int W = 256;
    private static final int H = 192;

    private static final int BG_DARK = 0xFF0A1525;
    private static final int BG_MID = 0xFF132540;
    private static final int BORDER = 0xFF3D5A80;
    private static final int ACCENT = 0xFF55CCFF;
    private static final int GREEN = 0xFF66FF99;
    private static final int RED = 0xFFFF5566;
    private static final int LOG_BG = 0xFF05101F;

    private EditBox freqInput;
    private EditBox msgInput;
    private Button tuneButton;
    private Button sendButton;

    public QuantumTerminalScreen(QuantumTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        int cx = (this.width - W) / 2;
        int cy = (this.height - H) / 2;

        freqInput = new EditBox(this.font, cx + 8, cy + 22, 80, 14, Component.literal("freq"));
        freqInput.setMaxLength(16);
        freqInput.setValue(menu.getFrequency());
        freqInput.setTextColor(0xFFCCEEFF);
        this.addRenderableWidget(freqInput);

        tuneButton = Button.builder(Component.literal("Tunar"), b -> sendFrequency())
                .bounds(cx + 92, cy + 20, 50, 18).build();
        this.addRenderableWidget(tuneButton);

        msgInput = new EditBox(this.font, cx + 8, cy + H - 26, 180, 14, Component.literal("msg"));
        msgInput.setMaxLength(200);
        msgInput.setTextColor(0xFFFFFFFF);
        this.addRenderableWidget(msgInput);

        sendButton = Button.builder(Component.literal("Enviar"), b -> sendMsg())
                .bounds(cx + 192, cy + H - 28, 56, 18).build();
        this.addRenderableWidget(sendButton);
    }

    private void sendFrequency() {
        ModNetwork.CHANNEL.sendToServer(new TerminalActionC2SPacket(menu.getPos(),
                TerminalActionC2SPacket.ACTION_SET_FREQ, freqInput.getValue().trim()));
    }

    private void sendMsg() {
        String msg = msgInput.getValue().trim();
        if (!msg.isEmpty()) {
            ModNetwork.CHANNEL.sendToServer(new TerminalActionC2SPacket(menu.getPos(),
                    TerminalActionC2SPacket.ACTION_SEND_MSG, msg));
            msgInput.setValue("");
        }
    }

    // ───────────────── r39: KEY HANDLING FIX ─────────────────

    /**
     * r39 FIX BUG: digitar G/T/E (ou qualquer hotkey de inventário) fechava
     * a GUI mesmo com EditBox focado. Solução: interceptar TODAS as teclas
     * quando algum EditBox tá focado, e SÓ deixar ESCAPE passar pro super.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESCAPE: deixa o super tratar (fecha GUI ou tira foco)
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        // Se msgInput tá focado: ENTER manda, resto vai pro EditBox
        if (msgInput != null && msgInput.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                sendMsg();
                return true;
            }
            if (msgInput.keyPressed(keyCode, scanCode, modifiers)) return true;
            // BLOQUEIA tudo que sobrar (E, T, G, hotkeys do inv, slot keys, etc)
            return true;
        }

        // Se freqInput tá focado: ENTER tuna, resto vai pro EditBox
        if (freqInput != null && freqInput.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                sendFrequency();
                return true;
            }
            if (freqInput.keyPressed(keyCode, scanCode, modifiers)) return true;
            // BLOQUEIA tudo que sobrar
            return true;
        }

        // Nenhum EditBox focado: comportamento normal (hotkeys ok)
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** r39: char typed também precisa ser interceptado pra letras. */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (msgInput != null && msgInput.isFocused()) {
            return msgInput.charTyped(codePoint, modifiers);
        }
        if (freqInput != null && freqInput.isFocused()) {
            return freqInput.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (freqInput != null && !freqInput.isFocused()) {
            String f = menu.getFrequency();
            if (!f.equals(freqInput.getValue())) freqInput.setValue(f);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int cx = (this.width - W) / 2;
        int cy = (this.height - H) / 2;

        // Background + border
        g.fill(cx, cy, cx + W, cy + H, BORDER);
        g.fill(cx + 1, cy + 1, cx + W - 1, cy + H - 1, BG_DARK);
        g.fill(cx + 2, cy + 2, cx + W - 2, cy + 18, BG_MID);

        // Title
        Component title = Component.literal("§b§lQUANTUM TERMINAL")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        int tw = font.width(title);
        g.drawString(font, title, cx + (W - tw) / 2, cy + 5, ACCENT, false);

        // Status badge
        boolean active = menu.isActive();
        String stat = active ? "§a● ON" : "§c○ OFF";
        g.drawString(font, Component.literal(stat), cx + W - 50, cy + 5, active ? GREEN : RED, false);

        // Freq label
        g.drawString(font, Component.literal("§7Freq:"), cx + 8, cy + 24 - 11, 0xFFAAAAAA, false);

        // Log area
        int logX = cx + 8;
        int logY = cy + 44;
        int logW = W - 16;
        int logH = H - 80;
        g.fill(logX, logY, logX + logW, logY + logH, LOG_BG);
        g.fill(logX + 1, logY + 1, logX + logW - 1, logY + logH - 1, 0xFF000000);

        // Mostrar log entries (mais recentes em cima)
        List<QuantumTerminalBlockEntity.LogEntry> entries = menu.getLog();
        int lineY = logY + 4;
        int maxLines = (logH - 8) / 10;
        for (int i = 0; i < Math.min(maxLines, entries.size()); i++) {
            QuantumTerminalBlockEntity.LogEntry e = entries.get(i);
            String line = "§7[§d" + e.frequency + "§7] §f" + e.sender + "§7: §f" + e.message;
            // Trunca se muito longo
            if (font.width(line) > logW - 8) {
                line = font.plainSubstrByWidth(line, logW - 12) + "...";
            }
            g.drawString(font, Component.literal(line), logX + 4, lineY, 0xFFCCDDFF, false);
            lineY += 10;
        }
        if (entries.isEmpty()) {
            String emptyMsg = menu.getFrequency().isEmpty()
                    ? "§8(tune uma frequência pra ver mensagens)"
                    : "§8(canal §d" + menu.getFrequency() + "§8 sem mensagens)";
            g.drawString(font, Component.literal(emptyMsg),
                    logX + 4, lineY, 0xFF666666, false);
        }

        // Send hint
        g.drawString(font, Component.literal("§7Msg:"), cx + 8, cy + H - 38, 0xFFAAAAAA, false);

        // Energy bar (canto direito do top)
        int energyCur = menu.getEnergyStored();
        int energyMax = menu.getEnergyMax();
        if (energyMax > 0) {
            int eX = cx + 150;
            int eY = cy + 24;
            int eW = 90;
            int eH = 6;
            g.fill(eX, eY, eX + eW, eY + eH, LOG_BG);
            int fillW = (int) ((double) energyCur / energyMax * (eW - 2));
            int color = energyCur > energyMax / 4 ? 0xFF55AAFF : RED;
            g.fill(eX + 1, eY + 1, eX + 1 + fillW, eY + eH - 1, color);
            String txt = energyCur + " FE";
            g.drawString(font, txt, eX + 1, eY - 9, 0xFFAACCFF, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // skip default labels
    }
}
