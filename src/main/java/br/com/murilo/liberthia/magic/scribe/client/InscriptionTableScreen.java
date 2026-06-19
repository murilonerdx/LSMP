package br.com.murilo.liberthia.magic.scribe.client;

import br.com.murilo.liberthia.magic.scribe.InscriptionRecipes;
import br.com.murilo.liberthia.magic.scribe.InscriptionTableMenu;
import br.com.murilo.liberthia.magic.scribe.SelectInscriptionRecipeC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * r166: <b>Inscription Table Screen</b> — layout compacto, sem sobreposição.
 *
 * <pre>
 * ┌──────────────────────────────────────────────┐
 * │  Mesa de Inscrição                            │
 * │ ┌── Receitas ──────────────┐  ┌── Slots ───┐ │
 * │ │ [ic] Pergaminho Vazio    │  │  Livro     │ │
 * │ │ [ic] Pergaminho de Glifo │  │  [slot]    │ │
 * │ │ [ic] Tinta de Sangue     │  │  Tinta     │ │
 * │ │ [ic] Tomo Aprendiz       │  │  [slot]    │ │
 * │ │ [ic] Pergaminho Místico  │  │  Saída     │ │
 * │ └──────────────────────────┘  │  [slot]    │ │
 * │                                └────────────┘ │
 * │  Inventário do player                         │
 * └──────────────────────────────────────────────┘
 * </pre>
 *
 * <p>Os 3 slots (Livro 180,34 · Tinta 180,66 · Saída 180,98) batem exatamente
 * com {@link InscriptionTableMenu}. Detalhes da receita aparecem no tooltip ao
 * passar o mouse — sem caixa fixa que sobrepunha o inventário.
 */
@OnlyIn(Dist.CLIENT)
public class InscriptionTableScreen extends AbstractContainerScreen<InscriptionTableMenu> {

    // ── Geometria do painel de receitas (coords relativas à origem da GUI) ──
    private static final int PANEL_X = 7;
    private static final int PANEL_Y = 20;
    private static final int PANEL_W = 144;
    private static final int PANEL_H = 98;
    private static final int ROW_TOP = 33;   // topo da 1ª linha
    private static final int ENTRY_H = 16;

    public InscriptionTableScreen(InscriptionTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 224;
        this.imageHeight = 212;
        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 120;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // ── Fundo principal ────────────────────────────────────────────────
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF180E2A);
        drawBorder(g, x, y, imageWidth, imageHeight, 0xFFB89050, 2);
        drawBorder(g, x + 3, y + 3, imageWidth - 6, imageHeight - 6, 0xFF5A3A1A, 1);

        // ── Barra de título ─────────────────────────────────────────────────
        g.fill(x, y, x + imageWidth, y + 16, 0xFF120A1F);
        drawBorder(g, x, y, imageWidth, 16, 0xFFB89050, 1);

        // ── Painel de receitas (esquerda) ───────────────────────────────────
        int px = x + PANEL_X, py = y + PANEL_Y;
        g.fill(px, py, px + PANEL_W, py + PANEL_H, 0xFF0E0720);
        drawBorder(g, px, py, PANEL_W, PANEL_H, 0xFF6644AA, 1);
        g.drawString(font, "Receitas", px + 5, py + 3, 0xFFDDBBFF, false);
        g.fill(px + 4, py + 12, px + PANEL_W - 4, py + 13, 0xFF4A3A6A);

        var recipes = InscriptionRecipes.LIST;
        for (int i = 0; i < recipes.size(); i++) {
            int ey = y + ROW_TOP + i * ENTRY_H;
            boolean isSelected = i == menu.getSelectedRecipe();
            boolean hovered = mouseX >= px + 2 && mouseX < px + PANEL_W - 2
                    && mouseY >= ey && mouseY < ey + ENTRY_H;

            int bg = isSelected ? 0xFF3A2266 : (hovered ? 0xFF241550 : 0xFF150B2C);
            g.fill(px + 2, ey, px + PANEL_W - 2, ey + ENTRY_H - 1, bg);
            if (isSelected) {
                g.fill(px + 2, ey, px + 4, ey + ENTRY_H - 1, 0xFFAA77FF); // barra de seleção
            }

            // Ícone do resultado (16×16, preenche a altura da linha)
            ItemStack resultIcon = recipes.get(i).result();
            if (!resultIcon.isEmpty()) {
                g.renderItem(resultIcon, px + 5, ey);
            }

            // Nome (cabe inteiro: ~116px de largura no painel)
            int nameColor = isSelected ? 0xFFFFFFFF : 0xFFCCCCCC;
            g.drawString(font, recipes.get(i).displayName, px + 25, ey + 4, nameColor, false);
        }

        // ── Coluna de slots (direita) ───────────────────────────────────────
        int cx = x + 155, cw = 64;
        g.fill(cx, py, cx + cw, py + PANEL_H, 0xFF0E0720);
        drawBorder(g, cx, py, cw, PANEL_H, 0xFF6644AA, 1);

        // Slots reais: Livro(180,34) · Tinta(180,66) · Saída(180,98) — ver Menu.
        g.drawString(font, "Livro", x + 174, y + 24, 0xFFCCCCCC, false);
        drawSlotBg(g, x + 180, y + 34, 0xFF0E0720, 0xFF6644AA);

        g.drawString(font, "+",     x + 187, y + 53, 0xFF8866BB, false);

        g.drawString(font, "Tinta", x + 174, y + 56, 0xFFCCCCCC, false);
        drawSlotBg(g, x + 180, y + 66, 0xFF1A0A00, 0xFF884422);

        g.drawString(font, "v",     x + 187, y + 85, 0xFFE8C870, false);

        g.drawString(font, "Saída", x + 174, y + 88, 0xFFFFEE88, false);
        drawSlotBg(g, x + 180, y + 98, 0xFF1A1000, 0xFF997700);

        // ── Inventário do player ────────────────────────────────────────────
        int invY = y + 124;
        g.fill(x + 4, invY, x + imageWidth - 4, y + imageHeight - 4, 0xFF120A1F);
        drawBorder(g, x + 4, invY, imageWidth - 8, y + imageHeight - 4 - invY, 0xFF333355, 1);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlotBg(g, x + 8 + col * 18, y + 130 + row * 18, 0xFF180E2A, 0xFF3A2A55);
        for (int col = 0; col < 9; col++)
            drawSlotBg(g, x + 8 + col * 18, y + 188, 0xFF180E2A, 0xFF4A3560);
    }

    private void drawSlotBg(GuiGraphics g, int x, int y, int fill, int border) {
        g.fill(x - 1, y - 1, x + 17, y + 17, border);
        g.fill(x, y, x + 16, y + 16, fill);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color, int t) {
        g.fill(x, y, x + w, y + t, color);
        g.fill(x, y + h - t, x + w, y + h, color);
        g.fill(x, y, x + t, y + h, color);
        g.fill(x + w - t, y, x + w, y + h, color);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, this.title, this.titleLabelX, this.titleLabelY, 0xFFE8C870, false);
        g.drawString(font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF9999BB, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int px = this.leftPos + PANEL_X;
        var recipes = InscriptionRecipes.LIST;
        for (int i = 0; i < recipes.size(); i++) {
            int ey = this.topPos + ROW_TOP + i * ENTRY_H;
            if (mx >= px + 2 && mx < px + PANEL_W - 2 && my >= ey && my < ey + ENTRY_H) {
                br.com.murilo.liberthia.network.ModNetwork.sendToServer(
                        new SelectInscriptionRecipeC2SPacket(menu.getBlockPos(), i));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);

        // Tooltip detalhado ao passar o mouse numa receita
        int px = this.leftPos + PANEL_X;
        var recipes = InscriptionRecipes.LIST;
        for (int i = 0; i < recipes.size(); i++) {
            int ey = this.topPos + ROW_TOP + i * ENTRY_H;
            if (mouseX >= px + 2 && mouseX < px + PANEL_W - 2 && mouseY >= ey && mouseY < ey + ENTRY_H) {
                InscriptionRecipes.Recipe r = recipes.get(i);
                g.renderComponentTooltip(font, List.of(
                        Component.literal("§e§l" + r.displayName),
                        Component.literal("§7" + r.description),
                        Component.literal("§8Livro: §f" + getItemName(r.inputItem.get())),
                        Component.literal("§8Tinta: §f" + getItemName(r.inkItem.get()))
                ), mouseX, mouseY);
                break;
            }
        }
    }

    private static String getItemName(net.minecraft.world.item.Item item) {
        try { return item.getDescription().getString(); } catch (Throwable t) { return "?"; }
    }
}
