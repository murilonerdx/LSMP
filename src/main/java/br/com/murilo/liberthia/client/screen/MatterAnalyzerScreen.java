package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.matter.ClientMatterProfileCache;
import br.com.murilo.liberthia.matter.MatterContent;
import br.com.murilo.liberthia.matter.MatterProfileType;
import br.com.murilo.liberthia.menu.MatterAnalyzerMenu;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.RequestMatterProfileSyncC2SPacket;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * GUI estilo "computador de pesquisa" — agora com 3 tabs internos (roadmap 0.2.0):
 *
 * <ol>
 *   <li><b>Análise</b> — Le o item inserido no slot e mostra DM/WM/YM,
 *       mutação composta e energia equivalente. Tela original (refatorada).</li>
 *   <li><b>Curas</b> — Lista estática das 3 pílulas e o que cada uma faz
 *       (Clear/Dark/Yellow Matter Pills). Aprendizado in-game pra novos jogadores.</li>
 *   <li><b>Perfil</b> — Mostra o perfil de matéria DO JOGADOR que abriu (não
 *       do item). Inclui 3 barras DM/WM/YM, tipo composto ativo, e recomendação
 *       de cura baseada no que tá maior. Substitui a ideia original de
 *       "Histórico de exposição" — que exigiria capability nova com log de
 *       eventos persistido (esforço maior, ficou pra futura release).</li>
 * </ol>
 *
 * <p><b>Click handling</b>: tabs ficam no topo, fora do imageWidth. Click neles
 * troca {@code activeTab} e re-renderiza. Não bloqueia o menu container (slot do
 * item segue ativo em qualquer tab — comportamento intencional).
 */
public class MatterAnalyzerScreen extends AbstractContainerScreen<MatterAnalyzerMenu> {

    private enum Tab {
        ANALYZE("Análise", 0xFFE0B0FF),
        CURES("Curas", 0xFFB0E0FF),
        PROFILE("Perfil", 0xFFFFD080);

        final String label;
        final int accentColor;
        Tab(String l, int c) { label = l; accentColor = c; }
    }

    private Tab activeTab = Tab.ANALYZE;

    public MatterAnalyzerScreen(MatterAnalyzerMenu m, Inventory inv, Component title) {
        super(m, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        // v1 bug fix: pede sync imediato do profile pro server pra evitar que a
        // tab Perfil mostre valores zerados/stale (antes da próxima sync periódica
        // de 5s). Server responde com MatterProfileSyncS2CPacket que atualiza
        // ClientMatterProfileCache.
        ModNetwork.CHANNEL.sendToServer(new RequestMatterProfileSyncC2SPacket());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // Tabs no topo, área de 22x12 cada, alinhada com o painel.
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;
        for (int i = 0; i < Tab.values().length; i++) {
            int tx = x + 6 + i * 38;
            int ty = y - 14;
            if (mx >= tx && mx < tx + 36 && my >= ty && my < ty + 13) {
                Tab clicked = Tab.values()[i];
                if (clicked != activeTab) {
                    activeTab = clicked;
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager().play(
                                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        // Painel principal
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0E0212);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF161028);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF3B1A5C);

        // Tabs no topo (fora do imageHeight, em y negativo)
        for (int i = 0; i < Tab.values().length; i++) {
            Tab t = Tab.values()[i];
            int tx = x + 6 + i * 38;
            int ty = y - 14;
            boolean active = t == activeTab;
            // Borda
            g.fill(tx, ty, tx + 36, ty + 13, 0xFF000000);
            // Background — ativa fica brilhante, inativa escura
            int bg = active ? 0xFF2A1850 : 0xFF120428;
            g.fill(tx + 1, ty + 1, tx + 35, ty + 12, bg);
            // Highlight no topo se ativa
            if (active) {
                g.fill(tx + 1, ty + 1, tx + 35, ty + 2, t.accentColor);
            }
            // Label
            int labelColor = active ? 0xFFFFFFFF : 0xFF8060A0;
            int labelWidth = this.font.width(t.label);
            g.drawString(this.font, Component.literal(t.label),
                    tx + (36 - labelWidth) / 2, ty + 3, labelColor, false);
        }

        // Renderiza o conteúdo da tab ativa
        switch (activeTab) {
            case ANALYZE -> renderAnalyzeTab(g, x, y);
            case CURES   -> renderCuresTab(g, x, y);
            case PROFILE -> renderProfileTab(g, x, y);
        }

        // Player inv panel (sempre visível em qualquer tab)
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + imageHeight - 6, 0xFF120420);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF7F40C8);
    }

    // ─────────────────────────────────────────────────── TAB: ANÁLISE
    private void renderAnalyzeTab(GuiGraphics g, int x, int y) {
        // Área da máquina
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 76, 0xFF080018);
        for (int gy = y + 18; gy < y + 76; gy += 4)
            g.fill(x + 8, gy, x + imageWidth - 8, gy + 1, 0xFF120428);

        // Slot input
        slot(g, x + 24 - 1, y + 35 - 1, 0xFFAA60FF);

        // "Tela" com 3 barras
        int panelX = x + 60, panelY = y + 18, panelW = 110, panelH = 42;
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF000000);
        g.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, 0xFF002010);
        for (int sy = panelY + 3; sy < panelY + panelH - 2; sy += 4)
            g.fill(panelX + 2, sy, panelX + panelW - 2, sy + 1, 0xFF003020);

        MatterContent c = menu.currentContent();
        drawMatterRow(g, panelX + 4, panelY + 4,  "DM", c.dark(),   0xFF8B40D8);
        drawMatterRow(g, panelX + 4, panelY + 16, "WM", c.white(),  0xFFE6E6FF);
        drawMatterRow(g, panelX + 4, panelY + 28, "YM", c.yellow(), 0xFFFFD23F);
    }

    // ─────────────────────────────────────────────────── TAB: CURAS
    private void renderCuresTab(GuiGraphics g, int x, int y) {
        // Painel
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 76, 0xFF080018);
        g.drawString(this.font, Component.literal("§dCuras conhecidas:"),
                x + 10, y + 19, 0xFFFFFF, false);

        // v1 fix: layout HORIZONTAL compacto. Antes nome+desc ocupavam 2 linhas
        // (~18px por entry, 54px total) e vazavam pra área do inventário no y+76.
        // Agora 3 entries empilhadas em só 1 linha cada (~16px por entry, 48px
        // total) cabe certinho no painel y+30..y+78.
        renderCureEntry(g, x + 8, y + 30,
                ModItems.DARK_MATTER_PILL.get().getDefaultInstance(),
                "Dark Matter Pill",
                "-30 DM",
                0xFF8B40D8);
        renderCureEntry(g, x + 8, y + 46,
                ModItems.CLEAR_MATTER_PILL.get().getDefaultInstance(),
                "Clear Matter Pill",
                "-25 WM",
                0xFFE6E6FF);
        renderCureEntry(g, x + 8, y + 62,
                ModItems.YELLOW_MATTER_PILL.get().getDefaultInstance(),
                "Yellow Matter Pill",
                "-25 YM",
                0xFFFFD23F);
    }

    private void renderCureEntry(GuiGraphics g, int x, int y, ItemStack icon, String name,
                                  String desc, int color) {
        // v1: layout horizontal compacto — ícone (16x16) + nome + desc na mesma
        // linha, vertical-centered em y+4. Cabe em ~16px de altura, evita
        // overflow pra área do inventário.
        g.renderItem(icon, x, y);
        int textY = y + 4;
        g.drawString(this.font, Component.literal(name), x + 20, textY, color, false);
        int nameWidth = this.font.width(name);
        g.drawString(this.font, Component.literal("§7" + desc),
                x + 20 + nameWidth + 4, textY, 0xCCCCCC, false);
    }

    // ─────────────────────────────────────────────────── TAB: PERFIL
    private void renderProfileTab(GuiGraphics g, int x, int y) {
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 76, 0xFF080018);

        // v1 fix: lê do ClientMatterProfileCache (atualizado pelo packet S2C).
        // Antes lia do capability do player local, que NUNCA é sincronizado no
        // client side — só o cache estático é. A Screen pede sync no init().
        if (Minecraft.getInstance().player == null) {
            g.drawString(this.font,
                    Component.literal("§7Carregando..."),
                    x + 10, y + 22, 0xFFFFFF, false);
            return;
        }

        float dm = ClientMatterProfileCache.dark();
        float wm = ClientMatterProfileCache.white();
        float ym = ClientMatterProfileCache.yellow();

        g.drawString(this.font, Component.literal("§dSeu perfil de matéria:"),
                x + 10, y + 19, 0xFFFFFF, false);

        // 3 barras DM/WM/YM
        drawMatterRow(g, x + 10, y + 30, "DM", dm, 0xFF8B40D8);
        drawMatterRow(g, x + 10, y + 42, "WM", wm, 0xFFE6E6FF);
        drawMatterRow(g, x + 10, y + 54, "YM", ym, 0xFFFFD23F);

        // Tipo ativo + recomendação
        MatterProfileType type = ClientMatterProfileCache.activeType();
        String typeStr = "» " + (type == null ? "NENHUM" : type.name());
        g.drawString(this.font, Component.literal(typeStr).withStyle(ChatFormatting.AQUA),
                x + 10, y + 65, 0xFFFFFF, false);

        // Recomendação simples
        String rec = recommendCure(dm, wm, ym);
        if (rec != null) {
            g.drawString(this.font, Component.literal("§7Sugestão: " + rec),
                    x + 10, y + 73, 0xCCCCCC, false);
        }
    }

    /** Heurística simples: qual matter está mais alto → recomenda a pílula correspondente. */
    private String recommendCure(float d, float w, float y) {
        float max = Math.max(d, Math.max(w, y));
        if (max < 20f) return "Perfil estável.";
        if (max == d) return "Use Dark Matter Pill.";
        if (max == w) return "Use Clear Matter Pill.";
        return "Use Yellow Matter Pill.";
    }

    // ─────────────────────────────────────────────────── helpers (compartilhados)
    private void drawMatterRow(GuiGraphics g, int x, int y, String label, float value, int color) {
        g.drawString(this.font, Component.literal(label), x, y, 0xFFCCFF, false);
        int bx = x + 18, by = y + 1;
        int bw = 80, bh = 7;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF000000);
        g.fill(bx, by, bx + bw, by + bh, 0xFF101020);
        int filled = (int) (bw * Math.min(1f, value / 100f));
        if (filled > 0) {
            g.fill(bx, by, bx + filled, by + bh, color);
            g.fill(bx, by, bx + filled, by + 1, 0xFFFFFFFF);
        }
        String txt = String.format("%.0f", value);
        g.drawString(this.font, Component.literal(txt), bx + bw + 4, y, 0xCCCCCC, false);
    }

    private void slot(GuiGraphics g, int x, int y, int hi) {
        g.fill(x, y, x + 18, y + 1, 0xFF2A0D44);
        g.fill(x, y, x + 1, y + 18, 0xFF2A0D44);
        g.fill(x, y + 17, x + 18, y + 18, hi);
        g.fill(x + 17, y, x + 18, y + 18, hi);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF080018);
    }

    // (renderSlot é private em AbstractContainerScreen, então não dá pra
    // override. Em vez disso cobrimos o slot pintando um retângulo opaco
    // por cima depois do super.render() — ver método render() abaixo.)

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(this.font,
                Component.translatable("container.liberthia.matter_analyzer")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        // v0.1.20 fix: label "Inventory" tava sobrepondo com conteúdo das tabs
        // Curas/Perfil (que escrevem até y+73, e o label fica em y+72). Só
        // mostra o label "Inventory" na tab Análise — nas outras tabs o player
        // ainda vê os slots do inventário sem precisar do título.
        if (activeTab == Tab.ANALYZE) {
            g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);
        }

        // Linha de status só na tab ANÁLISE (continua mostrando o item inserido)
        if (activeTab == Tab.ANALYZE) {
            ItemStack input = menu.getInputStack();
            if (input.isEmpty()) {
                g.drawString(this.font, Component.literal("[ Insira uma amostra ]")
                        .withStyle(ChatFormatting.DARK_GRAY), 60, 62, 0xFFFFFF, false);
            } else {
                MatterContent c = menu.currentContent();
                MatterContent.Mutation mut = c.dominantMutation();
                String name = input.getHoverName().getString();
                g.drawString(this.font, Component.literal(truncate(name, 16))
                        .withStyle(ChatFormatting.AQUA), 8, 62, 0xFFFFFF, false);
                String muText = "» " + mut.displayName;
                g.drawString(this.font, Component.literal(muText).withStyle(mut.color),
                        60, 62, 0xFFFFFF, false);
                int muWidth = this.font.width(muText);
                g.drawString(this.font,
                        Component.literal("  " + c.energyEquivalent() + " FE-eq")
                                .withStyle(ChatFormatting.GOLD),
                        60 + muWidth, 62, 0xFFFFFF, false);
            }
        }
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);

        // v0.1.20 fix: nas tabs Curas/Perfil, RE-RENDERIZA o conteúdo da tab
        // por cima do que super.render() desenhou (que inclui o item do slot).
        // Antes tentava cobrir só a área do slot com retângulo opaco mas isso
        // cobria texto/ícones legítimos das tabs (bug reportado: "bloco preto
        // no meio + textos sobrepostos").
        //
        // A re-renderização tira o item visualmente sem afetar layout — o
        // slot continua funcional no menu pra player interagir, mas o item
        // não desenha em cima do conteúdo.
        if (activeTab != Tab.ANALYZE) {
            int x = (this.width - imageWidth) / 2;
            int y = (this.height - imageHeight) / 2;
            // Re-desenha conteúdo da tab por cima do item
            switch (activeTab) {
                case CURES   -> renderCuresTab(g, x, y);
                case PROFILE -> renderProfileTab(g, x, y);
                default      -> {}
            }
        }

        // tooltip da tela: só na tab ANÁLISE
        if (activeTab == Tab.ANALYZE) {
            int x = (this.width - imageWidth) / 2;
            int y = (this.height - imageHeight) / 2;
            if (mx >= x + 60 && mx < x + 170 && my >= y + 18 && my < y + 74 && !menu.getInputStack().isEmpty()) {
                MatterContent.Mutation mut = menu.currentContent().dominantMutation();
                g.renderComponentTooltip(this.font, List.of(
                        Component.literal(mut.displayName).withStyle(mut.color, ChatFormatting.BOLD),
                        Component.literal(mut.description).withStyle(ChatFormatting.GRAY)
                ), mx, my);
            }
        }
        renderTooltip(g, mx, my);
    }
}
