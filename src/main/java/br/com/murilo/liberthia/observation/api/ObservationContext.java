package br.com.murilo.liberthia.observation.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r60: <b>ObservationContext</b> — state per-cast, inspired by AN's
 * {@code SpellContext}. Tracks execution position, level, caster, attachments,
 * cancel state.
 *
 * <p>Pattern crítico: {@code attachments} é o map onde Manifestations podem
 * armazenar state pra que Manifestations subsequentes no recipe leiam. Ex:
 * primeira Manifestation marca um target, segunda Manifestation lê e
 * intensifica baseado no marker.
 */
public final class ObservationContext {

    private final ObservationSpell spell;
    private final ServerLevel level;
    private final ServerPlayer caster;
    private int currentIndex = -1;
    private boolean canceled = false;
    private @Nullable CancelReason cancelReason = null;
    private final Map<ResourceLocation, Object> attachments = new HashMap<>();
    private @Nullable ObservationContext parent;

    public ObservationContext(ObservationSpell spell, ServerLevel level, ServerPlayer caster) {
        this.spell = spell;
        this.level = level;
        this.caster = caster;
    }

    public ObservationSpell spell() { return spell; }
    public ServerLevel level() { return level; }
    public ServerPlayer caster() { return caster; }

    /** Avança índice e retorna próxima part, ou null se acabou. */
    public @Nullable ObservationPart nextPart() {
        currentIndex++;
        if (currentIndex >= spell.recipe().size()) return null;
        return spell.recipe().get(currentIndex);
    }

    public boolean hasNextPart() {
        return currentIndex + 1 < spell.recipe().size();
    }

    public int currentIndex() { return currentIndex; }

    /** Cancela a execução. */
    public void cancel(CancelReason reason) {
        this.canceled = true;
        this.cancelReason = reason;
    }
    public boolean isCanceled() { return canceled; }
    public @Nullable CancelReason cancelReason() { return cancelReason; }

    // Attachments — Manifestations podem se comunicar
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getAttachment(ResourceLocation key) {
        return (T) attachments.get(key);
    }
    public void setAttachment(ResourceLocation key, Object value) {
        attachments.put(key, value);
    }
    public boolean hasAttachment(ResourceLocation key) {
        return attachments.containsKey(key);
    }

    // Nested casting (chain spells)
    public ObservationContext makeChildContext(ObservationSpell subSpell) {
        ObservationContext child = new ObservationContext(subSpell, level, caster);
        child.parent = this;
        return child;
    }
    public @Nullable ObservationContext parent() { return parent; }

    public enum CancelReason {
        NOT_ENOUGH_SANITY,
        FORBIDDEN_DIMENSION,
        WATCH_FAILED,
        LISTENER_BLOCKED,
        MANUAL_CANCEL
    }
}
