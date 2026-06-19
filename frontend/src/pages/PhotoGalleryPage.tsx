import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

const auth = () => ({ Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}` })

type Photo = {
  id: number; authorName: string; authorUuid: string;
  title: string; description: string; cursed: boolean;
  width: number; height: number; takenAt: string; sizeBytes: number;
}

/** Galeria de Fotos com Maldição — upload PNG, admin marca foto como "cursed". */
export function PhotoGalleryPage() {
  const [filterCursed, setFilterCursed] = useState(false)
  const [selected, setSelected] = useState<Photo | null>(null)
  const qc = useQueryClient()

  const q = useQuery({
    queryKey: ['photos', filterCursed],
    queryFn: async () => {
      const r = await fetch(`/api/photos${filterCursed ? '?cursed=true' : ''}`, { headers: auth() })
      return r.json()
    },
  })

  const upload = useMutation({
    mutationFn: async (form: FormData) => {
      const r = await fetch('/api/photos', { method: 'POST', headers: auth(), body: form })
      if (!r.ok) throw new Error(`${r.status}`)
      return r.json()
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['photos'] }),
  })

  const curse = useMutation({
    mutationFn: async ({ id, on }: { id: number; on: boolean }) => {
      const r = await fetch(`/api/photos/${id}/curse?on=${on}`, { method: 'POST', headers: auth() })
      return r.json()
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['photos'] }),
  })

  const remove = useMutation({
    mutationFn: async (id: number) => {
      await fetch(`/api/photos/${id}`, { method: 'DELETE', headers: auth() })
    },
    onSuccess: () => { setSelected(null); qc.invalidateQueries({ queryKey: ['photos'] }) },
  })

  const items: Photo[] = Array.isArray(q.data?.content) ? q.data.content : []

  function handleFile(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0]
    if (!f) return
    const fd = new FormData()
    fd.append('file', f)
    fd.append('title', f.name.replace(/\.[^.]+$/, ''))
    fd.append('authorName', 'admin')
    upload.mutate(fd)
  }

  return (
    <div className="p-6 space-y-6 text-white">
      <div className="flex justify-between">
        <h1 className="text-3xl font-bold">📸 Galeria de Fotos</h1>
        <div className="flex gap-2">
          <label className={`px-3 py-1 rounded cursor-pointer ${!filterCursed ? 'bg-purple-700' : 'bg-zinc-800'}`} onClick={() => setFilterCursed(false)}>Todas</label>
          <label className={`px-3 py-1 rounded cursor-pointer ${filterCursed ? 'bg-purple-700' : 'bg-zinc-800'}`} onClick={() => setFilterCursed(true)}>🩸 Cursed</label>
          <label className="px-3 py-1 rounded bg-emerald-700 hover:bg-emerald-600 cursor-pointer">
            + Upload
            <input type="file" accept="image/png,image/jpeg" className="hidden" onChange={handleFile} />
          </label>
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
        {items.map((p) => (
          <div key={p.id} onClick={() => setSelected(p)}
               className={`bg-zinc-900 border rounded overflow-hidden cursor-pointer hover:scale-[1.02] transition ${p.cursed ? 'border-red-700' : 'border-zinc-800'}`}>
            <img src={`/api/photos/${p.id}/image`} alt={p.title}
                 className={`w-full h-48 object-cover ${p.cursed ? 'sepia hue-rotate-15' : ''}`} />
            <div className="p-2">
              <div className="text-sm font-semibold truncate">{p.title || '(sem título)'}</div>
              <div className="text-xs text-zinc-500">{p.authorName} · {new Date(p.takenAt).toLocaleDateString()}</div>
              {p.cursed && <div className="text-xs text-red-400 mt-1">🩸 CURSED</div>}
            </div>
          </div>
        ))}
        {items.length === 0 && (
          <div className="col-span-full text-center text-zinc-500 py-12">Sem fotos ainda. Faça upload de uma .png/.jpg.</div>
        )}
      </div>

      {selected && (
        <div onClick={() => setSelected(null)} className="fixed inset-0 bg-black/80 flex items-center justify-center z-50 p-6">
          <div onClick={(e) => e.stopPropagation()} className="bg-zinc-900 rounded max-w-4xl w-full p-4 space-y-3">
            <img src={`/api/photos/${selected.id}/image`} className={`w-full max-h-[70vh] object-contain ${selected.cursed ? 'sepia' : ''}`} />
            <div className="flex justify-between items-center">
              <div>
                <h3 className="font-bold text-lg">{selected.title}</h3>
                <p className="text-sm text-zinc-400">{selected.authorName} · {selected.width}×{selected.height} · {(selected.sizeBytes/1024).toFixed(0)}KB</p>
              </div>
              <div className="flex gap-2">
                <button onClick={() => curse.mutate({ id: selected.id, on: !selected.cursed })}
                  className={`px-3 py-1 rounded ${selected.cursed ? 'bg-zinc-700' : 'bg-red-800 hover:bg-red-700'}`}>
                  {selected.cursed ? 'Descursar' : '🩸 Marcar Cursed'}
                </button>
                <button onClick={() => remove.mutate(selected.id)} className="px-3 py-1 rounded bg-red-900 hover:bg-red-700">🗑 Deletar</button>
                <button onClick={() => setSelected(null)} className="px-3 py-1 rounded bg-zinc-700">Fechar</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
