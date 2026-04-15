package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.config.LiberthiaConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class InfectionHudOverlay implements IGuiOverlay {
    public static final InfectionHudOverlay INSTANCE = new InfectionHudOverlay();

    private InfectionHudOverlay() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) return;

        int infection = ClientInfectionState.getInfection();

        // 1. MAIN HUD: Always render the permanent status (Phase 11.1)
        renderInfectionHud(minecraft, guiGraphics, screenWidth, screenHeight, infection);

        // 2. GEIGER HUD: Show when holding Geiger Counter (Phase 20.3)
        boolean hasGeiger = minecraft.player.getMainHandItem().is(br.com.murilo.liberthia.registry.ModItems.GEIGER_COUNTER.get()) ||
                            minecraft.player.getOffhandItem().is(br.com.murilo.liberthia.registry.ModItems.GEIGER_COUNTER.get());
                            
        if (hasGeiger) {
            renderExposureAlert(minecraft, guiGraphics);
        }
    }

    private void renderInfectionHud(Minecraft minecraft, GuiGraphics guiGraphics, int screenWidth, int screenHeight, int infection) {
        int x = LiberthiaConfig.CLIENT.infectionX.get();
        int y = LiberthiaConfig.CLIENT.infectionY.get();
        int width = 110;
        int height = 10;
        int filled = (int) (width * (infection / 100.0F));

        // PREMIUM VISUALS: Modern frame and gradient style
        guiGraphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xCC1A1A1A); // Outer frame
        guiGraphics.fill(x, y, x + width, y + height, 0xFF2D2D2D); // Background
        
        // Infection Bar with Stage-based colors
        int barColor = (infection >= 75) ? 0xFF800000 : (infection >= 50) ? 0xFF5A1A78 : 0xFF3D1F5B;
        guiGraphics.fill(x, y, x + filled, y + height, barColor);

        String stageText = switch (ClientInfectionState.getStage()) {
            case 4 -> "COLAPSO";
            case 3 -> "DEGRADAÇÃO";
            case 2 -> "CONTAMINAÇÃO";
            case 1 -> "EXPOSIÇÃO";
            default -> "LIMPO";
        };

        // Detailed Stats ("TYPE" and Mutations)
        String mutations = ClientInfectionState.getMutations();
        String typeText = mutations.isEmpty() ? "Viral" : mutations.replace(",", ", ");
        
        guiGraphics.drawString(minecraft.font, Component.literal("Nível: " + infection + "%"), x, y + 14, 0xFFFFFF, true);
        guiGraphics.drawString(minecraft.font, Component.literal("Estado: " + stageText), x, y + 22, 0xD9B3FF, true);
        guiGraphics.drawString(minecraft.font, Component.literal("Tipo: " + typeText), x, y + 30, 0xAAAAAA, true);
        
        if (ClientInfectionState.getPermanentHealthPenalty() > 0) {
            guiGraphics.drawString(minecraft.font, Component.literal("Penalidade: -" + (ClientInfectionState.getPermanentHealthPenalty() / 2.0F) + "❤"), x, y + 38, 0xFF5555, true);
        }

        // Pulse effect during high infection
        if (infection > 50) {
            float baseAlpha = Math.min(0.35F, infection / 300.0F);
            float pulse = (float) ((Math.sin((System.currentTimeMillis() / 150.0D)) + 1.0D) * 0.5D);
            int alpha = (int) ((baseAlpha + (pulse * 0.15F)) * 255.0F);
            RenderSystem.enableBlend();
            guiGraphics.fill(0, 0, screenWidth, screenHeight, (alpha << 24) | 0x2A0033);
            RenderSystem.disableBlend();
        }
    }

    private void renderExposureAlert(Minecraft minecraft, GuiGraphics guiGraphics) {
        int width = 140;
        int height = 35;
        int x = LiberthiaConfig.CLIENT.exposureX.get();
        int y = LiberthiaConfig.CLIENT.exposureY.get();

        int effectiveExposure = ClientInfectionState.getEffectiveExposure();

        // Criticality Logic (Phase 20.3) - Based on Ambient Exposure
        String criticalityLabel;
        int critColor;
        if (effectiveExposure >= 91) { criticalityLabel = "COLAPSO"; critColor = 0xAA00AA; }
        else if (effectiveExposure >= 61) { criticalityLabel = "MUITO ALTA"; critColor = 0xFF5555; }
        else if (effectiveExposure >= 41) { criticalityLabel = "ALTA"; critColor = 0xFFAA00; }
        else if (effectiveExposure >= 21) { criticalityLabel = "MÉDIA"; critColor = 0xFFFF55; }
        else if (effectiveExposure >= 5) { criticalityLabel = "BAIXA"; critColor = 0xAAAAAA; }
        else { criticalityLabel = "SEGURA"; critColor = 0x55FF55; }

        // Pulse for Collapse
        if (effectiveExposure >= 91 && (System.currentTimeMillis() / 250) % 2 == 0) critColor = 0xFFFFFF;

        // Premium Frame
        guiGraphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xCC111111);
        guiGraphics.fill(x, y, x + width, y + height, 0x99000000);

        // Header
        guiGraphics.drawString(minecraft.font, Component.literal("DETECTOR DE RADIAÇÃO"), x + 4, y + 4, 0x00FF00, true);
        
        // Stats
        guiGraphics.drawString(minecraft.font, Component.literal("CRITICIDADE: "), x + 4, y + 14, 0xFFFFFF, true);
        guiGraphics.drawString(minecraft.font, Component.literal(criticalityLabel), x + 72, y + 14, critColor, true);
        
        guiGraphics.drawString(minecraft.font, Component.literal("RADIAÇÃO TOTAL: " + effectiveExposure + " Sps"), x + 4, y + 24, 0xAAAAAA, true);

        // Signal Progress Bar
        int signalWidth = (int)((width - 8) * (Math.min(100, effectiveExposure * 4) / 100.0F));
        guiGraphics.fill(x + 4, y + height - 4, x + 4 + signalWidth, y + height - 2, critColor);
    }
}