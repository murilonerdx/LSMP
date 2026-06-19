package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.event.PossessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S: cliente do possessor manda inputs WASD/jump/sneak + olhar (yaw/pitch).
 * Server aplica diretamente no entity possuído:
 * <ul>
 *   <li>setDeltaMovement com vetor relativo ao yaw</li>
 *   <li>setYRot / setXRot</li>
 *   <li>jump: setDeltaMovement.y = 0.42 se estiver onGround</li>
 * </ul>
 *
 * <p>Server-side a gente confia que o possessor é dono legitimado da posse
 * (checado em PossessionManager). Se não for, ignora o packet silenciosamente.
 *
 * <p><b>v0.1.45 — FIX bug físico (user: "atravessam a terra e quando pulam saem
 * voando"):</b> antes a gente fazia {@code target.setPos(newX, newY, newZ)} e
 * {@code sp.connection.teleport(...)} aplicando o delta direto na posição, o que
 * <i>ignorava colisão de blocos</i> e <i>não deixava gravidade vanilla atuar</i>.
 * Agora a gente <b>SÓ seta velocidade</b> (setDeltaMovement) — o physics tick
 * do servidor aplica colisão {@code Entity#move(MoverType.SELF, delta)} +
 * gravidade automaticamente, então a entity:
 * <ul>
 *   <li>Cai naturalmente quando sai do chão</li>
 *   <li>Bate em paredes / não atravessa blocos</li>
 *   <li>Pula corretamente (0.42 = altura padrão de 1.25 blocos)</li>
 * </ul>
 */
public class PossessionMoveC2SPacket {

    // r30 SPEED FIX: era 0.21, mas depois passava por accel*friction novamente
    // → velocidade efetiva ficava em ~0.05/tick (1 m/s — bem mais lento que
    // player vanilla 4.3 m/s). Agora usamos velocidade DIRETA = player-like.
    private static final double WALK_SPEED = 0.215;     // m/tick — vanilla player walk
    private static final double SPRINT_SPEED = 0.28;    // segurando forward
    private static final double SNEAK_SPEED = 0.065;

    private final float forward;
    private final float strafe;
    private final boolean jump;
    private final boolean sneak;
    private final float yaw;
    private final float pitch;

    public PossessionMoveC2SPacket(float forward, float strafe, boolean jump, boolean sneak,
                                   float yaw, float pitch) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static void encode(PossessionMoveC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeFloat(pkt.forward);
        buf.writeFloat(pkt.strafe);
        buf.writeBoolean(pkt.jump);
        buf.writeBoolean(pkt.sneak);
        buf.writeFloat(pkt.yaw);
        buf.writeFloat(pkt.pitch);
    }

    public static PossessionMoveC2SPacket decode(FriendlyByteBuf buf) {
        return new PossessionMoveC2SPacket(
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public static void handle(PossessionMoveC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            // r21: usa Session com cache. Antes findEntity iterava ALL levels a
            // cada packet (20×/s). Agora é O(1) via cache da sessão.
            br.com.murilo.liberthia.event.PossessionSession session =
                    br.com.murilo.liberthia.event.PossessionManager.getSession(sender.getUUID());
            if (session == null) {
                // State desync — rate-limited recovery (max 1×/5s, antes era
                // 20×/s = FLOOD que travava o server thread).
                br.com.murilo.liberthia.event.PossessionManager.requestClientRecoverySync(sender);
                return;
            }
            // r21: input buffering — atualiza session com clamp pra evitar
            // valores impossíveis (anti-cheat suave + protege física).
            session.setInput(pkt.forward, pkt.strafe, pkt.jump, pkt.sneak, pkt.yaw, pkt.pitch);

            Entity entity = session.resolveTarget(sender.server);
            if (!(entity instanceof LivingEntity target)) {
                // Target sumiu — sinaliza recovery (rate-limitado)
                br.com.murilo.liberthia.event.PossessionManager.requestClientRecoverySync(sender);
                return;
            }

            // Aplica orientação primeiro — vetor de movimento depende do yaw.
            target.setYRot(pkt.yaw);
            target.setXRot(pkt.pitch);
            target.yHeadRot = pkt.yaw;
            target.yBodyRot = pkt.yaw;

            // r21: FAST-PATH — input idle + target no chão = pula física inteira.
            // Antes calculava friction/gravidade/collision pra todo packet
            // (20×/s) mesmo quando target estava completamente parado.
            // Economia gigantesca em servidor com vários possessors idle.
            boolean idle = pkt.forward == 0f && pkt.strafe == 0f && !pkt.jump;
            if (idle && target.onGround()
                    && target.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
                // Ainda envia sync de rotação pro ServerPlayer locked, mas
                // não roda física. Player parado fica visualmente correto.
                if (target instanceof ServerPlayer sp) {
                    sp.connection.teleport(
                            sp.getX(), sp.getY(), sp.getZ(),
                            pkt.yaw, pkt.pitch,
                            java.util.EnumSet.of(
                                    net.minecraft.world.entity.RelativeMovement.X,
                                    net.minecraft.world.entity.RelativeMovement.Y,
                                    net.minecraft.world.entity.RelativeMovement.Z));
                }
                return; // FAST EXIT
            }

            // Calcula direção horizontal a partir do yaw + inputs locais.
            // Mesma matemática do vanilla Mob#moveRelative.
            double yawRad = Math.toRadians(pkt.yaw);
            double sin = Math.sin(yawRad);
            double cos = Math.cos(yawRad);
            double mx = pkt.strafe * cos - pkt.forward * sin;
            double mz = pkt.strafe * sin + pkt.forward * cos;
            // r30 SPEED FIX: sprint quando segurando forward + não sneak, sneak quando shift.
            double speed;
            if (pkt.sneak) speed = SNEAK_SPEED;
            else if (pkt.forward > 0.5f) speed = SPRINT_SPEED;
            else speed = WALK_SPEED;
            mx *= speed;
            mz *= speed;

            // v0.1.45: MANTÉM velocidade vertical atual — gravidade vanilla
            // (ou nossa gravidade manual abaixo) continua atuando entre ticks
            // (cai quando sai do chão). Antes sobrescrevíamos Y=0 toda vez,
            // então a entity ficava "voando" se estivesse no ar.
            Vec3 cur = target.getDeltaMovement();
            double newY = cur.y;
            boolean canJump = target.onGround() || target.isInWater() || target.isInLava();
            if (pkt.jump && canJump) {
                // 0.42 = altura de pulo vanilla (~1.25 blocos). getJumpBoostPower()
                // adiciona o bônus do enchant/efeito Jump Boost se ativo.
                newY = target.getJumpBoostPower() + 0.42;
                // Sprint-jump boost horizontal: vanilla adiciona 0.2 na direção
                // do walk quando jump+forward simultâneo.
                if (Math.abs(pkt.forward) > 0.01) {
                    double sprintMul = 0.2;
                    mx += -Math.sin(yawRad) * sprintMul * Math.signum(pkt.forward);
                    mz +=  Math.cos(yawRad) * sprintMul * Math.signum(pkt.forward);
                }
            }

            // ⚠ CASO 1: mob com setNoAi(true) (default em PossessionManager.start).
            // Quando NoAi=true, LivingEntity#isEffectiveAi() retorna false, então
            // o vanilla NÃO chama travel(Vec3.ZERO) — isso significa que a entity
            // não pega gravidade nem colisão nem friction automaticamente.
            // Replicamos a fórmula do LivingEntity#travel aqui:
            //   1. Friction horizontal block-aware (slime=0.8, ice=0.989, default=0.6)
            //   2. Gravidade: motionY = (motionY - 0.08) * 0.98 quando no ar
            //   3. Colisão: target.move(MoverType.SELF, motion) — respeita AABB,
            //      step-up de 0.6, e zera componente do motion que bate em parede.
            //
            // v0.1.46: user reportou "pulo e física superficial". Agora a velocidade
            // horizontal acumula corretamente (input + friction da velocidade
            // anterior) em vez de SNAPAR pra zero quando o input solta — assim
            // o mob desacelera suavemente como um mob vanilla, em vez de parar
            // abrupto.
            if (target instanceof Mob mob && mob.isNoAi()) {
                boolean onGround = target.onGround();
                boolean inWater = target.isInWater();
                boolean inLava = target.isInLava();

                // r30 SPEED FIX: aplica velocidade DIRETA (mx, mz já estão na
                // escala de m/tick desejada). Antes multiplicávamos por accel
                // outra vez (0.1626/f³ ≈ 1.0 com friction default) E aplicávamos
                // friction → velocidade ficava em 5-10% do esperado.
                // Agora: velocidade horizontal = SE input ativo: target = (mx, mz),
                // SE input zero: decay suave (95% friction).
                double finalMx, finalMz;
                if (Math.abs(mx) > 0.001 || Math.abs(mz) > 0.001) {
                    finalMx = mx;
                    finalMz = mz;
                } else {
                    // Soltou input — decay rápido (não escorrega tanto)
                    finalMx = cur.x * 0.5;
                    finalMz = cur.z * 0.5;
                }

                // Gravidade vanilla (só no ar/fora d'água)
                double finalMy = newY;
                if (!onGround && !inWater && !inLava) {
                    finalMy = (newY - 0.08) * 0.98;
                }

                target.setDeltaMovement(finalMx, finalMy, finalMz);
                target.hasImpulse = true;
                target.move(MoverType.SELF, target.getDeltaMovement());
            }
            // CASO 2: ServerPlayer possuído OU mob sem NoAi.
            else {
                // r30: aplica velocidade DIRETA quando tem input, decay quando solta.
                double finalMx, finalMz;
                if (Math.abs(mx) > 0.001 || Math.abs(mz) > 0.001) {
                    finalMx = mx;
                    finalMz = mz;
                } else {
                    finalMx = cur.x * 0.5;
                    finalMz = cur.z * 0.5;
                }
                target.setDeltaMovement(finalMx, newY, finalMz);
                target.hasImpulse = true;
            }

            if (target instanceof ServerPlayer sp) {
                // Sync motion (não pos) pro client.
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));

                // v0.1.22 r8: FORÇA ROTATION no client do player possuído. User
                // pediu: "quando ele me possui não estou olhando para os lugares
                // que ele olha". Antes server setava setYRot/setXRot mas o client
                // local do target ignorava (input próprio dele tinha precedência).
                // Solução: usar connection.teleport com flags relativas X/Y/Z =
                // delta 0 (não move) + yaw/pitch absolutos (gira). Cliente recebe
                // ClientboundPlayerPositionPacket e atualiza só a rotação.
                sp.connection.teleport(
                        sp.getX(), sp.getY(), sp.getZ(),
                        pkt.yaw, pkt.pitch,
                        java.util.EnumSet.of(
                                net.minecraft.world.entity.RelativeMovement.X,
                                net.minecraft.world.entity.RelativeMovement.Y,
                                net.minecraft.world.entity.RelativeMovement.Z));
            }
        });
        context.setPacketHandled(true);
    }
}
