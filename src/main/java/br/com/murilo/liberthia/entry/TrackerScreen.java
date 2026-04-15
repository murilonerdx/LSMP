package br.com.murilo.liberthia.entry;

import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.UUID;

public class TrackerScreen extends Screen {
    private static final int W = 200, H = 100;
    private final UUID targetId;
    private String name;
    private double x, y, z;
    private String dimension = "?";
    private boolean signalLost = true;
    private int refreshTimer = 0;

    public TrackerScreen(UUID targetId, String name) {
        super(Component.literal("Rastreador"));
        this.targetId = targetId;
        this.name = name;
    }

    @Override
    protected void init() {
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Atualizar"), b ->
                ModNetwork.CHANNEL.sendToServer(new TrackerC2SPacket(targetId)))
                .bounds(left + W - 65, top + H - 24, 60, 18).build());
    }

    public void updateData(String name, double x, double y, double z, String dimension, boolean signalLost) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.signalLost = signalLost;
    }

    @Override
    public void tick() {
        refreshTimer++;
        if (refreshTimer % 40 == 0) {
            ModNetwork.CHANNEL.sendToServer(new TrackerC2SPacket(targetId));
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        this.renderBackground(gfx);
        int left = (width - W) / 2;
        int top = (height - H) / 2;

        // Background
        gfx.fill(left, top, left + W, top + H, 0xDD0A1A2E);
        gfx.fill(left, top, left + W, top + 1, 0xFF00AAFF);
        gfx.fill(left, top + H - 1, left + W, top + H, 0xFF00AAFF);
        gfx.fill(left, top, left + 1, top + H, 0xFF00AAFF);
        gfx.fill(left + W - 1, top, left + W, top + H, 0xFF00AAFF);

        gfx.drawString(this.font, "RASTREADOR", left + 5, top + 4, 0xFF00DDFF, false);

        if (signalLost) {
            gfx.drawString(this.font, "Alvo: " + name, left + 5, top + 18, 0xFFAAAAAA, false);
            gfx.drawString(this.font, "SINAL PERDIDO", left + 5, top + 34, 0xFFFF4444, false);
        } else {
            gfx.drawString(this.font, "Alvo: " + name, left + 5, top + 18, 0xFF55FF55, false);
            String pos = String.format(Locale.ROOT, "X: %.1f  Y: %.1f  Z: %.1f", x, y, z);
            gfx.drawString(this.font, pos, left + 5, top + 32, 0xFFFFFFFF, false);
            gfx.drawString(this.font, "Dim: " + dimension, left + 5, top + 44, 0xFFBBBBBB, false);
        }

        super.render(gfx, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
