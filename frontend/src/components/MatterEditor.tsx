import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { api } from '../lib/api'

export function MatterEditor({ uuid }: { uuid: string }) {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['matter', uuid], queryFn: () => api.matter(uuid), refetchInterval: 4000 })
  const [dm, setDm] = useState(0)
  const [wm, setWm] = useState(0)
  const [ym, setYm] = useState(0)

  useEffect(() => {
    if (q.data) { setDm(q.data.dm); setWm(q.data.wm); setYm(q.data.ym) }
  }, [q.data])

  const apply = async () => {
    await api.setMatter(uuid, { dm, wm, ym })
    qc.invalidateQueries({ queryKey: ['matter', uuid] })
  }

  return (
    <div className="card">
      <h3 className="font-bold mb-3">⚛ Perfil de Matéria</h3>
      <Slider label="Matéria Escura" value={dm} setValue={setDm} color="bg-purple-500" />
      <Slider label="Matéria Clara" value={wm} setValue={setWm} color="bg-white" />
      <Slider label="Matéria Amarela" value={ym} setValue={setYm} color="bg-yellow-400" />
      <div className="text-sm text-liberthia-300 mt-2">Tipo ativo: <span className="font-bold">{q.data?.type ?? '?'}</span></div>
      <button className="btn mt-3 w-full" onClick={apply}>Aplicar valores</button>
    </div>
  )
}

function Slider({ label, value, setValue, color }: { label: string; value: number; setValue: (v: number) => void; color: string }) {
  return (
    <div className="mb-3">
      <div className="flex justify-between text-sm mb-1">
        <span>{label}</span>
        <span className="font-mono">{value.toFixed(0)}</span>
      </div>
      <input
        type="range"
        min={0}
        max={100}
        step={1}
        value={value}
        onChange={e => setValue(parseInt(e.target.value))}
        className="w-full accent-liberthia-400"
      />
      <div className="h-1.5 bg-liberthia-900 rounded mt-1 overflow-hidden">
        <div className={`h-full ${color} opacity-60`} style={{ width: `${value}%` }} />
      </div>
    </div>
  )
}
