package br.com.murilo.liberthia.telemetry.timeline;

import br.com.murilo.liberthia.telemetry.events.EventType;
import br.com.murilo.liberthia.telemetry.events.PlayerEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Buffer circular fixo de eventos por player. Mantém os últimos N eventos
 * em ordem cronológica. Quando enche, descarta o mais antigo (FIFO).
 *
 * Por que circular: timeline da IA precisa de "memória curta" pra inferir
 * goal/frustration, não da história inteira. Eventos antigos importantes
 * são preservados pelo TelemetryStorage (JSON em disco).
 *
 * Thread-safe via synchronized — eventos podem ser adicionados de várias
 * threads de evento Forge, mas o worker async lê em snapshot.
 */
public class Timeline {

    private final int capacity;
    private final PlayerEvent[] buffer;
    private int head = 0;      // próxima posição de escrita
    private int size = 0;
    private final Object lock = new Object();

    public Timeline(int capacity) {
        if (capacity < 16) capacity = 16;
        this.capacity = capacity;
        this.buffer = new PlayerEvent[capacity];
    }

    public void add(PlayerEvent e) {
        if (e == null) return;
        synchronized (lock) {
            buffer[head] = e;
            head = (head + 1) % capacity;
            if (size < capacity) size++;
        }
    }

    /** Snapshot dos eventos em ordem cronológica (mais antigo → mais novo). */
    public List<PlayerEvent> snapshot() {
        synchronized (lock) {
            if (size == 0) return Collections.emptyList();
            List<PlayerEvent> out = new ArrayList<>(size);
            int start = (head - size + capacity) % capacity;
            for (int i = 0; i < size; i++) {
                out.add(buffer[(start + i) % capacity]);
            }
            return out;
        }
    }

    /** Últimos N eventos (snapshot parcial). */
    public List<PlayerEvent> last(int n) {
        synchronized (lock) {
            int take = Math.min(n, size);
            List<PlayerEvent> out = new ArrayList<>(take);
            int start = (head - take + capacity) % capacity;
            for (int i = 0; i < take; i++) {
                out.add(buffer[(start + i) % capacity]);
            }
            return out;
        }
    }

    /** Eventos do timestamp X até agora. */
    public List<PlayerEvent> since(long sinceTs) {
        List<PlayerEvent> all = snapshot();
        List<PlayerEvent> out = new ArrayList<>();
        for (PlayerEvent e : all) {
            if (e.getTimestamp() >= sinceTs) out.add(e);
        }
        return out;
    }

    public List<PlayerEvent> filter(Predicate<PlayerEvent> pred) {
        List<PlayerEvent> all = snapshot();
        List<PlayerEvent> out = new ArrayList<>();
        for (PlayerEvent e : all) if (pred.test(e)) out.add(e);
        return out;
    }

    public int countByType(EventType type, long sinceTs) {
        int n = 0;
        for (PlayerEvent e : snapshot()) {
            if (e.getType() == type && e.getTimestamp() >= sinceTs) n++;
        }
        return n;
    }

    public int size() { synchronized (lock) { return size; } }
    public int capacity() { return capacity; }

    public PlayerEvent latest() {
        synchronized (lock) {
            if (size == 0) return null;
            int idx = (head - 1 + capacity) % capacity;
            return buffer[idx];
        }
    }
}
