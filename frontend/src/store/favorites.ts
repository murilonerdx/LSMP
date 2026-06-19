import { useEffect, useState } from 'react'

/**
 * Sistema de favoritos pra navegação rápida no sidebar.
 *
 * Persistido em localStorage como JSON array de rotas (ex: ['/voice-map', '/ai']).
 * Hook React `useFavorites` faz subscribe — quando uma janela muda,
 * outras janelas/abas atualizam via storage event.
 */

const LS_KEY = 'liberthia.favorites'

function read(): string[] {
  if (typeof window === 'undefined') return []
  try {
    const raw = localStorage.getItem(LS_KEY)
    if (!raw) return []
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}
function write(favs: string[]): void {
  localStorage.setItem(LS_KEY, JSON.stringify(favs))
  // dispatch evento manual pra outros hooks na MESMA aba também atualizarem
  window.dispatchEvent(new StorageEvent('storage', { key: LS_KEY }))
}

export function useFavorites(): {
  favorites: string[]
  toggle: (path: string) => void
  isFavorite: (path: string) => boolean
} {
  const [favorites, setFavorites] = useState<string[]>(read)

  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === LS_KEY) setFavorites(read())
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])

  const toggle = (path: string) => {
    const next = favorites.includes(path)
      ? favorites.filter(p => p !== path)
      : [...favorites, path]
    write(next)
    setFavorites(next)
  }
  const isFavorite = (path: string) => favorites.includes(path)

  return { favorites, toggle, isFavorite }
}
