package br.com.murilo.liberthia.magic.spell.vfx;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.resources.ResourceLocation;

/**
 * r166: <b>SpriteVfxRegistry</b> — catalog of the 20 sprite-sheet effects sliced
 * from the Davit Masia "Pixel FX Designer" pack.
 *
 * <p>Each {@link Type} bundles:
 * <ul>
 *   <li><b>sheetName</b> — folder under {@code textures/vfx/}</li>
 *   <li><b>frameCount</b> — number of {@code frame_NNN.png} files (after empty-cell skip)</li>
 *   <li><b>lifetimeTicks</b> — total ticks the entity should live</li>
 *   <li><b>scale</b> — render half-size (in blocks)</li>
 *   <li><b>orientation</b> — billboard, ground-flat, or motion-axis</li>
 *   <li><b>loop</b> — if true, frames cycle continuously; false = play once over lifetime</li>
 * </ul>
 *
 * <p>Frame paths resolved as
 * {@code liberthia:textures/vfx/<sheet>/frame_NNN.png}.
 *
 * <p>To spawn a VFX in-world (server-side):
 * <pre>
 * SpriteVfxRegistry.spawn(serverLevel, pos, caster, SpriteVfxRegistry.Type.FIRE);
 * </pre>
 */
public final class SpriteVfxRegistry {

    public enum Orientation { BILLBOARD, GROUND_FLAT, MOTION_AXIS }

    public static final class Effect {
        public final String sheetName;
        public final int frameCount;
        public final int lifetimeTicks;
        public final float scale;
        public final Orientation orientation;
        public final boolean loop;

        public Effect(String sheetName, int frameCount, int lifetimeTicks,
                       float scale, Orientation orientation, boolean loop) {
            this.sheetName = sheetName;
            this.frameCount = frameCount;
            this.lifetimeTicks = lifetimeTicks;
            this.scale = scale;
            this.orientation = orientation;
            this.loop = loop;
        }

        /** Returns the ResourceLocation for a specific frame (0-indexed). */
        public ResourceLocation frame(int idx) {
            if (idx < 0) idx = 0;
            if (idx >= frameCount) idx = frameCount - 1;
            return new ResourceLocation(LiberthiaMod.MODID,
                    String.format("textures/vfx/%s/frame_%03d.png", sheetName, idx));
        }

        /** Frame indexer based on age + total life. */
        public int frameAt(int age) {
            if (loop) return (age / 2) % frameCount;
            // Play once over lifetime
            int idx = (age * frameCount) / Math.max(1, lifetimeTicks);
            return Math.min(frameCount - 1, idx);
        }
    }

    /**
     * Catalog of every effect. Type ordinal → Effect.
     * <b>WARNING:</b> the ordinal is serialized in the entity sync data; never
     * reorder existing entries, only append new ones.
     */
    public enum Type {
        // ─── Magic schools (purple/cyan/general) ────────────────────────────
        /** Generic magic spell burst — purple cyan starburst, 74 frames */
        MAGIC_SPELL    (new Effect("magicspell",       74, 40, 1.2F, Orientation.BILLBOARD, false)),
        /** Infinity "8" loop, pink/purple — sustained channel */
        MAGIC_LOOP     (new Effect("magic8",           61, 40, 0.9F, Orientation.BILLBOARD, true)),
        /** Cyan magicka hit impact, small */
        MAGICKA_HIT    (new Effect("magickahit",       39, 20, 0.8F, Orientation.BILLBOARD, false)),
        /** Big casting circle on the ground — windup VFX */
        CASTING        (new Effect("casting",          71, 50, 1.6F, Orientation.GROUND_FLAT, false)),
        /** Protection / ward circle, sustained, lays on floor */
        PROTECTION     (new Effect("protectioncircle", 61, 100, 2.0F, Orientation.GROUND_FLAT, true)),
        /** Loading / channel ring (slow spin) */
        CHANNEL_RING   (new Effect("loading",         121, 80, 1.0F, Orientation.BILLBOARD, true)),

        // ─── Fire family ────────────────────────────────────────────────────
        /** Bright orange fire burst */
        FIRE           (new Effect("fire",             61, 36, 1.1F, Orientation.BILLBOARD, false)),
        /** Brighter, white-hot fire variant */
        BRIGHT_FIRE    (new Effect("brightfire",       61, 36, 1.1F, Orientation.BILLBOARD, false)),
        /** Blue arcane fire */
        BLUE_FIRE      (new Effect("bluefire",         61, 36, 1.1F, Orientation.BILLBOARD, false)),
        /** Sun-burn solar flare */
        SUNBURN        (new Effect("sunburn",          61, 40, 1.4F, Orientation.BILLBOARD, false)),
        /** Whip-like flame lash, ground-oriented */
        FLAMELASH      (new Effect("flamelash",        44, 30, 1.5F, Orientation.MOTION_AXIS, false)),
        /** Spinning fire vortex */
        FIRE_SPIN      (new Effect("firespin",         61, 50, 1.3F, Orientation.BILLBOARD, true)),

        // ─── Eldritch / void / dark ─────────────────────────────────────────
        /** Cosmic vortex, eye-themed */
        VORTEX         (new Effect("vortex",           61, 80, 1.5F, Orientation.BILLBOARD, true)),
        /** Nebula cloud — slow drift */
        NEBULA         (new Effect("nebula",           61, 80, 2.0F, Orientation.BILLBOARD, true)),
        /** Phantom wisp — translucent, ghostly */
        PHANTOM        (new Effect("phantom",          61, 50, 1.0F, Orientation.BILLBOARD, false)),
        /** Midnight magic — dark purple */
        MIDNIGHT       (new Effect("midnight",         61, 50, 1.2F, Orientation.BILLBOARD, false)),
        /** Fel / demonic green magic */
        FELSPELL       (new Effect("felspell",         91, 60, 1.3F, Orientation.BILLBOARD, false)),

        // ─── Ice / freeze ───────────────────────────────────────────────────
        /** Freezing crystallize burst */
        FREEZING       (new Effect("freezing",         86, 50, 1.2F, Orientation.BILLBOARD, false)),

        // ─── Misc utility ───────────────────────────────────────────────────
        /** Weapon hit / slash impact */
        WEAPON_HIT     (new Effect("weaponhit",        30, 18, 0.9F, Orientation.BILLBOARD, false)),
        /** Magical bubbles — buff/heal feedback */
        MAGIC_BUBBLES  (new Effect("magicbubbles",     61, 50, 1.0F, Orientation.BILLBOARD, false));

        public final Effect effect;
        Type(Effect e) { this.effect = e; }

        public static Type fromOrdinalSafe(int ord) {
            Type[] vals = values();
            if (ord < 0 || ord >= vals.length) return MAGIC_SPELL;
            return vals[ord];
        }
    }

    private SpriteVfxRegistry() {}

    /**
     * Helper — spawn a {@link SpriteVfxEntity} at the given position.
     * The entity will be added to the world and live for {@code lifetimeTicks}.
     */
    public static SpriteVfxEntity spawn(net.minecraft.server.level.ServerLevel level,
                                          net.minecraft.world.phys.Vec3 pos,
                                          net.minecraft.world.entity.LivingEntity owner,
                                          Type type) {
        SpriteVfxEntity ent = new SpriteVfxEntity(level, owner, pos, type);
        level.addFreshEntity(ent);
        return ent;
    }

    /** Overload — spawn with custom scale multiplier (e.g. boss-sized effects). */
    public static SpriteVfxEntity spawn(net.minecraft.server.level.ServerLevel level,
                                          net.minecraft.world.phys.Vec3 pos,
                                          net.minecraft.world.entity.LivingEntity owner,
                                          Type type, float scaleMul) {
        SpriteVfxEntity ent = new SpriteVfxEntity(level, owner, pos, type, scaleMul);
        level.addFreshEntity(ent);
        return ent;
    }
}
