import { useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { setTesterAuth } from '../../store/testerAuth'

/**
 * Tela de "claim" — destino do link que o admin gera via /api/admin/tester/auto-create.
 * O tester chega aqui via `?code=ABC12345`, só precisa definir senha pra ativar
 * a conta. Não escolhe mcName (já foi escolhido pelo admin), não precisa de
 * outro código.
 */
export function TesterClaimPage() {
  const nav = useNavigate()
  const [params] = useSearchParams()
  const initialCode = (params.get('code') ?? '').toUpperCase()

  const [code, setCode] = useState(initialCode)
  const [password, setPassword] = useState('')
  const [confirmPwd, setConfirmPwd] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (!code.trim()) return setError('Código de claim obrigatório (vem no link)')
    if (password.length < 4) return setError('Senha mínima de 4 caracteres')
    if (password !== confirmPwd) return setError('Senhas não conferem')
    setBusy(true)
    try {
      const r = await fetch('/api/tester/auth/claim', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code: code.trim(), password })
      })
      const j = await r.json()
      if (!j.ok) throw new Error(j.error ?? 'falha')
      setTesterAuth(j.token, j.tester)
      nav('/tester/dashboard', { replace: true })
    } catch (e: any) {
      setError(e.message ?? 'erro')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-8
                    bg-gradient-to-br from-purple-950 via-liberthia-900 to-purple-950">
      <div className="w-full max-w-md">
        <div className="text-center mb-4">
          <div className="text-5xl mb-2">🔑</div>
          <h1 className="text-2xl font-bold gradient-text">Ativar Conta Tester</h1>
          <p className="text-xs text-liberthia-300/70 mt-2">
            Sua conta já foi criada pelo admin. Defina uma senha pra começar a usar.
          </p>
        </div>

        <form onSubmit={submit} className="card-glow space-y-3">
          <div>
            <label className="label text-[10px]">Código de claim (do link)</label>
            <input
              className="input font-mono text-sm tracking-wider"
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase().slice(0, 8))}
              placeholder="ABC12345"
              maxLength={8}
              autoFocus={!initialCode}
              required
            />
            <div className="text-[9px] text-liberthia-300/40 mt-1">
              Esse código veio no link que o admin te mandou
            </div>
          </div>

          <div>
            <label className="label text-[10px]">Defina sua senha</label>
            <input
              className="input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="mínimo 4 caracteres"
              minLength={4}
              autoFocus={!!initialCode}
              required
            />
          </div>

          <div>
            <label className="label text-[10px]">Confirme a senha</label>
            <input
              className="input"
              type="password"
              value={confirmPwd}
              onChange={(e) => setConfirmPwd(e.target.value)}
              placeholder="repita a senha"
              minLength={4}
              required
            />
          </div>

          {error && (
            <div className="rounded p-2 text-xs bg-red-900/30 border border-red-500/40 text-red-200">
              ⚠ {error}
            </div>
          )}

          <button type="submit" className="btn w-full" disabled={busy}>
            {busy ? '...' : '✓ Ativar conta'}
          </button>

          <div className="text-center text-[10px] text-liberthia-300/50 pt-2 border-t border-purple-500/20">
            Já tem conta ativa? <Link to="/tester/login" className="text-purple-300 hover:underline">Entrar</Link>
          </div>
        </form>
      </div>
    </div>
  )
}
