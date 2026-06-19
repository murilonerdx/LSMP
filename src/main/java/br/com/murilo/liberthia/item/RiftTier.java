package br.com.murilo.liberthia.item;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * r185 — níveis da Adaga Corta-Fendas. Cada nível libera mais dimensões:
 * <ul>
 *   <li>{@link #OVERWORLD_ONLY} — só o Overworld.</li>
 *   <li>{@link #OVERWORLD_NETHER} — Overworld + Nether.</li>
 *   <li>{@link #ALL_DIMENSIONS} — TODAS as dimensões (craft mais difícil do jogo).</li>
 * </ul>
 */
public enum RiftTier {
    OVERWORLD_ONLY(7, -2.2F, 0x88CCFF),
    OVERWORLD_NETHER(8, -2.1F, 0xFF6633),
    ALL_DIMENSIONS(10, -2.0F, 0xAA00FF);

    public final int attackDamage;
    public final float attackSpeed;
    public final int color;

    RiftTier(int attackDamage, float attackSpeed, int color) {
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.color = color;
    }

    public boolean canTeleportTo(ResourceKey<Level> dim) {
        return switch (this) {
            case OVERWORLD_ONLY -> dim.equals(Level.OVERWORLD);
            case OVERWORLD_NETHER -> dim.equals(Level.OVERWORLD) || dim.equals(Level.NETHER);
            case ALL_DIMENSIONS -> true;
        };
    }
}
