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
 * Tela do Liberthia Manual — sidebar com capítulos paginados (11/página) + área
 * central com conteúdo da página atual.
 *
 * <h3>Layout v0.1.13 (refeito do zero):</h3>
 * <ul>
 *   <li>Sidebar SINGLE-COLUMN com 11 capítulos por página. Era multi-column e
 *       sobrescrevia o painel de conteúdo quando passava de ~20 capítulos.</li>
 *   <li>Navegação de capítulos: setas {@code [<] Cap N/M [>]} no rodapé da sidebar.</li>
 *   <li>Painel de conteúdo fixo à direita — NUNCA sobrescrito por botões.</li>
 *   <li>Navegação de páginas (dentro do capítulo): setas no rodapé direito.</li>
 * </ul>
 *
 * <p>Tamanhos calibrados pra suportar até 100 capítulos sem overflow.
 */
public class LiberthiaManualScreen extends Screen {

    private static final int W = 440;
    private static final int H = 240;

    /** Sidebar wider pra caber título completo dos capítulos. */
    private static final int SIDEBAR_W = 140;
    private static final int CHAPTER_BTN_H = 14;
    /** Capítulos visíveis por página da sidebar. 11 cabe perfeitamente. */
    private static final int CHAPTERS_PER_PAGE = 11;

    private static final int FRAME_OUTER  = 0xFF0E0212;
    private static final int FRAME_INNER  = 0xFF1B0830;
    private static final int FRAME_HILITE = 0xFF3B1A5C;
    private static final int CONTENT_BG   = 0xFF120420;
    private static final int SIDEBAR_BG   = 0xFF160628;

    /** Índice global do capítulo selecionado (0..total-1). */
    private int chapterIdx = 0;
    /** Página dentro do capítulo atual. */
    private int pageIdx = 0;
    /** Página da SIDEBAR (qual bloco de 11 capítulos mostrar). */
    private int chapterListPage = 0;
    /** Origem (top-left) da janela. */
    private int x0, y0;

    public LiberthiaManualScreen() {
        super(Component.translatable("item.liberthia.liberthia_manual"));
    }

    /**
     * Retorna o conjunto de capítulos correspondente ao idioma do client.
     * Atualmente força PT-BR (catálogo completo de 39 capítulos / 363 páginas);
     * EN parcial está pausada até tradução completa.
     */
    private List<ManualContent.Chapter> getChapters() {
        try {
            String lang = Minecraft.getInstance().getLanguageManager().getSelected();
            return ManualContent.chaptersForLocale(lang);
        } catch (Throwable t) {
            return ManualContent.CHAPTERS;
        }
    }

    private int totalChapterPages() {
        int total = getChapters().size();
        return Math.max(1, (total + CHAPTERS_PER_PAGE - 1) / CHAPTERS_PER_PAGE);
    }

    @Override
    protected void init() {
        super.init();
        x0 = (this.width - W) / 2;
        y0 = (this.height - H) / 2;

        List<ManualContent.Chapter> chapters = getChapters();
        int total = chapters.size();
        int totalPages = totalChapterPages();
        if (chapterListPage >= totalPages) chapterListPage = totalPages - 1;
        if (chapterListPage < 0) chapterListPage = 0;

        // Garante que a sidebar mostre a página onde o capítulo selecionado está
        chapterListPage = chapterIdx / CHAPTERS_PER_PAGE;

        int startIdx = chapterListPage * CHAPTERS_PER_PAGE;
        int endIdx = Math.min(startIdx + CHAPTERS_PER_PAGE, total);
        int btnW = SIDEBAR_W - 12;

        // --- Botões dos capítulos (single column, página atual) ---
        for (int i = startIdx; i < endIdx; i++) {
            final int idx = i;
            var ch = chapters.get(i);
            int row = i - startIdx;
            int bx = x0 + 6;
            int by = y0 + 28 + row * (CHAPTER_BTN_H + 2);
            String label = stripFormatting(ch.title());
            // Sidebar 140px = ~22 chars no font Minecraft. Trunca em 22.
            if (label.length() > 22) label = label.substring(0, 21) + "…";
            final String finalLabel = label;
            this.addRenderableWidget(Button.builder(
                            Component.literal(finalLabel),
                            btn -> { chapterIdx = idx; pageIdx = 0; rebuild(); })
                    .bounds(bx, by, btnW, CHAPTER_BTN_H)
                    .build());
        }

        // --- Navegação de capítulos (rodapé sidebar, ANTES do botão Fechar) ---
        int navY = y0 + 28 + CHAPTERS_PER_PAGE * (CHAPTER_BTN_H + 2) + 4;
        // Botão "anterior" página de capítulos
        this.addRenderableWidget(Button.builder(
                        Component.literal("◀"),
                        btn -> {
                            if (chapterListPage > 0) {
                                chapterListPage--;
                                // Posiciona seleção no primeiro capítulo da nova página
                                chapterIdx = chapterListPage * CHAPTERS_PER_PAGE;
                                pageIdx = 0;
                                rebuild();
                            }
                        })
                .bounds(x0 + 6, navY, 22, 14)
                .build());
        // Label "Cap. N/M" no meio (renderizado em render(), não é um botão)
        // Botão "próxima" página de capítulos
        this.addRenderableWidget(Button.builder(
                        Component.literal("▶"),
                        btn -> {
                            if (chapterListPage < totalChapterPages() - 1) {
                                chapterListPage++;
                                chapterIdx = chapterListPage * CHAPTERS_PER_PAGE;
                                pageIdx = 0;
                                rebuild();
                            }
                        })
                .bounds(x0 + SIDEBAR_W - 28, navY, 22, 14)
                .build());

        // --- Botão Fechar (rodapé sidebar) ---
        this.addRenderableWidget(Button.builder(
                        Component.literal("Fechar"),
                        btn -> this.onClose())
                .bounds(x0 + 6, y0 + H - 22, SIDEBAR_W - 12, 16)
                .build());

        // --- Navegação de páginas DENTRO do capítulo (rodapé direito) ---
        this.addRenderableWidget(Button.builder(
                        Component.literal("◀"),
                        btn -> { if (pageIdx > 0) pageIdx--; })
                .bounds(x0 + W - 70, y0 + H - 22, 28, 16)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.literal("▶"),
                        btn -> {
                            int max = getChapters().get(chapterIdx).pages().size();
                            if (pageIdx < max - 1) pageIdx++;
                        })
                .bounds(x0 + W - 38, y0 + H - 22, 28, 16)
                .build());
    }

    /** Reconstrói os widgets — chamado quando capítulo ou página muda. */
    private void rebuild() {
        this.clearWidgets();
        this.init();
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

        // Sidebar bg (estende até a área dos botões de navegação)
        g.fill(x0 + 4, y0 + 26, x0 + SIDEBAR_W - 4, y0 + H - 26, SIDEBAR_BG);

        // Content panel bg — CRÍTICO: começa em SIDEBAR_W (não overlap mais)
        int cx = x0 + SIDEBAR_W;
        int cy = y0 + 26;
        int cw = W - SIDEBAR_W - 6;
        int ch = H - 52;
        g.fill(cx, cy, cx + cw, cy + ch, CONTENT_BG);

        // Conteúdo do capítulo selecionado
        List<ManualContent.Chapter> chapters = getChapters();
        if (chapterIdx >= chapters.size()) chapterIdx = 0;
        ManualContent.Chapter chapter = chapters.get(chapterIdx);
        int totalPagesInChapter = chapter.pages().size();
        if (pageIdx >= totalPagesInChapter) pageIdx = 0;
        ManualContent.Page page = chapter.pages().get(pageIdx);

        // Título da página + ícone (se houver)
        int titleX = cx + 8;
        int titleY = cy + 6;
        if (page.itemIcon() != null && !page.itemIcon().isEmpty()) {
            net.minecraft.world.item.Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getValue(new net.minecraft.resources.ResourceLocation(page.itemIcon()));
            if (item != null) {
                g.renderItem(new net.minecraft.world.item.ItemStack(item), titleX, titleY - 4);
                titleX += 20;
            }
        }
        g.drawString(this.font,
                Component.literal("§l" + page.title()),
                titleX, titleY, 0xFFE6E6FF, false);
        g.fill(cx + 8, cy + 18, cx + cw - 8, cy + 19, FRAME_HILITE);

        // Body wrapped
        renderWrapped(g, page.body(), cx + 8, cy + 24, cw - 16, ch - 32);

        // Rodapé: "Página X / Y" (do capítulo atual)
        String pageInfo = "§7Página " + (pageIdx + 1) + " / " + totalPagesInChapter;
        g.drawString(this.font, Component.literal(pageInfo),
                cx + 8, y0 + H - 18, 0xFFAAAAAA, false);

        // Label "Cap. N/M" no centro entre as setas da sidebar
        int navY = y0 + 28 + CHAPTERS_PER_PAGE * (CHAPTER_BTN_H + 2) + 4;
        String navLabel = "§dCap " + (chapterListPage + 1) + "/" + totalChapterPages();
        int navLabelW = this.font.width(navLabel);
        g.drawString(this.font, Component.literal(navLabel),
                x0 + 6 + (SIDEBAR_W - 12) / 2 - navLabelW / 2,
                navY + 3, 0xFFFFFFFF, false);

        // Destaque do capítulo selecionado (só se estiver visível na página atual)
        int startIdx = chapterListPage * CHAPTERS_PER_PAGE;
        if (chapterIdx >= startIdx && chapterIdx < startIdx + CHAPTERS_PER_PAGE) {
            int row = chapterIdx - startIdx;
            int bx = x0 + 6;
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
                curY += lineH / 2;
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

    /** Remove §-codes para usar em botões (que não suportam control chars). */
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
