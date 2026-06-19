package br.com.murilo.liberthia.client.gui;

import br.com.murilo.liberthia.client.texture.ImageUrlTextureCache;
import br.com.murilo.liberthia.item.computer.ComputerData;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SaveComputerC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tela do Computador — terminal "LIBERTHIA OS". Arquivos à esquerda, editor à
 * direita. Arquivos de <b>texto</b> (editor multi-linha) ou <b>foto</b> (campo
 * de URL + preview da imagem, via {@link ImageUrlTextureCache}).
 */
public class ComputerScreen extends Screen {

    private static final int BG = 0xF00A0E0A;
    private static final int PANEL = 0xFF0E1A0E;
    private static final int ACCENT = 0xFF35D85B;
    private static final int DIM = 0xFF1E3A1E;

    private final List<ComputerData.Entry> files;
    private int selected;
    private int scroll = 0;

    private EditBox nameBox;
    private MultiLineEditBox bodyBox; // texto
    private EditBox urlBox;           // foto

    private ComputerScreen(List<ComputerData.Entry> files) {
        super(Component.literal("LIBERTHIA OS"));
        this.files = files;
        this.selected = files.isEmpty() ? -1 : 0;
    }

    public static void openNow(CompoundTag data) {
        List<ComputerData.Entry> files = ComputerData.fromList(ComputerData.unwrap(data));
        Minecraft.getInstance().setScreen(new ComputerScreen(new ArrayList<>(files)));
    }

    private int listX() { return 14; }
    private int listW() { return 130; }
    private int listTop() { return 40; }
    private int rowH() { return 18; }
    private int rightX() { return listX() + listW() + 12; }
    private int rightW() { return this.width - rightX() - 14; }
    private int maxRows() { return Math.max(3, (this.height - listTop() - 70) / rowH()); }

    private ComputerData.Entry sel() {
        return (selected >= 0 && selected < files.size()) ? files.get(selected) : null;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.nameBox = null;
        this.bodyBox = null;
        this.urlBox = null;

        ComputerData.Entry e = sel();
        if (e != null) {
            this.nameBox = new EditBox(this.font, rightX(), listTop(), rightW(), 18,
                    Component.literal("nome"));
            this.nameBox.setMaxLength(ComputerData.MAX_NAME);
            this.nameBox.setHint(Component.literal("nome do arquivo"));
            this.nameBox.setValue(e.name);
            this.addRenderableWidget(this.nameBox);

            if (e.isPhoto()) {
                this.urlBox = new EditBox(this.font, rightX(), listTop() + 22, rightW(), 18,
                        Component.literal("url"));
                this.urlBox.setMaxLength(512);
                this.urlBox.setHint(Component.literal("cole a URL da imagem (https://...)"));
                this.urlBox.setValue(e.body);
                this.addRenderableWidget(this.urlBox);
            } else {
                int bodyY = listTop() + 24;
                int bodyH = this.height - bodyY - 40;
                this.bodyBox = new MultiLineEditBox(this.font, rightX(), bodyY, rightW(), bodyH,
                        Component.literal("conteúdo..."), Component.literal("body"));
                this.bodyBox.setCharacterLimit(ComputerData.MAX_BODY);
                this.bodyBox.setValue(e.body);
                this.addRenderableWidget(this.bodyBox);
            }
        }

        // Lista de arquivos.
        int maxRows = maxRows();
        if (scroll > Math.max(0, files.size() - maxRows)) scroll = Math.max(0, files.size() - maxRows);
        int y = listTop();
        for (int i = scroll; i < files.size() && i < scroll + maxRows; i++) {
            final int idx = i;
            ComputerData.Entry f = files.get(i);
            String nm = f.name.isEmpty() ? "(sem nome)" : f.name;
            if (nm.length() > 16) nm = nm.substring(0, 15) + "…";
            String mark = f.isPhoto() ? "§b▣ " : "§7▤ ";
            String label = (i == selected ? "§a> " : mark) + nm;
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> selectFile(idx))
                    .bounds(listX(), y, listW(), rowH() - 2).build());
            y += rowH();
        }

        if (files.size() > maxRows) {
            this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> { if (scroll > 0) { scroll--; rebuildWidgets(); } })
                    .bounds(listX(), this.height - 64, 24, 18).build());
            this.addRenderableWidget(Button.builder(Component.literal("▼"), b -> { scroll++; rebuildWidgets(); })
                    .bounds(listX() + 28, this.height - 64, 24, 18).build());
        }

        // + Novo (texto) / + Foto
        this.addRenderableWidget(Button.builder(Component.literal("§a+ Texto"), b -> newFile(ComputerData.TYPE_TEXT))
                .bounds(listX(), this.height - 42, listW() / 2 - 2, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("§b+ Foto"), b -> newFile(ComputerData.TYPE_PHOTO))
                .bounds(listX() + listW() / 2 + 2, this.height - 42, listW() / 2 - 2, 18).build());

        // Bottom: Salvar / Apagar / Fechar
        int by = this.height - 22;
        this.addRenderableWidget(Button.builder(Component.literal("§aSalvar"), b -> save())
                .bounds(rightX(), by, 80, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("§cApagar"), b -> deleteFile())
                .bounds(rightX() + 86, by, 80, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Fechar"), b -> onClose())
                .bounds(rightX() + 172, by, 80, 18).build());
    }

    private void commit() {
        ComputerData.Entry e = sel();
        if (e == null) return;
        if (nameBox != null) e.name = nameBox.getValue();
        if (e.isPhoto()) {
            if (urlBox != null) e.body = urlBox.getValue();
        } else if (bodyBox != null) {
            e.body = bodyBox.getValue();
        }
    }

    private void selectFile(int idx) {
        commit();
        selected = idx;
        rebuildWidgets();
    }

    private void newFile(int type) {
        commit();
        if (files.size() >= ComputerData.MAX_FILES) return;
        String base = type == ComputerData.TYPE_PHOTO ? "foto_" : "arquivo_";
        files.add(new ComputerData.Entry(base + (files.size() + 1), "", type));
        selected = files.size() - 1;
        scroll = Math.max(0, files.size() - maxRows());
        rebuildWidgets();
    }

    private void deleteFile() {
        if (sel() == null) return;
        files.remove(selected);
        if (selected >= files.size()) selected = files.size() - 1;
        rebuildWidgets();
    }

    private void save() {
        commit();
        ModNetwork.CHANNEL.sendToServer(new SaveComputerC2SPacket(
                ComputerData.wrap(ComputerData.toList(files))));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.fill(0, 0, this.width, this.height, BG);
        g.fill(listX() - 4, listTop() - 8, listX() + listW() + 4, this.height - 26, PANEL);
        super.render(g, mx, my, pt);
        g.drawString(this.font, "§a§l▌ LIBERTHIA OS", listX() - 2, 14, ACCENT, false);
        g.drawString(this.font, "§7arquivos", listX(), listTop() - 12, DIM, false);

        ComputerData.Entry e = sel();
        if (e == null) {
            g.drawCenteredString(this.font, "§8Crie um arquivo: + Texto ou + Foto",
                    this.width / 2 + 40, this.height / 2, 0xFF888888);
            return;
        }

        // Preview de foto.
        if (e.isPhoto() && urlBox != null) {
            String url = urlBox.getValue().trim();
            int px = rightX(), py = listTop() + 46;
            int maxW = rightW(), maxH = this.height - py - 30;
            if (url.isEmpty()) {
                g.drawString(this.font, "§8Cole a URL da imagem no campo acima.", px, py, 0xFF888888, false);
            } else {
                Optional<ImageUrlTextureCache.LoadedImage> img = ImageUrlTextureCache.get(url);
                if (img.isPresent()) {
                    ImageUrlTextureCache.LoadedImage im = img.get();
                    float ratio = im.width() / (float) Math.max(1, im.height());
                    int dw = maxW, dh = (int) (dw / ratio);
                    if (dh > maxH) { dh = maxH; dw = (int) (dh * ratio); }
                    g.blit(im.location(), px, py, dw, dh, 0, 0, im.width(), im.height(), im.width(), im.height());
                } else {
                    g.drawString(this.font, "§7Carregando imagem...", px, py, 0xAAAAAA, false);
                }
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
