import { useRef } from 'react'

/**
 * Editor de texto Minecraft com paleta de cores + estilos visuais.
 * Em vez de digitar §a manualmente, clica no swatch e injeta no caret.
 *
 * Suporta: 16 cores vanilla + bold/italic/underline/strike/obfuscated/reset.
 *
 * Uso:
 *   <MinecraftFormatter
 *     value={text}
 *     onChange={setText}
 *     rows={3}
 *     maxChars={256}
 *     showCounter
 *   />
 */

export const MC_COLORS: { code: string; label: string; hex: string }[] = [
  { code: '§0', label: 'Black', hex: '#000000' },
  { code: '§1', label: 'Dark Blue', hex: '#0000aa' },
  { code: '§2', label: 'Dark Green', hex: '#00aa00' },
  { code: '§3', label: 'Dark Aqua', hex: '#00aaaa' },
  { code: '§4', label: 'Dark Red', hex: '#aa0000' },
  { code: '§5', label: 'Dark Purple', hex: '#aa00aa' },
  { code: '§6', label: 'Gold', hex: '#ffaa00' },
  { code: '§7', label: 'Gray', hex: '#aaaaaa' },
  { code: '§8', label: 'Dark Gray', hex: '#555555' },
  { code: '§9', label: 'Blue', hex: '#5555ff' },
  { code: '§a', label: 'Green', hex: '#55ff55' },
  { code: '§b', label: 'Aqua', hex: '#55ffff' },
  { code: '§c', label: 'Red', hex: '#ff5555' },
  { code: '§d', label: 'Light Purple', hex: '#ff55ff' },
  { code: '§e', label: 'Yellow', hex: '#ffff55' },
  { code: '§f', label: 'White', hex: '#ffffff' },
]

const FORMATS = [
  { code: '§l', label: 'B', tip: 'Bold', style: { fontWeight: 'bold' as const } },
  { code: '§o', label: 'I', tip: 'Italic', style: { fontStyle: 'italic' as const } },
  { code: '§n', label: 'U', tip: 'Underline', style: { textDecoration: 'underline' } },
  { code: '§m', label: 'S', tip: 'Strike', style: { textDecoration: 'line-through' } },
  { code: '§k', label: 'M', tip: 'Magic/Obfuscated', style: {} },
  { code: '§r', label: 'R', tip: 'Reset', style: {} },
]

type Props = {
  value: string
  onChange: (v: string) => void
  rows?: number
  placeholder?: string
  maxChars?: number
  showCounter?: boolean
  showPreview?: boolean
  className?: string
}

export function MinecraftFormatter({
  value, onChange, rows = 3, placeholder, maxChars, showCounter = true, showPreview = true, className = '',
}: Props) {
  const ref = useRef<HTMLTextAreaElement>(null)

  function insert(code: string) {
    const ta = ref.current; if (!ta) { onChange(value + code); return }
    const start = ta.selectionStart, end = ta.selectionEnd
    const next = value.slice(0, start) + code + value.slice(end)
    if (maxChars && next.length > maxChars) return
    onChange(next)
    requestAnimationFrame(() => {
      ta.focus()
      const pos = start + code.length
      ta.setSelectionRange(pos, pos)
    })
  }

  function wrapSelection(code: string) {
    const ta = ref.current; if (!ta) return
    const start = ta.selectionStart, end = ta.selectionEnd
    if (start === end) { insert(code); return }
    const before = value.slice(0, start)
    const sel = value.slice(start, end)
    const after = value.slice(end)
    const next = before + code + sel + '§r' + after
    if (maxChars && next.length > maxChars) return
    onChange(next)
  }

  const len = value.length
  // atLimit = bateu no teto (input bloqueado); nearLimit = se aproximando (85%)
  const atLimit = maxChars ? len >= maxChars : false
  const nearLimit = maxChars ? len >= maxChars * 0.85 : false
  const counterColor = atLimit ? 'text-red-300' : nearLimit ? 'text-amber-300' : 'text-liberthia-300/60'

  return (
    <div className={`space-y-2 ${className}`}>
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-1.5 p-2 rounded-xl bg-liberthia-900/40 border border-liberthia-500/20">
        {/* Formatos */}
        <div className="flex gap-1 pr-2 border-r border-liberthia-500/30">
          {FORMATS.map((f) => (
            <button key={f.code} type="button"
              title={`${f.tip} — ${f.code}`}
              onClick={() => wrapSelection(f.code)}
              className="w-7 h-7 rounded-md bg-liberthia-700/50 hover:bg-liberthia-500/50 text-xs font-bold transition"
              style={f.style}
            >{f.label}</button>
          ))}
        </div>
        {/* Cores */}
        <div className="flex flex-wrap gap-1 flex-1">
          {MC_COLORS.map((c) => (
            <button key={c.code} type="button"
              title={`${c.label} — ${c.code}`}
              onClick={() => insert(c.code)}
              className="w-6 h-6 rounded border border-white/20 hover:scale-110 transition shadow-md"
              style={{ background: c.hex }}
            />
          ))}
        </div>
        <button type="button"
          onClick={() => onChange('')}
          className="text-xs px-2 py-1 rounded-md bg-liberthia-900/60 hover:bg-red-500/30 transition text-liberthia-300/70"
        >🧹</button>
      </div>

      {/* Textarea — maxLength bloqueia digitação no browser;
          onChange trunca pasta/paste pra garantir cap mesmo via paste */}
      <textarea
        ref={ref}
        rows={rows}
        className="code-editor"
        placeholder={placeholder}
        value={value}
        maxLength={maxChars}
        onChange={(e) => {
          if (maxChars && e.target.value.length > maxChars) {
            onChange(e.target.value.slice(0, maxChars))
          } else {
            onChange(e.target.value)
          }
        }}
        spellCheck={false}
      />

      {/* Footer: contador + preview */}
      <div className="flex items-center justify-between gap-3 flex-wrap text-xs">
        {showCounter && maxChars && (
          <div className={`font-mono ${counterColor}`}>
            <span>{len}</span>
            <span className="text-liberthia-300/40"> / {maxChars}</span>
            {nearLimit && !atLimit && <span className="ml-2">⚠ próximo do limite</span>}
            {atLimit && <span className="ml-2">✗ limite atingido — não dá pra digitar mais</span>}
          </div>
        )}
        {showPreview && value && (
          <div className="flex-1 min-w-0 max-w-full overflow-hidden">
            <span className="text-liberthia-300/40 mr-2">preview:</span>
            <span className="font-mono text-sm" style={{ wordBreak: 'break-word', whiteSpace: 'pre-wrap' }}>
              {renderMcText(value)}
            </span>
          </div>
        )}
      </div>
    </div>
  )
}

/** Renderiza texto §-formatado como spans coloridos (pra preview). */
export function renderMcText(text: string): React.ReactNode[] {
  const parts: { text: string; color?: string; bold?: boolean; italic?: boolean; underline?: boolean; strike?: boolean }[] = []
  let cur = { text: '', color: undefined as string | undefined, bold: false, italic: false, underline: false, strike: false }
  for (let i = 0; i < text.length; i++) {
    if (text[i] === '§' && i + 1 < text.length) {
      if (cur.text) parts.push({ ...cur })
      cur = { ...cur, text: '' }
      const code = text[i + 1].toLowerCase()
      const colorEntry = MC_COLORS.find((c) => c.code === '§' + code)
      if (colorEntry) {
        // Reset formats when changing color (vanilla behavior)
        cur = { text: '', color: colorEntry.hex, bold: false, italic: false, underline: false, strike: false }
      } else if (code === 'l') cur.bold = true
      else if (code === 'o') cur.italic = true
      else if (code === 'n') cur.underline = true
      else if (code === 'm') cur.strike = true
      else if (code === 'r') cur = { text: '', color: undefined, bold: false, italic: false, underline: false, strike: false }
      i++
    } else {
      cur.text += text[i]
    }
  }
  if (cur.text) parts.push(cur)
  return parts.map((p, i) => (
    <span key={i} style={{
      color: p.color,
      fontWeight: p.bold ? 'bold' : undefined,
      fontStyle: p.italic ? 'italic' : undefined,
      textDecoration: [p.underline ? 'underline' : '', p.strike ? 'line-through' : ''].filter(Boolean).join(' ') || undefined,
    }}>{p.text}</span>
  ))
}
