package br.com.murilo.liberthia.cosmic.living;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * v0.1.22 r47: <b>Chunk Memory</b> — uma "memória" invisível que cada chunk
 * acumula ao longo do tempo. Não é vanilla state — é um sub-sistema do mod
 * que armazena o que "aconteceu" naquele chunk.
 *
 * <h2>O que é gravado</h2>
 * <ul>
 *   <li><b>deathCount:</b> mortes naquele chunk (PvP ou mob)</li>
 *   <li><b>chatActivity:</b> mensagens de chat enviadas estando ali</li>
 *   <li><b>blocksBroken / blocksPlaced:</b> mining/building activity</li>
 *   <li><b>visitCount:</b> ticks de player presente</li>
 *   <li><b>pvpKills:</b> mortes PLAYER vs PLAYER</li>
 *   <li><b>rituals:</b> rituais executados ali</li>
 *   <li><b>echoes:</b> queue de eventos prontos pra replay</li>
 * </ul>
 *
 * <h2>Emotional Index</h2>
 * Combina os contadores num único score 0-100 que mede o quanto a chunk
 * está "contaminada psicologicamente". Threshold ≥ 30 começa a gerar echoes.
 */
public class ChunkMemory {

    public final ChunkPos pos;

    // Counters
    public int deathCount = 0;
    public int pvpKills = 0;
    public int chatActivity = 0;
    public int blocksBroken = 0;
    public int blocksPlaced = 0;
    public long visitTicks = 0;
    public int ritualsPerformed = 0;
    /** Last time something happened (gameTime). */
    public long lastEventTick = 0;

    /** Recent echoes ready to replay. */
    public final LinkedList<Echo> echoes = new LinkedList<>();

    public ChunkMemory(ChunkPos pos) {
        this.pos = pos;
    }

    /**
     * Emotional Index (0-100) — combina tudo com pesos.
     * Deaths e PvP pesam mais (são mais "intensos" emocionalmente).
     */
    public int emotionalIndex() {
        double idx = deathCount * 6
                + pvpKills * 10           // PvP é o mais intenso
                + ritualsPerformed * 8
                + Math.min(20, chatActivity * 0.5)
                + Math.min(15, (blocksBroken + blocksPlaced) * 0.02)
                + Math.min(10, visitTicks / 6000.0); // 5 min visita = 1 ponto
        return Math.max(0, Math.min(100, (int) Math.round(idx)));
    }

    /** Record event types. */
    public void recordDeath(long tick) {
        deathCount++;
        lastEventTick = tick;
    }

    public void recordPvpKill(long tick) {
        pvpKills++;
        deathCount++;
        lastEventTick = tick;
    }

    public void recordChat(long tick, String msg, UUID author) {
        chatActivity++;
        lastEventTick = tick;
        // Sometimes preserve the message as an echo to replay later
        if (Math.random() < 0.10 && msg.length() > 4 && msg.length() < 100) {
            addEcho(new Echo(Echo.Type.CHAT, msg, author, tick));
        }
    }

    public void recordBlockBreak(long tick) {
        blocksBroken++;
        if (tick - lastEventTick > 100) lastEventTick = tick;
    }

    public void recordBlockPlace(long tick) {
        blocksPlaced++;
        if (tick - lastEventTick > 100) lastEventTick = tick;
    }

    public void recordRitual(long tick) {
        ritualsPerformed++;
        lastEventTick = tick;
    }

    public void recordVisit(int ticks) {
        visitTicks += ticks;
    }

    public void addEcho(Echo echo) {
        echoes.addFirst(echo);
        while (echoes.size() > 8) echoes.removeLast(); // max 8 echoes
    }

    /** Pega um echo aleatório (não remove). */
    public Echo pickEcho() {
        if (echoes.isEmpty()) return null;
        return echoes.get((int) (Math.random() * echoes.size()));
    }

    // ────────── Echoes ──────────

    public static class Echo {
        public enum Type {
            FOOTSTEP,     // Som de passo de player phantom
            MINING,       // Som de mineração
            DOOR_OPEN,    // Som de porta abrindo
            CHAT,         // Mensagem de chat gravada
            DEATH_CRY,    // Som de morte
            BLOCK_BREAK,  // Som de bloco quebrando
            CONVERSATION  // Conversa fragmentada (2-3 msgs)
        }

        public final Type type;
        public final String data;
        public final UUID origin;
        public final long originalTick;

        public Echo(Type type, String data, UUID origin, long tick) {
            this.type = type;
            this.data = data == null ? "" : data;
            this.origin = origin;
            this.originalTick = tick;
        }

        public CompoundTag toNbt() {
            CompoundTag t = new CompoundTag();
            t.putString("Type", type.name());
            t.putString("Data", data);
            if (origin != null) t.putUUID("Origin", origin);
            t.putLong("Tick", originalTick);
            return t;
        }

        public static Echo fromNbt(CompoundTag t) {
            Type type;
            try { type = Type.valueOf(t.getString("Type")); }
            catch (Exception e) { type = Type.FOOTSTEP; }
            String data = t.getString("Data");
            UUID origin = t.hasUUID("Origin") ? t.getUUID("Origin") : null;
            long tick = t.getLong("Tick");
            return new Echo(type, data, origin, tick);
        }
    }

    // ────────── Serialization ──────────

    public CompoundTag toNbt() {
        CompoundTag t = new CompoundTag();
        t.putInt("X", pos.x);
        t.putInt("Z", pos.z);
        t.putInt("Deaths", deathCount);
        t.putInt("PvP", pvpKills);
        t.putInt("Chat", chatActivity);
        t.putInt("Broken", blocksBroken);
        t.putInt("Placed", blocksPlaced);
        t.putLong("Visits", visitTicks);
        t.putInt("Rituals", ritualsPerformed);
        t.putLong("LastEvent", lastEventTick);
        ListTag list = new ListTag();
        for (Echo e : echoes) list.add(e.toNbt());
        t.put("Echoes", list);
        return t;
    }

    public static ChunkMemory fromNbt(CompoundTag t) {
        ChunkMemory cm = new ChunkMemory(new ChunkPos(t.getInt("X"), t.getInt("Z")));
        cm.deathCount = t.getInt("Deaths");
        cm.pvpKills = t.getInt("PvP");
        cm.chatActivity = t.getInt("Chat");
        cm.blocksBroken = t.getInt("Broken");
        cm.blocksPlaced = t.getInt("Placed");
        cm.visitTicks = t.getLong("Visits");
        cm.ritualsPerformed = t.getInt("Rituals");
        cm.lastEventTick = t.getLong("LastEvent");
        ListTag list = t.getList("Echoes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            cm.echoes.add(Echo.fromNbt(list.getCompound(i)));
        }
        return cm;
    }
}
