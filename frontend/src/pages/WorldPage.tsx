import { useState } from 'react'
import { api } from '../lib/api'
import { PageHeader } from '../components/PageHeader'

export function WorldPage() {
  const [msg, setMsg] = useState<{ kind: 'ok' | 'err'; text: string } | null>(null)

  const wrap = async (label: string, fn: () => Promise<any>) => {
    setMsg(null)
    try { await fn(); setMsg({ kind: 'ok', text: `✓ ${label} aplicado` }) }
    catch (e: any) { setMsg({ kind: 'err', text: `✗ ${e.message}` }) }
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Mundo" subtitle="Hora, clima, dificuldade, save" icon="🌍" />

      {msg && (
        <div className={`card border-l-4 ${msg.kind === 'ok' ? 'border-emerald-400 text-emerald-300' : 'border-red-400 text-red-300'}`}>
          {msg.text}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
        {/* Hora */}
        <div className="card-glow">
          <h3 className="font-bold mb-3 flex items-center gap-2"><span>🕐</span> Hora do dia</h3>
          <div className="grid grid-cols-2 gap-2">
            <button className="btn" onClick={() => wrap('manhã', () => api.worldTime(0))}>🌅 Dia (0)</button>
            <button className="btn" onClick={() => wrap('meio-dia', () => api.worldTime(6000))}>☀️ Meio-dia</button>
            <button className="btn" onClick={() => wrap('noite', () => api.worldTime(13000))}>🌙 Noite</button>
            <button className="btn" onClick={() => wrap('meia-noite', () => api.worldTime(18000))}>🌌 Meia-noite</button>
          </div>
          <p className="text-[11px] text-liberthia-300/60 mt-3">
            Time setado em todas dimensões (overworld/nether/end).
          </p>
        </div>

        {/* Clima */}
        <div className="card-glow">
          <h3 className="font-bold mb-3 flex items-center gap-2"><span>☁</span> Clima</h3>
          <div className="grid grid-cols-1 gap-2">
            <button className="btn-success" onClick={() => wrap('céu limpo', () => api.worldWeather('clear'))}>☀️ Limpo</button>
            <button className="btn" onClick={() => wrap('chuva', () => api.worldWeather('rain'))}>🌧 Chuva</button>
            <button className="btn-danger" onClick={() => wrap('tempestade', () => api.worldWeather('thunder'))}>⛈ Tempestade</button>
          </div>
        </div>

        {/* Dificuldade */}
        <div className="card-glow">
          <h3 className="font-bold mb-3 flex items-center gap-2"><span>⚔</span> Dificuldade</h3>
          <div className="grid grid-cols-2 gap-2">
            <button className="btn-ghost" onClick={() => wrap('peaceful', () => api.worldDifficulty('peaceful'))}>🕊 Peaceful</button>
            <button className="btn-ghost" onClick={() => wrap('easy', () => api.worldDifficulty('easy'))}>🟢 Easy</button>
            <button className="btn-ghost" onClick={() => wrap('normal', () => api.worldDifficulty('normal'))}>🟡 Normal</button>
            <button className="btn-ghost" onClick={() => wrap('hard', () => api.worldDifficulty('hard'))}>🔴 Hard</button>
          </div>
        </div>

        {/* Save */}
        <div className="card-glow">
          <h3 className="font-bold mb-3 flex items-center gap-2"><span>💾</span> Manutenção</h3>
          <div className="space-y-2">
            <button className="btn w-full" onClick={() => wrap('save full', () => api.saveAll())}>💾 Save All Chunks</button>
          </div>
          <p className="text-[11px] text-liberthia-300/60 mt-3">
            Força salvar todos os chunks carregados no disco. Igual <code className="bg-black/30 px-1 rounded">/save-all flush</code>.
          </p>
        </div>

        {/* Hora custom */}
        <div className="card xl:col-span-2">
          <h3 className="font-bold mb-3">⏱ Hora customizada (em ticks)</h3>
          <CustomTime />
        </div>
      </div>
    </div>
  )
}

function CustomTime() {
  const [t, setT] = useState(6000)
  return (
    <div className="space-y-3">
      <div className="flex justify-between text-sm">
        <span>Time tick</span>
        <span className="font-mono text-liberthia-300">{t.toLocaleString()}</span>
      </div>
      <input
        type="range"
        min={0}
        max={24000}
        step={100}
        value={t}
        onChange={e => setT(parseInt(e.target.value))}
        className="w-full accent-liberthia-400"
      />
      <div className="flex justify-between text-[10px] text-liberthia-300/60">
        <span>0 - dawn</span>
        <span>6000 - noon</span>
        <span>13000 - night</span>
        <span>24000 - dawn</span>
      </div>
      <button className="btn w-full" onClick={() => api.worldTime(t).catch(() => {})}>
        Aplicar tick {t.toLocaleString()}
      </button>
    </div>
  )
}
