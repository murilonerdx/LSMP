package br.com.murilo.liberthia.client.hud.unified;

import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;

/**
 * r164: <b>Unified HUD Editor</b> — tela única que mostra TODOS os HUDs do mod
 * como retângulos dummy e permite drag-and-drop pra reposicionar. Ao soltar,
 * envia {@link UpdateHudPositionC2SPacket} pro server, que salva no NBT do
 * player. Reset all volta tudo pros defaults.
 *
 * <p>Abre via comando {@code /liberthia hud editor} (registrado no
 * {@link HudConfigCommand}).
 */
@OnlyIn(Dist.CLIENT)
public class UnifiedHudEditorScreen extends Screen {

    /** Mapeamento atual de posições enquanto edita (commit on release). */
    private final Map<HudId, int[]> draft = new EnumMap<>(HudId.class);
    private HudId dragging = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public UnifiedHudEditorScreen() {
        super(Component.literal("Editor de HUDs"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        // Inicializa draft com posições atuais (do client cache)
        draft.clear();
        for (HudId h : HudId.values()) {
            draft.put(h, new int[] { ClientHudPositions.rawX(h), ClientHudPositions.rawY(h) });
        }

        // Botões: Reset All, Fechar
        addRenderableWidget(Button.builder(
                Component.literal("§e↺ Resetar Tudo"),
                btn -> resetAll())
                .bounds(10, height - 30, 110, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("§a✓ Salvar e Fechar"),
                btn -> this.onClose())
                .bounds(width - 130, height - 30, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Background semi-transparent (não dim demais — usuário precisa ver o mundo atrás)
        g.fill(0, 0, width, height, 0x80000000);

        // Header
        String title = "§6§lEditor de HUDs §r§7— arraste pra reposicionar";
        int tw = font.width("Editor de HUDs — arraste pra reposicionar");
        g.drawString(font, title, (width - tw) / 2, 6, 0xFFFFFFFF, true);

        String hint = "§7Click+drag em qualquer caixa · §c[ESC]§7 fechar · §e[R]§7 reset hovered";
        int hw = font.width(hint.replaceAll("§.", ""));
        g.drawString(font, hint, (width - hw) / 2, 18, 0xFFCCCCCC, true);

        // Desenha cada HUD como caixa colorida no draft position
        HudId hoveredHud = null;
        for (HudId h : HudId.values()) {
            int[] xy = draft.get(h);
            int rx = h.anchorRight ? width - xy[0] - h.width : xy[0];
            int ry = h.anchorBottom ? height - xy[1] - h.height : xy[1];

            boolean hovered = mouseX >= rx && mouseX <= rx + h.width
                          && mouseY >= ry && mouseY <= ry + h.height;
            if (hovered) hoveredHud = h;
            boolean isDragged = h == dragging;

            int fillColor = isDragged ? 0xCCFFAA33 : (hovered ? 0xCC9D4DD6 : 0xAA1A0830);
            int borderColor = isDragged ? 0xFFFFEE66 : (hovered ? 0xFFFFFFFF : 0xFFAA66FF);

            g.fill(rx, ry, rx + h.width, ry + h.height, fillColor);
            // border 1px
            g.fill(rx, ry, rx + h.width, ry + 1, borderColor);
            g.fill(rx, ry + h.height - 1, rx + h.width, ry + h.height, borderColor);
            g.fill(rx, ry, rx + 1, ry + h.height, borderColor);
            g.fill(rx + h.width - 1, ry, rx + h.width, ry + h.height, borderColor);

            // Label
            String label = h.displayName;
            int lw = font.width(label);
            int textX = rx + Math.max(2, (h.width - lw) / 2);
            int textY = ry + Math.max(2, (h.height - 9) / 2);
            g.drawString(font, label, textX, textY, 0xFFFFFFFF, true);

            // Coord tooltip in corner
            if (hovered || isDragged) {
                String coords = "§7" + xy[0] + ", " + xy[1]
                        + (h.anchorRight ? " §8(R)" : "")
                        + (h.anchorBottom ? " §8(B)" : "");
                g.drawString(font, coords, rx, ry - 10, 0xFFFFFFAA, true);
            }
        }

        // R = reset hovered
        if (hoveredHud != null) {
            String rh = "§7Pressione §eR§7 pra resetar §f" + hoveredHud.displayName;
            int rhw = font.width(rh.replaceAll("§.", ""));
            g.drawString(font, rh, (width - rhw) / 2, 32, 0xFFFFEE66, true);
        }

        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;

        // Test which HUD was clicked (back-to-front: last drawn = top)
        HudId hit = null;
        for (HudId h : HudId.values()) {
            int[] xy = draft.get(h);
            int rx = h.anchorRight ? width - xy[0] - h.width : xy[0];
            int ry = h.anchorBottom ? height - xy[1] - h.height : xy[1];
            if (mx >= rx && mx <= rx + h.width && my >= ry && my <= ry + h.height) {
                hit = h;
            }
        }
        if (hit != null) {
            dragging = hit;
            int[] xy = draft.get(hit);
            int rx = hit.anchorRight ? width - xy[0] - hit.width : xy[0];
            int ry = hit.anchorBottom ? height - xy[1] - hit.height : xy[1];
            dragOffsetX = (int)(mx - rx);
            dragOffsetY = (int)(my - ry);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging == null) return super.mouseDragged(mx, my, button, dx, dy);

        // Compute raw position (relative to anchor)
        int screenX = (int)(mx - dragOffsetX);
        int screenY = (int)(my - dragOffsetY);
        int rawX = dragging.anchorRight ? width - screenX - dragging.width : screenX;
        int rawY = dragging.anchorBottom ? height - screenY - dragging.height : screenY;

        // Clamp to screen bounds (rawX/Y deve ser positivo dentro da tela)
        rawX = Math.max(0, Math.min(width - dragging.width, rawX));
        rawY = Math.max(0, Math.min(height - dragging.height, rawY));

        draft.put(dragging, new int[] { rawX, rawY });
        // Update client cache live pra HUDs reais reagirem
        ClientHudPositions.setLocal(dragging, rawX, rawY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null && button == 0) {
            // Commit pro server
            int[] xy = draft.get(dragging);
            ModNetwork.sendToServer(new UpdateHudPositionC2SPacket(
                    dragging.id, xy[0], xy[1], false));
            dragging = null;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            // Reset whatever is hovered
            int mx = (int) minecraft.mouseHandler.xpos()
                    * width / minecraft.getWindow().getScreenWidth();
            int my = (int) minecraft.mouseHandler.ypos()
                    * height / minecraft.getWindow().getScreenHeight();
            HudId hit = null;
            for (HudId h : HudId.values()) {
                int[] xy = draft.get(h);
                int rx = h.anchorRight ? width - xy[0] - h.width : xy[0];
                int ry = h.anchorBottom ? height - xy[1] - h.height : xy[1];
                if (mx >= rx && mx <= rx + h.width && my >= ry && my <= ry + h.height) hit = h;
            }
            if (hit != null) {
                draft.put(hit, new int[] { hit.defaultX, hit.defaultY });
                ClientHudPositions.resetLocal(hit);
                ModNetwork.sendToServer(new UpdateHudPositionC2SPacket(
                        hit.id, 0, 0, true));
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void resetAll() {
        for (HudId h : HudId.values()) {
            draft.put(h, new int[] { h.defaultX, h.defaultY });
            ClientHudPositions.resetLocal(h);
            ModNetwork.sendToServer(new UpdateHudPositionC2SPacket(h.id, 0, 0, true));
        }
    }
}
