package br.com.murilo.liberthia.cosmic.scare;

/**
 * r173: Tipos de "quebra de 4ª parede" disparados por {@code /liberthia scare}.
 * Tudo client-side e per-player — não toca o mundo, só a percepção do jogador.
 */
public enum ScareType {
    /** Tela cheia vermelha "EMERGENCY ALERT / CIVIL DANGER". */
    ALERT,
    /** Tela de crash FALSA (jogador fecha no botão/ESC). */
    CRASH,
    /** Tela de "Disconnected" FALSA. */
    KICK,
    /** Título/texto tremendo no centro. */
    SHAKE,
    /** Estática: chiado + ruído branco piscando na tela. */
    STATIC,
    /** Flash de um rosto de terror (variant = índice da imagem). */
    FLASH,
    // ── r175: novas quebras de 4ª parede / loucura ──
    /** Sussurro: texto fantasma surge/some no centro. */
    WHISPER,
    /** Apagão: tela 100% preta por alguns segundos. */
    BLACKOUT,
    /** Olhos brilhantes piscando nos cantos da tela. */
    EYES,
    /** Pulso cardíaco: vinheta vermelha pulsando + batida. */
    HEARTBEAT,
    /** Glitch: split RGB + tearing + barras coloridas. */
    GLITCH,
    /** Tela "Você Morreu" FALSA (botão volta ao jogo). */
    FAKEDEATH,
    /** Tela azul da morte (BSOD) FALSA. */
    BSOD,
    /** Mensagem de chat FALSA injetada no cliente. */
    FAKECHAT,
    /** Releitura da pasta liberthia_faces/ (carrega imagens novas sem reiniciar). */
    RELOADFACES,
    // ── r177: +8 quebras de 4ª parede ──
    /** Tela TRINCADA (vidro/monitor rachado) — padrão estável + som de vidro. */
    CRACK,
    /** Contagem regressiva ameaçadora no centro (usa a duração). */
    COUNTDOWN,
    /** Chat FALSO de entrou/saiu/morreu (variant escolhe o modo; text = nome). */
    FAKEJOIN,
    /** Marca d'água gigante translúcida (text) surgindo/sumindo. */
    WATERMARK,
    /** Um olho gigante abre, te encara e pisca no centro da tela. */
    EYE,
    /** Visão de túnel: a tela fecha em preto (desmaio) e reabre. */
    TUNNEL,
    /** Flash branco de "foto tirada" + mensagem de screenshot salvo. */
    SCREENSHOT,
    /** Barra de varredura rolando + desync RGB (perda de sinal de TV). */
    SCANROLL,
    // ── r177: Grupo A (+6) ──
    /** Fake "Não Respondendo" — tela parece travada com spinner. */
    NOTRESPONDING,
    /** Mundo DE CABEÇA PRA BAIXO (roll 180 da câmera). */
    REVERSE,
    /** HUD corrompido — vida/fome/hotbar bugando com valores errados. */
    CORRUPTHUD,
    /** Legenda no rodapé te descrevendo (text). */
    NARRATOR,
    /** Cursor de mouse fantasma se movendo sozinho. */
    CURSOR,
    /** Suas coords/nome aparecem rabiscados na tela (alguém anotando). */
    FALSECOORDS,
    /** r177: PISCADA — pálpebras pretas fecham e abrem (como um piscar de olhos). */
    BLINK,
    // ── r178: +4 ──
    /** Popup falso de "bateria fraca" do PC. */
    LOWBATTERY,
    /** Som de notificação + toast falso "1 nova mensagem". */
    DISCORDPING,
    /** HUD espelhado horizontalmente. */
    MIRROR,
    /** Texto se digitando sozinho no chat (alguém digitando do outro lado; text). */
    TYPETEXT;

    public static ScareType byOrdinal(int i) {
        ScareType[] v = values();
        return (i < 0 || i >= v.length) ? ALERT : v[i];
    }
}
