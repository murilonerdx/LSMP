package br.com.murilo.liberthia.cosmic.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

/**
 * v0.1.22 r35: <b>Cosmic Orbit Particle</b> — partícula que orbita em volta
 * do ponto de spawn (não fica parada nem vai reta).
 *
 * <h2>Comportamento procedural</h2>
 * <ul>
 *   <li>Spawn position = centro da órbita</li>
 *   <li>A cada tick: calcula nova posição em torno do centro (orbital motion)</li>
 *   <li>Raio varia: 1 → 3 blocks (cresce com tempo)</li>
 *   <li>Alpha: fade in primeiro 20% → full 60% → fade out últimos 20%</li>
 *   <li>Scale: começa 0.3 → cresce até 1.0 no meio → diminui pra 0.5</li>
 *   <li>Cor: rgb 0.4, 0.2, 0.8 (roxo cósmico) com modulação por tempo</li>
 * </ul>
 *
 * <p>Avoid vanilla-style — não usa friction, gravity, ou linear velocity.
 * Movimento 100% procedural via funções trig + lifetime.
 */
public class CosmicOrbitParticle extends TextureSheetParticle {

    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final float startAngle;
    private final float angularSpeed;
    private final float maxRadius;
    private final float orbitTiltX;
    private final float orbitTiltZ;

    public CosmicOrbitParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z);
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
        this.startAngle = (float) (level.random.nextDouble() * Math.PI * 2);
        this.angularSpeed = 0.15F + level.random.nextFloat() * 0.1F;
        this.maxRadius = 1.5F + level.random.nextFloat() * 1.5F;
        this.orbitTiltX = (level.random.nextFloat() - 0.5F) * 0.3F;
        this.orbitTiltZ = (level.random.nextFloat() - 0.5F) * 0.3F;
        // Lifetime — bem mais longo que vanilla
        this.lifetime = 60 + level.random.nextInt(40);
        // Cor base roxo cósmico
        this.rCol = 0.4F + level.random.nextFloat() * 0.3F;
        this.gCol = 0.15F + level.random.nextFloat() * 0.15F;
        this.bCol = 0.7F + level.random.nextFloat() * 0.3F;
        this.alpha = 0.0F; // starts transparent (fade in)
        this.quadSize = 0.3F;
        this.hasPhysics = false; // pure procedural
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float lifeProgress = (float) this.age / this.lifetime;
        // Orbital motion
        float currentAngle = startAngle + (this.age * angularSpeed);
        float currentRadius = maxRadius * (0.3F + lifeProgress * 0.7F);
        this.x = centerX + Math.cos(currentAngle) * currentRadius;
        this.y = centerY + Math.sin(currentAngle * 0.5F) * 0.8F + orbitTiltX * this.age * 0.02F;
        this.z = centerZ + Math.sin(currentAngle) * currentRadius;

        // Alpha curve: fade-in (0-20%) → full (20-80%) → fade-out (80-100%)
        if (lifeProgress < 0.2F) this.alpha = lifeProgress / 0.2F;
        else if (lifeProgress > 0.8F) this.alpha = (1.0F - lifeProgress) / 0.2F;
        else this.alpha = 1.0F;

        // Scale curve: 0.3 → 1.0 (50%) → 0.5
        if (lifeProgress < 0.5F) this.quadSize = 0.3F + (lifeProgress * 1.4F);
        else this.quadSize = 1.0F - ((lifeProgress - 0.5F) * 1.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Provider — usado pelo registry. */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double vx, double vy, double vz) {
            return new CosmicOrbitParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
