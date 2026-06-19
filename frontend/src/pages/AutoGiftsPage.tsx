import { useEffect, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, AutoGiftRuleDto, AutoGiftRunDto } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Sistema de auto-gift por cron. Admin cria regras tipo "Top 3 voice semanal →
 * diamantes" e o backend roda em background, distribuindo rewards.
 */
export function AutoGiftsPage() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState<AutoGiftRuleDto | null>(null)
  const [historyFor, setHistoryFor] = useState<number | null>(null)

  const rulesQ = useQuery({
    queryKey: ['autogift-rules'],
    queryFn: api.autoGiftRules,
    refetchInterval: 10_000,
  })
  const metricsQ = useQuery({ queryKey: ['autogift-metrics'], queryFn: api.autoGiftMetrics })
  const schedulesQ = useQuery({ queryKey: ['autogift-schedules'], queryFn: api.autoGiftSchedules })
  const templatesQ = useQuery({ queryKey: ['autogift-templates'], queryFn: api.autoGiftTemplates })

  const createMut = useMutation({
    mutationFn: api.autoGiftCreate,
    onSuccess: () => { toast.ok('✓ regra criada'); qc.invalidateQueries({ queryKey: ['autogift-rules'] }); setEditing(null) },
    onError: (e: any) => toast.err(e.message),
  })
  const updateMut = useMutation({
    mutationFn: ({ id, body }: { id: number; body: Partial<AutoGiftRuleDto> }) => api.autoGiftUpdate(id, body),
    onSuccess: () => { toast.ok('✓ atualizada'); qc.invalidateQueries({ queryKey: ['autogift-rules'] }); setEditing(null) },
    onError: (e: any) => toast.err(e.message),
  })
  const deleteMut = useMutation({
    mutationFn: api.autoGiftDelete,
    onSuccess: () => { toast.ok('🗑 deletada'); qc.invalidateQueries({ queryKey: ['autogift-rules'] }) },
  })
  const runNowMut = useMutation({
    mutationFn: api.autoGiftRunNow,
    onSuccess: (r) => {
      if (r.ok) toast.ok(`🎁 executada — ${r.winners ?? 0} winners (${r.status})`)
      else toast.err(r.error ?? 'falhou')
      qc.invalidateQueries({ queryKey: ['autogift-rules'] })
    },
    onError: (e: any) => toast.err(e.message),
  })

  const rules = Array.isArray(rulesQ.data) ? rulesQ.data : []
  const metrics = Array.isArray(metricsQ.data) ? metricsQ.data : []
  const schedules = Array.isArray(schedulesQ.data) ? schedulesQ.data : []
  const templates = Array.isArray(templatesQ.data) ? templatesQ.data : []

  function startFromTemplate(t: AutoGiftRuleDto) {
    setEditing({ ...t, id: undefined, active: true })
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-4 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">🎁 Auto-Gifts</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Regras cron que premiam top players de métricas (voice, chat, mortes, etc).
            Backend roda em background — config aqui e esquece.
          </p>
        </div>
        <button className="btn" onClick={() => setEditing({
          name: '', description: '', active: true, metric: 'VOICE_DURATION_MS',
          topN: 3, schedule: 'daily', rewardsJson: '[]', scaleByRank: false, minValue: 0,
        })}>+ Nova regra</button>
      </header>

      {/* Templates prontos */}
      <div className="card-glow mb-4">
        <div className="text-xs uppercase tracking-widest text-liberthia-300/60 mb-2">
          ⚡ Templates prontos — click pra começar
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-1.5">
          {templates.map((t, i) => (
            <button key={i} onClick={() => startFromTemplate(t)}
              className="text-left rounded bg-liberthia-900/60 hover:bg-purple-500/20 p-2 transition">
              <div className="font-bold text-sm">{t.name}</div>
              <div className="text-[10px] text-liberthia-300/60 mt-0.5">{t.description}</div>
              <div className="text-[9px] text-liberthia-300/40 mt-1">
                {t.metric} · Top {t.topN} · {t.schedule}
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Lista de regras */}
      <div className="space-y-2">
        {rules.map(r => (
          <div key={r.id} className={`card-glow ${!r.active ? 'opacity-60' : ''}`}>
            <div className="flex items-center gap-3 flex-wrap">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-base font-bold">{r.name}</span>
                  <span className={`badge text-[9px] ${r.active ? 'badge-green' : ''}`}>
                    {r.active ? '🟢 ativo' : '⏸ pausado'}
                  </span>
                  <span className="badge badge-purple text-[9px]">{r.metric}</span>
                  <span className="badge text-[9px]">Top {r.topN}</span>
                  <span className="badge text-[9px]">{r.schedule}</span>
                  {r.scaleByRank && <span className="badge badge-amber text-[9px]">📊 escalado</span>}
                </div>
                {r.description && (
                  <div className="text-xs text-liberthia-300/70 italic mt-0.5">{r.description}</div>
                )}
                <div className="text-[10px] text-liberthia-300/40 mt-0.5">
                  Próx: {r.nextRunAt ? new Date(r.nextRunAt).toLocaleString() : '—'} ·
                  Última: {r.lastRunAt ? new Date(r.lastRunAt).toLocaleString() : 'nunca'} ·
                  {r.runCount ?? 0} runs
                </div>
              </div>
              <button className="btn-ghost btn-sm" title="Executar agora"
                onClick={() => r.id && runNowMut.mutate(r.id)}>⚡</button>
              <button className="btn-ghost btn-sm" title="Ver histórico"
                onClick={() => r.id && setHistoryFor(r.id)}>📋</button>
              <button className="btn-ghost btn-sm" onClick={() => setEditing(r)}>✏</button>
              <button className="btn-danger btn-sm"
                onClick={() => { if (r.id && confirm(`Deletar "${r.name}"?`)) deleteMut.mutate(r.id) }}>🗑</button>
            </div>
          </div>
        ))}
        {!rulesQ.isLoading && rules.length === 0 && (
          <div className="card text-center py-12 text-liberthia-300/60">
            Sem regras ainda. Use os templates acima ou clica em "+ Nova regra".
          </div>
        )}
      </div>

      {editing && (
        <RuleEditor rule={editing} metrics={metrics} schedules={schedules}
          onCancel={() => setEditing(null)}
          onSave={(r) => {
            if (r.id) updateMut.mutate({ id: r.id, body: r })
            else createMut.mutate(r)
          }} />
      )}

      {historyFor && <HistoryDialog ruleId={historyFor} onClose={() => setHistoryFor(null)} />}
    </div>
  )
}

// ============================================================================
function RuleEditor({ rule, metrics, schedules, onSave, onCancel }: {
  rule: AutoGiftRuleDto
  metrics: Array<{ id: string; label: string; desc: string; unit: string }>
  schedules: Array<{ id: string; label: string }>
  onSave: (r: AutoGiftRuleDto) => void
  onCancel: () => void
}) {
  const [r, setR] = useState(rule)
  const [showPreview, setShowPreview] = useState(false)
  const previewQ = useQuery({
    enabled: showPreview && !!r.metric,
    queryKey: ['autogift-preview', r.metric, r.topN, r.minValue],
    queryFn: () => api.autoGiftPreview(r.metric, r.topN, r.minValue),
  })

  let rewardsValid = true
  try { JSON.parse(r.rewardsJson || '[]') } catch { rewardsValid = false }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70" onClick={onCancel}>
      <div className="card max-w-3xl w-full max-h-[92vh] overflow-y-auto space-y-3" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold text-lg">{r.id ? '✏ Editar' : '+ Nova'} regra</h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
          <div>
            <label className="label">Nome</label>
            <input className="input" value={r.name}
              onChange={(e) => setR({ ...r, name: e.target.value })} />
          </div>
          <div className="flex items-end gap-2">
            <label className="flex items-center gap-2 text-xs">
              <input type="checkbox" checked={r.active}
                onChange={(e) => setR({ ...r, active: e.target.checked })} />
              <span>Ativa (roda no scheduler)</span>
            </label>
          </div>
        </div>

        <div>
          <label className="label">Descrição</label>
          <input className="input" value={r.description ?? ''}
            onChange={(e) => setR({ ...r, description: e.target.value })} />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
          <div>
            <label className="label">Métrica de ranking</label>
            <select className="input" value={r.metric}
              onChange={(e) => setR({ ...r, metric: e.target.value })}>
              {metrics.map(m => <option key={m.id} value={m.id}>{m.label}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Top N</label>
            <input type="number" min={1} max={50} className="input" value={r.topN}
              onChange={(e) => setR({ ...r, topN: Number(e.target.value) || 1 })} />
          </div>
          <div>
            <label className="label">Schedule</label>
            <select className="input" value={r.schedule}
              onChange={(e) => setR({ ...r, schedule: e.target.value })}>
              {schedules.map(s => <option key={s.id} value={s.id}>{s.label}</option>)}
            </select>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
          <div>
            <label className="label">Valor mínimo (filtro)</label>
            <input type="number" min={0} className="input" value={r.minValue}
              onChange={(e) => setR({ ...r, minValue: Number(e.target.value) || 0 })} />
            <p className="text-[10px] text-liberthia-300/50 mt-1">
              Só conta player se métrica ≥ esse valor. Ex: 60000 = só quem falou &gt; 1min.
            </p>
          </div>
          <div className="flex items-end">
            <label className="flex items-center gap-2 text-xs">
              <input type="checkbox" checked={r.scaleByRank}
                onChange={(e) => setR({ ...r, scaleByRank: e.target.checked })} />
              <span>Escalar reward por rank (1º ganha mais, etc)</span>
            </label>
          </div>
        </div>

        <div>
          <label className="label">Rewards (JSON array)</label>
          <textarea className="input font-mono text-[11px]" rows={8}
            value={r.rewardsJson}
            onChange={(e) => setR({ ...r, rewardsJson: e.target.value })} />
          {!rewardsValid && <div className="text-[10px] text-red-400 mt-1">JSON inválido</div>}
          <p className="text-[10px] text-liberthia-300/50 mt-1">
            Tipos: <code>item</code> (id+count+nbt), <code>command</code> (value), <code>broadcast</code> (value).
            Placeholders: <code>{'{player}'}</code>, <code>{'{rank}'}</code>.
          </p>
          <details className="text-[10px] mt-1">
            <summary className="cursor-pointer text-liberthia-300/60">📋 Exemplos</summary>
            <pre className="bg-liberthia-950 rounded p-2 mt-1 overflow-x-auto">
{`[
  {"type":"item","id":"minecraft:diamond","count":5},
  {"type":"command","value":"effect give {player} minecraft:luck 600 0"},
  {"type":"broadcast","value":"🎁 {player} ganhou rank {rank}!"}
]`}
            </pre>
          </details>
        </div>

        {/* Preview do ranking */}
        <div className="card">
          <button className="flex items-center gap-2 text-xs w-full text-left"
            onClick={() => setShowPreview(s => !s)}>
            <span className="font-bold">🔍 Preview do ranking atual</span>
            <span className="text-liberthia-300/40 ml-auto">{showPreview ? '▼' : '▶'}</span>
          </button>
          {showPreview && (
            <div className="mt-2 text-xs">
              {previewQ.isLoading && <div className="text-liberthia-300/60">Carregando…</div>}
              {previewQ.data && previewQ.data.entries.length === 0 && (
                <div className="text-liberthia-300/60">Nenhum player bate com os critérios.</div>
              )}
              {previewQ.data && previewQ.data.entries.length > 0 && (
                <div className="space-y-1">
                  {previewQ.data.entries.map((e, i) => (
                    <div key={e.playerUuid} className="flex items-center gap-2 rounded bg-liberthia-900/60 p-1.5">
                      <span className="w-6 text-center font-bold">{i + 1}.</span>
                      <img src={`https://mc-heads.net/avatar/${encodeURIComponent(e.playerName)}/20`}
                        className="rounded" />
                      <span className="flex-1 font-bold">{e.playerName}</span>
                      <span className="font-mono text-purple-300">{e.value.toLocaleString()}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="flex gap-2 justify-end pt-2">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" disabled={!r.name || !rewardsValid}
            onClick={() => onSave(r)}>Salvar</button>
        </div>
      </div>
    </div>
  )
}

// ============================================================================
function HistoryDialog({ ruleId, onClose }: { ruleId: number; onClose: () => void }) {
  const q = useQuery({
    queryKey: ['autogift-runs', ruleId],
    queryFn: () => api.autoGiftRuns(ruleId),
  })
  const runs: AutoGiftRunDto[] = Array.isArray(q.data?.content) ? q.data!.content : []

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70" onClick={onClose}>
      <div className="card max-w-2xl w-full max-h-[80vh] overflow-y-auto space-y-2" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-bold">📋 Histórico de execuções</h3>
        {runs.length === 0 && <p className="text-xs text-liberthia-300/60">Nunca executou.</p>}
        {runs.map(r => {
          let winners: any[] = []
          try { winners = JSON.parse(r.winnersJson || '[]') } catch {}
          return (
            <div key={r.id} className="card text-xs">
              <div className="flex items-center gap-2">
                <span className={`badge ${r.status === 'SUCCESS' ? 'badge-green' :
                  r.status === 'NO_WINNERS' ? 'badge-amber' : 'badge-red'}`}>
                  {r.status}
                </span>
                <span className="text-liberthia-300/60">{new Date(r.ranAt).toLocaleString()}</span>
                <span className="text-liberthia-300/40">· {r.winnersCount} winners</span>
              </div>
              {winners.length > 0 && (
                <div className="mt-1 space-y-0.5">
                  {winners.map(w => (
                    <div key={w.playerUuid} className="flex gap-2 text-[10px]">
                      <span className="font-bold w-4">{w.rank}.</span>
                      <span>{w.playerName}</span>
                      <span className="text-liberthia-300/50 ml-auto">{w.value?.toLocaleString?.()}</span>
                    </div>
                  ))}
                </div>
              )}
              {r.errorMessage && (
                <div className="text-[10px] text-red-300 mt-1">{r.errorMessage}</div>
              )}
            </div>
          )
        })}
        <div className="flex justify-end pt-2">
          <button className="btn-ghost" onClick={onClose}>Fechar</button>
        </div>
      </div>
    </div>
  )
}
