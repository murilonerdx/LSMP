package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.event.PossessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S: o possessor manda ataque. Server faz o entity possuído atacar
 * {@code victimEntityId}.
 *
 * <p>Se possessed é Player → {@code attack(victim)}. Se é Mob →
 * {@code doHurtTarget(victim)} usando os atributos vanilla do mob.
 */
public class PossessionAttackC2SPacket {

    private final int victimEntityId;

    public PossessionAttackC2SPacket(int victimEntityId) {
        this.victimEntityId = victimEntityId;
    }

    public static void encode(PossessionAttackC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.victimEntityId);
    }

    public static PossessionAttackC2SPacket decode(FriendlyByteBuf buf) {
        return new PossessionAttackC2SPacket(buf.readVarInt());
    }

    public static void handle(PossessionAttackC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            // r21: usa Session cached. Recovery rate-limitado pra evitar flood.
            br.com.murilo.liberthia.event.PossessionSession session =
                    PossessionManager.getSession(sender.getUUID());
            if (session == null) {
                PossessionManager.requestClientRecoverySync(sender);
                return;
            }
            Entity possessed = session.resolveTarget(sender.server);
            if (!(possessed instanceof LivingEntity attacker)) {
                PossessionManager.requestClientRecoverySync(sender);
                return;
            }
            if (!(possessed.level() instanceof ServerLevel level)) return;

            Entity victim = level.getEntity(pkt.victimEntityId);
            if (!(victim instanceof LivingEntity living)) return;
            if (victim.is(possessed)) return;

            // Distância máxima 4 blocos (vanilla reach com tolerância de network).
            double distSq = possessed.distanceToSqr(victim);
            if (distSq > 4.5 * 4.5) return;

            // v0.1.22 r11: ativa bypass do nosso AttackEntityEvent handler
            // (que cancela ataques de possuídos por padrão). Sem isso o
            // playerAttacker.attack() seria cancelado e o possessor não
            // conseguiria bater em nada via possessão.
            PossessionManager.setAttackBypass(attacker.getUUID(), true);
            try {
                if (attacker instanceof Player playerAttacker) {
                    playerAttacker.attack(living);
                } else if (attacker instanceof Mob mobAttacker) {
                    mobAttacker.doHurtTarget(living);
                }
            } finally {
                PossessionManager.setAttackBypass(attacker.getUUID(), false);
            }
        });
        context.setPacketHandled(true);
    }
}
