import { create } from 'zustand'

const TOKEN_KEY = 'liberthia.token'

type AuthStore = {
  token: string | null
  loading: boolean
  error: string | null
  login: (password: string) => Promise<boolean>
  logout: () => void
  setToken: (t: string | null) => void
}

export const useAuth = create<AuthStore>((set) => ({
  token: localStorage.getItem(TOKEN_KEY),
  loading: false,
  error: null,
  setToken: (t) => {
    if (t) localStorage.setItem(TOKEN_KEY, t)
    else localStorage.removeItem(TOKEN_KEY)
    set({ token: t })
  },
  login: async (password) => {
    set({ loading: true, error: null })
    try {
      const r = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password }),
      })
      if (r.status === 401) {
        set({ loading: false, error: 'Senha incorreta' })
        return false
      }
      if (!r.ok) {
        set({ loading: false, error: `HTTP ${r.status}` })
        return false
      }
      const data = await r.json()
      localStorage.setItem(TOKEN_KEY, data.token)
      set({ loading: false, token: data.token, error: null })
      return true
    } catch (e: any) {
      set({ loading: false, error: e.message })
      return false
    }
  },
  logout: () => {
    localStorage.removeItem(TOKEN_KEY)
    set({ token: null })
  },
}))

export function getAuthToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}
