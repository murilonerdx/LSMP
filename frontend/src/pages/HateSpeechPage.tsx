import { useMemo, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, hateSpeechApi, HateSpeechAlert } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Painel de moderação de hate speech (transfobia / racismo / xenofobia / etc).
 *
 * Mostra alertas gerados automaticamente pelo HateSpeechDetectorService quando
 * o Whisper transcreve um clipe e detecta discurso de ódio (pipeline regex +
 * Ollama qwen2.5:1.5b).
 *
 * Layout:
 * - Sidebar esquerda: filtros (severity, status review, player) + estatísticas
 * - Área central: lista de alertas com player de áudio inline + razão LLM
 * - Cada alerta: botões de ação (Ignorar / Avisar / Mutar / Kickar / Banir)
 * - Tab debug: testar regex sem precisar transcrição (calibragem)
 */

const CATEGORY_LABELS: Record<string, string> = {
  transfobia: '🏳️‍⚧️ Transfobia',
  racismo: '✊🏿 Racismo',
  xenofobia: '🌎 Xenofobia',
  homofobia: '🏳️‍🌈 Homofobia',
  misoginia: '♀ Misoginia',
  capacitismo: '♿ Capacitismo',
  outro: '⚠ Outro',
}

const SEVERITY_COLORS: Record<string, string> = {
  LOW: 'bg-yellow-900/40 text-yellow-200 border-yellow-700',
  MEDIUM: 'bg-orange-900/40 text-orange-200 border-orange-700',
  HIGH: 'bg-red-900/60 text-red-100 border-red-600',
}

const ACTION_BTNS: Array<{ key: 'IGNORE' | 'WARN' | 'MUTE' | 'KICK' | 'BAN'; label: string; cls: string; icon: string }> = [
  { key: 'IGNORE', label: 'Ignorar', cls: 'bg-zinc-700 hover:bg-zinc-600', icon: '🙈' },
  { key: 'WARN',   label: 'Avisar', cls: 'bg-yellow-700 hover:bg-yellow-600', icon: '⚠' },
  { key: 'MUTE',   label: 'Mutar 1h', cls: 'bg-orange-700 hover:bg-orange-600', icon: '🔇' },
  { key: 'KICK',   label: 'Kickar', cls: 'bg-red-700 hover:bg-red-600', icon: '👢' },
  { key: 'BAN',    label: 'Banir', cls: 'bg-red-900 hover:bg-red-800', icon: '🔨' },
]

type Tab = 'alerts' | 'stats' | 'debug'

export function HateSpeechPage() {
  const qc = useQueryClient()
  const [tab, setTab] = useState<Tab>('alerts')
  const [filterReviewed, setFilterReviewed] = useState<'all' | 'pending' | 'reviewed'>('pending')
  const [filterSeverity, setFilterSeverity] = useState<string>('')
  const [filterPlayer, setFilterPlayer] = useState<string>('')

  const alertsQuery = useQuery({
    queryKey: ['hate-speech-alerts', filterReviewed, filterSeverity, filterPlayer],
    queryFn: () => hateSpeechApi.list({
      reviewed: filterReviewed === 'all' ? undefined : filterReviewed === 'reviewed',
      severity: filterSeverity || undefined,
      playerUuid: filterPlayer || undefined,
      limit: 200,
    }),
    refetchInterval: 15000,
  })

  const statsQuery = useQuery({
    queryKey: ['hate-speech-stats'],
    queryFn: hateSpeechApi.stats,
    refetchInterval: 30000,
  })

  const alerts = alertsQuery.data ?? []
  const stats = statsQuery.data

  return (
    <div className="p-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold flex items-center gap-2">
            <span className="text-red-400">🛡</span> Moderação de Discurso de Ódio
          </h1>
          <p className="text-zinc-400 text-sm mt-1">
            Detector automático em transcrições de voz · pipeline regex + Ollama
          </p>
        </div>
        {stats && (
          <div className="flex gap-2">
            <Badge label="Total" value={stats.total} color="bg-zinc-700" />
            <Badge
              label="Pendentes"
              value={stats.pendingReview}
              color={stats.pendingReview > 0 ? 'bg-red-700 animate-pulse' : 'bg-zinc-700'}
            />
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-4 border-b border-zinc-800">
        {([
          { k: 'alerts', label: '📋 Alertas', count: stats?.pendingReview },
          { k: 'stats',  label: '📊 Estatísticas', count: undefined },
          { k: 'debug',  label: '🔧 Debug / Calibrar', count: undefined },
        ] as Array<{ k: Tab; label: string; count: number | undefined }>).map(t => (
          <button
            key={t.k}
            onClick={() => setTab(t.k as Tab)}
            className={`px-4 py-2 -mb-px border-b-2 transition-colors ${
              tab === t.k
                ? 'border-red-500 text-red-300'
                : 'border-transparent text-zinc-400 hover:text-zinc-200'
            }`}
          >
            {t.label}
            {t.count !== undefined && t.count > 0 && (
              <span className="ml-2 bg-red-600 text-white text-xs px-2 py-0.5 rounded-full">
                {t.count}
              </span>
            )}
          </button>
        ))}
      </div>

      {tab === 'alerts' && (
        <div className="grid grid-cols-12 gap-6">
          {/* Sidebar: filtros */}
          <aside className="col-span-3 space-y-4">
            <Section title="Filtros">
              <FilterGroup label="Status">
                {(['pending', 'reviewed', 'all'] as const).map(v => (
                  <FilterPill
                    key={v}
                    active={filterReviewed === v}
                    onClick={() => setFilterReviewed(v)}
                  >
                    {v === 'pending' ? '⏳ Pendentes' : v === 'reviewed' ? '✓ Reviewed' : 'Todos'}
                  </FilterPill>
                ))}
              </FilterGroup>

              <FilterGroup label="Severity">
                {['', 'HIGH', 'MEDIUM', 'LOW'].map(v => (
                  <FilterPill
                    key={v}
                    active={filterSeverity === v}
                    onClick={() => setFilterSeverity(v)}
                  >
                    {v === '' ? 'Todos' : v}
                  </FilterPill>
                ))}
              </FilterGroup>
            </Section>

            {stats && stats.topPlayers && stats.topPlayers.length > 0 && (
              <Section title="🚨 Top Reportados">
                <div className="space-y-1">
                  {stats.topPlayers.slice(0, 8).map(p => (
                    <button
                      key={p.uuid}
                      onClick={() => setFilterPlayer(filterPlayer === p.uuid ? '' : p.uuid)}
                      className={`w-full flex justify-between items-center px-3 py-2 rounded text-sm transition-colors ${
                        filterPlayer === p.uuid
                          ? 'bg-red-900/40 border border-red-700'
                          : 'bg-zinc-800/50 hover:bg-zinc-800'
                      }`}
                    >
                      <span className="font-mono">{p.name}</span>
                      <span className="text-red-300 font-bold">{p.count}</span>
                    </button>
                  ))}
                </div>
                {filterPlayer && (
                  <button
                    onClick={() => setFilterPlayer('')}
                    className="mt-2 text-xs text-zinc-400 hover:text-zinc-200"
                  >
                    × limpar filtro de player
                  </button>
                )}
              </Section>
            )}
          </aside>

          {/* Lista de alertas */}
          <main className="col-span-9 space-y-3">
            {alertsQuery.isLoading && <div className="text-zinc-400">carregando...</div>}
            {!alertsQuery.isLoading && alerts.length === 0 && (
              <div className="bg-zinc-900/50 border border-zinc-800 rounded-lg p-8 text-center text-zinc-400">
                <div className="text-6xl mb-3">🕊</div>
                <div className="font-bold text-zinc-200 mb-1">Nenhum alerta no filtro atual</div>
                <div className="text-sm">
                  {filterReviewed === 'pending'
                    ? 'Não há alertas pendentes de review. Bom sinal!'
                    : 'Tente mudar os filtros.'}
                </div>
              </div>
            )}
            {alerts.map(a => (
              <AlertCard
                key={a.id}
                alert={a}
                onAction={(action, reason) => {
                  hateSpeechApi.review(a.id, action, reason).then(() => {
                    toast.ok(`${action} aplicado em ${a.playerName}`)
                    qc.invalidateQueries({ queryKey: ['hate-speech-alerts'] })
                    qc.invalidateQueries({ queryKey: ['hate-speech-stats'] })
                  }).catch(e => toast.err('Erro: ' + e.message))
                }}
              />
            ))}
          </main>
        </div>
      )}

      {tab === 'stats' && <StatsView stats={stats} />}
      {tab === 'debug' && <DebugView />}
    </div>
  )
}

// ─── Components ──────────────────────────────────────────────────────────

function Badge({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div className={`px-4 py-2 rounded-lg ${color} text-white`}>
      <div className="text-xs opacity-70">{label}</div>
      <div className="text-2xl font-bold">{value}</div>
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="bg-zinc-900/50 border border-zinc-800 rounded-lg p-4">
      <h3 className="text-sm font-bold text-zinc-300 mb-3 uppercase tracking-wider">{title}</h3>
      {children}
    </div>
  )
}

function FilterGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="mb-3 last:mb-0">
      <div className="text-xs text-zinc-500 mb-1">{label}</div>
      <div className="flex flex-wrap gap-1">{children}</div>
    </div>
  )
}

function FilterPill({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`px-2 py-1 text-xs rounded transition-colors ${
        active
          ? 'bg-red-900 text-red-100 border border-red-700'
          : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'
      }`}
    >
      {children}
    </button>
  )
}

function AlertCard({ alert, onAction }: {
  alert: HateSpeechAlert
  onAction: (action: 'IGNORE' | 'WARN' | 'MUTE' | 'KICK' | 'BAN', reason?: string) => void
}) {
  const [actionReason, setActionReason] = useState('')
  const [showActions, setShowActions] = useState(false)
  const audioUrl = api.voiceAudioUrl(alert.voiceClipId)
  const categories = (alert.categories || '').split(',').filter(Boolean)
  const triggers = (alert.triggers || '').split(',').filter(Boolean)

  // Destaca os gatilhos no texto
  const highlightedText = useMemo(() => {
    if (!alert.transcription || triggers.length === 0) return alert.transcription
    let result = alert.transcription
    triggers.forEach(t => {
      const escaped = t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      result = result.replace(new RegExp(escaped, 'gi'), `<<<HL>>>$&<<</HL>>>`)
    })
    return result
  }, [alert.transcription, triggers])

  return (
    <div className={`bg-zinc-900/70 border rounded-lg p-4 ${
      alert.reviewed ? 'border-zinc-800 opacity-60' : 'border-red-900/50'
    }`}>
      {/* Header */}
      <div className="flex items-center justify-between mb-3 flex-wrap gap-2">
        <div className="flex items-center gap-3">
          <SeverityBadge severity={alert.severity} />
          <div>
            <div className="font-bold text-zinc-100">{alert.playerName}</div>
            <div className="text-xs text-zinc-500 font-mono">
              {new Date(alert.ts).toLocaleString('pt-BR')} · {(alert.confidence * 100).toFixed(0)}% confiança
            </div>
          </div>
        </div>
        <div className="flex flex-wrap gap-1">
          {categories.map(c => (
            <span key={c} className="px-2 py-1 bg-red-950/60 text-red-200 text-xs rounded border border-red-800">
              {CATEGORY_LABELS[c] || c}
            </span>
          ))}
        </div>
      </div>

      {/* Transcrição com gatilhos destacados */}
      <div className="bg-zinc-950/60 border border-zinc-800 rounded p-3 mb-3 font-mono text-sm leading-relaxed">
        {renderHighlighted(highlightedText || '(transcrição vazia)')}
      </div>

      {/* Razão LLM */}
      {alert.reason && (
        <div className="bg-purple-950/30 border border-purple-900/50 rounded p-2 mb-3 text-sm">
          <span className="text-purple-300 font-bold">💭 Análise IA:</span>{' '}
          <span className="text-zinc-200">{alert.reason}</span>
        </div>
      )}

      {/* Áudio + meta */}
      <div className="flex items-center gap-3 mb-3 flex-wrap">
        <audio controls src={audioUrl} className="h-8" style={{ maxWidth: 280 }} />
        <span className="text-xs text-zinc-500">
          Modo: <span className="font-mono">{alert.detectionMode}</span>
        </span>
        {triggers.length > 0 && (
          <span className="text-xs text-zinc-500">
            Gatilhos: <span className="font-mono text-red-400">{triggers.join(', ')}</span>
          </span>
        )}
      </div>

      {/* Ações */}
      {alert.reviewed ? (
        <div className="text-sm text-zinc-400 italic">
          ✓ Reviewed por <span className="font-mono text-zinc-300">{alert.reviewedBy}</span>
          {' '}· Ação: <span className="font-bold text-zinc-200">{alert.actionTaken}</span>
          {alert.reviewedAt && <> · {new Date(alert.reviewedAt).toLocaleString('pt-BR')}</>}
        </div>
      ) : (
        <div>
          {!showActions ? (
            <button
              onClick={() => setShowActions(true)}
              className="px-3 py-1.5 bg-red-700 hover:bg-red-600 text-white text-sm rounded transition-colors"
            >
              ⚖ Tomar ação
            </button>
          ) : (
            <div className="space-y-2">
              <input
                type="text"
                value={actionReason}
                onChange={e => setActionReason(e.target.value)}
                placeholder="Motivo (opcional, será enviado ao player se WARN/KICK/BAN)"
                className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-1.5 text-sm"
              />
              <div className="flex gap-2 flex-wrap">
                {ACTION_BTNS.map(btn => (
                  <button
                    key={btn.key}
                    onClick={() => {
                      if (btn.key === 'BAN' && !confirm(`BANIR ${alert.playerName} permanentemente?`)) return
                      onAction(btn.key, actionReason)
                    }}
                    className={`px-3 py-1.5 text-white text-sm rounded transition-colors ${btn.cls}`}
                  >
                    {btn.icon} {btn.label}
                  </button>
                ))}
                <button
                  onClick={() => setShowActions(false)}
                  className="px-3 py-1.5 text-zinc-400 hover:text-zinc-200 text-sm"
                >
                  Cancelar
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function SeverityBadge({ severity }: { severity: string }) {
  const cls = SEVERITY_COLORS[severity] || 'bg-zinc-800 text-zinc-300 border-zinc-700'
  return (
    <span className={`px-3 py-1 text-xs font-bold uppercase rounded border ${cls}`}>
      {severity}
    </span>
  )
}

/** Renderiza texto com <<<HL>>>...<<<HL>>> como <mark>. */
function renderHighlighted(text: string) {
  const parts = text.split(/<<<HL>>>(.*?)<<<\/HL>>>/g)
  return parts.map((p, i) =>
    i % 2 === 1
      ? <mark key={i} className="bg-red-900/80 text-red-100 px-1 rounded">{p}</mark>
      : <span key={i}>{p}</span>
  )
}

function StatsView({ stats }: { stats: any }) {
  if (!stats) return <div className="text-zinc-400">carregando estatísticas...</div>
  const sevColors: Record<string, string> = {
    LOW: 'bg-yellow-600',
    MEDIUM: 'bg-orange-600',
    HIGH: 'bg-red-600',
  }
  return (
    <div className="grid grid-cols-2 gap-6 max-w-4xl">
      <Section title="Por Severity">
        {Object.entries(stats.bySeverity || {}).length === 0 ? (
          <div className="text-zinc-500 text-sm">Sem dados</div>
        ) : (
          <div className="space-y-2">
            {Object.entries(stats.bySeverity || {}).map(([sev, count]) => {
              const total = Object.values(stats.bySeverity || {}).reduce((a: any, b: any) => a + b, 0) as number
              const pct = total > 0 ? ((count as number) / total) * 100 : 0
              return (
                <div key={sev}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="font-mono">{sev}</span>
                    <span className="text-zinc-400">{count as number} ({pct.toFixed(0)}%)</span>
                  </div>
                  <div className="h-2 bg-zinc-800 rounded overflow-hidden">
                    <div className={`h-full ${sevColors[sev] || 'bg-zinc-600'}`} style={{ width: `${pct}%` }} />
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </Section>

      <Section title="Top 10 Players">
        {(!stats.topPlayers || stats.topPlayers.length === 0) ? (
          <div className="text-zinc-500 text-sm">Sem dados</div>
        ) : (
          <div className="space-y-1">
            {stats.topPlayers.map((p: any, i: number) => (
              <div key={p.uuid} className="flex justify-between items-center px-2 py-1 bg-zinc-800/40 rounded">
                <span className="text-zinc-400 text-sm w-6">#{i + 1}</span>
                <span className="font-mono flex-1">{p.name}</span>
                <span className="text-red-400 font-bold">{p.count}</span>
              </div>
            ))}
          </div>
        )}
      </Section>
    </div>
  )
}

function DebugView() {
  const [text, setText] = useState('')
  const [result, setResult] = useState<any>(null)
  const [loading, setLoading] = useState(false)

  const runScan = async () => {
    if (!text.trim()) return
    setLoading(true)
    try {
      const r = await hateSpeechApi.testScan(text)
      setResult(r)
    } catch (e: any) {
      setResult({ error: e.message })
    }
    setLoading(false)
  }

  return (
    <div className="max-w-3xl space-y-4">
      <Section title="🔧 Testar quick scan (regex)">
        <p className="text-sm text-zinc-400 mb-3">
          Cola uma fala aqui pra ver se os gatilhos regex disparariam. Útil pra
          calibrar a lista de palavras sem precisar gerar transcrição de voz real.
          <br />
          Esse teste <strong>não passa pelo LLM</strong> — só mostra o estágio 1
          (regex match).
        </p>
        <textarea
          value={text}
          onChange={e => setText(e.target.value)}
          placeholder="Digite ou cole um texto pra testar..."
          rows={4}
          className="w-full bg-zinc-950 border border-zinc-700 rounded px-3 py-2 font-mono text-sm"
        />
        <button
          onClick={runScan}
          disabled={loading || !text.trim()}
          className="mt-2 px-4 py-2 bg-purple-700 hover:bg-purple-600 disabled:opacity-50 text-white rounded"
        >
          {loading ? 'analisando...' : 'Rodar quick scan'}
        </button>

        {result && (
          <div className="mt-4 p-3 bg-zinc-950 border border-zinc-800 rounded">
            {result.error ? (
              <div className="text-red-400">Erro: {result.error}</div>
            ) : (
              <>
                <div className="mb-2">
                  <span className="text-zinc-400 text-sm">Resultado: </span>
                  {result.wouldAnalyze ? (
                    <span className="text-red-300 font-bold">⚠ Disparou LLM</span>
                  ) : (
                    <span className="text-green-300 font-bold">✓ Limpo</span>
                  )}
                </div>
                {result.matchedTriggers?.length > 0 && (
                  <div className="mb-2 text-sm">
                    <span className="text-zinc-400">Gatilhos:</span>{' '}
                    {result.matchedTriggers.map((t: string) => (
                      <span key={t} className="inline-block mx-1 px-2 py-0.5 bg-red-900/50 text-red-200 rounded text-xs font-mono">{t}</span>
                    ))}
                  </div>
                )}
                {result.categories?.length > 0 && (
                  <div className="text-sm">
                    <span className="text-zinc-400">Categorias:</span>{' '}
                    {result.categories.map((c: string) => (
                      <span key={c} className="inline-block mx-1 px-2 py-0.5 bg-purple-900/50 text-purple-200 rounded text-xs">
                        {CATEGORY_LABELS[c] || c}
                      </span>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </Section>

      <Section title="ℹ Como funciona o pipeline">
        <ol className="space-y-2 text-sm text-zinc-300 list-decimal list-inside">
          <li><strong>Estágio 1 (regex)</strong>: roda em TODA transcrição. ~µs. Filtra 95%+ sem custo.</li>
          <li><strong>Estágio 2 (Ollama qwen2.5:1.5b)</strong>: só pras transcrições que bateram regex. ~2-3s. Confirma se é ofensivo de fato ou meta-fala.</li>
          <li><strong>Ação manual</strong>: você decide aqui no painel — ignorar / avisar / mutar / kickar / banir. Comando vai direto pro servidor MC.</li>
        </ol>
      </Section>
    </div>
  )
}
