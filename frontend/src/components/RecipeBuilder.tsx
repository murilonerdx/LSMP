import { useEffect, useState } from 'react'
import { ItemAutocomplete, ItemIcon } from './ItemAutocomplete'

/**
 * Recipe Builder visual — grid 3x3 estilo crafting table do Minecraft.
 * Cada slot tem autocomplete pra escolher item. Setresult + count.
 *
 * Serializa em JSON: {
 *   "type": "shaped" | "shapeless" | "smelting",
 *   "slots": [9 strings ou null],
 *   "result": "liberthia:foo",
 *   "resultCount": 1
 * }
 *
 * Modo "shapeless": ordem dos slots ignorada. Modo "smelting": só slot 0 (input).
 */

export type RecipeData = {
  type: 'shaped' | 'shapeless' | 'smelting'
  slots: (string | null)[]
  result: string
  resultCount: number
}

const EMPTY: RecipeData = {
  type: 'shaped',
  slots: [null, null, null, null, null, null, null, null, null],
  result: '',
  resultCount: 1,
}

export type RecipeBuilderProps = {
  value?: string  // JSON serializado
  onChange: (json: string) => void
  /** Modo compacto pra preview sem edição. */
  readOnly?: boolean
}

export function RecipeBuilder({ value, onChange, readOnly }: RecipeBuilderProps) {
  const [data, setData] = useState<RecipeData>(() => parseRecipe(value))
  const [activeSlot, setActiveSlot] = useState<number | null>(null)

  useEffect(() => {
    setData(parseRecipe(value))
  }, [value])

  function update(next: RecipeData) {
    setData(next)
    onChange(JSON.stringify(next))
  }

  function setSlot(idx: number, id: string) {
    const slots = [...data.slots]
    slots[idx] = id || null
    update({ ...data, slots })
  }

  function clear() {
    update(EMPTY)
  }

  const isSmelting = data.type === 'smelting'

  return (
    <div className="card-glow !bg-black/40">
      <div className="flex items-center justify-between mb-3 flex-wrap gap-2">
        <h4 className="font-bold text-sm">🔨 Recipe</h4>
        {!readOnly && (
          <div className="flex gap-1">
            <select
              className="input text-xs"
              value={data.type}
              onChange={(e) => update({ ...data, type: e.target.value as any })}>
              <option value="shaped">🟦 Shaped 3×3</option>
              <option value="shapeless">🌀 Shapeless</option>
              <option value="smelting">🔥 Smelting (furnace)</option>
            </select>
            <button type="button" className="btn-ghost btn-sm text-[10px]" onClick={clear}>✕ Limpar</button>
          </div>
        )}
      </div>

      <div className="flex items-center justify-center gap-3 flex-wrap">
        {/* Grid 3x3 (ou 1 slot pra smelting) */}
        <div className={`grid ${isSmelting ? 'grid-cols-1' : 'grid-cols-3'} gap-1 p-2 rounded bg-stone-800/80 border-2 border-stone-700`}>
          {(isSmelting ? [0] : [0, 1, 2, 3, 4, 5, 6, 7, 8]).map((idx) => (
            <RecipeSlot
              key={idx}
              value={data.slots[idx]}
              onClick={() => !readOnly && setActiveSlot(activeSlot === idx ? null : idx)}
              active={activeSlot === idx}
              readOnly={readOnly}
            />
          ))}
        </div>

        {/* Seta + resultado */}
        <div className="text-2xl text-stone-500">→</div>

        <div className="flex flex-col items-center gap-1">
          <ResultSlot
            value={data.result}
            count={data.resultCount}
            onClick={() => !readOnly && setActiveSlot(activeSlot === -1 ? null : -1)}
            active={activeSlot === -1}
            readOnly={readOnly}
          />
          {!readOnly && (
            <input
              type="number"
              min={1}
              max={64}
              value={data.resultCount}
              onChange={(e) => update({ ...data, resultCount: Math.max(1, Math.min(64, Number(e.target.value) || 1)) })}
              className="input text-[10px] w-16 text-center"
              title="quantidade de saída"
            />
          )}
        </div>
      </div>

      {/* Painel de seleção: aparece quando clica num slot */}
      {!readOnly && activeSlot !== null && (
        <div className="mt-3 p-2 rounded bg-liberthia-900/70 border border-purple-500/30">
          <div className="text-[10px] text-liberthia-300/60 mb-1">
            {activeSlot === -1 ? 'Resultado' : `Slot ${activeSlot}`}
          </div>
          <ItemAutocomplete
            value={activeSlot === -1 ? data.result : (data.slots[activeSlot] ?? '')}
            onChange={(id) => {
              if (activeSlot === -1) update({ ...data, result: id })
              else setSlot(activeSlot, id)
            }}
            placeholder="ex: minecraft:diamond ou liberthia:..."
          />
          {((activeSlot === -1 && data.result) || (activeSlot >= 0 && data.slots[activeSlot])) && (
            <button type="button"
              className="btn-ghost btn-sm text-[10px] mt-1 text-red-300"
              onClick={() => {
                if (activeSlot === -1) update({ ...data, result: '' })
                else setSlot(activeSlot, '')
              }}>
              🗑 Limpar slot
            </button>
          )}
        </div>
      )}
    </div>
  )
}

function RecipeSlot({ value, onClick, active, readOnly }: {
  value: string | null
  onClick: () => void
  active?: boolean
  readOnly?: boolean
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={readOnly}
      className={`relative w-12 h-12 rounded border-2 transition flex items-center justify-center
        ${active ? 'border-purple-400 ring-2 ring-purple-400/50' : 'border-stone-600'}
        ${value ? 'bg-stone-700/60' : 'bg-stone-900/80'}
        ${readOnly ? 'cursor-default' : 'hover:bg-stone-600/60 cursor-pointer'}
      `}>
      {value ? (
        <>
          <ItemIcon id={value} size={32} />
          <span className="absolute top-0.5 left-0.5 text-[7px] font-mono text-stone-300 opacity-50 truncate max-w-[40px]">
            {value.split(':').pop()}
          </span>
        </>
      ) : (
        <span className="text-stone-600 text-[9px]">vazio</span>
      )}
    </button>
  )
}

function ResultSlot({ value, count, onClick, active, readOnly }: {
  value: string
  count: number
  onClick: () => void
  active?: boolean
  readOnly?: boolean
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={readOnly}
      className={`relative w-16 h-16 rounded border-2 transition flex items-center justify-center
        ${active ? 'border-purple-400 ring-2 ring-purple-400/50' : 'border-amber-500'}
        ${value ? 'bg-amber-700/30' : 'bg-stone-900/80'}
        ${readOnly ? 'cursor-default' : 'hover:bg-amber-700/40 cursor-pointer'}
      `}>
      {value ? (
        <>
          <ItemIcon id={value} size={48} />
          {count > 1 && (
            <span className="absolute bottom-0 right-0.5 text-[11px] font-bold text-white drop-shadow"
              style={{ textShadow: '1px 1px 0 #000' }}>
              ×{count}
            </span>
          )}
        </>
      ) : (
        <span className="text-stone-600 text-[10px]">?</span>
      )}
    </button>
  )
}

function parseRecipe(json?: string): RecipeData {
  if (!json || json.trim() === '') return { ...EMPTY }
  try {
    const obj = JSON.parse(json)
    return {
      type: obj.type ?? 'shaped',
      slots: Array.isArray(obj.slots) && obj.slots.length === 9
        ? obj.slots
        : [null, null, null, null, null, null, null, null, null],
      result: obj.result ?? '',
      resultCount: Number(obj.resultCount) || 1,
    }
  } catch {
    return { ...EMPTY }
  }
}
