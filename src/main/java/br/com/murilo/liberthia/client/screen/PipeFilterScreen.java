package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.PipeFilterMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI da configuração do filtro de uma face específica de Item Pipe.
 *
 * <p>Layout: 5 slots horizontais centralizados pra ghost items + botão de toggle
 * WHITELIST/BLACKLIST embaixo. Player inventory padrão abaixo.
 *
 * <p>Aberta via shift+right-click com pipe_filter ou pipe_filter_not numa face
 * de Item Pipe. Fix do bug "filtros pipe não funcionam" — o problema era UX
 * (não tinha forma fácil de ver/editar os 5 items do filtro).
 */
public class PipeFilterScreen extends AbstractContainerScreen<PipeFilterMenu> {

    private Button modeButton;

    public PipeFilterScreen(PipeFilterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        // Botão toggle WHITELIST/BLACKLIST centralizado abaixo dos slots
        modeButton = Button.builder(modeLabel(), btn -> {
                    this.menu.clickMenuButton(this.minecraft.player, 0);
                    this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    btn.setMessage(modeLabel());
                })
                .pos(x + 38, y + 56)
                .size(100, 14)
                .build();
        this.addRenderableWidget(modeButton);
    }

    private Component modeLabel() {
        boolean blacklist = menu.isBlacklist();
        return Component.literal(blacklist ? "≠ BLACKLIST" : "== WHITELIST")
                .withStyle(blacklist ? ChatFormatting.RED : ChatFormatting.GREEN);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
        // Re-atualiza label do botão caso DataSlot mudou
        if (modeButton != null) modeButton.setMessage(modeLabel());
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        // Painel principal
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0E0212);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF161028);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF3B1A5C);

        // Header area
        g.drawString(this.font, Component.literal("§dFiltro do Pipe"),
                x + 8, y + 8, 0xFFFFFF, false);
        g.drawString(this.font, Component.literal("§7Coloque até 5 items pra filtrar:"),
                x + 8, y + 22, 0xCCCCCC, false);

        // 5 slot frames horizontais — posição precisa MATCHAR PipeFilterMenu
        // (slotsStartX=26, slotsY=36 no Menu)
        int slotsStartX = 26;
        int slotsY = 36;
        for (int i = 0; i < PipeFilterMenu.FILTER_SIZE; i++) {
            slot(g, x + slotsStartX + i * 18 - 1, y + slotsY - 1, 0xFFAA60FF);
        }

        // Player inv panel
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + imageHeight - 6, 0xFF120420);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF7F40C8);
    }

    private void slot(GuiGraphics g, int x, int y, int hi) {
        g.fill(x, y, x + 18, y + 1, 0xFF2A0D44);
        g.fill(x, y, x + 1, y + 18, 0xFF2A0D44);
        g.fill(x, y + 17, x + 18, y + 18, hi);
        g.fill(x + 17, y, x + 18, y + 18, hi);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF080018);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // Já desenhamos o título no renderBg
        g.drawString(this.font, this.playerInventoryTitle,
                inventoryLabelX, inventoryLabelY, 0x9966CC, false);
    }
}
