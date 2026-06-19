import { useEffect, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, SoundEntry } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Custom Sounds — upload de .ogg + geração de resource pack.
 *
 * Fluxo:
 *  1. Operador faz upload de N .ogg
 *  2. Backend salva em ./liberthia_sounds/<namespace>/<key>.ogg
 *  3. Backend gera resource pack ZIP em /api/sounds-pack.zip (público)
 *  4. Operador coloca a URL no server.properties:
 *       resource-pack=http://<backend>/api/sounds-pack.zip
 *       resource-pack-sha1=<hash mostrado aqui>
 *  5. Players aceitam o pack ao entrar; comandos /playsound liberthia:<key> funcionam
 *
 * IMPORTANTE: re-aplicar pack após upload novo (sha1 mudou).
 */

export function SoundsPage() {
  const qc = useQueryClient()
  const listQ = useQuery({ queryKey: ['sounds'], queryFn: api.soundsList })
  const [namespace, setNamespace] = useState('liberthia')
  const [key, setKey] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const fileRef = useRef<HTMLInputElement>(null)

  async function upload() {
    if (!file) { toast.err('Selecione um arquivo .ogg'); return }
    if (!key) { toast.err('Defina uma key (ex: ancient_horn)'); return }
    if (!file.name.toLowerCase().endsWith('.ogg')) { toast.err('Apenas .ogg é aceito'); return }
    setUploading(true)
    try {
      const r: any = await api.soundUpload(file, namespace, key)
      toast.ok(`✓ ${r.playId} (${(r.sizeBytes / 1024).toFixed(1)} KB)`)
      setKey(''); setFile(null)
      if (fileRef.current) fileRef.current.value = ''
      qc.invalidateQueries({ queryKey: ['sounds'] })
    } catch (e: any) { toast.err(e.message) }
    setUploading(false)
  }

  async function remove(s: SoundEntry) {
    if (!confirm(`Remover ${s.playId}?`)) return
    try {
      await api.soundDelete(s.namespace, s.key)
      toast.ok('Removido')
      qc.invalidateQueries({ queryKey: ['sounds'] })
    } catch (e: any) { toast.err(e.message) }
  }

  // Preview com tag <audio>
  function previewUrl(s: SoundEntry) {
    // Direto do pack ZIP não dá; servimos no /api/sounds-pack.zip mas precisa unpack
    // Workaround: usa o endpoint sounds que serve o ogg cru
    // (não existe ainda, vamos criar inline; por ora usa o zip)
    return `/api/sounds-pack.zip`
  }

  const packUrl = `${window.location.origin}/api/sounds-pack.zip`
  const sha1 = listQ.data?.packSha1 ?? ''
  const sounds = listQ.data?.sounds ?? []
  const totalKb = sounds.reduce((a, s) => a + s.sizeBytes, 0) / 1024

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6">
        <h1 className="page-title">🎵 Custom Sounds</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Upload seus próprios .ogg, gera um resource pack que o servidor envia pros clientes,
          e usa via <span className="chip">/playsound liberthia:&lt;key&gt;</span> ou nas Story Beats.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_400px] gap-5">
        <div className="space-y-4">
          {/* Upload card */}
          <div className="card-glow">
            <h3 className="font-bold mb-3 flex items-center gap-2">📤 Upload</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-2 mb-3">
              <div>
                <label className="label block mb-1">Namespace</label>
                <input className="input font-mono text-xs" value={namespace}
                  onChange={(e) => setNamespace(e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '_'))} />
                <div className="text-[10px] text-liberthia-300/40 mt-1">só [a-z0-9_]</div>
              </div>
              <div>
                <label className="label block mb-1">Key (nome do som)</label>
                <input className="input font-mono text-xs" value={key} placeholder="ancient_horn"
                  onChange={(e) => setKey(e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '_'))} />
              </div>
            </div>

            <label className="label block mb-1">Arquivo .ogg</label>
            <div className="flex items-center gap-2 mb-3">
              <input ref={fileRef} type="file" accept=".ogg,audio/ogg" className="hidden"
                onChange={(e) => {
                  const f = e.target.files?.[0]
                  if (f) {
                    setFile(f)
                    // Auto-fill key com nome do arquivo se vazio
                    if (!key) {
                      const base = f.name.replace(/\.ogg$/i, '').toLowerCase().replace(/[^a-z0-9_]/g, '_')
                      setKey(base)
                    }
                  }
                }} />
              <button className="btn-ghost flex-1" onClick={() => fileRef.current?.click()}>
                📁 {file ? file.name : 'Escolher .ogg'}
              </button>
              {file && (
                <button className="btn-ghost btn-sm" onClick={() => { setFile(null); if (fileRef.current) fileRef.current.value = '' }}>🗑</button>
              )}
            </div>
            {file && (
              <div className="text-xs text-liberthia-300/60 mb-3">
                {file.name} · {(file.size / 1024).toFixed(1)} KB
              </div>
            )}

            <button className="btn w-full" onClick={upload} disabled={uploading || !file || !key}>
              {uploading ? '⏳ Enviando...' : `📤 Upload (${namespace}:${key || '?'})`}
            </button>

            <div className="mt-3 text-xs text-liberthia-300/50 leading-relaxed">
              <p>• Arquivos <span className="chip">.ogg Vorbis</span> são suportados pelo MC vanilla</p>
              <p>• Converter MP3/WAV → OGG: <a className="underline text-liberthia-300" href="https://convertio.co/mp3-ogg/" target="_blank" rel="noopener">convertio.co</a> ou <code>ffmpeg -i in.mp3 out.ogg</code></p>
            </div>
          </div>

          {/* Lista */}
          <div className="card-glow">
            <h3 className="font-bold mb-3 flex items-center gap-2">
              🎶 Sons no servidor
              <span className="badge badge-purple ml-2">{sounds.length}</span>
              {sounds.length > 0 && <span className="text-xs text-liberthia-300/50">· {totalKb.toFixed(1)} KB total</span>}
            </h3>
            {sounds.length === 0 && (
              <div className="text-center py-8 text-liberthia-300/50">
                <div className="text-4xl mb-2 opacity-50">🎵</div>
                <p className="text-sm">Nenhum som ainda. Faça upload acima.</p>
              </div>
            )}
            <div className="space-y-1.5">
              {sounds.map((s) => (
                <div key={`${s.namespace}:${s.key}`} className="flex items-center gap-2 p-2 rounded-lg bg-liberthia-900/40 border border-liberthia-500/20">
                  <span className="text-2xl">🎵</span>
                  <div className="flex-1 min-w-0">
                    <div className="font-mono text-sm font-bold truncate">{s.playId}</div>
                    <div className="text-[10px] text-liberthia-300/50">{(s.sizeBytes / 1024).toFixed(1)} KB</div>
                  </div>
                  <button className="btn-ghost btn-sm" title="Copiar /playsound"
                    onClick={() => {
                      navigator.clipboard.writeText(`playsound ${s.playId} master @a`)
                      toast.ok('Comando copiado')
                    }}>📋</button>
                  <button className="btn-ghost btn-sm" onClick={() => remove(s)}>🗑</button>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Sidebar: setup instructions */}
        <div className="space-y-4">
          <div className="card-glow">
            <h3 className="font-bold mb-3 flex items-center gap-2">⚙ Configurar servidor</h3>
            <p className="text-xs text-liberthia-300/70 mb-3">
              Cole isto no <code className="bg-black/40 px-1 rounded">server.properties</code> do MC e reinicie:
            </p>
            <div className="bg-black/60 rounded-lg p-3 font-mono text-[11px] mb-2 break-all">
              <div>resource-pack=<span className="text-emerald-300">{packUrl}</span></div>
              {sha1 && <div>resource-pack-sha1=<span className="text-cyan-300">{sha1}</span></div>}
              <div>require-resource-pack=<span className="text-amber-300">true</span></div>
            </div>
            <button className="btn-ghost btn-sm w-full mb-2"
              onClick={() => {
                const cfg = `resource-pack=${packUrl}\nresource-pack-sha1=${sha1}\nrequire-resource-pack=true\n`
                navigator.clipboard.writeText(cfg)
                toast.ok('Copiado pra clipboard')
              }}>📋 Copiar config</button>

            <div className="text-[10px] text-liberthia-300/50 leading-relaxed mt-3 space-y-1">
              <p><b>Importante:</b> a cada upload novo, o SHA1 muda. Reinicie o MC server ou rode <code>/reload</code> e peça pros players entrarem de novo pra pegar a versão nova do pack.</p>
              <p><b>Backend público?</b> Se o VPS expõe o backend, players de fora conseguem baixar. Se for rede privada (LAN/VPN), a URL precisa ser acessível pelos clients.</p>
            </div>
          </div>

          <div className="card">
            <h3 className="font-bold mb-2">📥 Pack atual</h3>
            <div className="text-xs space-y-1.5 text-liberthia-300/70">
              <div className="flex justify-between"><span>Sons:</span><span className="font-mono">{sounds.length}</span></div>
              <div className="flex justify-between"><span>Tamanho:</span><span className="font-mono">{((listQ.data?.packSize ?? 0) / 1024).toFixed(1)} KB</span></div>
              <div className="flex justify-between"><span>SHA1:</span><span className="font-mono text-[9px] truncate ml-2">{sha1 || '—'}</span></div>
            </div>
            <a className="btn w-full mt-3 inline-block text-center no-underline" href="/api/sounds-pack.zip" download>
              ⬇ Download pack
            </a>
          </div>

          <div className="card text-xs text-liberthia-300/70">
            <div className="font-bold text-liberthia-200 mb-2">💡 Uso</div>
            <p>• Em qualquer comando: <span className="chip">/playsound liberthia:meu_som master @a</span></p>
            <p>• Nas <b>Story Beats</b>: cole o playId no campo sound</p>
            <p>• No <b>Music Director</b>: idem</p>
            <p>• Em <b>Dialog</b>: idem</p>
          </div>
        </div>
      </div>
    </div>
  )
}
