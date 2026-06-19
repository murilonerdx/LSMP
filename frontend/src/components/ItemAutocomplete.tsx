import { useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'

/**
 * Autocomplete de items in-game. Carrega o registry de items via /api/items
 * (já existente) e oferece busca fuzzy.
 *
 * v95+: dropdown usa React Portal pra escapar de containers com overflow:hidden
 * (recipe builder dentro de card-glow estava cortando a lista). O posicionamento
 * é calculado em runtime via getBoundingClientRect do input.
 */
export type ItemAutocompleteProps = {
  value: string
  onChange: (id: string) => void
  placeholder?: string
  small?: boolean
  liberthiaOnly?: boolean
}

export function ItemAutocomplete({ value, onChange, placeholder = 'Buscar item...', small, liberthiaOnly }: ItemAutocompleteProps) {
  const [query, setQuery] = useState(value)
  const [open, setOpen] = useState(false)
  const [coords, setCoords] = useState({ top: 0, left: 0, width: 0 })
  const inputRef = useRef<HTMLInputElement>(null)
  const dropdownRef = useRef<HTMLDivElement>(null)

  const itemsQ = useQuery({
    queryKey: ['mc-items-all'],
    queryFn: api.items,
    staleTime: 5 * 60_000,
  })

  const allItems = itemsQ.data ?? []

  const matches = useMemo(() => {
    const q = query.trim().toLowerCase()
    let pool = allItems
    if (liberthiaOnly) pool = pool.filter(i => i.id.startsWith('liberthia:'))
    if (!q) return pool.slice(0, 50)
    return pool
      .filter(i => i.id.toLowerCase().includes(q) || (i.name ?? '').toLowerCase().includes(q))
      .slice(0, 50)
  }, [query, allItems, liberthiaOnly])

  useEffect(() => { setQuery(value) }, [value])

  // Calcula posição do dropdown baseado no input — chama no open + scroll/resize
  function updateCoords() {
    if (!inputRef.current) return
    const r = inputRef.current.getBoundingClientRect()
    setCoords({
      top: r.bottom + 4,
      left: r.left,
      width: Math.max(r.width, 260),
    })
  }

  useEffect(() => {
    if (!open) return
    updateCoords()
    function onScroll() { updateCoords() }
    window.addEventListener('scroll', onScroll, true) // capture pra apanhar todos scroll containers
    window.addEventListener('resize', updateCoords)
    return () => {
      window.removeEventListener('scroll', onScroll, true)
      window.removeEventListener('resize', updateCoords)
    }
  }, [open])

  // Fecha quando clica fora
  useEffect(() => {
    function onClick(e: MouseEvent) {
      if (dropdownRef.current?.contains(e.target as Node)) return
      if (inputRef.current?.contains(e.target as Node)) return
      setOpen(false)
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])

  function select(id: string) {
    onChange(id)
    setQuery(id)
    setOpen(false)
    inputRef.current?.blur()
  }

  /**
   * Detecta se o que o usuário digitou parece um ID válido de item Minecraft
   * MAS não existe no registry atual. Importante: quando você está criando
   * uma feature/item NOVO, o ID ainda não existe no servidor — esse campo
   * deve aceitar IDs "futuros" pra docs.
   */
  const trimmed = query.trim()
  const isKnownId = !!trimmed && allItems.some(i => i.id === trimmed)
  const looksLikeId = /^[a-z0-9_]+:[a-z0-9_/\.]+$/i.test(trimmed)
  const isUnknownButValid = !!trimmed && !isKnownId && looksLikeId

  const dropdown = open && matches.length > 0 ? (
    <div
      ref={dropdownRef}
      style={{
        position: 'fixed',
        top: coords.top,
        left: coords.left,
        width: coords.width,
        maxHeight: 320,
        zIndex: 9999,
      }}
      className="overflow-y-auto rounded
                 bg-liberthia-900/95 backdrop-blur border border-purple-500/40 shadow-2xl">
      {itemsQ.isLoading && (
        <div className="p-2 text-[10px] text-liberthia-300/50">carregando catalogo...</div>
      )}
      {matches.map(i => (
        <button
          key={i.id}
          type="button"
          onMouseDown={(e) => { e.preventDefault(); select(i.id) }}
          className="w-full flex items-center gap-2 text-left px-2 py-1.5 text-[10px]
                     hover:bg-purple-500/20 transition border-b border-purple-500/10">
          <ItemIcon id={i.id} size={20} />
          <div className="flex-1 min-w-0">
            <div className="font-bold text-[11px] truncate">{i.name ?? i.id}</div>
            <div className="font-mono text-[9px] text-liberthia-300/50 truncate">{i.id}</div>
          </div>
        </button>
      ))}
    </div>
  ) : null

  return (
    <div className={small ? '' : 'w-full'}>
      <input
        ref={inputRef}
        type="text"
        value={query}
        onFocus={() => setOpen(true)}
        onChange={(e) => {
          // CORREÇÃO: ao digitar, propaga IMEDIATAMENTE pro parent. Antes só
          // chamava onChange ao selecionar do dropdown — IDs novos (criados pelo
          // usuário, ainda não no registry) ficavam com itemId vazio no form.
          const v = e.target.value
          setQuery(v)
          onChange(v)
          setOpen(true)
        }}
        onBlur={() => {
          // Garante sync final caso usuário clique fora sem selecionar
          if (query !== value) onChange(query)
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            e.preventDefault()
            if (matches.length > 0) {
              select(matches[0].id)
            } else {
              // Sem matches mas Enter: confirma valor atual (ID custom novo)
              onChange(query)
              setOpen(false)
              inputRef.current?.blur()
            }
          }
          if (e.key === 'Tab') {
            // Tab também commita o valor atual (UX padrão)
            onChange(query)
            setOpen(false)
          }
          if (e.key === 'Escape') setOpen(false)
        }}
        placeholder={placeholder}
        className={`input font-mono w-full ${small ? 'text-[10px] py-0.5 px-1' : 'text-xs'} ${
          isKnownId ? 'border-green-500/50' :
          isUnknownButValid ? 'border-amber-500/50' :
          trimmed ? 'border-red-500/40' : ''
        }`}
      />
      {/* Feedback visual: status do ID */}
      {trimmed && !isKnownId && (
        <div className={`text-[9px] mt-0.5 ${isUnknownButValid ? 'text-amber-300/80' : 'text-red-300/80'}`}>
          {isUnknownButValid
            ? `⚠ ID "${trimmed}" não está no registry — será salvo como referência (item novo?)`
            : `✗ Formato inválido (use namespace:nome, ex: liberthia:meu_item)`}
        </div>
      )}
      {trimmed && isKnownId && (
        <div className="text-[9px] mt-0.5 text-green-300/80">
          ✓ Item registrado no mod
        </div>
      )}
      {dropdown && createPortal(dropdown, document.body)}
    </div>
  )
}

export function ItemIcon({ id, size = 32 }: { id: string; size?: number }) {
  if (!id) return <div style={{ width: size, height: size }} className="bg-liberthia-900/40 rounded" />
  const isVanilla = id.startsWith('minecraft:')
  const name = id.split(':').pop() ?? id
  const src = isVanilla
    ? `https://static.wikia.nocookie.net/minecraft_gamepedia/images/0/0e/Grid_${slugCase(name)}.png`
    : `/api/items/icon?id=${encodeURIComponent(id)}`
  return (
    <img
      src={src}
      alt={name}
      width={size}
      height={size}
      style={{ width: size, height: size, imageRendering: 'pixelated' }}
      onError={(e) => {
        const t = e.currentTarget as HTMLImageElement
        t.style.display = 'none'
      }}
    />
  )
}

function slugCase(s: string): string {
  return s.replace(/_/g, '_').replace(/\b\w/g, c => c.toUpperCase())
}
