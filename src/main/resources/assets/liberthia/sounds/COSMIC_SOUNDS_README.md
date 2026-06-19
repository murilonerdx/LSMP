# 🎙️ Cosmic Sound Framework — r52

Estrutura de pastas e nomes EXATOS dos arquivos `.ogg` pra você criar no
ElevenLabs e dropar aqui. Quando o build for refeito, eles serão carregados
automaticamente via `sounds.json` e `ModSounds.java`.

## 📁 Estrutura de pastas (já criadas)

```
src/main/resources/assets/liberthia/sounds/
├─ whispers/
│   ├─ distant_whispers.ogg            ← obrigatório
│   ├─ distant_whispers_2.ogg          ← opcional (variação)
│   └─ distant_whispers_3.ogg          ← opcional
├─ ambience/                            ← reserva pra ambientes longos
├─ entity_sounds/
│   ├─ void_breathing.ogg
│   ├─ void_breathing_2.ogg
│   ├─ distant_scream.ogg
│   └─ distant_scream_2.ogg
├─ observation/
│   └─ audience_presence.ogg
├─ hallucinations/
│   ├─ false_footsteps.ogg
│   ├─ false_footsteps_2.ogg
│   └─ false_footsteps_3.ogg
├─ broadcasts/
│   ├─ radio_broadcast.ogg
│   ├─ radio_broadcast_2.ogg
│   └─ radio_broadcast_3.ogg
├─ sky_events/
│   └─ sky_hum.ogg
├─ tendrils/
│   ├─ tendril_movement.ogg
│   └─ tendril_movement_2.ogg
├─ eye_artifacts/
│   ├─ eye_pulse.ogg
│   └─ eye_pulse_2.ogg
├─ distortion/
│   ├─ reality_distortion.ogg
│   └─ reality_distortion_2.ogg
└─ deep_water/                          ← reserva para futuras expansões
```

## 🎚️ Especificações ElevenLabs

- **Formato**: OGG Vorbis, 44.1kHz, mono
- **Bitrate**: 96-128kbps
- **Duração**: variável por categoria (ver abaixo)

| Sound | Categoria | Duração | Voz/Tipo |
|---|---|---|---|
| distant_whispers* | whispers | 4-6s | whisper masc/fem multi-layered |
| eye_pulse* | eye_artifacts | 1-2s | wet organic heartbeat-like bass |
| false_footsteps* | hallucinations | 0.5s cada | step on different surfaces |
| void_breathing* | entity_sounds | 3-5s | glacial exhale |
| radio_broadcast* | broadcasts | 3-5s | static + corrupted voices |
| sky_hum | sky_events | 10-15s loopable | atmospheric drone 40Hz |
| tendril_movement* | tendrils | 1-3s | wet flesh stretch/scrape |
| reality_distortion* | distortion | 2-4s | digital tearing + drones |
| distant_scream* | entity_sounds | 2-3s | human-like with echo |
| audience_presence | observation | 8-12s loopable | low frequency cosmic pressure |

## 🔌 Auto-load (já implementado)

Os SoundEvents são registrados em `ModSounds.java`:
```java
COSMIC_DISTANT_WHISPERS = register("cosmic.distant_whispers");
COSMIC_EYE_PULSE = register("cosmic.eye_pulse");
COSMIC_FALSE_FOOTSTEPS = register("cosmic.false_footsteps");
COSMIC_VOID_BREATHING = register("cosmic.void_breathing");
COSMIC_RADIO_BROADCAST = register("cosmic.radio_broadcast");
COSMIC_SKY_HUM = register("cosmic.sky_hum");
COSMIC_TENDRIL_MOVEMENT = register("cosmic.tendril_movement");
COSMIC_REALITY_DISTORTION = register("cosmic.reality_distortion");
COSMIC_DISTANT_SCREAM = register("cosmic.distant_scream");
COSMIC_AUDIENCE_PRESENCE = register("cosmic.audience_presence");
```

E mapeados em `sounds.json`:
```json
"cosmic.distant_whispers": {
  "category": "ambient",
  "sounds": [
    {"name": "liberthia:whispers/distant_whispers", "stream": false},
    {"name": "liberthia:whispers/distant_whispers_2", "stream": false},
    {"name": "liberthia:whispers/distant_whispers_3", "stream": false}
  ]
}
```

Minecraft escolhe RANDOM uma variant cada vez que o sound é tocado.

## 🎯 Auto-hook (já implementado)

`HallucinationManager.force()` automaticamente chama
`CosmicSoundManager.playForHallucination(sp, type)`. Mapeamento:

| HallucinationType | Sound played |
|---|---|
| FAKE_WHISPER, NAME_WHISPER | distant_whispers |
| FAKE_FOOTSTEP | false_footsteps |
| HEARTBEAT_PULSE | eye_pulse |
| FAKE_ENTITY_PERIPHERAL, TEMPORAL_GHOST | void_breathing |
| DISTORTED_AUDIO | reality_distortion |
| REVERSE_AUDIO_PULSE | radio_broadcast |
| IMPOSSIBLE_MOON | sky_hum |
| SHADOW_MOVEMENT | tendril_movement |
| REALITY_SHAKE, SCREEN_GLITCH_BURST | reality_distortion |
| FAKE_DEATH_FLASH | distant_scream |

## 📡 Per-player garantido

Todos os sounds são tocados via `ClientboundSoundPacket` enviado APENAS
pro `sp.connection` do target — outros players próximos NÃO ouvem.
Cooldown per-player per-sound previne spam.

## 🛠️ Como usar manualmente em código

```java
import br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager;

// Toca whispers atrás do player Steve
CosmicSoundManager.playDistantWhispers(steve);
CosmicSoundManager.playEyePulse(steve);
CosmicSoundManager.playFalseFootsteps(steve, true);   // true = atrás
CosmicSoundManager.playVoidBreathing(steve);
CosmicSoundManager.playRadioBroadcast(steve);
CosmicSoundManager.playSkyHum(steve);
CosmicSoundManager.playTendrilMovement(steve);
CosmicSoundManager.playRealityDistortion(steve);
CosmicSoundManager.playDistantScream(steve);
CosmicSoundManager.playAudiencePresence(steve);
```

## ⚙️ Workflow recomendado

1. Abra ElevenLabs → Sound Generation
2. Crie um sound conforme spec na tabela acima
3. Exporte OGG (ou MP3 → converta no Audacity)
4. Renomeie exatamente conforme essa lista
5. Coloque na pasta correta
6. Re-run `./gradlew build`
7. O sound aparece automaticamente in-game (sem mudar código)

## 🔍 Verificação

Após dropar arquivos:
```bash
# Ver quais sounds estão presentes
ls src/main/resources/assets/liberthia/sounds/*/

# Listar todos os .ogg
find src/main/resources/assets/liberthia/sounds -name "*.ogg"
```

Se o arquivo está faltando, MC LOG mostra `Unable to load sound event` mas
NÃO crasha — outros sons continuam funcionando.
