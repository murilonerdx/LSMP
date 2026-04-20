package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.TeleportExecutorActionC2SPacket;
import br.com.murilo.liberthia.util.MarkedPlayerEntry;
import br.com.murilo.liberthia.util.TeleportAnchor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public class TeleportExecutorScreen extends Screen {
    private final TeleportAnchor anchor;
    private final List<MarkedPlayerEntry> entries;

    public TeleportExecutorScreen(TeleportAnchor anchor, List<MarkedPlayerEntry> entries) {
        super(Component.literal("Executor de Teleporte"));
        this.anchor = anchor;
        this.entries = new ArrayList<>(entries);
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int centerX = this.width / 2;
        int startY = 40;

        this.addRenderableWidget(Button.builder(Component.literal("Teleportar todos"), button -> {
            ModNetwork.CHANNEL.sendToServer(new TeleportExecutorActionC2SPacket(TeleportExecutorActionC2SPacket.Action.TELEPORT_ALL, null));
        }).bounds(centerX - 110, startY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Limpar todos"), button -> {
            ModNetwork.CHANNEL.sendToServer(new TeleportExecutorActionC2SPacket(TeleportExecutorActionC2SPacket.Action.CLEAR_ALL, null));
            this.entries.clear();
            this.rebuildButtons();
        }).bounds(centerX + 10, startY, 100, 20).build());

        this.rebuildButtons();
    }

    private void rebuildButtons() {
        this.clearWidgets();
        int centerX = this.width / 2;
        int startY = 40;

        this.addRenderableWidget(Button.builder(Component.literal("Teleportar todos"), button -> {
            ModNetwork.CHANNEL.sendToServer(new TeleportExecutorActionC2SPacket(TeleportExecutorActionC2SPacket.Action.TELEPORT_ALL, null));
        }).bounds(centerX - 110, startY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Limpar todos"), button -> {
            ModNetwork.CHANNEL.sendToServer(new TeleportExecutorActionC2SPacket(TeleportExecutorActionC2SPacket.Action.CLEAR_ALL, null));
            this.entries.clear();
            this.rebuildButtons();
        }).bounds(centerX + 10, startY, 100, 20).build());

        int y = startY + 34;
        for (MarkedPlayerEntry entry : entries) {
            this.addRenderableWidget(Button.builder(Component.literal("TP " + entry.name()), button -> {
                ModNetwork.CHANNEL.sendToServer(new TeleportExecutorActionC2SPacket(TeleportExecutorActionC2SPacket.Action.TELEPORT_ONE, entry.uuid()));
            }).bounds(centerX - 110, y, 160, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("X"), button -> {
                ModNetwork.CHANNEL.sendToServer(new TeleportExecutorActionC2SPacket(TeleportExecutorActionC2SPacket.Action.UNMARK_ONE, entry.uuid()));
                this.entries.remove(entry);
                this.rebuildButtons();
            }).bounds(centerX + 56, y, 54, 20).build());

            y += 24;
        }

        this.addRenderableWidget(Button.builder(Component.literal("Fechar"), button -> this.onClose())
                .bounds(centerX - 50, Math.min(this.height - 28, y + 8), 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 12, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.literal(String.format("Destino: %s | X: %.1f Y: %.1f Z: %.1f",
                        anchor.dimension().location(), anchor.x(), anchor.y(), anchor.z())),
                centerX,
                24,
                0xAAAAFF);

        if (entries.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.literal("Nenhum player marcado."), centerX, 78, 0xFFAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
