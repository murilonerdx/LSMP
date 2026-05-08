package br.com.murilo.liberthia.matter;

/**
 * Os 5 perfis de matéria definidos pela lore. Detectados por proporção:
 *
 * <table>
 *   <tr><th>Perfil</th><th>Condição (DM/WM/YM)</th></tr>
 *   <tr><td>DARK</td>          <td>DM > 30, WM < 15, YM < 15</td></tr>
 *   <tr><td>DARK_WHITE</td>    <td>DM ≥ 20, WM ≥ 20, YM &lt; 15</td></tr>
 *   <tr><td>WHITE</td>         <td>WM > 30, DM < 15, YM < 15</td></tr>
 *   <tr><td>YELLOW</td>        <td>YM > 30, WM < 15, DM < 15</td></tr>
 *   <tr><td>YELLOW_WHITE</td>  <td>YM ≥ 20, WM ≥ 20, DM &lt; 15</td></tr>
 *   <tr><td>NONE</td>          <td>nenhum dos acima</td></tr>
 * </table>
 *
 * <p>Quando há mais de uma combinação satisfeita simultaneamente (raro), a
 * mais "perigosa/intensa" ganha (DARK > DARK_WHITE > YELLOW_WHITE > YELLOW > WHITE).
 */
public enum MatterProfileType {
    NONE,
    DARK,         // matéria escura sozinha — selvagem
    DARK_WHITE,   // dark + branca — contido, manipulável
    WHITE,        // branca pura — esquecido, teleporte
    YELLOW,       // amarela pura — caótico emocional
    YELLOW_WHITE; // amarela + branca — estrategista frio

    public static final float MIXED_THRESHOLD = 20f;
    public static final float PURE_THRESHOLD  = 30f;
    public static final float OFF_THRESHOLD   = 15f;

    public static MatterProfileType detect(float dm, float wm, float ym) {
        boolean darkPure   = dm > PURE_THRESHOLD && wm < OFF_THRESHOLD && ym < OFF_THRESHOLD;
        boolean darkWhite  = dm >= MIXED_THRESHOLD && wm >= MIXED_THRESHOLD && ym < OFF_THRESHOLD;
        boolean whitePure  = wm > PURE_THRESHOLD && dm < OFF_THRESHOLD && ym < OFF_THRESHOLD;
        boolean yellowPure = ym > PURE_THRESHOLD && wm < OFF_THRESHOLD && dm < OFF_THRESHOLD;
        boolean yellowWhite= ym >= MIXED_THRESHOLD && wm >= MIXED_THRESHOLD && dm < OFF_THRESHOLD;

        if (darkPure)    return DARK;
        if (darkWhite)   return DARK_WHITE;
        if (yellowWhite) return YELLOW_WHITE;
        if (yellowPure)  return YELLOW;
        if (whitePure)   return WHITE;
        return NONE;
    }
}
