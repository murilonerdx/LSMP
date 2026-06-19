import { api } from './api'

/**
 * Atmosphere Engine — runner compartilhado pra cenas atmosféricas customizadas.
 * Suporta steps complexos: flash, garble, loop, random.
 *
 * Usado pelas pages Horror / Divine / Magical / Apocalyptic / etc.
 */

export type AtmosphereStep =
  | { kind: 'title'; text: string; subtitle?: string; fadeIn?: number; stay?: number; fadeOut?: number; mode?: 'title' | 'subtitle' | 'actionbar' }
  | { kind: 'flash'; text: string; flashes: number; intervalMs: number; colors?: string[]; sound?: string }
  | { kind: 'garble'; text: string; iterations: number; finalText?: string; intervalMs: number; color?: string }
  | { kind: 'sound'; sound: string; pitch?: number; volume?: number }
  | { kind: 'chat'; message: string }
  | { kind: 'effect'; effect: string; duration: number; amplifier: number }
  | { kind: 'particle'; particle: string; relX: number; relY: number; relZ: number; count: number; spread?: number }
  | { kind: 'weather'; type: 'clear' | 'rain' | 'thunder'; duration: number }
  | { kind: 'time'; ticks: number }
  | { kind: 'command'; cmd: string }
  | { kind: 'wait'; ms: number }
  | { kind: 'loop'; steps: AtmosphereStep[]; iterations: number; gapMs?: number }
  | { kind: 'random'; choices: AtmosphereStep[]; count?: number }

export type Atmosphere = {
  id: string
  name: string
  emoji: string
  description: string
  category: string  // 'horror' | 'divine' | 'magical' | 'apocalyptic' | 'wasteland' | 'custom'
  steps: AtmosphereStep[]
  custom?: boolean
  loop?: boolean        // se true, repete tudo até cancelar
  updated?: number
}

export type RunContext = {
  target: 'all' | string             // 'all' ou uuid de player
  playerUuids: string[]
  playerSelector: string             // @a ou nome
  origin: string                     // tag pra api.command
  abort: { cancelled: boolean }
}

// ============= Garble helpers =============

const GLITCH_CHARS = ['#', '@', '$', '%', '&', '?', '!', '/', '\\', '|', '*', '+', '~', '`', '^', '<', '>', '[', ']', '{', '}']

export function garble(text: string, intensity: number): string {
  return text.split('').map((c) => {
    if (c === ' ') return c
    if (Math.random() < intensity) return GLITCH_CHARS[Math.floor(Math.random() * GLITCH_CHARS.length)]
    return c
  }).join('')
}

// ============= Runner =============

export async function runAtmosphere(atm: Atmosphere, ctx: RunContext): Promise<void> {
  do {
    for (const s of atm.steps) {
      if (ctx.abort.cancelled) return
      await runStep(s, ctx)
    }
  } while (atm.loop && !ctx.abort.cancelled)
}

export async function runStep(s: AtmosphereStep, ctx: RunContext): Promise<void> {
  if (ctx.abort.cancelled) return

  switch (s.kind) {
    case 'title': {
      const mode = s.mode ?? 'title'
      if (mode === 'actionbar') {
        await api.command(`title ${ctx.playerSelector} actionbar ${JSON.stringify({ text: s.text })}`, ctx.origin).catch(() => {})
      } else {
        for (const u of ctx.playerUuids) {
          api.title(
            u,
            mode === 'subtitle' ? ' ' : s.text,
            mode === 'subtitle' ? s.text : (s.subtitle ?? ''),
            s.fadeIn ?? 10, s.stay ?? 60, s.fadeOut ?? 20,
          ).catch(() => {})
        }
      }
      break
    }

    case 'flash': {
      const colors = s.colors ?? ['§c', '§4', '§f']
      for (let i = 0; i < s.flashes; i++) {
        if (ctx.abort.cancelled) return
        const color = colors[i % colors.length]
        for (const u of ctx.playerUuids) {
          api.title(u, `${color}§l${s.text}`, '', 0, Math.max(2, Math.floor(s.intervalMs / 50)), 0).catch(() => {})
        }
        if (s.sound) {
          for (const u of ctx.playerUuids) {
            api.sound(u, s.sound, 0.5, 0.5 + Math.random()).catch(() => {})
          }
        }
        await new Promise((r) => setTimeout(r, s.intervalMs))
      }
      break
    }

    case 'garble': {
      const color = s.color ?? '§8'
      for (let i = 0; i < s.iterations; i++) {
        if (ctx.abort.cancelled) return
        const intensity = i / Math.max(s.iterations - 1, 1)  // 0 → 1
        const garbled = garble(s.text, intensity)
        for (const u of ctx.playerUuids) {
          api.title(u, `${color}§l${garbled}`, '', 0, Math.max(2, Math.floor(s.intervalMs / 50)), 0).catch(() => {})
        }
        await new Promise((r) => setTimeout(r, s.intervalMs))
      }
      if (s.finalText) {
        for (const u of ctx.playerUuids) {
          api.title(u, s.finalText, '', 5, 40, 10).catch(() => {})
        }
      }
      break
    }

    case 'sound': {
      for (const u of ctx.playerUuids) {
        api.sound(u, s.sound, s.volume ?? 1, s.pitch ?? 1).catch(() => {})
      }
      break
    }

    case 'chat': {
      await api.command(`tellraw ${ctx.playerSelector} ${JSON.stringify({ text: s.message })}`, ctx.origin).catch(() => {})
      break
    }

    case 'effect': {
      await api.command(`effect give ${ctx.playerSelector} ${s.effect} ${s.duration} ${s.amplifier} true`, ctx.origin).catch(() => {})
      break
    }

    case 'particle': {
      // Coords relativas a cada player target
      const live = await api.players().catch(() => [] as any[])
      for (const u of ctx.playerUuids) {
        const p = live.find((x: any) => x.uuid === u); if (!p) continue
        const wx = p.position.x + s.relX
        const wy = p.position.y + s.relY
        const wz = p.position.z + s.relZ
        api.particle(s.particle, wx, wy, wz, s.count).catch(() => {})
      }
      break
    }

    case 'weather': await api.worldWeather(s.type, s.duration).catch(() => {}); break
    case 'time': await api.worldTime(s.ticks).catch(() => {}); break
    case 'command': await api.command(s.cmd, ctx.origin).catch(() => {}); break
    case 'wait': await new Promise((r) => setTimeout(r, s.ms)); break

    case 'loop': {
      for (let i = 0; i < s.iterations; i++) {
        if (ctx.abort.cancelled) return
        for (const sub of s.steps) {
          if (ctx.abort.cancelled) return
          await runStep(sub, ctx)
        }
        if (s.gapMs && i < s.iterations - 1) {
          await new Promise((r) => setTimeout(r, s.gapMs))
        }
      }
      break
    }

    case 'random': {
      const count = Math.min(s.count ?? 1, s.choices.length)
      const picks = [...s.choices].sort(() => Math.random() - 0.5).slice(0, count)
      for (const pick of picks) {
        if (ctx.abort.cancelled) return
        await runStep(pick, ctx)
      }
      break
    }
  }
}

// ============= Step factory =============

export function defaultStep(kind: AtmosphereStep['kind']): AtmosphereStep {
  switch (kind) {
    case 'title': return { kind: 'title', text: '§4§lTÍTULO', subtitle: '', fadeIn: 5, stay: 50, fadeOut: 15, mode: 'title' }
    case 'flash': return { kind: 'flash', text: 'ALERTA', flashes: 8, intervalMs: 100, colors: ['§4', '§c', '§f'] }
    case 'garble': return { kind: 'garble', text: 'sinal perdido', iterations: 12, finalText: '', intervalMs: 80, color: '§8' }
    case 'sound': return { kind: 'sound', sound: 'minecraft:entity.ghast.warn', pitch: 0.5, volume: 1 }
    case 'chat': return { kind: 'chat', message: '§8§oeu vejo você' }
    case 'effect': return { kind: 'effect', effect: 'minecraft:blindness', duration: 60, amplifier: 0 }
    case 'particle': return { kind: 'particle', particle: 'minecraft:soul_fire_flame', relX: 0, relY: 2, relZ: 0, count: 20 }
    case 'weather': return { kind: 'weather', type: 'thunder', duration: 1200 }
    case 'time': return { kind: 'time', ticks: 18000 }
    case 'command': return { kind: 'command', cmd: 'say hello' }
    case 'wait': return { kind: 'wait', ms: 1000 }
    case 'loop': return { kind: 'loop', steps: [], iterations: 3, gapMs: 200 }
    case 'random': return { kind: 'random', choices: [], count: 1 }
  }
}

export function stepLabel(s: AtmosphereStep): string {
  switch (s.kind) {
    case 'title':    return `📺 ${s.text.slice(0, 14)}`
    case 'flash':    return `⚡ flash ×${s.flashes}`
    case 'garble':   return `🜨 garble`
    case 'sound':    return `🔊 ${s.sound.split(':')[1]?.split('.').pop() ?? ''}`
    case 'chat':     return `💬 chat`
    case 'effect':   return `⚗ ${s.effect.replace('minecraft:', '')}`
    case 'particle': return `✨ ${s.particle.replace('minecraft:', '').slice(0, 10)}`
    case 'weather':  return `⛅ ${s.type}`
    case 'time':     return `🕐 ${s.ticks}t`
    case 'command':  return `⌨ cmd`
    case 'wait':     return `⏳ ${s.ms}ms`
    case 'loop':     return `🔁 ×${s.iterations}`
    case 'random':   return `🎲 random`
  }
}
