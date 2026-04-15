package br.com.murilo.liberthia.entry;

import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class WorkerVoiceScreen extends Screen {

    private static final int PANEL_W = 360;
    private static final int PANEL_H = 220;

    private record SoundEntry(String label, String id) {}

    private static final SoundEntry[] SOUNDS = new SoundEntry[] {
            new SoundEntry("Caverna Ambiente",  "minecraft:ambient.cave"),
            new SoundEntry("Warden Roar",       "minecraft:entity.warden.roar"),
            new SoundEntry("Warden Sonic Boom", "minecraft:entity.warden.sonic_boom"),
            new SoundEntry("Sculk Shrieker",    "minecraft:block.sculk_shrieker.shriek"),
            new SoundEntry("Ghast Scream",      "minecraft:entity.ghast.scream"),
            new SoundEntry("Ghast Hurt",        "minecraft:entity.ghast.hurt"),
            new SoundEntry("Creeper Primed",    "minecraft:entity.creeper.primed"),
            new SoundEntry("Creeper Death",     "minecraft:entity.creeper.death"),
            new SoundEntry("Creeper Hurt",      "minecraft:entity.creeper.hurt"),
            new SoundEntry("Zombie Ambient",    "minecraft:entity.zombie.ambient"),
            new SoundEntry("Zombie Death",      "minecraft:entity.zombie.death"),
            new SoundEntry("Skeleton Step",     "minecraft:entity.skeleton.step"),
            new SoundEntry("Skeleton Ambient",  "minecraft:entity.skeleton.ambient"),
            new SoundEntry("Enderman Stare",    "minecraft:entity.enderman.stare"),
            new SoundEntry("Enderman Scream",   "minecraft:entity.enderman.scream"),
            new SoundEntry("Wither Spawn",      "minecraft:entity.wither.spawn"),
            new SoundEntry("Wither Death",      "minecraft:entity.wither.death"),
            new SoundEntry("Lightning Thunder", "minecraft:entity.lightning_bolt.thunder"),
            new SoundEntry("Portal Trigger",    "minecraft:block.portal.trigger"),
            new SoundEntry("Portal Travel",     "minecraft:block.portal.travel"),
            new SoundEntry("Explosion",         "minecraft:entity.generic.explode"),
            new SoundEntry("Bell Resonate",     "minecraft:block.bell.resonate"),
            new SoundEntry("Door Creak",        "minecraft:block.wooden_door.open"),
            new SoundEntry("Heartbeat",         "minecraft:entity.warden.heartbeat"),
    };

    private final List<AdminPlayerEntry> players = new ArrayList<>();

    private int selectedSound = 0;
    private int selectedPlayer = -1; // -1 = self
    private int soundScroll = 0;
    private int playerScroll = 0;
    private int refreshTicker = 0;

    private EditBox volumeBox;
    private EditBox pitchBox;

    public WorkerVoiceScreen() {
        super(Component.literal("Voice Box"));
        this.players.addAll(AdminClientState.snapshotPlayers());
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        volumeBox = new EditBox(this.font, left + 38, top + 184, 36, 14, Component.literal("Vol"));
        volumeBox.setValue("2.0");
        volumeBox.setMaxLength(4);
        this.addRenderableWidget(volumeBox);

        pitchBox = new EditBox(this.font, left + 110, top + 184, 36, 14, Component.literal("Pitch"));
        pitchBox.setValue("1.0");
        pitchBox.setMaxLength(4);
        this.addRenderableWidget(pitchBox);

        this.addRenderableWidget(Button.builder(Component.literal("Tocar"), b -> play())
                .bounds(left + 154, top + 182, 60, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Self"), b -> selectedPlayer = -1)
                .bounds(left + 218, top + 182, 40, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("R"), b -> requestPlayers())
                .bounds(left + 262, top + 182, 24, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("X"), b -> this.onClose())
                .bounds(left + 290, top + 182, 24, 18).build());

        requestPlayers();
    }

    @Override
    public void tick() {
        super.tick();
        volumeBox.tick();
        pitchBox.tick();
        refreshTicker++;
        if (refreshTicker >= 40) {
            refreshTicker = 0;
            this.players.clear();
            this.players.addAll(AdminClientState.snapshotPlayers());
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void requestPlayers() {
        ModNetwork.CHANNEL.sendToServer(AdminActionC2SPacket.requestPlayers());
    }

    private void play() {
        if (selectedSound < 0 || selectedSound >= SOUNDS.length) return;
        float v = parseF(volumeBox.getValue(), 2.0F);
        float p = parseF(pitchBox.getValue(), 1.0F);
        java.util.UUID target = null;
        if (selectedPlayer >= 0 && selectedPlayer < players.size()) {
            target = players.get(selectedPlayer).uuid();
        }
        ModNetwork.CHANNEL.sendToServer(
                new WorkerVoicePlayC2SPacket(target, SOUNDS[selectedSound].id(), v, p)
        );
    }

    private float parseF(String s, float def) {
        try { return Float.parseFloat(s.trim()); } catch (Exception e) { return def; }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        this.renderBackground(g);

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        g.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xFFAA55AA);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF0101020);
        g.fill(left, top, left + PANEL_W, top + 14, 0xFF552255);
        g.drawString(this.font, "VOICE BOX", left + 6, top + 3, 0xFFAAFF, false);

        // Sounds list
        g.fill(left + 6, top + 18, left + 200, top + 30, 0xFF553388);
        g.drawString(this.font, "SONS", left + 10, top + 20, 0xFFAAFF, false);
        g.fill(left + 6, top + 30, left + 200, top + 178, 0x66000000);

        int visible = 12;
        for (int i = 0; i < visible; i++) {
            int idx = soundScroll + i;
            if (idx >= SOUNDS.length) break;
            int rowY = top + 33 + i * 12;
            if (idx == selectedSound) {
                g.fill(left + 7, rowY - 1, left + 199, rowY + 10, 0x66AA55AA);
            }
            g.drawString(this.font, SOUNDS[idx].label(), left + 9, rowY, 0xFFFFFF, false);
        }

        // Players list (target picker)
        g.fill(left + 204, top + 18, left + 354, top + 30, 0xFF223366);
        g.drawString(this.font, "ALVO", left + 208, top + 20, 0xAACCFF, false);
        g.fill(left + 204, top + 30, left + 354, top + 178, 0x66000000);

        int pVis = 12;
        for (int i = 0; i < pVis; i++) {
            int idx = playerScroll + i;
            if (idx >= players.size()) break;
            int rowY = top + 33 + i * 12;
            if (idx == selectedPlayer) {
                g.fill(left + 205, rowY - 1, left + 353, rowY + 10, 0x66339933);
            }
            g.drawString(this.font, cut(players.get(idx).name(), 18), left + 207, rowY, 0xFFFFFF, false);
        }

        g.drawString(this.font, "Vol", left + 14, top + 187, 0xCCCCCC, false);
        g.drawString(this.font, "Pit", left + 86, top + 187, 0xCCCCCC, false);
        g.drawString(this.font, selectedPlayer == -1 ? "[Self]" : "Target: " + (selectedPlayer + 1), left + 6, top + 204, 0xFFAA88, false);

        super.render(g, mouseX, mouseY, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        if (inside(mx, my, left + 6, top + 30, 194, 148)) {
            int row = ((int) my - (top + 33)) / 12;
            int idx = soundScroll + row;
            if (row >= 0 && idx >= 0 && idx < SOUNDS.length) {
                selectedSound = idx;
                return true;
            }
        }

        if (inside(mx, my, left + 204, top + 30, 150, 148)) {
            int row = ((int) my - (top + 33)) / 12;
            int idx = playerScroll + row;
            if (row >= 0 && idx >= 0 && idx < players.size()) {
                selectedPlayer = idx;
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        if (inside(mx, my, left + 6, top + 30, 194, 148)) {
            int max = Math.max(0, SOUNDS.length - 12);
            soundScroll = Mth.clamp(soundScroll - (int) Math.signum(delta), 0, max);
            return true;
        }
        if (inside(mx, my, left + 204, top + 30, 150, 148)) {
            int max = Math.max(0, players.size() - 12);
            playerScroll = Mth.clamp(playerScroll - (int) Math.signum(delta), 0, max);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String cut(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }
}
