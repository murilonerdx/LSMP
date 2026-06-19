package br.com.murilo.liberthia.magic.factory;

import com.google.gson.JsonObject;

/**
 * r148: Perfil de VFX de um feitiço criado via factory.
 *
 * <p>Tudo que afeta APENAS o visual e som — sem mexer no comportamento.
 * Pode ser parseado de JSON ou montado via builder.
 *
 * <h2>Camadas</h2>
 * <ul>
 *   <li><b>Cores:</b> primary (núcleo), secondary (halo) — overrides school default</li>
 *   <li><b>Sprite:</b> "auto" (usa school sheet), "orb", "star", "hex", "arc", "shard" (factory sprites)</li>
 *   <li><b>Trail:</b> density (partículas/tick), size (escala), lifetime (ticks)</li>
 *   <li><b>Charge aura:</b> intensity (0..1) controla densidade na mão durante channel</li>
 *   <li><b>Impact:</b> scale (1.0 = normal, 2.0 = big, 0.5 = small), particles count</li>
 *   <li><b>Screen shake:</b> intensity (0..1), duration (ticks)</li>
 *   <li><b>Light:</b> emite light level no impacto (0-15)</li>
 *   <li><b>Sounds:</b> "auto" (school default) ou sound id explícito</li>
 * </ul>
 */
public final class SpellVfxProfile {

    // ─── Colors ───────────────────────────────────────────────
    /** -1 = usar school default. */
    public final int colorPrimary;
    public final int colorSecondary;

    // ─── Sprite ──────────────────────────────────────────────
    public final String spriteStyle; // "auto" | "orb" | "star" | "hex" | "arc" | "shard"

    // ─── Trail (in-flight) ───────────────────────────────────
    public final int trailDensity;       // partículas/tick (3-15)
    public final float trailSize;        // escala (0.5-2.5)
    public final int trailLifetime;      // ticks (10-30)

    // ─── Charge aura (durante channel) ───────────────────────
    public final float chargeIntensity;  // 0..1

    // ─── Impact ───────────────────────────────────────────────
    public final float impactScale;      // 0.5..3.0
    public final int impactParticles;    // count (20-200)

    // ─── Screen effects ──────────────────────────────────────
    public final float screenShake;      // 0..2
    public final int screenShakeTicks;   // 4-20
    public final int impactLight;        // 0-15

    // ─── Sounds (resource locations or "auto") ───────────────
    public final String soundCastStart;
    public final String soundCastFinish;
    public final String soundImpact;

    public SpellVfxProfile(
            int colorPrimary, int colorSecondary, String spriteStyle,
            int trailDensity, float trailSize, int trailLifetime,
            float chargeIntensity,
            float impactScale, int impactParticles,
            float screenShake, int screenShakeTicks, int impactLight,
            String soundCastStart, String soundCastFinish, String soundImpact) {
        this.colorPrimary = colorPrimary;
        this.colorSecondary = colorSecondary;
        this.spriteStyle = spriteStyle == null ? "auto" : spriteStyle;
        this.trailDensity = clamp(trailDensity, 1, 30);
        this.trailSize = clampF(trailSize, 0.3F, 3.0F);
        this.trailLifetime = clamp(trailLifetime, 5, 60);
        this.chargeIntensity = clampF(chargeIntensity, 0F, 1F);
        this.impactScale = clampF(impactScale, 0.3F, 4.0F);
        this.impactParticles = clamp(impactParticles, 5, 500);
        this.screenShake = clampF(screenShake, 0F, 2.5F);
        this.screenShakeTicks = clamp(screenShakeTicks, 1, 40);
        this.impactLight = clamp(impactLight, 0, 15);
        this.soundCastStart = soundCastStart == null ? "auto" : soundCastStart;
        this.soundCastFinish = soundCastFinish == null ? "auto" : soundCastFinish;
        this.soundImpact = soundImpact == null ? "auto" : soundImpact;
    }

    /** Default profile baseado na school (cores -1 = usa school color). */
    public static SpellVfxProfile defaults() {
        return new SpellVfxProfile(
                -1, -1, "auto",
                8, 1.2F, 18,
                0.6F,
                1.0F, 80,
                0.6F, 8, 10,
                "auto", "auto", "auto");
    }

    /** Parse from JSON; missing fields fall back to defaults. */
    public static SpellVfxProfile fromJson(JsonObject json) {
        if (json == null) return defaults();
        SpellVfxProfile d = defaults();
        return new SpellVfxProfile(
                parseColor(json, "color_primary", d.colorPrimary),
                parseColor(json, "color_secondary", d.colorSecondary),
                getStr(json, "sprite", d.spriteStyle),
                getInt(json, "trail_density", d.trailDensity),
                getFloat(json, "trail_size", d.trailSize),
                getInt(json, "trail_lifetime", d.trailLifetime),
                getFloat(json, "charge_intensity", d.chargeIntensity),
                getFloat(json, "impact_scale", d.impactScale),
                getInt(json, "impact_particles", d.impactParticles),
                getFloat(json, "screen_shake", d.screenShake),
                getInt(json, "screen_shake_ticks", d.screenShakeTicks),
                getInt(json, "impact_light", d.impactLight),
                getStr(json, "sound_cast_start", d.soundCastStart),
                getStr(json, "sound_cast_finish", d.soundCastFinish),
                getStr(json, "sound_impact", d.soundImpact));
    }

    // ─── Helpers ──────────────────────────────────────────────

    private static int parseColor(JsonObject j, String key, int fallback) {
        if (!j.has(key)) return fallback;
        String s = j.get(key).getAsString();
        if (s.startsWith("#")) {
            try {
                return Integer.parseInt(s.substring(1), 16);
            } catch (NumberFormatException e) { return fallback; }
        }
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }

    private static String getStr(JsonObject j, String k, String d) {
        return j.has(k) ? j.get(k).getAsString() : d;
    }

    private static int getInt(JsonObject j, String k, int d) {
        return j.has(k) ? j.get(k).getAsInt() : d;
    }

    private static float getFloat(JsonObject j, String k, float d) {
        return j.has(k) ? j.get(k).getAsFloat() : d;
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static float clampF(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
}
