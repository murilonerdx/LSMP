package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SetDimensionalChannelC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Tela simples pro player digitar o canal do Baú Dimensional.
 */
public class DimensionalChestChannelScreen extends Screen {

    private final BlockPos chestPos;
    private final String currentChannel;
    private EditBox channelBox;

    public DimensionalChestChannelScreen(BlockPos pos, String currentChannel) {
        super(Component.translatable("gui.liberthia.dim_chest.set_channel"));
        this.chestPos = pos;
        this.currentChannel = currentChannel == null ? "" : currentChannel;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.channelBox = new EditBox(this.font, cx - 100, cy - 10, 200, 20,
                Component.literal("canal"));
        this.channelBox.setMaxLength(32);
        this.channelBox.setHint(Component.literal("ex: storage, ouro, junk..."));
        this.channelBox.setValue(this.currentChannel);
        this.addRenderableWidget(this.channelBox);
        this.setInitialFocus(this.channelBox);

        this.addRenderableWidget(Button.builder(Component.literal("Salvar"),
                        b -> save())
                .bounds(cx - 100, cy + 20, 95, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancelar"),
                        b -> this.onClose())
                .bounds(cx + 5, cy + 20, 95, 20).build());
    }

    private void save() {
        String c = this.channelBox.getValue().trim();
        ModNetwork.CHANNEL.sendToServer(new SetDimensionalChannelC2SPacket(chestPos, c));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        int cx = this.width / 2;
        int cy = this.height / 2;
        g.drawCenteredString(this.font, this.title, cx, cy - 50, 0xFFFFFF);
        g.drawCenteredString(this.font,
                Component.literal("§7Mesma string = baús linkados, mesmo cross-dim"),
                cx, cy - 30, 0xCCCCCC);
    }

    @Override public boolean isPauseScreen() { return false; }
}
