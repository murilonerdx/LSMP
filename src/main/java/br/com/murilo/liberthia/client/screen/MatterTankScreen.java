package br.com.murilo.liberthia.client.screen;

import br.com.murilo.liberthia.menu.MatterTankMenu;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * GUI do Matter Tank — tema dark violeta com barra vertical mostrando fluido +
 * 2 slots de bucket (in/out) + botão Purgar.
 *
 * <p>Cor do fluido muda conforme o tipo armazenado:
 *  - DARK = 0xFF6020A0 (violeta escuro)
 *  - CLEAR = 0xFFB0E8FF (branco perolado)
 *  - YELLOW = 0xFFFFD23F (dourado)
 */
public class MatterTankScreen extends AbstractContainerScreen<MatterTankMenu> {

    private Button purgeButton;

    public MatterTankScreen(MatterTankMenu m, Inventory inv, Component title) {
        super(m, inv, title);
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
        // Botão Purgar — top-right
        this.purgeButton = Button.builder(
                Component.translatable("gui.liberthia.matter_tank.purge"),
                btn -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    }
                })
                .bounds(x + 110, y + 16, 56, 14)
                .build();
        this.addRenderableWidget(this.purgeButton);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        // Painel principal — gradient violeta dark
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF0A0118);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF1A0828);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 2, 0xFF6020A0);

        // Painel da máquina
        g.fill(x + 6, y + 16, x + imageWidth - 6, y + 78, 0xFF050010);

        // Barra vertical de fluido à esquerda (entre x+10 e x+30)
        int barX = x + 10, barY = y + 18, barW = 20, barH = 54;
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF000000);
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF101020);

        int amount = menu.getAmount();
        int capacity = menu.getCapacity();
        int fluidId = menu.getFluidTypeId();
        if (capacity > 0 && amount > 0 && fluidId > 0) {
            int filled = (int) ((long) barH * amount / capacity);
            int color = fluidColorFor(fluidId);
            for (int i = 0; i < filled; i++) {
                g.fill(barX, barY + barH - 1 - i, barX + barW, barY + barH - i, color);
            }
            // Brilho no topo
            if (filled > 0) {
                g.fill(barX, barY + barH - filled, barX + barW, barY + barH - filled + 1, lighter(color));
            }
        }

        // Bordas dos 2 slots (centro)
        slot(g, x + 80 - 1, y + 20 - 1, 0xFFAA60FF); // slot IN
        slot(g, x + 80 - 1, y + 50 - 1, 0xFFFFD23F); // slot OUT

        // v0.1.48: REMOVIDO ghost-icons — user reportou que confundiam (parecia
        // ter item no slot mas não tinha, ele não conseguia clicar pra pegar).
        // Em vez disso, labels textuais simples ao lado dos slots.
        g.drawString(this.font, Component.literal("§7IN"),  x + 100, y + 23, 0xFFFFFF, false);
        g.drawString(this.font, Component.literal("§eOUT"), x + 100, y + 53, 0xFFFFFF, false);

        // Setas indicativas conectando slots ↔ barra de fluido:
        //   IN  → fluido    (balde despeja no tank, ou pega fluido)
        //   OUT ← fluido    (matter bucket sai do tank)
        drawArrow(g, x + 32, y + 26, x + 78, true, 0xFFAA60FF);  // IN → tank
        drawArrow(g, x + 32, y + 58, x + 78, false, 0xFFFFD23F); // tank → OUT

        // Player inv panel
        g.fill(x + 6, y + 80, x + imageWidth - 6, y + imageHeight - 6, 0xFF080018);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                slot(g, x + 8 + col * 18 - 1, y + 84 + row * 18 - 1, 0xFF55208A);
        for (int col = 0; col < 9; col++)
            slot(g, x + 8 + col * 18 - 1, y + 142 - 1, 0xFF7F40C8);
    }

    /** Retorna o matter bucket correspondente ao fluido (1=DM, 2=CM, 3=YM). */
    private static ItemStack matterBucketForId(int id) {
        return switch (id) {
            case 1 -> new ItemStack(ModItems.DARK_MATTER_BUCKET.get());
            case 2 -> new ItemStack(ModItems.CLEAR_MATTER_BUCKET.get());
            case 3 -> new ItemStack(ModItems.YELLOW_MATTER_BUCKET.get());
            default -> ItemStack.EMPTY;
        };
    }

    /**
     * Render simples de uma seta horizontal entre {@code x1} e {@code x2} na
     * altura {@code y}. Se {@code rightward} for true, a ponta vai pra direita;
     * se false, pra esquerda. Pixel-art 3px de altura.
     */
    private static void drawArrow(GuiGraphics g, int x1, int y, int x2, boolean rightward, int color) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        // corpo da seta (linha de 1px)
        g.fill(minX, y, maxX, y + 1, color);
        // ponta da seta (triangulo simples — 3 fills)
        if (rightward) {
            g.fill(maxX - 1, y - 1, maxX, y + 2, color);
            g.fill(maxX - 2, y - 2, maxX, y + 3, color);
            g.fill(maxX - 3, y - 3, maxX, y + 4, color);
        } else {
            g.fill(minX, y - 1, minX + 1, y + 2, color);
            g.fill(minX, y - 2, minX + 2, y + 3, color);
            g.fill(minX, y - 3, minX + 3, y + 4, color);
        }
    }

    private static int fluidColorFor(int id) {
        return switch (id) {
            case 1 -> 0xFF6020A0; // dark matter — violeta escuro
            case 2 -> 0xFFB0E8FF; // clear matter — branco perolado
            case 3 -> 0xFFFFD23F; // yellow matter — dourado
            default -> 0xFF505050;
        };
    }

    private static int lighter(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + 0x40);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 0x40);
        int b = Math.min(255, (color & 0xFF) + 0x40);
        return (a << 24) | (r << 16) | (g << 8) | b;
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
        // Title colorida conforme fluido (sem prefixo "Tanque:" — tomava muito espaço)
        int fid = menu.getFluidTypeId();
        ChatFormatting tColor = switch (fid) {
            case 1 -> ChatFormatting.DARK_PURPLE;
            case 2 -> ChatFormatting.AQUA;
            case 3 -> ChatFormatting.GOLD;
            default -> ChatFormatting.GRAY;
        };
        String fluidName = switch (fid) {
            case 1 -> "DARK MATTER";
            case 2 -> "CLEAR MATTER";
            case 3 -> "YELLOW MATTER";
            default -> "VAZIO";
        };
        g.drawString(this.font,
                Component.literal(fluidName).withStyle(tColor, ChatFormatting.BOLD),
                titleLabelX, titleLabelY, 0xFFFFFF, true);
        g.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x9966CC, false);

        // v0.1.48: info de capacidade reposicionada — antes tinha 2 linhas (amt/cap
        // em y=32 e pct% em y=44) que ficavam APERTADAS contra o slot OUT (y=50-68)
        // e às vezes sobrepunham o "Inventory" (y=72). Agora uma linha só, encolhida
        // e colocada ABAIXO da barra de fluido (que termina em y=18+54=72... hmm
        // mesmo limite). Vou colocar ao lado da barra, em (38, 60) — região vazia
        // entre setas e o painel do player inv.
        int amt = menu.getAmount(), cap = menu.getCapacity();
        if (cap > 0) {
            int pct = 100 * amt / cap;
            // Linha única compacta: "12% · 2000/16000 mB"
            String info = String.format("§b%d%%§7 · §f%d§8/§7%d mB", pct, amt, cap);
            g.drawString(this.font, Component.literal(info), 38, 64, 0xFFFFFF, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }
}
