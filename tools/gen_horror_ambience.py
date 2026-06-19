"""r176: gerador PROCEDURAL de ambientacao de terror (Liberthia).

Sintetiza sons de terror do zero com numpy/scipy e exporta OGG/Vorbis MONO
(formato que o Minecraft exige p/ som posicional). Offline, gratis, deterministico
(mesma --seed = mesmo som). Encaixa no padrao dos outros tools/gen_*.py.

Catalogo (presets):
  drones/leitos (loop):  dread_drone, sub_rumble, sky_hum, void_breathing,
                         wind_howl, audience_presence, distant_whispers,
                         granular_texture, heartbeat, false_footsteps
  one-shots:             riser, reverse_stinger, stinger_hit, metallic_groan,
                         bell_toll, static_burst, tendril_movement

Uso:
  python tools/gen_horror_ambience.py                 # gera TUDO + registra
  python tools/gen_horror_ambience.py --list          # lista presets
  python tools/gen_horror_ambience.py --only dread_drone,heartbeat
  python tools/gen_horror_ambience.py --seed 7        # varia o ruido
  python tools/gen_horror_ambience.py --no-register   # so gera os .ogg
  python tools/gen_horror_ambience.py --variants 3    # N takes por preset (_1.._N)

Saida:
  src/main/resources/assets/liberthia/sounds/ambience/<nome>.ogg
  + merge em sounds.json   (chave "ambience.<nome>")
  + bloco marcado em ModSounds.java  (AMBIENCE_<NOME>)
"""
import argparse
import json
import math
import re
import shutil
import subprocess
from pathlib import Path

import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 44100
TWO_PI = 2.0 * math.pi

ROOT = Path(__file__).resolve().parent.parent
SOUND_DIR = ROOT / "src/main/resources/assets/liberthia/sounds"
AMB_DIR = SOUND_DIR / "ambience"
SOUNDS_JSON = ROOT / "src/main/resources/assets/liberthia/sounds.json"
MODSOUNDS = ROOT / "src/main/java/br/com/murilo/liberthia/registry/ModSounds.java"

MARK_BEGIN = "    // >>> gen_horror_ambience (auto) — nao editar a mao"
MARK_END = "    // <<< gen_horror_ambience"

# ════════════════════════════════════════════════════════════════════════════
#  PRIMITIVAS DSP
# ════════════════════════════════════════════════════════════════════════════

def t_axis(dur):
    return np.arange(int(SR * dur)) / SR


def sine(freq, dur, phase=0.0):
    return np.sin(TWO_PI * freq * t_axis(dur) + phase)


def saw(freq, dur):
    t = t_axis(dur)
    return 2.0 * (t * freq - np.floor(0.5 + t * freq))


def glide_tone(f0, f1, dur, curve=1.0):
    """Tom com a frequencia variando de f0 a f1 (curve>1 = acelera no fim)."""
    n = int(SR * dur)
    frac = (np.arange(n) / max(1, n - 1)) ** curve
    inst = f0 + (f1 - f0) * frac
    phase = TWO_PI * np.cumsum(inst) / SR
    return np.sin(phase)


def fm(carrier, ratio, index, dur, index_env=None):
    """Sintese FM — bom p/ metalico/sino/dissonante (timbres inarmonicos)."""
    t = t_axis(dur)
    idx = index if index_env is None else index * index_env
    return np.sin(TWO_PI * carrier * t + idx * np.sin(TWO_PI * ratio * carrier * t))


def white(dur, rng):
    return rng.uniform(-1.0, 1.0, int(SR * dur))


def pink(dur, rng):
    """Ruido rosa (1/f) via shaping no dominio da frequencia."""
    w = white(dur, rng)
    X = np.fft.rfft(w)
    f = np.fft.rfftfreq(len(w), 1.0 / SR)
    f[0] = 1.0
    X = X / np.sqrt(f)
    out = np.fft.irfft(X, n=len(w))
    return out / (np.max(np.abs(out)) + 1e-9)


def brown(dur, rng):
    """Ruido marrom (1/f^2) — grave, bom p/ rumble."""
    b = np.cumsum(white(dur, rng))
    b -= np.mean(b)
    return b / (np.max(np.abs(b)) + 1e-9)


def _butter(sig, cutoff, btype, order=4):
    nyq = SR / 2.0
    if isinstance(cutoff, (list, tuple)):
        wn = [float(np.clip(c / nyq, 1e-4, 0.999)) for c in cutoff]
    else:
        wn = float(np.clip(cutoff / nyq, 1e-4, 0.999))
    b, a = signal.butter(order, wn, btype=btype)
    return signal.filtfilt(b, a, sig)


def lowpass(s, c, o=4):
    return _butter(s, c, "low", o)


def highpass(s, c, o=4):
    return _butter(s, c, "high", o)


def bandpass(s, lo, hi, o=4):
    return _butter(s, [lo, hi], "band", o)


def env_shape(n, points):
    """Envelope linear por pontos [(frac_tempo 0..1, nivel), ...]."""
    xs = [int(p[0] * (n - 1)) for p in points]
    ys = [p[1] for p in points]
    return np.interp(np.arange(n), xs, ys)


def fade(sig, fin=0.02, fout=0.05):
    n = len(sig)
    e = np.ones(n)
    fi, fo = int(fin * SR), int(fout * SR)
    if fi > 0:
        e[:fi] = np.linspace(0, 1, fi)
    if fo > 0:
        e[-fo:] = np.linspace(1, 0, fo)
    return sig * e


def tremolo(sig, rate, depth, phase=0.0):
    t = np.arange(len(sig)) / SR
    m = 1.0 - depth * 0.5 * (1.0 + np.sin(TWO_PI * rate * t + phase))
    return sig * m


def lfo_cut(rate, n, lo, hi, phase=0.0):
    """LFO mapeado p/ uma faixa [lo,hi] (ex.: cutoff de filtro variando)."""
    t = np.arange(n) / SR
    u = 0.5 * (1.0 + np.sin(TWO_PI * rate * t + phase))
    return lo + (hi - lo) * u


def reverb(sig, decay=2.0, mix=0.35, rng=None, tail=True):
    """Reverb por convolucao com IR sintetica (ruido com decay exponencial)."""
    r = rng if rng is not None else np.random.default_rng(0)
    ir_len = max(1, int(decay * SR))
    ir = r.uniform(-1, 1, ir_len) * np.exp(-np.linspace(0, 6, ir_len))
    ir = lowpass(ir, 5500)
    ir[0] += 1.0  # som direto
    wet = signal.fftconvolve(sig, ir)
    if not tail:
        wet = wet[: len(sig)]
    dry = np.pad(sig, (0, len(wet) - len(sig)))
    wet = wet / (np.max(np.abs(wet)) + 1e-9)
    return (1.0 - mix) * dry + mix * wet


def make_seamless(sig, xfade=0.5):
    """Crossfade do fim no comeco -> loop sem clique."""
    n = len(sig)
    xf = int(xfade * SR)
    if xf * 2 >= n:
        return sig
    win = np.linspace(0, 1, xf)
    head, tail = sig[:xf].copy(), sig[-xf:].copy()
    out = sig[:-xf].copy()
    out[:xf] = tail * (1 - win) + head * win
    return out


def normalize(sig, peak=0.72):
    m = np.max(np.abs(sig))
    return sig if m < 1e-9 else sig / m * peak


def mix_layers(*layers):
    n = max(len(x) for x in layers)
    out = np.zeros(n)
    for x in layers:
        out[: len(x)] += x  # cada camada entra no seu proprio tamanho
    return out


# ════════════════════════════════════════════════════════════════════════════
#  PRESETS  (cada funcao recebe (rng, dur) e devolve mono float)
# ════════════════════════════════════════════════════════════════════════════

def dread_drone(rng, dur):
    """Drone grave dissonante com batimento lento — tensao de fundo."""
    roots = [55.0, 55.35, 82.5, 58.27]  # A1, detune, fifth, minor-2nd (atrito)
    lays = []
    for i, f in enumerate(roots):
        s = 0.6 * sine(f, dur) + 0.4 * saw(f, dur)
        s = lowpass(s, 320 + 80 * i)
        s = tremolo(s, 0.07 + 0.03 * i, 0.4, phase=i)
        lays.append(s * (0.9 if i == 0 else 0.5))
    air = lowpass(pink(dur, rng), 1800) * 0.08
    sig = mix_layers(*lays, air)
    sig *= env_shape(len(sig), [(0, 0.6), (0.5, 1.0), (1, 0.7)])
    return sig


def sub_rumble(rng, dur):
    """Rumble subgrave — terremoto/presenca colossal abaixo do audivel."""
    b = lowpass(brown(dur, rng), 70, o=6)
    swell = env_shape(len(b), [(0, 0.3), (0.3, 1.0), (0.6, 0.5), (1, 0.9)])
    sub = 0.5 * sine(33, dur)
    return mix_layers(b * swell, lowpass(sub, 60) * 0.6)


def sky_hum(rng, dur):
    """Zumbido do ceu errado — sines detunados batendo + brilho fino."""
    a = sine(60, dur) + sine(60.4, dur) + 0.6 * sine(120.0, dur)
    a = lowpass(a, 500)
    shimmer = bandpass(white(dur, rng), 3200, 6500) * 0.05
    shimmer = tremolo(shimmer, 0.13, 0.7)
    sig = mix_layers(a * 0.7, shimmer)
    return sig * env_shape(len(sig), [(0, 0.5), (0.5, 1.0), (1, 0.6)])


def void_breathing(rng, dur):
    """Respiracao do vazio — inala/exala filtrado, com corpo grave."""
    n = int(SR * dur)
    out = np.zeros(n)
    pos = 0.0
    while pos < dur - 1.6:
        bl = 1.4 + rng.uniform(0, 0.5)
        nb = int(bl * SR)
        seg = bandpass(white(bl, rng), 300, 1600)
        # inala (sobe) -> pequena pausa -> exala (desce)
        seg *= env_shape(nb, [(0, 0), (0.18, 0.9), (0.38, 0.5),
                               (0.5, 0.55), (0.85, 0.0), (1, 0)])
        body = lowpass(sine(70 + rng.uniform(-6, 6), bl), 130) * 0.4
        seg = seg * 0.7 + body * env_shape(nb, [(0, 0), (0.3, 1), (1, 0)])
        s = int(pos * SR)
        out[s:s + nb] += seg[: max(0, n - s)][: nb]
        pos += bl + rng.uniform(0.25, 0.7)
    return out


def wind_howl(rng, dur):
    """Vento uivando por frestas — ruido com cutoff em LFO + assobio."""
    n = int(SR * dur)
    base = brown(dur, rng)
    cut = lfo_cut(0.08, n, 200, 1400)
    # filtragem variavel aproximada: mistura de 3 bandas pesadas pelo LFO
    lo = lowpass(base, 500)
    md = bandpass(base, 500, 1500)
    hi = bandpass(base, 1500, 3500)
    w = (cut - 200) / 1200.0
    sig = lo * (1 - w) + md * (0.7) + hi * (w * 0.5)
    whistle = bandpass(white(dur, rng), 1800, 2400) * 0.06
    whistle = tremolo(whistle, 0.21, 0.8)
    sig = mix_layers(sig * 0.6, whistle)
    return sig * env_shape(len(sig), [(0, 0.4), (0.5, 1.0), (1, 0.5)])


def audience_presence(rng, dur):
    """Plateia invisivel — murmurio distante de uma multidao que nao existe."""
    n = int(SR * dur)
    out = np.zeros(n)
    for _ in range(70):
        sl = rng.uniform(0.15, 0.5)
        ns = int(sl * SR)
        f0 = rng.uniform(300, 1400)
        seg = bandpass(white(sl, rng), f0, f0 * 1.8)
        seg *= env_shape(ns, [(0, 0), (0.4, 1), (1, 0)])
        at = int(rng.uniform(0, dur - sl) * SR)
        out[at:at + ns] += seg * rng.uniform(0.1, 0.3)
    out = lowpass(out, 3000)
    out = reverb(out, decay=1.6, mix=0.4, rng=rng, tail=False)
    return out


def distant_whispers(rng, dur):
    """Sussurros distantes — silabas de ruido nas formantes da voz, longe."""
    n = int(SR * dur)
    out = np.zeros(n)
    formants = [(700, 1100), (1100, 1600), (2300, 2900)]
    pos = 0.0
    while pos < dur - 0.4:
        syl = rng.uniform(0.08, 0.22)
        ns = int(syl * SR)
        lo, hi = formants[rng.integers(0, len(formants))]
        seg = bandpass(white(syl, rng), lo, hi)
        seg *= env_shape(ns, [(0, 0), (0.3, 1), (0.6, 0.7), (1, 0)])
        s = int(pos * SR)
        out[s:s + ns] += seg[: max(0, n - s)][:ns] * rng.uniform(0.4, 0.8)
        pos += syl + rng.uniform(0.02, 0.5)
    out = highpass(out, 500)
    out = reverb(out, decay=2.2, mix=0.45, rng=rng, tail=False)
    return out * 0.5


def granular_texture(rng, dur):
    """Nuvem granular — graos de tom numa escala sombria, lavados em reverb."""
    n = int(SR * dur)
    out = np.zeros(n)
    scale = [110.0, 116.5, 146.8, 155.6, 220.0]  # menor/dissonante
    for _ in range(220):
        gl = rng.uniform(0.04, 0.12)
        ng = int(gl * SR)
        f = scale[rng.integers(0, len(scale))] * (1 if rng.random() < 0.7 else 2)
        g = sine(f, gl) * np.hanning(ng)
        at = int(rng.uniform(0, dur - gl) * SR)
        out[at:at + ng] += g * rng.uniform(0.1, 0.4)
    out = reverb(out, decay=2.5, mix=0.5, rng=rng, tail=False)
    return out


def heartbeat(rng, dur):
    """Batimento (lub-dub) grave — loop em ~70 bpm."""
    n = int(SR * dur)
    out = np.zeros(n)

    def thump(f0, f1, length, amp):
        nn = int(length * SR)
        s = glide_tone(f0, f1, length) * env_shape(nn, [(0, 0), (0.04, 1), (1, 0)])
        return lowpass(s, 160) * amp

    period = dur  # 1 ciclo = arquivo todo (loopa)
    lub = thump(95, 45, 0.14, 1.0)
    dub = thump(80, 40, 0.16, 0.75)
    out[: len(lub)] += lub
    o2 = int(0.28 * SR)
    out[o2:o2 + len(dub)] += dub[: max(0, n - o2)]
    return out


def false_footsteps(rng, dur):
    """Passos que nao sao seus — impactos irregulares atras de voce."""
    n = int(SR * dur)
    out = np.zeros(n)
    pos = rng.uniform(0.1, 0.4)
    while pos < dur - 0.2:
        sl = 0.09
        ns = int(sl * SR)
        imp = lowpass(white(sl, rng), rng.uniform(400, 900))
        imp *= env_shape(ns, [(0, 1), (0.25, 0.5), (1, 0)])
        body = lowpass(sine(rng.uniform(55, 80), sl), 120) * 0.5
        seg = imp * 0.8 + body * env_shape(ns, [(0, 1), (1, 0)])
        s = int(pos * SR)
        out[s:s + ns] += seg[: max(0, n - s)][:ns] * rng.uniform(0.6, 1.0)
        pos += rng.uniform(0.45, 1.1)
    return out


def riser(rng, dur):
    """Riser de tensao — sobe em pitch e energia ate cortar (build-up)."""
    tone = glide_tone(80, 900, dur, curve=2.2)
    nz = white(dur, rng)
    n = len(nz)
    cut = env_shape(n, [(0, 300), (1, 7000)])
    # aproxima sweep com bandas crescentes
    nz = highpass(nz, 200) * env_shape(n, [(0, 0.0), (1, 1.0)]) ** 2
    sig = mix_layers(lowpass(tone, 4000) * 0.6, nz * 0.5)
    sig *= env_shape(len(sig), [(0, 0.0), (0.95, 1.0), (1, 0.0)])
    return fade(sig, 0.05, 0.01)


def reverse_stinger(rng, dur):
    """Succao reversa — swell que entra antes do susto (pre-jumpscare)."""
    cluster = mix_layers(saw(120, dur), saw(170, dur), saw(240.3, dur))
    cluster = lowpass(cluster, 3000)
    nz = bandpass(white(dur, rng), 500, 4000)
    fwd = mix_layers(cluster * 0.5, nz * 0.4)
    fwd *= env_shape(len(fwd), [(0, 1.0), (1, 0.0)])  # decai...
    rev = fwd[::-1].copy()                            # ...invertido = cresce
    rev = reverb(rev, decay=1.2, mix=0.3, rng=rng, tail=False)
    return fade(rev, 0.2, 0.005)


def stinger_hit(rng, dur):
    """Pancada dissonante de susto — cluster afiado + cauda de reverb + sub."""
    hit = mix_layers(saw(220, 0.5), saw(311.1, 0.5), saw(466.2, 0.5))  # tritono
    hit = lowpass(hit, 5000)
    hit *= env_shape(len(hit), [(0, 1), (0.05, 1), (1, 0)])
    sub = glide_tone(120, 40, 0.6) * env_shape(int(0.6 * SR), [(0, 1), (1, 0)])
    crack = highpass(white(0.08, rng), 3000)
    body = mix_layers(hit, lowpass(sub, 120) * 0.9, crack * 0.6)
    out = reverb(body, decay=max(1.0, dur - 0.6), mix=0.4, rng=rng, tail=True)
    return fade(out, 0.001, 0.1)


def metallic_groan(rng, dur):
    """Rangido metalico — FM inarmonico curvando em pitch, estrutura cedendo."""
    n = int(SR * dur)
    ienv = env_shape(n, [(0, 0.2), (0.3, 1.0), (1, 0.4)])
    base = fm(carrier=90, ratio=1.414, index=6.0, dur=dur, index_env=ienv)
    bend = glide_tone(120, 95, dur)  # leve descida
    sig = bandpass(base * 0.7 + bend * 0.3, 120, 3000)
    sig = tremolo(sig, 5.5, 0.3)  # vibracao de stress
    sig *= env_shape(n, [(0, 0), (0.1, 1), (0.8, 0.8), (1, 0)])
    out = reverb(sig, decay=2.0, mix=0.4, rng=rng, tail=True)
    return fade(out, 0.02, 0.1)


def bell_toll(rng, dur):
    """Sino fantasma — FM grave detunado com cauda longa, presságio."""
    n = int(SR * dur)
    denv = env_shape(n, [(0, 1.0), (0.2, 0.5), (1, 0.05)])
    b1 = fm(70, 2.76, 4.0, dur, index_env=denv) * denv
    b2 = fm(70.6, 5.04, 2.5, dur, index_env=denv) * denv * 0.6
    sig = lowpass(mix_layers(b1, b2), 4000)
    out = reverb(sig, decay=min(4.0, dur), mix=0.5, rng=rng, tail=True)
    return fade(out, 0.002, 0.2)


def static_burst(rng, dur):
    """Estatica/glitch de radio — ruido com crackle gateado."""
    nz = bandpass(white(dur, rng), 200, 6000)
    n = len(nz)
    gate = (rng.uniform(0, 1, n) > 0.5).astype(float)
    gate = lowpass(gate, 400)  # suaviza o gate -> crackle
    crackle = nz * gate
    tone = sine(2000, dur) * 0.05 * (rng.uniform(0, 1, n) > 0.7)
    sig = mix_layers(crackle * 0.8, tone)
    return fade(sig, 0.005, 0.03)


def tendril_movement(rng, dur):
    """Tentaculo se movendo — ruido umido com glide e flutter (carnoso)."""
    n = int(SR * dur)
    base = bandpass(white(dur, rng), 300, 2200)
    cut = lfo_cut(2.5, n, 400, 1800)
    w = (cut - 400) / 1400.0
    wet = base * (0.5 + 0.5 * w)
    slither = glide_tone(180, 90, dur) * 0.2
    flutter = 1.0 + 0.4 * np.sin(TWO_PI * 7 * t_axis(dur))
    sig = (wet + lowpass(slither, 600)) * flutter
    sig *= env_shape(n, [(0, 0), (0.2, 1), (0.8, 1), (1, 0)])
    out = reverb(sig, decay=1.4, mix=0.3, rng=rng, tail=False)
    return out


# preset -> (func, category, loop, dur_default)
PRESETS = {
    "dread_drone":       (dread_drone,       "ambient", True,  12.0),
    "sub_rumble":        (sub_rumble,        "ambient", True,  10.0),
    "sky_hum":           (sky_hum,           "ambient", True,  12.0),
    "void_breathing":    (void_breathing,    "ambient", True,  9.0),
    "wind_howl":         (wind_howl,         "ambient", True,  12.0),
    "audience_presence": (audience_presence, "ambient", True,  10.0),
    "distant_whispers":  (distant_whispers,  "ambient", True,  9.0),
    "granular_texture":  (granular_texture,  "ambient", True,  8.0),
    "heartbeat":         (heartbeat,         "hostile", True,  1.7),
    "false_footsteps":   (false_footsteps,   "hostile", True,  6.0),
    "riser":             (riser,             "ambient", False, 6.0),
    "reverse_stinger":   (reverse_stinger,   "ambient", False, 3.0),
    "stinger_hit":       (stinger_hit,       "hostile", False, 2.6),
    "metallic_groan":    (metallic_groan,    "hostile", False, 5.0),
    "bell_toll":         (bell_toll,         "ambient", False, 6.0),
    "static_burst":      (static_burst,      "block",   False, 2.0),
    "tendril_movement":  (tendril_movement,  "hostile", False, 4.0),
}


def render(name, rng):
    func, _cat, loop, dur = PRESETS[name]
    sig = func(rng, dur)
    sig = np.nan_to_num(sig)
    if loop:
        sig = make_seamless(sig, xfade=min(0.6, dur * 0.25))
    else:
        sig = fade(sig, 0.01, 0.06)
    return normalize(sig).astype(np.float32)


def write_ogg(path, sig):
    """Escreve OGG/Vorbis MONO via scipy (WAV) + ffmpeg (libvorbis).

    NÃO usamos o encoder Vorbis do libsndfile: a 2ª escrita no mesmo processo
    estoura a pilha nesta build do Windows. scipy.io.wavfile não toca nesse
    código e ffmpeg faz a codificação final — robusto e idempotente.
    """
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm16 = (np.clip(sig, -1.0, 1.0) * 32767.0).astype(np.int16)
    tmp = path.with_suffix(".tmp.wav")
    wavfile.write(str(tmp), SR, pcm16)
    try:
        subprocess.run(
            ["ffmpeg", "-y", "-loglevel", "error", "-i", str(tmp),
             "-ac", "1", "-c:a", "libvorbis", "-qscale:a", "5", str(path)],
            check=True,
        )
    finally:
        tmp.unlink(missing_ok=True)


# ════════════════════════════════════════════════════════════════════════════
#  REGISTRO (sounds.json + ModSounds.java)  — idempotente
# ════════════════════════════════════════════════════════════════════════════

def register(names, variants):
    # sounds.json
    data = json.loads(SOUNDS_JSON.read_text(encoding="utf-8")) if SOUNDS_JSON.exists() else {}
    for name in names:
        _f, cat, loop, _d = PRESETS[name]
        files = ([f"ambience/{name}"] if variants <= 1
                 else [f"ambience/{name}_{i+1}" for i in range(variants)])
        data[f"ambience.{name}"] = {
            "category": cat,
            "sounds": [{"name": f"liberthia:{f}", "stream": bool(loop)} for f in files],
        }
    SOUNDS_JSON.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    # ModSounds.java — bloco marcado (substituido a cada run)
    if MODSOUNDS.exists():
        src = MODSOUNDS.read_text(encoding="utf-8")
        lines = [MARK_BEGIN]
        for name in names:
            const = "AMBIENCE_" + name.upper()
            lines.append(f'    public static final RegistryObject<SoundEvent> {const} = '
                         f'register("ambience.{name}");')
        lines.append(MARK_END)
        block = "\n".join(lines)
        if MARK_BEGIN in src and MARK_END in src:
            src = re.sub(re.escape(MARK_BEGIN) + r".*?" + re.escape(MARK_END), block, src, flags=re.S)
        else:
            # insere antes do construtor privado
            src = src.replace("    private ModSounds() {", block + "\n\n    private ModSounds() {")
        MODSOUNDS.write_text(src, encoding="utf-8")


# ════════════════════════════════════════════════════════════════════════════

def main():
    ap = argparse.ArgumentParser(description="Gerador procedural de ambientacao de terror")
    ap.add_argument("--only", help="lista separada por virgula de presets")
    ap.add_argument("--seed", type=int, default=1337)
    ap.add_argument("--variants", type=int, default=1, help="N takes por preset (_1.._N)")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--no-register", action="store_true")
    args = ap.parse_args()

    if args.list:
        print(f"{len(PRESETS)} presets:")
        for n, (_f, cat, loop, dur) in PRESETS.items():
            print(f"  {n:<20} cat={cat:<8} {'loop' if loop else 'oneshot':<8} {dur:>4.1f}s")
        return

    names = ([s.strip() for s in args.only.split(",")] if args.only else list(PRESETS))
    bad = [n for n in names if n not in PRESETS]
    if bad:
        raise SystemExit(f"preset(s) desconhecido(s): {bad}\nuse --list")
    if shutil.which("ffmpeg") is None:
        raise SystemExit("ffmpeg nao encontrado no PATH — necessario p/ exportar OGG/Vorbis.")

    AMB_DIR.mkdir(parents=True, exist_ok=True)
    total = 0
    for name in names:
        for v in range(max(1, args.variants)):
            rng = np.random.default_rng(args.seed + v * 101 + hash(name) % 9973)
            sig = render(name, rng)
            suffix = "" if args.variants <= 1 else f"_{v+1}"
            out = AMB_DIR / f"{name}{suffix}.ogg"
            write_ogg(out, sig)
            dur_s = len(sig) / SR
            print(f"  [ok] {out.relative_to(ROOT)}  ({dur_s:.1f}s, {out.stat().st_size // 1024} KB)")
            total += 1

    if not args.no_register:
        register(names, args.variants)
        print(f"  -> registrado em sounds.json + ModSounds.java ({len(names)} eventos)")

    print(f"\n{total} arquivo(s) gerado(s) em {AMB_DIR.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
