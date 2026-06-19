# 🎙️ ELEVENLABS AUDIO MANIFEST — Cosmic Horror Expansion (r45)

Lista completa de arquivos `.ogg` pra criar no ElevenLabs e dropar nas pastas certas.

## 📁 Estrutura de pastas

Todos os áudios vão em:
```
src/main/resources/assets/liberthia/sounds/cosmic/
```

Após colocar os arquivos, registrar em:
```
src/main/resources/assets/liberthia/sounds.json
```

(Já existe — só adicionar as entries no final)

---

## 🗣️ VOZES HUMANAS (whispers, narrador insano)

| Arquivo | Voz | Texto/Direção ElevenLabs | Quando toca |
|---|---|---|---|
| `whisper_name_1.ogg` | whisper masc | *"... seu nome ..."* (sussurrado, ecoando) | NAME_WHISPER hallucination |
| `whisper_name_2.ogg` | whisper fem | *"... volta ..."* | name whisper tier 2 |
| `whisper_name_3.ogg` | whisper child | *"... me ajuda ..."* | name whisper tier 3 (raro) |
| `whisper_behind_1.ogg` | masc grave | *"... atrás de você ..."* (boca colada no mic) | shadow movement |
| `whisper_behind_2.ogg` | fem aguda | *"... olha pra cima ..."* | random whisper |
| `whisper_behind_3.ogg` | distorcida | *"... não estamos sozinhos ..."* | high horror tier |
| `whisper_chant_1.ogg` | multi-vozes | latim ritualistico: *"Ad noctem aeternum"* | ritual events |
| `whisper_chant_2.ogg` | multi-vozes | *"Tekeli-li, tekeli-li"* (Lovecraft ref) | manifestation phase |
| `whisper_chant_3.ogg` | reverso | qualquer frase pra REVERSE depois | reverse audio pulse |
| `whisper_giggle_child.ogg` | criança | risada baixinha lenta | random tier 3 |
| `whisper_breathing.ogg` | respiração | respiração ofegante 5s | static stalker present |
| `whisper_static.ogg` | rádio | static + voz quebrada *"...help..."* | reality glitch |

---

## 👹 CRIATURAS / DEMÔNIOS (vocalizações monstruosas)

| Arquivo | Voz | Descrição | Quando toca |
|---|---|---|---|
| `demon_roar_1.ogg` | grunhido grave | rugido de 3s reverberado | Entity Manifestation phase enter |
| `demon_roar_2.ogg` | rugido seco | bestial curto | Phantom appears |
| `demon_growl_low.ogg` | rosnado contínuo | 2s low frequency growl | Cosmic Horror MANIFESTATION ambient |
| `demon_screech.ogg` | grito agudo | piercing scream 1.5s | Cosmic Collapse explosion |
| `demon_breath.ogg` | respiração fria | exhale glacial 3s | Watcher Stalker proximity |
| `demon_laugh.ogg` | risada distorcida | dark laugh 2-3s | random tier 3 hallucination |
| `creature_clicking.ogg` | clicking insect | clicks rápidos 2s | Loom Dimension ambient |
| `creature_chittering.ogg` | enxame | chittering várias bocas | Reality Rupture phase |
| `creature_swallowing.ogg` | engolindo | sons orgânicos viscerais | Cursed Effigy passive |
| `creature_bone_crack.ogg` | osso quebrando | snap dry | Targeted curse tier 3 |

---

## 🌀 AMBIENTES CÓSMICOS (drones, atmosfera)

| Arquivo | Tipo | Descrição | Quando toca |
|---|---|---|---|
| `cosmic_drone_low.ogg` | drone bass | 10s loopable 40Hz drone | Cosmic Horror SUBTLE_PRESENCE |
| `cosmic_drone_med.ogg` | drone | 10s mid-freq 200Hz | DIMENSIONAL_CORRUPTION |
| `cosmic_drone_high.ogg` | drone tense | 10s high-freq tension | REALITY_RUPTURE |
| `cosmic_drone_void.ogg` | drone void | 15s deep void hum | ENTITY_MANIFESTATION |
| `cosmic_heartbeat.ogg` | batida coração | 1 beat ~0.7s | HEARTBEAT_PULSE hallucination |
| `cosmic_heartbeat_fast.ogg` | batida rápida | 1 beat ~0.4s | high paranoia |
| `cosmic_silence.ogg` | "silêncio" antes boom | -3s anti-sound | Phase 5 collapse pre-boom |
| `cosmic_dimensional_tear.ogg` | rasgo dimensional | 4s ripping reality | Phase 3 Rupture enter |
| `cosmic_void_collapse.ogg` | implosão | 3s gravitational implosion | Cosmic Collapse Phase 5 |
| `cosmic_echo_chamber.ogg` | eco ambiente | indefinido echoey ambient | Spirit World |

---

## 🔮 SONS DE ITEMS / RITUAIS

| Arquivo | Descrição | Item/Trigger |
|---|---|---|
| `item_cursed_pulse.ogg` | pulsação amaldiçoada 1s | Cursed item tick |
| `item_effigy_whisper.ogg` | sussurro próximo (íntimo) | Cursed Effigy held |
| `item_phantom_bell.ogg` | sino fantasma 2s | Phantom Caller use |
| `item_watcher_mark.ogg` | tom místico marcando | Watcher Mark applied |
| `item_lens_open.ogg` | abertura de portal | Vulto Lens use |
| `item_crown_activate.ogg` | poder cósmico desperta | Insanity Crown activate |
| `item_tentacle_grab.ogg` | viscoso agarrando | Tendril Sigil hit |
| `item_voice_distort.ogg` | voz se quebrando | Voice Curse Amulet equip |
| `ritual_chant_start.ogg` | cântico ritualistico | Ritual Circle activate |
| `ritual_chant_end.ogg` | resolução do ritual | Ritual complete |
| `ritual_failed.ogg` | dissonância | Ritual failed |
| `ritual_summon.ogg` | invocação massiva | Boss summon |

---

## 😱 SONS DE MEDO / IMPACTO

| Arquivo | Descrição | Trigger |
|---|---|---|
| `fear_pulse_1.ogg` | susto súbito agudo | random scary moment |
| `fear_pulse_2.ogg` | sussurro próximo + click | proximity to fake entity |
| `fear_camera_shake.ogg` | sub-bass rumble 2s | Reality Shake hallucination |
| `fear_static_burst.ogg` | static noise 0.5s | Screen Glitch Burst |
| `fear_glass_break.ogg` | vidro estilhaça | Fake Death Flash |
| `fear_doorknock.ogg` | batida em porta | random ambient terror |
| `fear_footstep_creak.ogg` | passo madeira velha | Fake Footstep behind |
| `fear_breath_close.ogg` | respiração no ouvido | Static Stalker proximity |
| `fear_static_voice.ogg` | static + voz fragmentada | Distorted Audio |

---

## 🔢 TOTAL: 53 áudios

| Categoria | Quantidade |
|---|---|
| Whispers/Vozes | 12 |
| Criaturas/Demônios | 10 |
| Ambientes Cósmicos | 10 |
| Items/Rituais | 12 |
| Medo/Impacto | 9 |

---

## ⚙️ Configurações ElevenLabs recomendadas

- **Voice Settings:** Stability 30-40 (mais expressivo), Clarity 75
- **Style Exaggeration:** 50-70 pra whispers, 80+ pra demons
- **Output:** OGG Vorbis se possível, senão MP3 → converter pra OGG via Audacity
- **Sample rate:** 44.1kHz mono
- **Bitrate:** 96-128kbps OGG (suficiente pra MC)

## 📝 Como adicionar ao mod

1. **Renomeie** os áudios exatamente como listados acima
2. **Coloque** em `src/main/resources/assets/liberthia/sounds/cosmic/`
3. **Edite** `src/main/resources/assets/liberthia/sounds.json` adicionando:
   ```json
   "cosmic.whisper_name_1": {
     "sounds": ["liberthia:cosmic/whisper_name_1"]
   }
   ```
4. **Registre** em `ModSounds.java`:
   ```java
   public static final RegistryObject<SoundEvent> COSMIC_WHISPER_NAME_1 =
       SOUNDS.register("cosmic.whisper_name_1",
           () -> SoundEvent.createVariableRangeEvent(
               new ResourceLocation(MODID, "cosmic.whisper_name_1")));
   ```
5. **Use** no código:
   ```java
   level.playSound(null, pos, ModSounds.COSMIC_WHISPER_NAME_1.get(),
       SoundSource.AMBIENT, 1.0F, 1.0F);
   ```

## 💡 Dicas de direção ElevenLabs

- Pra **whispers**: usa voz "Adam" ou "Antoni" em volume MUITO baixo, com efeito de "intimate"
- Pra **demons**: voz "Domi" ou criar Voice Custom com pitch -20 + reverb
- Pra **criança sinistra**: voz infantil + slow speed + reverse algumas
- Pra **chant latim**: usa Multi-Voice feature pra sobrepor 3-4 vozes simultaneamente
- **PROCESSAR depois**: pega no Audacity, aplica reverb profundo (cathedral), lowpass 6kHz, e aleatorize pitch ±10%
