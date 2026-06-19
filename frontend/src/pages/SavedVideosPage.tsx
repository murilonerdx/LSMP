import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, SavedVideoDto } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Admin page: listagem de vídeos salvos com editar título/descrição/público,
 * deletar, e link pra página pública /watch/{id}. Também tem upload manual.
 */

function fmtBytes(b: number): string {
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(0)}KB`
  if (b < 1024 * 1024 * 1024) return `${(b / (1024 * 1024)).toFixed(1)}MB`
  return `${(b / (1024 * 1024 * 1024)).toFixed(2)}GB`
}
function fmtDuration(ms: number): string {
  const s = Math.floor(ms / 1000)
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${String(sec).padStart(2, '0')}`
}

export function SavedVideosPage() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState<SavedVideoDto | null>(null)
  const [showUpload, setShowUpload] = useState(false)
  const videosQ = useQuery({
    queryKey: ['saved-videos'],
    queryFn: api.savedVideosList,
    refetchInterval: 30_000,
  })

  async function saveEdit() {
    if (!editing) return
    try {
      await api.savedVideoUpdate(editing.id, {
        title: editing.title,
        description: editing.description,
        isPublic: editing.isPublic,
      })
      toast.ok('💾 Salvo')
      setEditing(null)
      qc.invalidateQueries({ queryKey: ['saved-videos'] })
    } catch (e: any) { toast.err(e?.message ?? 'Falha') }
  }
  async function deleteVideo(v: SavedVideoDto) {
    if (!confirm(`Deletar "${v.title}"?\nEssa ação é IRREVERSÍVEL.`)) return
    try {
      await api.savedVideoDelete(v.id)
      toast.ok('🗑 Deletado')
      qc.invalidateQueries({ queryKey: ['saved-videos'] })
    } catch (e: any) { toast.err(e?.message ?? 'Falha') }
  }
  function publicUrl(v: SavedVideoDto): string {
    return `${window.location.origin}/watch/${v.id}`
  }
  async function copyLink(v: SavedVideoDto) {
    await navigator.clipboard.writeText(publicUrl(v))
    toast.ok('🔗 Link copiado pra clipboard')
  }

  const videos = videosQ.data?.videos ?? []

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎬 Vídeos Salvos</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Vídeos persistidos no servidor. Os marcados como <b>público</b> ficam
            acessíveis em <code className="text-amber-300">/watch/&#123;id&#125;</code>{' '}
            sem precisar de login.
          </p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowUpload(true)}>
          📤 Upload MP4
        </button>
      </header>

      {videosQ.isLoading && <div className="card text-center py-8">Carregando…</div>}

      {videos.length === 0 && !videosQ.isLoading && (
        <div className="card text-center py-12 text-liberthia-300/60">
          <div className="text-4xl mb-2 opacity-50">🎬</div>
          <p>Sem vídeos ainda. Salve um do Video Editor ou faça upload.</p>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {videos.map(v => (
          <div key={v.id} className="card-glow space-y-2">
            <video src={api.savedVideoStreamUrl(v.id)}
              controls preload="none"
              className="w-full rounded bg-black aspect-video" />
            <div className="flex items-start justify-between gap-2">
              <div className="flex-1 min-w-0">
                <div className="font-bold truncate">{v.title}</div>
                <div className="text-[10px] text-liberthia-300/50 flex gap-2 flex-wrap mt-0.5">
                  <span>{fmtDuration(v.durationMs)}</span>
                  <span>·</span>
                  <span>{fmtBytes(v.sizeBytes)}</span>
                  <span>·</span>
                  <span>{v.viewCount} views</span>
                  <span>·</span>
                  <span className={v.source === 'rendered' ? 'text-purple-300' : 'text-emerald-300'}>
                    {v.source === 'rendered' ? '🎬 editor' : '📤 upload'}
                  </span>
                </div>
              </div>
              <span className={`badge ${v.isPublic ? 'badge-green' : 'badge-red'}`}>
                {v.isPublic ? '🌐 público' : '🔒 privado'}
              </span>
            </div>
            {v.description && (
              <div className="text-[11px] text-liberthia-300/70 line-clamp-3">{v.description}</div>
            )}
            <div className="flex gap-1 flex-wrap">
              <button className="btn-ghost btn-sm flex-1" onClick={() => setEditing(v)}>
                ✏ Editar
              </button>
              {v.isPublic && (
                <button className="btn-ghost btn-sm flex-1" onClick={() => copyLink(v)}>
                  🔗 Copiar link
                </button>
              )}
              <a className="btn-ghost btn-sm flex-1 text-center"
                href={`/watch/${v.id}`} target="_blank" rel="noreferrer">
                👁 Ver
              </a>
              <button className="btn-ghost btn-sm text-red-300" onClick={() => deleteVideo(v)}>
                🗑
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* ============ Edit modal ============ */}
      {editing && (
        <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4"
          onClick={() => setEditing(null)}>
          <div className="card-glow max-w-2xl w-full" onClick={e => e.stopPropagation()}>
            <h3 className="font-bold mb-3">✏ Editar vídeo</h3>
            <label className="block mb-2">
              <span className="text-xs text-liberthia-300/70">Título</span>
              <input className="input w-full" value={editing.title}
                onChange={e => setEditing({ ...editing, title: e.target.value })} />
            </label>
            <label className="block mb-2">
              <span className="text-xs text-liberthia-300/70">Descrição</span>
              <textarea className="input w-full min-h-32" value={editing.description}
                onChange={e => setEditing({ ...editing, description: e.target.value })} />
            </label>
            <label className="flex items-center gap-2 mb-3 text-xs">
              <input type="checkbox" checked={editing.isPublic}
                onChange={e => setEditing({ ...editing, isPublic: e.target.checked })} />
              <span>Público (qualquer um com o link pode ver, sem login)</span>
            </label>
            <div className="flex gap-2">
              <button className="btn btn-primary flex-1" onClick={saveEdit}>💾 Salvar</button>
              <button className="btn-ghost" onClick={() => setEditing(null)}>Cancelar</button>
            </div>
          </div>
        </div>
      )}

      {/* ============ Upload modal ============ */}
      {showUpload && (
        <UploadModal onClose={() => setShowUpload(false)}
          onUploaded={() => qc.invalidateQueries({ queryKey: ['saved-videos'] })} />
      )}
    </div>
  )
}

function UploadModal({ onClose, onUploaded }: { onClose: () => void; onUploaded: () => void }) {
  const [file, setFile] = useState<File | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [isPublic, setIsPublic] = useState(true)
  const [uploading, setUploading] = useState(false)

  async function doUpload() {
    if (!file) { toast.err('Escolha um arquivo'); return }
    if (!title.trim()) { toast.err('Título obrigatório'); return }
    setUploading(true)
    try {
      await api.savedVideoUpload(file, title.trim(), description, isPublic)
      toast.ok('📤 Upload concluído')
      onUploaded()
      onClose()
    } catch (e: any) { toast.err(e?.message ?? 'Falha') }
    setUploading(false)
  }

  return (
    <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4"
      onClick={onClose}>
      <div className="card-glow max-w-2xl w-full" onClick={e => e.stopPropagation()}>
        <h3 className="font-bold mb-3">📤 Upload de MP4</h3>
        <input type="file" accept="video/mp4,video/webm,video/quicktime"
          onChange={(e) => { const f = e.target.files?.[0] ?? null; setFile(f); if (f && !title) setTitle(f.name.replace(/\.[^.]+$/, '')) }}
          className="mb-3" />
        {file && <div className="text-[10px] text-liberthia-300/50 mb-2">
          {file.name} · {(file.size / (1024 * 1024)).toFixed(1)}MB
        </div>}
        <label className="block mb-2">
          <span className="text-xs text-liberthia-300/70">Título</span>
          <input className="input w-full" value={title}
            onChange={e => setTitle(e.target.value)} />
        </label>
        <label className="block mb-2">
          <span className="text-xs text-liberthia-300/70">Descrição</span>
          <textarea className="input w-full min-h-24" value={description}
            onChange={e => setDescription(e.target.value)} />
        </label>
        <label className="flex items-center gap-2 mb-3 text-xs">
          <input type="checkbox" checked={isPublic}
            onChange={e => setIsPublic(e.target.checked)} />
          <span>Público</span>
        </label>
        <div className="flex gap-2">
          <button className="btn btn-primary flex-1" onClick={doUpload} disabled={uploading || !file}>
            {uploading ? '⏳ Enviando…' : '📤 Upload'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}
