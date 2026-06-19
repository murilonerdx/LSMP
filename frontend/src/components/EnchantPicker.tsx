import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { api } from '../lib/api'

export type SelectedEnch = { id: string; level: number }
type Props = {
  selected: SelectedEnch[]
  onChange: (s: SelectedEnch[]) => void
}

export function EnchantPicker({ selected, onChange }: Props) {
  const q = useQuery({
    queryKey: ['enchantments'],
    queryFn: api.enchantments,
    staleTime: 60_000,
  })
  const [search, setSearch] = useState('')
  const enchs = q.data ?? []
  const filtered = useMemo(() => {
    const t = search.toLowerCase()
    return enchs.filter(e => !t || e.id.toLowerCase().includes(t) || e.name.toLowerCase().includes(t)).slice(0, 100)
  }, [enchs, search])

  const isSel = (id: string) => selected.find(s => s.id === id)
  const toggle = (id: string, maxLevel: number) => {
    if (isSel(id)) {
      onChange(selected.filter(s => s.id !== id))
    } else {
      onChange([...selected, { id, level: maxLevel }])
    }
  }
  const setLevel = (id: string, level: number) => {
    onChange(selected.map(s => s.id === id ? { ...s, level } : s))
  }

  return (
    <div className="card">
      <h3 className="font-bold mb-2">✨ Encantamentos ({selected.length} selecionados)</h3>
      <input
        className="input mb-2"
        placeholder="Pesquisar (sharpness, fortune, ...)"
        value={search}
        onChange={e => setSearch(e.target.value)}
      />
      <div className="max-h-72 overflow-y-auto space-y-1">
        {filtered.map(e => {
          const sel = isSel(e.id)
          return (
            <div
              key={e.id}
              className={`px-2 py-1.5 rounded flex items-center gap-2 ${
                sel ? 'bg-liberthia-500/40 border border-liberthia-400' : 'hover:bg-liberthia-700'
              }`}
            >
              <input
                type="checkbox"
                checked={!!sel}
                onChange={() => toggle(e.id, e.maxLevel)}
                className="accent-liberthia-400"
              />
              <div className="flex-1 min-w-0">
                <div className="text-sm">{e.name} {e.isCurse && <span className="text-red-400">⚠</span>}</div>
                <div className="text-xs opacity-50 truncate">{e.id}</div>
              </div>
              {sel && (
                <input
                  type="number"
                  min={1}
                  max={Math.max(e.maxLevel, 10)}
                  value={sel.level}
                  onChange={ev => setLevel(e.id, parseInt(ev.target.value) || 1)}
                  className="input w-16 text-center"
                />
              )}
              <span className="text-xs opacity-50 w-10 text-right">max {e.maxLevel}</span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
