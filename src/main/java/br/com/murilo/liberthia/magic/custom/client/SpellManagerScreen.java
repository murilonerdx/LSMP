package br.com.murilo.liberthia.magic.custom.client;

import br.com.murilo.liberthia.magic.custom.CustomSpell;
import br.com.murilo.liberthia.magic.custom.CustomSpellClientCache;
import br.com.murilo.liberthia.magic.custom.DeleteSpellC2SPacket;
import br.com.murilo.liberthia.magic.custom.SelectSpellC2SPacket;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;

/**
 * v0.1.165 r140: <b>SpellManagerScreen</b> — lista todos os feitiços criados,
 * permite SELECIONAR ou DELETAR cada um.
 *
 * <p>Layout: scrollable list, cada row tem:
 * <ul>
 *   <li>Sprite icon + nome do feitiço (clicável → seleciona como ativo)</li>
 *   <li>Stats inline (mana/dano/range)</li>
 *   <li>Botão §c[X] de delete</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class SpellManagerScreen extends Screen {

    private final Screen parent;
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 28;
    private static final int VISIBLE_ROWS = 6;

    public SpellManagerScreen(Screen parent) {
        super(Component.literal("Meus Feitiços"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        this.clearWidgets();

        int cx = width / 2;
        int yTop = Math.max(20, (height - 240) / 2);
        int listX = cx - 160;
        int listY = yTop + 30;

        List<CustomSpell> spells = CustomSpellClientCache.getSpells();
        UUID selectedId = CustomSpellClientCache.getSelectedId();

        // Scroll buttons
        if (scrollOffset > 0) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("§a▲"),
                    b -> { scrollOffset--; rebuild(); })
                    .bounds(cx + 130, listY, 24, 18).build());
        }
        if (scrollOffset + VISIBLE_ROWS < spells.size()) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("§a▼"),
                    b -> { scrollOffset++; rebuild(); })
                    .bounds(cx + 130, listY + 5 * ROW_HEIGHT, 24, 18).build());
        }

        // Render rows
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = scrollOffset + i;
            if (idx >= spells.size()) break;
            CustomSpell s = spells.get(idx);
            int ry = listY + i * ROW_HEIGHT;

            boolean isSelected = s.id.equals(selectedId);
            // Select button (esquerda — wide, mostra nome)
            String label = (isSelected ? "§e★ " : "§7• ") + s.name;
            if (label.length() > 28) label = label.substring(0, 27) + "…";
            final UUID sid = s.id;
            this.addRenderableWidget(Button.builder(
                    Component.literal(label),
                    b -> {
                        ModNetwork.CHANNEL.sendToServer(new SelectSpellC2SPacket(sid));
                        // Optimistic update local cache
                        CustomSpellClientCache.setSelectedId(sid);
                        if (minecraft != null && minecraft.player != null) {
                            minecraft.player.displayClientMessage(
                                Component.literal("§a✓ Selecionado: " + s.name), true);
                        }
                        rebuild();
                    })
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("§l" + s.name + "\n§7" + s.element.displayName +
                            " — " + s.shape.displayName +
                            "\n§bM" + s.manaCost() + " §c" + (int)s.damage() + "dmg §aR" +
                            (int)s.range() + "b\n§8Click pra ativar")))
                    .bounds(listX, ry, 240, 22).build());

            // Delete button (direita — vermelho)
            this.addRenderableWidget(Button.builder(
                    Component.literal("§c§l✗"),
                    b -> {
                        ModNetwork.CHANNEL.sendToServer(new DeleteSpellC2SPacket(sid));
                        if (minecraft != null && minecraft.player != null) {
                            minecraft.player.displayClientMessage(
                                Component.literal("§c✗ Deletado: " + s.name), true);
                        }
                        // Wait for S2C sync — refresh asap
                        rebuild();
                    })
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("§cDeletar §e" + s.name)))
                    .bounds(listX + 244, ry, 26, 22).build());
        }

        // Back button (bottom)
        this.addRenderableWidget(Button.builder(
                Component.literal("§7← Voltar"),
                b -> {
                    if (minecraft != null) minecraft.setScreen(parent);
                })
                .bounds(cx - 60, yTop + 210, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Dark cosmic background
        g.fillGradient(0, 0, width, height, 0xEE0A0518, 0xFF1A0830);

        int cx = width / 2;
        int yTop = Math.max(20, (height - 240) / 2);

        // Title
        Component title = Component.literal("§e§l📜 Meus Feitiços");
        int tw = font.width(title);
        g.drawString(font, title, cx - tw / 2, yTop + 4, 0xFFFFFFFF, true);

        // Count
        List<CustomSpell> spells = CustomSpellClientCache.getSpells();
        String count = "§7" + spells.size() + " feitiço(s) criado(s)";
        int cw = font.width(count);
        g.drawString(font, count, cx - cw / 2, yTop + 16, 0xFFAAAAAA, false);

        if (spells.isEmpty()) {
            int yEmpty = yTop + 100;
            String msg = "§7Nenhum feitiço criado ainda.";
            String hint = "§8Volte e crie um!";
            g.drawString(font, msg, cx - font.width(msg) / 2, yEmpty, 0xFFFFFFFF, false);
            g.drawString(font, hint, cx - font.width(hint) / 2, yEmpty + 12, 0xFFAAAAAA, false);
        }

        super.render(g, mouseX, mouseY, partial);
    }
}
