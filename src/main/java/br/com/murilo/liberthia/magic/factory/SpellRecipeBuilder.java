package br.com.murilo.liberthia.magic.factory;

import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.List;

/**
 * r148: Fluent builder pra criar SpellRecipes em código Java.
 *
 * <h2>Exemplo</h2>
 * <pre>{@code
 * SpellRecipe fireball = SpellRecipeBuilder.of("super_fireball")
 *     .name("Super Bola de Fogo")
 *     .school(SpellSchool.FIRE)
 *     .rarity(Rarity.RARE)
 *     .mana(40).cooldown(80).damage(15).range(32)
 *     .type(SpellType.PROJECTILE)
 *     .lore("Versão turbinada da fireball clássica.")
 *     .behavior(b -> b.speed(2.0F).aoeRadius(4F).igniteBlocks(true))
 *     .vfx(v -> v.colorPrimary(0xFF8800).trailDensity(12).screenShake(0.8F))
 *     .effect(SpellEffectSpec.Type.IGNITE, 120, 0, 0)
 *     .effect(SpellEffectSpec.Type.KNOCKBACK, 0, 0, 1.0F)
 *     .build();
 * SpellRecipeRegistry.register(fireball);
 * }</pre>
 */
public final class SpellRecipeBuilder {

    private final String id;
    private String name = "Unnamed Spell";
    private SpellSchool school = SpellSchool.FIRE;
    private Rarity rarity = Rarity.COMMON;
    private int mana = 10;
    private int cooldown = 40;
    private float damage = 4;
    private float range = 16;
    private SpellType type = SpellType.PROJECTILE;
    private String lore = "";
    private SpellBehavior behavior = SpellBehavior.defaults();
    private SpellVfxProfile vfx = SpellVfxProfile.defaults();
    private final List<SpellEffectSpec> effects = new ArrayList<>();

    private SpellRecipeBuilder(String id) {
        this.id = id;
    }

    public static SpellRecipeBuilder of(String id) { return new SpellRecipeBuilder(id); }

    public SpellRecipeBuilder name(String n) { this.name = n; return this; }
    public SpellRecipeBuilder school(SpellSchool s) { this.school = s; return this; }
    public SpellRecipeBuilder rarity(Rarity r) { this.rarity = r; return this; }
    public SpellRecipeBuilder mana(int m) { this.mana = m; return this; }
    public SpellRecipeBuilder cooldown(int c) { this.cooldown = c; return this; }
    public SpellRecipeBuilder damage(float d) { this.damage = d; return this; }
    public SpellRecipeBuilder range(float r) { this.range = r; return this; }
    public SpellRecipeBuilder type(SpellType t) { this.type = t; return this; }
    public SpellRecipeBuilder lore(String l) { this.lore = l; return this; }

    public SpellRecipeBuilder behavior(java.util.function.Consumer<BehaviorBuilder> fn) {
        BehaviorBuilder b = new BehaviorBuilder();
        fn.accept(b);
        this.behavior = b.build();
        return this;
    }

    public SpellRecipeBuilder vfx(java.util.function.Consumer<VfxBuilder> fn) {
        VfxBuilder v = new VfxBuilder();
        fn.accept(v);
        this.vfx = v.build();
        return this;
    }

    public SpellRecipeBuilder effect(SpellEffectSpec.Type type, int duration, int amplifier, float magnitude) {
        this.effects.add(new SpellEffectSpec(type, duration, amplifier, magnitude));
        return this;
    }

    private SpellElement element = SpellElement.NONE;
    private SpellElement secondaryElement = SpellElement.NONE;
    private SpellCategory category = SpellCategory.PROJECTILE;

    public SpellRecipeBuilder element(SpellElement e) { this.element = e; return this; }
    public SpellRecipeBuilder secondaryElement(SpellElement e) { this.secondaryElement = e; return this; }
    public SpellRecipeBuilder category(SpellCategory c) { this.category = c; return this; }

    public SpellRecipe build() {
        return new SpellRecipe(id, name, school, rarity, mana, cooldown, damage, range,
                type, lore, behavior, vfx, effects, element, secondaryElement, category);
    }

    // ─── BehaviorBuilder ────────────────────────────────────────────
    public static final class BehaviorBuilder {
        SpellBehavior d = SpellBehavior.defaults();
        float speed = d.speed;
        int lifetime = d.lifetimeTicks;
        boolean pierce = d.pierce;
        float aoeRadius = d.aoeRadius;
        boolean igniteBlocks = d.igniteBlocks;
        int projectileCount = d.projectileCount;
        float spreadDegrees = d.spreadDegrees;
        boolean homing = d.homing;
        float radius = d.radius;
        float coneAngle = d.coneAngle;
        int rainStrikes = d.rainStrikes;
        int rainDurationTicks = d.rainDurationTicks;
        int auraDurationTicks = d.auraDurationTicks;
        int auraTickInterval = d.auraTickIntervalTicks;
        float dashDistance = d.dashDistance;
        float touchRange = d.touchRange;
        int beamMaxTicks = d.beamMaxTicks;
        float beamRange = d.beamRange;

        public BehaviorBuilder speed(float v) { speed = v; return this; }
        public BehaviorBuilder lifetime(int v) { lifetime = v; return this; }
        public BehaviorBuilder pierce(boolean v) { pierce = v; return this; }
        public BehaviorBuilder aoeRadius(float v) { aoeRadius = v; return this; }
        public BehaviorBuilder igniteBlocks(boolean v) { igniteBlocks = v; return this; }
        public BehaviorBuilder projectileCount(int v) { projectileCount = v; return this; }
        public BehaviorBuilder spread(float v) { spreadDegrees = v; return this; }
        public BehaviorBuilder homing(boolean v) { homing = v; return this; }
        public BehaviorBuilder radius(float v) { radius = v; return this; }
        public BehaviorBuilder coneAngle(float v) { coneAngle = v; return this; }
        public BehaviorBuilder rainStrikes(int v) { rainStrikes = v; return this; }
        public BehaviorBuilder rainDuration(int v) { rainDurationTicks = v; return this; }
        public BehaviorBuilder auraDuration(int v) { auraDurationTicks = v; return this; }
        public BehaviorBuilder auraTickInterval(int v) { auraTickInterval = v; return this; }
        public BehaviorBuilder dashDistance(float v) { dashDistance = v; return this; }
        public BehaviorBuilder touchRange(float v) { touchRange = v; return this; }
        public BehaviorBuilder beamMaxTicks(int v) { beamMaxTicks = v; return this; }
        public BehaviorBuilder beamRange(float v) { beamRange = v; return this; }

        // r151: campos novos
        float critChance = d.critChance;
        float critMultiplier = d.critMultiplier;
        float knockbackStrength = d.knockbackStrength;
        float height = d.height;
        boolean selfShake = d.selfShake;
        boolean penetrateBlocks = d.penetrateBlocks;

        public BehaviorBuilder critChance(float v) { critChance = v; return this; }
        public BehaviorBuilder critMultiplier(float v) { critMultiplier = v; return this; }
        public BehaviorBuilder knockback(float v) { knockbackStrength = v; return this; }
        public BehaviorBuilder height(float v) { height = v; return this; }
        public BehaviorBuilder selfShake(boolean v) { selfShake = v; return this; }
        public BehaviorBuilder penetrateBlocks(boolean v) { penetrateBlocks = v; return this; }

        SpellBehavior build() {
            return new SpellBehavior(speed, lifetime, pierce, aoeRadius, igniteBlocks,
                    projectileCount, spreadDegrees, homing, radius, coneAngle,
                    rainStrikes, rainDurationTicks, auraDurationTicks, auraTickInterval,
                    dashDistance, touchRange, beamMaxTicks, beamRange,
                    critChance, critMultiplier, knockbackStrength, height, selfShake, penetrateBlocks);
        }
    }

    // ─── VfxBuilder ─────────────────────────────────────────────────
    public static final class VfxBuilder {
        SpellVfxProfile d = SpellVfxProfile.defaults();
        int colorPrimary = d.colorPrimary;
        int colorSecondary = d.colorSecondary;
        String spriteStyle = d.spriteStyle;
        int trailDensity = d.trailDensity;
        float trailSize = d.trailSize;
        int trailLifetime = d.trailLifetime;
        float chargeIntensity = d.chargeIntensity;
        float impactScale = d.impactScale;
        int impactParticles = d.impactParticles;
        float screenShake = d.screenShake;
        int screenShakeTicks = d.screenShakeTicks;
        int impactLight = d.impactLight;
        String soundCastStart = d.soundCastStart;
        String soundCastFinish = d.soundCastFinish;
        String soundImpact = d.soundImpact;

        public VfxBuilder colorPrimary(int v) { colorPrimary = v; return this; }
        public VfxBuilder colorSecondary(int v) { colorSecondary = v; return this; }
        public VfxBuilder sprite(String v) { spriteStyle = v; return this; }
        public VfxBuilder trailDensity(int v) { trailDensity = v; return this; }
        public VfxBuilder trailSize(float v) { trailSize = v; return this; }
        public VfxBuilder trailLifetime(int v) { trailLifetime = v; return this; }
        public VfxBuilder chargeIntensity(float v) { chargeIntensity = v; return this; }
        public VfxBuilder impactScale(float v) { impactScale = v; return this; }
        public VfxBuilder impactParticles(int v) { impactParticles = v; return this; }
        public VfxBuilder screenShake(float v) { screenShake = v; return this; }
        public VfxBuilder screenShakeTicks(int v) { screenShakeTicks = v; return this; }
        public VfxBuilder impactLight(int v) { impactLight = v; return this; }

        SpellVfxProfile build() {
            return new SpellVfxProfile(colorPrimary, colorSecondary, spriteStyle,
                    trailDensity, trailSize, trailLifetime, chargeIntensity,
                    impactScale, impactParticles, screenShake, screenShakeTicks, impactLight,
                    soundCastStart, soundCastFinish, soundImpact);
        }
    }
}
