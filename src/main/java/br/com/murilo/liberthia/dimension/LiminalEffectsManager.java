package br.com.murilo.liberthia.dimension;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.CosmicForceRotationS2CPacket;
import br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r57: <b>Liminal Effects Manager</b> — driver dos efeitos
 * psicológicos das 3 dimensões liminais.
 *
 * <h2>Filosofia</h2>
 * Efeitos atuam em INTERVALOS, não constantes. Cada player tem seed/timer
 * próprio pra não sincronizar. Tudo é per-player via ClientboundLevelParticlesPacket.
 *
 * <h2>Upside Sea (drift + whales)</h2>
 * <ul>
 *   <li>Slow Falling infinito (faz player flutuar)</li>
 *   <li>Levitation pulses random — sobe e desce de leve</li>
 *   <li>Camera drift periodic — yaw lento "puxa" pra um lado</li>
 *   <li>Whale-like sound effects (ghast far)</li>
 *   <li>Particle: dripping_water acima do player</li>
 *   <li>Sometimes drops fall UP</li>
 * </ul>
 *
 * <h2>Folded City (recursion + distance)</h2>
 * <ul>
 *   <li>Pedestres fake (humanoid silhouettes) à distância</li>
 *   <li>Random teleport "loop" — após X tempo movendo, snaps de volta</li>
 *   <li>Whispers distantes</li>
 *   <li>Hum baixo constante</li>
 *   <li>Distorção visual ocasional (mouse drift soft)</li>
 *   <li>Mining blocks: 30% chance de re-spawn igual (cidade se "lembra")</li>
 * </ul>
 *
 * <h2>Wooden Place (frozen creatures)</h2>
 * <ul>
 *   <li>Husks marcados que só andam quando player não olha</li>
 *   <li>Tree particles (logs flying)</li>
 *   <li>Heartbeat low frequency</li>
 *   <li>Random root sounds</li>
 *   <li>Spawn esporádico de Husks "wood walkers"</li>
 * </ul>
 *
 * <h2>Death-no-loss</h2>
 * Como Spirit World — items preservados ao morrer em liminal.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class LiminalEffectsManager {

    // Per-player tick timers
    private static final Map<UUID, Long> NEXT_EFFECT_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_SOUND_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_TP_LOOP_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> LOOP_ANCHOR = new ConcurrentHashMap<>();
    private static final Map<UUID, java.util.List<UUID>> WOOD_CREATURES = new ConcurrentHashMap<>();

    private LiminalEffectsManager() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!LiminalDimensions.isInLiminal(sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;

        if (LiminalDimensions.isUpsideSea(sp)) {
            tickUpsideSea(sp, level);
        } else if (LiminalDimensions.isFoldedCity(sp)) {
            tickFoldedCity(sp, level);
        } else if (LiminalDimensions.isWoodenPlace(sp)) {
            tickWoodenPlace(sp, level);
        }
    }

    // ──────────────────────── UPSIDE SEA ────────────────────────

    private static void tickUpsideSea(ServerPlayer sp, ServerLevel level) {
        UUID id = sp.getUUID();

        // Slow falling permanente
        if (sp.tickCount % 60 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false));
            sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 80, 0, true, false));
        }

        // Levitation pulse random (a cada ~20s)
        Long nextEffect = NEXT_EFFECT_TICK.get(id);
        if (nextEffect == null || sp.tickCount >= nextEffect) {
            // Pequeno boost up
            if (Math.random() < 0.5) {
                sp.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0, true, false));
            } else {
                // Camera drift via packet
                try {
                    ModNetwork.sendToPlayer(sp, new CosmicForceRotationS2CPacket(
                            (float)((Math.random() - 0.5) * 6.0),
                            (float)((Math.random() - 0.5) * 3.0)));
                } catch (Throwable ignored) {}
            }
            NEXT_EFFECT_TICK.put(id, (long)sp.tickCount + 200 + (long)(Math.random() * 200));
        }

        // Water particles falling UP (random spots)
        if (sp.tickCount % 15 == 0) {
            for (int i = 0; i < 4; i++) {
                double dx = (Math.random() - 0.5) * 16;
                double dz = (Math.random() - 0.5) * 16;
                double dy = -2 - Math.random() * 4;
                // Upward velocity
                level.sendParticles(ParticleTypes.SPLASH,
                        sp.getX() + dx, sp.getY() + dy, sp.getZ() + dz,
                        1, 0, 0.2, 0, 0.05);
            }
        }
        // Ash drifting (debris)
        if (sp.tickCount % 8 == 0) {
            for (int i = 0; i < 3; i++) {
                double dx = (Math.random() - 0.5) * 20;
                double dy = (Math.random() - 0.5) * 12;
                double dz = (Math.random() - 0.5) * 20;
                level.sendParticles(ParticleTypes.WHITE_ASH,
                        sp.getX() + dx, sp.getY() + dy, sp.getZ() + dz,
                        1, 0, 0, 0, 0);
            }
        }

        // Whale-like sound (ghast far)
        Long nextSound = NEXT_SOUND_TICK.get(id);
        if (nextSound == null || sp.tickCount >= nextSound) {
            try {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.Holder.direct(SoundEvents.GHAST_AMBIENT),
                        SoundSource.AMBIENT,
                        sp.getX() + (Math.random() - 0.5) * 60,
                        sp.getY() + 30 + Math.random() * 20,
                        sp.getZ() + (Math.random() - 0.5) * 60,
                        4.0F, 0.3F + (float)(Math.random() * 0.2F),
                        sp.level().random.nextLong()));
            } catch (Throwable ignored) {}
            NEXT_SOUND_TICK.put(id, (long)sp.tickCount + 300 + (long)(Math.random() * 400));
        }
    }

    // ──────────────────────── FOLDED CITY ────────────────────────

    private static void tickFoldedCity(ServerPlayer sp, ServerLevel level) {
        UUID id = sp.getUUID();

        // Slow movement (city feels heavy)
        if (sp.tickCount % 60 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0, true, false));
        }

        // Recursive loop: a cada 90s, se o player se moveu muito, snap de volta
        Vec3 anchor = LOOP_ANCHOR.get(id);
        if (anchor == null) {
            LOOP_ANCHOR.put(id, sp.position());
        } else {
            double dist = sp.position().distanceTo(anchor);
            Long lastLoop = LAST_TP_LOOP_TICK.get(id);
            if (lastLoop == null) lastLoop = 0L;
            // Após 90s movendo + dist > 50, fold de volta
            if (sp.tickCount - lastLoop > 1800 && dist > 50) {
                // SNAP teleport de volta — sem aviso
                sp.teleportTo(anchor.x, anchor.y, anchor.z);
                LAST_TP_LOOP_TICK.put(id, (long)sp.tickCount);
                level.sendParticles(ParticleTypes.PORTAL,
                        anchor.x, anchor.y + 1, anchor.z, 30, 0.3, 0.5, 0.3, 0.05);
                sp.displayClientMessage(Component.literal(
                        "§8§o✦ você já passou por aqui."), true);
            }
            // Reset anchor a cada 5 minutos
            if (sp.tickCount % 6000 == 0) {
                LOOP_ANCHOR.put(id, sp.position());
                LAST_TP_LOOP_TICK.put(id, (long)sp.tickCount);
            }
        }

        // Pedestres fake distantes — silhuetas no horizonte
        if (sp.tickCount % 60 == 0 && Math.random() < 0.3) {
            spawnDistantFigure(sp, level);
        }

        // Whispers/hum random
        Long nextSound = NEXT_SOUND_TICK.get(id);
        if (nextSound == null || sp.tickCount >= nextSound) {
            int pick = (int)(Math.random() * 100);
            if (pick < 40) CosmicSoundManager.playDistantWhispers(sp);
            else if (pick < 70) CosmicSoundManager.playSkyHum(sp);
            else CosmicSoundManager.playFalseFootsteps(sp, true);
            NEXT_SOUND_TICK.put(id, (long)sp.tickCount + 200 + (long)(Math.random() * 300));
        }

        // Camera drift soft
        Long nextEffect = NEXT_EFFECT_TICK.get(id);
        if (nextEffect == null || sp.tickCount >= nextEffect) {
            try {
                ModNetwork.sendToPlayer(sp, new CosmicForceRotationS2CPacket(
                        (float)((Math.random() - 0.5) * 4.0), 0F));
            } catch (Throwable ignored) {}
            NEXT_EFFECT_TICK.put(id, (long)sp.tickCount + 400 + (long)(Math.random() * 400));
        }

        // Ash falling (city is ancient)
        if (sp.tickCount % 6 == 0) {
            level.sendParticles(ParticleTypes.WHITE_ASH,
                    sp.getX() + (Math.random() - 0.5) * 16,
                    sp.getY() + 8 + Math.random() * 4,
                    sp.getZ() + (Math.random() - 0.5) * 16,
                    2, 0.2, 0, 0.2, 0);
        }
    }

    private static void spawnDistantFigure(ServerPlayer sp, ServerLevel level) {
        // Spawna cluster de soul particles formando uma silhueta a 30-50b
        double a = Math.random() * Math.PI * 2;
        double r = 30 + Math.random() * 20;
        double fx = sp.getX() + Math.cos(a) * r;
        double fz = sp.getZ() + Math.sin(a) * r;
        double fy = sp.getY();
        // 6 particles verticais
        for (int i = 0; i < 6; i++) {
            level.sendParticles(ParticleTypes.SOUL,
                    fx, fy + i * 0.35, fz, 1, 0.05, 0.05, 0.05, 0);
        }
        // Aura
        level.sendParticles(ParticleTypes.SMOKE, fx, fy + 1, fz, 3, 0.3, 0.5, 0.3, 0.01);
    }

    // ──────────────────────── WOODEN PLACE ────────────────────────

    private static void tickWoodenPlace(ServerPlayer sp, ServerLevel level) {
        UUID id = sp.getUUID();

        // Heartbeat low-frequency (a cada ~6s)
        if (sp.tickCount % 120 == 0) {
            try {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.Holder.direct(SoundEvents.WARDEN_HEARTBEAT),
                        SoundSource.AMBIENT,
                        sp.getX(), sp.getY(), sp.getZ(),
                        1.5F, 0.4F + (float)(Math.random() * 0.2F),
                        sp.level().random.nextLong()));
            } catch (Throwable ignored) {}
        }

        // Spawna criaturas wood a cada ~60s, max 3
        java.util.List<UUID> creatures = WOOD_CREATURES.computeIfAbsent(id, k -> new java.util.ArrayList<>());
        // Limpa lista (creaturas mortas/desaparecidas)
        creatures.removeIf(cid -> {
            var e = level.getEntity(cid);
            return e == null || !e.isAlive();
        });
        if (creatures.size() < 3 && sp.tickCount % 1200 == 0 && Math.random() < 0.6) {
            spawnWoodCreature(sp, level, creatures);
        }

        // Wood creatures "freeze on look" — tick AI
        if (sp.tickCount % 10 == 0) {
            Vec3 playerLook = sp.getLookAngle();
            for (UUID cid : creatures) {
                var e = level.getEntity(cid);
                if (!(e instanceof LivingEntity creature)) continue;
                Vec3 toCreature = creature.position().subtract(sp.position()).normalize();
                double dot = toCreature.dot(playerLook);
                if (dot > 0.5 && sp.distanceTo(creature) < 50) {
                    // Player olhando — FREEZE
                    creature.setDeltaMovement(0, creature.getDeltaMovement().y, 0);
                    creature.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 200, true, false));
                    creature.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 10, true, false));
                } else {
                    // Não olhando — MOVE RAPIDLY
                    Vec3 dirToPlayer = sp.position().subtract(creature.position()).normalize();
                    creature.setDeltaMovement(dirToPlayer.x * 0.5, creature.getDeltaMovement().y, dirToPlayer.z * 0.5);
                    creature.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 3, true, false));
                }
            }
        }

        // Wood particles caindo
        if (sp.tickCount % 5 == 0) {
            BlockParticleOption leaves = new BlockParticleOption(
                    ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState());
            for (int i = 0; i < 3; i++) {
                double dx = (Math.random() - 0.5) * 14;
                double dz = (Math.random() - 0.5) * 14;
                level.sendParticles(leaves,
                        sp.getX() + dx, sp.getY() + 5 + Math.random() * 3, sp.getZ() + dz,
                        1, 0.05, 0.05, 0.05, 0);
            }
        }

        // Root sounds random
        Long nextSound = NEXT_SOUND_TICK.get(id);
        if (nextSound == null || sp.tickCount >= nextSound) {
            try {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.Holder.direct(SoundEvents.WOOD_BREAK),
                        SoundSource.AMBIENT,
                        sp.getX() + (Math.random() - 0.5) * 30,
                        sp.getY() + Math.random() * 4,
                        sp.getZ() + (Math.random() - 0.5) * 30,
                        2.0F, 0.4F + (float)(Math.random() * 0.3F),
                        sp.level().random.nextLong()));
            } catch (Throwable ignored) {}
            NEXT_SOUND_TICK.put(id, (long)sp.tickCount + 200 + (long)(Math.random() * 400));
        }
    }

    private static void spawnWoodCreature(ServerPlayer sp, ServerLevel level, java.util.List<UUID> creatures) {
        var husk = net.minecraft.world.entity.EntityType.HUSK.create(level);
        if (husk == null) return;
        double a = Math.random() * Math.PI * 2;
        double r = 20 + Math.random() * 15;
        husk.moveTo(sp.getX() + Math.cos(a) * r, sp.getY(), sp.getZ() + Math.sin(a) * r, 0, 0);
        husk.setCustomName(Component.literal("§2§oCaminhante de Casca"));
        husk.setCustomNameVisible(false);
        husk.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 24000, 0, true, false));
        husk.setPersistenceRequired();
        husk.getPersistentData().putBoolean("liberthia.wood_walker", true);
        husk.getPersistentData().putUUID("liberthia.wood_target", sp.getUUID());
        level.addFreshEntity(husk);
        creatures.add(husk.getUUID());
        // Sound de spawn (crack)
        level.sendParticles(ParticleTypes.SQUID_INK,
                husk.getX(), husk.getY() + 1, husk.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
    }

    // ──────────────────────── DEATH PROTECTION ────────────────────────

    /** No-item-loss em todas liminais (mesmo padrão do Spirit World). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLiminalDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!LiminalDimensions.isInLiminal(p)) return;

        java.util.List<net.minecraft.world.item.ItemStack> preserved = new java.util.ArrayList<>();
        event.getDrops().forEach(ie -> {
            if (!ie.getItem().isEmpty()) preserved.add(ie.getItem().copy());
        });
        if (!preserved.isEmpty()) {
            // Salva no NBT do player pra restaurar no respawn
            var listTag = new net.minecraft.nbt.ListTag();
            for (var s : preserved) listTag.add(s.save(new net.minecraft.nbt.CompoundTag()));
            p.getPersistentData().put("liberthia.liminal_preserved", listTag);
        }
        event.setCanceled(true);
        if (p instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(
                    "§8§o✦ algo te entrega de volta o que cai."), false);
        }
    }

    @SubscribeEvent
    public static void onLiminalRespawn(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        Player oldP = event.getOriginal();
        Player newP = event.getEntity();
        if (!oldP.getPersistentData().contains("liberthia.liminal_preserved")) return;

        var listTag = oldP.getPersistentData().getList(
                "liberthia.liminal_preserved", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            net.minecraft.world.item.ItemStack stack = net.minecraft.world.item.ItemStack.of(
                    listTag.getCompound(i));
            if (!stack.isEmpty()) {
                if (!newP.getInventory().add(stack)) newP.drop(stack, false);
            }
        }
        oldP.getPersistentData().remove("liberthia.liminal_preserved");
        if (newP instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5seus pertences retornam do lugar errado."), false);
        }
    }

    /** Cleanup quando player desloga. */
    public static void cleanup(UUID id) {
        NEXT_EFFECT_TICK.remove(id);
        NEXT_SOUND_TICK.remove(id);
        LAST_TP_LOOP_TICK.remove(id);
        LOOP_ANCHOR.remove(id);
        WOOD_CREATURES.remove(id);
    }
}
