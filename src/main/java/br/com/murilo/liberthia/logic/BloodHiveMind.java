package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.22: Mente colmeia (hive mind) das criaturas de sangue.
 *
 * <p>Mecânica: quando um player ataca um {@link BloodKinPassage#isBloodCreature
 * blood creature}, o dano é acumulado numa "threat table". A cada 60 ticks (3s)
 * todas as blood creatures num raio de 32 blocos do player com MAIOR threat são
 * forçadas a target esse player — convergem na maior ameaça.
 *
 * <p>Threat decay: a cada tick passado, threat decresce ~0.5%. Em 60s sem dano,
 * a entrada some completamente. Permite "esconder" depois de fugir.
 *
 * <p>User pediu: "monstros é para funcionar como se fosse uma mente colmeia
 * eles vão atacar quem dá mais dano".
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class BloodHiveMind {

    /** Player UUID → threat acumulado (não-negativo). */
    private static final Map<UUID, Float> THREAT = new HashMap<>();
    /** Player UUID → último tick em que tomou dano (pra decay). */
    private static final Map<UUID, Long> LAST_UPDATE = new HashMap<>();

    /** A cada N ticks (3s) reorienta mobs. */
    private static final int REORIENT_INTERVAL = 60;
    /** Raio em volta do player onde mobs são "recrutados" pelo hive. */
    private static final double HIVE_RADIUS = 32.0;
    /** Decay por tick (multiplicador). 0.995^60 ≈ 0.74/check ≈ -26%/3s. */
    private static final float DECAY_PER_TICK = 0.995f;
    /** Mínimo threat pra ser considerado "alvo principal". */
    private static final float MIN_THREAT = 1.0f;

    private BloodHiveMind() {}

    /**
     * Hook: player atacou blood creature → soma damage na threat table.
     */
    @SubscribeEvent
    public static void onBloodCreatureHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0) return;
        LivingEntity victim = event.getEntity();
        if (!BloodKinPassage.isBloodCreatureStatic(victim)) return;

        var srcEntity = event.getSource().getEntity();
        if (!(srcEntity instanceof Player attacker)) return;
        // Player em creative/spectator não gera threat (admin ghost mode)
        if (attacker.isCreative() || attacker.isSpectator()) return;
        // Player com sigil também NÃO gera threat (caso o sigil falhe a proteção
        // por algum motivo, ele segue invisível pra hive)
        if (BloodKinPassage.hasSigil(attacker)) return;

        UUID id = attacker.getUUID();
        float current = THREAT.getOrDefault(id, 0f);
        THREAT.put(id, current + event.getAmount());
        LAST_UPDATE.put(id, attacker.level().getGameTime());
    }

    /**
     * Tick global: a cada 60t, aplica decay + reorienta mobs de sangue.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % REORIENT_INTERVAL != 0) return;
        if (THREAT.isEmpty()) return;

        long now = event.getServer().getTickCount();
        // Decay + cleanup
        THREAT.entrySet().removeIf(entry -> {
            UUID id = entry.getKey();
            long last = LAST_UPDATE.getOrDefault(id, 0L);
            long elapsed = Math.max(0, now - last);
            float decayed = (float) (entry.getValue() * Math.pow(DECAY_PER_TICK, elapsed));
            if (decayed < MIN_THREAT) {
                LAST_UPDATE.remove(id);
                return true;
            }
            entry.setValue(decayed);
            return false;
        });

        // Pra cada player com threat, recruta blood mobs no raio HIVE_RADIUS.
        // Maior threat = maior prioridade.
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Player player : level.players()) {
                if (player.isCreative() || player.isSpectator()) continue;
                Float threat = THREAT.get(player.getUUID());
                if (threat == null || threat < MIN_THREAT) continue;
                recruitMobs(level, player);
            }
        }
    }

    /**
     * Força mobs de sangue num raio HIVE_RADIUS a target o player. Convergência
     * de hive.
     */
    private static void recruitMobs(ServerLevel level, Player player) {
        AABB box = new AABB(player.blockPosition()).inflate(HIVE_RADIUS);
        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!(le instanceof Mob mob)) continue;
            if (!BloodKinPassage.isBloodCreatureStatic(mob)) continue;
            // Sigil-protected players são pulados (já checado no recruit loop
            // acima, mas garantia em profundidade)
            if (BloodKinPassage.hasSigil(player)) continue;
            // Se o mob já tem o player como target, não força (evita reset de IA path)
            if (mob.getTarget() == player) continue;
            // Compara threat do current target vs novo player
            LivingEntity currentTarget = mob.getTarget();
            if (currentTarget instanceof Player oldTarget) {
                float oldThreat = THREAT.getOrDefault(oldTarget.getUUID(), 0f);
                float newThreat = THREAT.getOrDefault(player.getUUID(), 0f);
                if (newThreat <= oldThreat) continue;
            }
            mob.setTarget(player);
        }
    }

    /** Limpa estado quando server desliga (não persiste entre reinícios). */
    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        THREAT.clear();
        LAST_UPDATE.clear();
    }
}
