package br.com.murilo.liberthia.magic.factory;

import com.google.gson.JsonObject;

/**
 * r148: Parâmetros de comportamento de um feitiço.
 *
 * <p>Campos são inclusivos — cada {@link SpellType} usa SUBSET destes valores.
 * Ex: PROJECTILE usa speed/lifetime/pierce/aoe, AURA usa duration/radius/tickInterval,
 * DASH usa dashDistance, etc.
 *
 * <p>Defaults razoáveis pra qualquer tipo de spell.
 */
public final class SpellBehavior {

    // ─── Projectile-like ────────────────────────────────────
    public final float speed;            // blocks/tick (0.5-3.0)
    public final int lifetimeTicks;      // ticks até despawnar (20-200)
    public final boolean pierce;          // passa por entidades
    public final float aoeRadius;         // splash damage radius (0 = single)
    public final boolean igniteBlocks;    // FIRE spells põem fogo no chão
    public final int projectileCount;     // pra MULTI_SHOT
    public final float spreadDegrees;     // ângulo do cone de spread
    public final boolean homing;          // só faz sentido se type == HOMING

    // ─── Area effects ───────────────────────────────────────
    public final float radius;            // raio do AOE/NOVA/CONE/RAIN
    public final float coneAngle;         // pra CONE (graus)
    public final int rainStrikes;         // pra RAIN (número de strikes)
    public final int rainDurationTicks;   // pra RAIN (tempo total)

    // ─── Aura (sustained) ───────────────────────────────────
    public final int auraDurationTicks;   // ticks que dura a aura
    public final int auraTickIntervalTicks; // a cada X ticks aplica dmg/heal

    // ─── Self / Touch / Dash ────────────────────────────────
    public final float dashDistance;      // blocks (pra DASH)
    public final float touchRange;        // blocos pra TOUCH (default 4)

    // ─── Channel/Beam ───────────────────────────────────────
    public final int beamMaxTicks;        // duração max do beam
    public final float beamRange;         // alcance do beam (override range)

    // ─── r151: novos campos AVANÇADOS ───────────────────────
    /** Chance de crítico 0..1 — multiplica dano por critMultiplier. */
    public final float critChance;
    /** Multiplicador de dano em crítico. */
    public final float critMultiplier;
    /** Força do knockback aplicado no impacto (0 = sem KB). */
    public final float knockbackStrength;
    /** Altura vertical do AOE (default = radius). */
    public final float height;
    /** Vibration screen no caster (não inimigos). */
    public final boolean selfShake;
    /** Penetra através de blocos? (raycast ignora terreno) */
    public final boolean penetrateBlocks;

    public SpellBehavior(
            float speed, int lifetimeTicks, boolean pierce, float aoeRadius,
            boolean igniteBlocks, int projectileCount, float spreadDegrees, boolean homing,
            float radius, float coneAngle, int rainStrikes, int rainDurationTicks,
            int auraDurationTicks, int auraTickIntervalTicks,
            float dashDistance, float touchRange,
            int beamMaxTicks, float beamRange,
            float critChance, float critMultiplier, float knockbackStrength,
            float height, boolean selfShake, boolean penetrateBlocks) {
        this.speed = speed;
        this.lifetimeTicks = lifetimeTicks;
        this.pierce = pierce;
        this.aoeRadius = aoeRadius;
        this.igniteBlocks = igniteBlocks;
        this.projectileCount = Math.max(1, projectileCount);
        this.spreadDegrees = spreadDegrees;
        this.homing = homing;
        this.radius = radius;
        this.coneAngle = coneAngle;
        this.rainStrikes = rainStrikes;
        this.rainDurationTicks = rainDurationTicks;
        this.auraDurationTicks = auraDurationTicks;
        this.auraTickIntervalTicks = Math.max(1, auraTickIntervalTicks);
        this.dashDistance = dashDistance;
        this.touchRange = touchRange;
        this.beamMaxTicks = beamMaxTicks;
        this.beamRange = beamRange;
        this.critChance = Math.max(0F, Math.min(1F, critChance));
        this.critMultiplier = Math.max(1F, critMultiplier);
        this.knockbackStrength = Math.max(0F, knockbackStrength);
        this.height = Math.max(0F, height);
        this.selfShake = selfShake;
        this.penetrateBlocks = penetrateBlocks;
    }

    public static SpellBehavior defaults() {
        return new SpellBehavior(
                1.4F, 80, false, 0F,
                false, 1, 0F, false,
                3F, 60F, 5, 60,
                60, 10,
                4F, 4F,
                40, 16F,
                0F, 1.5F, 0F, 3F, false, false);
    }

    public static SpellBehavior fromJson(JsonObject j) {
        if (j == null) return defaults();
        SpellBehavior d = defaults();
        return new SpellBehavior(
                getF(j, "speed", d.speed),
                getI(j, "lifetime_ticks", d.lifetimeTicks),
                getB(j, "pierce", d.pierce),
                getF(j, "aoe_radius", d.aoeRadius),
                getB(j, "ignite_blocks", d.igniteBlocks),
                getI(j, "projectile_count", d.projectileCount),
                getF(j, "spread_degrees", d.spreadDegrees),
                getB(j, "homing", d.homing),
                getF(j, "radius", d.radius),
                getF(j, "cone_angle", d.coneAngle),
                getI(j, "rain_strikes", d.rainStrikes),
                getI(j, "rain_duration_ticks", d.rainDurationTicks),
                getI(j, "aura_duration_ticks", d.auraDurationTicks),
                getI(j, "aura_tick_interval", d.auraTickIntervalTicks),
                getF(j, "dash_distance", d.dashDistance),
                getF(j, "touch_range", d.touchRange),
                getI(j, "beam_max_ticks", d.beamMaxTicks),
                getF(j, "beam_range", d.beamRange),
                // r151 — novos
                getF(j, "crit_chance", d.critChance),
                getF(j, "crit_multiplier", d.critMultiplier),
                getF(j, "knockback_strength", d.knockbackStrength),
                getF(j, "height", d.height),
                getB(j, "self_shake", d.selfShake),
                getB(j, "penetrate_blocks", d.penetrateBlocks));
    }

    private static int getI(JsonObject j, String k, int d) { return j.has(k) ? j.get(k).getAsInt() : d; }
    private static float getF(JsonObject j, String k, float d) { return j.has(k) ? j.get(k).getAsFloat() : d; }
    private static boolean getB(JsonObject j, String k, boolean d) { return j.has(k) ? j.get(k).getAsBoolean() : d; }
}
