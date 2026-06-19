package br.com.murilo.liberthia.cosmic.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * <b>Void Flying Eye</b> — partícula CUSTOM (não-vanilla) de um olho vermelho que
 * VOA/flutua, igual à invocação do Underzealot (Alex's Caves). Movimento 100%
 * procedural: sobe em espiral, deriva pra fora, "pisca" ciclando os 4 frames do
 * spritesheet, e some com fade. Render translúcido (alpha das texturas).
 *
 * <p>A velocidade inicial (vx,vy,vz) passada no spawn vira a direção de "fuga" do
 * olho — quem invoca passa um vetor radial pra eles se espalharem.
 */
public class VoidFlyingEyeParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final double cx, cy, cz;        // centro de órbita (spawn)
    private final float startAngle;
    private final float angularSpeed;
    private final float driftOut;           // quão rápido afasta do centro
    private final float riseSpeed;

    public VoidFlyingEyeParticle(ClientLevel level, double x, double y, double z,
                                 double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.sprites = sprites;
        this.cx = x; this.cy = y; this.cz = z;
        // ângulo inicial vem da direção do vetor (pra espalhar uniforme)
        this.startAngle = (float) Math.atan2(vz, vx) + (level.random.nextFloat() - 0.5F) * 0.6F;
        this.angularSpeed = (0.06F + level.random.nextFloat() * 0.06F) * (level.random.nextBoolean() ? 1F : -1F);
        this.driftOut = 0.018F + level.random.nextFloat() * 0.02F;
        this.riseSpeed = 0.012F + level.random.nextFloat() * 0.018F;

        this.lifetime = 45 + level.random.nextInt(35);   // ~2-4s, bem mais longo que vanilla
        this.gravity = 0;
        this.hasPhysics = false;
        this.alpha = 0F;
        this.quadSize = 0.32F + level.random.nextFloat() * 0.18F; // olhos variados
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;
        if (this.age++ >= this.lifetime) { this.remove(); return; }

        // "pisca"/anima ciclando os frames pela idade
        this.setSpriteFromAge(sprites);

        float t = (float) this.age / this.lifetime;
        // espiral pra fora + sobe (voo do olho)
        float ang = startAngle + this.age * angularSpeed;
        float radius = 0.3F + this.age * driftOut;
        this.x = cx + Math.cos(ang) * radius;
        this.z = cz + Math.sin(ang) * radius;
        this.y = cy + this.age * riseSpeed + Math.sin(this.age * 0.2F) * 0.08F; // bob vertical

        // alpha: fade-in 15% → cheio → fade-out 30%
        if (t < 0.15F) this.alpha = t / 0.15F;
        else if (t > 0.7F) this.alpha = Math.max(0F, (1F - t) / 0.3F);
        else this.alpha = 1F;

        // leve pulsar de tamanho (olho "respira")
        this.quadSize = (0.32F + 0.10F * (float) Math.sin(this.age * 0.35F)) + t * 0.15F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double vx, double vy, double vz) {
            return new VoidFlyingEyeParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
