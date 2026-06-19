package br.com.murilo.liberthia.client.gui;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SetWalkieFreqC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Tela do Walkie Talkie — digita o código secreto (frequência) e liga/desliga.
 */
public class WalkieTalkieScreen extends Screen {

    private final String initialFreq;
    private boolean on;

    private EditBox freqBox;
    private Button toggleBtn;

    private WalkieTalkieScreen(String initialFreq, boolean on) {
        super(Component.translatable("screen.liberthia.walkie_talkie"));
        this.initialFreq = initialFreq == null ? "" : initialFreq;
        this.on = on;
    }

    public static void openNow(String freq, boolean on) {
        Minecraft.getInstance().setScreen(new WalkieTalkieScreen(freq, on));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;

        freqBox = new EditBox(this.font, cx - 120, cy - 10, 240, 20,
                Component.translatable("screen.liberthia.walkie_talkie.code"));
        freqBox.setMaxLength(24);
        freqBox.setValue(initialFreq);
        freqBox.setBordered(true);
        addRenderableWidget(freqBox);
        setInitialFocus(freqBox);

        toggleBtn = Button.builder(toggleLabel(), b -> {
                    on = !on;
                    toggleBtn.setMessage(toggleLabel());
                })
                .bounds(cx - 120, cy - 40, 240, 22)
                .build();
        addRenderableWidget(toggleBtn);

        addRenderableWidget(Button.builder(Component.literal("§a✓ Salvar"), b -> save())
                .bounds(cx - 102, cy + 18, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("§c✗ Cancelar"), b -> onClose())
                .bounds(cx + 2, cy + 18, 100, 20)
                .build());
    }

    private Component toggleLabel() {
        return Component.literal(on ? "§a§l● LIGADO" : "§8§l○ Desligado");
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new SetWalkieFreqC2SPacket(freqBox.getValue(), on));
        onClose();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 257 || key == 335) { // Enter
            save();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        int cx = this.width / 2;
        int cy = this.height / 2;
        g.drawCenteredString(this.font, Component.literal("§b§l📻 Walkie Talkie"),
                cx, cy - 70, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.literal("§7Código secreto do canal (só quem tiver o mesmo escuta)"),
                cx, cy + 50, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
