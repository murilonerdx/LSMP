package br.com.murilo.liberthia.cosmic.hallucination;

/**
 * v0.1.22 r40: Tipos de hallucination que o server pode injetar no client.
 *
 * <h2>Filosofia</h2>
 * <p>Hallucinations são <b>client-only</b> — outros players próximos NÃO veem
 * ou ouvem nada do que o player afetado experimenta. O server decide QUANDO
 * e QUE tipo, manda packet, client renderiza localmente.
 *
 * <p>Cada tipo tem um payload de até 4 floats + 2 ints + 1 string pra config:
 * <ul>
 *   <li><b>x, y, z</b> — posição relativa ao player (offset world coords)</li>
 *   <li><b>duration</b> — ticks de duração (60 = 3s típico)</li>
 *   <li><b>intensity</b> — 0-1 escala (volume/alpha/size)</li>
 *   <li><b>variant</b> — int sub-tipo (ex: qual entidade fake)</li>
 *   <li><b>aux</b> — string extra (chat msg, sound id, etc)</li>
 * </ul>
 */
public enum HallucinationType {

    /** Entidade fake na periferia da visão — sombra humanoide. */
    FAKE_ENTITY_PERIPHERAL,

    /** Som de passos atrás do player. */
    FAKE_FOOTSTEP,

    /** Whisper sussurrado privado. */
    FAKE_WHISPER,

    /** Mensagem fake aparece no chat (só pra esse player). */
    FAKE_CHAT_MESSAGE,

    /** Hearts/HP bar pisca como se tivesse tomado dano (mas sem dano real). */
    FAKE_DAMAGE_INDICATOR,

    /** Tela vermelha flash como se morreu (mas player não morreu). */
    FAKE_DEATH_FLASH,

    /** Glitch burst — chromatic aberration intenso por 0.5s. */
    SCREEN_GLITCH_BURST,

    /** Pulse de áudio reverso por 1-2s. */
    REVERSE_AUDIO_PULSE,

    /** Som distorcido/pitch-shifted (low frequency drone). */
    DISTORTED_AUDIO,

    /** Ghost de outro player aparece e desaparece em 1s. */
    TEMPORAL_GHOST,

    /** Block flash visual — block parece ter virado outro coisa por 0.5s. */
    FAKE_BLOCK_FLASH,

    /** Light source fake — área iluminada onde não tem luz. */
    FALSE_LIGHT,

    /** Lua impossível no céu (gigante, vermelha, num lugar errado). */
    IMPOSSIBLE_MOON,

    /** Sombra se move no canto da tela. */
    SHADOW_MOVEMENT,

    /** Heartbeat acelerando — som batendo no headset. */
    HEARTBEAT_PULSE,

    /** Inventory hallucination — item fake aparece no slot por 1 frame. */
    FAKE_INVENTORY_ITEM,

    /** Voz chamando seu nome (1 vez, longe). */
    NAME_WHISPER,

    /** Tela treme + screen tearing 1s. */
    REALITY_SHAKE;

    private static final HallucinationType[] VALUES = values();

    public static HallucinationType byOrdinal(int i) {
        if (i < 0 || i >= VALUES.length) return FAKE_WHISPER;
        return VALUES[i];
    }

    public int ord() { return ordinal(); }
}
