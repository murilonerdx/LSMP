package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.manual.ManualContent;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * r160: Liberthia Manual com sidebar + content + <b>busca textual</b>.
 *
 * <p>Busca:
 * <ul>
 *   <li>EditBox no topo do sidebar — digite e aperte Enter (ou click 🔍)</li>
 *   <li>Procura case-insensitive em title + body de TODAS as páginas</li>
 *   <li>Resultados substituem a lista de capítulos — click pula direto pra página</li>
 *   <li>Botão "✖" limpa busca e volta pra lista normal</li>
 *   <li>"Nenhum resultado" se não achar nada</li>
 * </ul>
 */
public class LiberthiaManualScreen extends Screen {

    private static final int W = 460;
    private static final int H = 260;
    private static final int SIDEBAR_W = 150;
    private static final int CHAPTER_BTN_H = 13;
    private static final int CHAPTERS_PER_PAGE = 10;
    private static final int RESULTS_PER_PAGE = 10;

    private static final int FRAME_OUTER  = 0xFF0E0212;
    private static final int FRAME_INNER  = 0xFF1B0830;
    private static final int FRAME_HILITE = 0xFF3B1A5C;
    private static final int CONTENT_BG   = 0xFF120420;
    private static final int SIDEBAR_BG   = 0xFF160628;
    private static final int SEARCH_BG    = 0xFF230C40;

    private int chapterIdx = 0;
    private int pageIdx = 0;
    private int chapterListPage = 0;
    private int x0, y0;

    /** r160: search state. */
    private EditBox searchBox;
    private String activeQuery = "";
    private List<SearchHit> searchHits = new ArrayList<>();
    private int searchResultsPage = 0;

    /** Snippet pra cada result. */
    private record SearchHit(int chapterIdx, int pageIdx, String chapterTitle,
                              String pageTitle, String snippet) {}

    /** r180: categoria do livro temático (ALL = manual completo). */
    private final ManualContent.Category category;

    public LiberthiaManualScreen() { this(ManualContent.Category.ALL); }

    public LiberthiaManualScreen(ManualContent.Category category) {
        super(Component.translatable("item.liberthia.liberthia_manual"));
        this.category = category == null ? ManualContent.Category.ALL : category;
    }

    private List<ManualContent.Chapter> getChapters() {
        try {
            String lang = Minecraft.getInstance().getLanguageManager().getSelected();
            return ManualContent.chaptersFor(lang, category);
        } catch (Throwable t) {
            return ManualContent.CHAPTERS;
        }
    }

    private int totalChapterPages() {
        int total = getChapters().size();
        return Math.max(1, (total + CHAPTERS_PER_PAGE - 1) / CHAPTERS_PER_PAGE);
    }

    private int totalResultPages() {
        if (searchHits.isEmpty()) return 1;
        return Math.max(1, (searchHits.size() + RESULTS_PER_PAGE - 1) / RESULTS_PER_PAGE);
    }

    @Override
    protected void init() {
        super.init();
        x0 = (this.width - W) / 2;
        y0 = (this.height - H) / 2;

        // ─── Search Box (topo do sidebar) ──────────────────────────────────
        int searchY = y0 + 26;
        int searchBoxW = SIDEBAR_W - 32; // deixa espaço pros 2 botões
        EditBox prev = searchBox;
        searchBox = new EditBox(this.font, x0 + 6, searchY, searchBoxW, 14,
                Component.literal("buscar..."));
        searchBox.setHint(Component.literal("§7buscar..."));
        searchBox.setMaxLength(64);
        if (prev != null) searchBox.setValue(prev.getValue());
        else if (!activeQuery.isEmpty()) searchBox.setValue(activeQuery);
        searchBox.setResponder(s -> {/* só busca em Enter */});
        this.addRenderableWidget(searchBox);

        // Botão de buscar
        this.addRenderableWidget(Button.builder(
                        Component.literal("🔍"),
                        btn -> performSearch())
                .bounds(x0 + 6 + searchBoxW + 2, searchY, 12, 14)
                .build());
        // Botão de limpar busca
        this.addRenderableWidget(Button.builder(
                        Component.literal("✖"),
                        btn -> clearSearch())
                .bounds(x0 + 6 + searchBoxW + 16, searchY, 12, 14)
                .build());

        int listStartY = searchY + 18;

        // ─── Modo SEARCH RESULTS ───────────────────────────────────────────
        if (!activeQuery.isEmpty()) {
            buildResultButtons(listStartY);
        } else {
            buildChapterButtons(listStartY);
        }

        // ─── Botão Fechar (rodapé sidebar) ─────────────────────────────────
        this.addRenderableWidget(Button.builder(
                        Component.literal("Fechar"),
                        btn -> this.onClose())
                .bounds(x0 + 6, y0 + H - 22, SIDEBAR_W - 12, 16)
                .build());

        // ─── Navegação de páginas DENTRO do capítulo ───────────────────────
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

        // Initial focus on search box for UX
        this.setInitialFocus(searchBox);
    }

    private void buildChapterButtons(int startY) {
        List<ManualContent.Chapter> chapters = getChapters();
        int total = chapters.size();
        int totalPages = totalChapterPages();
        if (chapterListPage >= totalPages) chapterListPage = totalPages - 1;
        if (chapterListPage < 0) chapterListPage = 0;

        // Garante que a sidebar mostre a página onde o capítulo selecionado está
        if (chapterIdx / CHAPTERS_PER_PAGE != chapterListPage) {
            chapterListPage = chapterIdx / CHAPTERS_PER_PAGE;
        }

        int startIdx = chapterListPage * CHAPTERS_PER_PAGE;
        int endIdx = Math.min(startIdx + CHAPTERS_PER_PAGE, total);
        int btnW = SIDEBAR_W - 12;

        for (int i = startIdx; i < endIdx; i++) {
            final int idx = i;
            var ch = chapters.get(i);
            int row = i - startIdx;
            int bx = x0 + 6;
            int by = startY + row * (CHAPTER_BTN_H + 2);
            String label = stripFormatting(ch.title());
            if (label.length() > 24) label = label.substring(0, 23) + "…";
            final String finalLabel = label;
            this.addRenderableWidget(Button.builder(
                            Component.literal(finalLabel),
                            btn -> { chapterIdx = idx; pageIdx = 0; rebuild(); })
                    .bounds(bx, by, btnW, CHAPTER_BTN_H)
                    .build());
        }

        // Setas pra navegar páginas de capítulos
        int navY = startY + CHAPTERS_PER_PAGE * (CHAPTER_BTN_H + 2) + 2;
        this.addRenderableWidget(Button.builder(
                        Component.literal("◀"),
                        btn -> {
                            if (chapterListPage > 0) {
                                chapterListPage--;
                                chapterIdx = chapterListPage * CHAPTERS_PER_PAGE;
                                pageIdx = 0;
                                rebuild();
                            }
                        })
                .bounds(x0 + 6, navY, 22, 14)
                .build());
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
    }

    private void buildResultButtons(int startY) {
        int totalResults = searchHits.size();
        int totalPages = totalResultPages();
        if (searchResultsPage >= totalPages) searchResultsPage = totalPages - 1;
        if (searchResultsPage < 0) searchResultsPage = 0;

        int startI = searchResultsPage * RESULTS_PER_PAGE;
        int endI = Math.min(startI + RESULTS_PER_PAGE, totalResults);
        int btnW = SIDEBAR_W - 12;

        for (int i = startI; i < endI; i++) {
            SearchHit hit = searchHits.get(i);
            final int hitChapterIdx = hit.chapterIdx;
            final int hitPageIdx = hit.pageIdx;
            int row = i - startI;
            int bx = x0 + 6;
            int by = startY + row * (CHAPTER_BTN_H + 2);
            String label = stripFormatting(hit.chapterTitle) + " · p" + (hit.pageIdx + 1);
            if (label.length() > 24) label = label.substring(0, 23) + "…";
            final String finalLabel = label;
            this.addRenderableWidget(Button.builder(
                            Component.literal(finalLabel),
                            btn -> {
                                chapterIdx = hitChapterIdx;
                                pageIdx = hitPageIdx;
                                // mantém busca ativa pra usuário ver outros resultados
                                rebuild();
                            })
                    .bounds(bx, by, btnW, CHAPTER_BTN_H)
                    .build());
        }

        // Setas pra navegar páginas de resultados (se houver mais de 10)
        if (totalResults > RESULTS_PER_PAGE) {
            int navY = startY + RESULTS_PER_PAGE * (CHAPTER_BTN_H + 2) + 2;
            this.addRenderableWidget(Button.builder(
                            Component.literal("◀"),
                            btn -> {
                                if (searchResultsPage > 0) {
                                    searchResultsPage--;
                                    rebuild();
                                }
                            })
                    .bounds(x0 + 6, navY, 22, 14)
                    .build());
            this.addRenderableWidget(Button.builder(
                            Component.literal("▶"),
                            btn -> {
                                if (searchResultsPage < totalResultPages() - 1) {
                                    searchResultsPage++;
                                    rebuild();
                                }
                            })
                    .bounds(x0 + SIDEBAR_W - 28, navY, 22, 14)
                    .build());
        }
    }

    private void performSearch() {
        String q = searchBox.getValue().trim();
        if (q.isEmpty()) {
            clearSearch();
            return;
        }
        activeQuery = q;
        String needle = q.toLowerCase();
        searchHits.clear();
        List<ManualContent.Chapter> chapters = getChapters();
        for (int ci = 0; ci < chapters.size(); ci++) {
            var ch = chapters.get(ci);
            for (int pi = 0; pi < ch.pages().size(); pi++) {
                var p = ch.pages().get(pi);
                String title = stripFormatting(p.title()).toLowerCase();
                String body = stripFormatting(p.body()).toLowerCase();
                if (title.contains(needle) || body.contains(needle)) {
                    String snippet = makeSnippet(stripFormatting(p.body()), q);
                    searchHits.add(new SearchHit(ci, pi,
                            stripFormatting(ch.title()),
                            stripFormatting(p.title()),
                            snippet));
                }
            }
        }
        searchResultsPage = 0;
        rebuild();
    }

    private void clearSearch() {
        activeQuery = "";
        searchHits.clear();
        searchResultsPage = 0;
        if (searchBox != null) searchBox.setValue("");
        rebuild();
    }

    /** Extrai trecho ao redor do match para preview. */
    private String makeSnippet(String body, String query) {
        String lower = body.toLowerCase();
        int idx = lower.indexOf(query.toLowerCase());
        if (idx < 0) return body.length() > 60 ? body.substring(0, 60) + "..." : body;
        int start = Math.max(0, idx - 20);
        int end = Math.min(body.length(), idx + query.length() + 30);
        String out = body.substring(start, end).replace("\n", " ");
        if (start > 0) out = "..." + out;
        if (end < body.length()) out = out + "...";
        return out;
    }

    private void rebuild() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter no search box dispara busca
        if (searchBox != null && searchBox.isFocused() && keyCode == 257 /* ENTER */) {
            performSearch();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        g.fill(x0 + 4, y0 + 24, x0 + SIDEBAR_W - 4, y0 + H - 26, SIDEBAR_BG);
        // Search box bg
        g.fill(x0 + 5, y0 + 25, x0 + SIDEBAR_W - 5, y0 + 41, SEARCH_BG);

        // Content panel bg
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

        // Título da página + ícone
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

        // Body wrapped — com highlight de match se busca ativa
        renderWrapped(g, page.body(), cx + 8, cy + 24, cw - 16, ch - 32);

        // Rodapé: "Página X / Y"
        String pageInfo = "§7Página " + (pageIdx + 1) + " / " + totalPagesInChapter;
        g.drawString(this.font, Component.literal(pageInfo),
                cx + 8, y0 + H - 18, 0xFFAAAAAA, false);

        // Label de status da sidebar (resultado da busca ou cap N/M)
        int statusY = y0 + H - 40;
        String statusLabel;
        if (!activeQuery.isEmpty()) {
            if (searchHits.isEmpty()) {
                statusLabel = "§cNenhum resultado";
            } else {
                statusLabel = "§a" + searchHits.size() + " result"
                        + (searchHits.size() == 1 ? "" : "s")
                        + " · pg " + (searchResultsPage + 1) + "/" + totalResultPages();
            }
        } else {
            statusLabel = "§dCap " + (chapterListPage + 1) + "/" + totalChapterPages();
        }
        g.drawString(this.font, Component.literal(statusLabel),
                x0 + 8, statusY, 0xFFFFFFFF, false);

        // Destaque do capítulo selecionado (só na lista normal)
        if (activeQuery.isEmpty()) {
            int startIdx = chapterListPage * CHAPTERS_PER_PAGE;
            if (chapterIdx >= startIdx && chapterIdx < startIdx + CHAPTERS_PER_PAGE) {
                int row = chapterIdx - startIdx;
                int bx = x0 + 6;
                int by = y0 + 44 + row * (CHAPTER_BTN_H + 2);
                g.fill(bx - 2, by - 1, bx, by + CHAPTER_BTN_H + 1, 0xFFD080FF);
            }
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

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
