import { useEffect, useRef, useState } from 'react'
import { getTesterToken } from '../store/testerAuth'

/**
 * Player de áudio com:
 *  - Play/pause
 *  - Barra de progresso (clicável pra seek)
 *  - Volume slider
 *  - Timer current/total
 *
 * Como o backend exige Authorization: Bearer header (tester token),
 * <audio src=URL> direto não funciona. Solução: fetch + blob URL.
 * Cache do blob no componente; URL é liberada no unmount.
 *
 * Suporta tanto token tester (default) quanto admin via prop `adminMode`.
 */

interface Props {
    streamUrl: string
    title?: string
    mimeType?: string
    /** Se true, usa o token admin do localStorage em vez do tester. */
    adminMode?: boolean
    onPlay?: () => void
    onEnded?: () => void
    autoPlay?: boolean
    compact?: boolean
}

function fmtTime(s: number): string {
    if (!isFinite(s) || s < 0) return '00:00'
    const m = Math.floor(s / 60)
    const sec = Math.floor(s % 60)
    return `${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`
}

export function AudioPlayer({ streamUrl, title, mimeType, adminMode, onPlay, onEnded, autoPlay, compact }: Props) {
    const audioRef = useRef<HTMLAudioElement>(null)
    const [blobUrl, setBlobUrl] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(true)
    const [playing, setPlaying] = useState(false)
    const [current, setCurrent] = useState(0)
    const [duration, setDuration] = useState(0)
    const [volume, setVolume] = useState(0.8)

    useEffect(() => {
        let cancelled = false
        let currentBlob: string | null = null
        setLoading(true)
        setError(null)

        const token = adminMode
            ? localStorage.getItem('liberthia.token')
            : getTesterToken()
        if (!token) {
            setError('Sessão expirou')
            setLoading(false)
            return
        }

        fetch(streamUrl, { headers: { Authorization: `Bearer ${token}` } })
            .then(async r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`)
                return r.blob()
            })
            .then(blob => {
                if (cancelled) return
                currentBlob = URL.createObjectURL(
                    mimeType && !blob.type ? new Blob([blob], { type: mimeType }) : blob
                )
                setBlobUrl(currentBlob)
                setLoading(false)
            })
            .catch(e => {
                if (cancelled) return
                setError(e.message ?? String(e))
                setLoading(false)
            })

        return () => {
            cancelled = true
            if (currentBlob) URL.revokeObjectURL(currentBlob)
        }
    }, [streamUrl, mimeType, adminMode])

    useEffect(() => {
        if (audioRef.current) audioRef.current.volume = volume
    }, [volume, blobUrl])

    function togglePlay() {
        const a = audioRef.current
        if (!a || !blobUrl) return
        if (a.paused) {
            a.play().catch(e => setError(e.message))
            setPlaying(true)
            onPlay?.()
        } else {
            a.pause()
            setPlaying(false)
        }
    }

    function seek(e: React.MouseEvent<HTMLDivElement>) {
        const a = audioRef.current
        if (!a || !duration) return
        const rect = e.currentTarget.getBoundingClientRect()
        const pct = (e.clientX - rect.left) / rect.width
        a.currentTime = Math.max(0, Math.min(duration, pct * duration))
        setCurrent(a.currentTime)
    }

    if (error) {
        return (
            <div className="rounded border border-red-700 bg-red-950/30 p-2 text-xs text-red-300">
                ⚠️ {error}
            </div>
        )
    }

    return (
        <div className={`rounded-lg bg-slate-800/60 border border-slate-700/50 ${compact ? 'p-1.5' : 'p-2.5'}`}>
            {blobUrl && (
                <audio
                    ref={audioRef}
                    src={blobUrl}
                    autoPlay={autoPlay}
                    onTimeUpdate={e => setCurrent(e.currentTarget.currentTime)}
                    onLoadedMetadata={e => setDuration(e.currentTarget.duration)}
                    onEnded={() => { setPlaying(false); onEnded?.() }}
                    onPlay={() => setPlaying(true)}
                    onPause={() => setPlaying(false)}
                    onError={() => setError('falha ao tocar — formato suportado?')}
                />
            )}
            <div className="flex items-center gap-2">
                <button
                    onClick={togglePlay}
                    disabled={loading || !!error}
                    className={`flex-shrink-0 rounded-full bg-purple-500 hover:bg-purple-400 text-white font-bold flex items-center justify-center transition-all disabled:opacity-50 ${
                        compact ? 'w-7 h-7 text-xs' : 'w-9 h-9 text-sm'
                    }`}
                    title={playing ? 'pausar' : 'tocar'}
                >
                    {loading ? '⌛' : playing ? '⏸' : '▶'}
                </button>

                <div className="flex-1 min-w-0">
                    {title && !compact && (
                        <div className="text-xs text-slate-200 truncate font-medium">{title}</div>
                    )}
                    <div
                        className="h-1.5 bg-slate-700 rounded-full cursor-pointer overflow-hidden mt-0.5"
                        onClick={seek}
                    >
                        <div
                            className="h-full bg-gradient-to-r from-purple-500 to-pink-500 transition-[width] duration-100"
                            style={{ width: `${duration ? (current / duration) * 100 : 0}%` }}
                        />
                    </div>
                    <div className="text-[9px] text-slate-400 mt-0.5 font-mono flex justify-between">
                        <span>{fmtTime(current)}</span>
                        <span>{fmtTime(duration)}</span>
                    </div>
                </div>

                {!compact && (
                    <div className="hidden sm:flex items-center gap-1">
                        <span className="text-xs">🔊</span>
                        <input
                            type="range"
                            min={0}
                            max={1}
                            step={0.05}
                            value={volume}
                            onChange={e => setVolume(parseFloat(e.target.value))}
                            className="w-16 accent-purple-500"
                            title={`volume: ${Math.round(volume * 100)}%`}
                        />
                    </div>
                )}
            </div>
        </div>
    )
}
