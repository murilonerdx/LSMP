package br.com.murilo.liberthia.observation.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * v0.1.22 r60: <b>ObservationPart</b> — base de qualquer "glyph" do sistema
 * Observation Casting.
 *
 * <h2>Inspirado em (não copiado)</h2>
 * Ars Nouveau's {@code AbstractSpellPart}, mas reframed pra horror cósmico:
 * em vez de "spell parts" temos "perception parts". 3 tipos:
 *
 * <ul>
 *   <li>{@link WatchMethod} — como o feitiço é "observado" (entry point)</li>
 *   <li>{@link Manifestation} — o que se manifesta (effect)</li>
 *   <li>{@link Distortion} — modifica intensidade/escopo (augment)</li>
 * </ul>
 *
 * <h2>Filosofia diferente</h2>
 * Ars Nouveau: glyphs combinam em recipe linear.
 * <br>
 * Observation: glyphs <b>requerem condições de percepção</b> — você só pode
 * castar uma Distorção de Gravidade se observou um objeto caindo nas últimas
 * 5 segundos. O mundo é o input.
 *
 * <p>Cada part tem:
 * <ul>
 *   <li>{@link #id()} — ResourceLocation namespace:name</li>
 *   <li>{@link #typeIndex()} — 1=WatchMethod, 5=Manifestation, 10=Distortion</li>
 *   <li>{@link #sanityCost()} — quanto custa em sanidade</li>
 *   <li>{@link #incompatibleWith()} — outros parts que não podem coexistir</li>
 * </ul>
 */
public abstract class ObservationPart {

    private final ResourceLocation id;
    private final String displayName;

    protected ObservationPart(ResourceLocation id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public final ResourceLocation id() { return id; }

    public final Component displayComponent() {
        return Component.literal(displayName);
    }

    /** 1=WatchMethod, 5=Manifestation, 10=Distortion. */
    public abstract int typeIndex();

    /** Custo BASE em sanity por uso. Distortions custam via getCostForPart(parent). */
    public abstract int sanityCost();

    /** r61: Custo BASE em Source (mana). Default 5x sanity cost. Override pra customizar. */
    public int sourceCost() {
        return Math.max(1, sanityCost() * 5);
    }

    /** Parts incompatíveis (não podem coexistir no mesmo recipe). */
    public Set<ResourceLocation> incompatibleWith() {
        return Collections.emptySet();
    }

    /** Lore poético (sem explicar mecânica diretamente). */
    public abstract List<Component> lore();

    /** Cor associada (pra particles). */
    public int color() { return 0x9d4dd6; }

    /** r71: Classe elemental do feitiço. Default NONE. Override em subclasses. */
    public SpellClass spellClass() { return SpellClass.NONE; }

    /**
     * Mutado pelo SpellStats — pode modificar atributos antes da execução.
     * Override em Distortion subclasses.
     */
    public void applyToStats(ObservationStats.Builder builder, @Nullable ObservationPart augmented) {
        // default no-op
    }
}
