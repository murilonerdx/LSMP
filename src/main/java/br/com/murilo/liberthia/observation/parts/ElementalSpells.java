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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * v0.1.22 r71: <b>25 ElementalSpells</b> — feitiços classificados em
 * Fogo/Água/Terra/Ar/Cósmico.
 *
 * <p>Cada um implementa Manifestation (effects). Quando equipado em ImbuedSword,
 * a classe elemental define propriedades passivas extras na espada.
 *
 * <h2>Distribution</h2>
 * <ul>
 *   <li>FIRE (5): Fireball, Inferno, Cleansing Flame, Solar Pulse, Burning Aura</li>
 *   <li>WATER (5): Bubble Shield, Tidal Wave, Frost Lance, Mist Veil, Healing Rain</li>
 *   <li>EARTH (5): Stone Spikes, Quake Step, Vein Sight, Earthen Wall, Roots</li>
 *   <li>AIR (5): Gust, Tornado, Sky Step, Velocity, Wind Cutter</li>
 *   <li>COSMIC (5): Void Pull, Dread Stare, Mind Spike, Reality Tear, Singularity</li>
 * </ul>
 */
public final class ElementalSpells {

    private ElementalSpells() {}

    // ═══════════════════════ FIRE (5) ═══════════════════════

    /** Fireball — pequena explosão de fogo + ignite. */
    public static class FireballEffect extends Manifestation {
        public FireballEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "elem/fireball"), "Bola de Fogo");
        }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 20; }
        @Override public int color() { return 0xFF5522; }
        @Override public SpellClass spellClass() { return SpellClass.FIRE; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            Vec3 pos = hit.getLocation();
            level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 8, 0.5, 0.5, 0.5, 0.1);
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.hurt(level.damageSources().magic(), (float)(4.0 * stats.intensity));
                le.setRemainingFireTicks(80);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§c§oBola de fogo. Dano + queima.")); }
    }

    /** Inferno — high damage AOE fire centered on hit. */
    public static class InfernoEffect extends Manifestation {
        public InfernoEffect() {
            super(new ResourceLocation(LiberthiaMod.MODID, "elem/inferno"), "Inferno");
        }
        @Override public int sanityCost() { return 6; }
        @Override public int sourceCost() { return 45; }
        @Override public int color() { return 0xFF3300; }
        @Override public SpellClass spellClass() { return SpellClass.FIRE; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            Vec3 pos = hit.getLocation();
            var targets = level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(pos.subtract(4,4,4), pos.add(4,4,4)),
                e -> e != caster && e.isAlive());
            for (var t : targets) {
                t.hurt(level.damageSources().magic(), (float)(8.0 * stats.intensity));
                t.setRemainingFireTicks(200);
            }
            for (int i = 0; i < 30; i++) {
                double a = Math.random() * Math.PI * 2, r = Math.random() * 4;
                level.sendParticles(ParticleTypes.FLAME,
                    pos.x + Math.cos(a)*r, pos.y + Math.random()*2, pos.z + Math.sin(a)*r,
                    1, 0.1, 0.1, 0.1, 0.05);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§c§lAOE 4b §rfogo intenso. §c8x dano.")); }
    }

    /** Cleansing Flame — heal self + burn undead nearby. */
    public static class CleansingFlame extends Manifestation {
        public CleansingFlame() {
            super(new ResourceLocation(LiberthiaMod.MODID, "elem/cleansing_flame"), "Chama Purificadora");
        }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 25; }
        @Override public int color() { return 0xFFAA44; }
        @Override public SpellClass spellClass() { return SpellClass.FIRE; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            caster.heal(6.0f);
            caster.removeEffect(MobEffects.WITHER);
            caster.removeEffect(MobEffects.POISON);
            // Burn undead 3b ao redor
            var undeads = level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(3.0),
                e -> e != caster && e.getMobType() == net.minecraft.world.entity.MobType.UNDEAD);
            for (var u : undeads) {
                u.hurt(level.damageSources().magic(), 6.0f);
                u.setRemainingFireTicks(100);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§6Cura self + queima mortos-vivos.")); }
    }

    /** Solar Pulse — anti-darkness, blinds enemies. */
    public static class SolarPulse extends Manifestation {
        public SolarPulse() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/solar_pulse"), "Pulso Solar"); }
        @Override public int sanityCost() { return 4; }
        @Override public int sourceCost() { return 30; }
        @Override public int color() { return 0xFFEE66; }
        @Override public SpellClass spellClass() { return SpellClass.FIRE; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(8.0),
                e -> e != caster);
            for (var n : nearby) {
                n.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                n.hurt(level.damageSources().magic(), 2.0f);
            }
            caster.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§eCega inimigos em 8b. Marca self.")); }
    }

    /** Burning Aura — buff: weapon ignites on hit. */
    public static class BurningAura extends Manifestation {
        public BurningAura() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/burning_aura"), "Aura Ardente"); }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 20; }
        @Override public int color() { return 0xFF8833; }
        @Override public SpellClass spellClass() { return SpellClass.FIRE; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            int dur = stats.duration > 0 ? stats.duration : 600;
            caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, dur, 0));
            caster.getPersistentData().putInt("liberthia.burning_aura_ticks", dur);
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§6Fire Resist + ataques queimam.")); }
    }

    // ═══════════════════════ WATER (5) ═══════════════════════

    /** Bubble Shield — water_breathing + small absorption. */
    public static class BubbleShield extends Manifestation {
        public BubbleShield() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/bubble_shield"), "Bolha"); }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 15; }
        @Override public int color() { return 0x66CCFF; }
        @Override public SpellClass spellClass() { return SpellClass.WATER; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            caster.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 600, 0));
            caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§bWater Breathing + 4 absorção.")); }
    }

    /** Tidal Wave — radial knockback + water flood VFX. */
    public static class TidalWave extends Manifestation {
        public TidalWave() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/tidal_wave"), "Maremoto"); }
        @Override public int sanityCost() { return 4; }
        @Override public int sourceCost() { return 30; }
        @Override public int color() { return 0x3366CC; }
        @Override public SpellClass spellClass() { return SpellClass.WATER; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(6.0),
                e -> e != caster);
            for (var n : nearby) {
                Vec3 dir = n.position().subtract(caster.position()).normalize();
                n.setDeltaMovement(dir.x*1.5, 0.5, dir.z*1.5);
                n.hurtMarked = true;
                n.hurt(level.damageSources().magic(), 3.0f);
            }
            // VFX
            for (int i = 0; i < 50; i++) {
                double a = i * Math.PI*2/50;
                level.sendParticles(ParticleTypes.SPLASH,
                    caster.getX() + Math.cos(a)*3, caster.getY()+0.5, caster.getZ() + Math.sin(a)*3,
                    2, 0.1, 0.1, 0.1, 0.05);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§3Onda radial empurra + 3 damage.")); }
    }

    /** Frost Lance — freeze projectile-style on hit. */
    public static class FrostLance extends Manifestation {
        public FrostLance() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/frost_lance"), "Lança de Gelo"); }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 22; }
        @Override public int color() { return 0xAADDFF; }
        @Override public SpellClass spellClass() { return SpellClass.WATER; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.hurt(level.damageSources().magic(), (float)(5.0 * stats.intensity));
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                le.setTicksFrozen(le.getTicksFrozen() + 200);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§b5 damage + slowness III + freeze.")); }
    }

    /** Mist Veil — invisibility + speed. */
    public static class MistVeil extends Manifestation {
        public MistVeil() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/mist_veil"), "Véu de Névoa"); }
        @Override public int sanityCost() { return 4; }
        @Override public int sourceCost() { return 28; }
        @Override public int color() { return 0xCCDDFF; }
        @Override public SpellClass spellClass() { return SpellClass.WATER; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            int dur = stats.duration > 0 ? stats.duration : 200;
            caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, dur, 0));
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 1));
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§7Invisibilidade + speed II — fuga.")); }
    }

    /** Healing Rain — heal AOE. */
    public static class HealingRain extends Manifestation {
        public HealingRain() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/healing_rain"), "Chuva Curativa"); }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 30; }
        @Override public int color() { return 0x88FFAA; }
        @Override public SpellClass spellClass() { return SpellClass.WATER; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            var allies = level.getEntitiesOfClass(Player.class,
                caster.getBoundingBox().inflate(8.0), p -> true);
            for (var p : allies) {
                p.heal(8.0f);
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            }
            // VFX
            Vec3 pos = caster.position();
            for (int i = 0; i < 40; i++) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.x + (Math.random()-0.5)*8, pos.y+3, pos.z + (Math.random()-0.5)*8,
                    1, 0, -0.1, 0, 0);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§aCura todos players em 8b + regen II.")); }
    }

    // ═══════════════════════ EARTH (5) ═══════════════════════

    /** Stone Spikes — damage + slowdown. */
    public static class StoneSpikes extends Manifestation {
        public StoneSpikes() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/stone_spikes"), "Espinhos de Pedra"); }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 22; }
        @Override public int color() { return 0x886655; }
        @Override public SpellClass spellClass() { return SpellClass.EARTH; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.hurt(level.damageSources().magic(), (float)(6.0 * stats.intensity));
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                // Stone block particles via BlockStateParticleOption
                level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                        net.minecraft.core.particles.ParticleTypes.BLOCK,
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                    le.getX(), le.getY(), le.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§77 damage + lentidão.")); }
    }

    /** Quake Step — teleport short range + AOE. */
    public static class QuakeStep extends Manifestation {
        public QuakeStep() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/quake_step"), "Passo Sísmico"); }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 25; }
        @Override public int color() { return 0xAA8866; }
        @Override public SpellClass spellClass() { return SpellClass.EARTH; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            Vec3 target = hit.getLocation();
            caster.teleportTo(target.x, target.y, target.z);
            // AOE damage on arrival
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(3.0), e -> e != caster);
            for (var n : nearby) {
                n.hurt(level.damageSources().magic(), 3.0f);
                Vec3 dir = n.position().subtract(target).normalize();
                n.setDeltaMovement(dir.x, 0.4, dir.z);
                n.hurtMarked = true;
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§6TP até hit + tremor AOE.")); }
    }

    /** Vein Sight — Glowing on nearby ores (mob-find via glow). */
    public static class VeinSight extends Manifestation {
        public VeinSight() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/vein_sight"), "Visão de Veios"); }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 15; }
        @Override public int color() { return 0x88AA66; }
        @Override public SpellClass spellClass() { return SpellClass.EARTH; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            // Glow nearby living entities (proxy for "find things")
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(16.0), e -> e != caster);
            for (var n : nearby) {
                n.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
            }
            caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§7Glowing em 16b + night vision.")); }
    }

    /** Earthen Wall — short summon block wall (placeholder = absorption). */
    public static class EarthenWall extends Manifestation {
        public EarthenWall() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/earthen_wall"), "Parede de Terra"); }
        @Override public int sanityCost() { return 3; }
        @Override public int sourceCost() { return 25; }
        @Override public int color() { return 0x664422; }
        @Override public SpellClass spellClass() { return SpellClass.EARTH; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
            caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 3));
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§6Resistance III + 8 absorption.")); }
    }

    /** Roots — root target in place. */
    public static class RootsEffect extends Manifestation {
        public RootsEffect() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/roots"), "Raízes"); }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 18; }
        @Override public int color() { return 0x556622; }
        @Override public SpellClass spellClass() { return SpellClass.EARTH; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4));
                le.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, -10)); // anti-jump
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§2Trava alvo no chão por 10s.")); }
    }

    // ═══════════════════════ AIR (5) ═══════════════════════

    /** Gust — knockback + jumpboost. */
    public static class GustEffect extends Manifestation {
        public GustEffect() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/gust"), "Rajada"); }
        @Override public int sanityCost() { return 1; }
        @Override public int sourceCost() { return 8; }
        @Override public int color() { return 0xEEEEFF; }
        @Override public SpellClass spellClass() { return SpellClass.AIR; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (hit instanceof EntityHitResult ehr) {
                var e = ehr.getEntity();
                Vec3 dir = e.position().subtract(caster.position()).normalize();
                e.setDeltaMovement(dir.x*2.0, 1.0, dir.z*2.0);
                e.hurtMarked = true;
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§fEmpurra forte + sobe.")); }
    }

    /** Tornado — pulls + lifts. */
    public static class TornadoEffect extends Manifestation {
        public TornadoEffect() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/tornado"), "Tornado"); }
        @Override public int sanityCost() { return 5; }
        @Override public int sourceCost() { return 35; }
        @Override public int color() { return 0xCCCCEE; }
        @Override public SpellClass spellClass() { return SpellClass.AIR; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            Vec3 center = hit.getLocation();
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center.subtract(5,5,5), center.add(5,5,5)),
                e -> e != caster);
            for (var n : nearby) {
                Vec3 toCenter = center.subtract(n.position()).normalize();
                n.setDeltaMovement(toCenter.x*0.5, 1.2, toCenter.z*0.5);
                n.hurtMarked = true;
                n.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 1));
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§fPuxa + leva pra cima — AOE 5.")); }
    }

    /** Sky Step — teleport up + slow falling. */
    public static class SkyStep extends Manifestation {
        public SkyStep() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/sky_step"), "Passo Celeste"); }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 18; }
        @Override public int color() { return 0xDDDDFF; }
        @Override public SpellClass spellClass() { return SpellClass.AIR; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            caster.teleportTo(caster.getX(), caster.getY() + 8, caster.getZ());
            caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§fSobe 8 blocos + slow fall.")); }
    }

    /** Velocity — speed III. */
    public static class VelocityEffect extends Manifestation {
        public VelocityEffect() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/velocity"), "Velocidade"); }
        @Override public int sanityCost() { return 2; }
        @Override public int sourceCost() { return 12; }
        @Override public int color() { return 0xFFFFCC; }
        @Override public SpellClass spellClass() { return SpellClass.AIR; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            int dur = stats.duration > 0 ? stats.duration : 400;
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 2));
            caster.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, dur, 1));
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§eSpeed III + Haste II.")); }
    }

    /** Wind Cutter — high damage line slash. */
    public static class WindCutter extends Manifestation {
        public WindCutter() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/wind_cutter"), "Lâmina do Vento"); }
        @Override public int sanityCost() { return 4; }
        @Override public int sourceCost() { return 28; }
        @Override public int color() { return 0xCCFFCC; }
        @Override public SpellClass spellClass() { return SpellClass.AIR; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            Vec3 look = caster.getLookAngle();
            Vec3 start = caster.position();
            for (int i = 1; i <= 10; i++) {
                Vec3 p = start.add(look.scale(i));
                var targets = level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(p.subtract(1.5,1.5,1.5), p.add(1.5,1.5,1.5)),
                    e -> e != caster);
                for (var t : targets) {
                    t.hurt(level.damageSources().magic(), (float)(4.0 * stats.intensity));
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y+1, p.z, 1, 0, 0, 0, 0);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§fLinha de 10b cortando tudo.")); }
    }

    // ═══════════════════════ COSMIC (5) ═══════════════════════

    /** Void Pull — teleport target to caster's feet. */
    public static class VoidPull extends Manifestation {
        public VoidPull() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/void_pull"), "Atração do Vazio"); }
        @Override public int sanityCost() { return 4; }
        @Override public int sourceCost() { return 28; }
        @Override public int color() { return 0x442266; }
        @Override public SpellClass spellClass() { return SpellClass.COSMIC; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.teleportTo(caster.getX(), caster.getY(), caster.getZ() + 1);
                le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                le.hurt(level.damageSources().magic(), 3.0f);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§5TP alvo até você.")); }
    }

    /** Dread Stare — fear + slowness + nausea. */
    public static class DreadStare extends Manifestation {
        public DreadStare() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/dread_stare"), "Olhar de Pavor"); }
        @Override public int sanityCost() { return 5; }
        @Override public int sourceCost() { return 30; }
        @Override public int color() { return 0x331144; }
        @Override public SpellClass spellClass() { return SpellClass.COSMIC; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            int dur = stats.duration > 0 ? stats.duration : 200;
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 3));
                le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, dur, 0));
                le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, dur, 0));
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 2));
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§5Slow + nausea + darkness + weakness.")); }
    }

    /** Mind Spike — pure mental damage ignoring armor. */
    public static class MindSpike extends Manifestation {
        public MindSpike() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/mind_spike"), "Estaca Mental"); }
        @Override public int sanityCost() { return 6; }
        @Override public int sourceCost() { return 35; }
        @Override public int color() { return 0x880088; }
        @Override public SpellClass spellClass() { return SpellClass.COSMIC; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
                // Drain health bypass armor via fall damage type (no armor reduction)
                float dmg = (float)(8.0 * stats.intensity);
                le.hurt(level.damageSources().magic(), dmg);
                le.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 2));
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§58 damage mágico + hunger III.")); }
    }

    /** Reality Tear — wither + nausea AOE. */
    public static class RealityTear extends Manifestation {
        public RealityTear() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/reality_tear"), "Fenda da Realidade"); }
        @Override public int sanityCost() { return 7; }
        @Override public int sourceCost() { return 45; }
        @Override public int color() { return 0x220033; }
        @Override public SpellClass spellClass() { return SpellClass.COSMIC; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            Vec3 center = hit.getLocation();
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center.subtract(5,5,5), center.add(5,5,5)),
                e -> e != caster);
            for (var n : nearby) {
                n.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
                n.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
                n.hurt(level.damageSources().magic(), 4.0f);
            }
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§5Wither III + confusion AOE 5b.")); }
    }

    /** Singularity — pull all entities to a single point. */
    public static class SingularityEffect extends Manifestation {
        public SingularityEffect() { super(new ResourceLocation(LiberthiaMod.MODID, "elem/singularity"), "Singularidade"); }
        @Override public int sanityCost() { return 8; }
        @Override public int sourceCost() { return 60; }
        @Override public int color() { return 0x000022; }
        @Override public SpellClass spellClass() { return SpellClass.COSMIC; }
        @Override
        public void manifest(HitResult hit, ServerLevel level, ServerPlayer caster,
                              ObservationStats stats, ObservationContext ctx) {
            Vec3 pull = hit.getLocation();
            var nearby = level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(pull.subtract(10,10,10), pull.add(10,10,10)),
                e -> e != caster);
            for (var n : nearby) {
                Vec3 dir = pull.subtract(n.position()).normalize();
                n.setDeltaMovement(dir.x*0.8, dir.y*0.8 + 0.2, dir.z*0.8);
                n.hurtMarked = true;
                n.hurt(level.damageSources().magic(), 5.0f);
                n.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0));
            }
            level.sendParticles(ParticleTypes.SCULK_SOUL, pull.x, pull.y, pull.z, 40, 0.5, 0.5, 0.5, 0.1);
        }
        @Override public List<Component> lore() { return List.of(Component.literal("§5Black hole. Puxa tudo em 10b + 5 damage.")); }
    }
}
