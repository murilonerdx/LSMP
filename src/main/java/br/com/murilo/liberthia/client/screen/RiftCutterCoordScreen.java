package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenRiftCutterCoordScreenS2CPacket;
import br.com.murilo.liberthia.network.packet.SaveRiftCutterCoordsC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * r185 — GUI de coordenadas da Adaga Corta-Fendas. Tela pura (sem Menu), aberta via S2C.
 * Digita X/Y/Z, escolhe a dimensão (ciclo) e confirma → envia {@link SaveRiftCutterCoordsC2SPacket}.
 */
public class RiftCutterCoordScreen extends Screen {
    private final int initX, initY, initZ;
    private final List<String> dims = new ArrayList<>(List.of(
            "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end", "liberthia:spirit_world"));
    private int dimIndex = 0;

    private EditBox boxX, boxY, boxZ;
    private Button dimButton;

    public RiftCutterCoordScreen(OpenRiftCutterCoordScreenS2CPacket msg) {
        super(Component.literal("Corta-Fendas — Configurar Alvo"));
        this.initX = msg.getX();
        this.initY = msg.getY();
        this.initZ = msg.getZ();
        if (msg.getDim() != null && !msg.getDim().isEmpty()) {
            if (!dims.contains(msg.getDim())) dims.add(msg.getDim());
            dimIndex = Math.max(0, dims.indexOf(msg.getDim()));
        }
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y0 = this.height / 2 - 50;

        boxX = numBox(cx - 40, y0, initX);
        boxY = numBox(cx - 40, y0 + 24, initY);
        boxZ = numBox(cx - 40, y0 + 48, initZ);
        addRenderableWidget(boxX);
        addRenderableWidget(boxY);
        addRenderableWidget(boxZ);

        dimButton = Button.builder(Component.literal(dimLabel()), b -> {
            dimIndex = (dimIndex + 1) % dims.size();
            b.setMessage(Component.literal(dimLabel()));
        }).bounds(cx - 80, y0 + 76, 160, 20).build();
        addRenderableWidget(dimButton);

        addRenderableWidget(Button.builder(Component.literal("§aConfirmar"), b -> confirm())
                .bounds(cx - 80, y0 + 102, 76, 20).build());
        addRenderableWidget(Button.builder(Component.literal("§cCancelar"), b -> onClose())
                .bounds(cx + 4, y0 + 102, 76, 20).build());
    }

    private EditBox numBox(int x, int y, int value) {
        EditBox e = new EditBox(this.font, x, y, 80, 18, Component.literal(""));
        e.setValue(Integer.toString(value));
        e.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d{0,9}"));
        return e;
    }

    private String dimLabel() { return "Dimensão: " + dims.get(dimIndex); }

    private void confirm() {
        int x = parse(boxX.getValue()), y = parse(boxY.getValue()), z = parse(boxZ.getValue());
        ModNetwork.sendToServer(new SaveRiftCutterCoordsC2SPacket(x, y, z, dims.get(dimIndex)));
        onClose();
    }

    private int parse(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int cx = this.width / 2;
        int y0 = this.height / 2 - 50;
        g.fill(cx - 100, y0 - 26, cx + 100, y0 + 132, 0xCC120A1E);
        g.fill(cx - 100, y0 - 26, cx + 100, y0 - 25, 0xFFAA00FF);
        g.drawCenteredString(this.font, "§5Corta-Fendas", cx, y0 - 20, 0xFFFFFF);
        g.drawString(this.font, "X", cx - 56, y0 + 5, 0xFFAAAAAA, false);
        g.drawString(this.font, "Y", cx - 56, y0 + 29, 0xFFAAAAAA, false);
        g.drawString(this.font, "Z", cx - 56, y0 + 53, 0xFFAAAAAA, false);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
