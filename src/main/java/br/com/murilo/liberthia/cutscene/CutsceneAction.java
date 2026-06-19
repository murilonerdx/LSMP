package br.com.murilo.liberthia.cutscene;

/** r190 — ações do sistema de cutscene/vídeo. */
public enum CutsceneAction {
    PLAY,    // inicia com URL + flags de modo
    STOP,    // para imediatamente (com fade)
    RESTART, // reinicia com a URL guardada no cliente
    LINK,    // só atualiza a URL guardada (não inicia)
    RESET    // para + limpa URL + fecha tela
}
