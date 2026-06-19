import { useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

const CATEGORIES = [
  { key: 'art', label: '🎨 Arte', color: 'text-purple-300' },
  { key: 'meme', label: '😂 Meme', color: 'text-amber-300' },
  { key: 'event', label: '🎭 Evento', color: 'text-emerald-300' },
  { key: 'sketch', label: '✏ Sketch', color: 'text-cyan-300' },
]

export function PaintingsGalleryPage() {
  const qc = useQueryClient()
  const [filterCat, setFilterCat] = useState<string>('')
  const [filterFeatured, setFilterFeatured] = useState(false)
  const [showUpload, setShowUpload] = useState(false)
  const [preview, setPreview] = useState<any>(null)

  const listQ = useQuery({
    queryKey: ['paintings', filterCat, filterFeatured],
    queryFn: () => api.paintingsList({
      category: filterCat || undefined,
      featured: filterFeatured || undefined,
      size: 200,
    }),
  })

  const featureMut = useMutation({
    mutationFn: ({ id, on }: { id: number; on: boolean }) => api.paintingFeature(id, on),
    onSuccess: () => { toast.ok('⭐ updated'); qc.invalidateQueries({ queryKey: ['paintings'] }) },
  })
  const deleteMut = useMutation({
    mutationFn: api.paintingDelete,
    onSuccess: () => { toast.ok('🗑 deletado'); qc.invalidateQueries({ queryKey: ['paintings'] }); setPreview(null) },
  })

  const items = Array.isArray(listQ.data?.content) ? listQ.data!.content : []

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎨 Paintings Gallery</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Galeria curada das pinturas custom dos players (xercapaint, custom paintings, screenshots de
            builds). Upload manual — admin recebe arte e cataloga.
          </p>
        </div>
        <button className="btn" onClick={() => setShowUpload(true)}>+ Upload</button>
      </header>

      <div className="flex items-center gap-2 flex-wrap mb-3 text-xs">
        <button className={`btn-ghost btn-sm ${filterCat === '' ? 'ring-1 ring-purple-400' : ''}`}
          onClick={() => setFilterCat('')}>Todas categorias</button>
        {CATEGORIES.map(c => (
          <button key={c.key}
            className={`btn-ghost btn-sm ${filterCat === c.key ? 'ring-1 ring-purple-400' : ''}`}
            onClick={() => setFilterCat(c.key)}>{c.label}</button>
        ))}
        <button className={`btn-ghost btn-sm ${filterFeatured ? 'ring-1 ring-amber-400' : ''}`}
          onClick={() => setFilterFeatured(f => !f)}>⭐ Só featured</button>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">
        {items.map((p: any) => (
          <button key={p.id} onClick={() => setPreview(p)}
            className="card-glow text-left p-0 overflow-hidden hover:ring-2 hover:ring-purple-400/40 transition group">
            <div className="aspect-square bg-liberthia-950 flex items-center justify-center">
              <img src={api.paintingImageUrl(p.id)} alt={p.title}
                className="max-w-full max-h-full object-contain"
                onError={(ev) => { (ev.target as HTMLImageElement).style.display = 'none' }} />
            </div>
            <div className="p-2">
              <div className="flex items-center gap-1">
                {p.featured && <span>⭐</span>}
                <div className="font-bold text-xs truncate flex-1">{p.title}</div>
              </div>
              <div className="text-[10px] text-liberthia-300/50">por {p.authorName} · 👁 {p.viewCount}</div>
            </div>
          </button>
        ))}
        {!listQ.isLoading && items.length === 0 && (
          <div className="card col-span-full text-center py-12 text-liberthia-300/60">
            Sem pinturas ainda. Upload a primeira!
          </div>
        )}
      </div>

      {showUpload && <UploadDialog onClose={() => setShowUpload(false)} onUploaded={() => {
        qc.invalidateQueries({ queryKey: ['paintings'] }); setShowUpload(false)
      }} />}

      {preview && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80" onClick={() => setPreview(null)}>
          <div className="card max-w-3xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-2">
              <h3 className="font-bold text-lg">{preview.title}</h3>
              <div className="flex gap-1">
                <button className="btn-ghost btn-sm" onClick={() => featureMut.mutate({ id: preview.id, on: !preview.featured })}>
                  {preview.featured ? '★ Featured' : '☆ Feature'}
                </button>
                <button className="btn-danger btn-sm"
                  onClick={() => { if (confirm('Deletar?')) deleteMut.mutate(preview.id) }}>🗑</button>
              </div>
            </div>
            <img src={api.paintingImageUrl(preview.id)} className="w-full max-h-[60vh] object-contain rounded bg-liberthia-950" />
            <div className="text-xs mt-2 text-liberthia-300/70">
              <p><strong>Autor:</strong> {preview.authorName}</p>
              <p><strong>Categoria:</strong> {preview.category}</p>
              <p><strong>Tamanho:</strong> {preview.width}×{preview.height}px</p>
              {preview.description && <p className="italic mt-2">"{preview.description}"</p>}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function UploadDialog({ onClose, onUploaded }: { onClose: () => void; onUploaded: () => void }) {
  const fileRef = useRef<HTMLInputElement>(null)
  const [title, setTitle] = useState('')
  const [authorName, setAuthorName] = useState('')
  const [category, setCategory] = useState('art')
  const [description, setDescription] = useState('')

  const uploadMut = useMutation({
    mutationFn: (form: FormData) => api.paintingUpload(form),
    onSuccess: () => { toast.ok('🎨 upload feito'); onUploaded() },
    onError: (e: any) => toast.err(e.message),
  })

  function submit() {
    const f = fileRef.current?.files?.[0]
    if (!f || !title) return
    const fd = new FormData()
    fd.append('file', f)
    fd.append('title', title)
    fd.append('authorName', authorName)
    fd.append('category', category)
    fd.append('description', description)
    uploadMut.mutate(fd)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70" onClick={onClose}>
      <div className="card max-w-md w-full space-y-2" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold">📤 Upload pintura</h3>
        <input type="file" accept="image/*" ref={fileRef} className="input" />
        <input className="input" placeholder="Título" value={title} onChange={(e) => setTitle(e.target.value)} />
        <input className="input" placeholder="Autor (nome)" value={authorName} onChange={(e) => setAuthorName(e.target.value)} />
        <select className="input" value={category} onChange={(e) => setCategory(e.target.value)}>
          {CATEGORIES.map(c => <option key={c.key} value={c.key}>{c.label}</option>)}
        </select>
        <textarea className="input" rows={2} placeholder="Descrição (opcional)"
          value={description} onChange={(e) => setDescription(e.target.value)} />
        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
          <button className="btn" disabled={!title || uploadMut.isPending} onClick={submit}>Upload</button>
        </div>
      </div>
    </div>
  )
}
