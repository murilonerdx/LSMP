package br.com.murilo.liberthia.freeze;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Implementa a mecânica de "freeze":
 *
 *  • A cada tick, players congelados são teleportados de volta pra posição de
 *    âncora se tiverem se mexido. Mexer = +1 attempt counter.
 *  • Dano cresce com a quantidade de tentativas (proporcional). Decay leve
 *    se ficar parado.
 *  • Action bar permanente "❄ NÃO SE MEXA ❄"
 *  • Partículas de gelo + neve no entorno cada 5 ticks
 *  • TODOS os eventos de interação (right/left click, attack, place, break,
 *    pickup) cancelados.
 *  • Inventário forçado a fechar (containerClosePacket cada 10 ticks).
 *  • Fall damage zerado.
 */
@Mod.EventBusSubscriber(modid = "liberthia")
public final class FreezeEvents {

    private static final Random RNG = new Random();
    /** Dano base por tentativa de movimento. Multiplicado pelo attempts count. */
    private static final float DAMAGE_PER_ATTEMPT = 0.5f;
    /** Distância em blocos pra contar como "tentativa de movimento" (filtra jitter). */
    private static final double MOVE_THRESHOLD_SQ = 0.0025; // 0.05 bloco
    /** Decay de attempts: depois de N ticks sem mexer, decrementa 1. */
    private static final int ATTEMPT_DECAY_TICKS = 20; // 1s parado decrementa 1
    /** Cooldown mínimo entre danos consecutivos (em ticks). */
    private static final int DAMAGE_COOLDOWN_TICKS = 5;

    private FreezeEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        var state = FreezeManager.getState(sp.getUUID());
        if (state == null) return;

        ServerLevel level = sp.serverLevel();
        long now = level.getGameTime();

        // Sempre zera velocidade + fall damage
        sp.setDeltaMovement(0, 0, 0);
        sp.fallDistance = 0f;

        // Verifica se ele se mexeu da âncora
        double dx = sp.getX() - state.anchorX;
        double dy = sp.getY() - state.anchorY;
        double dz = sp.getZ() - state.anchorZ;
        double sqDist = dx * dx + dy * dy + dz * dz;

        if (sqDist > MOVE_THRESHOLD_SQ) {
            // Player tentou se mover — teleporta de volta + dano
            sp.teleportTo(state.anchorX, state.anchorY, state.anchorZ);
            sp.setYRot(state.anchorYaw);
            sp.setXRot(state.anchorPitch);
            sp.setDeltaMovement(0, 0, 0);

            if (now - state.lastAttemptTick >= DAMAGE_COOLDOWN_TICKS) {
                state.moveAttempts = Math.min(state.moveAttempts + 1, 20);
                state.lastAttemptTick = now;
                float dmg = DAMAGE_PER_ATTEMPT * state.moveAttempts;
                sp.hurt(sp.damageSources().freeze(), dmg);
                // Som de dor + flash de partículas
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        sp.getX(), sp.getY() + 1, sp.getZ(),
                        20, 0.5, 0.8, 0.5, 0.1);
            }
        } else if (state.moveAttempts > 0 && (now - state.lastAttemptTick) >= ATTEMPT_DECAY_TICKS) {
            // Player ficou parado o suficiente — alivia 1 attempt
            state.moveAttempts -= 1;
            state.lastAttemptTick = now;
        }

        // Action bar a cada 20 ticks (action bar dura ~30 ticks no client)
        if (now % 20L == 0L) {
            Component msg = Component.literal("❄ NÃO SE MEXA OU TOMARÁ DANO ❄")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            sp.connection.send(new ClientboundSetActionBarTextPacket(msg));
        }

        // Partículas ambientes a cada 5 ticks
        if (now % 5L == 0L) {
            for (int i = 0; i < 6; i++) {
                double px = sp.getX() + (RNG.nextDouble() - 0.5) * 1.6;
                double py = sp.getY() + RNG.nextDouble() * 2.0;
                double pz = sp.getZ() + (RNG.nextDouble() - 0.5) * 1.6;
                level.sendParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 1, 0, 0, 0, 0.01);
            }
            if (now % 20L == 0L) {
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        sp.getX(), sp.getY() + 1, sp.getZ(),
                        8, 0.3, 0.8, 0.3, 0.05);
            }
        }

        // Força inventário a fechar cada 10 ticks (caso tenha aberto E ou container)
        if (now % 10L == 0L) {
            int cid = sp.containerMenu.containerId;
            sp.connection.send(new ClientboundContainerClosePacket(cid));
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (FreezeManager.isFrozen(e.getEntity().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (FreezeManager.isFrozen(e.getEntity().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty e) {
        if (FreezeManager.isFrozen(e.getEntity().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (FreezeManager.isFrozen(e.getEntity().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty e) {
        if (FreezeManager.isFrozen(e.getEntity().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (FreezeManager.isFrozen(e.getEntity().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (FreezeManager.isFrozen(e.getEntity().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent e) {
        if (FreezeManager.isFrozen(e.getEntity().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent e) {
        if (FreezeManager.isFrozen(e.getPlayer().getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && FreezeManager.isFrozen(sp.getUUID())) {
            e.setCanceled(true);
        }
    }

    /** Player desconectou: limpa estado (free-from-freeze grátis pra quem relogou). */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        FreezeManager.unfreeze(e.getEntity().getUUID());
    }

    /** Morte: descongelar (já tomou dano máximo de qualquer forma). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            FreezeManager.unfreeze(sp.getUUID());
        }
    }

    /**
     * Dano externo: deixa passar normalmente, mas se o atacante for um player
     * congelado, anular (já cancelamos via AttackEntityEvent, isso é defesa em
     * profundidade).
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent e) {
        var src = e.getSource().getEntity();
        if (src instanceof ServerPlayer sp && FreezeManager.isFrozen(sp.getUUID())) {
            e.setCanceled(true);
        }
    }
}
