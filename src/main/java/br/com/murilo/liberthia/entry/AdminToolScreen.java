package br.com.murilo.liberthia.entry;

import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class AdminToolScreen extends Screen {

    private static final int PANEL_W = 400;
    private static final int PANEL_H = 244;

    private final List<AdminPlayerEntry> players = new ArrayList<>();
    private final List<MobEffect> allEffects = new ArrayList<>();
    private final List<MobEffect> effects = new ArrayList<>();

    private int selectedPlayerIndex = -1;
    private int selectedEffectIndex = 0;
    private int playerScroll = 0;
    private int effectScroll = 0;
    private int refreshTicker = 0;
    private String effectFilter = "";

    private EditBox effectSearchBox;
    private EditBox durationBox;
    private EditBox amplifierBox;
    private EditBox itemIdBox;
    private EditBox itemCountBox;
    private EditBox entityIdBox;
    private EditBox entityCountBox;

    public AdminToolScreen() {
        super(Component.literal("Admin Tool"));
        this.players.addAll(AdminClientState.snapshotPlayers());
    }

    @Override
    protected void init() {
        if (allEffects.isEmpty()) {
            allEffects.addAll(
                    ForgeRegistries.MOB_EFFECTS.getValues().stream()
                            .sorted(Comparator.comparing(effect -> {
                                ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                                return key == null ? "" : key.toString();
                            }))
                            .toList()
            );
        }
        rebuildEffects();

        if (!players.isEmpty() && selectedPlayerIndex < 0) {
            selectedPlayerIndex = 0;
        }

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Effect search box — sits in the effects header row.
        effectSearchBox = new EditBox(this.font, left + 220, top + 133, 170, 10,
                Component.literal("Buscar efeito..."));
        effectSearchBox.setMaxLength(48);
        effectSearchBox.setValue(effectFilter);
        effectSearchBox.setResponder(txt -> {
            effectFilter = txt == null ? "" : txt;
            effectScroll = 0;
            selectedEffectIndex = effects.isEmpty() ? -1 : 0;
            rebuildEffects();
        });
        this.addRenderableWidget(effectSearchBox);

        // Section B controls (effects)
        durationBox = new EditBox(this.font, left + 24, top + 184, 30, 14, Component.literal("Sec"));
        durationBox.setValue("10");
        durationBox.setMaxLength(4);
        this.addRenderableWidget(durationBox);

        amplifierBox = new EditBox(this.font, left + 80, top + 184, 24, 14, Component.literal("Amp"));
        amplifierBox.setValue("0");
        amplifierBox.setMaxLength(3);
        this.addRenderableWidget(amplifierBox);

        this.addRenderableWidget(Button.builder(Component.literal("Aplicar"), b -> applyEffect())
                .bounds(left + 110, top + 182, 60, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Limpar"), b -> clearEffect())
                .bounds(left + 174, top + 182, 56, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Susto"), b -> scareSingle())
                .bounds(left + 234, top + 182, 50, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Susto Área"), b -> scareArea())
                .bounds(left + 288, top + 182, 70, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("R"), b -> requestPlayers())
                .bounds(left + 362, top + 182, 30, 18).build());

        // Section C row 1 — items
        itemIdBox = new EditBox(this.font, left + 6, top + 206, 168, 14, Component.literal("Item ID"));
        itemIdBox.setValue("minecraft:diamond");
        itemIdBox.setMaxLength(120);
        this.addRenderableWidget(itemIdBox);

        itemCountBox = new EditBox(this.font, left + 178, top + 206, 22, 14, Component.literal("Qtd"));
        itemCountBox.setValue("1");
        itemCountBox.setMaxLength(4);
        this.addRenderableWidget(itemCountBox);

        this.addRenderableWidget(Button.builder(Component.literal("Dar"), b -> giveItem())
                .bounds(left + 204, top + 204, 30, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Rem"), b -> removeItem())
                .bounds(left + 236, top + 204, 30, 18).build());
        // "Bloco": places block/fluid at target (count=radius, 0=single, >=1=cube)
        this.addRenderableWidget(Button.builder(Component.literal("Bloco"), b -> placeBlock())
                .bounds(left + 268, top + 204, 34, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Inv"), b -> requestInventory())
                .bounds(left + 304, top + 204, 28, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Pos"), b -> sendPosition())
                .bounds(left + 334, top + 204, 26, 18).build());
        // Water shortcut
        this.addRenderableWidget(Button.builder(Component.literal("Wt"), b -> { itemIdBox.setValue("minecraft:water"); placeBlock(); })
                .bounds(left + 362, top + 204, 14, 18).build());
        // Dark matter fluid shortcut
        this.addRenderableWidget(Button.builder(Component.literal("Dk"), b -> { itemIdBox.setValue("liberthia:dark_matter"); placeBlock(); })
                .bounds(left + 378, top + 204, 14, 18).build());

        // Section C row 2 — monsters
        entityIdBox = new EditBox(this.font, left + 6, top + 224, 168, 14, Component.literal("Entity ID"));
        entityIdBox.setValue("minecraft:zombie");
        entityIdBox.setMaxLength(120);
        this.addRenderableWidget(entityIdBox);

        entityCountBox = new EditBox(this.font, left + 178, top + 224, 22, 14, Component.literal("Qtd"));
        entityCountBox.setValue("1");
        entityCountBox.setMaxLength(3);
        this.addRenderableWidget(entityCountBox);

        this.addRenderableWidget(Button.builder(Component.literal("Summon"), b -> summonMonster())
                .bounds(left + 204, top + 222, 56, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Zb"), b -> { entityIdBox.setValue("minecraft:zombie"); summonMonster(); })
                .bounds(left + 262, top + 222, 22, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Sk"), b -> { entityIdBox.setValue("minecraft:skeleton"); summonMonster(); })
                .bounds(left + 286, top + 222, 22, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cr"), b -> { entityIdBox.setValue("minecraft:creeper"); summonMonster(); })
                .bounds(left + 310, top + 222, 22, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Wd"), b -> { entityIdBox.setValue("minecraft:warden"); summonMonster(); })
                .bounds(left + 334, top + 222, 22, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Wt"), b -> { entityIdBox.setValue("minecraft:wither"); summonMonster(); })
                .bounds(left + 358, top + 222, 22, 18).build());

        requestPlayers();
    }

    private void rebuildEffects() {
        effects.clear();
        String needle = effectFilter == null ? "" : effectFilter.toLowerCase(java.util.Locale.ROOT).trim();
        if (needle.isEmpty()) {
            effects.addAll(allEffects);
        } else {
            for (MobEffect e : allEffects) {
                if (effectName(e).toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                    effects.add(e);
                }
            }
        }
        if (selectedEffectIndex >= effects.size()) {
            selectedEffectIndex = effects.isEmpty() ? -1 : 0;
        }
        int max = Math.max(0, effects.size() - 3);
        if (effectScroll > max) effectScroll = max;
    }

    @Override
    public void tick() {
        super.tick();
        if (effectSearchBox != null) effectSearchBox.tick();
        durationBox.tick();
        amplifierBox.tick();
        itemIdBox.tick();
        itemCountBox.tick();
        entityIdBox.tick();
        entityCountBox.tick();

        refreshTicker++;
        if (refreshTicker >= 40) {
            refreshTicker = 0;
            requestPlayers();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void replacePlayers(List<AdminPlayerEntry> newPlayers) {
        UUID keep = selectedPlayer() != null ? selectedPlayer().uuid() : null;

        players.clear();
        players.addAll(newPlayers);

        if (players.isEmpty()) {
            selectedPlayerIndex = -1;
            playerScroll = 0;
            return;
        }

        if (keep != null) {
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).uuid().equals(keep)) {
                    selectedPlayerIndex = i;
                    return;
                }
            }
        }

        selectedPlayerIndex = Mth.clamp(selectedPlayerIndex, 0, players.size() - 1);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        // Frame
        guiGraphics.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xFF5555AA);
        guiGraphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xF0101020);

        // Title bar
        guiGraphics.fill(left, top, left + PANEL_W, top + 14, 0xFF2A2A55);
        guiGraphics.drawString(this.font, "ADMIN TOOL", left + 6, top + 3, 0xFFFFAA, false);

        // ===== Section A: Players + Target (y 18..128) =====
        // Players list
        guiGraphics.fill(left + 6, top + 18, left + 186, top + 30, 0xFF223366);
        guiGraphics.drawString(this.font, "PLAYERS", left + 10, top + 20, 0xAACCFF, false);
        guiGraphics.fill(left + 6, top + 30, left + 186, top + 128, 0x66000000);

        int visiblePlayers = 8;
        for (int i = 0; i < visiblePlayers; i++) {
            int index = playerScroll + i;
            if (index >= players.size()) break;

            int rowY = top + 33 + i * 12;
            AdminPlayerEntry entry = players.get(index);

            if (index == selectedPlayerIndex) {
                guiGraphics.fill(left + 7, rowY - 1, left + 185, rowY + 10, 0x66339933);
            }

            String text = cut(entry.name(), 22);
            guiGraphics.drawString(this.font, text, left + 9, rowY, 0xFFFFFF, false);
        }

        // Target details
        guiGraphics.fill(left + 190, top + 18, left + 394, top + 30, 0xFF334422);
        guiGraphics.drawString(this.font, "ALVO", left + 194, top + 20, 0xAAFFAA, false);
        guiGraphics.fill(left + 190, top + 30, left + 394, top + 128, 0x66000000);

        AdminPlayerEntry selected = selectedPlayer();
        if (selected != null) {
            guiGraphics.drawString(this.font, selected.name(), left + 194, top + 33, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "Pos " + (int) selected.x() + "," + (int) selected.y() + "," + (int) selected.z(), left + 194, top + 45, 0xCCCCCC, false);
            guiGraphics.drawString(this.font, "HP " + format(selected.health()) + "/" + format(selected.maxHealth()), left + 194, top + 57, 0xFF8888, false);
            guiGraphics.drawString(this.font, "Food " + selected.food() + "  Arm " + selected.armor(), left + 194, top + 69, 0xCCCCCC, false);
            guiGraphics.drawString(this.font, cut(selected.dimension(), 28), left + 194, top + 81, 0x88AAFF, false);
            guiGraphics.drawString(this.font, "M:" + cut(selected.mainHand(), 26), left + 194, top + 93, 0xCCCCCC, false);
            guiGraphics.drawString(this.font, "O:" + cut(selected.offHand(), 26), left + 194, top + 105, 0xCCCCCC, false);
        } else {
            guiGraphics.drawString(this.font, "(nenhum)", left + 194, top + 35, 0xFF7777, false);
        }

        // ===== Section B: Effects (y 132..200) =====
        // Header strip stops before the search box (which sits at x+220).
        guiGraphics.fill(left + 6, top + 132, left + 218, top + 144, 0xFF553388);
        guiGraphics.drawString(this.font, "POÇÕES / EFEITOS", left + 10, top + 134, 0xFFAAFF, false);
        guiGraphics.fill(left + 6, top + 144, left + 394, top + 180, 0x66000000);

        int visibleEffects = 3;
        for (int i = 0; i < visibleEffects; i++) {
            int index = effectScroll + i;
            if (index >= effects.size()) break;

            MobEffect effect = effects.get(index);
            int rowY = top + 147 + i * 11;

            if (index == selectedEffectIndex) {
                guiGraphics.fill(left + 7, rowY - 1, left + 393, rowY + 10, 0x665555AA);
            }

            guiGraphics.drawString(this.font, cut(effectName(effect), 60), left + 9, rowY, 0xFFFFFF, false);
        }

        guiGraphics.drawString(this.font, "S", left + 12, top + 187, 0xA0A0A0, false);
        guiGraphics.drawString(this.font, "A", left + 68, top + 187, 0xA0A0A0, false);

        // ===== Section C header strips =====
        guiGraphics.fill(left + 6, top + 202, left + 200, top + 204, 0xFF888888);
        guiGraphics.fill(left + 6, top + 220, left + 200, top + 222, 0xFF883322);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        if (inside(mouseX, mouseY, left + 6, top + 30, 180, 98)) {
            int relY = (int) mouseY - (top + 33);
            int row = relY / 12;
            int idx = playerScroll + row;

            if (row >= 0 && idx >= 0 && idx < players.size()) {
                selectedPlayerIndex = idx;
                return true;
            }
        }

        if (inside(mouseX, mouseY, left + 6, top + 144, 388, 36)) {
            int relY = (int) mouseY - (top + 147);
            int row = relY / 11;
            int idx = effectScroll + row;

            if (row >= 0 && idx >= 0 && idx < effects.size()) {
                selectedEffectIndex = idx;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        if (inside(mouseX, mouseY, left + 6, top + 30, 180, 98)) {
            int max = Math.max(0, players.size() - 8);
            playerScroll = Mth.clamp(playerScroll - (int) Math.signum(delta), 0, max);
            return true;
        }

        if (inside(mouseX, mouseY, left + 6, top + 144, 388, 36)) {
            int max = Math.max(0, effects.size() - 3);
            effectScroll = Mth.clamp(effectScroll - (int) Math.signum(delta), 0, max);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void requestPlayers() {
        ModNetwork.CHANNEL.sendToServer(AdminActionC2SPacket.requestPlayers());
    }

    private void requestInventory() {
        AdminPlayerEntry target = selectedPlayer();
        if (target == null) return;
        ModNetwork.CHANNEL.sendToServer(AdminActionC2SPacket.requestInventory(target.uuid()));
    }

    private void sendPosition() {
        AdminPlayerEntry target = selectedPlayer();
        if (target == null) return;
        ModNetwork.CHANNEL.sendToServer(AdminActionC2SPacket.sendPosition(target.uuid()));
    }

    private void scareSingle() {
        AdminPlayerEntry target = selectedPlayer();
        if (target == null) return;
        ModNetwork.CHANNEL.sendToServer(AdminActionC2SPacket.scareSingle(target.uuid()));
    }

    private void scareArea() {
        AdminPlayerEntry target = selectedPlayer();
        if (target == null) return;
        ModNetwork.CHANNEL.sendToServer(AdminActionC2SPacket.scareArea(target.uuid()));
    }

    private void applyEffect() {
        AdminPlayerEntry target = selectedPlayer();
        ResourceLocation effectId = selectedEffectId();
        if (target == null || effectId == null) return;
        int seconds = parseInt(durationBox.getValue(), 10, 1, 3600);
        int amplifier = parseInt(amplifierBox.getValue(), 0, 0, 255);
        ModNetwork.CHANNEL.sendToServer(
                AdminActionC2SPacket.applyEffect(target.uuid(), effectId.toString(), seconds, amplifier)
        );
    }

    private void clearEffect() {
        AdminPlayerEntry target = selectedPlayer();
        ResourceLocation effectId = selectedEffectId();
        if (target == null || effectId == null) return;
        ModNetwork.CHANNEL.sendToServer(
                AdminActionC2SPacket.clearEffect(target.uuid(), effectId.toString())
        );
    }

    private void giveItem() {
        AdminPlayerEntry target = selectedPlayer();
        if (target == null) return;
        int count = parseInt(itemCountBox.getValue(), 1, 1, 4096);
        ModNetwork.CHANNEL.sendToServer(
                AdminActionC2SPacket.giveItem(target.uuid(), itemIdBox.getValue().trim(), count)
        );
    }

    private void summonMonster() {
        AdminPlayerEntry target = selectedPlayer();
        if (target == null) return;
        int count = parseInt(entityCountBox.getValue(), 1, 1, 32);
        ModNetwork.CHANNEL.sendToServer(
                AdminActionC2SPacket.summonMonster(target.uuid(), entityIdBox.getValue().trim(), count)
        );
    }

    private void placeBlock() {
        AdminPlayerEntry target = selectedPlayer();
        if (target == null) return;
        // Use itemCountBox as radius: 0 = single block at feet, 1..8 = cube around
        int radius = parseInt(itemCountBox.getValue(), 0, 0, 8);
        ModNetwork.CHANNEL.sendToServer(
                AdminActionC2SPacket.placeBlock(target.uuid(), itemIdBox.getValue().trim(), radius)
        );
    }

    private void removeItem() {
        AdminPlayerEntry target = selectedPlayer();
        if (target == null) return;
        int count = parseInt(itemCountBox.getValue(), 1, 1, 4096);
        ModNetwork.CHANNEL.sendToServer(
                AdminActionC2SPacket.removeItem(target.uuid(), itemIdBox.getValue().trim(), count)
        );
    }

    private AdminPlayerEntry selectedPlayer() {
        if (selectedPlayerIndex < 0 || selectedPlayerIndex >= players.size()) return null;
        return players.get(selectedPlayerIndex);
    }

    private ResourceLocation selectedEffectId() {
        if (selectedEffectIndex < 0 || selectedEffectIndex >= effects.size()) return null;
        return ForgeRegistries.MOB_EFFECTS.getKey(effects.get(selectedEffectIndex));
    }

    private String effectName(MobEffect effect) {
        ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        String id = key == null ? "unknown" : key.toString();
        return Component.translatable(effect.getDescriptionId()).getString() + " (" + id + ")";
    }

    private int parseInt(String text, int fallback, int min, int max) {
        try {
            return Mth.clamp(Integer.parseInt(text.trim()), min, max);
        } catch (Exception e) {
            return fallback;
        }

    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private String cut(String text, int max) {
        if (text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }

    private String format(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
