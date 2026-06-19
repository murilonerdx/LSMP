import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, type WhisperConfigSnapshot } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Painel de controle do Whisper — liga/desliga, schedule por hora, e ajuste
 * fino de threads/beam/bestOf/workers/nice em runtime.
 *
 * <p>Status (4s polling): mostra se está processando agora, qual clipe, e
 * stats acumulados. Config (separada, sem polling — só on demand) tem o
 * editor de campos.
 *
 * <p>Mudanças aplicam em até 5s (cache interno do backend), sem restart.
 */
export function WhisperControlPanel() {
    const qc = useQueryClient()

    const statusQ = useQuery({
        queryKey: ['voice-whisper-status'],
        queryFn: api.voiceWhisperStatus,
        refetchInterval: 4000,
    })
    const configQ = useQuery({
        queryKey: ['voice-whisper-config'],
        queryFn: api.voiceWhisperConfigGet,
    })

    const pauseM = useMutation({
        mutationFn: api.voiceWhisperPause,
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['voice-whisper-config'] })
            qc.invalidateQueries({ queryKey: ['voice-whisper-status'] })
            toast.ok('⏸ Whisper pausado')
        },
    })
    const resumeM = useMutation({
        mutationFn: api.voiceWhisperResume,
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['voice-whisper-config'] })
            qc.invalidateQueries({ queryKey: ['voice-whisper-status'] })
            toast.ok('▶ Whisper retomado')
        },
    })
    const updateM = useMutation({
        mutationFn: (patch: Partial<WhisperConfigSnapshot>) => api.voiceWhisperConfigPut(patch),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['voice-whisper-config'] })
            qc.invalidateQueries({ queryKey: ['voice-whisper-status'] })
            toast.ok('💾 config salva (aplica em ~5s)')
        },
        onError: (e: any) => toast.err(e?.message ?? 'falhou'),
    })

    const status = statusQ.data
    const config = configQ.data

    // Estado local pros sliders — evita commit a cada drag
    const [local, setLocal] = useState<Partial<WhisperConfigSnapshot>>({})
    useEffect(() => {
        if (config) setLocal({
            scheduleEnabled: config.scheduleEnabled,
            scheduleHourFrom: config.scheduleHourFrom,
            scheduleHourTo: config.scheduleHourTo,
            threads: config.threads,
            beamSize: config.beamSize,
            bestOf: config.bestOf,
            workers: config.workers,
            cpuPriority: config.cpuPriority,
        })
    }, [config])

    if (!status || !config) {
        return (
            <div className="card-glow text-xs text-liberthia-300/60 animate-pulse">
                Carregando estado do Whisper...
            </div>
        )
    }

    const isActive = config.shouldProcessNow
    const stateColor = isActive ? 'text-green-400' : 'text-amber-400'
    const stateLabel = isActive ? '🟢 ATIVO' : '⏸ PAUSADO'

    function commit(field: keyof WhisperConfigSnapshot, value: any) {
        updateM.mutate({ [field]: value } as any)
    }

    return (
        <div className="card-glow space-y-3">
            <div className="flex items-center justify-between gap-2 flex-wrap">
                <div>
                    <h3 className="font-bold text-sm">🎙 Whisper — Controle de Transcrição</h3>
                    <p className="text-[10px] text-liberthia-300/60">
                        Liga/desliga, agenda horários e ajusta qualidade vs CPU em tempo real.
                    </p>
                </div>
                <div className="text-right">
                    <div className={`font-bold text-sm ${stateColor}`}>{stateLabel}</div>
                    <div className="text-[10px] text-liberthia-300/50">{config.pausedReason}</div>
                </div>
            </div>

            {/* Stats live */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-[10px]">
                <div className="bg-liberthia-900/50 rounded p-1.5">
                    <div className="text-liberthia-300/60">Workers</div>
                    <div className="font-mono font-bold">
                        {status.activeWorkers}/{status.workers}
                    </div>
                </div>
                <div className="bg-liberthia-900/50 rounded p-1.5">
                    <div className="text-liberthia-300/60">Clipes processados</div>
                    <div className="font-mono font-bold">{status.clipsProcessed}</div>
                </div>
                <div className="bg-liberthia-900/50 rounded p-1.5">
                    <div className="text-liberthia-300/60">Ticks</div>
                    <div className="font-mono font-bold">{status.ticksRun}</div>
                </div>
                <div className="bg-liberthia-900/50 rounded p-1.5">
                    <div className="text-liberthia-300/60">Último</div>
                    <div className="font-mono text-[9px] truncate" title={status.lastTickInfo}>
                        {status.lastTickInfo}
                    </div>
                </div>
            </div>

            {/* Quick actions */}
            <div className="flex gap-1 flex-wrap">
                <button
                    className="btn btn-sm text-xs"
                    onClick={() => pauseM.mutate()}
                    disabled={pauseM.isPending || !config.enabled}
                >
                    ⏸ Pause agora
                </button>
                <button
                    className="btn btn-sm text-xs"
                    onClick={() => resumeM.mutate()}
                    disabled={resumeM.isPending || config.enabled}
                >
                    ▶ Resume
                </button>
            </div>

            {/* Schedule */}
            <div className="bg-liberthia-900/30 rounded p-2 space-y-2">
                <label className="flex items-center gap-2 text-xs cursor-pointer">
                    <input
                        type="checkbox"
                        checked={local.scheduleEnabled ?? false}
                        onChange={(e) => {
                            setLocal((s) => ({ ...s, scheduleEnabled: e.target.checked }))
                            commit('scheduleEnabled', e.target.checked)
                        }}
                    />
                    <span className="font-semibold">📅 Agendar horário (rodar só em janela)</span>
                </label>
                {local.scheduleEnabled && (
                    <div className="flex items-center gap-2 text-xs ml-5">
                        <span>Das</span>
                        <input
                            type="number"
                            min={0}
                            max={24}
                            value={local.scheduleHourFrom ?? 0}
                            onChange={(e) => setLocal((s) => ({ ...s, scheduleHourFrom: parseInt(e.target.value) || 0 }))}
                            onBlur={(e) => commit('scheduleHourFrom', parseInt(e.target.value) || 0)}
                            className="input input-sm w-14 text-center"
                        />
                        <span>h até</span>
                        <input
                            type="number"
                            min={0}
                            max={24}
                            value={local.scheduleHourTo ?? 24}
                            onChange={(e) => setLocal((s) => ({ ...s, scheduleHourTo: parseInt(e.target.value) || 24 }))}
                            onBlur={(e) => commit('scheduleHourTo', parseInt(e.target.value) || 24)}
                            className="input input-sm w-14 text-center"
                        />
                        <span>h</span>
                        <span className="text-[10px] text-liberthia-300/50">
                            ({(local.scheduleHourFrom ?? 0) > (local.scheduleHourTo ?? 24)
                                ? 'janela cruza meia-noite'
                                : 'janela contínua'})
                        </span>
                    </div>
                )}
            </div>

            {/* Performance sliders */}
            <div className="space-y-2">
                <div className="text-xs font-semibold text-liberthia-300/80">⚙ Performance</div>
                <Slider
                    label="Threads (por inferência)"
                    value={local.threads ?? 2}
                    min={1}
                    max={8}
                    onChange={(v) => setLocal((s) => ({ ...s, threads: v }))}
                    onCommit={(v) => commit('threads', v)}
                    hint="Mais threads = mais rápido, mas usa mais CPU. Recomendado: 2 (VPS 4 cores)"
                />
                <Slider
                    label="Beam size (hipóteses)"
                    value={local.beamSize ?? 3}
                    min={1}
                    max={10}
                    onChange={(v) => setLocal((s) => ({ ...s, beamSize: v }))}
                    onCommit={(v) => commit('beamSize', v)}
                    hint="Mais alto = melhor qualidade mas mais lento. 3 é sweet spot."
                />
                <Slider
                    label="Best-of (variações)"
                    value={local.bestOf ?? 3}
                    min={1}
                    max={10}
                    onChange={(v) => setLocal((s) => ({ ...s, bestOf: v }))}
                    onCommit={(v) => commit('bestOf', v)}
                    hint="Quantas variações antes de escolher. 3 = bom."
                />
                <Slider
                    label="Nice level (prioridade CPU)"
                    value={local.cpuPriority ?? 15}
                    min={0}
                    max={19}
                    onChange={(v) => setLocal((s) => ({ ...s, cpuPriority: v }))}
                    onCommit={(v) => commit('cpuPriority', v)}
                    hint="0 = normal, 19 = baixíssima. 15 = padrão (cede CPU pra JVM/ffmpeg)"
                />
            </div>

            <div className="text-[9px] text-liberthia-300/40 italic border-t border-liberthia-700/30 pt-2">
                Mudanças aplicam no próximo tick do scheduler (até ~5s). Workers (paralelos) precisam de
                restart do backend pra mudar — outros campos são live.
            </div>
        </div>
    )
}

function Slider({ label, value, min, max, onChange, onCommit, hint }: {
    label: string
    value: number
    min: number
    max: number
    onChange: (v: number) => void
    onCommit: (v: number) => void
    hint?: string
}) {
    return (
        <div className="flex items-center gap-2 text-xs">
            <label className="w-40 text-liberthia-300/80 flex-shrink-0">{label}</label>
            <input
                type="range"
                min={min}
                max={max}
                value={value}
                onChange={(e) => onChange(parseInt(e.target.value))}
                onMouseUp={() => onCommit(value)}
                onTouchEnd={() => onCommit(value)}
                onKeyUp={() => onCommit(value)}
                className="flex-1 accent-purple-500"
            />
            <span className="w-8 text-right font-mono font-bold">{value}</span>
            {hint && (
                <span
                    className="text-[10px] text-liberthia-300/50 cursor-help"
                    title={hint}
                >ⓘ</span>
            )}
        </div>
    )
}
