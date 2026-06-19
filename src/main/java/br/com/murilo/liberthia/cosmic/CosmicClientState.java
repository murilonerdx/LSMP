package br.com.murilo.liberthia.cosmic;

/**
 * v0.1.22 r35: Espelho client-side do estado Cosmic Horror. Todos os renderers
 * (fog, screen overlay, sky rift, camera shake, eyes-in-sky) leem daqui.
 *
 * <h2>Smooth interpolation</h2>
 * Quando server troca de phase, valores client SAO INTERPOLADOS suavemente
 * via {@link #tickInterpolation} chamado 60×/s pelo client tick.
 */
public final class CosmicClientState {

    /** Phase recebida do server (alvo). */
    private static volatile CosmicHorrorPhase serverPhase = CosmicHorrorPhase.DORMANT;
    /** Intensity recebida do server (0-1, ramp dentro da phase). */
    private static volatile float serverIntensity = 0.0F;

    // ─── Valores INTERPOLADOS (suaves) — esses que renderers leem ───
    public static float currentFogIntensity = 0.0F;
    public static float currentChromaticAbb = 0.0F;
    public static float currentScreenShake = 0.0F;
    public static float currentDistortion = 0.0F;
    public static float currentParticleRate = 0.0F;
    public static float currentSkyRiftAlpha = 0.0F;
    public static float currentCameraJitter = 0.0F;
    public static int   currentEyesCount = 0;

    /** Animation time global pra shaders. Incrementa a cada client tick. */
    public static float animTime = 0.0F;

    private CosmicClientState() {}

    /** Chamado pelo packet handler quando server sync. */
    public static void update(CosmicHorrorPhase phase, float intensity) {
        serverPhase = phase;
        serverIntensity = intensity;
    }

    /** Chamado a cada client tick (50ms) — interpola valores suavemente. */
    public static void tickInterpolation() {
        animTime += 0.05F;
        if (animTime > 10000) animTime -= 10000;

        // Target values pela phase atual scaled by intensity
        float targetFog = serverPhase.fogIntensity * serverIntensity;
        float targetChroma = serverPhase.chromaticAbb * serverIntensity;
        float targetShake = serverPhase.screenShake * serverIntensity;
        float targetDistort = serverPhase.distortionAmount * serverIntensity;
        float targetParticle = serverPhase.particleRate * serverIntensity;
        float targetSky = serverPhase.skyRiftAlpha * serverIntensity;
        float targetJitter = serverPhase.cameraJitterStrength * serverIntensity;
        int targetEyes = (int) (serverPhase.eyesInSkyCount * serverIntensity);

        // Lerp suave 5% por tick (20 ticks pra ~63% do caminho, 60 pra ~95%)
        float t = 0.05F;
        currentFogIntensity = lerp(currentFogIntensity, targetFog, t);
        currentChromaticAbb = lerp(currentChromaticAbb, targetChroma, t);
        currentScreenShake = lerp(currentScreenShake, targetShake, t);
        currentDistortion = lerp(currentDistortion, targetDistort, t);
        currentParticleRate = lerp(currentParticleRate, targetParticle, t);
        currentSkyRiftAlpha = lerp(currentSkyRiftAlpha, targetSky, t);
        currentCameraJitter = lerp(currentCameraJitter, targetJitter, t);
        currentEyesCount = targetEyes; // int, sem lerp
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static CosmicHorrorPhase getServerPhase() {
        return serverPhase;
    }

    public static boolean isActive() {
        return serverPhase != CosmicHorrorPhase.DORMANT;
    }
}
