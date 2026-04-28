package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.WorkerTeleporterTargetC2SPacket;
import br.com.murilo.liberthia.util.MarkedPlayerEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Client-only. Lists the online players; clicking one teleports the caller
 * to that player (server-side validation in {@link WorkerTeleporterTargetC2SPacket}).
 */
public class WorkerTeleporterScreen extends Screen {
    private final List<MarkedPlayerEntry> all;
    private List<MarkedPlayerEntry> filtered;
    private EditBox search;

    public WorkerTeleporterScreen(List<MarkedPlayerEntry> players) {
        super(Component.literal("Pedra de Teletransporte"));
        this.all = new ArrayList<>(players);
        this.filtered = new ArrayList<>(this.all);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int centerX = this.width / 2;

        this.search = new EditBox(this.font, centerX - 110, 32, 220, 18,
                Component.literal("Buscar..."));
        this.search.setResponder(this::onSearch);
        this.addRenderableWidget(this.search);

        rebuild();
    }

    private void onSearch(String txt) {
        String needle = txt.toLowerCase(Locale.ROOT).trim();
        filtered.clear();
        if (needle.isEmpty()) {
            filtered.addAll(all);
        } else {
            for (MarkedPlayerEntry e : all) {
                if (e.name().toLowerCase(Locale.ROOT).contains(needle)) {
                    filtered.add(e);
                }
            }
        }
        rebuild();
    }

    private void rebuild() {
        // Clear everything except the search box, then re-add buttons.
        this.clearWidgets();
        if (this.search != null) this.addRenderableWidget(this.search);

        int centerX = this.width / 2;
        int y = 64;
        int maxY = this.height - 40;

        for (MarkedPlayerEntry entry : filtered) {
            if (y > maxY) break;
            this.addRenderableWidget(Button.builder(
                    Component.literal("TP → " + entry.name()),
                    btn -> {
                        ModNetwork.CHANNEL.sendToServer(
                                new WorkerTeleporterTargetC2SPacket(entry.uuid()));
                        this.onClose();
                    }
            ).bounds(centerX - 110, y, 220, 20).build());
            y += 22;
        }

        this.addRenderableWidget(Button.builder(Component.literal("Fechar"),
                b -> this.onClose())
                .bounds(centerX - 50, Math.min(this.height - 28, y + 6), 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        int centerX = this.width / 2;
        g.drawCenteredString(this.font, this.title, centerX, 12, 0xFFFFFF);
        if (filtered.isEmpty()) {
            g.drawCenteredString(this.font,
                    Component.literal("Nenhum jogador encontrado."),
                    centerX, 60, 0xFFAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
