package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.145 r113: <b>CastBarOverlay</b> — barra de cooldown do spell ativo
 * acima da hotbar quando o player segura um {@link UniversalSpellScrollItem}.
 *
 * <p>Quando o spell está em cooldown, mostra:
 * <ul>
 *   <li>Nome do spell (com cor da escola)</li>
 *   <li>Barra horizontal (180px) preenchendo da esquerda pra direita</li>
 *   <li>Tempo restante em segundos (centro da barra)</li>
 * </ul>
 *
 * <p>Quando ready (sem cooldown), mostra "✓ Pronto" em verde.
 *
 * <p>Original code, escrito do zero usando API pública IGuiOverlay.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CastBarOverlay {

    private CastBarOverlay() {}

    public static final IGuiOverlay CAST_BAR = (gui, g, partial, sw, sh) -> {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) return;

        // Só mostra se segurando spell scroll (main ou off hand)
        ItemStack main = p.getMainHandItem();
        ItemStack off = p.getOffhandItem();
        UniversalSpellScrollItem scroll = null;
        ItemStack scrollStack = null;
        if (main.getItem() instanceof UniversalSpellScrollItem s) {
            scroll = s; scrollStack = main;
        } else if (off.getItem() instanceof UniversalSpellScrollItem s) {
            scroll = s; scrollStack = off;
        }
        if (scroll == null || scrollStack == null) return;

        SpellDef def = scroll.def(scrollStack);
        if (def == null) return;

        // Cooldown progress (0.0 = ready, 1.0 = full cooldown remaining)
        float cdProgress = p.getCooldowns().getCooldownPercent(scroll, partial);

        int barW = 180;
        int barH = 8;
        // r165: arrastável via UnifiedHud editor (HudId.CAST_BAR — anchorBottom)
        int x = br.com.murilo.liberthia.client.hud.unified.ClientHudPositions.x(
                br.com.murilo.liberthia.client.hud.unified.HudId.CAST_BAR, sw);
        int y = br.com.murilo.liberthia.client.hud.unified.ClientHudPositions.y(
                br.com.murilo.liberthia.client.hud.unified.HudId.CAST_BAR, sh);

        // ─── Spell name + school color ───
        int hex = def.school.colorHex();
        int color = 0xFF000000 | (hex & 0x00FFFFFF);
        String name = def.name;
        int textW = mc.font.width(name);
        g.drawString(mc.font, Component.literal(name).withStyle(def.school.color()),
                x + (barW - textW) / 2, y - 10, color, true);

        // ─── Bar background ───
        g.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0xFF000000);   // black border
        g.fill(x, y, x + barW, y + barH, 0xFF1A0A2A);                    // dark inner

        if (cdProgress > 0F) {
            // ─── Cooldown bar (red→yellow→green gradient) ───
            int fillW = (int)(barW * (1F - cdProgress));
            int fillColor;
            if (cdProgress > 0.6F) fillColor = 0xFFFF5555;       // red
            else if (cdProgress > 0.3F) fillColor = 0xFFFFAA33;  // orange
            else fillColor = 0xFFFFFF55;                          // yellow-ready
            g.fill(x, y, x + fillW, y + barH, fillColor);
            // Pixel highlight on top
            g.fill(x, y, x + fillW, y + 1, (fillColor & 0x00FFFFFF) | 0x80FFFFFF);

            // Time remaining text
            int totalCdTicks = (int)(def.cooldownTicks
                    * SpellLevels.cooldownMultiplier(SpellLevels.getLevel(scrollStack)));
            int remainingTicks = (int)(cdProgress * totalCdTicks);
            float remainingS = remainingTicks / 20F;
            String label = String.format("%.1fs", remainingS);
            int lw = mc.font.width(label);
            g.drawString(mc.font, label, x + (barW - lw) / 2, y - 1, 0xFFFFFFFF, true);
        } else {
            // ─── Ready state — green filled bar ───
            g.fill(x, y, x + barW, y + barH, 0xFF55DD55);
            g.fill(x, y, x + barW, y + 1, 0x80FFFFFF);
            String label = "§a✓ Pronto";
            int lw = mc.font.width(label);
            g.drawString(mc.font, label, x + (barW - lw) / 2, y - 1, 0xFFFFFFFF, true);
        }
    };

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), "spell_cast_bar", CAST_BAR);
    }
}
