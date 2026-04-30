package br.com.murilo.liberthia.effect;

import br.com.murilo.liberthia.particle.engine.ConfigurableParticleOptions;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParticleEffectConfig {

    private final boolean colorTintEnabled;

    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    private final float startSize;
    private final float endSize;

    private final int lifetime;
    private final int count;
    private final int cooldownTicks;

    private final float gravity;
    private final float friction;
    private final float spinSpeed;

    private final boolean hasPhysics;
    private final boolean emissive;

    private final double speed;
    private final double upwardSpeed;
    private final double spread;
    private final double range;
    private final double damageRadius;
    private final double knockback;

    private final float damage;

    private final SoundEvent sound;
    private final float soundVolume;
    private final float soundPitch;

    private final List<MobEffectInstance> effects;

    private ParticleEffectConfig(Builder builder) {
        this.colorTintEnabled = builder.colorTintEnabled;

        this.red = builder.red;
        this.green = builder.green;
        this.blue = builder.blue;
        this.alpha = builder.alpha;

        this.startSize = builder.startSize;
        this.endSize = builder.endSize;

        this.lifetime = builder.lifetime;
        this.count = builder.count;
        this.cooldownTicks = builder.cooldownTicks;

        this.gravity = builder.gravity;
        this.friction = builder.friction;
        this.spinSpeed = builder.spinSpeed;

        this.hasPhysics = builder.hasPhysics;
        this.emissive = builder.emissive;

        this.speed = builder.speed;
        this.upwardSpeed = builder.upwardSpeed;
        this.spread = builder.spread;
        this.range = builder.range;
        this.damageRadius = builder.damageRadius;
        this.knockback = builder.knockback;

        this.damage = builder.damage;

        this.sound = builder.sound;
        this.soundVolume = builder.soundVolume;
        this.soundPitch = builder.soundPitch;

        this.effects = Collections.unmodifiableList(new ArrayList<>(builder.effects));
    }

    public ConfigurableParticleOptions toParticleOptions() {
        return new ConfigurableParticleOptions(
                ModParticles.ENGINE_PARTICLE.get(),
                this.red,
                this.green,
                this.blue,
                this.alpha,
                this.startSize,
                this.endSize,
                this.lifetime,
                this.gravity,
                this.friction,
                this.spinSpeed,
                this.hasPhysics,
                this.emissive,
                this.colorTintEnabled
        );
    }

    public boolean colorTintEnabled() {
        return this.colorTintEnabled;
    }

    public float red() {
        return this.red;
    }

    public float green() {
        return this.green;
    }

    public float blue() {
        return this.blue;
    }

    public float alpha() {
        return this.alpha;
    }

    public float startSize() {
        return this.startSize;
    }

    public float endSize() {
        return this.endSize;
    }

    public int lifetime() {
        return this.lifetime;
    }

    public int count() {
        return this.count;
    }

    public int cooldownTicks() {
        return this.cooldownTicks;
    }

    public float gravity() {
        return this.gravity;
    }

    public float friction() {
        return this.friction;
    }

    public float spinSpeed() {
        return this.spinSpeed;
    }

    public boolean hasPhysics() {
        return this.hasPhysics;
    }

    public boolean emissive() {
        return this.emissive;
    }

    public double speed() {
        return this.speed;
    }

    public double upwardSpeed() {
        return this.upwardSpeed;
    }

    public double spread() {
        return this.spread;
    }

    public double range() {
        return this.range;
    }

    public double damageRadius() {
        return this.damageRadius;
    }

    public double knockback() {
        return this.knockback;
    }

    public float damage() {
        return this.damage;
    }

    public SoundEvent sound() {
        return this.sound;
    }

    public float soundVolume() {
        return this.soundVolume;
    }

    public float soundPitch() {
        return this.soundPitch;
    }

    public List<MobEffectInstance> effects() {
        return this.effects;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        /*
         * false por padrão:
         * se você NÃO chamar .color(...), a partícula usa a cor original do PNG.
         */
        private boolean colorTintEnabled = false;

        /*
         * Branco puro.
         * Mesmo se colorTintEnabled fosse true, branco preservaria o sprite.
         */
        private float red = 1.0F;
        private float green = 1.0F;
        private float blue = 1.0F;
        private float alpha = 1.0F;

        private float startSize = 0.15F;
        private float endSize = 0.55F;

        private int lifetime = 24;
        private int count = 40;
        private int cooldownTicks = 10;

        private float gravity = 0.0F;
        private float friction = 0.90F;
        private float spinSpeed = 0.08F;

        private boolean hasPhysics = true;
        private boolean emissive = true;

        private double speed = 0.05D;
        private double upwardSpeed = 0.02D;
        private double spread = 0.35D;
        private double range = 4.0D;
        private double damageRadius = 1.0D;
        private double knockback = 0.0D;

        private float damage = 0.0F;

        private SoundEvent sound = null;
        private float soundVolume = 1.0F;
        private float soundPitch = 1.0F;

        private final List<MobEffectInstance> effects = new ArrayList<>();

        private Builder() {
        }

        /**
         * Chamar este método ATIVA tint.
         * A cor do PNG será multiplicada por RGB.
         */
        public Builder color(float red, float green, float blue, float alpha) {
            this.colorTintEnabled = true;
            this.red = clamp01(red);
            this.green = clamp01(green);
            this.blue = clamp01(blue);
            this.alpha = clamp01(alpha);
            return this;
        }

        /**
         * Só muda transparência, sem tingir a textura.
         * Útil quando você quer respeitar o sprite original, mas controlar alpha.
         */
        public Builder alpha(float alpha) {
            this.alpha = clamp01(alpha);
            return this;
        }

        /**
         * Força usar a cor original do sprite.
         * Mesmo que .color(...) tenha sido chamado antes.
         */
        public Builder useSpriteColor() {
            this.colorTintEnabled = false;
            this.red = 1.0F;
            this.green = 1.0F;
            this.blue = 1.0F;
            return this;
        }

        /**
         * Alias semântico.
         */
        public Builder noColorTint() {
            return this.useSpriteColor();
        }

        public Builder size(float startSize, float endSize) {
            this.startSize = Math.max(0.01F, startSize);
            this.endSize = Math.max(0.01F, endSize);
            return this;
        }

        public Builder lifetime(int lifetime) {
            this.lifetime = Math.max(1, lifetime);
            return this;
        }

        public Builder count(int count) {
            this.count = Math.max(1, count);
            return this;
        }

        public Builder cooldown(int cooldownTicks) {
            this.cooldownTicks = Math.max(0, cooldownTicks);
            return this;
        }

        public Builder gravity(float gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder friction(float friction) {
            this.friction = clamp01(friction);
            return this;
        }

        public Builder spin(float spinSpeed) {
            this.spinSpeed = spinSpeed;
            return this;
        }

        public Builder physics(boolean hasPhysics) {
            this.hasPhysics = hasPhysics;
            return this;
        }

        public Builder emissive(boolean emissive) {
            this.emissive = emissive;
            return this;
        }

        public Builder speed(double speed) {
            this.speed = speed;
            return this;
        }

        public Builder upwardSpeed(double upwardSpeed) {
            this.upwardSpeed = upwardSpeed;
            return this;
        }

        public Builder spread(double spread) {
            this.spread = spread;
            return this;
        }

        public Builder range(double range) {
            this.range = range;
            return this;
        }

        public Builder damage(float damage) {
            this.damage = Math.max(0.0F, damage);
            return this;
        }

        public Builder damageRadius(double damageRadius) {
            this.damageRadius = Math.max(0.0D, damageRadius);
            return this;
        }

        public Builder knockback(double knockback) {
            this.knockback = Math.max(0.0D, knockback);
            return this;
        }

        public Builder sound(SoundEvent sound, float volume, float pitch) {
            this.sound = sound;
            this.soundVolume = volume;
            this.soundPitch = pitch;
            return this;
        }

        public Builder effect(MobEffectInstance effect) {
            this.effects.add(effect);
            return this;
        }

        public ParticleEffectConfig build() {
            return new ParticleEffectConfig(this);
        }

        private static float clamp01(float value) {
            if (value < 0.0F) {
                return 0.0F;
            }

            if (value > 1.0F) {
                return 1.0F;
            }

            return value;
        }
    }
}