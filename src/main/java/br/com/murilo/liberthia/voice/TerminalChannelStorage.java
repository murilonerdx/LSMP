package br.com.murilo.liberthia.voice;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.block.entity.QuantumTerminalBlockEntity.LogEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.22 r39: Storage server-side de mensagens por canal (frequência).
 *
 * <h2>Conceito</h2>
 * <p>Mensagens dos Quantum Terminals NÃO são mais broadcast pro chat — ficam
 * SALVAS num arquivo no nível overworld (via {@link SavedData}). Cada canal
 * (frequência) tem sua própria lista de até {@link #MAX_PER_CHANNEL} mensagens
 * (auto-evict mais antigas).
 *
 * <h2>Persistência</h2>
 * <p>Arquivo: {@code <world>/data/liberthia_terminal_channels.dat} —
 * formato NBT (CompoundTag), gerenciado pelo Vanilla via {@link SavedData}.
 *
 * <h2>API</h2>
 * <ul>
 *   <li>{@link #get(MinecraftServer)} — obtém a instância singleton (per server)</li>
 *   <li>{@link #getMessages(String)} — copia da lista de mensagens do canal</li>
 *   <li>{@link #addMessage(String, LogEntry)} — adiciona mensagem ao canal</li>
 *   <li>{@link #clearChannel(String)} — limpa todas mensagens de um canal</li>
 * </ul>
 *
 * <h2>Acesso por terminais</h2>
 * Quando um terminal muda de frequência, ele chama {@link #getMessages} pra
 * recarregar o display local. Quando recebe broadcast, chama
 * {@link #addMessage} pra salvar e refresh o display.
 */
public class TerminalChannelStorage extends SavedData {

    public static final String STORAGE_KEY = "liberthia_terminal_channels";
    public static final int MAX_PER_CHANNEL = 100;

    /** Map de canal → fila (newest first). ConcurrentHashMap pra thread-safety. */
    private final Map<String, LinkedList<LogEntry>> channels = new ConcurrentHashMap<>();

    public TerminalChannelStorage() {
        super();
    }

    /** Carrega de NBT salvo. */
    public static TerminalChannelStorage load(CompoundTag tag) {
        TerminalChannelStorage s = new TerminalChannelStorage();
        if (tag.contains("Channels")) {
            CompoundTag channelsTag = tag.getCompound("Channels");
            for (String channelName : channelsTag.getAllKeys()) {
                ListTag entriesTag = channelsTag.getList(channelName, Tag.TAG_COMPOUND);
                LinkedList<LogEntry> list = new LinkedList<>();
                for (int i = 0; i < entriesTag.size(); i++) {
                    list.add(LogEntry.fromNbt(entriesTag.getCompound(i)));
                }
                s.channels.put(channelName, list);
            }
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag channelsTag = new CompoundTag();
        for (Map.Entry<String, LinkedList<LogEntry>> entry : channels.entrySet()) {
            ListTag entriesTag = new ListTag();
            // Snapshot defensivo
            List<LogEntry> snapshot;
            synchronized (entry.getValue()) {
                snapshot = new ArrayList<>(entry.getValue());
            }
            for (LogEntry e : snapshot) {
                entriesTag.add(e.toNbt());
            }
            channelsTag.put(entry.getKey(), entriesTag);
        }
        tag.put("Channels", channelsTag);
        return tag;
    }

    /**
     * Singleton accessor — usa overworld pra persistir (canais são globais
     * cross-dimension, igual {@link AntennaNetwork}).
     */
    public static TerminalChannelStorage get(MinecraftServer server) {
        if (server == null) return new TerminalChannelStorage(); // fallback in-memory
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                TerminalChannelStorage::load,
                TerminalChannelStorage::new,
                STORAGE_KEY);
    }

    /** Versão estática via level (pega o server do level). */
    public static TerminalChannelStorage get(ServerLevel level) {
        return get(level.getServer());
    }

    /**
     * Cópia das mensagens do canal (newest first). Retorna lista vazia se
     * canal não existe ou ainda não tem mensagens.
     */
    public List<LogEntry> getMessages(String channel) {
        if (channel == null || channel.isEmpty()) return Collections.emptyList();
        LinkedList<LogEntry> list = channels.get(channel);
        if (list == null) return Collections.emptyList();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    /**
     * Adiciona mensagem ao canal. Mantém no máximo {@link #MAX_PER_CHANNEL}
     * mensagens (auto-evict antigas). Marca SavedData como dirty pra salvar
     * no próximo flush.
     */
    public void addMessage(String channel, LogEntry entry) {
        if (channel == null || channel.isEmpty() || entry == null) return;
        LinkedList<LogEntry> list = channels.computeIfAbsent(channel, k -> new LinkedList<>());
        synchronized (list) {
            list.addFirst(entry);
            while (list.size() > MAX_PER_CHANNEL) list.removeLast();
        }
        setDirty();
    }

    /** Limpa todas mensagens de um canal específico. */
    public void clearChannel(String channel) {
        if (channel == null || channel.isEmpty()) return;
        if (channels.remove(channel) != null) setDirty();
    }

    /** Limpa TUDO (debug/admin). */
    public void clearAll() {
        if (!channels.isEmpty()) {
            channels.clear();
            setDirty();
        }
    }

    /** Lista canais conhecidos. */
    public java.util.Set<String> getChannelNames() {
        return new java.util.HashSet<>(channels.keySet());
    }

    /** Conta mensagens num canal. */
    public int countMessages(String channel) {
        LinkedList<LogEntry> list = channels.get(channel);
        return list == null ? 0 : list.size();
    }

    static {
        LiberthiaMod.LOGGER.info("[TerminalChannelStorage] r39: per-channel message storage initialized");
    }
}
