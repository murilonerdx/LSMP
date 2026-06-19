import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, Player } from '../lib/api'
import { MinecraftFormatter } from '../components/MinecraftFormatter'
import { MinecraftTitlePreview, DeliveryModeSelector, DeliveryMode } from '../components/MinecraftTitlePreview'

/**
 * Power Tools — 8 ferramentas avançadas pra mexer no servidor:
 * Title, Sound, Lightning, Heal, XP, GameMode, SpawnEntity, Particle/Explosion/Backup.
 */
export function PowerToolsPage() {
  const playersQ = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 5000 })
  const players = playersQ.data ?? []
  const [selected, setSelected] = useState<string>('')
  const target = players.find(p => p.uuid === selected) ?? players[0]
  const targetUuid = target?.uuid

  return (
    <div className="route-fade max-w-[1400px]">
      <header className="mb-6">
        <h1 className="page-title">⚡ Power Tools</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Ferramentas avançadas. Selecione um player pra ações individuais ou execute global.
        </p>
      </header>

      {/* Player selector */}
      <div className="card mb-6 flex items-center gap-3 flex-wrap">
        <span className="label">Alvo</span>
        <select className="input flex-1 max-w-md" value={selected} onChange={e => setSelected(e.target.value)}>
          <option value="">— escolher player —</option>
          {players.map(p => (
            <option key={p.uuid} value={p.uuid}>
              {p.name} · {p.dimension} · {p.health.toFixed(0)}❤
            </option>
          ))}
        </select>
        {target && (
          <div className="flex items-center gap-2 text-xs text-liberthia-300/80">
            <span className="pill">{target.gameMode}</span>
            <span className="pill">XP {target.level}</span>
            <span className="pill">
              {target.position.x.toFixed(0)}, {target.position.y.toFixed(0)}, {target.position.z.toFixed(0)}
            </span>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-5">
        <TitleCard uuid={targetUuid} />
        <SoundCard uuid={targetUuid} />
        <LightningCard uuid={targetUuid} player={target} />
        <HealCard uuid={targetUuid} />
        <XpCard uuid={targetUuid} />
        <GameModeCard uuid={targetUuid} />
        <TpToCard uuid={targetUuid} players={players} />
        <SpawnEntityCard pos={target?.position} dim={target?.dimension} />
        <ParticleCard pos={target?.position} />
        <ExplosionCard pos={target?.position} />
        <ServerCard />
      </div>
    </div>
  )
}

// ============== Cards ==============

function TitleCard({ uuid }: { uuid?: string }) {
  const [title, setTitle] = useState('§6Bem-vindo!')
  const [subtitle, setSubtitle] = useState('§7Liberthia Server')
  const [mode, setMode] = useState<DeliveryMode>('title')
  const [fadeIn, setFadeIn] = useState(10)
  const [stay, setStay] = useState(70)
  const [fadeOut, setFadeOut] = useState(20)
  const [busy, setBusy] = useState(false)

  async function send() {
    if (!uuid) return
    setBusy(true)
    try {
      if (mode === 'title') {
        await api.title(uuid, title, subtitle, fadeIn, stay, fadeOut)
      } else if (mode === 'subtitle') {
        // só subtitle (sem title big) — manda title vazio + subtitle
        await api.title(uuid, ' ', title || subtitle, fadeIn, stay, fadeOut)
      } else if (mode === 'actionbar') {
        const players = await api.players()
        const name = players.find((p) => p.uuid === uuid)?.name ?? '@p'
        await api.command(`title ${name} actionbar ${JSON.stringify({ text: title || subtitle })}`, 'powertools')
      } else {
        await api.command(`tellraw @a ${JSON.stringify({ text: title || subtitle })}`, 'powertools')
      }
    } finally { setBusy(false) }
  }

  return (
    <div className="card-glow xl:col-span-2">
      <h3 className="font-bold text-lg mb-1">📢 Title / Subtitle / Actionbar</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Texto na tela do player. Escolha o tamanho — vanilla não tem scale, mas o canal define como aparece.</p>

      <label className="label block mb-1">Tamanho / canal</label>
      <DeliveryModeSelector value={mode} onChange={setMode} />

      <div className="mt-3 grid grid-cols-1 lg:grid-cols-2 gap-3">
        <div>
          <label className="label block mb-1">{mode === 'title' ? 'Title (linha grande)' : 'Texto'}</label>
          <MinecraftFormatter value={title} onChange={setTitle} rows={2} maxChars={120} showCounter={false} />
          {mode === 'title' && (
            <>
              <label className="label block mb-1 mt-2">Subtitle</label>
              <MinecraftFormatter value={subtitle} onChange={setSubtitle} rows={1} maxChars={120} showCounter={false} />
            </>
          )}
          {(mode === 'title' || mode === 'subtitle') && (
            <div className="grid grid-cols-3 gap-2 mt-3 text-xs">
              <div><label className="label">Fade in</label><input type="number" className="input" value={fadeIn} onChange={(e) => setFadeIn(Number(e.target.value))} /></div>
              <div><label className="label">Stay</label><input type="number" className="input" value={stay} onChange={(e) => setStay(Number(e.target.value))} /></div>
              <div><label className="label">Fade out</label><input type="number" className="input" value={fadeOut} onChange={(e) => setFadeOut(Number(e.target.value))} /></div>
            </div>
          )}
          <button className="btn w-full mt-3" disabled={!uuid || busy} onClick={send}>
            {busy ? '⏳' : 'Enviar'}
          </button>
        </div>

        <div>
          <label className="label block mb-1">Preview</label>
          <MinecraftTitlePreview
            title={title}
            subtitle={mode === 'title' ? subtitle : undefined}
            mode={mode}
            className="h-full"
          />
        </div>
      </div>
    </div>
  )
}

function SoundCard({ uuid }: { uuid?: string }) {
  const [sound, setSound] = useState('minecraft:entity.experience_orb.pickup')
  const [pitch, setPitch] = useState(1)
  const PRESETS: [string, string][] = [
    ['minecraft:entity.experience_orb.pickup', '✨ Level Up'],
    ['minecraft:entity.player.levelup', '🎉 LevelUp'],
    ['minecraft:block.note_block.pling', '🎵 Pling'],
    ['minecraft:entity.ender_dragon.growl', '🐉 Dragon'],
    ['minecraft:entity.lightning_bolt.thunder', '⛈ Thunder'],
    ['minecraft:ui.toast.challenge_complete', '🏆 Achievement'],
  ]
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">🔊 Sound</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Toca um som no player.</p>
      <input className="input mb-2 font-mono text-xs" value={sound} onChange={e => setSound(e.target.value)} />
      <div className="flex flex-wrap gap-1.5 mb-3">
        {PRESETS.map(([id, lbl]) => (
          <button key={id} className="btn-ghost btn-sm" onClick={() => setSound(id)}>{lbl}</button>
        ))}
      </div>
      <label className="label">Pitch: {pitch.toFixed(1)}</label>
      <input type="range" min="0.5" max="2" step="0.1" value={pitch}
        onChange={e => setPitch(Number(e.target.value))} className="w-full mb-3" />
      <button className="btn w-full" disabled={!uuid}
        onClick={() => uuid && api.sound(uuid, sound, 1, pitch)}>Tocar</button>
    </div>
  )
}

function LightningCard({ uuid, player }: { uuid?: string; player?: Player }) {
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">⚡ Lightning</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Raio na cabeça do player. Causa dano e fogo.</p>
      <div className="text-center py-4 text-5xl mb-2">⚡</div>
      <button className="btn-amber w-full" disabled={!uuid}
        onClick={() => uuid && api.lightning(uuid)}>
        Strike {player?.name}
      </button>
    </div>
  )
}

function HealCard({ uuid }: { uuid?: string }) {
  const [busy, setBusy] = useState('')
  const wrap = async (fn: () => Promise<any>, key: string) => { setBusy(key); try { await fn() } finally { setBusy('') } }
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">❤ Heal & Feed</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Restaura HP, fome, remove efeitos e fogo.</p>
      <div className="grid grid-cols-2 gap-2">
        <button className="btn-success" disabled={!uuid || busy === 'h'}
          onClick={() => uuid && wrap(() => api.heal(uuid), 'h')}>
          ❤ Full Heal
        </button>
        <button className="btn-success" disabled={!uuid || busy === 'f'}
          onClick={() => uuid && wrap(() => api.feed(uuid), 'f')}>
          🍗 Feed
        </button>
      </div>
      <div className="divider-glow" />
      <button className="btn-success w-full" disabled={busy === 'all'}
        onClick={() => wrap(() => api.healAll(), 'all')}>
        ❤❤ Heal ALL Players
      </button>
    </div>
  )
}

function XpCard({ uuid }: { uuid?: string }) {
  const [levels, setLevels] = useState(5)
  const [mode, setMode] = useState<'add' | 'set'>('add')
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">⭐ Experience</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Adiciona ou seta níveis de XP.</p>
      <div className="flex gap-2 mb-3">
        <button className={`btn-ghost btn-sm flex-1 ${mode === 'add' ? '!bg-liberthia-500/30 !text-white' : ''}`} onClick={() => setMode('add')}>Add</button>
        <button className={`btn-ghost btn-sm flex-1 ${mode === 'set' ? '!bg-liberthia-500/30 !text-white' : ''}`} onClick={() => setMode('set')}>Set</button>
      </div>
      <label className="label">Levels: {levels}</label>
      <input type="range" min={-30} max={100} value={levels} onChange={e => setLevels(Number(e.target.value))} className="w-full mb-3" />
      <button className="btn w-full" disabled={!uuid}
        onClick={() => uuid && api.xp(uuid, levels, 0, mode)}>Aplicar</button>
    </div>
  )
}

function GameModeCard({ uuid }: { uuid?: string }) {
  const MODES: { key: 'survival' | 'creative' | 'adventure' | 'spectator'; emoji: string; label: string }[] = [
    { key: 'survival', emoji: '⚔', label: 'Survival' },
    { key: 'creative', emoji: '🎨', label: 'Creative' },
    { key: 'adventure', emoji: '🗺', label: 'Adventure' },
    { key: 'spectator', emoji: '👁', label: 'Spectator' },
  ]
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">🎮 GameMode</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Muda modo de jogo do player.</p>
      <div className="grid grid-cols-2 gap-2">
        {MODES.map(m => (
          <button key={m.key} className="btn-ghost py-3 flex flex-col items-center gap-1"
            disabled={!uuid} onClick={() => uuid && api.gamemode(uuid, m.key)}>
            <span className="text-2xl">{m.emoji}</span>
            <span className="text-xs">{m.label}</span>
          </button>
        ))}
      </div>
    </div>
  )
}

function TpToCard({ uuid, players }: { uuid?: string; players: Player[] }) {
  const [target, setTarget] = useState('')
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">🌀 TP entre Players</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Teleporta player selecionado para outro.</p>
      <select className="input mb-3" value={target} onChange={e => setTarget(e.target.value)}>
        <option value="">— destino —</option>
        {players.filter(p => p.uuid !== uuid).map(p => (
          <option key={p.uuid} value={p.uuid}>{p.name}</option>
        ))}
      </select>
      <button className="btn-cyan w-full" disabled={!uuid || !target}
        onClick={() => uuid && target && api.tpTo(uuid, target)}>Teleportar</button>
    </div>
  )
}

function SpawnEntityCard({ pos, dim }: { pos?: { x: number; y: number; z: number }; dim?: string }) {
  const [entity, setEntity] = useState('minecraft:zombie')
  const [count, setCount] = useState(1)
  const PRESETS: [string, string][] = [
    ['minecraft:zombie', '🧟 Zombie'],
    ['minecraft:skeleton', '💀 Skeleton'],
    ['minecraft:creeper', '💚 Creeper'],
    ['minecraft:wither', '☠ Wither'],
    ['minecraft:ender_dragon', '🐉 Dragon'],
    ['minecraft:cow', '🐄 Cow'],
    ['minecraft:villager', '🧑 Villager'],
    ['minecraft:tnt', '💣 TNT'],
  ]
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">👾 Spawn Entity</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Spawna no local do player selecionado.</p>
      <input className="input mb-2 font-mono text-xs" value={entity} onChange={e => setEntity(e.target.value)} />
      <div className="flex flex-wrap gap-1.5 mb-3">
        {PRESETS.map(([id, lbl]) => (
          <button key={id} className="btn-ghost btn-sm" onClick={() => setEntity(id)}>{lbl}</button>
        ))}
      </div>
      <label className="label">Quantidade: {count}</label>
      <input type="range" min="1" max="20" value={count} onChange={e => setCount(Number(e.target.value))} className="w-full mb-3" />
      <button className="btn w-full" disabled={!pos}
        onClick={() => pos && api.spawnEntity(entity, pos.x, pos.y, pos.z, count, dim)}>Spawn</button>
    </div>
  )
}

function ParticleCard({ pos }: { pos?: { x: number; y: number; z: number } }) {
  const [particle, setParticle] = useState('minecraft:end_rod')
  const PRESETS = [
    'minecraft:end_rod', 'minecraft:flame', 'minecraft:soul_fire_flame',
    'minecraft:heart', 'minecraft:dragon_breath', 'minecraft:portal',
    'minecraft:totem_of_undying', 'minecraft:explosion',
  ]
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">✨ Particles</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Burst de partículas no local.</p>
      <select className="input mb-3" value={particle} onChange={e => setParticle(e.target.value)}>
        {PRESETS.map(p => <option key={p} value={p}>{p.replace('minecraft:', '')}</option>)}
      </select>
      <button className="btn w-full" disabled={!pos}
        onClick={() => pos && api.particle(particle, pos.x, pos.y + 1, pos.z, 60)}>Spray ✨</button>
    </div>
  )
}

function ExplosionCard({ pos }: { pos?: { x: number; y: number; z: number } }) {
  const [power, setPower] = useState(4)
  const [fire, setFire] = useState(false)
  const [blockDmg, setBlockDmg] = useState(false)
  return (
    <div className="card-glow">
      <h3 className="font-bold text-lg mb-1">💥 Explosion</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Cuidado — força acima de 8 destrói tudo.</p>
      <label className="label">Power: {power}</label>
      <input type="range" min="1" max="12" value={power} onChange={e => setPower(Number(e.target.value))} className="w-full mb-3" />
      <div className="flex gap-3 mb-3 text-xs">
        <label className="flex items-center gap-1"><input type="checkbox" checked={fire} onChange={e => setFire(e.target.checked)} /> Fogo</label>
        <label className="flex items-center gap-1"><input type="checkbox" checked={blockDmg} onChange={e => setBlockDmg(e.target.checked)} /> Block dmg</label>
      </div>
      <button className="btn-danger w-full" disabled={!pos}
        onClick={() => pos && api.explosion(pos.x, pos.y, pos.z, power, fire, blockDmg)}>BOOM 💥</button>
    </div>
  )
}

function ServerCard() {
  const [busy, setBusy] = useState('')
  const [last, setLast] = useState('')
  return (
    <div className="card-glow lg:col-span-2 xl:col-span-1">
      <h3 className="font-bold text-lg mb-1">💾 Server</h3>
      <p className="text-xs text-liberthia-300/60 mb-3">Save mundo + backup zip completo.</p>
      <div className="grid grid-cols-1 gap-2">
        <button className="btn-cyan" disabled={busy === 's'}
          onClick={async () => { setBusy('s'); try { await api.saveAll(); setLast('✓ saved') } catch (e: any) { setLast('✗ ' + e.message) } finally { setBusy('') } }}>
          💾 Save Now
        </button>
        <button className="btn-amber" disabled={busy === 'b'}
          onClick={async () => { setBusy('b'); try { const r = await api.backup(); setLast(`✓ backup: ${(r.sizeBytes/1024/1024).toFixed(1)} MB`) } catch (e: any) { setLast('✗ ' + e.message) } finally { setBusy('') } }}>
          📦 Backup ZIP
        </button>
      </div>
      {last && <div className="text-xs mt-3 text-liberthia-300/70 font-mono">{last}</div>}
    </div>
  )
}
