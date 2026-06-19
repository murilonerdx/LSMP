/**
 * r161: Magic Control Page — controle total do sistema de magia via frontend.
 *
 * Tabs:
 *  - Spells: lista 116 feitiços filtráveis, give/cast em player
 *  - Custom Spell Designer: cria JSON novo via formulário
 *  - Magic Stats: edita level / source / sanity de qualquer player
 *  - Observer Clones: spawn de clone-observer (modo de horror)
 */
import { useEffect, useMemo, useState } from 'react'
import { api, type SpellDto, type MagicStatsDto, type Player } from '../lib/api'

const SCHOOLS = ['ALL', 'FIRE', 'ICE', 'LIGHTNING', 'BLOOD', 'ELDRITCH', 'HOLY', 'NATURE'] as const
const RARITIES = ['ALL', 'COMMON', 'UNCOMMON', 'RARE', 'EPIC'] as const
const TYPES = ['PROJECTILE', 'BEAM', 'AOE_BURST', 'NOVA', 'CONE', 'RAIN', 'SUMMON',
                'DASH', 'SELF_BUFF', 'TARGETED', 'AURA', 'EXPLOSION'] as const

const SCHOOL_COLORS: Record<string, string> = {
  FIRE: '#ff6633', ICE: '#66ccff', LIGHTNING: '#ffff44', BLOOD: '#990033',
  ELDRITCH: '#7a3dff', HOLY: '#ffeeaa', NATURE: '#33aa33',
}

type Tab = 'spells' | 'designer' | 'stats' | 'observer'

export function MagicControlPage() {
  const [tab, setTab] = useState<Tab>('spells')
  const [players, setPlayers] = useState<Player[]>([])
  const [selectedPlayer, setSelectedPlayer] = useState<string | null>(null)

  useEffect(() => {
    api.players().then(setPlayers).catch(console.error)
    const t = setInterval(() => api.players().then(setPlayers).catch(() => {}), 8000)
    return () => clearInterval(t)
  }, [])

  return (
    <div style={{ padding: 20, background: '#0e0a18', minHeight: '100vh', color: '#e0d0ff' }}>
      <h1 style={{
        background: 'linear-gradient(90deg, #7a3dff, #ffd700)',
        WebkitBackgroundClip: 'text',
        WebkitTextFillColor: 'transparent',
        fontSize: 32, fontWeight: 800, margin: 0,
      }}>
        ✦ Magic Control Center ✦
      </h1>
      <div style={{ color: '#aaa', marginBottom: 16 }}>
        Controle total do sistema de magia: spells, stats, observer-horror — sem entrar no jogo.
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 4, borderBottom: '2px solid #2a1244', marginBottom: 16 }}>
        {[
          ['spells',   '📜 Spells (116)'],
          ['designer', '🎨 Custom Designer'],
          ['stats',    '📊 Player Stats'],
          ['observer', '👁 Observer Clones'],
        ].map(([key, label]) => (
          <button key={key}
            onClick={() => setTab(key as Tab)}
            style={{
              padding: '10px 18px',
              background: tab === key ? '#7a3dff' : 'transparent',
              color: tab === key ? '#fff' : '#aaa',
              border: 'none',
              borderBottom: tab === key ? '3px solid #ffd700' : 'none',
              cursor: 'pointer',
              fontWeight: 600,
            }}>
            {label}
          </button>
        ))}
      </div>

      {/* Player selector (shared by all tabs) */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16, padding: 12, background: '#1a1024', borderRadius: 8 }}>
        <span style={{ color: '#aaa' }}>👤 Player alvo:</span>
        <select
          value={selectedPlayer || ''}
          onChange={(e) => setSelectedPlayer(e.target.value || null)}
          style={{ padding: '6px 10px', background: '#2a1244', color: '#fff', border: '1px solid #7a3dff', borderRadius: 4 }}>
          <option value="">(nenhum)</option>
          {players.map(p => (
            <option key={p.uuid} value={p.uuid}>{p.name}</option>
          ))}
        </select>
        <span style={{ color: '#666', fontSize: 12 }}>{players.length} online</span>
      </div>

      {tab === 'spells'   && <SpellsTab selectedPlayer={selectedPlayer} />}
      {tab === 'designer' && <DesignerTab />}
      {tab === 'stats'    && <StatsTab selectedPlayer={selectedPlayer} />}
      {tab === 'observer' && <ObserverTab selectedPlayer={selectedPlayer} players={players} />}
    </div>
  )
}

// ═══════════════════════════════════════════════════════════════════════════
// Spells Tab — list, filter, give/cast
// ═══════════════════════════════════════════════════════════════════════════
function SpellsTab({ selectedPlayer }: { selectedPlayer: string | null }) {
  const [spells, setSpells] = useState<SpellDto[]>([])
  const [search, setSearch] = useState('')
  const [schoolFilter, setSchoolFilter] = useState<string>('ALL')
  const [rarityFilter, setRarityFilter] = useState<string>('ALL')
  const [status, setStatus] = useState<string>('')

  useEffect(() => {
    api.spellsList().then(r => setSpells(r.spells)).catch(e => setStatus('Erro: ' + e))
  }, [])

  const filtered = useMemo(() => {
    return spells.filter(s => {
      if (schoolFilter !== 'ALL' && s.school !== schoolFilter) return false
      if (rarityFilter !== 'ALL' && s.rarity !== rarityFilter) return false
      if (search && !s.id.toLowerCase().includes(search.toLowerCase())
              && !s.name.toLowerCase().includes(search.toLowerCase())) return false
      return true
    })
  }, [spells, search, schoolFilter, rarityFilter])

  const handleGive = async (sid: string) => {
    if (!selectedPlayer) { setStatus('Selecione um player.'); return }
    try {
      const r = await api.spellGive(selectedPlayer, sid)
      setStatus(r.ok ? `✓ Deu ${sid} ao player` : `Erro: ${r.status}`)
    } catch (e) { setStatus('Erro: ' + e) }
  }
  const handleCast = async (sid: string) => {
    if (!selectedPlayer) { setStatus('Selecione um player.'); return }
    try {
      const r = await api.spellCast(selectedPlayer, sid)
      setStatus(r.ok ? `✓ Cast ${sid} no player` : `Erro: ${r.status}`)
    } catch (e) { setStatus('Erro: ' + e) }
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 12, flexWrap: 'wrap' }}>
        <input
          placeholder="🔍 Buscar spell..."
          value={search} onChange={e => setSearch(e.target.value)}
          style={{ padding: '6px 10px', background: '#2a1244', color: '#fff', border: '1px solid #7a3dff', borderRadius: 4, flex: 1, minWidth: 200 }} />
        <select value={schoolFilter} onChange={e => setSchoolFilter(e.target.value)}
          style={{ padding: '6px 10px', background: '#2a1244', color: '#fff', border: '1px solid #7a3dff', borderRadius: 4 }}>
          {SCHOOLS.map(s => <option key={s} value={s}>{s}</option>)}
        </select>
        <select value={rarityFilter} onChange={e => setRarityFilter(e.target.value)}
          style={{ padding: '6px 10px', background: '#2a1244', color: '#fff', border: '1px solid #7a3dff', borderRadius: 4 }}>
          {RARITIES.map(r => <option key={r} value={r}>{r}</option>)}
        </select>
      </div>

      <div style={{ color: '#aaa', marginBottom: 8 }}>
        {filtered.length} / {spells.length} spells {status && <span style={{ color: '#ffd700', marginLeft: 12 }}>{status}</span>}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 8 }}>
        {filtered.map(s => (
          <div key={s.id} style={{
            background: '#1a1024',
            padding: 10,
            borderRadius: 6,
            borderLeft: `4px solid ${SCHOOL_COLORS[s.school] || '#7a3dff'}`,
          }}>
            <div style={{ fontWeight: 700, color: SCHOOL_COLORS[s.school] || '#fff' }}>{s.name}</div>
            <div style={{ color: '#888', fontSize: 11 }}>{s.id}</div>
            <div style={{ fontSize: 11, color: '#aaa', margin: '4px 0' }}>
              {s.school} · {s.rarity} · {s.damage > 0 ? `${s.damage} dmg · ` : ''}{s.manaCost} mana · CD {s.cooldownTicks}t
            </div>
            <div style={{ fontSize: 11, color: '#888', fontStyle: 'italic', marginBottom: 6 }}>{s.lore}</div>
            <div style={{ display: 'flex', gap: 4 }}>
              <button onClick={() => handleGive(s.id)}
                style={{ flex: 1, padding: '4px 8px', background: '#5a2dcc', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
                🎁 Give
              </button>
              <button onClick={() => handleCast(s.id)}
                style={{ flex: 1, padding: '4px 8px', background: '#ffd700', color: '#1a0824', border: 'none', borderRadius: 4, fontWeight: 700, cursor: 'pointer' }}>
                ⚡ Cast
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

// ═══════════════════════════════════════════════════════════════════════════
// Custom Designer Tab — form to create spell JSON
// ═══════════════════════════════════════════════════════════════════════════
function DesignerTab() {
  const [id, setId] = useState('my_custom_spell')
  const [name, setName] = useState('Meu Feitiço Custom')
  const [school, setSchool] = useState('FIRE')
  const [rarity, setRarity] = useState('UNCOMMON')
  const [type, setType] = useState('PROJECTILE')
  const [mana, setMana] = useState(40)
  const [cooldown, setCooldown] = useState(100)
  const [damage, setDamage] = useState(10)
  const [range, setRange] = useState(24)
  const [colorPrimary, setColorPrimary] = useState('#ff5500')
  const [lore, setLore] = useState('Um feitiço criado via frontend.')
  const [status, setStatus] = useState('')

  const handleSave = async () => {
    const spell = {
      id: id.startsWith('factory_') ? id : 'factory_' + id,
      name, school, rarity, type, lore,
      mana, cooldown, damage, range,
      behavior: { speed: 1.6, lifetime_ticks: 80, pierce: false, aoe_radius: 2 },
      vfx: {
        color_primary: colorPrimary,
        color_secondary: '#ffaa00',
        trail_density: 10, trail_size: 1.2,
        impact_scale: 1.0, impact_particles: 60,
        screen_shake: 0.3, impact_light: 10,
      },
      effects: [],
      element: school,
      category: type,
    }
    try {
      const r = await api.spellCreate(spell)
      setStatus(r.ok ? `✓ Salvo ${r.id} (em runtime; persistir = adicionar JSON em data/liberthia/spells/)` : 'Erro')
    } catch (e) { setStatus('Erro: ' + e) }
  }

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
      <div>
        <h3 style={{ color: '#ffd700' }}>Identidade</h3>
        <Field label="ID:"><input value={id} onChange={e => setId(e.target.value)} style={inp} /></Field>
        <Field label="Nome:"><input value={name} onChange={e => setName(e.target.value)} style={inp} /></Field>
        <Field label="Lore:"><textarea value={lore} onChange={e => setLore(e.target.value)} style={{ ...inp, height: 60 }} /></Field>
        <Field label="School:">
          <select value={school} onChange={e => { setSchool(e.target.value); setColorPrimary(SCHOOL_COLORS[e.target.value] || '#ff5500') }} style={inp}>
            {SCHOOLS.filter(s => s !== 'ALL').map(s => <option key={s}>{s}</option>)}
          </select>
        </Field>
        <Field label="Rarity:">
          <select value={rarity} onChange={e => setRarity(e.target.value)} style={inp}>
            {RARITIES.filter(r => r !== 'ALL').map(r => <option key={r}>{r}</option>)}
          </select>
        </Field>
        <Field label="Type:">
          <select value={type} onChange={e => setType(e.target.value)} style={inp}>
            {TYPES.map(t => <option key={t}>{t}</option>)}
          </select>
        </Field>
      </div>
      <div>
        <h3 style={{ color: '#ffd700' }}>Stats & VFX</h3>
        <Field label={`Mana: ${mana}`}><input type="range" min={1} max={200} value={mana} onChange={e => setMana(+e.target.value)} style={inp} /></Field>
        <Field label={`Cooldown: ${cooldown}t`}><input type="range" min={10} max={600} value={cooldown} onChange={e => setCooldown(+e.target.value)} style={inp} /></Field>
        <Field label={`Damage: ${damage}`}><input type="range" min={0} max={80} value={damage} onChange={e => setDamage(+e.target.value)} style={inp} /></Field>
        <Field label={`Range: ${range}`}><input type="range" min={4} max={64} value={range} onChange={e => setRange(+e.target.value)} style={inp} /></Field>
        <Field label="Color:"><input type="color" value={colorPrimary} onChange={e => setColorPrimary(e.target.value)} style={{ ...inp, height: 40 }} /></Field>
        <button onClick={handleSave} style={{ ...inp, background: '#ffd700', color: '#1a0824', fontWeight: 700, cursor: 'pointer', height: 44 }}>
          💾 Salvar Spell
        </button>
        {status && <div style={{ marginTop: 8, color: status.startsWith('✓') ? '#0f0' : '#f80' }}>{status}</div>}
      </div>
    </div>
  )
}

// ═══════════════════════════════════════════════════════════════════════════
// Stats Tab — magic level, source
// ═══════════════════════════════════════════════════════════════════════════
function StatsTab({ selectedPlayer }: { selectedPlayer: string | null }) {
  const [stats, setStats] = useState<MagicStatsDto | null>(null)
  const [status, setStatus] = useState('')
  const [levelEdit, setLevelEdit] = useState(1)
  const [sourceEdit, setSourceEdit] = useState(0)

  useEffect(() => {
    if (!selectedPlayer) { setStats(null); return }
    api.magicStatsGet(selectedPlayer).then(s => {
      setStats(s)
      if (s.level !== undefined) setLevelEdit(s.level)
      if (s.source !== undefined) setSourceEdit(s.source)
    }).catch(e => setStatus('Erro: ' + e))
  }, [selectedPlayer])

  if (!selectedPlayer) {
    return <div style={{ color: '#888' }}>Selecione um player no topo.</div>
  }
  if (!stats) return <div style={{ color: '#888' }}>Carregando...</div>

  const handleApply = async () => {
    try {
      await api.magicStatsSet(selectedPlayer, { level: levelEdit, source: sourceEdit })
      const fresh = await api.magicStatsGet(selectedPlayer)
      setStats(fresh)
      setStatus('✓ Aplicado')
    } catch (e) { setStatus('Erro: ' + e) }
  }

  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginBottom: 16 }}>
        <Card title="Magic Level" value={`${stats.level}/10`} color="#ffd700" />
        <Card title="Kills This Level" value={`${stats.kills} / ${(stats.level || 1) * 100}`} color="#7a3dff" />
        <Card title="Source" value={`${stats.source} / ${stats.sourceMax}`} color="#66ccff" />
      </div>
      <h3 style={{ color: '#ffd700' }}>Editar</h3>
      <Field label={`Level: ${levelEdit}/10`}>
        <input type="range" min={1} max={10} value={levelEdit} onChange={e => setLevelEdit(+e.target.value)} style={inp} />
      </Field>
      <Field label={`Source: ${sourceEdit}`}>
        <input type="range" min={0} max={1500} value={sourceEdit} onChange={e => setSourceEdit(+e.target.value)} style={inp} />
      </Field>
      <button onClick={handleApply} style={{ ...inp, background: '#ffd700', color: '#1a0824', fontWeight: 700, cursor: 'pointer', height: 40 }}>
        💾 Aplicar
      </button>
      {status && <div style={{ marginTop: 8, color: status.startsWith('✓') ? '#0f0' : '#f80' }}>{status}</div>}
    </div>
  )
}

// ═══════════════════════════════════════════════════════════════════════════
// Observer Tab — spawn observer clones
// ═══════════════════════════════════════════════════════════════════════════
function ObserverTab({ selectedPlayer, players }: { selectedPlayer: string | null; players: Player[] }) {
  const [offsetX, setOffsetX] = useState(0)
  const [offsetZ, setOffsetZ] = useState(20)
  const [count, setCount] = useState(1)
  const [status, setStatus] = useState('')

  const handleSpawn = async () => {
    if (!selectedPlayer) { setStatus('Selecione um player.'); return }
    const target = players.find(p => p.uuid === selectedPlayer)
    if (!target) { setStatus('Player não encontrado.'); return }
    setStatus('Spawning...')
    for (let i = 0; i < count; i++) {
      try {
        await api.spawnObserverClone({
          x: target.position.x + offsetX + (i * 3),
          y: target.position.y,
          z: target.position.z + offsetZ,
          dimension: target.dimension,
          playerName: target.name,
          playerUuid: target.uuid,
        })
      } catch (e) {
        setStatus('Erro: ' + e)
        return
      }
    }
    setStatus(`✓ Spawnados ${count} Observer Clone(s)`)
  }

  return (
    <div>
      <div style={{ background: '#1a1024', padding: 16, borderRadius: 8, marginBottom: 16 }}>
        <h3 style={{ color: '#ff3030', marginTop: 0 }}>👁 Observer Clone</h3>
        <ul style={{ color: '#aaa', fontSize: 13 }}>
          <li>Spawna um clone na aparência do player selecionado</li>
          <li>Só se move <b>quando NÃO está sendo olhado</b></li>
          <li>Contato = 8 hearts de dano</li>
          <li>Atacado por qualquer coisa que <b>não seja machado</b> → teleporta 10 blocos atrás do attacker</li>
          <li>Só morre com <b>machado</b></li>
        </ul>
      </div>
      <Field label={`Offset X: ${offsetX}`}>
        <input type="range" min={-50} max={50} value={offsetX} onChange={e => setOffsetX(+e.target.value)} style={inp} />
      </Field>
      <Field label={`Offset Z: ${offsetZ}`}>
        <input type="range" min={-50} max={50} value={offsetZ} onChange={e => setOffsetZ(+e.target.value)} style={inp} />
      </Field>
      <Field label={`Quantidade: ${count}`}>
        <input type="range" min={1} max={10} value={count} onChange={e => setCount(+e.target.value)} style={inp} />
      </Field>
      <button onClick={handleSpawn} style={{ ...inp, background: '#ff3030', color: '#fff', fontWeight: 700, cursor: 'pointer', height: 44 }}>
        👁 SPAWN OBSERVER CLONE{count > 1 ? `S (${count})` : ''}
      </button>
      {status && <div style={{ marginTop: 8, color: status.startsWith('✓') ? '#0f0' : '#f80' }}>{status}</div>}
    </div>
  )
}

// ═══════════════════════════════════════════════════════════════════════════
// Shared mini-components
// ═══════════════════════════════════════════════════════════════════════════
function Field({ label, children }: { label: string; children: any }) {
  return (
    <div style={{ marginBottom: 8 }}>
      <label style={{ display: 'block', color: '#aaa', fontSize: 12, marginBottom: 2 }}>{label}</label>
      {children}
    </div>
  )
}

function Card({ title, value, color }: { title: string; value: string; color: string }) {
  return (
    <div style={{ background: '#1a1024', padding: 16, borderRadius: 8, borderTop: `3px solid ${color}` }}>
      <div style={{ color: '#888', fontSize: 12 }}>{title}</div>
      <div style={{ color, fontSize: 24, fontWeight: 800 }}>{value}</div>
    </div>
  )
}

const inp: React.CSSProperties = {
  width: '100%',
  padding: '6px 10px',
  background: '#2a1244',
  color: '#fff',
  border: '1px solid #7a3dff',
  borderRadius: 4,
  fontSize: 14,
}
