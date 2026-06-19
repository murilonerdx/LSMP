import { useEffect, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ParticleEntry } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Particle Designer — substitui a textura de partículas vanilla por imagens custom.
 *
 * Vanilla MC NÃO permite adicionar partículas novas via resource pack —
 * só substituir as existentes. Então o fluxo é:
 *   1. Escolhe uma partícula vanilla pra "sacrificar" (ex: glow)
 *   2. Upload de PNG (idealmente 8x8 ou 16x16 com transparência)
 *   3. Backend gera resource pack ZIP que substitui essa textura
 *   4. Servidor MC carrega o pack via server.properties
 *   5. /particle minecraft:glow agora mostra sua imagem
 *
 * Suporta também canvas drawing pra criar do zero.
 */

// Partículas vanilla que aceitam override de textura (.png simples no path):
// Listadas no assets/minecraft/textures/particle/ da vanilla 1.20+
const OVERRIDABLE = [
  { key: 'glow', emoji: '✨', label: 'Glow (squid)' },
  { key: 'soul_fire_flame', emoji: '🟦', label: 'Soul Flame' },
  { key: 'flame', emoji: '🔥', label: 'Flame' },
  { key: 'end_rod', emoji: '⚪', label: 'End Rod' },
  { key: 'fish', emoji: '🐟', label: 'Fish' },
  { key: 'heart', emoji: '❤', label: 'Heart' },
  { key: 'splash', emoji: '💧', label: 'Splash' },
  { key: 'crit', emoji: '⚔', label: 'Crit' },
  { key: 'enchanted_hit', emoji: '✨', label: 'Enchant Hit' },
  { key: 'nautilus', emoji: '🐚', label: 'Nautilus' },
  { key: 'bubble', emoji: '🫧', label: 'Bubble' },
  { key: 'note', emoji: '🎵', label: 'Note' },
  { key: 'angry', emoji: '💢', label: 'Angry' },
  { key: 'effect_0', emoji: '💫', label: 'Effect 0' },
  { key: 'effect_1', emoji: '💫', label: 'Effect 1' },
  { key: 'glitter_0', emoji: '⭐', label: 'Glitter 0' },
  { key: 'sneeze', emoji: '🤧', label: 'Sneeze' },
  { key: 'sweep_0', emoji: '⚔', label: 'Sweep 0' },
  { key: 'drip_hang', emoji: '💧', label: 'Drip Hang' },
  { key: 'flash', emoji: '⚡', label: 'Flash' },
] as const

const PRESET_COLORS = ['#ffffff', '#aa40e8', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#000000', '#ffff00', '#ff0080']

export function ParticleDesignerPage() {
  const qc = useQueryClient()
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const listQ = useQuery({ queryKey: ['particles'], queryFn: api.particlesList })

  const [mode, setMode] = useState<'upload' | 'draw'>('upload')
  const [overrideKey, setOverrideKey] = useState('glow')
  const [file, setFile] = useState<File | null>(null)
  const [filePreview, setFilePreview] = useState<string>('')
  const [uploading, setUploading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  // Canvas drawing
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const [size, setSize] = useState(16)
  const [color, setColor] = useState('#aa40e8')
  const [tool, setTool] = useState<'paint' | 'erase'>('paint')
  const [pixels, setPixels] = useState<string[]>(() => Array(16 * 16).fill('transparent'))
  const drawingRef = useRef(false)

  // Reset pixels quando size muda
  useEffect(() => {
    setPixels(Array(size * size).fill('transparent'))
  }, [size])

  // Render canvas pra preview
  useEffect(() => {
    if (mode !== 'draw') return
    const c = canvasRef.current; if (!c) return
    const ctx = c.getContext('2d'); if (!ctx) return
    const SCALE = 16
    c.width = size * SCALE; c.height = size * SCALE
    ctx.imageSmoothingEnabled = false
    // Checker background pra transparência visível
    for (let y = 0; y < size; y++) {
      for (let x = 0; x < size; x++) {
        ctx.fillStyle = ((x + y) % 2 === 0) ? 'rgba(80,60,100,0.4)' : 'rgba(40,30,60,0.4)'
        ctx.fillRect(x * SCALE, y * SCALE, SCALE, SCALE)
      }
    }
    // Pixels
    for (let i = 0; i < pixels.length; i++) {
      const x = i % size, y = Math.floor(i / size)
      if (pixels[i] !== 'transparent') {
        ctx.fillStyle = pixels[i]
        ctx.fillRect(x * SCALE, y * SCALE, SCALE, SCALE)
      }
    }
    // Grid
    ctx.strokeStyle = 'rgba(255,255,255,0.05)'
    ctx.lineWidth = 1
    for (let i = 0; i <= size; i++) {
      ctx.beginPath()
      ctx.moveTo(i * SCALE, 0); ctx.lineTo(i * SCALE, size * SCALE)
      ctx.moveTo(0, i * SCALE); ctx.lineTo(size * SCALE, i * SCALE)
      ctx.stroke()
    }
  }, [pixels, size, mode])

  function pixelAt(e: React.MouseEvent | React.PointerEvent) {
    const c = canvasRef.current; if (!c) return -1
    const r = c.getBoundingClientRect()
    const x = Math.floor(((e as any).clientX - r.left) / (r.width / size))
    const y = Math.floor(((e as any).clientY - r.top) / (r.height / size))
    if (x < 0 || y < 0 || x >= size || y >= size) return -1
    return y * size + x
  }

  function paint(e: React.MouseEvent | React.PointerEvent) {
    const i = pixelAt(e); if (i < 0) return
    setPixels((pr) => {
      const n = [...pr]
      n[i] = tool === 'paint' ? color : 'transparent'
      return n
    })
  }

  async function uploadFromCanvas() {
    const c = canvasRef.current; if (!c) return
    // Export pixels como PNG sem o checker background — recriar canvas só com pixels
    const tmp = document.createElement('canvas')
    tmp.width = size; tmp.height = size
    const tctx = tmp.getContext('2d')!
    tctx.clearRect(0, 0, size, size)
    for (let i = 0; i < pixels.length; i++) {
      if (pixels[i] === 'transparent') continue
      const x = i % size, y = Math.floor(i / size)
      tctx.fillStyle = pixels[i]
      tctx.fillRect(x, y, 1, 1)
    }
    const blob: Blob | null = await new Promise((res) => tmp.toBlob(res, 'image/png'))
    if (!blob) { toast.err('Falha ao gerar PNG'); return }
    await doUpload(new File([blob], `${overrideKey}.png`, { type: 'image/png' }))
  }

  async function doUpload(f: File) {
    setUploading(true)
    try {
      const r: any = await api.particleUpload(f, overrideKey)
      toast.ok(`✓ ${r.particleId} (${(r.sizeBytes / 1024).toFixed(1)} KB)`)
      qc.invalidateQueries({ queryKey: ['particles'] })
      setFile(null); setFilePreview('')
      if (fileInputRef.current) fileInputRef.current.value = ''
    } catch (e: any) { toast.err(e.message) }
    setUploading(false)
  }

  async function uploadFile() {
    if (!file) return
    await doUpload(file)
  }

  async function remove(key: string) {
    if (!confirm(`Remover override de ${key}?`)) return
    try {
      await api.particleDelete(key)
      qc.invalidateQueries({ queryKey: ['particles'] })
      toast.ok('Removido')
    } catch (e: any) { toast.err(e.message) }
  }

  async function testFire(playerUuid: string, key: string) {
    if (!playerUuid) { toast.err('Selecione player'); return }
    const player = players.find((p) => p.uuid === playerUuid); if (!player) return
    try {
      // Dispara perto do player
      await api.particle(`minecraft:${key}`, player.position.x, player.position.y + 1, player.position.z, 40)
      toast.ok(`✨ minecraft:${key} no ${player.name}`)
    } catch (e: any) { toast.err(e.message) }
  }

  const packUrl = `${window.location.origin}/api/particles-pack.zip`
  const sha1 = listQ.data?.packSha1 ?? ''
  const existing = listQ.data?.particles ?? []
  const [testTarget, setTestTarget] = useState('')

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6">
        <h1 className="page-title">🎨 Particle Designer</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Substitui a textura de partículas vanilla com sua arte. Upload PNG ou pinta no canvas. Vanilla MC só permite
          override — não criar partículas novas — mas você pode "sacrificar" várias.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_400px] gap-5">
        <div className="space-y-4">
          {/* Tabs */}
          <div className="tab-strip">
            <div className={`tab-item ${mode === 'upload' ? 'active' : ''}`} onClick={() => setMode('upload')}>📁 Upload PNG</div>
            <div className={`tab-item ${mode === 'draw' ? 'active' : ''}`} onClick={() => setMode('draw')}>🎨 Pintar no canvas</div>
          </div>

          {/* Override picker */}
          <div className="card-glow">
            <h3 className="font-bold mb-2">🎯 Substituir qual partícula vanilla?</h3>
            <p className="text-xs text-liberthia-300/60 mb-3">
              A partícula original será substituída pela sua arte. Use uma que você não usa muito.
            </p>
            <div className="grid grid-cols-3 sm:grid-cols-5 gap-1.5">
              {OVERRIDABLE.map((p) => (
                <button key={p.key}
                  className={`btn-ghost btn-sm flex flex-col items-center !py-2 ${overrideKey === p.key ? '!bg-liberthia-500/30 !text-white !border-liberthia-400/60' : ''}`}
                  onClick={() => setOverrideKey(p.key)}>
                  <span className="text-2xl">{p.emoji}</span>
                  <span className="text-[10px]">{p.label}</span>
                </button>
              ))}
            </div>
            <div className="mt-2 text-xs text-liberthia-300/70">
              ID: <code className="bg-black/40 px-2 py-0.5 rounded font-mono">minecraft:{overrideKey}</code>
            </div>
          </div>

          {/* Upload mode */}
          {mode === 'upload' && (
            <div className="card-glow">
              <h3 className="font-bold mb-2">📤 Upload PNG</h3>
              <p className="text-xs text-liberthia-300/60 mb-3">
                Tamanho recomendado: <b>8×8</b> ou <b>16×16</b> px com fundo transparente. PNGs maiores funcionam mas ficam pixelados.
              </p>
              <input ref={fileInputRef} type="file" accept=".png,image/png" className="hidden"
                onChange={(e) => {
                  const f = e.target.files?.[0]
                  if (f) {
                    setFile(f)
                    const reader = new FileReader()
                    reader.onload = () => setFilePreview(reader.result as string)
                    reader.readAsDataURL(f)
                  }
                }} />
              <button className="btn-ghost w-full mb-3" onClick={() => fileInputRef.current?.click()}>
                📁 {file ? file.name : 'Escolher .png'}
              </button>
              {filePreview && (
                <div className="bg-liberthia-900/40 border border-liberthia-500/20 rounded-lg p-3 mb-3 flex items-center gap-3">
                  <img src={filePreview} alt="" className="w-16 h-16 rounded border border-liberthia-500/30" style={{ imageRendering: 'pixelated' }} />
                  <div className="text-xs">
                    <div className="font-mono">{file?.name}</div>
                    <div className="text-liberthia-300/60">{file && (file.size / 1024).toFixed(1)} KB</div>
                  </div>
                </div>
              )}
              <button className="btn w-full" onClick={uploadFile} disabled={!file || uploading}>
                {uploading ? '⏳ Enviando...' : `📤 Substituir minecraft:${overrideKey}`}
              </button>
            </div>
          )}

          {/* Draw mode */}
          {mode === 'draw' && (
            <div className="card-glow">
              <h3 className="font-bold mb-2">🎨 Pintar pixel-art</h3>
              <div className="flex flex-wrap gap-2 mb-3 items-center">
                <span className="label">Tamanho:</span>
                {[8, 16, 32].map((s) => (
                  <button key={s} className={`btn-ghost btn-sm ${size === s ? '!bg-liberthia-500/30 !text-white' : ''}`} onClick={() => setSize(s)}>
                    {s}×{s}
                  </button>
                ))}
                <span className="label ml-3">Ferramenta:</span>
                <button className={`btn-ghost btn-sm ${tool === 'paint' ? '!bg-liberthia-500/30 !text-white' : ''}`} onClick={() => setTool('paint')}>✏ Paint</button>
                <button className={`btn-ghost btn-sm ${tool === 'erase' ? '!bg-liberthia-500/30 !text-white' : ''}`} onClick={() => setTool('erase')}>🧹 Apagar</button>
              </div>

              <div className="flex gap-2 mb-3 flex-wrap items-center">
                <span className="label">Cor:</span>
                <input type="color" className="input h-9 w-14" value={color} onChange={(e) => setColor(e.target.value)} />
                <div className="flex gap-1 flex-wrap">
                  {PRESET_COLORS.map((c) => (
                    <button key={c} type="button" className="w-7 h-7 rounded border border-white/20" style={{ background: c }} onClick={() => setColor(c)} />
                  ))}
                </div>
                <button className="btn-ghost btn-sm ml-auto" onClick={() => setPixels(Array(size * size).fill('transparent'))}>🧹 Limpar tudo</button>
              </div>

              <canvas
                ref={canvasRef}
                className="rounded-lg cursor-crosshair select-none mx-auto block"
                style={{ width: 'min(100%, 480px)', height: 'auto', imageRendering: 'pixelated' }}
                onPointerDown={(e) => { drawingRef.current = true; paint(e) }}
                onPointerUp={() => { drawingRef.current = false }}
                onPointerLeave={() => { drawingRef.current = false }}
                onPointerMove={(e) => { if (drawingRef.current) paint(e) }}
              />

              <button className="btn w-full mt-3" onClick={uploadFromCanvas} disabled={uploading}>
                {uploading ? '⏳' : `📤 Salvar como minecraft:${overrideKey}`}
              </button>
            </div>
          )}

          {/* Existing overrides */}
          <div className="card-glow">
            <h3 className="font-bold mb-3 flex items-center gap-2">
              🖼 Overrides ativos <span className="badge badge-purple">{existing.length}</span>
            </h3>
            {existing.length === 0 && (
              <div className="text-center py-8 text-liberthia-300/50 text-sm">
                Nenhum override ainda. Faça upload ou pinte acima.
              </div>
            )}
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-2">
              {existing.map((p: ParticleEntry) => (
                <div key={p.key} className="p-2 rounded-lg bg-liberthia-900/40 border border-liberthia-500/20">
                  <img src={api.particlePreviewUrl(p.key)} alt={p.key}
                       className="w-full aspect-square rounded mb-1 bg-checker"
                       style={{ imageRendering: 'pixelated', background: 'repeating-conic-gradient(#332 0 25%, #221 0 50%) 50% / 16px 16px' }} />
                  <div className="font-mono text-[10px] truncate">{p.particleId}</div>
                  <div className="text-[9px] text-liberthia-300/50">{(p.sizeBytes / 1024).toFixed(1)} KB</div>
                  <div className="grid grid-cols-2 gap-1 mt-1">
                    <select className="input text-[10px] !py-0.5 !px-1"
                      onChange={(e) => { if (e.target.value) { testFire(e.target.value, p.key); e.target.value = '' } }}>
                      <option value="">Testar...</option>
                      {players.map((pl) => <option key={pl.uuid} value={pl.uuid}>{pl.name}</option>)}
                    </select>
                    <button className="btn-ghost btn-sm !text-[10px]" onClick={() => remove(p.key)}>🗑</button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-4">
          <div className="card-glow">
            <h3 className="font-bold mb-3">⚙ Setup do servidor</h3>
            <p className="text-xs text-liberthia-300/70 mb-3">
              <b>Atenção:</b> server.properties só aceita 1 resource pack. Se você já tem o pack de sons,
              os players têm que escolher um. Pra ter ambos, mescla os 2 ZIPs manualmente OU sirva como pack opcional.
            </p>
            <div className="bg-black/60 rounded-lg p-3 font-mono text-[11px] mb-2 break-all">
              resource-pack=<span className="text-emerald-300">{packUrl}</span><br />
              {sha1 && <>resource-pack-sha1=<span className="text-cyan-300">{sha1}</span></>}
            </div>
            <button className="btn-ghost btn-sm w-full"
              onClick={() => {
                navigator.clipboard.writeText(`resource-pack=${packUrl}\nresource-pack-sha1=${sha1}\n`)
                toast.ok('Copiado')
              }}>📋 Copiar config</button>
          </div>

          <div className="card">
            <h3 className="font-bold mb-2">📥 Pack atual</h3>
            <div className="text-xs space-y-1.5 text-liberthia-300/70">
              <div className="flex justify-between"><span>Overrides:</span><span className="font-mono">{existing.length}</span></div>
              <div className="flex justify-between"><span>Tamanho:</span><span className="font-mono">{((listQ.data?.packSize ?? 0) / 1024).toFixed(1)} KB</span></div>
              <div className="flex justify-between"><span>SHA1:</span><span className="font-mono text-[9px] truncate ml-2">{sha1 || '—'}</span></div>
            </div>
            <a className="btn w-full mt-3 inline-block text-center no-underline" href="/api/particles-pack.zip" download>⬇ Download pack</a>
          </div>

          <div className="card text-xs text-liberthia-300/70">
            <div className="font-bold text-liberthia-200 mb-2">💡 Dicas</div>
            <p>• Use uma partícula que você não usa muito como "sacrificada"</p>
            <p>• 8×8 ou 16×16 é o ideal — vanilla particles são pequenas</p>
            <p>• Transparência funciona, use PNGs com alpha</p>
            <p>• Após upload, players precisam re-aceitar o pack (SHA1 muda)</p>
            <p>• Use via <span className="chip">/particle minecraft:{`<key>`}</span> ou nos editores de Atmosphere</p>
          </div>
        </div>
      </div>
    </div>
  )
}
