package br.com.murilo.liberthia.magic.effect;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * v0.1.24 r83: <b>Synergy Effects</b> — 8 mob effects que chainam entre si.
 *
 * <h2>Synergy chains</h2>
 * <ul>
 *   <li>CHILLED → 5+ stacks vira FROSTBITE → dano hipotermia</li>
 *   <li>BURNING → tomou fogo de novo vira IMMOLATE → explode pequena</li>
 *   <li>BLEED → low HP triggera HEMORRHAGE → DOT massivo</li>
 *   <li>STATIC → 3+ hits triggera STORM_MARK → próximo hit lightning chain</li>
 *   <li>HEARTSTOP → 2s parado, mata se sanity &lt; 20</li>
 * </ul>
 */
public final class SynergyEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, LiberthiaMod.MODID);

    // ─── ICE CHAIN ───
    public static final RegistryObject<MobEffect> CHILLED = EFFECTS.register("chilled",
            () -> new ChilledEffect());
    public static final RegistryObject<MobEffect> FROSTBITE = EFFECTS.register("frostbite",
            () -> new FrostbiteEffect());

    // ─── FIRE CHAIN ───
    public static final RegistryObject<MobEffect> BURNING_MARK = EFFECTS.register("burning_mark",
            () -> new BurningMarkEffect());
    public static final RegistryObject<MobEffect> IMMOLATE = EFFECTS.register("immolate",
            () -> new ImmolateEffect());

    // ─── BLOOD CHAIN ───
    public static final RegistryObject<MobEffect> BLEED = EFFECTS.register("bleed",
            () -> new BleedEffect());
    public static final RegistryObject<MobEffect> HEMORRHAGE = EFFECTS.register("hemorrhage",
            () -> new HemorrhageEffect());

    // ─── LIGHTNING CHAIN ───
    public static final RegistryObject<MobEffect> STATIC_CHARGE = EFFECTS.register("static_charge",
            () -> new StaticChargeEffect());
    public static final RegistryObject<MobEffect> STORM_MARK = EFFECTS.register("storm_mark",
            () -> new StormMarkEffect());

    // ─── ELDRITCH ───
    public static final RegistryObject<MobEffect> HEARTSTOP = EFFECTS.register("heartstop",
            () -> new HeartstopEffect());

    // ─── ASCENSION (r96) ───
    public static final RegistryObject<MobEffect> ASCENSION = EFFECTS.register("ascension",
            () -> new AscensionEffect());

    private SynergyEffects() {}

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    // ════════════════════════════════════════════════════════════════════════
    // EFFECT IMPLEMENTATIONS
    // ════════════════════════════════════════════════════════════════════════

    /** Chilled — slowness leve, deepens com stacks. */
    public static class ChilledEffect extends MobEffect {
        public ChilledEffect() {
            super(MobEffectCategory.HARMFUL, 0xFF66CCFF);
        }
        @Override
        public void applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amp) {
            // Slowness escalado pelo amplifier
            entity.getAttributes().getInstance(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            // Trigger sinergia: amp >= 5 promove a Frostbite
            if (amp >= 5 && !entity.hasEffect(SynergyEffects.FROSTBITE.get())) {
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        SynergyEffects.FROSTBITE.get(), 200, 0, true, true));
                entity.removeEffect(this);
            }
        }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 20 == 0; }
    }

    /** Frostbite — DOT de hipotermia. */
    public static class FrostbiteEffect extends MobEffect {
        public FrostbiteEffect() {
            super(MobEffectCategory.HARMFUL, 0xFF99DDFF);
        }
        @Override
        public void applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amp) {
            entity.hurt(entity.damageSources().freeze(), 1.5F + amp * 0.5F);
        }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 30 == 0; }
    }

    /** Burning Mark — flag de que tomou fogo recentemente. Sem efeito direto, só track. */
    public static class BurningMarkEffect extends MobEffect {
        public BurningMarkEffect() {
            super(MobEffectCategory.HARMFUL, 0xFFFF6633);
        }
        @Override
        public void applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amp) {
            if (entity.getRemainingFireTicks() > 0 && amp >= 2) {
                // Já estava queimando + recebeu fogo de novo = IMMOLATE
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        SynergyEffects.IMMOLATE.get(), 60, 0, true, true));
                entity.removeEffect(this);
            }
        }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 10 == 0; }
    }

    /** Immolate — explosão pequena ao expirar. */
    public static class ImmolateEffect extends MobEffect {
        public ImmolateEffect() {
            super(MobEffectCategory.HARMFUL, 0xFFFF3300);
        }
        @Override
        public void applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amp) {
            entity.setRemainingFireTicks(60);
            if (entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                        entity.getX(), entity.getY() + 1, entity.getZ(),
                        15, 0.5, 0.5, 0.5, 0.05);
            }
            // Pop-off final: explode pequena ao expirar
            int remaining = entity.getEffect(this).getDuration();
            if (remaining < 5) {
                entity.level().explode(null, entity.getX(), entity.getY(), entity.getZ(),
                        1.5F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            }
        }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 10 == 0; }
    }

    /** Bleed — DOT leve, escala em low HP. */
    public static class BleedEffect extends MobEffect {
        public BleedEffect() {
            super(MobEffectCategory.HARMFUL, 0xFF990033);
        }
        @Override
        public void applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amp) {
            entity.hurt(entity.damageSources().magic(), 1.0F + amp * 0.5F);
            // Trigger Hemorrhage em low HP
            if (entity.getHealth() < entity.getMaxHealth() * 0.3F
                    && !entity.hasEffect(SynergyEffects.HEMORRHAGE.get())) {
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        SynergyEffects.HEMORRHAGE.get(), 120, 1, true, true));
                entity.removeEffect(this);
            }
        }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 25 == 0; }
    }

    /** Hemorrhage — DOT massivo. */
    public static class HemorrhageEffect extends MobEffect {
        public HemorrhageEffect() {
            super(MobEffectCategory.HARMFUL, 0xFFCC0033);
        }
        @Override
        public void applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amp) {
            entity.hurt(entity.damageSources().magic(), 3.0F + amp * 1.5F);
            // Particles de sangue
            if (entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR,
                        entity.getX(), entity.getY() + 1, entity.getZ(),
                        5, 0.3, 0.5, 0.3, 0.02);
            }
        }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 15 == 0; }
    }

    /** Static Charge — stacks contam ataques. 3+ stacks promove Storm Mark. */
    public static class StaticChargeEffect extends MobEffect {
        public StaticChargeEffect() {
            super(MobEffectCategory.HARMFUL, 0xFFFFFF44);
        }
        @Override
        public void applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amp) {
            if (amp >= 2 && !entity.hasEffect(SynergyEffects.STORM_MARK.get())) {
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        SynergyEffects.STORM_MARK.get(), 100, 0, true, true));
                entity.removeEffect(this);
            }
        }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 40 == 0; }
    }

    /** Storm Mark — próximo hit chama lightning. */
    public static class StormMarkEffect extends MobEffect {
        public StormMarkEffect() {
            super(MobEffectCategory.HARMFUL, 0xFFAAAAFF);
        }
        @Override
        public void applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amp) {
            // Particles spark
            if (entity.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        entity.getX(), entity.getY() + 1, entity.getZ(),
                        3, 0.3, 0.5, 0.3, 0.05);
            }
        }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 8 == 0; }
    }

    /** Heartstop — para o coração por 2s; se sanity baixa, mata. */
    public static class HeartstopEffect extends MobEffect {
        public HeartstopEffect() {
            super(MobEffectCategory.HARMFUL, 0xFF330033);
        }
        @Override
        public void applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amp) {
            // Slowness extremo
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 5, 250, true, false));
            // Kill se for player com sanity < 20
            if (entity instanceof net.minecraft.world.entity.player.Player p) {
                int sanity = br.com.murilo.liberthia.dimension.SpiritDimension.getSanity(p);
                if (sanity < 20) {
                    entity.hurt(entity.damageSources().magic(), entity.getMaxHealth() * 0.5F);
                }
            }
        }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 5 == 0; }
    }
}
