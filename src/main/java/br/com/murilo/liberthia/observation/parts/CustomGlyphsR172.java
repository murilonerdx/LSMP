package br.com.murilo.liberthia.observation.parts;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.api.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * r172: <b>8 glifos novos e divertidos</b> p/ customização avançada de feitiços.
 * Todos {@link Manifestation} (Manifestações) com EFEITO REAL no alvo/área.
 * Reusam texturas de glifo existentes (círculos rúnicos) — ver models.
 *
 * <p>Registrados em {@link br.com.murilo.liberthia.observation.ObservationParts}.
 */
public final class CustomGlyphsR172 {

    private CustomGlyphsR172() {}

    /** Helper: alvo vivo do hit (ou null). */
    private static LivingEntity target(HitResult hit) {
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) return le;
        return null;
    }

    private static int dur(ObservationStats stats, int def) {
        return stats.duration > 0 ? stats.duration : def;
    }

    private static float dmg(ObservationStats stats, float base) {
        return (float) (base * Math.max(0.5, stats.intensity));
    }

    // 1 ── Olhar Vampírico: dano + cura o caster igual ao dano.
    public static class VampiricGaze extends Manifestation {
        public VampiricGaze() { super(new ResourceLocation(LiberthiaMod.MODID, "glyph/vampiric_gaze"), "Olhar Vampírico"); }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 18; }
        @Override public int color() { return 0xAA1133; }
        @Override public SpellClass spellClass() { return SpellClass.COSMIC; }
        @Override public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster, ObservationStats stats, ObservationContext ctx) {
            LivingEntity le = target(hit);
            if (le == null) return;
            float d = dmg(stats, 6F);
            le.hurt(level.damageSources().indirectMagic(caster, caster), d);
            caster.heal(d);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, le.getX(), le.getY() + 1, le.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
            level.sendParticles(ParticleTypes.HEART, caster.getX(), caster.getY() + 1, caster.getZ(), 4, 0.3, 0.4, 0.3, 0.02);
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§cDrena vida do alvo e cura você."),
                Component.literal("§8Dano escala com Amplificar.")); }
    }

    // 2 ── Atração: puxa o alvo até o caster.
    public static class GravityPull extends Manifestation {
        public GravityPull() { super(new ResourceLocation(LiberthiaMod.MODID, "glyph/gravity_pull"), "Atração"); }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 12; }
        @Override public int color() { return 0x5533AA; }
        @Override public SpellClass spellClass() { return SpellClass.COSMIC; }
        @Override public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster, ObservationStats stats, ObservationContext ctx) {
            LivingEntity le = target(hit);
            if (le == null) return;
            Vec3 pull = caster.position().subtract(le.position()).normalize().scale(1.4);
            le.setDeltaMovement(pull.x, 0.25, pull.z);
            le.hurtMarked = true;
            level.sendParticles(ParticleTypes.PORTAL, le.getX(), le.getY() + 1, le.getZ(), 16, 0.3, 0.5, 0.3, 0.1);
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§5Puxa o alvo na sua direção.")); }
    }

    // 3 ── Repulsão: empurra forte pra longe + dano leve.
    public static class Repulse extends Manifestation {
        public Repulse() { super(new ResourceLocation(LiberthiaMod.MODID, "glyph/repulse"), "Repulsão"); }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 12; }
        @Override public int color() { return 0x66CCEE; }
        @Override public SpellClass spellClass() { return SpellClass.AIR; }
        @Override public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster, ObservationStats stats, ObservationContext ctx) {
            LivingEntity le = target(hit);
            if (le == null) return;
            le.hurt(level.damageSources().indirectMagic(caster, caster), dmg(stats, 3F));
            Vec3 away = le.position().subtract(caster.position()).normalize().scale(1.8);
            le.setDeltaMovement(away.x, 0.5, away.z);
            le.hurtMarked = true;
            level.sendParticles(ParticleTypes.CLOUD, le.getX(), le.getY() + 1, le.getZ(), 14, 0.3, 0.3, 0.3, 0.1);
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§bArremessa o alvo pra longe.")); }
    }

    // 4 ── Petrificar: lentidão extrema + fadiga (prende no chão).
    public static class Petrify extends Manifestation {
        public Petrify() { super(new ResourceLocation(LiberthiaMod.MODID, "glyph/petrify"), "Petrificar"); }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 16; }
        @Override public int color() { return 0x999088; }
        @Override public SpellClass spellClass() { return SpellClass.EARTH; }
        @Override public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster, ObservationStats stats, ObservationContext ctx) {
            LivingEntity le = target(hit);
            if (le == null) return;
            int d = dur(stats, 120);
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, d, 6));
            le.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, d, 4));
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, d, 1));
            level.sendParticles(ParticleTypes.WHITE_ASH, le.getX(), le.getY() + 1, le.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§7Petrifica: lentidão e fraqueza extremas.")); }
    }

    // 5 ── Apaziguar: cura/regen + acalma mobs (limpa alvo).
    public static class Soothe extends Manifestation {
        public Soothe() { super(new ResourceLocation(LiberthiaMod.MODID, "glyph/soothe"), "Apaziguar"); }
        @Override public int sanityCost() { return 1; }
        @Override public int sourceCost() { return 12; }
        @Override public int color() { return 0x66DD88; }
        @Override public SpellClass spellClass() { return SpellClass.EARTH; }
        @Override public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster, ObservationStats stats, ObservationContext ctx) {
            LivingEntity le = target(hit);
            if (le == null) le = caster;
            le.addEffect(new MobEffectInstance(MobEffects.REGENERATION, dur(stats, 120), 1));
            if (le instanceof Mob mob) mob.setTarget(null);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, le.getX(), le.getY() + 1, le.getZ(), 14, 0.3, 0.5, 0.3, 0.02);
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§aRegeneração + acalma o alvo (para de te atacar).")); }
    }

    // 6 ── Crestar: incendeia forte + dano de fogo.
    public static class Scorch extends Manifestation {
        public Scorch() { super(new ResourceLocation(LiberthiaMod.MODID, "glyph/scorch"), "Crestar"); }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 14; }
        @Override public int color() { return 0xFF7722; }
        @Override public SpellClass spellClass() { return SpellClass.FIRE; }
        @Override public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster, ObservationStats stats, ObservationContext ctx) {
            LivingEntity le = target(hit);
            if (le == null) return;
            le.setSecondsOnFire(8);
            le.hurt(level.damageSources().indirectMagic(caster, caster), dmg(stats, 4F));
            level.sendParticles(ParticleTypes.FLAME, le.getX(), le.getY() + 1, le.getZ(), 20, 0.3, 0.5, 0.3, 0.04);
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§6Incendeia o alvo por 8s + dano.")); }
    }

    // 7 ── Corrente Ascendente: levita o alvo + queda lenta (anti-gravidade).
    public static class Updraft extends Manifestation {
        public Updraft() { super(new ResourceLocation(LiberthiaMod.MODID, "glyph/updraft"), "Corrente Ascendente"); }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 12; }
        @Override public int color() { return 0xCCE8FF; }
        @Override public SpellClass spellClass() { return SpellClass.AIR; }
        @Override public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster, ObservationStats stats, ObservationContext ctx) {
            LivingEntity le = target(hit);
            if (le == null) le = caster;
            le.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1));
            le.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, dur(stats, 120), 0));
            level.sendParticles(ParticleTypes.CLOUD, le.getX(), le.getY() + 0.2, le.getZ(), 16, 0.3, 0.1, 0.3, 0.08);
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§fEleva o alvo no ar + queda suave.")); }
    }

    // 8 ── Enfraquecer: fraqueza + fadiga + lentidão (desarma o alvo).
    public static class Enfeeble extends Manifestation {
        public Enfeeble() { super(new ResourceLocation(LiberthiaMod.MODID, "glyph/enfeeble"), "Enfraquecer"); }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 16; }
        @Override public int color() { return 0x886699; }
        @Override public SpellClass spellClass() { return SpellClass.COSMIC; }
        @Override public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster, ObservationStats stats, ObservationContext ctx) {
            LivingEntity le = target(hit);
            if (le == null) return;
            int d = dur(stats, 200);
            le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, d, 2));
            le.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, d, 2));
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, d, 1));
            level.sendParticles(ParticleTypes.SQUID_INK, le.getX(), le.getY() + 1, le.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§5Enfraquece muito o alvo (ataque/quebra/velocidade).")); }
    }
}
