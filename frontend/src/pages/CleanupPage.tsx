import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Server Cleanup — remove tudo que pode ter ficado "pendurado" no servidor:
 *  - Bossbars (todas marcadas como liberthia_)
 *  - NPCs (entities com tag liberthia_npc)
 *  - Frames (liberthia_frame)
 *  - Memoriais (liberthia_memorial)
 *  - Doppelgangers (liberthia_doppel)
 *  - Effects em todos os players
 *  - Titles ativos
 *  - Times (liberthia_)
 *
 * Útil quando: servidor crasha no meio de uma atmosfera, frontend fechou
 * sem stop, bossbar ficou no chat após restart, etc.
 */

type CleanupResult = { label: string; ok: boolean; detail?: string }

const SCOPES = [
  { id: 'effects', emoji: '⚗', label: 'Effects de todos players',
    cmds: ['effect clear @a', 'effect give @a minecraft:slow_falling 100 0 true', 'effect give @a minecraft:regeneration 60 1 true'] },
  { id: 'titles', emoji: '📺', label: 'Titles em todos players',
    cmds: ['title @a clear', 'title @a reset'] },
  { id: 'bossbars', emoji: '📊', label: 'Bossbars (liberthia_*)',
    cmds: [
      // Pega lista e remove cada uma — feito via /bossbar remove em todos os ids conhecidos
      // já que não tem como listar via comando direto. Usamos pattern match local.
    ] },
  { id: 'npcs', emoji: '🎭', label: 'NPCs Liberthia (todos)',
    cmds: ['kill @e[tag=liberthia_npc]'] },
  { id: 'doppel', emoji: '👥', label: 'Doppelgangers',
    cmds: ['kill @e[tag=liberthia_doppel]'] },
  { id: 'frames', emoji: '🖼', label: 'Frames Liberthia',
    cmds: ['kill @e[type=minecraft:glow_item_frame,tag=liberthia_frame]'] },
  { id: 'memorial', emoji: '🗿', label: 'Memorial entities',
    cmds: ['kill @e[tag=liberthia_memorial]'] },
  { id: 'weather', emoji: '☀', label: 'Reset clima/tempo',
    cmds: ['weather clear 12000', 'time set day'] },
  { id: 'gamerule', emoji: '⚙', label: 'GameRules de proteção',
    cmds: ['gamerule keepInventory true', 'gamerule doImmediateRespawn false'] },
  { id: 'teams', emoji: '🎌', label: 'Teams Liberthia (todos)',
    cmds: [] }, // dynamic — busca do banco (kv_configs)
] as const

export function CleanupPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [running, setRunning] = useState(false)
  const [results, setResults] = useState<CleanupResult[]>([])
  const [selected, setSelected] = useState<Set<string>>(new Set(SCOPES.map((s) => s.id)))
  const [knownCounts, setKnownCounts] = useState({ bossbars: 0, teams: 0 })

  // Atualiza contagens do banco a cada 5s
  useEffect(() => {
    let alive = true
    const update = async () => {
      try {
        const [bars, teams] = await Promise.all([getKnownBossbarIds(), getKnownTeamIds()])
        if (alive) setKnownCounts({ bossbars: bars.length, teams: teams.length })
      } catch {}
    }
    update()
    const t = setInterval(update, 5000)
    return () => { alive = false; clearInterval(t) }
  }, [])

  function toggle(id: string) {
    setSelected((cur) => {
      const n = new Set(cur)
      if (n.has(id)) n.delete(id); else n.add(id)
      return n
    })
  }

  /** Pega bossbar IDs do banco (kv_configs) */
  async function getKnownBossbarIds(): Promise<string[]> {
    try {
      const [bars, facs] = await Promise.all([
        api.kvGet<any[]>('bossbars').then((r) => r.data ?? []).catch(() => []),
        api.kvGet<any[]>('factions').then((r) => r.data ?? []).catch(() => []),
      ])
      const ids: string[] = []
      bars.forEach((b: any) => ids.push(b.id))
      facs.forEach((f: any) => ids.push(`fac_${f.id}`))
      return ids
    } catch { return [] }
  }

  async function getKnownTeamIds(): Promise<string[]> {
    try {
      const facs = await api.kvGet<any[]>('factions').then((r) => r.data ?? []).catch(() => [])
      return facs.map((f: any) => `liberthia_${f.id}`)
    } catch { return [] }
  }

  async function panic() {
    if (running) return
    if (!confirm('Confirma cleanup de TUDO selecionado? Isso vai remover bossbars, NPCs, effects, titles e mais.')) return
    setRunning(true); setResults([])
    const items: CleanupResult[] = []

    for (const scope of SCOPES) {
      if (!selected.has(scope.id)) continue
      const cmds: string[] = [...scope.cmds]
      if (scope.id === 'bossbars') {
        const ids = await getKnownBossbarIds()
        for (const id of ids) {
          cmds.push(`bossbar remove minecraft:${id}`)
          cmds.push(`bossbar remove minecraft:fac_${id}`)
        }
        // Fallback: tenta remover alguns padrões comuns
        cmds.push('execute store result score #x dummy run bossbar list')
      }
      if (scope.id === 'teams') {
        for (const t of await getKnownTeamIds()) {
          cmds.push(`team remove ${t}`)
        }
      }

      let ok = true; let failedCount = 0
      for (const c of cmds) {
        try { await api.command(c, 'cleanup') }
        catch { ok = false; failedCount++ }
      }
      items.push({
        label: `${scope.emoji} ${scope.label}`,
        ok,
        detail: cmds.length > 0 ? `${cmds.length - failedCount}/${cmds.length} ok` : 'skip',
      })
    }

    setResults(items)
    setRunning(false)
    toast.ok('🧹 Cleanup completo')
  }

  async function quickPanic() {
    setRunning(true); setResults([])
    const must = [
      'effect clear @a',
      'effect give @a minecraft:slow_falling 100 0 true',
      'title @a clear',
      'title @a reset',
      'kill @e[tag=liberthia_npc]',
      'kill @e[tag=liberthia_doppel]',
      'kill @e[tag=liberthia_frame]',
      'kill @e[tag=liberthia_memorial]',
      'weather clear 12000',
    ]
    // Bossbars conhecidas (do banco)
    for (const id of await getKnownBossbarIds()) {
      must.push(`bossbar remove minecraft:${id}`)
      must.push(`bossbar remove minecraft:fac_${id}`)
    }
    for (const t of await getKnownTeamIds()) must.push(`team remove ${t}`)

    let ok = 0, fail = 0
    for (const c of must) {
      try { await api.command(c, 'cleanup'); ok++ } catch { fail++ }
    }
    setResults([{ label: '🚨 QUICK PANIC', ok: fail === 0, detail: `${ok}/${ok + fail} ok` }])
    setRunning(false)
    toast.ok(`🧹 ${ok} comandos executados${fail > 0 ? ` · ${fail} falhas` : ''}`)
  }

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6">
        <h1 className="page-title">🧹 Server Cleanup</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Remove tudo que pode ter ficado pendurado: bossbars, NPCs, effects, titles, frames, memoriais.
          Use depois de crash, restart, ou quando uma atmosfera deixou efeitos sobrando.
        </p>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-5">
        <div className="space-y-4">
          {/* Quick panic — botão grande */}
          <div className="card-glow !border-red-400/40 !bg-red-500/5">
            <div className="flex items-start gap-3 flex-wrap">
              <div className="text-5xl">🚨</div>
              <div className="flex-1 min-w-0">
                <h2 className="font-bold text-lg">PANIC GLOBAL</h2>
                <p className="text-sm text-liberthia-300/80 mt-1">
                  Roda <b>todos</b> os cleanups críticos imediatamente. Use quando algo tá travado na tela dos players e
                  você não sabe o que é. Não confirma — clique único.
                </p>
                <button className="btn-danger mt-3" onClick={quickPanic} disabled={running}>
                  {running ? '⏳ Limpando...' : '🚨 EXECUTAR PANIC GLOBAL'}
                </button>
              </div>
            </div>
          </div>

          {/* Cleanup seletivo */}
          <div className="card-glow">
            <h3 className="font-bold mb-3">🧹 Cleanup seletivo</h3>
            <p className="text-xs text-liberthia-300/60 mb-3">Marque o que quer limpar, depois Executar.</p>
            <div className="space-y-1.5">
              {SCOPES.map((s) => (
                <label key={s.id} className="flex items-center gap-2 p-2 rounded-lg bg-liberthia-900/40 border border-liberthia-500/20 cursor-pointer hover:bg-liberthia-700/30">
                  <input type="checkbox" checked={selected.has(s.id)} onChange={() => toggle(s.id)} />
                  <span className="text-2xl">{s.emoji}</span>
                  <span className="text-sm flex-1">{s.label}</span>
                  <span className="text-[10px] text-liberthia-300/50 font-mono">{s.cmds.length}+ cmds</span>
                </label>
              ))}
            </div>
            <div className="flex gap-2 mt-3 flex-wrap">
              <button className="btn-ghost btn-sm" onClick={() => setSelected(new Set(SCOPES.map((s) => s.id)))}>Marcar tudo</button>
              <button className="btn-ghost btn-sm" onClick={() => setSelected(new Set())}>Desmarcar</button>
              <button className="btn-amber ml-auto" onClick={panic} disabled={running || selected.size === 0}>
                {running ? '⏳' : `🧹 Executar (${selected.size})`}
              </button>
            </div>
          </div>

          {/* Resultados */}
          {results.length > 0 && (
            <div className="card-glow">
              <h3 className="font-bold mb-2">📋 Resultados</h3>
              <div className="space-y-1.5">
                {results.map((r, i) => (
                  <div key={i} className={`flex items-center gap-2 p-2 rounded-lg ${r.ok ? 'bg-emerald-500/10 border border-emerald-400/30' : 'bg-amber-500/10 border border-amber-400/30'}`}>
                    <span>{r.ok ? '✓' : '⚠'}</span>
                    <span className="flex-1 text-sm">{r.label}</span>
                    {r.detail && <span className="text-[10px] font-mono text-liberthia-300/60">{r.detail}</span>}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="space-y-4">
          <div className="card">
            <h3 className="font-bold mb-2">📊 Estado atual</h3>
            <div className="text-sm space-y-1.5">
              <div className="flex justify-between"><span className="text-liberthia-300/70">Players online</span><span className="font-mono">{players.length}</span></div>
              <div className="flex justify-between"><span className="text-liberthia-300/70">Bossbars conhecidas</span><span className="font-mono">{knownCounts.bossbars}</span></div>
              <div className="flex justify-between"><span className="text-liberthia-300/70">Teams conhecidos</span><span className="font-mono">{knownCounts.teams}</span></div>
            </div>
          </div>

          <div className="card text-xs text-liberthia-300/70 leading-relaxed">
            <div className="font-bold text-liberthia-200 mb-2">💡 Quando usar</div>
            <p>• <b>Crash do server</b> com atmosfera ativa</p>
            <p>• <b>Bossbar grudada</b> após reinício</p>
            <p>• <b>Player com darkness/wither</b> infinito por bug</p>
            <p>• <b>NPC ficou no mundo</b> sem dono</p>
            <p>• <b>Frame fantasma</b> de mural desativado</p>
            <p className="mt-2">A localStorage do navegador guarda os IDs conhecidos. Se você apagou ela, o Cleanup tenta padrões comuns mas pode não pegar tudo.</p>
          </div>
        </div>
      </div>
    </div>
  )
}
