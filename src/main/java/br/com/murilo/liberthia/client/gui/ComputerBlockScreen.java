package br.com.murilo.liberthia.client.gui;

import br.com.murilo.liberthia.item.computer.ComputerData;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.ComputerConfigC2SPacket;
import br.com.murilo.liberthia.network.packet.ComputerLoginC2SPacket;
import br.com.murilo.liberthia.network.packet.ComputerPrintC2SPacket;
import br.com.murilo.liberthia.network.packet.ComputerHdSlotC2SPacket;
import br.com.murilo.liberthia.storage.ComputerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Tela do Computador (bloco) — "LIBERTHIA OS". Mostra os relatórios salvos
 * (DNA/nome/sangue/matéria), permite imprimir num livro (com Impressora ao lado),
 * gravar/ler num HD, e — pro dono — configurar login com senha.
 */
@OnlyIn(Dist.CLIENT)
public class ComputerBlockScreen extends Screen {

    private static final int GUI_W = 256;
    private static final int GUI_H = 184;

    private final BlockPos pos;
    private final boolean locked;
    private final boolean owner;
    private final int energy;
    private final int maxEnergy;
    private final List<ComputerData.Entry> files;

    private int left, top;
    private int selected = -1;
    private int scroll = 0;
    private boolean configMode = false;
    private boolean cfgLogin = false;

    private EditBox passBox;

    public ComputerBlockScreen(BlockPos pos, boolean locked, boolean owner, boolean loginEnabled,
                               int energy, int maxEnergy, List<ComputerData.Entry> files) {
        super(Component.literal("LIBERTHIA OS"));
        this.pos = pos;
        this.locked = locked;
        this.owner = owner;
        this.cfgLogin = loginEnabled; // reflete o estado salvo no bloco
        this.energy = energy;
        this.maxEnergy = maxEnergy;
        this.files = files;
        if (!files.isEmpty()) selected = 0;
    }

    /** Chamado pelo packet S2C (client thread). */
    public static void open(BlockPos pos, boolean locked, boolean owner, boolean loginEnabled,
                            int energy, int maxEnergy, CompoundTag data) {
        List<ComputerData.Entry> files = ComputerData.fromList(ComputerData.unwrap(data));
        Minecraft.getInstance().setScreen(new ComputerBlockScreen(pos, locked, owner, loginEnabled, energy, maxEnergy, files));
    }

    @Override
    protected void init() {
        left = (this.width - GUI_W) / 2;
        top = (this.height - GUI_H) / 2;

        if (locked) {
            passBox = new EditBox(font, left + GUI_W / 2 - 80, top + 84, 160, 18, Component.literal("senha"));
            passBox.setMaxLength(64);
            addRenderableWidget(passBox);
            addRenderableWidget(Button.builder(Component.literal("Entrar"), b -> submitLogin())
                    .bounds(left + GUI_W / 2 - 35, top + 110, 70, 20).build());
            setInitialFocus(passBox);
            return;
        }

        if (configMode) {
            addRenderableWidget(Button.builder(loginLabel(), b -> {
                cfgLogin = !cfgLogin;
                rebuildWidgets();
            }).bounds(left + 28, top + 56, GUI_W - 56, 20).build());

            passBox = new EditBox(font, left + 28, top + 92, GUI_W - 56, 18,
                    Component.literal("senha (vazio = sem senha)"));
            passBox.setMaxLength(64);
            passBox.setHint(Component.literal("senha..."));
            addRenderableWidget(passBox);

            addRenderableWidget(Button.builder(Component.literal("Salvar"), b -> submitConfig())
                    .bounds(left + 28, top + 122, (GUI_W - 56) / 2 - 4, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Voltar"), b -> {
                configMode = false;
                rebuildWidgets();
            }).bounds(left + 28 + (GUI_W - 56) / 2 + 4, top + 122, (GUI_W - 56) / 2 - 4, 20).build());
            return;
        }

        // ── Visão principal ──
        if (owner) {
            addRenderableWidget(Button.builder(Component.literal("Config"), b -> {
                configMode = true;
                rebuildWidgets();
            }).bounds(left + GUI_W - 62, top + 16, 54, 16).build());
        }
        // O HD é o armazenamento — não há mais "gravar/ler HD" (basta pôr/tirar o
        // HD na bay abaixo). Só imprimir continua aqui.
        int by = top + GUI_H - 26;
        addRenderableWidget(Button.builder(Component.literal("Imprimir"), b -> print(false))
                .bounds(left + 132, by, 56, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Imprimir Tudo"), b -> print(true))
                .bounds(left + 192, by, 56, 20).build());
    }

    private Component loginLabel() {
        return Component.literal("Login: " + (cfgLogin ? "§aLIGADO" : "§cDESLIGADO"));
    }

    // ── Ações ────────────────────────────────────────────────────────────────
    private void submitLogin() {
        if (passBox != null) ModNetwork.sendToServer(new ComputerLoginC2SPacket(pos, passBox.getValue()));
    }
    private void submitConfig() {
        ModNetwork.sendToServer(new ComputerConfigC2SPacket(pos, cfgLogin, passBox == null ? "" : passBox.getValue()));
    }
    private void print(boolean all) {
        ModNetwork.sendToServer(new ComputerPrintC2SPacket(pos, all ? -1 : selected));
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        // Painel
        g.fill(left, top, left + GUI_W, top + GUI_H, 0xFF0A0A12);
        drawBorder(g, left, top, GUI_W, GUI_H, 0xFF2A8A4A, 2);
        g.fill(left, top, left + GUI_W, top + 14, 0xFF0F2A18);
        // scanlines
        for (int sy = top + 16; sy < top + GUI_H - 28; sy += 4)
            g.fill(left + 2, sy, left + GUI_W - 2, sy + 1, 0x14000000 | 0x0020FF20);

        g.drawString(font, "§a> LIBERTHIA OS §2v2", left + 6, top + 4, 0xFF55FF99, false);
        // Energia (FE) no canto direito da barra de título.
        int ecol = energy <= 0 ? 0xFFFF5555
                : (energy < ComputerBlockEntity.OPEN_COST ? 0xFFFFAA55 : 0xFF66DDFF);
        String estr = energy + "/" + maxEnergy + " FE";
        g.drawString(font, estr, left + GUI_W - 6 - font.width(estr), top + 4, ecol, false);

        if (locked) {
            g.drawString(font, "§c[ ACESSO RESTRITO ]", left + GUI_W / 2 - 56, top + 40, 0xFFFF5555, false);
            g.drawString(font, "§7Digite a senha:", left + GUI_W / 2 - 80, top + 70, 0xFFAAAAAA, false);
            super.render(g, mx, my, pt);
            return;
        }

        if (configMode) {
            g.drawString(font, "§e⚙ Configuração", left + 28, top + 36, 0xFFFFEE88, false);
            g.drawString(font, "§7Senha (deixe vazio = sem senha):", left + 28, top + 82, 0xFF999999, false);
            super.render(g, mx, my, pt);
            return;
        }

        // Header: dono/contagem
        g.drawString(font, "§7Arquivos: §b" + files.size() + "§7/§b" + ComputerData.MAX_FILES,
                left + 8, top + 18, 0xFFAAAAAA, false);

        // Lista de arquivos (esquerda)
        int listX = left + 6, listTop = top + 30, listW = 92, rowH = 12, visible = 9;
        g.fill(listX, listTop - 1, listX + listW, listTop + visible * rowH + 1, 0xFF06120A);
        drawBorder(g, listX, listTop - 1, listW, visible * rowH + 2, 0xFF1E5E36, 1);
        for (int i = 0; i < visible; i++) {
            int idx = scroll + i;
            if (idx >= files.size()) break;
            int ry = listTop + i * rowH;
            boolean sel = idx == selected;
            if (sel) g.fill(listX + 1, ry, listX + listW - 1, ry + rowH, 0xFF12451F);
            String nm = truncate(files.get(idx).name, 14);
            g.drawString(font, (sel ? "§a> " : "§7") + nm, listX + 3, ry + 2, sel ? 0xFFAAFFAA : 0xFF88BB99, false);
        }

        // Corpo do arquivo (direita)
        int bodyX = left + 104, bodyTop = top + 30, bodyW = GUI_W - 104 - 8, bodyBottom = top + GUI_H - 30;
        g.fill(bodyX - 2, bodyTop - 1, left + GUI_W - 6, bodyBottom, 0xFF06120A);
        drawBorder(g, bodyX - 2, bodyTop - 1, GUI_W - 104 - 4, bodyBottom - bodyTop + 1, 0xFF1E5E36, 1);
        if (selected >= 0 && selected < files.size()) {
            ComputerData.Entry e = files.get(selected);
            g.drawString(font, "§a" + truncate(e.name, 22), bodyX, bodyTop + 1, 0xFF66FF99, false);
            List<FormattedCharSequence> lines = font.split(Component.literal("§7" + e.body), bodyW);
            int ly = bodyTop + 13;
            for (FormattedCharSequence line : lines) {
                if (ly > bodyBottom - 10) break;
                g.drawString(font, line, bodyX, ly, 0xFFAACCAA, false);
                ly += 9;
            }
        } else {
            ComputerBlockEntity cbe0 = clientBe();
            boolean noHd = cbe0 == null || cbe0.getHd().isEmpty();
            if (noHd) {
                g.drawString(font, "§cSem HD.", bodyX, bodyTop + 4, 0xFFFF7777, false);
                g.drawString(font, "§8Insira um HD na", bodyX, bodyTop + 16, 0xFF668866, false);
                g.drawString(font, "§8bay abaixo pra ver", bodyX, bodyTop + 26, 0xFF668866, false);
                g.drawString(font, "§8e salvar relatórios.", bodyX, bodyTop + 36, 0xFF668866, false);
            } else {
                g.drawString(font, "§8Sem dados.", bodyX, bodyTop + 4, 0xFF668866, false);
                g.drawString(font, "§8Use o Matter", bodyX, bodyTop + 16, 0xFF668866, false);
                g.drawString(font, "§8Analyzer ao lado.", bodyX, bodyTop + 26, 0xFF668866, false);
            }
        }

        // ── Bay do HD (sob a lista) — clique pra pôr/tirar ──
        int hx = left + 8, hy = top + 140;
        g.fill(hx - 1, hy - 1, hx + 17, hy + 17, 0xFF2A8A4A);
        g.fill(hx, hy, hx + 16, hy + 16, 0xFF06120A);
        ComputerBlockEntity cbe = clientBe();
        ItemStack hd = cbe != null ? cbe.getHd() : ItemStack.EMPTY;
        if (!hd.isEmpty()) {
            g.renderItem(hd, hx, hy);
            int cnt = br.com.murilo.liberthia.item.HardDriveItem.readFiles(hd).size();
            g.drawString(font, "§bHD §7(" + cnt + " arq)", hx + 21, hy + 1, 0xFFAACCAA, false);
            g.drawString(font, "§8click: tirar", hx + 21, hy + 9, 0xFF668866, false);
        } else {
            g.drawString(font, "§8HD: vazio", hx + 21, hy + 1, 0xFF668866, false);
            g.drawString(font, "§8click(+HD): pôr", hx + 21, hy + 9, 0xFF556655, false);
        }

        super.render(g, mx, my, pt);
    }

    private ComputerBlockEntity clientBe() {
        var lvl = net.minecraft.client.Minecraft.getInstance().level;
        if (lvl != null && lvl.getBlockEntity(pos) instanceof ComputerBlockEntity c) return c;
        return null;
    }

    @Override
    public boolean mouseClicked(double mxd, double myd, int button) {
        if (!locked && !configMode) {
            int listX = left + 6, listTop = top + 30, listW = 92, rowH = 12, visible = 9;
            if (mxd >= listX && mxd < listX + listW && myd >= listTop && myd < listTop + visible * rowH) {
                int idx = scroll + (int) ((myd - listTop) / rowH);
                if (idx >= 0 && idx < files.size()) {
                    selected = idx;
                    return true;
                }
            }
            // Bay do HD: clique põe (se tem HD na mão) ou tira
            int hx = left + 8, hy = top + 140;
            if (mxd >= hx && mxd < hx + 16 && myd >= hy && myd < hy + 16) {
                ComputerBlockEntity cbe = clientBe();
                boolean present = cbe != null && !cbe.getHd().isEmpty();
                ModNetwork.sendToServer(new ComputerHdSlotC2SPacket(pos, present));
                return true;
            }
        }
        return super.mouseClicked(mxd, myd, button);
    }

    @Override
    public boolean mouseScrolled(double mxd, double myd, double delta) {
        if (!locked && !configMode && files.size() > 9) {
            scroll = Math.max(0, Math.min(files.size() - 9, scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mxd, myd, delta);
    }

    private void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color, int t) {
        g.fill(x, y, x + w, y + t, color);
        g.fill(x, y + h - t, x + w, y + h, color);
        g.fill(x, y, x + t, y + h, color);
        g.fill(x + w - t, y, x + w, y + h, color);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
