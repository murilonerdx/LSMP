package br.com.murilo.liberthia.admin.engine.videoeditor;

import java.util.List;

/**
 * DTO de projeto de vídeo enviado do frontend.
 *
 * Estrutura:
 *  - backgroundImageId: assetId retornado por POST /api/video/upload-asset
 *    (kind=image). Se null, fundo preto 1280x720 é usado.
 *  - musicId: assetId de upload-asset (kind=music). Opcional.
 *  - musicVolume: 0..2, default 0.3 (música fica abaixo das vozes)
 *  - voiceClips: lista de tracks
 *  - title: texto opcional (ainda não implementado)
 */
public record VideoProject(
        String backgroundImageId,
        String musicId,
        Float musicVolume,
        Boolean musicLoop,        // ← true: música repete até o fim do vídeo
        List<VoiceTrack> voiceClips,
        String title
) {}

/**
 * Uma faixa de voz no timeline.
 *  - clipId: id do voice clip (do voice_clips na DB)
 *  - startMs: quando começa (ms desde início do vídeo)
 *  - volume: 0..2, default 1.0
 */
record VoiceTrack(
        long clipId,
        long startMs,
        Float volume
) {}
