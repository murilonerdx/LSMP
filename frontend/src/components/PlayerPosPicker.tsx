import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, Player } from '../lib/api'

/**
 * Mini picker: dropdown com players online. Quando seleciona, dispara `onPick`
 * com posição + dim + nome + uuid do player.
 *
 * Use em pages que precisam de coordenadas pra "copiar do player X que está
 * online agora".
 *
 *   <PlayerPosPicker onPick={(p) => setForm({...form, x: Math.floor(p.x), ...})} />
 */

type Picked = {
  uuid: string
  name: string
  x: number
  y: number
  z: number
  yaw: number
  dim: string  // sem o "minecraft:" prefix
}

type Props = {
  onPick: (p: Picked) => void
  label?: string  // texto do select (default "📍 Pegar pos do player")
  size?: 'sm' | 'md'
  className?: string
}

export function PlayerPosPicker({ onPick, label = '📍 Pos do player...', size = 'sm', className }: Props) {
  const [value, setValue] = useState('')
  const q = useQuery({ queryKey: ['players'], queryFn: api.players, refetchInterval: 3000 })
  const players: Player[] = q.data ?? []

  function handle(uuid: string) {
    if (!uuid) return
    const p = players.find((x) => x.uuid === uuid)
    if (!p) return
    onPick({
      uuid: p.uuid,
      name: p.name,
      x: p.position.x,
      y: p.position.y,
      z: p.position.z,
      yaw: p.position.yaw,
      dim: p.dimension.replace('minecraft:', ''),
    })
    setValue('')  // reseta select
  }

  const cls = className ?? (size === 'sm' ? 'input text-xs' : 'input')

  return (
    <select className={cls} value={value} onChange={(e) => handle(e.target.value)} disabled={players.length === 0}>
      <option value="">{players.length === 0 ? '(sem players online)' : label}</option>
      {players.map((p) => (
        <option key={p.uuid} value={p.uuid}>
          📍 {p.name} ({p.position.x.toFixed(0)},{p.position.y.toFixed(0)},{p.position.z.toFixed(0)})
        </option>
      ))}
    </select>
  )
}
