package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class InfectionStatusOverlay implements IGuiOverlay {
    public static final InfectionStatusOverlay INSTANCE = new InfectionStatusOverlay();

    private InfectionStatusOverlay() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof InventoryScreen) {
            renderStatusPanel(minecraft, guiGraphics, screenWidth, screenHeight);
        }
    }

    private void renderStatusPanel(Minecraft minecraft, GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        int infection = ClientInfectionState.getInfection();
        int stage = ClientInfectionState.getStage();
        int penalty = ClientInfectionState.getPermanentHealthPenalty();
        int rawExposure = ClientInfectionState.getRawExposure();
        int protectionPct = ClientInfectionState.getArmorProtectionPercent();

        int panelWidth = 150;
        int panelHeight = 110;
        int x = screenWidth - panelWidth - 8;
        int y = 8;

        // Panel background
        guiGraphics.fill(x - 2, y - 2, x + panelWidth + 2, y + panelHeight + 2, 0xCC000000);
        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, 0xAA1A1A2E);

        // Title bar
        guiGraphics.fill(x, y, x + panelWidth, y + 12, 0xDD2A0845);
        guiGraphics.drawCenteredString(minecraft.font, Component.literal("§l☣ STATUS DE INFECÇÃO"), x + panelWidth / 2, y + 2, 0xFFFFFF);

        int textY = y + 16;

        // Infection bar
        guiGraphics.drawString(minecraft.font, "§7» §dInfecção: " + infection + "%", x + 6, textY, 0xFFFFFF, false);
        textY += 10;
        int barWidth = panelWidth - 12;
        int filled = (int) (barWidth * (infection / 100.0F));
        guiGraphics.fill(x + 6, textY, x + 6 + barWidth, textY + 5, 0xFF2D2D2D);
        int barColor = infection >= 75 ? 0xFFFF0000 : infection >= 50 ? 0xFFFF6600 : infection >= 25 ? 0xFFFFAA00 : 0xFF7A2A95;
        guiGraphics.fill(x + 6, textY, x + 6 + filled, textY + 5, barColor);
        textY += 9;

        // Stage
        String stageText = switch (stage) {
            case 4 -> "§4§l☣ COLAPSO ☣";
            case 3 -> "§6⚠ DEGRADAÇÃO";
            case 2 -> "§e⚠ EXPOSIÇÃO";
            case 1 -> "§a✥ CONTATO";
            default -> "§2✔ ESTÁVEL";
        };
        guiGraphics.drawString(minecraft.font, "§7» Estado: " + stageText, x + 6, textY, 0xFFFFFF, false);
        textY += 12;

        // Exposure
        String exposureSymbol = rawExposure > 0 ? "§c↑" : "§a-";
        guiGraphics.drawString(minecraft.font, "§7» Exposição: " + exposureSymbol + " §e" + rawExposure, x + 6, textY, 0xFFFFFF, false);
        textY += 10;

        // Protection
        guiGraphics.drawString(minecraft.font, "§7» Proteção: §b" + protectionPct + "%", x + 6, textY, 0xFFFFFF, false);
        textY += 12;

        // Health penalty
        if (penalty > 0) {
            guiGraphics.fill(x + 4, textY - 1, x + panelWidth - 4, textY + 9, 0x44FF0000);
            guiGraphics.drawString(minecraft.font, " §c♥ Dano Vital: -" + (penalty / 2.0F), x + 6, textY, 0xFFFFFF, false);
            textY += 12;
        }

        // Active effects indicators
        if (minecraft.player != null) {
            if (minecraft.player.hasEffect(ModEffects.DARK_INFECTION.get()) || 
                minecraft.player.hasEffect(ModEffects.RADIATION_SICKNESS.get()) ||
                minecraft.player.hasEffect(ModEffects.CLEAR_SHIELD.get())) {
                
                guiGraphics.fill(x + 4, textY, x + panelWidth - 4, textY + 1, 0x33FFFFFF);
                textY += 4;
                
                if (minecraft.player.hasEffect(ModEffects.DARK_INFECTION.get())) {
                    guiGraphics.drawString(minecraft.font, " §5• §oInfectado", x + 6, textY, 0xFFFFFF, false);
                    textY += 9;
                }
                if (minecraft.player.hasEffect(ModEffects.RADIATION_SICKNESS.get())) {
                    guiGraphics.drawString(minecraft.font, " §2• §oContaminado", x + 6, textY, 0xFFFFFF, false);
                    textY += 9;
                }
                if (minecraft.player.hasEffect(ModEffects.CLEAR_SHIELD.get())) {
                    guiGraphics.drawString(minecraft.font, " §b• §oProtegido", x + 6, textY, 0xFFFFFF, false);
                    textY += 12;
                }
            }
        }

        // Mutations List
        String mutationsRaw = ClientInfectionState.getMutations();
        if (!mutationsRaw.isEmpty()) {
            guiGraphics.fill(x + 4, textY, x + panelWidth - 4, textY + 1, 0x33FFFFFF);
            textY += 4;
            guiGraphics.drawString(minecraft.font, "§d✥ MUTAÇÕES:", x + 6, textY, 0xFFFFFF, false);
            textY += 10;

            String[] activeMutations = mutationsRaw.split(",");
            for (String mut : activeMutations) {
                String translated = switch (mut) {
                    case "HEAVY_STEPS" -> "§7- Passos Pesados";
                    case "RADIO_EYES" -> "§7- Olhos Radiantes";
                    case "HUNGRY_VOID" -> "§7- Fome do Vazio";
                    case "BRITTLE_BONES" -> "§7- Ossos Frágeis";
                    case "FRAGILE_LUNGS" -> "§7- Pulmões Fracos";
                    case "AQUATIC_ADAPTATION" -> "§b- Barba de Peixe";
                    case "STATIC_DISCHARGE" -> "§e- Carga Estática";
                    case "SWIFT_SIGHT" -> "§a- Visão Rápida";
                    default -> "§7- " + mut;
                };
                guiGraphics.drawString(minecraft.font, translated, x + 10, textY, 0xFFFFFF, false);
                textY += 9;
            }
        }
    }
}
