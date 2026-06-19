import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { testerApi } from '../../lib/api'
import { setTesterAuth } from '../../store/testerAuth'

/**
 * Login do mod tester — separado do admin.
 * Acessível em /tester/login (ou /tester).
 *
 * Aceita ?expired=1 na URL pra mostrar mensagem amigável quando o token
 * expirou (forçado pelo testerReq quando recebe 401 em GETs).
 */
export function TesterLoginPage() {
  const nav = useNavigate()
  const [params] = useSearchParams()
  const [mcName, setMcName] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [errorReason, setErrorReason] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (params.get('expired') === '1') {
      setInfo('⏱ Sua sessão expirou. Faça login de novo pra continuar.')
    }
  }, [params])

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError(null); setErrorReason(null); setInfo(null); setBusy(true)
    try {
      const r = await testerApi.login({ mcName: mcName.trim(), password })
      if (!r.ok) throw new Error(r.error ?? 'falha no login')
      setTesterAuth(r.token, r.tester.mcName)
      nav('/tester/dashboard', { replace: true })
    } catch (e: any) {
      setError(e.message || 'erro desconhecido')
      setErrorReason(e.reason ?? null)
    } finally { setBusy(false) }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-950 via-liberthia-900 to-purple-950 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-6">
          <div className="text-6xl mb-2">🧪</div>
          <h1 className="text-3xl font-bold gradient-text">Mod Tester</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Área dos beta testers — diferente do painel admin.
          </p>
        </div>

        <form onSubmit={submit} className="card-glow space-y-3">
          <h2 className="font-bold mb-2">🔓 Entrar</h2>

          <div>
            <label className="label">Nome do Minecraft</label>
            <input className="input" placeholder="Steve"
              value={mcName} onChange={(e) => setMcName(e.target.value)}
              autoFocus required />
          </div>

          <div>
            <label className="label">Senha</label>
            <input className="input" type="password"
              value={password} onChange={(e) => setPassword(e.target.value)}
              required />
          </div>

          {info && (
            <div className="text-xs text-amber-300 bg-amber-900/20 border border-amber-500/30 rounded p-2">
              {info}
            </div>
          )}
          {error && (
            <div className="text-xs text-red-300 bg-red-900/20 border border-red-500/30 rounded p-3 space-y-2">
              <div className="font-bold">⚠ {error}</div>
              {/* Dicas baseadas no motivo específico */}
              {errorReason === 'account_not_found' && (
                <div className="text-[10px] text-liberthia-300/80">
                  • Confere se digitou o nick exato (case-insensitive, sem espaços)<br />
                  • Se nunca se cadastrou, <Link to="/tester/apply" className="text-emerald-300 hover:underline">inscreva-se aqui</Link>
                </div>
              )}
              {errorReason === 'wrong_password' && (
                <div className="text-[10px] text-liberthia-300/80">
                  • Tem certeza da senha? Não tem "esqueci a senha" ainda<br />
                  • Peça pro admin resetar via Discord/chat
                </div>
              )}
              {errorReason?.startsWith('account_banned') && (
                <div className="text-[10px] text-red-200">
                  Sua conta foi banida. Entra em contato com o admin pra recurso.
                </div>
              )}
              {errorReason === 'pending_claim' && (
                <div className="text-[10px] text-purple-200">
                  Use o link de claim que o admin te mandou —{' '}
                  <Link to="/tester/claim" className="underline">/tester/claim</Link>
                </div>
              )}
            </div>
          )}

          <button type="submit" className="btn w-full" disabled={busy}>
            {busy ? '...' : 'Entrar'}
          </button>

          <div className="text-center text-xs text-liberthia-300/70 mt-3 space-y-1">
            <div>
              Já tem código?{' '}
              <Link to="/tester/register" className="text-purple-300 hover:underline">
                Registrar →
              </Link>
            </div>
            <div>
              Quer ser tester?{' '}
              <Link to="/tester/apply" className="text-emerald-300 hover:underline">
                📝 Inscrever-se no processo seletivo
              </Link>
            </div>
          </div>
        </form>

        <div className="flex justify-center gap-3 mt-4 text-[10px] text-liberthia-300/50">
          <Link to="/changelog" className="hover:text-liberthia-300">📋 Changelog</Link>
          <span>·</span>
          <Link to="/roadmap" className="hover:text-liberthia-300">🗺 Roadmap</Link>
          <span>·</span>
          <Link to="/leaderboard" className="hover:text-liberthia-300">🏆 Ranking</Link>
        </div>
        <div className="text-center mt-2 text-[10px] text-liberthia-300/50">
          <Link to="/" className="hover:text-liberthia-300">← Área administrativa</Link>
        </div>
      </div>
    </div>
  )
}
