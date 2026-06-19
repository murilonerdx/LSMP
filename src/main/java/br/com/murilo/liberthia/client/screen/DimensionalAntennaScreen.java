package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.DimensionalAntennaMenu;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SetAntennaFrequencyC2SPacket;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * v0.1.22 r37: GUI da Dimensional Antenna — LAYOUT REDESENHADO de novo.
 *
 * <h2>Problema r29</h2>
 * <ul>
 *   <li>Labels "Shard"/"DM" renderizados em §dx=40,y=36§r — mesma row dos slots
 *       em §dx=44,y=36§r → texto desenhado <b>POR DENTRO do slot</b>.</li>
 *   <li>Linha de Status em y=96 + overlay text do energy bar em y=99 → texto
 *       sobreposto a texto.</li>
 *   <li>Tuning bar em y=124-132 + inv label em y=128 → bar atrás do label.</li>
 * </ul>
 *
 * <h2>Solução r37</h2>
 * <ul>
 *   <li>GUI cresceu pra <b>176×232</b>. Menu desce inv pra y=148, hotbar y=206.</li>
 *   <li>Labels dos insumos integrados no <b>header de seção</b> em y=24
 *       (acima dos slots em y=36), sem texto dentro do slot.</li>
 *   <li>Telemetria virou <b>linha única</b> ("Status | Tunadas | Sintonia | Facing")
 *       em y=110, sem rows empilhadas que se atropelam.</li>
 *   <li>Energy bar (h=10) com texto centralizado <b>dentro</b> em y=122, longe
 *       do inv label em y=136.</li>
 *   <li>Sintonia agora é só TEXTO (percent + arrow), não bar separado — bar de
 *       sintonia foi removida porque empilhar dois bars dentro do espaço
 *       disponível sempre overlapa o inv label.</li>
 * </ul>
 */
public class DimensionalAntennaScreen extends AbstractContainerScreen<DimensionalAntennaMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 232;

    // Paleta — roxo escuro cósmico
    private static final int BG_DARK = 0xFF1A0F2E;
    private static final int BG_MID = 0xFF24164A;
    private static final int BG_LIGHT = 0xFF2D1A4A;
    private static final int BORDER = 0xFF6E4FB0;
    private static final int BORDER_BRIGHT = 0xFFAA88FF;
    private static final int ACCENT = 0xFFAA88FF;
    private static final int CYAN = 0xFF55CCFF;
    private static final int GREEN = 0xFF55FF99;
    private static final int RED = 0xFFFF5555;
    private static final int AMBER = 0xFFFFAA00;
    private static final int SLOT_BG = 0xFF0F0820;
    private static final int LINE_DIM = 0xFF3A2868;

    private EditBox freqInput;
    private Button tuneButton;

    public DimensionalAntennaScreen(DimensionalAntennaMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        // r37: label "Inventário" desce 8px junto com o inv (menu y=148)
        this.inventoryLabelY = 136;
        this.titleLabelY = -100; // desativa título default (desenhamos custom no renderBg)
    }

    @Override
    protected void init() {
        super.init();
        int cx = (this.width - GUI_WIDTH) / 2;
        int cy = (this.height - GUI_HEIGHT) / 2;

        // r37: input/botão posicionados sem conflitar com labels/separadores
        freqInput = new EditBox(this.font, cx + 8, cy + 74, 100, 14,
                Component.literal("freq"));
        freqInput.setMaxLength(16);
        freqInput.setBordered(true);
        freqInput.setVisible(true);
        freqInput.setTextColor(0xFFCCCCFF);
        freqInput.setValue(menu.getFrequency());
        this.addRenderableWidget(freqInput);

        tuneButton = Button.builder(Component.literal("Tunar"), btn -> sendFrequency())
                .bounds(cx + 112, cy + 72, 56, 18)
                .build();
        this.addRenderableWidget(tuneButton);
    }

    private void sendFrequency() {
        String freq = freqInput.getValue().trim();
        ModNetwork.CHANNEL.sendToServer(new SetAntennaFrequencyC2SPacket(menu.getPos(), freq));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (freqInput != null && !freqInput.isFocused()) {
            String serverFreq = menu.getFrequency();
            if (!serverFreq.equals(freqInput.getValue())) {
                freqInput.setValue(serverFreq);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int cx = (this.width - GUI_WIDTH) / 2;
        int cy = (this.height - GUI_HEIGHT) / 2;

        // ───────────── background + double border ─────────────
        g.fill(cx, cy, cx + GUI_WIDTH, cy + GUI_HEIGHT, BORDER);
        g.fill(cx + 1, cy + 1, cx + GUI_WIDTH - 1, cy + GUI_HEIGHT - 1, BG_DARK);
        g.fill(cx + 3, cy + 3, cx + GUI_WIDTH - 3, cy + GUI_HEIGHT - 3, BG_MID);

        // Highlight inner top border
        g.fill(cx + 3, cy + 3, cx + GUI_WIDTH - 3, cy + 4, BORDER_BRIGHT);

        // ───────────── y=6-15: TITLE ─────────────
        Component title = Component.literal("ANTENA DIMENSIONAL")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        int tw = font.width(title);
        g.drawString(font, title, cx + (GUI_WIDTH - tw) / 2, cy + 6, ACCENT, false);

        // separator after title
        sepLine(g, cx, cy + 18);

        // ───────────── y=22-31: INSUMOS HEADER (com labels integrados na mesma row) ─────────────
        // §dShard label (acima do slot esquerdo em x=44) — centro do label em x=44+8=52
        // §dDM label (acima do slot direito em x=116) — centro em x=124
        g.drawString(font, Component.literal("§7▸ §dINSUMOS"), cx + 8, cy + 22, 0xFFAA99CC, false);

        Component shardLbl = Component.literal("§dShard");
        int swSh = font.width(shardLbl);
        g.drawString(font, shardLbl, cx + 52 - swSh / 2, cy + 22, ACCENT, false);

        Component dmLbl = Component.literal("§5DM");
        int swDm = font.width(dmLbl);
        g.drawString(font, dmLbl, cx + 124 - swDm / 2, cy + 22, ACCENT, false);

        // ───────────── y=36-52: slot row ─────────────
        // (slots são renderizados pelo super em x=44/116, y=36 — menu config)
        drawSlotFrame(g, cx + 43, cy + 35);
        drawSlotFrame(g, cx + 115, cy + 35);

        // Center antenna preview — render the actual block item at 1x scale
        renderAntennaPreview(g, cx + 80, cy + 36);

        // separator after slots
        sepLine(g, cx, cy + 56);

        // ───────────── y=60-69: FREQUENCIA header ─────────────
        Component freqLabel = Component.literal("§7▸ §dFREQUÊNCIA");
        g.drawString(font, freqLabel, cx + 8, cy + 60, 0xFFAA99CC, false);
        // (freqInput + tuneButton rendered as widgets at y=72-90)

        // separator after freq section
        sepLine(g, cx, cy + 96);

        // ───────────── y=100-109: TELEMETRIA header ─────────────
        Component telLabel = Component.literal("§7▸ §dTELEMETRIA");
        g.drawString(font, telLabel, cx + 8, cy + 100, 0xFFAA99CC, false);

        // ───────────── y=110: STATUS LINE (inline) ─────────────
        // Format: "● ATIVA   Tunadas: 5   Sintonia: 75%   →N"
        boolean active = menu.isActive();
        String statusText = active ? "§a● ATIVA" : "§c○ INATIVA";
        g.drawString(font, statusText, cx + 8, cy + 110, 0xFFFFFFFF, false);

        // Tunadas (center-left)
        String tunedTxt = "§7Tx§f " + menu.getTunedCount();
        g.drawString(font, tunedTxt, cx + 58, cy + 110, 0xFFCCCCFF, false);

        // Sintonia % (center-right)
        int tuning = menu.getTuningPercent();
        String tuningColor;
        if (tuning >= 75) tuningColor = "§a";
        else if (tuning >= 40) tuningColor = "§6";
        else tuningColor = "§c";
        String tuningTxt = "§7Snt§r " + tuningColor + tuning + "%";
        g.drawString(font, tuningTxt, cx + 94, cy + 110, 0xFFCCCCFF, false);

        // Facing arrow (right)
        net.minecraft.core.Direction facing = menu.getFacing();
        String arrow = switch (facing) {
            case NORTH -> "↑N";
            case EAST -> "→E";
            case SOUTH -> "↓S";
            case WEST -> "←O";
            default -> "?";
        };
        String facingTxt = "§d" + arrow;
        int fTw = font.width(facingTxt);
        g.drawString(font, facingTxt, cx + GUI_WIDTH - 8 - fTw, cy + 110, ACCENT, false);

        // ───────────── y=122-131: ENERGY BAR (com texto dentro) ─────────────
        int energyCur = menu.getEnergyStored();
        int energyMax = menu.getEnergyMax();
        if (energyMax > 0) {
            int barX = cx + 8;
            int barY = cy + 122;
            int barW = GUI_WIDTH - 16;
            int barH = 10;
            // bg
            g.fill(barX, barY, barX + barW, barY + barH, SLOT_BG);
            // fill
            int fillW = (int) ((double) energyCur / energyMax * (barW - 2));
            int eColor = energyCur > energyMax / 4 ? 0xFF55AAFF : RED;
            g.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, eColor);
            // border highlight
            g.fill(barX, barY, barX + barW, barY + 1, LINE_DIM);
            g.fill(barX, barY + barH - 1, barX + barW, barY + barH, LINE_DIM);

            // Centered text INSIDE the bar (com sombra pra legibilidade)
            String eTxt = "⚡ " + formatFE(energyCur) + " / " + formatFE(energyMax) + " FE";
            int eTw = font.width(eTxt);
            g.drawString(font, eTxt, barX + (barW - eTw) / 2, barY + 1, 0xFFFFFFFF, true);
        }

        // separator before inventory (y=134, 2px above inv label at y=136)
        sepLine(g, cx, cy + 134);
    }

    private void sepLine(GuiGraphics g, int cx, int y) {
        g.fill(cx + 6, y, cx + GUI_WIDTH - 6, y + 1, LINE_DIM);
    }

    /** Frame visual em volta de um slot 16×16 (slot real é renderizado pelo super). */
    private void drawSlotFrame(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, BORDER);
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT_BG);
    }

    /**
     * Renderiza o item Dimensional Antenna como ícone central (1× = 16×16) —
     * mostra o modelo 3D real do bloco.
     */
    private void renderAntennaPreview(GuiGraphics g, int x, int y) {
        // background frame
        g.fill(x - 2, y - 2, x + 18, y + 18, SLOT_BG);
        g.fill(x - 1, y - 1, x + 17, y + 17, BG_DARK);

        // Active glow
        if (menu.isActive()) {
            int glow = 0x6655FF99;
            g.fill(x - 2, y - 2, x + 18, y - 1, glow);
            g.fill(x - 2, y + 17, x + 18, y + 18, glow);
            g.fill(x - 2, y - 2, x - 1, y + 18, glow);
            g.fill(x + 17, y - 2, x + 18, y + 18, glow);
        }

        try {
            ItemStack antenna = new ItemStack(ModItems.DIMENSIONAL_ANTENNA_ITEM.get());
            g.renderItem(antenna, x, y);
        } catch (Throwable ignored) {
            // fallback: pequeno desenho da antena
            int c = menu.isActive() ? GREEN : 0xFF666666;
            // base
            g.fill(x + 2, y + 12, x + 14, y + 16, c);
            // pole
            g.fill(x + 7, y + 4, x + 9, y + 12, c);
            // dish
            g.fill(x + 4, y + 2, x + 12, y + 4, c);
        }
    }

    /** Formato compacto de FE: 50000 → "50k", 1500000 → "1.5M". */
    private static String formatFE(int v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000) return String.format("%dk", v / 1_000);
        return String.valueOf(v);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // r37: desenha só o label "Inventário" do player, sem o título default
        // (já desenhamos o título custom no renderBg).
        g.drawString(font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                0xFFAA99CC, false);
    }
}
