package br.com.murilo.liberthia.cosmic.wooddim;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r56: <b>Wood Dimension Event</b> — em sanidade muito baixa, o
 * player pode ser teleportado por 20s para um "lugar de madeira" — uma área
 * temporária cheia de criaturas de madeira que só se movem quando o player
 * não está olhando.
 *
 * <h2>Implementação</h2>
 * Não cria dimensão real — usa Spirit World como container e força um
 * setor "wood" via builders de blocos efêmeros. Player é tp'd pra setor
 * pré-construído quando trigger ativa.
 *
 * <p>Versão simplificada: spawna 4-6 Husks invisíveis com glow ao redor do
 * player + cluster de log particles. Eles atacam quando o player desvia o olhar.
 * Após 20s, player retorna automaticamente.
 *
 * <h2>Trigger</h2>
 * Sanity ≤ 25, em qualquer dimensão. Cooldown 30 min.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class WoodDimensionEvent {

    /** Player UUID → estado ativo. */
    private static final Map<UUID, WoodState> ACTIVE = new ConcurrentHashMap<>();
    /** Cooldowns. */
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    /** Duração total do evento em ticks. */
    private static final int DURATION_TICKS = 400; // 20s
    private static final long COOLDOWN_TICKS = 36000; // 30 min

    private WoodDimensionEvent() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;

        UUID id = sp.getUUID();
        WoodState state = ACTIVE.get(id);

        if (state != null) {
            tickActive(sp, level, state, id);
            return;
        }

        // Try trigger
        if (sp.tickCount % 100 != 0) return;
        Long cd = COOLDOWNS.get(id);
        if (cd != null && sp.tickCount < cd) return;

        int sanity = SpiritDimension.getSanity(sp);
        if (sanity > 25) return;

        if (Math.random() > 0.008) return; // 0.8% per check (~1 trigger per ~15 checks at low sanity)

        trigger(sp, level, id);
    }

    private static void trigger(ServerPlayer sp, ServerLevel level, UUID id) {
        WoodState state = new WoodState();
        state.startTick = sp.tickCount;
        state.lastLookTick = sp.tickCount;
        state.origin = sp.position();
        ACTIVE.put(id, state);
        COOLDOWNS.put(id, (long)sp.tickCount + COOLDOWN_TICKS);

        // Burst de log particles em volta
        BlockParticleOption logP = new BlockParticleOption(
                ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState());
        BlockParticleOption leavesP = new BlockParticleOption(
                ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState());
        for (int i = 0; i < 200; i++) {
            double a = Math.random() * Math.PI * 2;
            double r = 3 + Math.random() * 8;
            double dy = Math.random() * 6;
            level.sendParticles(i % 2 == 0 ? logP : leavesP,
                    sp.getX() + Math.cos(a) * r,
                    sp.getY() + dy,
                    sp.getZ() + Math.sin(a) * r,
                    1, 0.05, 0.05, 0.05, 0);
        }
        // Aura mágica
        level.sendParticles(ParticleTypes.PORTAL,
                sp.getX(), sp.getY() + 1, sp.getZ(), 100, 2, 2, 2, 0.1);

        // Spawna 5 Husks invisíveis "wood creature" em volta
        for (int i = 0; i < 5; i++) {
            double a = (i / 5.0) * Math.PI * 2;
            double r = 8 + Math.random() * 4;
            double sx = sp.getX() + Math.cos(a) * r;
            double sz = sp.getZ() + Math.sin(a) * r;
            Husk husk = net.minecraft.world.entity.EntityType.HUSK.create(level);
            if (husk == null) continue;
            husk.moveTo(sx, sp.getY(), sz, 0, 0);
            husk.setCustomName(Component.literal("§2§lFigura de Madeira"));
            husk.setCustomNameVisible(false);
            // Invisibility ativa só vezes em vezes (look-blink mechanic)
            husk.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DURATION_TICKS, 0, false, false));
            husk.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, DURATION_TICKS, 1, false, false));
            husk.setPersistenceRequired();
            husk.getPersistentData().putBoolean("liberthia.wood_creature", true);
            husk.getPersistentData().putLong("liberthia.wood_despawn_at",
                    sp.tickCount + DURATION_TICKS);
            husk.getPersistentData().putUUID("liberthia.wood_target", sp.getUUID());
            state.creatures.add(husk.getUUID());
            level.addFreshEntity(husk);
        }

        // Som de teleport + breathing
        CosmicSoundManager.playRealityDistortion(sp);
        CosmicSoundManager.playVoidBreathing(sp);
        sp.displayClientMessage(Component.literal(
                "§2§o✦ §rO ar fica grosso. Você sente raízes."), false);

        LiberthiaMod.LOGGER.info("[WoodDim] {} triggered (sanity-driven)",
                sp.getName().getString());
    }

    private static void tickActive(ServerPlayer sp, ServerLevel level, WoodState state, UUID id) {
        int elapsed = sp.tickCount - state.startTick;

        // Check expiration
        if (elapsed >= DURATION_TICKS) {
            endEvent(sp, level, state, id);
            return;
        }

        // A cada 20 ticks (1s), spawn ambient particles
        if (sp.tickCount % 20 == 0) {
            BlockParticleOption leavesP = new BlockParticleOption(
                    ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState());
            for (int i = 0; i < 15; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 4 + Math.random() * 8;
                level.sendParticles(leavesP,
                        sp.getX() + Math.cos(a) * r,
                        sp.getY() + Math.random() * 4,
                        sp.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }

        // Creatures "look-blink" — atacam quando player NÃO está olhando
        if (sp.tickCount % 40 == 0) {
            Vec3 lookDir = sp.getLookAngle();
            for (UUID creatureId : state.creatures) {
                var ent = level.getEntity(creatureId);
                if (!(ent instanceof LivingEntity creature)) continue;
                Vec3 toCreature = creature.position().subtract(sp.position()).normalize();
                double dot = toCreature.dot(lookDir);
                if (dot < 0.4) {
                    // Não está olhando — creature avança
                    Vec3 dirToPlayer = sp.position().subtract(creature.position()).normalize();
                    creature.setDeltaMovement(creature.getDeltaMovement().add(
                            dirToPlayer.scale(0.3)));
                    // Toggle invisibility off temporariamente pra spawning de particles
                    if (creature.distanceTo(sp) < 5) {
                        // Attack
                        sp.hurt(sp.damageSources().mobAttack(creature), 2.0F);
                        creature.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    }
                }
                // Particles na creature
                level.sendParticles(ParticleTypes.SQUID_INK,
                        creature.getX(), creature.getY() + 1, creature.getZ(),
                        3, 0.2, 0.4, 0.2, 0.02);
            }
        }

        // Confusion durante todo o evento
        if (sp.tickCount % 200 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 240, 0, false, false));
        }

        // Som ambiente periódico
        if (sp.tickCount % 80 == 0) {
            CosmicSoundManager.playTendrilMovement(sp);
        }
    }

    private static void endEvent(ServerPlayer sp, ServerLevel level, WoodState state, UUID id) {
        // Despawn creatures
        for (UUID creatureId : state.creatures) {
            var ent = level.getEntity(creatureId);
            if (ent != null) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        ent.getX(), ent.getY() + 1, ent.getZ(),
                        20, 0.3, 0.5, 0.3, 0.05);
                ent.discard();
            }
        }
        // VFX de "retorno"
        level.sendParticles(ParticleTypes.PORTAL,
                sp.getX(), sp.getY() + 1, sp.getZ(), 60, 1, 1, 1, 0.1);
        sp.displayClientMessage(Component.literal(
                "§7§o✦ O cheiro de seiva some. O ar volta."), false);
        ACTIVE.remove(id);
        LiberthiaMod.LOGGER.info("[WoodDim] {} event ended", sp.getName().getString());
    }

    public static void cleanup(UUID playerId) {
        ACTIVE.remove(playerId);
        COOLDOWNS.remove(playerId);
    }

    private static class WoodState {
        int startTick;
        int lastLookTick;
        Vec3 origin;
        java.util.List<UUID> creatures = new java.util.ArrayList<>();
    }
}
