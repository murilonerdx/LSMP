import { useEffect, useRef, useState } from 'react'
import { api } from './api'

/**
 * Substitui useState + localStorage por persistência no banco via KV.
 *
 *   const [rolls, setRolls] = useKvState<DiceRoll[]>('dice_rolls', [])
 *
 *  - GET inicial: carrega do backend
 *  - setRolls(...): atualiza state local + debounced save no backend
 *  - Otimista: UI atualiza imediatamente, persistência roda em background
 */
export function useKvState<T>(key: string, initial: T, debounceMs = 500): [T, (v: T | ((p: T) => T)) => void, { loading: boolean; saving: boolean }] {
  const [value, setValue] = useState<T>(initial)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const saveTimer = useRef<number | null>(null)
  const isInitial = useRef(true)

  // Carrega do backend
  useEffect(() => {
    let alive = true
    api.kvGet<T>(key)
      .then((r) => { if (alive && r.data != null) setValue(r.data) })
      .catch((e) => console.warn(`[kv:${key}] load fail:`, e.message))
      .finally(() => { if (alive) { setLoading(false); isInitial.current = false } })
    return () => { alive = false }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])

  // Salva debounced
  useEffect(() => {
    if (isInitial.current) return
    if (saveTimer.current) window.clearTimeout(saveTimer.current)
    setSaving(true)
    saveTimer.current = window.setTimeout(() => {
      api.kvPut(key, value)
        .catch((e) => console.warn(`[kv:${key}] save fail:`, e.message))
        .finally(() => setSaving(false))
    }, debounceMs)
    return () => { if (saveTimer.current) window.clearTimeout(saveTimer.current) }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, key])

  return [value, setValue, { loading, saving }]
}
