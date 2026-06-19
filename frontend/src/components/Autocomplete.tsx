import { useEffect, useId, useMemo, useRef, useState } from 'react'

/**
 * Combobox: input com autocomplete dropdown filtrado.
 * - `options`: lista de strings OU objetos `{value, label}`
 * - Filtragem por substring (case-insensitive)
 * - Mouse + teclado (↑↓ Enter Esc)
 * - Mostra até `maxResults` (default 20)
 *
 * Uso típico:
 *   <Autocomplete value={effect} onChange={setEffect} options={VANILLA_EFFECTS}
 *      placeholder="minecraft:..." />
 */

type Option = string | { value: string; label?: string }

type Props = {
  value: string
  onChange: (v: string) => void
  options: readonly Option[] | Option[]
  placeholder?: string
  className?: string
  maxResults?: number
  /** Permite digitar valores fora da lista (default true). */
  freeform?: boolean
  disabled?: boolean
}

function getValue(o: Option | null | undefined): string {
  if (o == null) return ''
  if (typeof o === 'string') return o
  return o.value ?? ''
}
function getLabel(o: Option | null | undefined): string {
  if (o == null) return ''
  if (typeof o === 'string') return o
  return o.label ?? o.value ?? ''
}

export function Autocomplete({
  value, onChange, options, placeholder, className = 'input text-xs font-mono',
  maxResults = 20, freeform = true, disabled = false,
}: Props) {
  const [open, setOpen] = useState(false)
  const [highlight, setHighlight] = useState(0)
  const inputRef = useRef<HTMLInputElement>(null)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const listId = useId()

  // Sanitiza: remove entradas null/undefined/sem value. Protege contra dados
  // tortos vindos do backend (404 + cache stale do React Query, mod offline
  // retornando shape inesperado, etc.) — sem isso o Autocomplete crasha
  // toda a página com "Cannot read properties of undefined (toLowerCase)".
  const safeOptions = useMemo(() => {
    if (!Array.isArray(options)) return [] as Option[]
    return options.filter((o): o is Option => {
      if (o == null) return false
      if (typeof o === 'string') return o.length > 0
      return typeof o.value === 'string' && o.value.length > 0
    })
  }, [options])

  const filtered = useMemo(() => {
    const q = (value ?? '').trim().toLowerCase()
    if (!q) return safeOptions.slice(0, maxResults)
    const matches: Option[] = []
    // 1) prefix matches
    for (const o of safeOptions) {
      if (matches.length >= maxResults) break
      if (getValue(o).toLowerCase().startsWith(q) || getLabel(o).toLowerCase().startsWith(q)) {
        matches.push(o)
      }
    }
    // 2) substring matches
    for (const o of safeOptions) {
      if (matches.length >= maxResults) break
      if (matches.includes(o)) continue
      const lv = getValue(o).toLowerCase()
      const ll = getLabel(o).toLowerCase()
      if (lv.includes(q) || ll.includes(q)) matches.push(o)
    }
    return matches
  }, [value, safeOptions, maxResults])

  // Reset highlight quando lista muda
  useEffect(() => { setHighlight(0) }, [filtered.length, value])

  // Fechar quando clicar fora
  useEffect(() => {
    function onClickOutside(e: MouseEvent) {
      if (!wrapperRef.current) return
      if (!wrapperRef.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  function pick(o: Option) {
    onChange(getValue(o))
    setOpen(false)
    inputRef.current?.blur()
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'ArrowDown') {
      e.preventDefault(); setOpen(true)
      setHighlight((h) => Math.min(filtered.length - 1, h + 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setHighlight((h) => Math.max(0, h - 1))
    } else if (e.key === 'Enter' && open && filtered[highlight]) {
      e.preventDefault()
      pick(filtered[highlight])
    } else if (e.key === 'Escape') {
      setOpen(false)
    } else if (e.key === 'Tab' && open && filtered[highlight]) {
      // Tab pra completar
      onChange(getValue(filtered[highlight]))
    }
  }

  return (
    <div ref={wrapperRef} className="relative">
      <input
        ref={inputRef}
        className={className}
        value={value}
        onChange={(e) => { onChange(e.target.value); setOpen(true) }}
        onFocus={() => setOpen(true)}
        onKeyDown={onKeyDown}
        placeholder={placeholder}
        disabled={disabled}
        autoComplete="off"
        spellCheck={false}
        role="combobox"
        aria-expanded={open}
        aria-controls={listId}
      />
      {open && filtered.length > 0 && (
        <ul
          id={listId}
          className="absolute z-50 mt-1 left-0 right-0 max-h-60 overflow-y-auto rounded-lg border border-liberthia-500/40 bg-liberthia-900/95 backdrop-blur-md shadow-xl"
          role="listbox">
          {filtered.map((o, i) => {
            const v = getValue(o)
            const lbl = getLabel(o)
            const isHi = i === highlight
            return (
              <li
                key={v + i}
                role="option"
                aria-selected={isHi}
                onMouseDown={(e) => { e.preventDefault(); pick(o) }}
                onMouseEnter={() => setHighlight(i)}
                className={`px-2 py-1 text-xs cursor-pointer font-mono ${isHi ? 'bg-liberthia-600/40 text-white' : 'text-liberthia-300/90'}`}>
                {lbl !== v ? (
                  <>
                    <span>{lbl}</span>
                    <span className="ml-2 text-[10px] text-liberthia-300/40">{v}</span>
                  </>
                ) : v}
              </li>
            )
          })}
          {filtered.length === maxResults && (
            <li className="px-2 py-1 text-[10px] text-liberthia-300/40 italic border-t border-liberthia-700/40">
              + outros... refine sua busca
            </li>
          )}
        </ul>
      )}
      {!freeform && value && !options.find((o) => getValue(o) === value) && (
        <div className="text-[10px] text-amber-300/70 mt-0.5">⚠ valor não está na lista</div>
      )}
    </div>
  )
}
