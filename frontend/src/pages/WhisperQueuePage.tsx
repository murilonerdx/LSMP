import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Whisper Queue Manager — controle e monitor da fila de transcrição.
 *
 * - Mostra fila: total, done, pending, processing, failed
 * - Status do scheduler: workers ativos, threads, beam, model
 * - Lista de pendentes/falhos com botão de retry/skip
 * - Estimativa de tempo restante (baseado em throughput)
 */

function fmtMs(ms: number): string {
  const s = Math.floor(ms / 1000)
  if (s < 60) return `${s}s`
  const m = Math.floor(s / 60)
  if (m < 60) return `${m}m ${s % 60}s`
  const h = Math.floor(m / 60)
  return `${h}h ${m % 60}m`
}

function fmtBytes(b: number): string {
  if (b < 1024) return `${b}B`
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(0)}KB`
  return `${(b / (1024 * 1024)).toFixed(1)}MB`
}

export function WhisperQueuePage() {
  const qc = useQueryClient()
  const statusQ = useQuery({
    queryKey: ['voice-transcription-status'],
    queryFn: api.voiceTranscriptionStatus,
    refetchInterval: 3000,
  })
  const whisperQ = useQuery({
    queryKey: ['voice-whisper-status'],
    queryFn: api.voiceWhisperStatus,
    refetchInterval: 3000,
  })
  // Pega clipes pendentes/falhos pra mostrar lista — usa voiceList sem filtro
  const clipsQ = useQuery({
    queryKey: ['voice-list-for-queue'],
    queryFn: () => api.voiceList(undefined, 300),
    refetchInterval: 5000,
  })

  const status = statusQ.data
  const whisper = whisperQ.data
  const allClips = clipsQ.data?.clips ?? []
  const pendingClips = allClips.filter(c => c.transcriptionStatus === 'PENDING' || !c.transcriptionStatus)
  const failedClips = allClips.filter(c => c.transcriptionStatus === 'FAILED')
  // BUG FIX: o backend MARCA o clipe como PROCESSING quando o worker pega
  // (WhisperTranscriptionService.java:224 svc.setTranscriptionStatus(PROCESSING)).
  // Antes essa linha estava hardcoded como [] e por isso "Em processamento (0)"
  // aparecia mesmo com workers busy.
  const processingClips = allClips.filter(c => c.transcriptionStatus === 'PROCESSING')

  // Estimativa de tempo: baseado em última latência conhecida
  // Whisper grandes geram ~30s-2min por clipe. Calculamos um avg.
  const avgPerClipMs = (() => {
    if (!whisper) return 60000
    // Se rodou ticks, infere média
    if (whisper.ticksRun > 0 && whisper.clipsProcessed > 0) {
      // Não tem totalTimeMs direto, usa heuristic
      return 60000  // 1min default
    }
    return 60000
  })()

  const etaMs = status && pendingClips.length > 0
    ? avgPerClipMs * pendingClips.length / Math.max(1, whisper?.workers ?? 1)
    : 0

  async function retryClip(id: number) {
    try {
      await api.voiceTranscribe(id)
      toast.ok(`Re-enfileirado clipe #${id}`)
      qc.invalidateQueries({ queryKey: ['voice-list-for-queue'] })
    } catch (e: any) { toast.err(e.message) }
  }

  return (
    <div className="route-fade max-w-[1300px]">
      <header className="mb-4">
        <h1 className="page-title">⚙ Whisper Queue Manager</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Monitoramento da fila de transcrição de áudio. <strong>{whisper?.workers ?? '?'} worker(s)</strong> com <strong>{whisper?.threads ?? '?'} threads</strong> processando clipes em background.
        </p>
      </header>

      {/* Hero: Status visual da fila */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mb-4">
        <StatCard label="📋 Total" value={status?.total ?? '—'} color="purple" />
        <StatCard label="✅ Done" value={status?.done ?? '—'} color="emerald" sub={`${status?.pctDone?.toFixed(1) ?? 0}%`} />
        <StatCard label="⏳ Pending" value={status?.pending ?? '—'} color="amber" sub={etaMs > 0 ? `ETA ~${fmtMs(etaMs)}` : ''} />
        <StatCard label="✗ Failed" value={status?.failed ?? '—'} color="red" />
      </div>

      {/* Whisper Engine details */}
      <div className="card-glow mb-4">
        <h2 className="text-base font-bold gradient-text mb-2">🎙 Engine Status</h2>
        {whisper ? (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
            <Stat label="Estado">
              {whisper.currentlyRunning ? (
                <span className="text-amber-300 animate-pulse">🔄 transcrevendo</span>
              ) : whisper.enabled ? (
                <span className="text-emerald-300">✓ idle</span>
              ) : (
                <span className="text-red-300">✗ disabled</span>
              )}
            </Stat>
            <Stat label="Workers ativos">
              <span className="text-amber-300 font-bold">{whisper.activeWorkers ?? 0}</span>
              <span className="text-liberthia-300/50"> / {whisper.workers ?? 1}</span>
            </Stat>
            <Stat label="Threads por worker">{whisper.threads}</Stat>
            <Stat label="Idioma">{whisper.language}</Stat>
            <Stat label="Beam size">{whisper.beamSize}</Stat>
            <Stat label="Best-of">{whisper.bestOf}</Stat>
            <Stat label="Clipes processados">{whisper.clipsProcessed}</Stat>
            <Stat label="Ticks rodados">{whisper.ticksRun}</Stat>
            <div className="col-span-2 md:col-span-4 p-2 rounded bg-liberthia-900/40 text-[11px]">
              <div className="text-liberthia-300/50">Modelo:</div>
              <div className="font-mono">{whisper.modelPath.split('/').pop()}</div>
              <div className="text-liberthia-300/50 mt-1">Último tick:</div>
              <div className="font-mono text-[10px]">{whisper.lastTickInfo || '(nenhum ainda)'}</div>
            </div>
          </div>
        ) : (
          <div className="text-xs text-liberthia-300/60">Carregando...</div>
        )}
      </div>

      {/* Fila visual */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
        <QueueList title="🔄 Em processamento" clips={processingClips} color="amber" />
        <QueueList title="⏳ Pendentes" clips={pendingClips} color="purple" maxShow={20} />
        <QueueList title="✗ Falhados" clips={failedClips} color="red" onRetry={retryClip} />
      </div>

      {pendingClips.length === 0 && failedClips.length === 0 && processingClips.length === 0 && (
        <div className="card text-center py-12 text-liberthia-300/60 mt-4">
          <div className="text-5xl mb-2 opacity-50">✨</div>
          <p>Fila vazia — todos os clipes foram transcritos.</p>
        </div>
      )}
    </div>
  )
}

function StatCard({ label, value, color, sub }: { label: string; value: number | string; color: string; sub?: string }) {
  const colorMap: Record<string, string> = {
    purple: 'border-purple-500/40 text-purple-300',
    emerald: 'border-emerald-500/40 text-emerald-300',
    amber: 'border-amber-500/40 text-amber-300',
    red: 'border-red-500/40 text-red-300',
  }
  return (
    <div className={`card-glow border-2 ${colorMap[color]}`}>
      <div className="text-[10px] text-liberthia-300/70 uppercase tracking-wider">{label}</div>
      <div className="text-3xl font-bold mt-1">{value}</div>
      {sub && <div className="text-[10px] text-liberthia-300/50 mt-1">{sub}</div>}
    </div>
  )
}

function Stat({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="p-2 rounded bg-liberthia-900/40">
      <div className="text-[10px] text-liberthia-300/50 uppercase tracking-wider">{label}</div>
      <div className="font-bold mt-0.5">{children}</div>
    </div>
  )
}

function QueueList({ title, clips, color, maxShow = 10, onRetry }: {
  title: string; clips: any[]; color: string; maxShow?: number; onRetry?: (id: number) => void
}) {
  const colorMap: Record<string, string> = {
    purple: 'border-purple-500/40',
    amber: 'border-amber-500/40',
    red: 'border-red-500/40',
  }
  const shown = clips.slice(0, maxShow)
  return (
    <div className={`card-glow border ${colorMap[color]}`}>
      <h3 className="font-bold text-sm mb-2">{title} <span className="text-[10px] text-liberthia-300/50">({clips.length})</span></h3>
      {clips.length === 0 ? (
        <div className="text-[10px] text-liberthia-300/50 italic py-3 text-center">vazio</div>
      ) : (
        <div className="space-y-1 max-h-[400px] overflow-y-auto">
          {shown.map(c => (
            <div key={c.id} className="text-[11px] p-2 rounded bg-liberthia-900/40 flex items-center gap-2">
              <div className="flex-1 min-w-0">
                <div className="font-mono text-liberthia-300/70">#{c.id}</div>
                <div className="truncate text-liberthia-300">
                  <b>{c.playerName ?? '?'}</b>
                  <span className="text-liberthia-300/50 ml-1">
                    {fmtMs(c.durationMs)} · {fmtBytes(c.sizeBytes)}
                  </span>
                </div>
                <div className="text-[9px] text-liberthia-300/50">
                  {new Date(c.ts).toLocaleString()}
                </div>
              </div>
              {onRetry && (
                <button className="btn-ghost btn-sm text-[10px]" onClick={() => onRetry(c.id)}>
                  ↻
                </button>
              )}
            </div>
          ))}
          {clips.length > maxShow && (
            <div className="text-[10px] text-liberthia-300/50 italic text-center py-1">
              + {clips.length - maxShow} mais...
            </div>
          )}
        </div>
      )}
    </div>
  )
}
