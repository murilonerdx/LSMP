import { useQuery } from '@tanstack/react-query'
import { api } from './api'
import {
  VANILLA_EFFECTS,
  VANILLA_PARTICLES,
  VANILLA_ENTITIES,
  COMMON_SOUNDS,
  VANILLA_ENCHANTMENTS,
  COMMON_COMMANDS,
} from './mcRegistry'

/**
 * Hooks que retornam listas pra Autocomplete.
 *
 * Estratégia: SEMPRE tenta buscar do backend (que proxia do mod e devolve
 * vanilla + mods carregados). Se a query ainda não respondeu OU falhou, cai
 * pra lista estática como fallback.
 *
 * Defensivo: filtra entradas null/undefined/sem id pra nunca propagar lixo
 * pra UI (o Autocomplete crasha em `undefined.toLowerCase()`).
 */

const ONE_HOUR = 60 * 60 * 1000

type Option = { value: string; label: string }
type RawEntry = { id?: string | null; name?: string | null } | null | undefined

function asOptions(list: unknown): Option[] {
  if (!Array.isArray(list)) return []
  const out: Option[] = []
  for (const raw of list) {
    const x = raw as RawEntry
    if (!x || typeof x !== 'object') continue
    const id = typeof x.id === 'string' ? x.id : null
    if (!id) continue
    const name = typeof x.name === 'string' && x.name !== id ? x.name : null
    out.push({ value: id, label: name ? `${name} — ${id}` : id })
  }
  return out
}

function fallback(ids: readonly string[]): Option[] {
  return ids.filter((id) => typeof id === 'string' && id.length > 0)
    .map((id) => ({ value: id, label: id }))
}

export function useItemOptions(): Option[] {
  const q = useQuery({
    queryKey: ['items-list'],
    queryFn: api.items,
    staleTime: ONE_HOUR,
    gcTime: ONE_HOUR,
    retry: 1,
  })
  const opts = asOptions(q.data)
  return opts.length > 0 ? opts : []
}

export function useEnchantmentOptions(): Option[] {
  const q = useQuery({
    queryKey: ['enchants-list'],
    queryFn: api.enchantments,
    staleTime: ONE_HOUR,
    gcTime: ONE_HOUR,
    retry: 1,
  })
  if (Array.isArray(q.data) && q.data.length > 0) {
    const out: Option[] = []
    for (const e of q.data as any[]) {
      if (!e || typeof e !== 'object') continue
      const id = typeof e.id === 'string' ? e.id : null
      if (!id) continue
      const name = typeof e.name === 'string' && e.name !== id ? e.name : null
      const lvl = typeof e.maxLevel === 'number' ? ` (lv ${e.maxLevel})` : ''
      out.push({ value: id, label: name ? `${name}${lvl} — ${id}` : id })
    }
    if (out.length > 0) return out
  }
  return fallback(VANILLA_ENCHANTMENTS)
}

export function useSoundOptions(): Option[] {
  const q = useQuery({
    queryKey: ['sounds-list'],
    queryFn: api.sounds,
    staleTime: ONE_HOUR,
    gcTime: ONE_HOUR,
    retry: 1,
  })
  const opts = asOptions(q.data)
  return opts.length > 0 ? opts : fallback(COMMON_SOUNDS)
}

export function useParticleOptions(): Option[] {
  const q = useQuery({
    queryKey: ['particles-list'],
    queryFn: api.particles,
    staleTime: ONE_HOUR,
    gcTime: ONE_HOUR,
    retry: 1,
  })
  const opts = asOptions(q.data)
  return opts.length > 0 ? opts : fallback(VANILLA_PARTICLES)
}

export function useEffectOptions(): Option[] {
  const q = useQuery({
    queryKey: ['effects-list'],
    queryFn: api.effects,
    staleTime: ONE_HOUR,
    gcTime: ONE_HOUR,
    retry: 1,
  })
  const opts = asOptions(q.data)
  return opts.length > 0 ? opts : fallback(VANILLA_EFFECTS)
}

export function useEntityOptions(): Option[] {
  const q = useQuery({
    queryKey: ['entities-list'],
    queryFn: api.entities,
    staleTime: ONE_HOUR,
    gcTime: ONE_HOUR,
    retry: 1,
  })
  const opts = asOptions(q.data)
  return opts.length > 0 ? opts : fallback(VANILLA_ENTITIES)
}

/**
 * Comandos MC vanilla — templates curtos com placeholder onde aplicável.
 * Usar como Autocomplete em campos onde o operador digita comando.
 */
export function useCommandOptions(): Option[] {
  return fallback(COMMON_COMMANDS)
}
