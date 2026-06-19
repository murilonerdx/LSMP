import { useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'

/**
 * Página PÚBLICA de exibição de vídeo — não precisa login.
 * URL: /watch/:id
 *
 * Layout standalone (sem sidebar admin). Mostra:
 *  - Vídeo player (full width)
 *  - Título grande + descrição
 *  - Stats (views, duração, data de criação)
 *  - Link pra galeria de outros vídeos públicos
 */

function fmtDuration(ms: number): string {
  const s = Math.floor(ms / 1000)
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${String(sec).padStart(2, '0')}`
}

export function WatchVideoPage() {
  const { id } = useParams<{ id: string }>()
  const videoId = Number(id)

  const videoQ = useQuery({
    enabled: !!videoId,
    queryKey: ['public-video', videoId],
    queryFn: () => api.publicVideoGet(videoId),
    retry: 0,
  })

  // Update document title
  useEffect(() => {
    if (videoQ.data) {
      document.title = `🎬 ${videoQ.data.title} — Liberthia`
    }
    return () => { document.title = 'Liberthia' }
  }, [videoQ.data])

  return (
    <div className="min-h-screen bg-gradient-to-b from-liberthia-950 to-black text-liberthia-100">
      {/* Header */}
      <header className="border-b border-liberthia-700/40 bg-liberthia-900/60 backdrop-blur sticky top-0 z-30">
        <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
          <Link to="/watch" className="flex items-center gap-2 hover:opacity-80">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-liberthia-400 to-liberthia-600 flex items-center justify-center text-xl shadow-lg ring-glow">⚛</div>
            <div>
              <div className="font-bold text-lg gradient-text">Liberthia</div>
              <div className="text-[10px] text-liberthia-300/60 uppercase tracking-widest">Galeria de Vídeos</div>
            </div>
          </Link>
          <Link to="/watch" className="btn-ghost btn-sm">
            📺 Outros vídeos
          </Link>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-6">
        {videoQ.isLoading && (
          <div className="card text-center py-20 text-liberthia-300/60">
            <div className="text-6xl mb-3 opacity-50">⏳</div>
            <p>Carregando vídeo…</p>
          </div>
        )}

        {videoQ.error && (
          <div className="card text-center py-20">
            <div className="text-6xl mb-3 opacity-50">🚫</div>
            <h2 className="text-xl font-bold mb-2">Vídeo não encontrado</h2>
            <p className="text-liberthia-300/60 text-sm">
              Esse vídeo pode ter sido removido, privado, ou o link está errado.
            </p>
            <Link to="/watch" className="btn btn-primary mt-4 inline-block">
              📺 Ver galeria
            </Link>
          </div>
        )}

        {videoQ.data && (
          <div className="space-y-4">
            {/* Player */}
            <div className="rounded-xl overflow-hidden shadow-2xl shadow-purple-500/20 ring-1 ring-purple-500/30">
              <video src={api.savedVideoStreamUrl(videoQ.data.id)}
                controls autoPlay
                className="w-full aspect-video bg-black" />
            </div>

            {/* Title + meta */}
            <div className="card-glow">
              <h1 className="text-2xl md:text-3xl font-bold gradient-text mb-2">
                {videoQ.data.title}
              </h1>
              <div className="flex items-center gap-3 text-xs text-liberthia-300/60 flex-wrap mb-3">
                <span>⏱ {fmtDuration(videoQ.data.durationMs)}</span>
                <span>·</span>
                <span>👁 {videoQ.data.viewCount} views</span>
                <span>·</span>
                <span>📅 {new Date(videoQ.data.createdAt).toLocaleDateString('pt-BR', { dateStyle: 'long' })}</span>
                {videoQ.data.source === 'rendered' && (
                  <>
                    <span>·</span>
                    <span className="text-purple-300">🎬 montado no editor</span>
                  </>
                )}
              </div>
              {videoQ.data.description ? (
                <div className="text-sm text-liberthia-200/90 whitespace-pre-wrap leading-relaxed border-l-2 border-purple-500/40 pl-4">
                  {videoQ.data.description}
                </div>
              ) : (
                <div className="text-sm text-liberthia-300/40 italic">Sem descrição.</div>
              )}
            </div>
          </div>
        )}
      </main>

      <footer className="text-center py-6 text-[10px] text-liberthia-300/40 border-t border-liberthia-700/30 mt-12">
        Liberthia Server · <Link to="/watch" className="hover:text-liberthia-300/70">galeria pública</Link>
      </footer>
    </div>
  )
}

/** Listagem pública — /watch (sem id). */
export function WatchGalleryPage() {
  const listQ = useQuery({
    queryKey: ['public-videos-list'],
    queryFn: api.publicVideosList,
  })

  useEffect(() => {
    document.title = '🎬 Galeria — Liberthia'
    return () => { document.title = 'Liberthia' }
  }, [])

  return (
    <div className="min-h-screen bg-gradient-to-b from-liberthia-950 to-black text-liberthia-100">
      <header className="border-b border-liberthia-700/40 bg-liberthia-900/60 backdrop-blur">
        <div className="max-w-6xl mx-auto px-4 py-3 flex items-center gap-2">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-liberthia-400 to-liberthia-600 flex items-center justify-center text-xl shadow-lg ring-glow">⚛</div>
          <div>
            <div className="font-bold text-lg gradient-text">Liberthia</div>
            <div className="text-[10px] text-liberthia-300/60 uppercase tracking-widest">Galeria de Vídeos</div>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-6">
        <h1 className="text-3xl font-bold gradient-text mb-1">📺 Vídeos do Servidor</h1>
        <p className="text-sm text-liberthia-300/60 mb-6">
          Crônicas, momentos memoráveis e bastidores do mundo de Liberthia.
        </p>

        {listQ.isLoading && <div className="text-center py-12">Carregando…</div>}

        {listQ.data?.videos?.length === 0 && (
          <div className="card text-center py-20 text-liberthia-300/60">
            <div className="text-6xl mb-3 opacity-50">📺</div>
            <p>Ainda não tem vídeos públicos.</p>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {(listQ.data?.videos ?? []).map(v => (
            <Link key={v.id} to={`/watch/${v.id}`}
              className="card-glow hover:scale-[1.02] hover:ring-2 hover:ring-purple-400/40 transition cursor-pointer">
              <video src={api.savedVideoStreamUrl(v.id)}
                preload="metadata"
                className="w-full aspect-video rounded bg-black mb-2" />
              <div className="font-bold truncate">{v.title}</div>
              <div className="text-[10px] text-liberthia-300/60 flex gap-2 mt-0.5">
                <span>{fmtDuration(v.durationMs)}</span>
                <span>·</span>
                <span>{v.viewCount} views</span>
                <span>·</span>
                <span>{new Date(v.createdAt).toLocaleDateString('pt-BR')}</span>
              </div>
              {v.description && (
                <div className="text-[11px] text-liberthia-300/70 line-clamp-2 mt-1">
                  {v.description}
                </div>
              )}
            </Link>
          ))}
        </div>
      </main>

      <footer className="text-center py-6 text-[10px] text-liberthia-300/40 border-t border-liberthia-700/30 mt-12">
        Liberthia Server · Galeria pública
      </footer>
    </div>
  )
}
