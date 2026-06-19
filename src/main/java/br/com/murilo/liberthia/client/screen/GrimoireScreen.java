package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.grimoire.GrimoireBookItem;
import br.com.murilo.liberthia.magic.grimoire.GrimoireMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * v0.1.151 r119: <b>GrimoireScreen</b> — UI do Grimório de feitiços.
 *
 * <p>9 slots em linha. Slot ativo destacado com glow dourado. Click esquerdo
 * em slot vazio → coloca scroll. Click em slot cheio com nada na mão → seleciona
 * (vira active_slot). Click direito → remove.
 */
public class GrimoireScreen extends AbstractContainerScreen<GrimoireMenu> {

    private static final ResourceLocation BG = new ResourceLocation(
            LiberthiaMod.MODID, "textures/gui/grimoire.png");

    public GrimoireScreen(GrimoireMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 5;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        g.blit(BG, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Highlight active slot
        int active = menu.activeSlot();
        long time = System.currentTimeMillis();
        int alpha = (int)(120 + Math.sin(time / 200.0) * 80);
        int color = (alpha << 24) | 0xFFEE00;
        int slotX = leftPos + 8 + active * 18;
        int slotY = topPos + 30;
        // Outer border
        g.fill(slotX - 1, slotY - 1, slotX + 17, slotY, color);
        g.fill(slotX - 1, slotY + 16, slotX + 17, slotY + 17, color);
        g.fill(slotX - 1, slotY, slotX, slotY + 17, color);
        g.fill(slotX + 16, slotY, slotX + 17, slotY + 17, color);

        // Slot number under each slot (1-9)
        for (int i = 0; i < 9; i++) {
            int x = leftPos + 8 + i * 18;
            String n = String.valueOf(i + 1);
            g.drawString(font, n, x + 6, topPos + 50, 0xFFAAAAAA, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        // Hint label
        g.drawString(font, "§7Click pra selecionar o feitiço ativo",
                leftPos + 8, topPos + 60, 0xFFFFFFFF, false);
        // Active slot label
        ItemStack active = menu.getSlot(menu.activeSlot()).getItem();
        if (!active.isEmpty()) {
            g.drawString(font, Component.literal("§6Ativo: ").append(active.getHoverName()),
                    leftPos + 8, topPos + 18, 0xFFFFFFFF, false);
        } else {
            g.drawString(font, "§8Slot " + (menu.activeSlot() + 1) + ": vazio",
                    leftPos + 8, topPos + 18, 0xFFFFFFFF, false);
        }
        // r157: Magic Level display (level + progress bar + kills/required)
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            int level = br.com.murilo.liberthia.observation.source.MagicLevelData.getLevel(mc.player);
            int kills = br.com.murilo.liberthia.observation.source.MagicLevelData.getKillsThisLevel(mc.player);
            int needed = br.com.murilo.liberthia.observation.source.MagicLevelData.getKillsRequired(level);
            float progress = br.com.murilo.liberthia.observation.source.MagicLevelData.getProgress(mc.player);
            // Title with level
            String levelLine = "§5§l✦ Nível Mágico: §d" + level + "§7/10";
            g.drawString(font, levelLine, leftPos + 8, topPos + 70, 0xFFFFFFFF, false);
            if (level < 10) {
                g.drawString(font, "§7Kills: §a" + kills + "§7/" + needed, leftPos + 8, topPos + 80, 0xFFFFFFFF, false);
                // Progress bar
                int barW = 160;
                int barH = 4;
                int barX = leftPos + 8;
                int barY = topPos + 90;
                g.fill(barX, barY, barX + barW, barY + barH, 0xFF222222);
                int filled = Math.round(barW * progress);
                g.fill(barX, barY, barX + filled, barY + barH, 0xFFAA66FF);
                g.drawString(font, "§7−" + (level * 5) + "% custo §a| §7+" + (level * 5) + "% max",
                        leftPos + 8, topPos + 96, 0xFFFFFFFF, false);
            } else {
                g.drawString(font, "§6§l✦ NÍVEL MÁXIMO §7(−50% custo, +50% max)",
                        leftPos + 8, topPos + 80, 0xFFFFFFFF, false);
            }
        }
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Detecta click sobre os scroll slots: ao clicar com nada na mão, troca active
        int rx = (int)(mouseX - leftPos);
        int ry = (int)(mouseY - topPos);
        if (ry >= 30 && ry < 46 && rx >= 8 && rx < 8 + 9 * 18) {
            int slot = (rx - 8) / 18;
            if (button == 0 && menu.getCarried().isEmpty()) {
                // Apenas seleciona — não move item
                menu.setActiveSlot(slot);
                // não consume event — segue fluxo normal de slot click
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
