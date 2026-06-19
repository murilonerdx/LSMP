package br.com.murilo.liberthia.telemetry.events;

/**
 * Tipos de eventos capturados pelo sistema nervoso do servidor.
 *
 * Categorias (cf. arquitetura do mod):
 *  - SESSION   → login / logout / AFK (controle de presença)
 *  - MOVEMENT  → posição/rotação (throttled, 100ms)
 *  - COMBAT    → dano dado/recebido, morte
 *  - INVENTORY → click, craft, container open
 *  - WORLD     → quebrar/colocar bloco, mudar dimensão
 *  - SOCIAL    → chat, proximidade de player
 *
 * Cada tipo tem flag isHighFrequency() — eventos de alta frequência (MOVE)
 * são amostrados; eventos discretos (DEATH, CHAT) são instantâneos.
 */
public enum EventType {
    // Sessão
    LOGIN(false),
    LOGOUT(false),
    AFK_START(false),
    AFK_END(false),

    // Movimento (alta frequência → throttle)
    MOVE(true),
    JUMP(false),
    FALL(false),

    // Combate
    DAMAGE_DEALT(false),
    DAMAGE_TAKEN(false),
    KILL(false),
    DEATH(false),

    // Inventário
    INVENTORY_OPEN(false),
    INVENTORY_CLOSE(false),
    SLOT_CHANGE(false),
    CRAFT(false),
    CONTAINER_OPEN(false),

    // Mundo
    BLOCK_BREAK(false),
    BLOCK_PLACE(false),
    DIMENSION_CHANGE(false),
    BIOME_CHANGE(false),

    // Social
    CHAT(false),
    PLAYER_NEARBY(false);

    private final boolean highFrequency;

    EventType(boolean highFrequency) {
        this.highFrequency = highFrequency;
    }

    public boolean isHighFrequency() { return highFrequency; }
}
