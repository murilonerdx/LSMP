package br.com.murilo.liberthia.magic.effect.custom;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * r151: <b>15 Custom Mob Effects</b> — magia + cosmic horror.
 *
 * <p>Todos extendem {@link SimpleEffect} ou {@link TickingEffect} pra reduzir boilerplate.
 *
 * <h2>Cosmic Horror (5)</h2>
 * <ol>
 *   <li>VOID_TOUCH — drena HP 1/2s</li>
 *   <li>WHISPERS — teleporte aleatório a cada 8s + confusion</li>
 *   <li>STATIC_VISION — flashes de blindness</li>
 *   <li>TIME_FRACTURE — slowness aleatório intermitente</li>
 *   <li>HOLLOW_HUNGER — drena saturação 5x mais rápido</li>
 * </ol>
 *
 * <h2>Magic (10)</h2>
 * <ol start="6">
 *   <li>AETHERIC_SHIFT — recebe 50% menos dano</li>
 *   <li>SOUL_BLEED — DOT que ignora armadura</li>
 *   <li>MANA_SURGE — buff de spell dmg (lido via SourceData)</li>
 *   <li>ARCANE_WARD — resistance magic</li>
 *   <li>ASTRAL_SIGHT — glowing em volta</li>
 *   <li>ELEMENTAL_RESONANCE — boost a elementos</li>
 *   <li>MIND_FORTRESS — limpa nausea/confusion/blindness sempre</li>
 *   <li>CHRONOSURGE — speed III + jump + haste</li>
 *   <li>VOID_ARMOR — armor cresce com tempo</li>
 *   <li>DRAGON_BREATH — fire damage em mobs adjacentes</li>
 * </ol>
 */
public final class CustomMagicEffects {

    private CustomMagicEffects() {}

    // ─── Base classes ───────────────────────────────────────────

    /** Effect simples — sem tick, só serve como flag (lido por outros sistemas). */
    public static class SimpleEffect extends MobEffect {
        public SimpleEffect(MobEffectCategory cat, int color) { super(cat, color); }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return false; }
    }

    /** Effect que tickea a cada N ticks com lambda. */
    public static class TickingEffect extends MobEffect {
        private final int tickInterval;
        private final TickAction action;
        public TickingEffect(MobEffectCategory cat, int color, int tickInterval, TickAction action) {
            super(cat, color);
            this.tickInterval = tickInterval;
            this.action = action;
        }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) {
            return duration % tickInterval == 0;
        }
        @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
            try { action.tick(entity, amplifier); }
            catch (Throwable ignored) {}
        }
        @FunctionalInterface
        public interface TickAction { void tick(LivingEntity e, int amp); }
    }

    // ─── 1. VOID_TOUCH ─────────────────────────────────────────
    public static class VoidTouchEffect extends TickingEffect {
        public VoidTouchEffect() {
            super(MobEffectCategory.HARMFUL, 0x1a0a2e, 40, (e, amp) -> {
                e.hurt(e.damageSources().magic(), 1.0F + amp * 0.5F);
            });
        }
    }

    // ─── 2. WHISPERS — teleporte aleatório curto ────────────────
    public static class WhispersEffect extends TickingEffect {
        public WhispersEffect() {
            super(MobEffectCategory.HARMFUL, 0x6a4a8a, 160, (e, amp) -> {
                if (e.level().isClientSide) return;
                double dx = (e.getRandom().nextDouble() - 0.5) * 8;
                double dz = (e.getRandom().nextDouble() - 0.5) * 8;
                e.randomTeleport(e.getX() + dx, e.getY(), e.getZ() + dz, true);
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, amp));
            });
        }
    }

    // ─── 3. STATIC_VISION — blindness pulse ────────────────────
    public static class StaticVisionEffect extends TickingEffect {
        public StaticVisionEffect() {
            super(MobEffectCategory.HARMFUL, 0x888899, 80, (e, amp) -> {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30 + amp * 15, 0));
            });
        }
    }

    // ─── 4. TIME_FRACTURE — slowness aleatório ─────────────────
    public static class TimeFractureEffect extends TickingEffect {
        public TimeFractureEffect() {
            super(MobEffectCategory.HARMFUL, 0x4a4a6a, 60, (e, amp) -> {
                if (e.getRandom().nextFloat() < 0.4F) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2 + amp));
                }
            });
        }
    }

    // ─── 5. HOLLOW_HUNGER — drena fome rápido ──────────────────
    public static class HollowHungerEffect extends TickingEffect {
        public HollowHungerEffect() {
            super(MobEffectCategory.HARMFUL, 0x6a5a3a, 20, (e, amp) -> {
                if (e instanceof Player player) {
                    player.causeFoodExhaustion(0.3F * (amp + 1));
                }
            });
        }
    }

    // ─── 6. AETHERIC_SHIFT — flag-only (lido em hurt event) ─────
    public static class AethericShiftEffect extends SimpleEffect {
        public AethericShiftEffect() { super(MobEffectCategory.BENEFICIAL, 0xaaccff); }
    }

    // ─── 7. SOUL_BLEED — DOT ignora armadura ────────────────────
    public static class SoulBleedEffect extends TickingEffect {
        public SoulBleedEffect() {
            super(MobEffectCategory.HARMFUL, 0xa00050, 30, (e, amp) -> {
                e.invulnerableTime = 0; // bypass i-frames
                e.hurt(e.damageSources().magic(), 1.5F + amp);
            });
        }
    }

    // ─── 8. MANA_SURGE — flag (lido por spell items) ────────────
    public static class ManaSurgeEffect extends SimpleEffect {
        public ManaSurgeEffect() { super(MobEffectCategory.BENEFICIAL, 0x00ccff); }
    }

    // ─── 9. ARCANE_WARD — resistance magic via aplica Resistance ─
    public static class ArcaneWardEffect extends TickingEffect {
        public ArcaneWardEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x9966ff, 100, (e, amp) -> {
                e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 110, 1 + amp));
            });
        }
    }

    // ─── 10. ASTRAL_SIGHT — auto-glowing aliados próximos ──────
    public static class AstralSightEffect extends TickingEffect {
        public AstralSightEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xffff99, 100, (e, amp) -> {
                for (LivingEntity other : e.level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(e.getX() - 16, e.getY() - 8, e.getZ() - 16,
                                 e.getX() + 16, e.getY() + 8, e.getZ() + 16))) {
                    if (other != e) {
                        other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
                    }
                }
            });
        }
    }

    // ─── 11. ELEMENTAL_RESONANCE — flag ─────────────────────────
    public static class ElementalResonanceEffect extends SimpleEffect {
        public ElementalResonanceEffect() { super(MobEffectCategory.BENEFICIAL, 0xff8844); }
    }

    // ─── 12. MIND_FORTRESS — limpa debuffs mentais ─────────────
    public static class MindFortressEffect extends TickingEffect {
        public MindFortressEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x88ddaa, 20, (e, amp) -> {
                e.removeEffect(MobEffects.CONFUSION);
                e.removeEffect(MobEffects.BLINDNESS);
                e.removeEffect(MobEffects.WEAKNESS);
            });
        }
    }

    // ─── 13. CHRONOSURGE — speed + jump + haste ─────────────────
    public static class ChronosurgeEffect extends TickingEffect {
        public ChronosurgeEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xffaa44, 40, (e, amp) -> {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 2 + amp));
                e.addEffect(new MobEffectInstance(MobEffects.JUMP, 50, 1 + amp));
                e.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 50, 2 + amp));
            });
        }
    }

    // ─── 14. VOID_ARMOR — absorção crescente ────────────────────
    public static class VoidArmorEffect extends TickingEffect {
        public VoidArmorEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x2a0a4a, 100, (e, amp) -> {
                e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 110, amp + 1));
            });
        }
    }

    // ─── 15. DRAGON_BREATH — passive fire AOE ──────────────────
    public static class DragonBreathEffect extends TickingEffect {
        public DragonBreathEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xff5500, 40, (e, amp) -> {
                for (LivingEntity other : e.level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(e.getX() - 3, e.getY() - 2, e.getZ() - 3,
                                 e.getX() + 3, e.getY() + 2, e.getZ() + 3))) {
                    if (other != e && other != e.getControllingPassenger()) {
                        other.hurt(e.damageSources().inFire(), 1.0F + amp);
                        other.setSecondsOnFire(2);
                    }
                }
            });
        }
    }
}
