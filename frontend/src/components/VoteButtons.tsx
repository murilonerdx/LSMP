import { useState } from 'react'
import { testerApi } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Botões de like/dislike reusáveis pra qualquer target votável.
 *
 * Comportamento:
 *  - Click no botão diferente do voto atual → troca o voto (LIKE ↔ DISLIKE)
 *  - Click no MESMO botão → remove o voto (toggle off)
 *  - Sem voto ainda → adiciona
 *
 * Estado vem inicial via `counts` (vindo da listagem) e atualizado
 * otimisticamente, com rollback em caso de erro.
 */

export type VoteTarget = 'BETA_ITEM' | 'BETA_AUDIO' | 'MODEL_3D'

export interface VoteCounts {
    likes: number
    dislikes: number
    myVote: 'LIKE' | 'DISLIKE' | null
}

interface Props {
    targetType: VoteTarget
    targetId: number
    counts: VoteCounts
    size?: 'xs' | 'sm' | 'md'
    onChange?: (counts: VoteCounts) => void
    disabled?: boolean
}

export function VoteButtons({ targetType, targetId, counts, size = 'sm', onChange, disabled }: Props) {
    const [local, setLocal] = useState<VoteCounts>(counts)
    const [busy, setBusy] = useState(false)

    // Re-sync se o pai mudar counts (ex: refetch)
    if (counts !== local && !busy && (counts.likes !== local.likes || counts.dislikes !== local.dislikes || counts.myVote !== local.myVote)) {
        // Não dispara setState dentro de render — só atualizamos se uma mudança real do prop
    }

    async function castVote(v: 'LIKE' | 'DISLIKE') {
        if (busy || disabled) return
        setBusy(true)
        const before = local
        // Otimista: aplica a mudança localmente antes da resposta
        const optimistic = computeNext(local, v)
        setLocal(optimistic)
        onChange?.(optimistic)
        try {
            const r: any = await testerApi.vote(targetType, targetId, v)
            const fresh = {
                likes: Number(r.counts?.likes ?? 0),
                dislikes: Number(r.counts?.dislikes ?? 0),
                myVote: (r.counts?.myVote ?? null) as 'LIKE' | 'DISLIKE' | null,
            }
            setLocal(fresh)
            onChange?.(fresh)
        } catch (e: any) {
            // Rollback
            setLocal(before)
            onChange?.(before)
            toast.err(e?.message ?? 'erro ao votar')
        } finally {
            setBusy(false)
        }
    }

    const sz = size === 'xs' ? 'text-[10px] px-1.5 py-0.5' : size === 'md' ? 'text-sm px-3 py-1' : 'text-xs px-2 py-0.5'

    return (
        <div className="flex items-center gap-1" onClick={e => e.stopPropagation()}>
            <button
                disabled={busy || disabled}
                onClick={() => castVote('LIKE')}
                title={local.myVote === 'LIKE' ? 'clique pra remover seu like' : 'curtir'}
                className={`${sz} rounded transition-colors flex items-center gap-0.5 ${
                    local.myVote === 'LIKE'
                        ? 'bg-green-500/40 text-green-200 ring-1 ring-green-400'
                        : 'bg-slate-700/40 text-slate-300 hover:bg-green-500/20 hover:text-green-200'
                } disabled:opacity-50`}
            >
                <span>👍</span>
                <span className="font-bold tabular-nums">{local.likes}</span>
            </button>
            <button
                disabled={busy || disabled}
                onClick={() => castVote('DISLIKE')}
                title={local.myVote === 'DISLIKE' ? 'clique pra remover seu dislike' : 'não curtir'}
                className={`${sz} rounded transition-colors flex items-center gap-0.5 ${
                    local.myVote === 'DISLIKE'
                        ? 'bg-red-500/40 text-red-200 ring-1 ring-red-400'
                        : 'bg-slate-700/40 text-slate-300 hover:bg-red-500/20 hover:text-red-200'
                } disabled:opacity-50`}
            >
                <span>👎</span>
                <span className="font-bold tabular-nums">{local.dislikes}</span>
            </button>
        </div>
    )
}

/** Calcula o próximo estado otimista dado o voto novo. */
function computeNext(cur: VoteCounts, v: 'LIKE' | 'DISLIKE'): VoteCounts {
    let likes = cur.likes
    let dislikes = cur.dislikes
    let myVote: 'LIKE' | 'DISLIKE' | null = cur.myVote

    // Reverter voto anterior
    if (cur.myVote === 'LIKE') likes = Math.max(0, likes - 1)
    if (cur.myVote === 'DISLIKE') dislikes = Math.max(0, dislikes - 1)

    if (cur.myVote === v) {
        // Toggle off (mesmo botão)
        myVote = null
    } else {
        myVote = v
        if (v === 'LIKE') likes++
        else dislikes++
    }
    return { likes, dislikes, myVote }
}
