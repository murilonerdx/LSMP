package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.config.LiberthiaConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Map;

public class DnaMutationOverlay implements IGuiOverlay {
    public static final DnaMutationOverlay INSTANCE = new DnaMutationOverlay();

    private static final float SCALE = 0.75f;

    private static final Map<String, String> MUTATION_DESCRIPTIONS = Map.ofEntries(
            Map.entry("HEAVY_STEPS", "Passos pesados"),
            Map.entry("RADIO_EYES", "Visao noturna"),
            Map.entry("HUNGRY_VOID", "Fome constante"),
            Map.entry("DARK_VEIL", "Cegueira parcial"),
            Map.entry("RADIANT_SKIN", "Resistencia a dano"),
            Map.entry("AQUATIC_ADAPTATION", "Respiracao aquatica"),
            Map.entry("SWIFT_SIGHT", "Velocidade extra"),
            Map.entry("STATIC_DISCHARGE", "Dano na chuva"),
            Map.entry("LUCID_CORRUPTION", "Forca + visao"),
            Map.entry("TACTICAL_REASON", "Resistencia + velocidade")
    );

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        String mutationsRaw = ClientInfectionState.getMutations();
        if (mutationsRaw == null || mutationsRaw.isEmpty()) {
            return;
        }

        String[] mutations = mutationsRaw.split(",");
        if (mutations.length == 0 || (mutations.length == 1 && mutations[0].trim().isEmpty())) {
            return;
        }

        int x = LiberthiaConfig.CLIENT.dnaX.get();
        int y = LiberthiaConfig.CLIENT.dnaY.get();
        int rowH = 10;
        int activeCount = 0;

        for (String m : mutations) {
            if (!m.trim().isEmpty()) activeCount++;
        }

        if (activeCount == 0) return;

        int panelHeight = 14 + (activeCount * rowH);
        int width = 105;

        RenderSystem.enableBlend();

        // Compact translucent background
        guiGraphics.fill(x - 3, y - 4, x + width + 3, y + panelHeight, 0xAA0A0A0A);

        // Title
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(SCALE, SCALE, 1.0f);
        int sx = (int) (x / SCALE);
        int sy = (int) ((y - 1) / SCALE);
        guiGraphics.drawString(gui.getFont(), "\u00a7dMutations", sx, sy, 0xFFE0E0E0, false);
        guiGraphics.pose().popPose();

        // Each mutation: name + short description
        int row = 0;
        for (String mutation : mutations) {
            String m = mutation.trim();
            if (m.isEmpty()) continue;

            String desc = MUTATION_DESCRIPTIONS.getOrDefault(m, "");
            String display = "\u00a7f" + m.toLowerCase().replace("_", " ");
            if (!desc.isEmpty()) {
                display += " \u00a77- " + desc;
            }

            int my = y + 10 + (row * rowH);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(SCALE, SCALE, 1.0f);
            guiGraphics.drawString(gui.getFont(), display,
                    (int) (x / SCALE), (int) (my / SCALE), 0xFFFFFFFF, false);
            guiGraphics.pose().popPose();
            row++;
        }

        RenderSystem.disableBlend();
    }
}
