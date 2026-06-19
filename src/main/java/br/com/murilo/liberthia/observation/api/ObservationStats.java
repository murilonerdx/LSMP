package br.com.murilo.liberthia.observation.api;

/**
 * v0.1.22 r60: <b>ObservationStats</b> — atributos calculados de uma observação,
 * acumulados pelos Distortions antes da execução do Manifestation.
 *
 * <p>Inspirado em Ars Nouveau's {@code SpellStats}, mas com campos cosmic horror:
 * <ul>
 *   <li>{@code intensity} — escala do efeito (~damage)</li>
 *   <li>{@code reach} — alcance espacial em blocos</li>
 *   <li>{@code duration} — em ticks</li>
 *   <li>{@code sanityCost} — custo total (pode ser modificado por Distortions)</li>
 *   <li>{@code perceptionLevel} — quão "real" o efeito é (low = só o caster vê)</li>
 *   <li>{@code echoCount} — quantas vezes o efeito repete depois</li>
 * </ul>
 */
public final class ObservationStats {

    public final double intensity;
    public final double reach;
    public final int duration;
    public final int sanityCost;
    public final int perceptionLevel; // 0=só caster vê, 5=todos vêem
    public final int echoCount;

    private ObservationStats(Builder b) {
        this.intensity = b.intensity;
        this.reach = b.reach;
        this.duration = b.duration;
        this.sanityCost = b.sanityCost;
        this.perceptionLevel = b.perceptionLevel;
        this.echoCount = b.echoCount;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        public double intensity = 1.0;
        public double reach = 4.0;
        public int duration = 60;
        public int sanityCost = 1;
        public int perceptionLevel = 0;
        public int echoCount = 0;

        public Builder intensity(double v) { this.intensity = v; return this; }
        public Builder reach(double v) { this.reach = v; return this; }
        public Builder duration(int v) { this.duration = v; return this; }
        public Builder sanityCost(int v) { this.sanityCost = v; return this; }
        public Builder perceptionLevel(int v) { this.perceptionLevel = v; return this; }
        public Builder echoCount(int v) { this.echoCount = v; return this; }

        public Builder addIntensity(double v) { this.intensity += v; return this; }
        public Builder addReach(double v) { this.reach += v; return this; }
        public Builder addDuration(int v) { this.duration += v; return this; }
        public Builder addSanityCost(int v) { this.sanityCost += v; return this; }
        public Builder addEchoCount(int v) { this.echoCount += v; return this; }

        public ObservationStats build() { return new ObservationStats(this); }
    }
}
