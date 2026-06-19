import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { testerApi } from '../../lib/api'
import { setTesterAuth } from '../../store/testerAuth'

/**
 * Registro do tester — fluxo 2 etapas:
 *  1. Player digita nome MC + código de convite
 *  2. Se OK, exibe campo de senha + confirmação → cria conta
 *
 * Como o backend valida tudo numa só chamada, a "etapa 1" é apenas visual
 * (avança o form). A validação real do código acontece no POST final.
 */
export function TesterRegisterPage() {
  const nav = useNavigate()
  const [step, setStep] = useState<1 | 2>(1)
  const [mcName, setMcName] = useState('')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPwd, setConfirmPwd] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  function nextStep(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (!mcName.trim()) return setError('Nome MC obrigatório')
    if (!code.trim()) return setError('Código obrigatório')
    if (code.trim().length !== 8) return setError('Código tem 8 caracteres')
    setStep(2)
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (password.length < 4) return setError('Senha mínima de 4 caracteres')
    if (password !== confirmPwd) return setError('Senhas não conferem')
    setBusy(true)
    try {
      const r = await testerApi.register({
        mcName: mcName.trim(),
        code: code.trim().toUpperCase(),
        password,
      })
      if (!r.ok) throw new Error(r.error ?? 'falha no registro')
      setTesterAuth(r.token, r.tester.mcName)
      nav('/tester/dashboard', { replace: true })
    } catch (e: any) {
      setError(e.message || 'erro desconhecido')
      // Se erro foi de código, volta pra etapa 1
      if (e.message?.includes('código')) setStep(1)
    } finally { setBusy(false) }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-950 via-liberthia-900 to-purple-950 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-6">
          <div className="text-6xl mb-2">🎫</div>
          <h1 className="text-3xl font-bold gradient-text">Cadastro Tester</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">
            Precisa de código fornecido pelo admin.
          </p>
        </div>

        {/* Progress dots */}
        <div className="flex justify-center gap-2 mb-4">
          <span className={`w-2 h-2 rounded-full ${step === 1 ? 'bg-purple-400' : 'bg-emerald-400'}`} />
          <span className={`w-2 h-2 rounded-full ${step === 2 ? 'bg-purple-400' : 'bg-liberthia-700'}`} />
        </div>

        {step === 1 ? (
          <form onSubmit={nextStep} className="card-glow space-y-3">
            <h2 className="font-bold">📝 Identificação</h2>

            <div>
              <label className="label">Nome do Minecraft</label>
              <input className="input" placeholder="Steve"
                value={mcName} onChange={(e) => setMcName(e.target.value)}
                autoFocus required />
              <div className="text-[10px] text-liberthia-300/50 mt-1">
                Use o nome exato da sua conta MC (case-insensitive).
              </div>
            </div>

            <div>
              <label className="label">Código de Convite</label>
              <input className="input font-mono text-center text-lg tracking-widest uppercase"
                placeholder="XXXXXXXX" maxLength={8}
                value={code} onChange={(e) => setCode(e.target.value.toUpperCase())}
                required />
              <div className="text-[10px] text-liberthia-300/50 mt-1">
                Código de 8 caracteres fornecido pelo admin.
              </div>
            </div>

            {error && (
              <div className="text-xs text-red-400 bg-red-900/20 border border-red-500/30 rounded p-2">
                {error}
              </div>
            )}

            <button type="submit" className="btn w-full">Avançar →</button>

            <div className="text-center text-xs text-liberthia-300/70 mt-3">
              Já tem conta?{' '}
              <Link to="/tester/login" className="text-purple-300 hover:underline">
                Entrar →
              </Link>
            </div>
          </form>
        ) : (
          <form onSubmit={submit} className="card-glow space-y-3">
            <h2 className="font-bold">🔐 Defina sua senha</h2>

            <div className="text-xs text-liberthia-300/70 bg-liberthia-900/40 rounded p-2">
              Nome: <b className="text-purple-300">{mcName}</b><br />
              Código: <b className="font-mono">{code}</b>
            </div>

            <div>
              <label className="label">Senha</label>
              <input className="input" type="password"
                value={password} onChange={(e) => setPassword(e.target.value)}
                autoFocus required minLength={4} />
            </div>

            <div>
              <label className="label">Confirmar senha</label>
              <input className="input" type="password"
                value={confirmPwd} onChange={(e) => setConfirmPwd(e.target.value)}
                required />
            </div>

            {error && (
              <div className="text-xs text-red-400 bg-red-900/20 border border-red-500/30 rounded p-2">
                {error}
              </div>
            )}

            <div className="flex gap-2">
              <button type="button" className="btn-ghost flex-1"
                onClick={() => setStep(1)} disabled={busy}>← Voltar</button>
              <button type="submit" className="btn flex-1" disabled={busy}>
                {busy ? '...' : 'Criar conta'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
