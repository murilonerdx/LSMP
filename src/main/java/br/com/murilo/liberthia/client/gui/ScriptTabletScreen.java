package br.com.murilo.liberthia.client.gui;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SaveScriptTabletC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Multi-line editor for the LiberScript tablet. Includes a Verbose toggle and
 * skeleton-insert buttons for if/while/repeat/etc.
 */
public class ScriptTabletScreen extends Screen {
    private final String initialSource;
    private final String initialLabel;
    private boolean verbose;
    private MultiLineEditBox editor;
    private EditBox labelBox;
    private Button verboseBtn;

    private static final String SAMPLE =
            "# Script de exemplo — spawn de zumbis em frente ao jogador.\n" +
            "#\n" +
            "# 2 cuidados que parecem bugs mas não são:\n" +
            "#   1) Em Peaceful o vanilla apaga TODOS os mobs hostis no\n" +
            "#      próximo tick, independente de PersistenceRequired.\n" +
            "#      A linha abaixo força easy antes de spawnar.\n" +
            "#   2) ~ ~ ~ spawna em cima do jogador → sufocação.\n" +
            "#      Use ^ ^ ^3 (3 blocos na direção que você olha).\n" +
            "#\n" +
            "run difficulty easy\n" +
            "let n = 5\n" +
            "while n > 0\n" +
            "  run summon zombie ^ ^ ^3 {PersistenceRequired:1b}\n" +
            "  let n = n - 1\n" +
            "  wait 10\n" +
            "end\n" +
            "say {n} zumbis spawnados (era pra ser 5)\n";

    public ScriptTabletScreen(String source, String label, boolean verbose) {
        super(Component.literal("Tablete de Script (LiberScript)"));
        this.initialSource = source == null ? "" : source;
        this.initialLabel = label == null ? "" : label;
        this.verbose = verbose;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int cx = this.width / 2;
        int boxW = Math.min(460, this.width - 60);
        int leftX = cx - boxW / 2;

        this.labelBox = new EditBox(this.font, leftX, 24, boxW, 20,
                Component.literal("Apelido"));
        this.labelBox.setMaxLength(64);
        this.labelBox.setHint(Component.literal("apelido (opcional)"));
        this.labelBox.setValue(this.initialLabel);
        this.addRenderableWidget(this.labelBox);

        int editorY = 50;
        int editorH = this.height - editorY - 90;
        this.editor = new MultiLineEditBox(this.font,
                leftX, editorY, boxW, editorH,
                Component.literal("Cole seu script aqui."),
                Component.literal("source"));
        this.editor.setCharacterLimit(16_000);
        this.editor.setValue(this.initialSource.isEmpty() ? SAMPLE : this.initialSource);
        this.addRenderableWidget(this.editor);

        int btnY = this.height - 64;
        this.addRenderableWidget(Button.builder(Component.literal("Salvar"), b -> save())
                .bounds(cx - 230, btnY, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Salvar e Fechar"),
                        b -> { save(); this.onClose(); })
                .bounds(cx - 155, btnY, 130, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancelar"),
                        b -> this.onClose())
                .bounds(cx - 20, btnY, 80, 20).build());
        this.verboseBtn = Button.builder(verboseLabel(), b -> {
                    this.verbose = !this.verbose;
                    b.setMessage(verboseLabel());
                })
                .bounds(cx + 65, btnY, 165, 20).build();
        this.addRenderableWidget(this.verboseBtn);

        int hY = this.height - 36;
        this.addRenderableWidget(Button.builder(Component.literal("+ if"),
                        b -> insertAtEnd("if x == 0\n  \nelse\n  \nend"))
                .bounds(cx - 220, hY, 60, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+ while"),
                        b -> insertAtEnd("while x > 0\n  \n  let x = x - 1\nend"))
                .bounds(cx - 155, hY, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+ repeat"),
                        b -> insertAtEnd("repeat 5\n  say {?}\nend"))
                .bounds(cx - 80, hY, 75, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+ wait"),
                        b -> insertAtEnd("wait 20"))
                .bounds(cx, hY, 60, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+ run"),
                        b -> insertAtEnd("run effect give @s minecraft:speed 30 1"))
                .bounds(cx + 65, hY, 60, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+ summon"),
                        b -> insertAtEnd("run summon zombie ^ ^ ^3 {PersistenceRequired:1b}"))
                .bounds(cx + 130, hY, 80, 20).build());
    }

    private Component verboseLabel() {
        return this.verbose
                ? Component.literal("Saída: §averbosa")
                : Component.literal("Saída: §csilenciosa");
    }

    private void insertAtEnd(String snippet) {
        if (this.editor == null) return;
        String cur = this.editor.getValue();
        if (!cur.isEmpty() && !cur.endsWith("\n")) cur += "\n";
        this.editor.setValue(cur + snippet + "\n");
    }

    private void save() {
        if (this.editor == null) return;
        String src = this.editor.getValue();
        String label = this.labelBox == null ? "" : this.labelBox.getValue();
        ModNetwork.CHANNEL.sendToServer(new SaveScriptTabletC2SPacket(src, label, this.verbose));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        int cx = this.width / 2;
        g.drawCenteredString(this.font, this.title, cx, 8, 0xFFFFFF);
        g.drawString(this.font,
                Component.literal("§7§oif/else/end · while/end · repeat N/end · let x = expr · wait N · say ... · run /cmd · # comentário · {var} interpola"),
                cx - 220, this.height - 78, 0xCCCCCC, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
