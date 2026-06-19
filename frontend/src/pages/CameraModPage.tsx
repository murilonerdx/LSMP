import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Camera Mod (camera-forge) — kits de câmera + POV cinemático.
 *
 * O mod salva fotos client-side, então a integração admin é limitada a:
 *  - Dar câmera + filme + álbum pra players
 *  - TP admin pra POV de outro player (spectator)
 *  - Forçar modo cinematic em um player (spectator durante cutscene)
 */

export function CameraModPage() {
  const [kitTarget, setKitTarget] = useState('')
  const [withAlbum, setWithAlbum] = useState(true)
  const [photoCount, setPhotoCount] = useState(16)
  const [povAdmin, setPovAdmin] = useState('')
  const [povTarget, setPovTarget] = useState('')

  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 8000 })
  const players = Array.isArray(playersQ.data) ? playersQ.data : []

  const giveMut = useMutation({
    mutationFn: api.cameraGiveKit,
    onSuccess: () => toast.ok('📷 kit entregue'),
    onError: (e: any) => toast.err(e.message),
  })
  const povMut = useMutation({
    mutationFn: ({ admin, target }: { admin: string; target: string }) => api.cameraPov(admin, target),
    onSuccess: () => toast.ok('👁 admin em POV'),
    onError: (e: any) => toast.err(e.message),
  })
  const povExitMut = useMutation({
    mutationFn: api.cameraPovExit,
    onSuccess: () => toast.ok('↩ POV encerrado'),
    onError: (e: any) => toast.err(e.message),
  })
  const cinematicMut = useMutation({
    mutationFn: ({ name, enable }: { name: string; enable: boolean }) => api.cameraCinematic(name, enable),
    onSuccess: () => toast.ok('🎬 modo cinematic'),
    onError: (e: any) => toast.err(e.message),
  })

  return (
    <div className="route-fade max-w-[1200px] space-y-4">
      <header>
        <h1 className="page-title">📷 Camera Mod</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Kits de câmera (camera-forge) + POV admin + modo cinematic. O mod salva fotos
          <em> client-side</em>, então elas não vêm pro backend — pra fotos auto-upload, use o Exposure (já integrado).
        </p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">

        {/* Kit de câmera */}
        <div className="card-glow space-y-2">
          <h3 className="font-bold">🎁 Dar kit de câmera</h3>
          <select className="input" value={kitTarget} onChange={(e) => setKitTarget(e.target.value)}>
            <option value="">Selecione um player…</option>
            {players.map(p => <option key={p.uuid} value={p.name}>{p.name}</option>)}
          </select>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div>
              <label className="label">Filmes</label>
              <input type="number" min={0} max={64} className="input"
                value={photoCount} onChange={(e) => setPhotoCount(Number(e.target.value) || 0)} />
            </div>
            <label className="flex items-center gap-2 mt-5">
              <input type="checkbox" checked={withAlbum} onChange={(e) => setWithAlbum(e.target.checked)} />
              <span>+ Álbum</span>
            </label>
          </div>
          <button className="btn w-full" disabled={!kitTarget}
            onClick={() => giveMut.mutate({ playerName: kitTarget, withAlbum, photoCount })}>
            📷 Entregar kit
          </button>
        </div>

        {/* POV admin */}
        <div className="card-glow space-y-2">
          <h3 className="font-bold">👁 POV Admin (spectator)</h3>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div>
              <label className="label">Admin (vai ver)</label>
              <select className="input" value={povAdmin} onChange={(e) => setPovAdmin(e.target.value)}>
                <option value="">…</option>
                {players.map(p => <option key={p.uuid} value={p.name}>{p.name}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Alvo (vai ser visto)</label>
              <select className="input" value={povTarget} onChange={(e) => setPovTarget(e.target.value)}>
                <option value="">…</option>
                {players.map(p => <option key={p.uuid} value={p.name}>{p.name}</option>)}
              </select>
            </div>
          </div>
          <div className="flex gap-1">
            <button className="btn flex-1" disabled={!povAdmin || !povTarget}
              onClick={() => povMut.mutate({ admin: povAdmin, target: povTarget })}>
              🔍 Entrar em POV
            </button>
            <button className="btn-ghost" disabled={!povAdmin}
              onClick={() => povExitMut.mutate(povAdmin)}>
              ↩ Sair
            </button>
          </div>
          <p className="text-[10px] text-liberthia-300/50 italic">
            Admin entra em spectator + spectate. Sai voltando pra survival.
          </p>
        </div>

        {/* Modo cinematic forçado */}
        <div className="card-glow space-y-2 md:col-span-2">
          <h3 className="font-bold">🎬 Modo Cinematic Forçado</h3>
          <p className="text-xs text-liberthia-300/60">
            Joga 1 player em spectator pra ele só assistir o evento (não pode mexer). Útil pra cutscenes
            em que ele deve ficar parado.
          </p>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 items-end">
            <div>
              <label className="label">Player</label>
              <select className="input"
                onChange={(e) => {
                  const name = e.target.value
                  if (!name) return
                  cinematicMut.mutate({ name, enable: true })
                }}>
                <option value="">Selecione pra entrar em cinematic…</option>
                {players.map(p => <option key={p.uuid} value={p.name}>{p.name}</option>)}
              </select>
            </div>
            <div className="md:col-span-2 flex gap-1">
              {players.map(p => (
                <button key={p.uuid} className="btn-ghost btn-sm flex-1"
                  onClick={() => cinematicMut.mutate({ name: p.name, enable: false })}
                  title={`Volta ${p.name} pra survival`}>
                  ↩ {p.name}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
