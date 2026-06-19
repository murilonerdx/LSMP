package br.com.murilo.liberthia.magic.antimagic;

import br.com.murilo.liberthia.compat.mna.AntiMagicWardHandler;
import br.com.murilo.liberthia.magic.school.SchoolDamageRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;

/**
 * r182 — núcleo do sistema TECNO-ARCANO (anti-magia tech). Ponto central de:
 * <ul>
 *   <li><b>Supressão de mana por zona</b> — o {@code ManaSuppressorBlock} marca players
 *       no raio; {@code PlayerSpellKnowledge.consumeMana} e {@code SourceData.consume}
 *       checam {@link #isSuppressed} e FALHAM o cast.</li>
 *   <li><b>Marcação de conjuração</b> — todo cast bem-sucedido chama {@link #markCast};
 *       o {@code ArcaneSentinelBlock} pune quem tem a marca (dano + lentidão + cegueira).</li>
 *   <li><b>Detecção unificada de dano mágico</b> ({@link #isMagicDamage}) p/ armadura/escudo.</li>
 * </ul>
 * Marcadores ficam na persistentData do player (contagem decrescente, decaída no tick).
 */
public final class AntiMagic {
    private AntiMagic() {}

    public static final String NBT_SUPPRESS = "liberthia.manaSuppressed"; // int countdown
    public static final String NBT_CASTFLASH = "liberthia.castFlash";     // int countdown
    private static final String NBT_SUPPRESS_MSG = "liberthia.suppressMsgCd";

    // ── supressão ──
    public static boolean isSuppressed(Player p) {
        return p != null && p.getPersistentData().getInt(NBT_SUPPRESS) > 0;
    }
    /** Chamado pelo selo supressor a cada tick p/ players no raio. */
    public static void suppress(Player p, int ticks) {
        p.getPersistentData().putInt(NBT_SUPPRESS, Math.max(p.getPersistentData().getInt(NBT_SUPPRESS), ticks));
    }
    /** Aviso na action bar (throttle p/ não floodar). */
    public static void notifySuppressed(Player p) {
        var d = p.getPersistentData();
        if (d.getInt(NBT_SUPPRESS_MSG) > 0) return;
        d.putInt(NBT_SUPPRESS_MSG, 30);
        p.displayClientMessage(Component.translatable("msg.liberthia.mana_suppressed").withStyle(ChatFormatting.AQUA), true);
    }

    // ── marcação de conjuração ──
    public static void markCast(Player p) {
        if (p != null) p.getPersistentData().putInt(NBT_CASTFLASH, 8);
    }
    public static boolean castFlash(Player p) {
        return p != null && p.getPersistentData().getInt(NBT_CASTFLASH) > 0;
    }
    public static void clearCast(Player p) {
        if (p != null) p.getPersistentData().putInt(NBT_CASTFLASH, 0);
    }

    /** Decaimento dos marcadores — chamado no PlayerTickEvent. */
    public static void tickDecay(Player p) {
        var d = p.getPersistentData();
        dec(d, NBT_SUPPRESS);
        dec(d, NBT_CASTFLASH);
        dec(d, NBT_SUPPRESS_MSG);
    }
    private static void dec(net.minecraft.nbt.CompoundTag d, String k) {
        int v = d.getInt(k);
        if (v > 0) d.putInt(k, v - 1);
    }

    // ── detecção unificada de dano mágico (todas as fontes do mod + vanilla + M&A) ──
    public static boolean isMagicDamage(DamageSource src) {
        if (src == null) return false;
        return src.is(DamageTypes.MAGIC)
                || src.is(DamageTypes.INDIRECT_MAGIC)
                || src.is(DamageTypeTags.WITCH_RESISTANT_TO)
                || SchoolDamageRegistry.isSchoolDamage(src)
                || AntiMagicWardHandler.isMnaSource(src);
    }
}
