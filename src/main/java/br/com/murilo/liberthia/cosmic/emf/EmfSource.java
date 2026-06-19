package br.com.murilo.liberthia.cosmic.emf;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * r174: Lógica central de "irregularidade dimensional" — calcula a intensidade
 * (0..1) de irregularidade num ponto do mundo, lida pelos {@link EmfMeterItem}.
 *
 * <h2>Fontes de irregularidade</h2>
 * <ul>
 *   <li><b>Player com {@link DimensionalIrregularityItem} no inventário</b> —
 *       fonte forte (admin ambulante). Alcance 44 blocos.</li>
 *   <li><b>Player marcado por comando</b> ({@code /liberthia irregularity}) — flag
 *       persistente. Mesma força.</li>
 *   <li><b>Entidades cósmicas/espirituais</b> próximas — fonte fraca (ambiente).
 *       Alcance 20 blocos, ×0.55.</li>
 * </ul>
 */
public final class EmfSource {

    private EmfSource() {}

    /** Flag persistente: este player É uma irregularidade (marcado por comando). */
    public static final String NBT_FLAG = "liberthia.emf.irregular";

    private static final double PLAYER_RANGE = 44.0;
    private static final double MOB_RANGE = 24.0; // "a região" ao redor do portador

    private static final String[] COSMIC_KEYS = {
            "spirit", "whisper", "soul", "ghost", "shade", "wraith", "pale",
            "loom", "watcher", "peripheral", "observer", "lurker", "phantom",
            "void", "cosmic", "dark_consciousness", "eye_of_horus", "clone",
            "cacador", "horror", "absence", "empty_man", "remembered", "visitante",
            "mulher", "wooden_horror", "espreitador", "screamer", "worm", "window", "idol"
    };

    /** Caçadores ativos (te perseguem) fazem a agulha disparar mais que ambiente. */
    private static final String[] HUNTER_KEYS = {
            "observer", "watcher", "peripheral", "lurker", "cacador",
            "stalker", "window", "screamer", "espreitador", "idol"
    };

    public static void setFlag(Player p, boolean on) {
        if (on) p.getPersistentData().putBoolean(NBT_FLAG, true);
        else p.getPersistentData().remove(NBT_FLAG);
    }

    public static boolean hasFlag(Player p) {
        return p.getPersistentData().getBoolean(NBT_FLAG);
    }

    /** True se o player carrega a Irregularidade Dimensional no inventário. */
    public static boolean carriesIrregularity(Player p) {
        for (var stack : p.getInventory().items) {
            if (stack.getItem() instanceof DimensionalIrregularityItem) return true;
        }
        if (p.getOffhandItem().getItem() instanceof DimensionalIrregularityItem) return true;
        return false;
    }

    public static boolean isSourcePlayer(Player p) {
        return hasFlag(p) || carriesIrregularity(p);
    }

    /**
     * Intensidade de irregularidade (0..1) no ponto {@code pos}.
     * @param exclude player a ignorar (o próprio dono do EMF Meter).
     */
    public static double intensityAt(Level level, Vec3 pos, Player exclude) {
        double best = 0.0;

        AABB pbox = new AABB(pos.x - PLAYER_RANGE, pos.y - PLAYER_RANGE, pos.z - PLAYER_RANGE,
                pos.x + PLAYER_RANGE, pos.y + PLAYER_RANGE, pos.z + PLAYER_RANGE);
        for (Player p : level.getEntitiesOfClass(Player.class, pbox)) {
            if (p == exclude) continue;
            if (!isSourcePlayer(p)) continue;
            double d = Math.sqrt(p.distanceToSqr(pos));
            double s = clamp01(1.0 - d / PLAYER_RANGE);
            if (s > best) best = s;
        }

        // Mobs cósmicos: a agulha responde ao NÍVEL de horror da região — não só ao
        // mais próximo, mas à QUANTIDADE. O mais perto domina; cada horror extra
        // empurra a leitura pro vermelho. Caçadores (que te perseguem) leem mais forte.
        AABB mbox = new AABB(pos.x - MOB_RANGE, pos.y - MOB_RANGE, pos.z - MOB_RANGE,
                pos.x + MOB_RANGE, pos.y + MOB_RANGE, pos.z + MOB_RANGE);
        double mobBest = 0.0, mobSum = 0.0;
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, mbox)) {
            if (le instanceof Player) continue;
            double w = cosmicWeight(le);
            if (w <= 0.0) continue;
            double d = Math.sqrt(le.distanceToSqr(pos));
            double s = clamp01(1.0 - d / MOB_RANGE) * w;
            if (s > mobBest) mobBest = s;
            mobSum += s;
        }
        // leitura = horror mais próximo + bônus de "enxame" (cada extra soma até +0.18)
        double mobIntensity = clamp01(mobBest + Math.min(0.5, (mobSum - mobBest) * 0.18));
        if (mobIntensity > best) best = mobIntensity;
        return best;
    }

    /**
     * Peso de irregularidade de um ser (0 = não-cósmico). Caçadores ativos leem
     * mais quente (0.9) que horrores ambientes (0.6) — a agulha "sente" predador.
     */
    private static double cosmicWeight(LivingEntity le) {
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(le.getType());
        if (rl == null) return 0.0;
        String path = rl.getPath();
        boolean cosmic = false;
        for (String k : COSMIC_KEYS) if (path.contains(k)) { cosmic = true; break; }
        if (!cosmic) return 0.0;
        for (String k : HUNTER_KEYS) if (path.contains(k)) return 0.9;
        return 0.6;
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }
}
