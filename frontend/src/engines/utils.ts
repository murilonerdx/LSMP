import { useEffect, useState } from 'react'

/**
 * Engine framework — utilitários compartilhados pelas engines globais.
 *
 * Engines rodam no nível do <App /> (em <EnginesRunner />) e continuam ativas
 * enquanto a aba do browser estiver aberta, independente da página visível.
 *
 * Estado das engines vive em localStorage. Pages mutam direto via setEngineState.
 * Quando engine atualiza LS (ex: incrementa contador), dispara CustomEvent
 * pra avisar pages abertas a re-sincronizarem.
 */

export const ENGINE_EVENT_PREFIX = 'liberthia:engine:'

/** Dispara um evento custom avisando que a engine atualizou o LS. */
export function dispatchEngineUpdate(engineId: string, detail?: any) {
  try { window.dispatchEvent(new CustomEvent(`${ENGINE_EVENT_PREFIX}${engineId}`, { detail })) }
  catch {}
}

/** Lê do LS com fallback seguro + parse error handling. */
export function readLS<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    if (raw == null) return fallback
    return JSON.parse(raw) as T
  } catch { return fallback }
}

/** Salva no LS (silenciosamente falha em quota exceeded). */
export function writeLS<T>(key: string, value: T): void {
  try { localStorage.setItem(key, JSON.stringify(value)) }
  catch (e) { console.warn('[engines] LS write failed', key, e) }
}

/**
 * Hook usado por PAGES pra manter state sincronizado com LS quando engine
 * grava atualizações. Lê via `loader` na montagem e quando recebe o custom event.
 */
export function useEngineSync<T>(engineId: string, loader: () => T): [T, (v: T | ((prev: T) => T)) => void] {
  const [value, setValue] = useState<T>(loader)
  useEffect(() => {
    const refresh = () => setValue(loader())
    window.addEventListener(`${ENGINE_EVENT_PREFIX}${engineId}`, refresh)
    return () => window.removeEventListener(`${ENGINE_EVENT_PREFIX}${engineId}`, refresh)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [engineId])
  return [value, setValue]
}

/** Helper para registrar engine como ativa + log dev. */
export function logEngine(engineId: string, msg: string, ...args: any[]) {
  console.info(`[engine:${engineId}]`, msg, ...args)
}

/** Pega lista de players sem precisar do useQuery (usado pelas engines). */
export async function fetchPlayers(): Promise<any[]> {
  try {
    const { api } = await import('../lib/api')
    return await api.players()
  } catch { return [] }
}
