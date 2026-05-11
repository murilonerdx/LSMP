package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.manual.ManualContent;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Tela do Liberthia Manual — sidebar com capítulos + área central com texto.
 *
 * <p>Layout responsivo: 320×210 base, escalado pra ocupar a tela. Sidebar de
 * ~110px com botões de capítulo. Conteúdo à direita com título e parágrafos.
 * Setas no rodapé direito navegam páginas dentro do capítulo atual.
 */
public class LiberthiaManualScreen extends Screen {

    private static final int W = 420;
    private static final int H = 240;

    private static final int SIDEBAR_W = 180;     // 2 colunas de capítulos
    private static final int CHAPTER_BTN_H = 14;
    private static final int CHAPTER_BTN_W = 84;  // (SIDEBAR_W - 12) / 2

    private static final int FRAME_OUTER = 0xFF0E0212;
    private static final int FRAME_INNER = 0xFF1B0830;
    private static final int FRAME_HILITE = 0xFF3B1A5C;
    private static final int CONTENT_BG  = 0xFF120420;
    private static final int SIDEBAR_BG  = 0xFF160628;

    private int chapterIdx = 0;
    private int pageIdx = 0;
    /** Origem (top-left) da janela. */
    private int x0, y0;

    public LiberthiaManualScreen() {
        super(Component.translatable("item.liberthia.liberthia_manual"));
    }

    @Override
    protected void init() {
        super.init();
        x0 = (this.width - W) / 2;
        y0 = (this.height - H) / 2;

        // Botões de capítulo em 2 COLUNAS pra caber muitos capítulos
        int total = ManualContent.CHAPTERS.size();
        int availH = H - 28 - 26; // espaço pra botões
        int rowsPerCol = Math.max(1, availH / (CHAPTER_BTN_H + 2));
        for (int i = 0; i < total; i++) {
            final int idx = i;
            var ch = ManualContent.CHAPTERS.get(i);
            int col = i / rowsPerCol;
            int row = i % rowsPerCol;
            int bx = x0 + 6 + col * (CHAPTER_BTN_W + 2);
            int by = y0 + 28 + row * (CHAPTER_BTN_H + 2);
            String label = stripFormatting(ch.title());
            if (label.length() > 14) label = label.substring(0, 13) + "…";
            String finalLabel = label;
            this.addRenderableWidget(Button.builder(
                            Component.literal(finalLabel),
                            btn -> { chapterIdx = idx; pageIdx = 0; })
                    .bounds(bx, by, CHAPTER_BTN_W, CHAPTER_BTN_H)
                    .build());
        }

        // Botão Fechar (rodapé sidebar)
        this.addRenderableWidget(Button.builder(
                        Component.literal("Fechar"),
                        btn -> this.onClose())
                .bounds(x0 + 6, y0 + H - 22, SIDEBAR_W - 12, 16)
                .build());

        // Botões prev/next page
        this.addRenderableWidget(Button.builder(
                        Component.literal("◀"),
                        btn -> { if (pageIdx > 0) pageIdx--; })
                .bounds(x0 + W - 70, y0 + H - 22, 28, 16)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.literal("▶"),
                        btn -> {
                            int max = ManualContent.CHAPTERS.get(chapterIdx).pages().size();
                            if (pageIdx < max - 1) pageIdx++;
                        })
                .bounds(x0 + W - 38, y0 + H - 22, 28, 16)
                .build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        RenderSystem.enableBlend();

        // Painel principal
        g.fill(x0, y0, x0 + W, y0 + H, FRAME_OUTER);
        g.fill(x0 + 1, y0 + 1, x0 + W - 1, y0 + H - 1, FRAME_INNER);
        g.fill(x0 + 1, y0 + 1, x0 + W - 1, y0 + 2, FRAME_HILITE);

        // Faixa de título
        g.fill(x0 + 4, y0 + 4, x0 + W - 4, y0 + 22, 0xFF2B0F4D);
        g.drawString(this.font,
                Component.literal("§l§dLiberthia — Manual do Pesquisador").getString(),
                x0 + 10, y0 + 9, 0xFFFFFFFF, true);

        // Sidebar bg
        g.fill(x0 + 4, y0 + 26, x0 + SIDEBAR_W - 4, y0 + H - 26, SIDEBAR_BG);

        // Content panel bg
        int cx = x0 + SIDEBAR_W;
        int cy = y0 + 26;
        int cw = W - SIDEBAR_W - 6;
        int ch = H - 52;
        g.fill(cx, cy, cx + cw, cy + ch, CONTENT_BG);

        // Conteúdo
        ManualContent.Chapter chapter = ManualContent.CHAPTERS.get(chapterIdx);
        int totalPages = chapter.pages().size();
        if (pageIdx >= totalPages) pageIdx = 0;
        ManualContent.Page page = chapter.pages().get(pageIdx);

        // Título da página + ícone (se houver)
        int titleX = cx + 8;
        int titleY = cy + 6;
        if (page.itemIcon() != null && !page.itemIcon().isEmpty()) {
            // Renderiza ícone 16x16 do item à esquerda do título
            net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getValue(new net.minecraft.resources.ResourceLocation(page.itemIcon()));
            if (item != null) {
                g.renderItem(new net.minecraft.world.item.ItemStack(item), titleX, titleY - 4);
                titleX += 20;  // shift título pra direita
            }
        }
        g.drawString(this.font,
                Component.literal("§l" + page.title()),
                titleX, titleY, 0xFFE6E6FF, false);
        // Linha decorativa
        g.fill(cx + 8, cy + 18, cx + cw - 8, cy + 19, FRAME_HILITE);

        // Body wrapped
        renderWrapped(g, page.body(), cx + 8, cy + 24, cw - 16, ch - 32);

        // Rodapé: "Página X / Y"
        String pageInfo = "§7Página " + (pageIdx + 1) + " / " + totalPages;
        g.drawString(this.font, Component.literal(pageInfo),
                cx + 8, y0 + H - 18, 0xFFAAAAAA, false);

        // Capítulo destacado na sidebar (multi-coluna)
        if (chapterIdx >= 0 && chapterIdx < ManualContent.CHAPTERS.size()) {
            int availH = H - 28 - 26;
            int rowsPerCol = Math.max(1, availH / (CHAPTER_BTN_H + 2));
            int col = chapterIdx / rowsPerCol;
            int row = chapterIdx % rowsPerCol;
            int bx = x0 + 6 + col * (CHAPTER_BTN_W + 2);
            int by = y0 + 28 + row * (CHAPTER_BTN_H + 2);
            g.fill(bx - 2, by - 1, bx, by + CHAPTER_BTN_H + 1, 0xFFD080FF);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Quebra texto em linhas e desenha. Suporta §-codes. Quebra por '\n' explícito. */
    private void renderWrapped(GuiGraphics g, String text, int x, int y, int w, int h) {
        int lineH = this.font.lineHeight + 1;
        int curY = y;

        for (String paragraph : text.split("\n")) {
            if (curY > y + h) break;
            if (paragraph.isEmpty()) {
                curY += lineH / 2; // espaço entre parágrafos
                continue;
            }
            FormattedText ft = FormattedText.of(paragraph);
            List<FormattedCharSequence> wrapped = this.font.split(ft, w);
            for (FormattedCharSequence line : wrapped) {
                if (curY > y + h) break;
                g.drawString(this.font, line, x, curY, 0xFFFFFFFF, false);
                curY += lineH;
            }
        }
    }

    /** Remove §-codes para usar em botões (que não suportam fcontrols). */
    private static String stripFormatting(String s) {
        StringBuilder sb = new StringBuilder();
        boolean skip = false;
        for (char c : s.toCharArray()) {
            if (c == '§') { skip = true; continue; }
            if (skip) { skip = false; continue; }
            sb.append(c);
        }
        return sb.toString();
    }

    @Override public boolean isPauseScreen() { return false; }
}
