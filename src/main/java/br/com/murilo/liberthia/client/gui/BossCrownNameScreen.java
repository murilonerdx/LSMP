package br.com.murilo.liberthia.client.gui;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SetBossCrownNameC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * v0.1.22 r3: tela única do Boss Crown — combina toggle Ativo/Inativo +
 * EditBox do nome da bossbar. Botão Salvar envia ambos pro server.
 */
public class BossCrownNameScreen extends Screen {
    private final String initialName;
    private boolean active;

    private EditBox nameBox;
    private Button toggleBtn;

    private BossCrownNameScreen(String initialName, boolean active) {
        super(Component.literal("Coroa do Boss"));
        this.initialName = initialName == null ? "" : initialName;
        this.active = active;
    }

    /** Abre a tela no client thread. Chamado pelo packet S2C handler. */
    public static void openNow(String currentName, boolean active) {
        LiberthiaMod.LOGGER.info("[BossCrown screen] openNow name='{}' active={}",
                currentName, active);
        Minecraft.getInstance().setScreen(new BossCrownNameScreen(currentName, active));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int cy = this.height / 2;

        // EditBox pro nome
        nameBox = new EditBox(this.font, cx - 120, cy - 10, 240, 20,
                Component.literal("Nome da Bossbar"));
        nameBox.setMaxLength(32);
        nameBox.setValue(initialName);
        nameBox.setBordered(true);
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);

        // Botão Toggle (ativo/inativo) — mostra estado atual
        toggleBtn = Button.builder(
                        Component.literal(active
                                ? "§4§l⊕ DESPERTA §c§l⊕"
                                : "§7§l⊖ Adormecida §8§l⊖"),
                        b -> {
                            active = !active;
                            toggleBtn.setMessage(Component.literal(active
                                    ? "§4§l⊕ DESPERTA §c§l⊕"
                                    : "§7§l⊖ Adormecida §8§l⊖"));
                        })
                .bounds(cx - 120, cy - 40, 240, 22)
                .build();
        addRenderableWidget(toggleBtn);

        // Botão Salvar — envia name + active
        addRenderableWidget(Button.builder(
                        Component.literal("§a✓ Salvar"),
                        b -> save())
                .bounds(cx - 102, cy + 18, 100, 20)
                .build());

        // Botão Cancelar
        addRenderableWidget(Button.builder(
                        Component.literal("§c✗ Cancelar"),
                        b -> onClose())
                .bounds(cx + 2, cy + 18, 100, 20)
                .build());
    }

    private void save() {
        String name = nameBox.getValue();
        LiberthiaMod.LOGGER.info("[BossCrown screen] save click — name='{}' active={}", name, active);
        ModNetwork.CHANNEL.sendToServer(new SetBossCrownNameC2SPacket(name, active));
        onClose();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        // Enter salva
        if (key == 257 || key == 335) {
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
        g.drawCenteredString(this.font,
                Component.literal("§4§l⊕ Coroa do Boss §c§l⊕"),
                cx, cy - 70, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.literal("§7Click no botão acima pra alternar ativa/dormida"),
                cx, cy + 50, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.literal("§8Nome da bossbar (vazio = nome do player)"),
                cx, cy + 65, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
