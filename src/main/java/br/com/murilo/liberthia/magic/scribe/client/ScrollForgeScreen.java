package br.com.murilo.liberthia.magic.scribe.client;

import br.com.murilo.liberthia.magic.scribe.ScrollForgeBlockEntity;
import br.com.murilo.liberthia.magic.scribe.ScrollForgeMenu;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * r166: <b>Scroll Forge Screen</b> — layout compacto e alinhado.
 *
 * <p>Duas zonas no topo (acima do inventário):
 * <ul>
 *   <li><b>Esquerda</b> — legenda das 7 escolas (quadradinho colorido + nome).
 *       A escola do Focus colocado fica destacada.</li>
 *   <li><b>Direita</b> — [Focus] »» [Scroll] com instruções curtas embaixo.</li>
 * </ul>
 *
 * <p>As coordenadas dos slots batem exatamente com {@link ScrollForgeMenu}
 * (Focus 124,36 · Output 170,36), então os itens caem dentro dos fundos
 * desenhados aqui — nada mais vaza pra fora do painel.
 */
@OnlyIn(Dist.CLIENT)
public class ScrollForgeScreen extends AbstractContainerScreen<ScrollForgeMenu> {

    /** Nome de cada escola — índice = {@link SpellSchool#ordinal()}. */
    private static final String[] SCHOOL_NAMES = {
        "Fogo", "Gelo", "Raio", "Sangue", "Eldritch", "Sagrado", "Natureza"
    };
    /** Cor do quadradinho/nome de cada escola (ARGB, opaco). */
    private static final int[] SCHOOL_COLORS = {
        0xFFFF5533, 0xFF55CCFF, 0xFFFFEE44, 0xFFB01726,
        0xFFAA55EE, 0xFFEEEEDD, 0xFF44CC44
    };

    public ScrollForgeScreen(ScrollForgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 200;
        this.imageHeight = 186;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 94;
        this.titleLabelX = 8;
        this.titleLabelY = 5;
    }

    /** Pulso 0..1 baseado no relógio (sem precisar de tick). */
    private float getPulse() {
        return (float) (Math.sin(System.currentTimeMillis() / 500.0)) * 0.5f + 0.5f;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // ── Fundo principal ──────────────────────────────────────────────────
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1A1428);
        drawBorder(g, x, y, imageWidth, imageHeight, 0xFF8888AA, 2);
        drawBorder(g, x + 3, y + 3, imageWidth - 6, imageHeight - 6, 0xFF444466, 1);

        // ── Barra de título ──────────────────────────────────────────────────
        g.fill(x, y, x + imageWidth, y + 16, 0xFF221836);
        drawBorder(g, x, y, imageWidth, 16, 0xFF8888AA, 1);

        // ── Painel de legenda (esquerda) ─────────────────────────────────────
        int px = x + 7, py = y + 20, pw = 90, ph = 70;
        g.fill(px, py, px + pw, py + ph, 0xFF120C20);
        drawBorder(g, px, py, pw, ph, 0xFF555580, 1);
        g.drawString(font, "Escolas", px + 5, py + 3, 0xFFAABBDD, false);
        g.fill(px + 4, py + 12, px + pw - 4, py + 13, 0xFF3A3A5A); // separador

        SpellSchool active = ScrollForgeBlockEntity.identifySchool(
                menu.getBE().getItemHandler().getStackInSlot(ScrollForgeBlockEntity.SLOT_FOCUS));
        for (int i = 0; i < SCHOOL_NAMES.length; i++) {
            int ey = py + 16 + i * 8;
            boolean isActive = active != null && active.ordinal() == i;
            if (isActive) {
                g.fill(px + 2, ey - 1, px + pw - 2, ey + 7, 0xFF2E2050);
                g.fill(px + 2, ey - 1, px + 3, ey + 7, SCHOOL_COLORS[i]); // barra de seleção
            }
            // quadradinho colorido (6×6) com contorno escuro
            g.fill(px + 6, ey, px + 13, ey + 7, 0xFF000000);
            g.fill(px + 7, ey + 1, px + 12, ey + 6, SCHOOL_COLORS[i]);
            int nameColor = isActive ? 0xFFFFFFFF : SCHOOL_COLORS[i];
            g.drawString(font, SCHOOL_NAMES[i], px + 17, ey, nameColor, false);
        }

        // ── Zona de forja (direita) ──────────────────────────────────────────
        // Slots reais: Focus(124,36) · Output(170,36) — ver ScrollForgeMenu.
        g.drawString(font, "Focus",  x + 116, y + 26, 0xFFCCCCCC, false);
        g.drawString(font, "Scroll", x + 160, y + 26, 0xFFFFEE88, false);

        drawSlotBg(g, x + 124, y + 36, 0xFF1E1040, hasFocus()  ? 0xFF8844FF : 0xFF443366);
        drawSlotBg(g, x + 170, y + 36, 0xFF201408, hasOutput() ? 0xFFCC8800 : 0xFF443322);

        int arrowColor = blendColors(0xFF445566, 0xFF00DDCC, getPulse());
        g.drawString(font, ">>", x + 149, y + 40, arrowColor, false);

        // ── Caixa de instruções ──────────────────────────────────────────────
        int ix = x + 103, iy = y + 58, iw = 90, ih = 32;
        g.fill(ix, iy, ix + iw, iy + ih, 0xFF160F28);
        drawBorder(g, ix, iy, iw, ih, 0xFF444466, 1);
        g.drawString(font, "Como usar:",     ix + 4, iy + 3,  0xFFCCCCCC, false);
        g.drawString(font, "- Insira Focus",  ix + 4, iy + 13, 0xFFAAAAAA, false);
        g.drawString(font, "- Pegue Scroll",  ix + 4, iy + 22, 0xFFAAAAAA, false);

        // ── Fundo do inventário do player ─────────────────────────────────────
        g.fill(x + 4, y + 98, x + imageWidth - 4, y + imageHeight - 4, 0xFF18122A);
        drawBorder(g, x + 4, y + 98, imageWidth - 8, imageHeight - 102, 0xFF333355, 1);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotBg(g, x + 8 + col * 18, y + 104 + row * 18, 0xFF1A1428, 0xFF333350);
        for (int col = 0; col < 9; col++)
            drawSlotBg(g, x + 8 + col * 18, y + 162, 0xFF1A1428, 0xFF3D365A);
    }

    private boolean hasFocus() {
        return !menu.getBE().getItemHandler().getStackInSlot(ScrollForgeBlockEntity.SLOT_FOCUS).isEmpty();
    }
    private boolean hasOutput() {
        return !menu.getBE().getItemHandler().getStackInSlot(ScrollForgeBlockEntity.SLOT_OUTPUT).isEmpty();
    }

    private void drawSlotBg(GuiGraphics g, int x, int y, int fill, int border) {
        g.fill(x - 1, y - 1, x + 17, y + 17, border);
        g.fill(x, y, x + 16, y + 16, fill);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color, int thickness) {
        g.fill(x, y, x + w, y + thickness, color);
        g.fill(x, y + h - thickness, x + w, y + h, color);
        g.fill(x, y, x + thickness, y + h, color);
        g.fill(x + w - thickness, y, x + w, y + h, color);
    }

    /** Interpolação linear entre duas cores ARGB. t em [0,1]. */
    private static int blendColors(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (rr << 16) | (rg << 8) | rb;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, this.title, this.titleLabelX, this.titleLabelY, 0xFFCCBBFF, false);
        g.drawString(font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF9999BB, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);
    }
}
