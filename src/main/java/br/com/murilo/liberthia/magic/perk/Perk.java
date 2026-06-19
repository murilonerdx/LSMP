package br.com.murilo.liberthia.magic.perk;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * v0.1.24 r90: <b>Perk</b> — passive bonus que pode ser slotado em armor.
 *
 * <p>Cada perk tem:
 * <ul>
 *   <li>{@code id} — string única (ex.: "jump", "magic_resist")</li>
 *   <li>{@code displayName} — texto exibido</li>
 *   <li>{@code description} — tooltip</li>
 *   <li>{@code maxStack} — quantos slots iguais podem ser combinados (1-3)</li>
 *   <li>{@link #onTick} — chamado a cada 20t pelo player</li>
 *   <li>{@link #onApply} — chamado quando perk é equipado (1x)</li>
 *   <li>{@link #onRemove} — chamado quando perk é desequipado (1x)</li>
 * </ul>
 */
public abstract class Perk {

    private final String id;
    private final String displayName;
    private final String description;
    private final int maxStack;

    protected Perk(String id, String displayName, String description, int maxStack) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.maxStack = maxStack;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getMaxStack() { return maxStack; }

    public Component getLabel() {
        return Component.literal("§b" + displayName);
    }

    public void onTick(Player player, int stackLevel) {}
    public void onApply(Player player, int stackLevel) {}
    public void onRemove(Player player) {}
}
