import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, PossessionEntityDto } from '../lib/api'
import { toast } from '../store/toast'
import { renderMcText, MinecraftFormatter } from '../components/MinecraftFormatter'
import { Autocomplete } from '../components/Autocomplete'
import { useParticleOptions, useSoundOptions, useEffectOptions } from '../lib/mcAutocomplete'

/**
 * Eldritch Possession V2 — backend-driven.
 * Engine roda 24/7 no Spring Boot. Possessões persistem no PostgreSQL.
 */

export function EldritchPossessionPage() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['possession'], queryFn: api.possessionList, refetchInterval: 3000 })
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const entities = q.data?.entities ?? []
  const active = q.data?.active ?? []
  const [editing, setEditing] = useState<PossessionEntityDto | null>(null)

  const save = useMutation({
    mutationFn: api.possessionSave,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['possession'] }); toast.ok('✓ salvo') },
    onError: (e: any) => toast.err(e.message),
  })
  const del = useMutation({
    mutationFn: api.possessionDelete,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['possession'] }),
  })
  const start = useMutation({
    mutationFn: (v: { entityId: string; playerUuid: string; durationSec: number }) =>
      api.possessionStart(v.entityId, v.playerUuid, v.durationSec),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['possession'] }); toast.ok('👁 possuído') },
  })
  const end = useMutation({
    mutationFn: api.possessionEnd,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['possession'] }),
  })

  function newEntity() {
    setEditing({
      emoji: '👁', displayName: '§5§lNova Entidade§r',
      particle: 'minecraft:sculk_soul', sound: 'minecraft:ambient.cave',
      voiceLinesJson: '["..."]',
      enterMsg: '§5§o— ela chega —', exitMsg: '§5§o— ela vai —',
      speakIntervalSec: 20,
      effectsJson: '[{"effect":"minecraft:glowing","amplifier":0}]',
      auraColor: '#a78bfa',
    })
  }

  return (
    <div className="route-fade max-w-[1500px]">
      <header className="mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="page-title">👁 Eldritch Possession</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Possessão por entidade cósmica.
            <span className="text-emerald-300 font-bold"> Engine no backend — roda mesmo com a página fechada.</span>
          </p>
        </div>
        <button className="btn" onClick={newEntity}>+ Nova Entidade</button>
      </header>

      {active.length > 0 && (
        <div className="card-glow mb-4 !border-purple-400/40 !bg-purple-500/5">
          <h3 className="font-bold mb-3">⚡ Possessões ativas ({active.length})</h3>
          <div className="space-y-2">
            {active.map((a) => {
              const ent = entities.find((e) => e.id === a.entityId)
              if (!ent) return null
              const remaining = Math.max(0, Math.floor((new Date(a.endsAt).getTime() - Date.now()) / 1000))
              return (
                <div key={a.id} className="flex items-center gap-3 bg-liberthia-900/40 rounded-xl p-2">
                  <div className="text-2xl" style={{ filter: `drop-shadow(0 0 8px ${ent.auraColor})` }}>{ent.emoji}</div>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm">{renderMcText(ent.displayName)} <span className="text-liberthia-300/60">⇢ {a.playerName}</span></div>
                    <div className="text-[10px] text-liberthia-300/50">restante: {remaining}s</div>
                  </div>
                  <button className="btn-danger btn-sm" onClick={() => end.mutate(a.id)}>✕ Encerrar</button>
                </div>
              )
            })}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {entities.map((ent) => (
          <div key={ent.id} className="card-glow"
            style={{ boxShadow: `0 0 24px -8px ${ent.auraColor}` }}>
            <div className="flex items-start gap-3 mb-2">
              <div className="text-4xl" style={{ filter: `drop-shadow(0 0 12px ${ent.auraColor})` }}>{ent.emoji}</div>
              <div className="flex-1 min-w-0">
                <div className="font-bold">{renderMcText(ent.displayName)}</div>
                <div className="text-[10px] text-liberthia-300/50">{parseLines(ent.voiceLinesJson).length} falas · {ent.speakIntervalSec}s</div>
              </div>
            </div>
            <PossessForm ent={ent} players={players} onStart={(eid, uuid, dur) => start.mutate({ entityId: eid, playerUuid: uuid, durationSec: dur })} />
            <div className="grid grid-cols-2 gap-1 mt-2">
              <button className="btn-ghost btn-sm" onClick={() => setEditing(ent)}>✎ Editar</button>
              <button className="btn-ghost btn-sm" onClick={() => del.mutate(ent.id!)}>🗑</button>
            </div>
          </div>
        ))}
      </div>

      {editing && (
        <EntityEditor ent={editing}
          onSave={(e) => save.mutate(e, { onSuccess: () => setEditing(null) })}
          onCancel={() => setEditing(null)} />
      )}
    </div>
  )
}

function parseLines(json: string): string[] {
  try { const a = JSON.parse(json); return Array.isArray(a) ? a : [] } catch { return [] }
}

function PossessForm({ ent, players, onStart }: { ent: PossessionEntityDto; players: any[]; onStart: (entId: string, plyUuid: string, dur: number) => void }) {
  const [target, setTarget] = useState('')
  const [dur, setDur] = useState(120)
  return (
    <div className="grid grid-cols-[1fr_70px_auto] gap-1">
      <select className="input text-xs" value={target} onChange={(e) => setTarget(e.target.value)}>
        <option value="">— player —</option>
        {players.map((p) => <option key={p.uuid} value={p.uuid}>{p.name}</option>)}
      </select>
      <input type="number" className="input text-xs" value={dur} min={30} onChange={(e) => setDur(Number(e.target.value))} title="duração (s)" />
      <button className="btn-danger btn-sm" disabled={!target} onClick={() => onStart(ent.id!, target, dur)}>👁 Possuir</button>
    </div>
  )
}

function EntityEditor({ ent, onSave, onCancel }: { ent: PossessionEntityDto; onSave: (e: PossessionEntityDto) => void; onCancel: () => void }) {
  const [e, setE] = useState<PossessionEntityDto>(ent)
  const lines = parseLines(e.voiceLinesJson)
  const particles = useParticleOptions()
  const sounds = useSoundOptions()
  const effects = useEffectOptions()

  type Eff = { effect: string; amplifier: number }
  function parseEffects(): Eff[] {
    try { const r = JSON.parse(e.effectsJson); return Array.isArray(r) ? r : [] } catch { return [] }
  }
  function setEffects(list: Eff[]) {
    setE({ ...e, effectsJson: JSON.stringify(list) })
  }
  const effList = parseEffects()

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm" onClick={onCancel}>
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(ev) => ev.stopPropagation()}>
        <h3 className="font-bold text-lg mb-4">👁 Editar Entidade</h3>
        <div className="grid grid-cols-[60px_1fr] gap-2 mb-2">
          <input className="input text-2xl text-center" value={e.emoji} onChange={(ev) => setE({ ...e, emoji: ev.target.value })} />
          <input className="input font-mono" value={e.displayName} onChange={(ev) => setE({ ...e, displayName: ev.target.value })} />
        </div>
        <div className="bg-liberthia-900/40 px-2 py-1 rounded mb-3 text-xs">preview: {renderMcText(e.displayName)}</div>
        <div className="grid grid-cols-2 gap-2 mb-2">
          <div><label className="label">Particle</label><Autocomplete value={e.particle} onChange={(v) => setE({ ...e, particle: v })} options={particles} placeholder="minecraft:sculk_soul" /></div>
          <div><label className="label">Sound</label><Autocomplete value={e.sound} onChange={(v) => setE({ ...e, sound: v })} options={sounds} placeholder="minecraft:ambient.cave" /></div>
          <div><label className="label">Intervalo fala (s)</label><input type="number" className="input" value={e.speakIntervalSec} min={5} onChange={(ev) => setE({ ...e, speakIntervalSec: Number(ev.target.value) })} /></div>
          <div><label className="label">Cor aura</label><input type="color" className="input h-9" value={e.auraColor} onChange={(ev) => setE({ ...e, auraColor: ev.target.value })} /></div>
        </div>

        <label className="label">Entrada (suporta múltiplas linhas, §-codes)</label>
        <MinecraftFormatter value={e.enterMsg} onChange={(v) => setE({ ...e, enterMsg: v })} rows={2} maxChars={300} showCounter={false} />

        <div className="h-2" />
        <label className="label">Saída (suporta múltiplas linhas, §-codes)</label>
        <MinecraftFormatter value={e.exitMsg} onChange={(v) => setE({ ...e, exitMsg: v })} rows={2} maxChars={300} showCounter={false} />

        <div className="h-2" />
        <label className="label">Vocabulário (1 frase por linha — Enter quebra linha)</label>
        <textarea className="input mb-3 text-xs" rows={6} value={lines.join('\n')}
          onKeyDown={(ev) => { if (ev.key === 'Enter') ev.stopPropagation() }}
          onChange={(ev) => {
            // NÃO usa .filter(Boolean) — isso removia strings vazias e impedia o Enter
            // de funcionar (linha em branco era removida no mesmo frame). Salvamos as
            // linhas brutas; o filter final acontece no save.
            const raw = ev.target.value.split('\n')
            setE({ ...e, voiceLinesJson: JSON.stringify(raw) })
          }} />

        <label className="label">Effects aplicados durante a possessão</label>
        <div className="space-y-1 mb-3">
          {effList.map((ef, i) => (
            <div key={i} className="grid grid-cols-[1fr_70px_36px] gap-1">
              <Autocomplete value={ef.effect} onChange={(v) => setEffects(effList.map((x, j) => j === i ? { ...x, effect: v } : x))} options={effects} placeholder="minecraft:glowing" />
              <input type="number" className="input text-xs" placeholder="amp" value={ef.amplifier} onChange={(ev) => setEffects(effList.map((x, j) => j === i ? { ...x, amplifier: Number(ev.target.value) } : x))} />
              <button className="btn-ghost btn-sm" onClick={() => setEffects(effList.filter((_, j) => j !== i))}>🗑</button>
            </div>
          ))}
          <button className="btn-ghost btn-sm" onClick={() => setEffects([...effList, { effect: 'minecraft:glowing', amplifier: 0 }])}>+ Effect</button>
        </div>

        <div className="flex gap-2 justify-end">
          <button className="btn-ghost" onClick={onCancel}>Cancelar</button>
          <button className="btn" onClick={() => {
            // Filtra linhas vazias só agora, na hora de salvar
            const cleaned = { ...e }
            try {
              const lines = JSON.parse(e.voiceLinesJson || '[]') as string[]
              const filtered = lines.filter(l => l && l.trim().length > 0)
              cleaned.voiceLinesJson = JSON.stringify(filtered)
            } catch {}
            onSave(cleaned)
          }}>Salvar</button>
        </div>
      </div>
    </div>
  )
}
