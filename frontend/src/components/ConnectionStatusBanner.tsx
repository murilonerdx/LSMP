import { useEffect, useRef, useState } from 'react'

/**
 * Banner fixo no topo que mostra status da conexão com o mod do Minecraft.
 *
 * Polling em /api/mod-status (endpoint público, sem auth) a cada 5s.
 * Quando o backend reporta `online: false`, mostra banner com:
 *   - razão da falha
 *   - countdown até próximo retry (se circuit breaker abriu)
 *   - botão pra forçar retry imediato
 *
 * Não mexe com nenhuma query do react-query — só polls esse endpoint
 * independente. Mostra/esconde de forma suave (fade).
 */

interface ModStatus {
    online: boolean
    reachable: boolean | null
    circuitOpen: boolean
    retryInSec: number
    consecutiveFails: number
    lastFailReason: string | null
    lastSuccessAt: string | null
}

const POLL_INTERVAL_OK = 30_000     // 30s quando tudo OK
const POLL_INTERVAL_DOWN = 10_000   // 10s quando offline. Era 3s mas com 3 browsers
                                    //  abertos no dashboard, o backend levava 9 req/s
                                    //  só desse polling — gerava ruído nos logs do nginx
                                    //  e ainda esmagava o circuit breaker.

/**
 * Rotas onde o banner NÃO deve aparecer.
 * O dashboard do tester é uma área "consumer-facing" — testers não precisam
 * ver alertas técnicos de infraestrutura sobre o servidor MC estar offline.
 * O frontend já degrada graciosamente nessas páginas (mostra empty state ou
 * "Mod offline" inline quando faz sentido). Esses banners ruidosos só servem
 * pro admin que precisa reagir.
 */
const HIDDEN_PATH_PREFIXES = [
    '/tester/dashboard',
    '/tester/login',
    '/tester/register',
    '/tester/apply',
    '/tester/claim',
]

function shouldHideOnCurrentPath(): boolean {
    if (typeof window === 'undefined') return false
    const path = window.location.pathname
    return HIDDEN_PATH_PREFIXES.some(p => path.startsWith(p))
}

export function ConnectionStatusBanner() {
    const [status, setStatus] = useState<ModStatus | null>(null)
    const [showSuccess, setShowSuccess] = useState(false)
    const wasOffline = useRef<boolean>(false)
    // Re-avalia se deve esconder a cada mudança de URL (navegação SPA).
    // Sem isso, abrir o site em /admin e navegar pra /tester/dashboard manteria
    // o banner visível porque o componente só checa o path no mount.
    const [hideOnPath, setHideOnPath] = useState<boolean>(shouldHideOnCurrentPath)
    useEffect(() => {
        const onNav = () => setHideOnPath(shouldHideOnCurrentPath())
        window.addEventListener('popstate', onNav)
        // React Router usa history.pushState, que não dispara popstate.
        // Hook em pushState/replaceState pra captar SPA nav.
        const origPush = window.history.pushState
        const origReplace = window.history.replaceState
        window.history.pushState = function (this: History, ...args: any[]) {
            (origPush as any).apply(this, args)
            onNav()
        } as typeof window.history.pushState
        window.history.replaceState = function (this: History, ...args: any[]) {
            (origReplace as any).apply(this, args)
            onNav()
        } as typeof window.history.replaceState
        return () => {
            window.removeEventListener('popstate', onNav)
            window.history.pushState = origPush
            window.history.replaceState = origReplace
        }
    }, [])
    // Ref pra status atual — sem isso o useEffect dependia de [status?.online],
    // que disparava re-cleanup/re-mount toda vez que o status mudava. Resultado:
    // setTimeout era cancelado e recriado, mas se houvesse 2 chamadas em paralelo
    // o segundo poll rodava em paralelo com o primeiro pendente.
    const statusRef = useRef<ModStatus | null>(null)

    useEffect(() => {
        let cancelled = false
        let timeoutId: any

        async function poll() {
            if (cancelled) return
            try {
                const r = await fetch('/api/mod-status', {
                    headers: { Accept: 'application/json' },
                    cache: 'no-store',
                })
                if (!r.ok) throw new Error(`HTTP ${r.status}`)
                const data: ModStatus = await r.json()
                if (cancelled) return

                // Detecta "voltou ao normal" — mostra banner verde por 3s
                if (wasOffline.current && data.online) {
                    setShowSuccess(true)
                    setTimeout(() => setShowSuccess(false), 3000)
                }
                wasOffline.current = !data.online
                statusRef.current = data
                setStatus(data)
            } catch {
                // Backend off — mantém status anterior (não força banner)
            } finally {
                if (!cancelled) {
                    const interval = statusRef.current?.online === false ? POLL_INTERVAL_DOWN : POLL_INTERVAL_OK
                    timeoutId = setTimeout(poll, interval)
                }
            }
        }

        poll()
        return () => {
            cancelled = true
            clearTimeout(timeoutId)
        }
        // Empty deps — usa ref pra ler status atual sem re-mount.
    }, [])

    // Esconde tudo (banner verde de sucesso E vermelho de offline) em páginas
    // de tester. Esses usuários não precisam de alertas técnicos sobre o mod.
    if (hideOnPath) return null

    // Banner verde quando voltou online
    if (showSuccess) {
        return (
            <div className="fixed top-0 left-0 right-0 z-[9999] bg-green-900/95 border-b border-green-600 text-white text-xs px-3 py-1.5 text-center shadow-lg animate-[slideDown_.3s_ease]">
                ✅ Conexão com o servidor Minecraft restaurada
            </div>
        )
    }

    // Nada ainda carregado ou está online
    if (!status || status.online) return null

    // Banner vermelho/amarelo de offline
    const showAsWarning = status.consecutiveFails < 3 // ainda tentando
    const bg = showAsWarning ? 'bg-yellow-900/95 border-yellow-600' : 'bg-red-900/95 border-red-700'
    const icon = showAsWarning ? '⚠️' : '🔴'

    return (
        <div className={`fixed top-0 left-0 right-0 z-[9999] ${bg} border-b text-white text-xs px-3 py-1.5 shadow-lg animate-[slideDown_.3s_ease]`}>
            <div className="max-w-6xl mx-auto flex items-center gap-2 justify-center flex-wrap">
                <span className="text-base">{icon}</span>
                <span className="font-bold">
                    {showAsWarning ? 'Conexão com o servidor MC instável' : 'Servidor Minecraft offline'}
                </span>
                {status.circuitOpen && status.retryInSec > 0 ? (
                    <span className="opacity-80">
                        · próxima tentativa em <b className="text-yellow-200">{status.retryInSec}s</b>
                    </span>
                ) : (
                    <span className="opacity-80">· tentando reconectar...</span>
                )}
                {status.lastFailReason && (
                    <span className="hidden md:inline opacity-60 truncate max-w-xs">
                        · {status.lastFailReason}
                    </span>
                )}
                <span className="text-[10px] opacity-70 ml-2">
                    Funcionalidades dependentes do mod podem falhar até reconectar.
                </span>
            </div>
        </div>
    )
}

