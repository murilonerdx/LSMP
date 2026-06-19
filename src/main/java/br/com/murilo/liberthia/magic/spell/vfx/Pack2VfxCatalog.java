package br.com.murilo.liberthia.magic.spell.vfx;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.resources.ResourceLocation;

/**
 * r167: Catalog of the <b>pack-2</b> sprite assets (15 Parts × 12 colors each =
 * 16326 frames). Sliced from {@code assets/liberthia/pack-sprites/Part N/*.png}
 * by {@code slice_pack2_sprites.py}, output at
 * {@code textures/vfx_pack2/partNN/cKK/frame_NNN.png}.
 *
 * <p>Each part has the same animation in 12 color variants (c00..c11), where
 * each color was the corresponding .png file in the source folder sorted
 * alphabetically. Frame index is independent of color — i.e.
 * {@code partXX/c00/frame_050.png} and {@code partXX/c07/frame_050.png}
 * are the same animation frame in different colors.
 *
 * <p>The {@link Type} enum names each Part by its visual character (based on
 * inspecting the preview GIF middle frame).
 *
 * <h2>Usage</h2>
 * <pre>
 * // server-side
 * Pack2VfxCatalog.spawn(serverLevel, pos, caster,
 *     Pack2VfxCatalog.Type.EXPLOSION, 5);  // color #5
 * </pre>
 */
public final class Pack2VfxCatalog {

    /** Number of color variants per Part (always 12 in this pack). */
    public static final int COLORS = 12;

    /**
     * <b>r170 FIX FINAL</b>: Sprites foram re-geradas via HSV hue rotation
     * sobre UMA base textura por Part. Cada cKK folder agora tem TODOS os
     * frames numa cor consistente, com mapeamento garantido:
     *
     * <ul>
     *   <li>c00 = red    | c06 = purple</li>
     *   <li>c01 = orange | c07 = magenta</li>
     *   <li>c02 = yellow | c08 = white</li>
     *   <li>c03 = green  | c09 = brown</li>
     *   <li>c04 = cyan   | c10 = pink</li>
     *   <li>c05 = blue   | c11 = teal</li>
     * </ul>
     *
     * <p>{@code frameCount} é agora o número exato de frames row-0 da textura
     * base (sliced via {@code colorize_pack2.py}).
     */
    public enum Type {
        /** Part 01 — Swirling slashes & spiral claws. */
        SWIRL_SLASH       ("part01", 11, 40, 1.5F, false),
        /** Part 02 — Fire eruption / explosive bloom. */
        ERUPTION          ("part02",  9, 25, 1.4F, false),
        /** Part 03 — Crescent moon arcs / curved sweeps. */
        MOON_ARC          ("part03", 12, 40, 1.6F, false),
        /** Part 04 — Snowflake / geometric mandalas. */
        MANDALA           ("part04", 14, 40, 1.8F, false),
        /** Part 05 — X-strikes & cross slashes. Sharp impact. */
        CROSS_STRIKE      ("part05",  9, 20, 1.2F, false),
        /** Part 06 — Stars, sparks, small bursts. */
        STAR_BURST        ("part06",  9, 25, 1.0F, false),
        /** Part 07 — Curved tendrils & scattered particles. */
        TENDRIL_SCATTER   ("part07",  7, 20, 1.3F, false),
        /** Part 08 — Slash trails & directional sharp strikes. */
        SLASH_TRAIL       ("part08",  8, 25, 1.5F, false),
        /** Part 09 — Cloud puffs, rings & sparkle showers. */
        SPARKLE_RING      ("part09",  8, 30, 1.4F, false),
        /** Part 10 — Smoke puffs & projectile trails. */
        SMOKE_TRAIL       ("part10", 11, 40, 1.3F, false),
        /** Part 11 — Big smoke clouds & fire waves. */
        SMOKE_CLOUD       ("part11", 11, 40, 1.8F, false),
        /** Part 12 — Sun/flare rings & comet streaks. */
        SOLAR_FLARE       ("part12", 13, 40, 1.7F, false),
        /** Part 13 — Skulls, eyes, faces. Horror/cosmic. */
        HORROR_SIGIL      ("part13", 12, 45, 1.6F, false),
        /** Part 14 — Fireworks & splash drops. */
        FIREWORK          ("part14", 15, 45, 1.5F, false),
        /** Part 15 — Magic seals, hexagons & runic symbols. */
        RUNIC_SEAL        ("part15", 21, 60, 2.0F, true);

        public final String partSlug;
        public final int frameCount;
        public final int lifetimeTicks;
        public final float scale;
        public final boolean loop;

        Type(String partSlug, int frameCount, int lifetimeTicks, float scale, boolean loop) {
            this.partSlug = partSlug;
            this.frameCount = frameCount;
            this.lifetimeTicks = lifetimeTicks;
            this.scale = scale;
            this.loop = loop;
        }

        /** Returns the ResourceLocation for a specific frame + color (0..11). */
        public ResourceLocation frame(int frameIdx, int colorIdx) {
            if (frameIdx < 0) frameIdx = 0;
            if (frameIdx >= frameCount) frameIdx = frameCount - 1;
            int color = ((colorIdx % COLORS) + COLORS) % COLORS;
            return new ResourceLocation(LiberthiaMod.MODID,
                    String.format("textures/vfx_pack2/%s/c%02d/frame_%03d.png",
                            partSlug, color, frameIdx));
        }

        public int frameAt(int age) {
            if (loop) return (age / 2) % frameCount;
            int idx = (age * frameCount) / Math.max(1, lifetimeTicks);
            return Math.min(frameCount - 1, idx);
        }

        public static Type fromOrdinalSafe(int ord) {
            Type[] vals = values();
            if (ord < 0 || ord >= vals.length) return SWIRL_SLASH;
            return vals[ord];
        }
    }

    private Pack2VfxCatalog() {}

    // ── Public spawn API ─────────────────────────────────────────────────────

    /** Spawn a pack-2 VFX entity at the given position with color index 0. */
    public static SpriteVfxEntity spawn(net.minecraft.server.level.ServerLevel level,
                                          net.minecraft.world.phys.Vec3 pos,
                                          net.minecraft.world.entity.LivingEntity owner,
                                          Type type) {
        return spawn(level, pos, owner, type, 0, 1.0F);
    }

    /** Spawn with explicit color index (0..11). */
    public static SpriteVfxEntity spawn(net.minecraft.server.level.ServerLevel level,
                                          net.minecraft.world.phys.Vec3 pos,
                                          net.minecraft.world.entity.LivingEntity owner,
                                          Type type, int colorIdx) {
        return spawn(level, pos, owner, type, colorIdx, 1.0F);
    }

    /** Spawn with color + scale multiplier. */
    public static SpriteVfxEntity spawn(net.minecraft.server.level.ServerLevel level,
                                          net.minecraft.world.phys.Vec3 pos,
                                          net.minecraft.world.entity.LivingEntity owner,
                                          Type type, int colorIdx, float scaleMul) {
        SpriteVfxEntity ent = new SpriteVfxEntity(level, owner, pos, type, colorIdx, scaleMul);
        level.addFreshEntity(ent);
        return ent;
    }

    /**
     * r170: Mapeamento DEFINITIVO — cores agora são consistentes (geradas por
     * HSV rotation no script {@code colorize_pack2.py}).
     *
     * <ul>
     *   <li>c00=red, c01=orange, c02=yellow, c03=green</li>
     *   <li>c04=cyan, c05=blue, c06=purple, c07=magenta</li>
     *   <li>c08=white, c09=brown, c10=pink, c11=teal</li>
     * </ul>
     */
    public static int colorForSchool(br.com.murilo.liberthia.magic.school.SpellSchool school) {
        return switch (school) {
            case FIRE      -> 1;  // orange
            case ICE       -> 4;  // cyan
            case LIGHTNING -> 2;  // yellow
            case BLOOD     -> 0;  // red
            case ELDRITCH  -> 6;  // purple
            case HOLY      -> 8;  // white
            case NATURE    -> 3;  // green
        };
    }
}
