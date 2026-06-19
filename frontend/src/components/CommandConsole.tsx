import { useState } from 'react'
import { api } from '../lib/api'

export function CommandConsole() {
  const [cmd, setCmd] = useState('')
  const [history, setHistory] = useState<{ cmd: string; result: any; ts: number }[]>([])

  const send = async (c: string) => {
    if (!c.trim()) return
    try {
      const res = await api.command(c)
      setHistory(h => [{ cmd: c, result: res, ts: Date.now() }, ...h].slice(0, 50))
    } catch (e: any) {
      setHistory(h => [{ cmd: c, result: { error: e.message }, ts: Date.now() }, ...h].slice(0, 50))
    }
    setCmd('')
  }

  return (
    <div className="card">
      <h3 className="font-bold mb-2">⌨ Console de Comandos (op level 4)</h3>
      <form onSubmit={e => { e.preventDefault(); send(cmd) }} className="flex gap-2 mb-3">
        <span className="text-liberthia-300 self-center">/</span>
        <input
          className="input font-mono flex-1"
          placeholder='give @a minecraft:diamond 64'
          value={cmd}
          onChange={e => setCmd(e.target.value)}
        />
        <button type="submit" className="btn">Run</button>
      </form>
      <div className="space-y-1 max-h-60 overflow-y-auto font-mono text-xs">
        {history.map(h => (
          <div key={h.ts} className="border-l-2 border-liberthia-500 pl-2">
            <div className="text-liberthia-300/80">/ {h.cmd}</div>
            <div className={`opacity-70 ${h.result.error ? 'text-red-400' : 'text-green-400'}`}>
              → {h.result.error ? h.result.error : `result=${h.result.result}`}
            </div>
          </div>
        ))}
        {history.length === 0 && <p className="text-liberthia-300/40 italic">Nenhum comando ainda.</p>}
      </div>
    </div>
  )
}
