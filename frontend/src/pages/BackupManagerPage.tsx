import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'
import { useKvState } from '../lib/kvState'

/**
 * Backup Manager — dispara backups manualmente e mostra histórico das execuções
 * (registrado em localStorage, não tem listagem direta no mod).
 */

type Run = { ts: number; file: string; sizeBytes: number }

export function BackupManagerPage() {
  const qc = useQueryClient()
  const [runs, setRuns] = useKvState<Run[]>('backups', [])
  const [busy, setBusy] = useState(false)

  const serverQ = useQuery({ queryKey: ['serverInfo'], queryFn: api.serverInfo, refetchInterval: 5000, retry: false })

  function persist(list: Run[]) {
    setRuns(list)
  }

  async function backup() {
    if (busy) return
    setBusy(true)
    toast.info('📦 Iniciando backup... pode levar alguns segundos')
    try {
      const r: any = await api.backup()
      persist([{ ts: Date.now(), file: r.file, sizeBytes: r.sizeBytes }, ...runs].slice(0, 50))
      toast.ok(`✓ Backup: ${(r.sizeBytes / 1024 / 1024).toFixed(1)} MB`)
    } catch (e: any) { toast.err(e.message) }
    setBusy(false)
  }

  async function saveAll() {
    try { await api.saveAll(); toast.ok('💾 Save flushed') } catch (e: any) { toast.err(e.message) }
  }

  const totalSize = runs.reduce((a, r) => a + r.sizeBytes, 0)

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">💾 Backup Manager</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Gera ZIP completo do mundo. Salva em <code>world/liberthia_backups/backup_*.zip</code>.
          </p>
        </div>
        <div className="flex gap-2">
          <button className="btn-ghost" onClick={saveAll}>💾 Save mundo</button>
          <button className="btn" onClick={backup} disabled={busy}>
            {busy ? '⏳ Backupando...' : '📦 Novo Backup'}
          </button>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-5">
        <div className="card-glow">
          <h3 className="font-bold mb-3">📜 Backups feitos via painel</h3>
          {runs.length === 0 && (
            <div className="text-center py-8 text-liberthia-300/50 text-sm">
              Nenhum backup feito pelo painel ainda. Click <b>Novo Backup</b>.
            </div>
          )}
          <div className="space-y-2">
            {runs.map((r, i) => (
              <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-liberthia-900/40 border border-liberthia-500/20">
                <span className="text-2xl">📦</span>
                <div className="flex-1 min-w-0">
                  <div className="font-mono text-xs truncate">{r.file}</div>
                  <div className="text-[10px] text-liberthia-300/60">{new Date(r.ts).toLocaleString()}</div>
                </div>
                <span className="chip">{(r.sizeBytes / 1024 / 1024).toFixed(1)} MB</span>
                <button className="btn-ghost btn-sm"
                  onClick={() => { navigator.clipboard.writeText(r.file); toast.ok('Path copiado') }}
                  title="Copiar path"
                >📋</button>
                <button className="btn-ghost btn-sm" onClick={() => persist(runs.filter((_, j) => j !== i))}>🗑</button>
              </div>
            ))}
          </div>
        </div>

        <div className="space-y-4">
          <div className="card">
            <h3 className="font-bold mb-2">📊 Status</h3>
            <div className="text-xs space-y-1.5">
              <div className="flex justify-between"><span>Players:</span><span className="font-mono">{serverQ.data?.playerCount ?? '?'}/{serverQ.data?.maxPlayers ?? '?'}</span></div>
              <div className="flex justify-between"><span>TPS:</span><span className="font-mono">{serverQ.data?.tps?.toFixed(1) ?? '?'}</span></div>
              <div className="flex justify-between"><span>Tick atual:</span><span className="font-mono">{(serverQ.data?.tickCount ?? 0).toLocaleString()}</span></div>
              <div className="flex justify-between"><span>Backups:</span><span className="font-mono">{runs.length}</span></div>
              <div className="flex justify-between"><span>Tamanho total:</span><span className="font-mono">{(totalSize / 1024 / 1024).toFixed(1)} MB</span></div>
            </div>
          </div>

          <div className="card text-xs text-liberthia-300/70">
            <div className="font-bold text-liberthia-200 mb-2">💡 Dicas</div>
            <p>• Backups vivem em <code>world/liberthia_backups/</code> no servidor MC</p>
            <p>• ZIP inclui tudo do mundo (exceto session.lock)</p>
            <p>• Save mundo flush os chunks pendentes antes do backup</p>
            <p>• Recomenda automatizar via cron no host do MC (não no painel)</p>
          </div>
        </div>
      </div>
    </div>
  )
}
