package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum SealTier {

    BONE(0, "Selo de Ossos", ChatFormatting.GRAY, 24000),
    GOLD(1, "Selo de Ouro", ChatFormatting.GOLD, 36000),
    DIAMOND(2, "Selo de Diamante", ChatFormatting.AQUA, 72000),
    NETHERITE(3, "Selo de Netherite", ChatFormatting.DARK_PURPLE, 120000);

    private final int powerLevel;
    private final String displayName;
    private final ChatFormatting color;
    private final int durability;

    SealTier(int powerLevel, String displayName, ChatFormatting color, int durability) {
        this.powerLevel = powerLevel;
        this.displayName = displayName;
        this.color = color;
        this.durability = durability;
    }

    public int powerLevel() {
        return powerLevel;
    }

    public Component displayName() {
        return Component.literal(displayName).withStyle(color);
    }

    public ChatFormatting color() {
        return color;
    }

    public int durability() {
        return durability;
    }

    public boolean canCaptureRequiredLevel(int requiredLevel) {
        return this.powerLevel >= requiredLevel;
    }
}