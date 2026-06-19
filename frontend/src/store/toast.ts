import { create } from 'zustand'

export type Toast = { id: number; kind: 'ok' | 'err' | 'info'; text: string }

type ToastStore = {
  list: Toast[]
  push: (kind: Toast['kind'], text: string) => void
  dismiss: (id: number) => void
}

let nextId = 1

export const useToasts = create<ToastStore>((set) => ({
  list: [],
  push: (kind, text) => {
    const id = nextId++
    set((s) => ({ list: [...s.list, { id, kind, text }] }))
    setTimeout(() => set((s) => ({ list: s.list.filter((t) => t.id !== id) })), 4000)
  },
  dismiss: (id) => set((s) => ({ list: s.list.filter((t) => t.id !== id) })),
}))

export const toast = {
  ok: (t: string) => useToasts.getState().push('ok', t),
  err: (t: string) => useToasts.getState().push('err', t),
  info: (t: string) => useToasts.getState().push('info', t),
}
