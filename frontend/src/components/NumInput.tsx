import { useEffect, useState } from 'react'

/**
 * Number input que aceita negativos, vazio e edição parcial sem ficar NaN.
 *
 * Problema com `<input type="number" value={x} onChange={e => set(Number(e.target.value))} />`:
 *  - User digita "-" pra começar negativo → Number("-") = NaN
 *  - User apaga tudo → Number("") = 0 (perde o que digitava)
 *  - User digita "1.5" mas é parcial ".5" no meio → erros
 *
 * Solução: mantém STRING local enquanto edita. Só commita o parent quando
 * temos um número válido. Sincroniza quando o valor externo muda.
 *
 * Funciona como drop-in pro <input type="number">.
 */

type Props = {
  value: number
  onChange: (v: number) => void
  className?: string
  placeholder?: string
  step?: number
  min?: number
  max?: number
  disabled?: boolean
  title?: string
  /** Permite vazio durante edição (default true). Se false, mantém o último válido. */
  allowEmpty?: boolean
}

export function NumInput({ value, onChange, className, placeholder, step, min, max, disabled, title, allowEmpty = true }: Props) {
  const [local, setLocal] = useState<string>(String(value ?? 0))
  const [focused, setFocused] = useState(false)

  // Sincroniza com valor externo, mas só quando NÃO tá focado
  // (evita sobrescrever a edição do user)
  useEffect(() => {
    if (!focused) setLocal(String(value ?? 0))
  }, [value, focused])

  return (
    <input
      type="number"
      className={className ?? 'input'}
      placeholder={placeholder}
      step={step ?? 1}
      min={min}
      max={max}
      disabled={disabled}
      title={title}
      value={local}
      onFocus={() => setFocused(true)}
      onBlur={() => {
        setFocused(false)
        // Commita o valor final (se vazio e allowEmpty=false, mantém o último)
        const n = parseFloat(local)
        if (!isNaN(n)) onChange(n)
        else if (!allowEmpty) setLocal(String(value ?? 0))
        else onChange(0)
      }}
      onChange={(e) => {
        const v = e.target.value
        setLocal(v)
        // Estados intermediários permitidos: "", "-", "0.", "-0", ".5", etc.
        // Só commita o parent quando parseia pra número finito.
        if (v === '' || v === '-' || v === '-.' || v === '.') return
        const n = parseFloat(v)
        if (!isNaN(n) && isFinite(n)) onChange(n)
      }}
    />
  )
}
