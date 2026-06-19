import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ItemStack } from '../lib/api'

type Props = { uuid: string }

export function InventoryGrid({ uuid }: Props) {
  const qc = useQueryClient()
  const q = useQuery({
    queryKey: ['inventory', uuid],
    queryFn: () => api.inventory(uuid),
    refetchInterval: 3000,
  })

  if (q.isLoading) return <div className="text-liberthia-300">Carregando inventário...</div>
  if (q.error) return <div className="text-red-400">Erro: {(q.error as Error).message}</div>
  const inv = q.data!

  const main = inv.main ?? []
  const armor = inv.armor ?? []

  const onRemove = async (slot: number) => {
    if (!confirm(`Remover slot ${slot}?`)) return
    await api.remove(uuid, slot, 64)
    qc.invalidateQueries({ queryKey: ['inventory', uuid] })
  }
  const onClear = async () => {
    if (!confirm('Limpar inventário inteiro?')) return
    await api.clearInv(uuid)
    qc.invalidateQueries({ queryKey: ['inventory', uuid] })
  }

  return (
    <div className="card">
      <div className="flex justify-between items-center mb-3">
        <h2 className="text-lg font-bold">🎒 Inventário de {inv.name}</h2>
        <button onClick={onClear} className="btn-danger">Limpar Tudo</button>
      </div>

      <div className="space-y-3">
        <Section title="Armadura">
          <div className="grid grid-cols-4 gap-1">
            {armor.map((it) => <Slot key={'a' + it.slot} it={it} onClick={() => onRemove(36 + (it.slot ?? 0))} />)}
          </div>
        </Section>

        <Section title="Offhand">
          <Slot it={inv.offhand} onClick={() => onRemove(40)} />
        </Section>

        <Section title="Mochila (3×9)">
          <div className="grid grid-cols-9 gap-1">
            {main.slice(9, 36).map((it) => <Slot key={it.slot} it={it} onClick={() => onRemove(it.slot!)} />)}
          </div>
        </Section>

        <Section title="Hotbar">
          <div className="grid grid-cols-9 gap-1">
            {main.slice(0, 9).map((it) => <Slot key={it.slot} it={it} onClick={() => onRemove(it.slot!)} />)}
          </div>
        </Section>
      </div>
    </div>
  )
}

function Section({ title, children }: { title: string; children: any }) {
  return (
    <div>
      <div className="label mb-1">{title}</div>
      {children}
    </div>
  )
}

function Slot({ it, onClick }: { it: ItemStack; onClick: () => void }) {
  const empty = it.empty || !it.id
  return (
    <button
      onClick={empty ? undefined : onClick}
      title={empty ? 'vazio' : `${it.name} ×${it.count}\n${it.id}\n${it.enchantments?.map(e => `${e.id} ${e.level}`).join('\n') ?? ''}`}
      className={`w-12 h-12 rounded border ${
        empty ? 'border-liberthia-700 bg-liberthia-900' : 'border-liberthia-500 bg-liberthia-700 hover:bg-red-700/40'
      } flex items-center justify-center text-xs cursor-pointer relative`}
    >
      {!empty && (
        <>
          <span className="opacity-80 truncate px-1">{shortName(it.name ?? it.id ?? '')}</span>
          {(it.count ?? 1) > 1 && (
            <span className="absolute bottom-0 right-1 text-xs font-bold text-yellow-300">{it.count}</span>
          )}
          {it.enchantments && it.enchantments.length > 0 && (
            <span className="absolute top-0 left-0 text-[8px] text-purple-300">✦</span>
          )}
        </>
      )}
    </button>
  )
}

function shortName(s: string): string {
  const n = s.split(':').pop() ?? s
  if (n.length > 6) return n.slice(0, 5) + '…'
  return n
}
