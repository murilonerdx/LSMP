import { useState, useEffect, useMemo } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Voice Retention — controle do cleanup automático dos voice clips.
 *
 * Funcionalidades:
 *  - Toggle enable/disable do scheduler
 *  - Slider de retention days (1d → 365d)
 *  - Preview: quantos clipes serão deletados se rodar com X dias
 *  - Botão "rodar agora" pra forçar cleanup manual
 *  - Status do Whisper transcriber (pra debug)
 *
 * Schedule: cron "0 0 5 * * *" (5h AM todo dia). Mudanças via UI valem
 * na próxima execução — sem precisar redeploy.
 */

const PRESETS = [
  { days: 1, label: '1 dia', danger: true },
  { days: 3, label: '3 dias', danger: true },
  { days: 7, label: '1 semana', danger: false },
  { days: 14, label: '2 semanas (default)', danger: false },
  { days: 30, label: '1 mês', danger: false },
  { days: 90, label: '3 meses', danger: false },
  { days: 180, label: '6 meses', danger: false },
  { days: 365, label: '1 ano', danger: false },
]

function fmtBytes(b: number): string {
  if (b < 1024) return `${b}B`
  if (b < 1024 * 1024) return `${(b / 1024).toFixed(1)}KB`
  if (b < 1024 * 1024 * 1024) return `${(b / (1024 * 1024)).toFixed(1)}MB`
  return `${(b / (1024 * 1024 * 1024)).toFixed(2)}GB`
}
function fmtMs(ms: number): string {
  const s = Math.floor(ms / 1000)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (h > 0) return `${h}h ${m}m`
  if (m > 0) return `${m}m ${sec}s`
  return `${sec}s`
}

export function VoiceRetentionPage() {
  const qc = useQueryClient()
  const settingsQ = useQuery({
    queryKey: ['voice-retention'],
    queryFn: api.voiceRetentionGet,
  })
  const statusQ = useQuery({
    queryKey: ['voice-transcription-status'],
    queryFn: api.voiceTranscriptionStatus,
    refetchInterval: 5_000,
  })
  const whisperQ = useQuery({
    queryKey: ['voice-whisper-status'],
    queryFn: api.voiceWhisperStatus,
    refetchInterval: 5_000,
  })

  // Estado local — só fica diferente do server enquanto user mexe + Salvar
  const [enabled, setEnabled] = useState(true)
  const [days, setDays] = useState(14)
  const [maxClipsPerPlayer, setMaxClipsPerPlayer] = useState(0)
  const [dirty, setDirty] = useState(false)
  const [previewDays, setPreviewDays] = useState(14)

  useEffect(() => {
    if (settingsQ.data && !dirty) {
      setEnabled(settingsQ.data.enabled)
      setDays(settingsQ.data.retentionDays)
      setMaxClipsPerPlayer(settingsQ.data.maxClipsPerPlayer ?? 0)
      setPreviewDays(settingsQ.data.retentionDays)
    }
  }, [settingsQ.data, dirty])

  const previewQ = useQuery({
    queryKey: ['voice-retention-preview', previewDays],
    queryFn: () => api.voiceRetentionPreview(previewDays),
    enabled: previewDays > 0,
  })

  async function save() {
    try {
      await api.voiceRetentionUpdate({
        enabled, retentionDays: days,
        maxClipsPerPlayer, updatedBy: 'admin'
      })
      const limitMsg = maxClipsPerPlayer > 0 ? `, máx ${maxClipsPerPlayer}/player` : ', sem limite por player'
      toast.ok(`Configuração salva: ${enabled ? 'ativo' : 'desativado'}, ${days} dias${limitMsg}`)
      setDirty(false)
      qc.invalidateQueries({ queryKey: ['voice-retention'] })
    } catch (e: any) {
      toast.err(`Falha ao salvar: ${e?.message ?? e}`)
    }
  }

  async function enforcePerPlayerNow() {
    if (maxClipsPerPlayer <= 0) {
      toast.err('Configure um limite primeiro (> 0)')
      return
    }
    if (!confirm(
      `Aplicar limite de ${maxClipsPerPlayer} clipes por player AGORA?\n\n` +
      `Players que excederem terão os clipes MAIS ANTIGOS deletados ` +
      `(protegidos não são afetados).\n\nIrreversível.`
    )) return
    try {
      const r = await api.voiceRetentionEnforcePerPlayer(maxClipsPerPlayer)
      toast.ok(`✂ ${r.removed} clipe(s) removido(s) (limite: ${r.maxUsed}/player)`)
      qc.invalidateQueries({ queryKey: ['voice-retention'] })
      qc.invalidateQueries({ queryKey: ['voice-clips'] })
    } catch (e: any) {
      toast.err(`Falha: ${e?.message ?? e}`)
    }
  }

  async function runNow() {
    if (!confirm(`Rodar cleanup AGORA com ${previewDays} dias?\n\n` +
      `Vai deletar ${previewQ.data?.wouldDelete ?? '?'} clipes (${fmtBytes(previewQ.data?.totalBytes ?? 0)}).\n` +
      `Esta ação é IRREVERSÍVEL.`)) return
    try {
      const result = await api.voiceRetentionRun(previewDays)
      toast.ok(`Cleanup executado: ${result.removed} clipes deletados`)
      qc.invalidateQueries({ queryKey: ['voice-retention-preview'] })
      qc.invalidateQueries({ queryKey: ['voice-transcription-status'] })
    } catch (e: any) {
      toast.err(`Falha: ${e?.message ?? e}`)
    }
  }

  const lastTickAgo = useMemo(() => {
    const ts = whisperQ.data?.lastTickTs ?? 0
    if (!ts) return '—'
    const sec = Math.floor((Date.now() - ts) / 1000)
    if (sec < 60) return `${sec}s atrás`
    if (sec < 3600) return `${Math.floor(sec / 60)}m atrás`
    return `${Math.floor(sec / 3600)}h atrás`
  }, [whisperQ.data?.lastTickTs])

  return (
    <div className="route-fade max-w-[1200px]">
      <header className="mb-4">
        <h1 className="page-title">🗑 Voice Retention</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Controle quanto tempo os voice clips ficam armazenados antes do cleanup automático.
          Cleanup roda todo dia às <strong>5h da manhã</strong>. Clipes marcados como protegidos
          (⭐ na Voice Library) NUNCA são deletados.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* ============ Card 1: Settings ============ */}
        <div className="card-glow space-y-4">
          <h2 className="text-base font-bold gradient-text">⚙ Configuração</h2>

          {/* Toggle enabled */}
          <div className="flex items-center justify-between p-3 rounded bg-liberthia-900/40">
            <div>
              <div className="font-bold">Cleanup automático</div>
              <div className="text-[11px] text-liberthia-300/60">
                {enabled
                  ? 'Ativo — vai rodar todo dia às 5h'
                  : 'DESLIGADO — clipes ficam pra sempre (até disco encher)'}
              </div>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input type="checkbox" checked={enabled}
                onChange={(e) => { setEnabled(e.target.checked); setDirty(true) }}
                className="sr-only peer" />
              <div className="w-14 h-7 bg-liberthia-700 rounded-full peer-checked:bg-emerald-500 transition-colors relative">
                <div className={`absolute top-0.5 left-0.5 w-6 h-6 rounded-full bg-white transition-transform ${enabled ? 'translate-x-7' : ''}`} />
              </div>
            </label>
          </div>

          {/* Retention days */}
          <div className={`space-y-2 ${!enabled ? 'opacity-50 pointer-events-none' : ''}`}>
            <div className="flex items-baseline justify-between">
              <label className="text-sm font-bold">Reter por <span className="text-purple-300 text-lg">{days}</span> dias</label>
              <span className="text-[11px] text-liberthia-300/50">{days <= 7 ? '⚠ agressivo' : days <= 30 ? 'moderado' : 'conservador'}</span>
            </div>
            <input type="range" min={1} max={365} value={days}
              onChange={(e) => { setDays(Number(e.target.value)); setDirty(true) }}
              className="w-full" />
            <div className="flex flex-wrap gap-1.5">
              {PRESETS.map(p => (
                <button key={p.days}
                  onClick={() => { setDays(p.days); setDirty(true) }}
                  className={`btn-ghost btn-sm text-[10px] ${
                    days === p.days ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''
                  } ${p.danger ? 'text-red-300' : ''}`}>
                  {p.label}
                </button>
              ))}
            </div>
            <div className="text-[10px] text-liberthia-300/50 italic">
              Cron: <code className="text-purple-300">0 0 5 * * *</code> (todo dia 5h AM).
              Última alteração: {settingsQ.data?.updatedBy || '—'} {settingsQ.data?.updatedAt && `em ${new Date(settingsQ.data.updatedAt).toLocaleString()}`}
            </div>
          </div>

          <button className="btn btn-primary w-full" onClick={save} disabled={!dirty}>
            {dirty ? '💾 Salvar configuração' : '✓ Salvo'}
          </button>
        </div>

        {/* ============ Card 1B: Limite por player ============ */}
        <div className="card-glow space-y-4">
          <h2 className="text-base font-bold gradient-text">👤 Limite por player</h2>
          <p className="text-[11px] text-liberthia-300/70">
            Quando um player passa do limite, os clipes <strong>mais antigos</strong> são
            deletados automaticamente (FIFO). Clipes <strong>protegidos</strong> (⭐) nunca
            contam e nunca são removidos. Aplicado a cada novo upload + no cleanup das 5h.
          </p>

          <div className="space-y-2">
            <div className="flex items-baseline justify-between">
              <label className="text-sm font-bold">
                Máximo: <span className="text-purple-300 text-lg">
                  {maxClipsPerPlayer === 0 ? '∞' : maxClipsPerPlayer}
                </span> clipes / player
              </label>
              <span className="text-[11px] text-liberthia-300/50">
                {maxClipsPerPlayer === 0 ? 'ilimitado' :
                  maxClipsPerPlayer <= 100 ? '⚠ agressivo' :
                  maxClipsPerPlayer <= 1000 ? 'moderado' : 'conservador'}
              </span>
            </div>
            <input type="range" min={0} max={5000} step={50}
              value={maxClipsPerPlayer}
              onChange={(e) => { setMaxClipsPerPlayer(Number(e.target.value)); setDirty(true) }}
              className="w-full" />
            <div className="flex items-center gap-2">
              <input type="number" min={0} max={100000}
                value={maxClipsPerPlayer}
                onChange={(e) => { setMaxClipsPerPlayer(Math.max(0, Number(e.target.value) || 0)); setDirty(true) }}
                className="input text-xs w-24" />
              <span className="text-[11px] text-liberthia-300/60">clipes (0 = ilimitado)</span>
            </div>
            <div className="flex flex-wrap gap-1.5">
              {[
                { v: 0, label: '∞ ilimitado' },
                { v: 100, label: '100', danger: true },
                { v: 250, label: '250' },
                { v: 500, label: '500 (sugerido)' },
                { v: 1000, label: '1000' },
                { v: 2500, label: '2500' },
              ].map(p => (
                <button key={p.v}
                  onClick={() => { setMaxClipsPerPlayer(p.v); setDirty(true) }}
                  className={`btn-ghost btn-sm text-[10px] ${
                    maxClipsPerPlayer === p.v ? 'ring-1 ring-purple-400 bg-purple-500/20' : ''
                  } ${p.danger ? 'text-red-300' : ''}`}>
                  {p.label}
                </button>
              ))}
            </div>
          </div>

          {/* SAVE first — usuários antes clicavam direto em "Aplicar AGORA"
              achando que isso salvava. Não salvava: só aplicava à fila atual.
              O save permanente fica destacado em cima do "Aplicar AGORA". */}
          <button className="btn btn-primary w-full" onClick={save} disabled={!dirty}>
            {dirty ? '💾 Salvar limite (persiste no DB + aplica todo dia 5h)' : '✓ Salvo'}
          </button>
          <p className="text-[10px] text-liberthia-300/60 italic">
            Salvar persiste o limite. Cleanup automático às 5h aplica TODO DIA — players
            que excederem terão os clipes mais antigos (não-protegidos) deletados.
          </p>

          <hr className="border-liberthia-500/20" />

          <button className="btn bg-amber-600/80 hover:bg-amber-600 text-white w-full"
            onClick={enforcePerPlayerNow}
            disabled={maxClipsPerPlayer <= 0}>
            ✂ Aplicar limite AGORA ({maxClipsPerPlayer || '?'}/player)
          </button>
          <p className="text-[10px] text-liberthia-300/50 italic">
            Roda a limpeza one-shot AGORA sem esperar 5h. Bom pra testar valor novo
            antes de salvar de vez.
          </p>
        </div>

        {/* ============ Card 2: Preview + Run Now ============ */}
        <div className="card-glow space-y-4">
          <h2 className="text-base font-bold gradient-text">🔍 Preview / Executar agora</h2>

          <div>
            <label className="text-sm font-bold">Simular cleanup com <span className="text-amber-300">{previewDays}</span> dias</label>
            <input type="range" min={1} max={365} value={previewDays}
              onChange={(e) => setPreviewDays(Number(e.target.value))}
              className="w-full" />
          </div>

          {previewQ.isLoading && (
            <div className="text-xs text-liberthia-300/60">Calculando...</div>
          )}
          {previewQ.data && (
            <div className="p-4 rounded bg-amber-900/20 border border-amber-500/30 space-y-2">
              <div className="text-2xl font-bold text-amber-300">
                {previewQ.data.wouldDelete} <span className="text-sm font-normal text-liberthia-300/70">clipes serão deletados</span>
              </div>
              <div className="grid grid-cols-2 gap-2 text-xs">
                <div>
                  <div className="text-liberthia-300/50">Tamanho liberado</div>
                  <div className="font-mono text-base">{fmtBytes(previewQ.data.totalBytes)}</div>
                </div>
                <div>
                  <div className="text-liberthia-300/50">Duração total</div>
                  <div className="font-mono text-base">{fmtMs(previewQ.data.totalDurationMs)}</div>
                </div>
              </div>
              <div className="text-[10px] text-liberthia-300/50">
                Cutoff: clipes anteriores a {new Date(previewQ.data.cutoff).toLocaleString()}
              </div>
            </div>
          )}

          <button className="btn bg-red-600/80 hover:bg-red-600 text-white w-full"
            onClick={runNow}
            disabled={previewQ.data?.wouldDelete === 0}>
            🗑 Rodar cleanup AGORA ({previewDays}d)
          </button>
          <p className="text-[10px] text-liberthia-300/50 italic">
            ⚠ Ação IRREVERSÍVEL. Clipes marcados como protegidos NÃO são afetados.
            "Rodar agora" funciona mesmo se o cleanup automático estiver desligado.
          </p>
        </div>

        {/* ============ Card 3: Whisper status (debug) ============ */}
        <div className="card-glow space-y-3 lg:col-span-2">
          <h2 className="text-base font-bold gradient-text">🎙 Whisper Transcriber (debug)</h2>
          {whisperQ.data ? (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
              <Stat label="Status" value={
                whisperQ.data.enabled
                  ? whisperQ.data.currentlyRunning
                    ? <span className="text-amber-300">🔄 transcrevendo</span>
                    : <span className="text-emerald-300">✓ ativo</span>
                  : <span className="text-red-300">✗ desligado</span>
              } />
              <Stat label="Ticks rodados" value={whisperQ.data.ticksRun} />
              <Stat label="Clipes processados" value={whisperQ.data.clipsProcessed} />
              <Stat label="Último tick" value={lastTickAgo} />
              <Stat label="Modelo" value={
                <span className="font-mono text-[10px]">
                  {whisperQ.data.modelPath.split('/').pop()}
                </span>
              } />
              <Stat label="Workers (paralelo)" value={
                <span>
                  <span className="text-amber-300 font-bold">{whisperQ.data.activeWorkers ?? 0}</span>
                  <span className="text-liberthia-300/50"> / {whisperQ.data.workers ?? 1}</span>
                </span>
              } />
              <Stat label="Threads / worker" value={whisperQ.data.threads} />
              <Stat label="Beam / Best-of" value={`${whisperQ.data.beamSize} / ${whisperQ.data.bestOf}`} />
              <Stat label="Idioma" value={whisperQ.data.language} />
              <div className="col-span-2 md:col-span-4 p-2 rounded bg-liberthia-900/40 text-[11px]">
                <div className="text-liberthia-300/50">Último resultado:</div>
                <div className="font-mono">{whisperQ.data.lastTickInfo}</div>
              </div>
            </div>
          ) : (
            <div className="text-xs text-liberthia-300/60">Carregando status...</div>
          )}

          {statusQ.data && (
            <div className="mt-3 pt-3 border-t border-liberthia-700/40">
              <div className="text-xs text-liberthia-300/70 mb-1">Fila de transcrição:</div>
              <div className="flex flex-wrap gap-2 text-xs">
                <span className="badge badge-purple">Total: {statusQ.data.total}</span>
                <span className="badge badge-green">✓ {statusQ.data.done}</span>
                <span className="badge">⏳ {statusQ.data.pending}</span>
                <span className="badge badge-amber">🔄 {statusQ.data.processing}</span>
                {statusQ.data.failed > 0 && (
                  <span className="badge badge-red">✗ {statusQ.data.failed}</span>
                )}
                {statusQ.data.skipped > 0 && (
                  <span className="badge">⊘ {statusQ.data.skipped}</span>
                )}
                <span className="badge badge-purple">{statusQ.data.pctDone.toFixed(1)}% completo</span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function Stat({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="p-2 rounded bg-liberthia-900/40">
      <div className="text-[10px] text-liberthia-300/50 uppercase tracking-wider">{label}</div>
      <div className="font-bold mt-0.5">{value}</div>
    </div>
  )
}
