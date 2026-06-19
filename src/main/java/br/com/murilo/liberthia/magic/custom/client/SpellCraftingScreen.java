package br.com.murilo.liberthia.magic.custom.client;

import br.com.murilo.liberthia.magic.custom.CraftSpellC2SPacket;
import br.com.murilo.liberthia.magic.custom.CustomSpell;
import br.com.murilo.liberthia.magic.custom.SpellElement;
import br.com.murilo.liberthia.magic.custom.SpellShape;
import br.com.murilo.liberthia.magic.custom.SpellSprite;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.22 r44: <b>Spell Crafting Screen — LAYOUT COMPACTO</b>
 *
 * <h2>r44 FIX</h2>
 * <ul>
 *   <li>Layout reduzido pra ~240px (cabe em qualquer GUI scale ≥3)</li>
 *   <li>2 COLUNAS: esquerda = sprites, direita = forma/elemento/poder/save</li>
 *   <li>Sprite buttons 24×24 (era 28×28)</li>
 *   <li>Shape/Element buttons 16 tall (era 18)</li>
 *   <li>SALVAR button IMEDIATAMENTE após Power (sem gap)</li>
 *   <li>Stats inline com Poder label, não em row separada</li>
 *   <li>Anchored TOP — title sempre visível, escala pro baixo</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class SpellCraftingScreen extends Screen {

    private EditBox nameBox;
    private SpellSprite selectedSprite = SpellSprite.FIRE_BLAST;
    private SpellShape selectedShape = SpellShape.PROJECTILE;
    private SpellElement selectedElement = SpellElement.FIRE;
    private int selectedPower = 3;

    private final Map<SpellSprite, SpellIconButton> spriteButtons = new HashMap<>();
    private final Map<SpellShape, Button> shapeButtons = new HashMap<>();
    private final Map<SpellElement, Button> elementButtons = new HashMap<>();
    private final Map<Integer, Button> powerButtons = new HashMap<>();

    private Button craftBtn;

    // Anchor layouts in 2 columns
    private static final int COL_WIDTH = 170;
    private static final int LEFT_COL_X = -180;    // relative to cx
    private static final int RIGHT_COL_X = 10;

    public SpellCraftingScreen() {
        super(Component.literal("Spell Crafting"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        spriteButtons.clear();
        shapeButtons.clear();
        elementButtons.clear();
        powerButtons.clear();

        int cx = width / 2;
        // Center vertically with comfortable padding — total ~240px tall
        int yTop = Math.max(6, (height - 240) / 2);

        // ───────── HEADER (title + name input full-width) ─────────
        // Title at yTop=0
        // Name input at yTop=14 (full width)
        // r140: campo de nome SEM placeholder text — fica realmente vazio até o player digitar
        nameBox = new EditBox(font, cx - 95, yTop + 22, 190, 14, Component.empty());
        nameBox.setMaxLength(32);
        nameBox.setValue("");
        // removido setSuggestion — usuário pediu campo vazio sem texto placeholder
        this.addRenderableWidget(nameBox);

        // ───────── LEFT COLUMN — Sprite VFX (10 buttons 24×24, 5 per row) ─────────
        SpellSprite[] sprites = SpellSprite.values();
        int spriteSize = 24;
        int spriteSpacing = 3;
        int spriteRowWidth = 5 * spriteSize + 4 * spriteSpacing;
        int spriteStartX = cx + LEFT_COL_X + (COL_WIDTH - spriteRowWidth) / 2;
        for (int i = 0; i < sprites.length; i++) {
            final SpellSprite sp = sprites[i];
            int col = i % 5;
            int row = i / 5;
            int bx = spriteStartX + col * (spriteSize + spriteSpacing);
            int by = yTop + 56 + row * (spriteSize + spriteSpacing);
            ResourceLocation tex = new ResourceLocation("liberthia",
                    "textures/particle/spell_" + sp.id + "_0.png");
            SpellIconButton btn = new SpellIconButton(bx, by, spriteSize, tex, sp.color,
                    Component.literal(sp.displayName + "\n§7" + sp.description),
                    () -> {
                        selectedSprite = sp;
                        refreshSelections();
                    });
            spriteButtons.put(sp, btn);
            this.addRenderableWidget(btn);
        }

        // ───────── RIGHT COLUMN — Shape (6 buttons inline) ─────────
        SpellShape[] shapes = SpellShape.values();
        int shapeWidth = 50;
        int shapeSpacing = 2;
        // 3 per row, 2 rows
        for (int i = 0; i < shapes.length; i++) {
            final SpellShape sh = shapes[i];
            int col = i % 3;
            int row = i / 3;
            int bx = cx + RIGHT_COL_X + col * (shapeWidth + shapeSpacing);
            int by = yTop + 56 + row * 18;
            Button b = Button.builder(Component.literal(sh.displayName), btn -> {
                selectedShape = sh;
                refreshSelections();
            })
                    .bounds(bx, by, shapeWidth, 16)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.literal("§l" + sh.displayName + "\n§7" + sh.description
                                    + "\n§7Mana base: " + sh.baseManaCost)))
                    .build();
            shapeButtons.put(sh, b);
            this.addRenderableWidget(b);
        }

        // ───────── LEFT COLUMN — Element (10 buttons, 2 rows of 5, 32 wide) ─────────
        SpellElement[] elems = SpellElement.values();
        int elemWidth = 32;
        int elemSpacing = 2;
        int elemRowWidth = 5 * elemWidth + 4 * elemSpacing;
        int elemStartX = cx + LEFT_COL_X + (COL_WIDTH - elemRowWidth) / 2;
        for (int i = 0; i < elems.length; i++) {
            final SpellElement el = elems[i];
            int col = i % 5;
            int row = i / 5;
            int bx = elemStartX + col * (elemWidth + elemSpacing);
            int by = yTop + 132 + row * 18;
            Button b = Button.builder(shortElemLabel(el), btn -> {
                selectedElement = el;
                refreshSelections();
            })
                    .bounds(bx, by, elemWidth, 16)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                            el.displayComponent().copy().append(Component.literal(
                                    "\n§7" + el.description))))
                    .build();
            elementButtons.put(el, b);
            this.addRenderableWidget(b);
        }

        // ───────── RIGHT COLUMN — Power (5 buttons inline) ─────────
        int powerSize = 22;
        int powerSpacing = 3;
        int powerRowWidth = 5 * powerSize + 4 * powerSpacing;
        int powerStartX = cx + RIGHT_COL_X + (COL_WIDTH - powerRowWidth) / 2;
        for (int i = 1; i <= 5; i++) {
            final int p = i;
            int bx = powerStartX + (i - 1) * (powerSize + powerSpacing);
            int by = yTop + 110;
            Button b = Button.builder(Component.literal("§l§n" + p), btn -> {
                selectedPower = p;
                refreshSelections();
            })
                    .bounds(bx, by, powerSize, 18)
                    .build();
            powerButtons.put(p, b);
            this.addRenderableWidget(b);
        }

        // ───────── SAVE button (centered, 140 wide) + MEUS FEITIÇOS (gerenciar) ─────────
        craftBtn = Button.builder(
                Component.literal("§a§l✦ SALVAR ✦"),
                btn -> tryCraft())
                .bounds(cx - 105, yTop + 188, 140, 22)
                .build();
        this.addRenderableWidget(craftBtn);

        // r140: Botão "Meus Feitiços" — abre tela de gerenciamento (lista + delete)
        Button manageBtn = Button.builder(
                Component.literal("§e§l📜"),
                btn -> {
                    if (minecraft != null) {
                        minecraft.setScreen(new SpellManagerScreen(this));
                    }
                })
                .bounds(cx + 40, yTop + 188, 65, 22)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("§eMeus Feitiços\n§7Ver, selecionar e deletar feitiços criados")))
                .build();
        this.addRenderableWidget(manageBtn);

        refreshSelections();
    }

    /** Curta o label do element pra caber em 32px de largura. */
    private static Component shortElemLabel(SpellElement el) {
        String s = switch (el) {
            case FIRE -> "Fogo";
            case ICE -> "Gelo";
            case VOID -> "Vazio";
            case LIGHT -> "Luz";
            case BLOOD -> "Sang";
            case ARCANE -> "Arc";
            case EARTH -> "Ter";
            case LIGHTNING -> "Elet";
            case SHADOW -> "Som";
            case NATURE -> "Nat";
        };
        return Component.literal(s).withStyle(t -> t.withColor(el.color));
    }

    private void refreshSelections() {
        spriteButtons.forEach((sp, btn) -> btn.setSelected(sp == selectedSprite));
    }

    private void tryCraft() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) {
            // r140: SEM setSuggestion — só action bar message
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal("§c⚠ Dê um nome ao feitiço antes!"), true);
            }
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new CraftSpellC2SPacket(
                name, selectedSprite, selectedShape, selectedElement, selectedPower));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Dark cosmic background
        g.fillGradient(0, 0, width, height, 0xEE0A0518, 0xFF1A0830);

        int cx = width / 2;
        int yTop = Math.max(6, (height - 240) / 2);

        // ───────── Title ─────────
        Component title = Component.literal("§5§l✦ CRIAR FEITIÇO ✦");
        int tw = font.width(title);
        g.drawString(font, title, cx - tw / 2, yTop, 0xFFFFFFFF, true);

        // ───────── Name label ─────────
        g.drawString(font, "§dNome:", cx - 95, yTop + 12, 0xFFFFFFFF, true);

        // ───────── LEFT COL labels ─────────
        // Sprite (acima do bloco de sprites)
        g.drawString(font, "§d§lSprite VFX §r§7" + selectedSprite.displayName,
                cx + LEFT_COL_X + 4, yTop + 46, 0xFFFFFFFF, true);

        // Elemento (acima do bloco de elementos)
        g.drawString(font, "§d§lElemento §r" + selectedElement.displayName,
                cx + LEFT_COL_X + 4, yTop + 122, 0xFFFFFFFF, true);

        // ───────── RIGHT COL labels ─────────
        // Forma (acima do bloco de shape buttons)
        g.drawString(font, "§d§lForma §r§7" + selectedShape.displayName,
                cx + RIGHT_COL_X, yTop + 46, 0xFFFFFFFF, true);

        // Power label + stats inline (acima do bloco de power buttons)
        CustomSpell preview = new CustomSpell(UUID.randomUUID(), "preview",
                selectedSprite, selectedShape, selectedElement, selectedPower);
        String statLine = String.format(
                "§d§lPoder §r§6§l%d§r§7 | §bM%d §c%ddmg §e%.1fs §aR%db",
                selectedPower, preview.manaCost(), (int) preview.damage(),
                preview.cooldownTicks() / 20.0, preview.range());
        g.drawString(font, statLine, cx + RIGHT_COL_X, yTop + 100, 0xFFFFFFFF, true);

        // ───────── Highlights (gold border em selected) ─────────
        // Power
        Button selPower = powerButtons.get(selectedPower);
        if (selPower != null) drawGoldBorder(g, selPower);
        // Shape
        Button selShape = shapeButtons.get(selectedShape);
        if (selShape != null) drawGoldBorder(g, selShape);
        // Element
        Button selElem = elementButtons.get(selectedElement);
        if (selElem != null) drawGoldBorder(g, selElem);

        // ───────── Hint ─────────
        String hint = "§7Atalhos: §61-5§7 = poder | §6Enter§7 = salvar | §6ESC§7 = cancelar";
        int hw = font.width(hint);
        g.drawString(font, hint, cx - hw / 2, yTop + 220, 0xFFAAAAAA, true);

        super.render(g, mouseX, mouseY, partial);
    }

    private void drawGoldBorder(GuiGraphics g, Button b) {
        int px = b.getX() - 1;
        int py = b.getY() - 1;
        int pw = b.getWidth() + 2;
        int ph = b.getHeight() + 2;
        int gold = 0xFFFFCC00;
        g.fill(px, py, px + pw, py + 1, gold);
        g.fill(px, py + ph - 1, px + pw, py + ph, gold);
        g.fill(px, py, px + 1, py + ph, gold);
        g.fill(px + pw - 1, py, px + pw, py + ph, gold);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_5) {
            if (nameBox != null && !nameBox.isFocused()) {
                selectedPower = (keyCode - GLFW.GLFW_KEY_0);
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (nameBox != null && nameBox.isFocused()) {
                nameBox.setFocused(false);
                return true;
            }
            tryCraft();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
