package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.magic.school.SpellSchool;
import br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData;
import br.com.murilo.liberthia.particle.engine.ConfigurableParticleOptions;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

/**
 * r146: Aura de carregamento spawnada na MÃO do player durante o channel
 * de uma spell. Inspirado no efeito do Iron's Spells de cast charging —
 * antes do projétil sair, partículas orbitando o pulso já avisam o spell
 * que está chegando.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>Spawn em volta da mão do player segurando o item</li>
 *   <li>Densidade cresce com o ticksUsed (0 → max)</li>
 *   <li>Cor segue a escola do feitiço</li>
 *   <li>Mistura SpellTrail (animated spritesheet) + ENGINE emissive sparks</li>
 *   <li>Orbital motion — partículas giram em volta da mão em vez de subir reto</li>
 * </ul>
 */
public final class SpellChargeAura {

    private SpellChargeAura() {}

    /** Spawna 1 tick de aura na mão do player. */
    public static void tick(ServerPlayer player, SpellSchool school,
                            int ticksUsed, int totalDuration,
                            InteractionHand hand) {
        ServerLevel sl = player.serverLevel();
        if (sl == null) return;

        // Charge ratio 0..1 (afetando densidade e tamanho)
        float ratio = totalDuration <= 0 ? 1F : Math.min(1F, ticksUsed / (float) totalDuration);

        // Posição da mão — aproxima do pulso do player no espaço do mundo
        Vec3 handPos = getHandPos(player, hand);

        // ─── Camada 1: SpellTrail orbital (3 partículas/tick) ──────────
        // Orbita em volta da mão num círculo horizontal pequeno
        double orbitRadius = 0.18 + ratio * 0.12; // 0.18 → 0.30
        double angularSpeed = 0.4; // rad/tick
        double baseAngle = (player.tickCount * angularSpeed) % (Math.PI * 2);

        SpellTrailParticleData orbital = new SpellTrailParticleData(
                school, 0.45F + ratio * 0.25F, 10);

        for (int i = 0; i < 3; i++) {
            double a = baseAngle + (Math.PI * 2 / 3) * i;
            double dx = Math.cos(a) * orbitRadius;
            double dz = Math.sin(a) * orbitRadius;
            // Velocidade tangencial leve (não-zero pra não morrer parada)
            double vx = -Math.sin(a) * 0.02;
            double vz = Math.cos(a) * 0.02;
            sl.sendParticles(orbital,
                    handPos.x + dx, handPos.y, handPos.z + dz,
                    0, vx, 0.01, vz, 0.0);
        }

        // ─── Camada 2: ENGINE emissive (chispas no centro da mão) ─────
        int hex = school.colorHex();
        float r = ((hex >> 16) & 0xFF) / 255F;
        float g = ((hex >> 8) & 0xFF) / 255F;
        float b = (hex & 0xFF) / 255F;
        ConfigurableParticleOptions core = new ConfigurableParticleOptions(
                ModParticles.ENGINE_PARTICLE.get(),
                Math.min(1F, r + 0.15F),
                Math.min(1F, g + 0.15F),
                Math.min(1F, b + 0.15F),
                0.85F,
                0.18F + ratio * 0.18F, 0.0F,
                8,
                0.0F, 0.92F,
                0.7F,
                false, true, true);
        // Densidade cresce com charge: 1 → 3 chispas/tick
        int sparkCount = 1 + (int)(ratio * 2);
        sl.sendParticles(core,
                handPos.x, handPos.y, handPos.z,
                sparkCount,
                0.06, 0.06, 0.06,
                0.015);

        // ─── Camada 3: tick final (charge complete) — flash de prévia ───
        if (ticksUsed == totalDuration - 1) {
            // Quase pronto — pulse maior pra avisar "vai sair AGORA"
            SpellTrailParticleData flash = new SpellTrailParticleData(
                    school, 1.8F, 8);
            sl.sendParticles(flash,
                    handPos.x, handPos.y, handPos.z,
                    8, 0.15, 0.15, 0.15, 0.04);
        }

        // ─── Camada 4: vanilla complementar por escola (cheap backdrop) ─
        if (player.tickCount % 3 == 0) {
            net.minecraft.core.particles.ParticleOptions vanilla = switch (school) {
                case FIRE -> ParticleTypes.FLAME;
                case ICE -> ParticleTypes.SNOWFLAKE;
                case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
                case BLOOD -> ParticleTypes.DAMAGE_INDICATOR;
                case ELDRITCH -> ParticleTypes.PORTAL;
                case HOLY -> ParticleTypes.END_ROD;
                case NATURE -> ParticleTypes.HAPPY_VILLAGER;
            };
            sl.sendParticles(vanilla,
                    handPos.x, handPos.y, handPos.z,
                    1, 0.08, 0.08, 0.08, 0.0);
        }
    }

    /**
     * Calcula posição aproximada da mão do player no espaço do mundo.
     * Usa offset baseado em yaw + arm side (LEFT/RIGHT).
     */
    private static Vec3 getHandPos(ServerPlayer player, InteractionHand hand) {
        // Vetor lateral do player (perpendicular ao look)
        float yaw = player.getYRot();
        double yawRad = Math.toRadians(yaw);
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);
        // Vetor frente
        double frontX = -Math.sin(yawRad);
        double frontZ = Math.cos(yawRad);

        // Mão dominante do player (vanilla = RIGHT por default)
        HumanoidArm dominant = player.getMainArm();
        boolean isMain = hand == InteractionHand.MAIN_HAND;
        // Se segurando off-hand, lado contrário
        HumanoidArm armSide = isMain ? dominant
                : (dominant == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT);

        double lateralOffset = armSide == HumanoidArm.RIGHT ? 0.35 : -0.35;
        double frontOffset = 0.45;

        return new Vec3(
                player.getX() + rightX * lateralOffset + frontX * frontOffset,
                player.getY() + 1.25,
                player.getZ() + rightZ * lateralOffset + frontZ * frontOffset
        );
    }
}
