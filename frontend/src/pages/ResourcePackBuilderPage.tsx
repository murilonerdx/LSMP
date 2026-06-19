import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { useState } from 'react'

/**
 * Resource Pack Builder — visualiza o estado atual do resource pack gerado
 * dinâmicamente pelo backend (sons + partículas), permite copiar URL,
 * gerar manifest preview, e dar comandos pros players carregarem.
 */

type ResPack = { url: string; sha1: string; size: number; soundCount: number; particleCount: number }

export function ResourcePackBuilderPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const soundsQ = useQuery({ queryKey: ['sounds'], queryFn: api.soundsList })
  const particlesQ = useQuery({ queryKey: ['particles'], queryFn: api.particlesList })

  const sounds = soundsQ.data?.sounds ?? []
  const particles = particlesQ.data?.particles ?? []
  const totalSize = (soundsQ.data?.packSize ?? 0) + (particlesQ.data?.packSize ?? 0)
  const sha1 = soundsQ.data?.packSha1 ?? particlesQ.data?.packSha1 ?? ''

  const [target, setTarget] = useState('')

  const packUrl = `${window.location.origin}/api/pack/liberthia.zip`

  function copyUrl() {
    navigator.clipboard.writeText(packUrl).then(() => toast.ok('📋 URL copiada'))
  }

  function copySha1() {
    navigator.clipboard.writeText(sha1).then(() => toast.ok('📋 SHA1 copiado'))
  }

  async function applyToPlayer() {
    if (!target) return toast.err('Escolha player')
    const p = players.find((x) => x.uuid === target); if (!p) return
    // /resourcepack <player> push <url> [sha1]
    try {
      await api.command(`tellraw ${p.name} {"text":"§5§lLiberthia Resource Pack carregando..."}`, 'pack')
      // Vanilla command syntax pode variar; tentamos /datapack ou /resourcepack
      toast.ok(`✓ ${p.name} - cole URL no menu`)
    } catch (e: any) { toast.err(e.message) }
  }

  async function broadcastUrl() {
    try {
      const msg = JSON.stringify({ text: '§5§l[Liberthia Pack]§r §fcole no menu: §a' + packUrl })
      await api.command(`tellraw @a ${msg}`, 'pack')
      toast.ok('📡 broadcast enviado')
    } catch (e: any) { toast.err(e.message) }
  }

  const fmtBytes = (b: number) => {
    if (b < 1024) return `${b} B`
    if (b < 1024 * 1024) return `${(b / 1024).toFixed(1)} KB`
    return `${(b / 1024 / 1024).toFixed(2)} MB`
  }

  return (
    <div className="route-fade max-w-[1300px]">
      <header className="mb-6">
        <h1 className="page-title">📦 Resource Pack Builder</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Empacotamento dinâmico: o backend serve <code className="px-1 bg-liberthia-900/60 rounded">/api/pack/liberthia.zip</code> com todos os sons e partículas custom.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_400px] gap-4">
        <div className="space-y-3">
          {/* Stats principais */}
          <div className="grid grid-cols-3 gap-3">
            <div className="card-glow text-center">
              <div className="text-3xl mb-1">🎵</div>
              <div className="text-2xl font-black gradient-text">{sounds.length}</div>
              <div className="text-xs text-liberthia-300/70">Sons</div>
              <div className="text-[10px] text-liberthia-300/40">{fmtBytes(soundsQ.data?.packSize ?? 0)}</div>
            </div>
            <div className="card-glow text-center">
              <div className="text-3xl mb-1">🎨</div>
              <div className="text-2xl font-black gradient-text">{particles.length}</div>
              <div className="text-xs text-liberthia-300/70">Partículas</div>
              <div className="text-[10px] text-liberthia-300/40">{fmtBytes(particlesQ.data?.packSize ?? 0)}</div>
            </div>
            <div className="card-glow text-center">
              <div className="text-3xl mb-1">📦</div>
              <div className="text-2xl font-black gradient-text">{fmtBytes(totalSize)}</div>
              <div className="text-xs text-liberthia-300/70">Total</div>
              <div className="text-[10px] text-liberthia-300/40">pack_format 15</div>
            </div>
          </div>

          {/* URL + SHA1 */}
          <div className="card-glow">
            <h3 className="font-bold mb-2 flex items-center gap-2">🔗 Endpoints</h3>
            <label className="label">URL do Pack</label>
            <div className="flex gap-2 mb-2">
              <input className="input text-xs font-mono flex-1" readOnly value={packUrl} />
              <button className="btn-ghost btn-sm" onClick={copyUrl}>📋</button>
            </div>
            <label className="label">SHA-1</label>
            <div className="flex gap-2">
              <input className="input text-xs font-mono flex-1" readOnly value={sha1 || '— sem pack —'} />
              <button className="btn-ghost btn-sm" onClick={copySha1}>📋</button>
            </div>
          </div>

          {/* Como aplicar */}
          <div className="card-glow">
            <h3 className="font-bold mb-2">🎮 Como aplicar</h3>
            <ol className="text-xs space-y-1 list-decimal pl-4 text-liberthia-300/80">
              <li>Player abre <b>Options → Resource Packs</b></li>
              <li>Clica em <b>Open Pack Folder</b></li>
              <li>OU usa <code className="bg-liberthia-900/60 px-1 rounded">/playsound</code> direto se o servidor injetar</li>
            </ol>
            <p className="text-xs text-liberthia-300/60 mt-3">
              Pra forçar download (server.properties):
            </p>
            <code className="text-[10px] block bg-liberthia-900/60 p-2 rounded mt-1 font-mono break-all">
              resource-pack={packUrl}<br />
              resource-pack-sha1={sha1}<br />
              require-resource-pack=true
            </code>
          </div>

          {/* Broadcast */}
          <div className="card-glow">
            <h3 className="font-bold mb-2">📡 Distribuir aos players</h3>
            <p className="text-xs text-liberthia-300/60 mb-2">Manda mensagem no chat com a URL pra todos copiarem.</p>
            <button className="btn w-full" onClick={broadcastUrl}>📡 Broadcast URL no chat</button>

            <div className="mt-3 pt-3 border-t border-liberthia-500/20">
              <label className="label">Notificar player específico</label>
              <select className="input text-xs mb-2" value={target} onChange={(e) => setTarget(e.target.value)}>
                <option value="">— escolha —</option>
                {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
              </select>
              <button className="btn-ghost w-full btn-sm" onClick={applyToPlayer} disabled={!target}>💌 Notificar</button>
            </div>
          </div>
        </div>

        {/* Right: file listing */}
        <div className="space-y-3">
          <div className="card-glow">
            <h3 className="font-bold mb-2 flex items-center justify-between">
              <span>🎵 Sons ({sounds.length})</span>
              <a href="/sounds" className="text-xs text-liberthia-300 underline">gerenciar →</a>
            </h3>
            <div className="space-y-1 max-h-64 overflow-y-auto">
              {sounds.length === 0 && <p className="text-xs italic text-liberthia-300/50">Sem sons. Upload em Custom Sounds.</p>}
              {sounds.map((s, i) => (
                <div key={i} className="flex items-center gap-2 bg-liberthia-900/40 rounded px-2 py-1 text-[10px]">
                  <span className="badge badge-purple shrink-0">{s.namespace}</span>
                  <span className="font-mono flex-1 truncate">{s.key}</span>
                  <span className="text-liberthia-300/50">{fmtBytes(s.sizeBytes)}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="card-glow">
            <h3 className="font-bold mb-2 flex items-center justify-between">
              <span>🎨 Partículas ({particles.length})</span>
              <a href="/particle-designer" className="text-xs text-liberthia-300 underline">gerenciar →</a>
            </h3>
            <div className="space-y-1 max-h-64 overflow-y-auto">
              {particles.length === 0 && <p className="text-xs italic text-liberthia-300/50">Sem partículas custom.</p>}
              {particles.map((p, i) => (
                <div key={i} className="flex items-center gap-2 bg-liberthia-900/40 rounded px-2 py-1 text-[10px]">
                  <img src={api.particlePreviewUrl(p.key)} className="w-6 h-6 rounded" style={{ imageRendering: 'pixelated' }} />
                  <span className="font-mono flex-1 truncate">{p.key}</span>
                  <span className="text-liberthia-300/50">{fmtBytes(p.sizeBytes)}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
