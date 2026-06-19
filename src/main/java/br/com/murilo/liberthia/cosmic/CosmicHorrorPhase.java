package br.com.murilo.liberthia.cosmic;

/**
 * v0.1.22 r38: Enum das 4 fases do Sistema de Cosmic Horror Ambiental.
 *
 * <h2>r38 CHANGES</h2>
 * <ul>
 *   <li>Cada phase agora dura 60s (era 30s); total = 4 min antes de TP pra
 *       spirit world.</li>
 *   <li>Intensities AUMENTADAS — horror cresce mais rápido e mais intenso
 *       até virar quase incontrolável na MANIFESTATION.</li>
 *   <li>Novos params: {@code mouseSpinChance}, {@code starePulseChance},
 *       {@code paranoiaChance}, {@code footstepChance} — controlam novos
 *       eventos paranóicos.</li>
 * </ul>
 *
 * <p>A progressão é UNIDIRECIONAL — uma vez entrou em DIMENSIONAL_CORRUPTION,
 * não volta pra SUBTLE_PRESENCE sem reset explícito.
 *
 * <p>Phase params (todos client-driven a partir do intensity 0-1):
 * <ul>
 *   <li><b>fogIntensity</b>: multiplicador do fog (0=normal, 1=denso)</li>
 *   <li><b>chromaticAbb</b>: força do shader chromatic aberration (0-1)</li>
 *   <li><b>screenShake</b>: amplitude do shake (0-1)</li>
 *   <li><b>distortionAmount</b>: força de UV warp post-process (0-1)</li>
 *   <li><b>particleRate</b>: particles/sec emitidos perto do player</li>
 *   <li><b>whisperChance</b>: chance/sec de whisper aleatório (0-1)</li>
 *   <li><b>skyRiftAlpha</b>: opacidade do rasgo no céu (0-1)</li>
 *   <li><b>eyesInSkyCount</b>: número de olhos flutuantes</li>
 *   <li><b>cameraJitterStrength</b>: jitter constante da câmera</li>
 *   <li><b>mouseSpinChance</b>: r38 — chance/sec do mouse girar sozinho (0-1)</li>
 *   <li><b>starePulseChance</b>: r38 — chance/sec de mobs/players encararem (0-1)</li>
 *   <li><b>paranoiaChance</b>: r38 — chance/sec de spawn de monstro fake (0-1)</li>
 *   <li><b>footstepChance</b>: r38 — chance/sec de passos atrás do player (0-1)</li>
 * </ul>
 */
public enum CosmicHorrorPhase {

    /** Estado dormente — nada acontece. */
    DORMANT(0, 0, 0, 0, 0, 0, 0, 0, 0,   0, 0, 0, 0),

    /** Phase 1 (0-60s): presença sutil, jogador SENTE algo mas não vê. */
    SUBTLE_PRESENCE(
            0.30F, 0.10F, 0.04F, 0.0F,   1.0F, 0.02F, 0.0F, 0, 0.02F,
            0.0F, 0.05F, 0.0F, 0.10F),

    /** Phase 2 (60-120s): corrupção dimensional — partículas, aberração, fog pulsa. */
    DIMENSIONAL_CORRUPTION(
            0.60F, 0.35F, 0.12F, 0.20F,   6.0F, 0.06F, 0.20F, 2, 0.06F,
            0.05F, 0.15F, 0.10F, 0.25F),

    /** Phase 3 (120-180s): ruptura — rasgos no céu, distorção agressiva, paranoia. */
    REALITY_RUPTURE(
            0.85F, 0.65F, 0.35F, 0.55F,   16.0F, 0.12F, 0.65F, 4, 0.16F,
            0.20F, 0.30F, 0.25F, 0.50F),

    /** Phase 4 (180-240s): manifestação plena — entidade aparece, distorção máxima, full paranoia. */
    ENTITY_MANIFESTATION(
            1.0F, 0.95F, 0.55F, 0.95F,   32.0F, 0.20F, 1.0F, 8, 0.32F,
            0.45F, 0.55F, 0.50F, 0.80F);

    public final float fogIntensity;
    public final float chromaticAbb;
    public final float screenShake;
    public final float distortionAmount;
    public final float particleRate;
    public final float whisperChance;
    public final float skyRiftAlpha;
    public final int   eyesInSkyCount;
    public final float cameraJitterStrength;

    // r38: novos params de paranoia
    public final float mouseSpinChance;
    public final float starePulseChance;
    public final float paranoiaChance;
    public final float footstepChance;

    CosmicHorrorPhase(float fog, float chroma, float shake, float distort,
                      float particles, float whisper, float skyRift,
                      int eyes, float jitter,
                      float mouseSpin, float stare, float paranoia, float footstep) {
        this.fogIntensity = fog;
        this.chromaticAbb = chroma;
        this.screenShake = shake;
        this.distortionAmount = distort;
        this.particleRate = particles;
        this.whisperChance = whisper;
        this.skyRiftAlpha = skyRift;
        this.eyesInSkyCount = eyes;
        this.cameraJitterStrength = jitter;
        this.mouseSpinChance = mouseSpin;
        this.starePulseChance = stare;
        this.paranoiaChance = paranoia;
        this.footstepChance = footstep;
    }

    /** Linear interpolation entre fases pra transições suaves. */
    public static CosmicHorrorPhase lerpVisual(CosmicHorrorPhase from,
                                                CosmicHorrorPhase to, float t) {
        // Apenas pra display — não muda enum; client interpola valores
        return to; // mantém enum, mas params são interpolados client-side
    }

    public static CosmicHorrorPhase byOrdinal(int i) {
        var all = values();
        if (i < 0 || i >= all.length) return DORMANT;
        return all[i];
    }
}
