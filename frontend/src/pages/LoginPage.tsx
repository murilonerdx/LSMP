import { useState } from 'react'
import { useAuth } from '../store/auth'

export function LoginPage() {
  const [pwd, setPwd] = useState('')
  const login = useAuth((s) => s.login)
  const loading = useAuth((s) => s.loading)
  const error = useAuth((s) => s.error)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    await login(pwd)
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-6 relative">
      {/* Animated mesh backdrop */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -left-40 w-96 h-96 rounded-full blur-[120px] opacity-40"
             style={{ background: 'radial-gradient(circle, #aa40e8 0%, transparent 70%)' }} />
        <div className="absolute -bottom-40 -right-40 w-[500px] h-[500px] rounded-full blur-[140px] opacity-30"
             style={{ background: 'radial-gradient(circle, #06b6d4 0%, transparent 70%)' }} />
      </div>

      <form onSubmit={submit} className="card-glow w-full max-w-md relative z-10">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-liberthia-400 to-liberthia-600 flex items-center justify-center text-2xl shadow-lg shadow-liberthia-500/40">⚛</div>
          <div>
            <div className="text-2xl font-black gradient-text">Liberthia</div>
            <div className="text-xs uppercase tracking-widest text-liberthia-300/70">Admin Panel</div>
          </div>
        </div>

        <div className="mb-4">
          <label className="label block mb-2">Senha do servidor</label>
          <input
            type="password"
            className="input"
            placeholder="••••••••"
            value={pwd}
            onChange={(e) => setPwd(e.target.value)}
            autoFocus
          />
          <div className="text-[10px] text-liberthia-300/50 mt-2 leading-relaxed">
            Definida em <code className="bg-black/40 px-1 rounded">backend/src/main/resources/application.yml</code>{' '}
            via <code className="bg-black/40 px-1 rounded">admin.web.password</code> ou env var{' '}
            <code className="bg-black/40 px-1 rounded">LIBERTHIA_PASSWORD</code>.
          </div>
        </div>

        {error && (
          <div className="mb-4 px-3 py-2 rounded-lg bg-red-500/15 border border-red-400/30 text-red-200 text-sm">
            ✗ {error}
          </div>
        )}

        <button type="submit" className="btn w-full" disabled={loading || !pwd}>
          {loading ? '⏳ Entrando...' : '→ Entrar'}
        </button>

        <div className="mt-6 pt-4 border-t border-liberthia-500/20 text-[10px] text-liberthia-300/50 leading-relaxed">
          Token expira em 7 dias. Cada request é assinada via HMAC-SHA256.
          Em produção, defina <code className="bg-black/40 px-1 rounded">LIBERTHIA_SECRET</code> pra rotação.
        </div>
      </form>
    </div>
  )
}
