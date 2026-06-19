package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.event.PossessionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * v0.1.49: C2S — possessor manda "quebra esse bloco" (left click sustentado
 * em block hit result). Server destrói o bloco se:
 * <ul>
 *   <li>O possessor tem possessão ativa.</li>
 *   <li>O bloco está a ≤ 6 blocos da entity possuída (anti-cheese).</li>
 *   <li>O bloco não é indestrutível (hardness ≥ 0).</li>
 * </ul>
 *
 * <p>Drop atribuído ao possessor (pra XP/advancements). Bloco quebrado pela
 * entity possuída visualmente — gera particles + sound vanilla.
 *
 * <p>Pra V1 é <b>instant break</b> em vez de mining progressivo (que requer
 * sync de progresso por tick). Aceitável porque o possessor já é "OP" — quebra
 * stone/dirt 1 click. Limite: hardness máxima 5.0 (impede quebrar obsidiana
 * sem ferramenta apropriada).
 */
public class PossessionBreakBlockC2SPacket {

    private static final double MAX_REACH = 6.0;
    private static final float MAX_HARDNESS = 5.0f;

    private final BlockPos pos;

    public PossessionBreakBlockC2SPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(PossessionBreakBlockC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
    }

    public static PossessionBreakBlockC2SPacket decode(FriendlyByteBuf buf) {
        return new PossessionBreakBlockC2SPacket(buf.readBlockPos());
    }

    public static void handle(PossessionBreakBlockC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer possessor = context.getSender();
            if (possessor == null) return;
            // r21: cached entity + rate-limited recovery.
            br.com.murilo.liberthia.event.PossessionSession session =
                    PossessionManager.getSession(possessor.getUUID());
            if (session == null) {
                PossessionManager.requestClientRecoverySync(possessor);
                return;
            }
            Entity possessed = session.resolveTarget(possessor.server);
            if (!(possessed instanceof LivingEntity entity)) {
                PossessionManager.requestClientRecoverySync(possessor);
                return;
            }
            if (!(possessed.level() instanceof ServerLevel level)) return;

            // Reach check: bloco a ≤ MAX_REACH blocos da entidade possuída
            Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() / 2, 0);
            Vec3 blockCenter = Vec3.atCenterOf(pkt.pos);
            if (entityCenter.distanceTo(blockCenter) > MAX_REACH) return;

            // Hardness check — impede quebrar bedrock/obsidiana sem tool
            BlockState state = level.getBlockState(pkt.pos);
            if (state.isAir()) return;
            float hardness = state.getDestroySpeed(level, pkt.pos);
            if (hardness < 0) return; // indestrutível (bedrock)
            if (hardness > MAX_HARDNESS) return;

            // Destrói o bloco com drops, atribui XP ao possessor
            level.destroyBlock(pkt.pos, true, possessor);
        });
        context.setPacketHandled(true);
    }
}
