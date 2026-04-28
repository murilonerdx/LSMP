package br.com.murilo.liberthia.client.gui;

import br.com.murilo.liberthia.item.CommandTabletItem;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SaveCommandTabletC2SPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Multi-line command editor for the Command Tablet, with a "compose" line at
 * the top that uses vanilla {@link CommandSuggestions} for full Brigadier
 * autocomplete (Tab completes, Enter or "+" appends to the program below).
 */
public class CommandTabletScreen extends Screen {
    private final List<String> initialCommands;
    private final String initialLabel;
    private String targetMode;
    private String targetName;
    private boolean verbose;

    private MultiLineEditBox editor;
    private EditBox labelBox;
    private EditBox targetNameBox;
    private Button targetModeBtn;
    private Button verboseBtn;

    private EditBox composeBox;
    private CommandSuggestions composeSuggestions;

    public CommandTabletScreen(List<String> commands, String label,
                               String targetMode, String targetName,
                               boolean verbose) {
        super(Component.literal("Tablete de Comando"));
        this.initialCommands = new ArrayList<>(commands);
        this.initialLabel = label == null ? "" : label;
        this.targetMode = CommandTabletItem.MODE_OTHER.equalsIgnoreCase(targetMode)
                ? CommandTabletItem.MODE_OTHER : CommandTabletItem.MODE_SELF;
        this.targetName = targetName == null ? "" : targetName;
        this.verbose = verbose;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int cx = this.width / 2;
        int boxW = Math.min(420, this.width - 60);
        int leftX = cx - boxW / 2;

        // Row 1 (y=24): label | mode | target name
        int row1Y = 24;
        int labelW = boxW / 2 - 6;
        this.labelBox = new EditBox(this.font, leftX, row1Y, labelW, 20,
                Component.literal("Apelido"));
        this.labelBox.setMaxLength(64);
        this.labelBox.setHint(Component.literal("apelido (opcional)"));
        this.labelBox.setValue(this.initialLabel);
        this.addRenderableWidget(this.labelBox);

        this.targetModeBtn = Button.builder(modeButtonLabel(), b -> {
                    cycleMode();
                    b.setMessage(modeButtonLabel());
                    refreshTargetBoxState();
                })
                .bounds(leftX + labelW + 6, row1Y, 90, 20).build();
        this.addRenderableWidget(this.targetModeBtn);

        int targetBoxX = leftX + labelW + 6 + 90 + 6;
        int targetBoxW = boxW - (labelW + 6 + 90 + 6);
        this.targetNameBox = new EditBox(this.font, targetBoxX, row1Y, targetBoxW, 20,
                Component.literal("Nome do jogador"));
        this.targetNameBox.setMaxLength(32);
        this.targetNameBox.setHint(Component.literal("nome do jogador"));
        this.targetNameBox.setValue(this.targetName);
        this.targetNameBox.setResponder(s -> this.targetName = s);
        this.addRenderableWidget(this.targetNameBox);
        refreshTargetBoxState();

        // Row 2 (y=50): compose box with autocomplete + add button
        int composeY = 50;
        int addBtnW = 24;
        this.composeBox = new EditBox(this.font, leftX, composeY, boxW - addBtnW - 4, 20,
                Component.literal("Comando"));
        this.composeBox.setMaxLength(256);
        this.composeBox.setHint(Component.literal("digite um comando — Tab completa, Enter adiciona"));
        this.composeBox.setResponder(s -> {
            if (this.composeSuggestions != null) this.composeSuggestions.updateCommandInfo();
        });
        this.addRenderableWidget(this.composeBox);

        this.addRenderableWidget(Button.builder(Component.literal("+"),
                        b -> appendComposeToProgram())
                .bounds(leftX + boxW - addBtnW, composeY, addBtnW, 20).build());

        // Brigadier-powered suggestions, anchored above the compose box (drawn upwards)
        // so they don't hide the multi-line editor below.
        this.composeSuggestions = new CommandSuggestions(
                this.minecraft, this, this.composeBox, this.font,
                /*commandsOnly*/ true,
                /*onlyShowIfCursorPastError*/ false,
                /*lineStartOffset*/ 1,
                /*suggestionLineLimit*/ 8,
                /*anchorToBottom*/ true,
                /*fillColor*/ 0xD0000000);
        this.composeSuggestions.setAllowSuggestions(true);
        this.composeSuggestions.updateCommandInfo();

        // Row 3: multi-line editor
        int editorY = 76;
        int editorH = this.height - editorY - 90;
        this.editor = new MultiLineEditBox(this.font,
                leftX, editorY, boxW, editorH,
                Component.literal("Um comando por linha. Ex: scale add 0.1 @s"),
                Component.literal("comandos"));
        this.editor.setCharacterLimit(32 * 256);
        this.editor.setValue(String.join("\n", this.initialCommands));
        this.addRenderableWidget(this.editor);

        // Row 4: action buttons
        int btnY = this.height - 64;
        this.addRenderableWidget(Button.builder(Component.literal("Salvar"), b -> save())
                .bounds(cx - 230, btnY, 70, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Salvar e Fechar"),
                        b -> { save(); this.onClose(); })
                .bounds(cx - 155, btnY, 130, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> this.onClose())
                .bounds(cx - 20, btnY, 80, 20).build());
        this.verboseBtn = Button.builder(verboseLabel(), b -> {
                    this.verbose = !this.verbose;
                    b.setMessage(verboseLabel());
                })
                .bounds(cx + 65, btnY, 165, 20).build();
        this.addRenderableWidget(this.verboseBtn);

        // Row 5: quick-insert helper buttons
        int hY = this.height - 36;
        this.addRenderableWidget(Button.builder(Component.literal("+ /scale add 0.1 @s"),
                        b -> insertAtEnd("scale add 0.1 @s"))
                .bounds(cx - 200, hY, 130, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+ /effect give @s ..."),
                        b -> insertAtEnd("effect give @s minecraft:speed 30 1"))
                .bounds(cx - 65, hY, 140, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+ {target}"),
                        b -> insertAtEnd("say Olá {target}!"))
                .bounds(cx + 80, hY, 120, 20).build());
    }

    private void appendComposeToProgram() {
        if (this.composeBox == null || this.editor == null) return;
        String line = this.composeBox.getValue().trim();
        if (line.isEmpty()) return;
        if (line.startsWith("/")) line = line.substring(1);
        String cur = this.editor.getValue();
        if (!cur.isEmpty() && !cur.endsWith("\n")) cur += "\n";
        this.editor.setValue(cur + line);
        this.composeBox.setValue("");
        if (this.composeSuggestions != null) this.composeSuggestions.updateCommandInfo();
        this.setFocused(this.composeBox);
    }

    private void cycleMode() {
        this.targetMode = CommandTabletItem.MODE_SELF.equals(this.targetMode)
                ? CommandTabletItem.MODE_OTHER : CommandTabletItem.MODE_SELF;
    }

    private Component modeButtonLabel() {
        return CommandTabletItem.MODE_OTHER.equals(this.targetMode)
                ? Component.literal("Alvo: §eOutro")
                : Component.literal("Alvo: §aSelf");
    }

    private Component verboseLabel() {
        return this.verbose
                ? Component.literal("Saída: §averbosa")
                : Component.literal("Saída: §csilenciosa");
    }

    private void refreshTargetBoxState() {
        if (this.targetNameBox == null) return;
        boolean other = CommandTabletItem.MODE_OTHER.equals(this.targetMode);
        this.targetNameBox.setEditable(other);
        this.targetNameBox.active = other;
        this.targetNameBox.setVisible(true);
    }

    private void insertAtEnd(String snippet) {
        if (this.editor == null) return;
        String cur = this.editor.getValue();
        if (!cur.isEmpty() && !cur.endsWith("\n")) cur += "\n";
        this.editor.setValue(cur + snippet);
    }

    private void save() {
        if (this.editor == null) return;
        String all = this.editor.getValue();
        String[] parts = all.split("\\R");
        List<String> cmds = new ArrayList<>(Arrays.asList(parts));
        while (!cmds.isEmpty() && cmds.get(cmds.size() - 1).isBlank()) {
            cmds.remove(cmds.size() - 1);
        }
        if (cmds.size() > 32) cmds = cmds.subList(0, 32);
        String label = this.labelBox == null ? "" : this.labelBox.getValue();
        String name = this.targetNameBox == null ? "" : this.targetNameBox.getValue();
        ModNetwork.CHANNEL.sendToServer(
                new SaveCommandTabletC2SPacket(cmds, label, this.targetMode, name, this.verbose));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.composeBox != null && this.composeBox.isFocused()
                && this.composeSuggestions != null
                && this.composeSuggestions.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.composeBox != null && this.composeBox.isFocused()
                && (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)) {
            appendComposeToProgram();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (this.composeSuggestions != null
                && this.composeSuggestions.mouseClicked((int) x, (int) y, button)) {
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (this.composeSuggestions != null && this.composeSuggestions.mouseScrolled(delta)) {
            return true;
        }
        return super.mouseScrolled(x, y, delta);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        if (this.composeSuggestions != null) {
            this.composeSuggestions.render(g, mx, my);
        }
        int cx = this.width / 2;
        g.drawCenteredString(this.font, this.title, cx, 8, 0xFFFFFF);
        g.drawString(this.font,
                Component.literal("§7§oTab completa · §oEnter adiciona · §o#§r§7§o comenta · §o@<ticks>§r§7§o atrasa · §o{target}§r§7§o = nome do alvo"),
                cx - 220, this.height - 78, 0xCCCCCC, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
