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
        PROFILE("Perfil", 0xFFFFD080),
        BLOOD("Sangue", 0xFFFF5555);

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

        // Botão "Exportar" → manda a leitura pro Computador (tabs Análise/Sangue).
        if ((activeTab == Tab.ANALYZE || activeTab == Tab.BLOOD)
                && mx >= x + 114 && mx < x + 170 && my >= y + 3 && my < y + 15) {
            ModNetwork.CHANNEL.sendToServer(new br.com.murilo.liberthia.network.packet.AnalyzerToComputerC2SPacket());
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
            }
            return true;
        }

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
            case BLOOD   -> renderBloodTab(g, x, y);
        }

        // Botão "Exportar → PC" (tabs Análise/Sangue) — salva a leitura num Computador.
        if (activeTab == Tab.ANALYZE || activeTab == Tab.BLOOD) {
            int bx = x + 114, by = y + 3, bw = 56, bh = 12;
            g.fill(bx, by, bx + bw, by + bh, 0xFF000000);
            g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, 0xFF103820);
            g.drawString(this.font, Component.literal("§bSalvar PC"), bx + 7, by + 2, 0xFFFFFF, false);
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

        // Se for uma SERINGA DE SANGUE cheia, as 3 barras mostram o perfil de
        // matéria do PLAYER de quem o sangue foi tirado (análise de sangue).
        ItemStack in = menu.getInputStack();
        boolean blood = in.getItem() instanceof br.com.murilo.liberthia.item.BloodSyringeItem
                && br.com.murilo.liberthia.item.BloodSyringeItem.isFilled(in);
        if (blood) {
            drawMatterRow(g, panelX + 4, panelY + 4,  "DM", br.com.murilo.liberthia.item.BloodSyringeItem.getDark(in),   0xFF8B40D8);
            drawMatterRow(g, panelX + 4, panelY + 16, "WM", br.com.murilo.liberthia.item.BloodSyringeItem.getWhite(in),  0xFFE6E6FF);
            drawMatterRow(g, panelX + 4, panelY + 28, "YM", br.com.murilo.liberthia.item.BloodSyringeItem.getYellow(in), 0xFFFFD23F);
        } else {
            MatterContent c = menu.currentContent();
            drawMatterRow(g, panelX + 4, panelY + 4,  "DM", c.dark(),   0xFF8B40D8);
            drawMatterRow(g, panelX + 4, panelY + 16, "WM", c.white(),  0xFFE6E6FF);
            drawMatterRow(g, panelX + 4, panelY + 28, "YM", c.yellow(), 0xFFFFD23F);
        }
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

    // ─────────────────────────────────────────────────── TAB: SANGUE
    // Layout espelha a tab Análise: slot da seringa à ESQUERDA (a seringa fica
    // DENTRO do quadro, não flutuando) + painel-tela à DIREITA com os dados.
    // Como o conteúdo fica todo à direita do slot, o item nunca sobrepõe os
    // textos — por isso o blood tab NÃO entra no re-render de render().
    private void renderBloodTab(GuiGraphics g, int x, int y) {
        // Fundo avermelhado da "máquina"
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 76, 0xFF1A0606);
        for (int gy = y + 18; gy < y + 76; gy += 4)
            g.fill(x + 8, gy, x + imageWidth - 8, gy + 1, 0xFF250A0A);

        // Slot da seringa (mesma posição do menu: 24,35)
        slot(g, x + 24 - 1, y + 35 - 1, 0xFFD04040);
        g.drawString(this.font, Component.literal("§7Amostra"), x + 11, y + 58, 0xFFCCCCCC, false);

        // Painel-tela à direita (x+58..x+168) — não encosta no slot (x+22..x+42)
        int px = x + 58, py = y + 18, pw = imageWidth - 58 - 8, ph = 54;
        g.fill(px, py, px + pw, py + ph, 0xFF000000);
        g.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xFF200808);

        ItemStack in = menu.getInputStack();
        boolean blood = in.getItem() instanceof br.com.murilo.liberthia.item.BloodSyringeItem
                && br.com.murilo.liberthia.item.BloodSyringeItem.isFilled(in);
        if (!blood) {
            g.drawString(this.font, Component.literal("§cInsira uma"),        px + 6, py + 8,  0xFFFFFF, false);
            g.drawString(this.font, Component.literal("§cSeringa de Sangue"), px + 6, py + 20, 0xFFFFFF, false);
            g.drawString(this.font, Component.literal("§ccheia."),            px + 6, py + 32, 0xFFFFFF, false);
            g.drawString(this.font, Component.literal("§8(extraia de alguém)"), px + 6, py + 44, 0xCCCCCC, false);
            return;
        }

        int inf = br.com.murilo.liberthia.item.BloodSyringeItem.getStoredInfection(in);
        if (inf < 0) inf = 0;
        String src = br.com.murilo.liberthia.item.BloodSyringeItem.getSource(in);
        if (src.isEmpty()) src = "?";

        g.drawString(this.font, Component.literal("§4❤ §f" + truncate(src, 13)), px + 4, py + 3, 0xFFFFFF, false);

        int infColor = inf == 0 ? 0xFF35D85B : inf < 25 ? 0xFFFFE23F
                : inf < 50 ? 0xFFFFA030 : inf < 75 ? 0xFFFF4040 : 0xFF8B0000;
        drawMatterRowW(g, px + 4, py + 14, "INF", inf, infColor, 52);
        drawMatterRowW(g, px + 4, py + 24, "DM", br.com.murilo.liberthia.item.BloodSyringeItem.getDark(in),   0xFF8B40D8, 52);
        drawMatterRowW(g, px + 4, py + 34, "WM", br.com.murilo.liberthia.item.BloodSyringeItem.getWhite(in),  0xFFE6E6FF, 52);
        drawMatterRowW(g, px + 4, py + 44, "YM", br.com.murilo.liberthia.item.BloodSyringeItem.getYellow(in), 0xFFFFD23F, 52);
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

    /** Variante do drawMatterRow com largura de barra configurável (pra painéis estreitos). */
    private void drawMatterRowW(GuiGraphics g, int x, int y, String label, float value, int color, int bw) {
        g.drawString(this.font, Component.literal(label), x, y, 0xFFCCFF, false);
        int bx = x + 22, by = y + 1, bh = 6;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF000000);
        g.fill(bx, by, bx + bw, by + bh, 0xFF101020);
        int filled = (int) (bw * Math.min(1f, value / 100f));
        if (filled > 0) {
            g.fill(bx, by, bx + filled, by + bh, color);
            g.fill(bx, by, bx + filled, by + 1, 0xFFFFFFFF);
        }
        g.drawString(this.font, Component.literal(String.format("%.0f", value)), bx + bw + 3, y, 0xCCCCCC, false);
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
            } else if (input.getItem() instanceof br.com.murilo.liberthia.item.BloodSyringeItem
                    && br.com.murilo.liberthia.item.BloodSyringeItem.isFilled(input)) {
                int inf = br.com.murilo.liberthia.item.BloodSyringeItem.getStoredInfection(input);
                String src = br.com.murilo.liberthia.item.BloodSyringeItem.getSource(input);
                if (src.isEmpty()) src = "?";
                ChatFormatting col = inf < 0 ? ChatFormatting.GRAY
                        : inf == 0 ? ChatFormatting.GREEN
                        : inf < 25 ? ChatFormatting.YELLOW
                        : inf < 50 ? ChatFormatting.GOLD
                        : inf < 75 ? ChatFormatting.RED : ChatFormatting.DARK_RED;
                g.drawString(this.font, Component.literal("§4❤ " + truncate(src, 10))
                        .withStyle(ChatFormatting.RED), 8, 62, 0xFFFFFF, false);
                g.drawString(this.font, Component.literal("Infecção: " + (inf < 0 ? "?" : inf + "%"))
                        .withStyle(col), 70, 62, 0xFFFFFF, false);
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
        // BLOOD não entra aqui: seu conteúdo fica todo à direita do slot, então
        // a seringa pode aparecer normalmente dentro do quadro (sem flutuar).
        if (activeTab == Tab.CURES || activeTab == Tab.PROFILE) {
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
