package br.com.murilo.liberthia.magic.mageclass;

import net.minecraft.world.entity.player.Player;

/**
 * r162: Persiste classe + level do player.
 *
 * <p>Storage: PersistentData NBT.
 */
public final class MageClassData {

    public static final String NBT_CLASS = "liberthia.mage_class";
    public static final String NBT_CLASS_LEVEL = "liberthia.mage_class_level";
    public static final String NBT_CLASS_XP = "liberthia.mage_class_xp";

    private MageClassData() {}

    public static MageClass get(Player p) {
        String name = p.getPersistentData().getString(NBT_CLASS);
        if (name.isEmpty()) return null;
        try { return MageClass.valueOf(name); } catch (Exception e) { return null; }
    }

    public static void set(Player p, MageClass cls) {
        if (cls == null) {
            p.getPersistentData().remove(NBT_CLASS);
        } else {
            p.getPersistentData().putString(NBT_CLASS, cls.name());
            // Setar level 1 se for primeira vez nessa classe
            if (p.getPersistentData().getInt(NBT_CLASS_LEVEL) <= 0) {
                p.getPersistentData().putInt(NBT_CLASS_LEVEL, 1);
            }
        }
    }

    public static int getLevel(Player p) {
        int lv = p.getPersistentData().getInt(NBT_CLASS_LEVEL);
        return Math.max(1, Math.min(10, lv));
    }

    public static void setLevel(Player p, int level) {
        p.getPersistentData().putInt(NBT_CLASS_LEVEL,
                Math.max(1, Math.min(10, level)));
    }

    public static int getXp(Player p) {
        return p.getPersistentData().getInt(NBT_CLASS_XP);
    }

    public static void addXp(Player p, int delta) {
        int xp = getXp(p) + delta;
        int level = getLevel(p);
        // XP needed for next: level^2 * 100
        while (level < 10 && xp >= (long)(level * level * 100)) {
            xp -= level * level * 100;
            level++;
            // Level-up message
            if (p.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                MageClass cls = get(p);
                String clsName = cls == null ? "Mago" : cls.colorCode + cls.displayName + "§r";
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§l✦ " + clsName + " §rsubiu pra §enível " + level + "§r/10!"));
                sl.playSound(null, p.blockPosition(),
                        net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1F, 1.3F);
            }
        }
        p.getPersistentData().putInt(NBT_CLASS_XP, xp);
        p.getPersistentData().putInt(NBT_CLASS_LEVEL, level);
    }
}
