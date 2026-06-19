package br.com.murilo.liberthia.cosmic.staredown;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * v0.1.22 r55: <b>Red Eye Stare Manager</b> — quando o player tem baixa
 * sanidade, mobs próximos parecem encarar ele com olhos vermelhos.
 *
 * <h2>Implementação</h2>
 * Como Forge 1.20.1 não tem hook fácil pra mudar texture per-player, usamos
 * <b>particles posicionadas no HEAD do mob</b> visíveis APENAS para o player
 * com baixa sanidade. Outros players próximos NÃO vêem.
 *
 * <p>Particles são red dust 0.2 size colocadas onde os olhos do mob estariam
 * (head + 1.5 do center). 2 particles, lado esquerdo + direito, persistindo
 * o tempo todo que o mob estiver no field of view do player paranóico.
 *
 * <h2>Side-effect: stare</h2>
 * Quando o effect ativo, mob também é forçado a {@code lookAt(player)} se o
 * player não está atacando. Isso reforça a sensação de "todos estão me olhando".
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class RedEyeStareManager {

    private static final double RADIUS = 16.0;
    /** Sanity threshold abaixo do qual o efeito ativa. */
    private static final int SANITY_THRESHOLD = 40;
    /** Vermelho de demônio. */
    private static final DustParticleOptions RED_EYE =
            new DustParticleOptions(new Vector3f(1.0F, 0.05F, 0.0F), 0.45F);

    private RedEyeStareManager() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        if (sp.tickCount % 8 != 0) return; // 2.5Hz

        int sanity = SpiritDimension.getSanity(sp);
        if (sanity > SANITY_THRESHOLD && !SpiritDimension.isInSpiritWorld(sp)) return;

        // r58 FIX: include ALL mobs (animals + monsters) — players próximos NÃO vêem
        for (Mob mob : level.getEntitiesOfClass(Mob.class,
                sp.getBoundingBox().inflate(RADIUS))) {
            try {
                // Posição dos "olhos" — head + offset
                Vec3 head = mob.position().add(0, mob.getEyeHeight(), 0);
                Vec3 look = mob.getLookAngle();
                // Right side (cross com up)
                Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();

                // 2 olhos
                Vec3 leftEye = head.add(right.scale(-0.1)).add(look.scale(0.15));
                Vec3 rightEye = head.add(right.scale(0.1)).add(look.scale(0.15));

                // Envia particles APENAS pro sp via packet ClientboundLevelParticlesPacket
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(
                        RED_EYE, true,
                        leftEye.x, leftEye.y, leftEye.z, 0, 0, 0, 0, 1));
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(
                        RED_EYE, true,
                        rightEye.x, rightEye.y, rightEye.z, 0, 0, 0, 0, 1));

                // r58 FIX: FREEZE — mob fica congelado encarando o player com baixa sanidade
                // Aplica MOVEMENT_SLOWDOWN amp 200 (efetivamente stop) + força lookAt
                if (sanity <= 30 || SpiritDimension.isInSpiritWorld(sp)) {
                    // Sanity bem baixa = freeze pesado
                    mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                            12, 200, true, false));
                    // ZERO delta movement
                    mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
                }
                // Força mob a olhar pro player sempre que está paranoico
                mob.getLookControl().setLookAt(sp, 60F, 60F);

                // Marca NBT — outros sistemas podem inspecionar
                mob.getPersistentData().putBoolean("liberthia.staring_at_paranoid_player", true);
                mob.getPersistentData().putLong("liberthia.staring_until",
                        mob.tickCount + 20L);
            } catch (Throwable t) {
                LiberthiaMod.LOGGER.debug("[RedEye] error: {}", t.toString());
            }
        }
    }
}
