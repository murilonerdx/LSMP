import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { testerApi } from '../../lib/api'

/**
 * Inscrição PÚBLICA pro processo seletivo de mod tester.
 * Sem auth, sem código — a pessoa só preenche o form e aguarda aprovação.
 *
 * Quando admin aprovar, sistema gera InviteCode que é entregue à pessoa.
 * Aí ela vai em /tester/register usar o código pra criar a conta.
 *
 * v95: checa /api/tester/config — se applicationsEnabled=false, mostra
 * tela de "inscrições fechadas" em vez do form.
 */
export function TesterApplyPage() {
  const configQ = useQuery({
    queryKey: ['tester-public-config'],
    queryFn: testerApi.publicConfig,
    refetchInterval: 60_000,
  })
  const applicationsEnabled = configQ.data?.applicationsEnabled ?? true

  const [realName, setRealName] = useState('')
  const [mcName, setMcName] = useState('')
  const [contact, setContact] = useState('')
  const [availability, setAvailability] = useState<'yes' | 'no'>('yes')
  const [motivation, setMotivation] = useState('')
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Inscrições fechadas — mostra tela específica
  if (!configQ.isLoading && !applicationsEnabled) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-purple-950 via-liberthia-900 to-purple-950 flex items-center justify-center p-4">
        <div className="w-full max-w-md card-glow text-center py-10">
          <div className="text-6xl mb-4">⛔</div>
          <h1 className="text-2xl font-bold gradient-text mb-2">Inscrições Fechadas</h1>
          <p className="text-sm text-liberthia-300/80 mb-4">
            O processo seletivo de mod testers está temporariamente fechado.
          </p>
          <p className="text-xs text-liberthia-300/60">
            Volte mais tarde, ou entre em contato no Discord pra saber quando reabrir.
          </p>
          <div className="mt-6 flex justify-center gap-3 text-xs">
            <Link to="/tester/login" className="text-purple-300 hover:underline">
              Já é tester? Entrar →
            </Link>
          </div>
        </div>
      </div>
    )
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    if (!realName.trim() || !mcName.trim() || !motivation.trim()) {
      return setError('Preencha todos os campos obrigatórios')
    }
    if (motivation.trim().length < 30) {
      return setError('Motivação muito curta — conta um pouco mais (mínimo 30 caracteres)')
    }
    setBusy(true)
    try {
      const r = await testerApi.apply({
        realName: realName.trim(),
        mcName: mcName.trim(),
        contact: contact.trim() || undefined,
        weeklyAvailability: availability === 'yes',
        motivation: motivation.trim(),
      })
      if (!r.ok) throw new Error(r.error ?? 'falha')
      setDone(true)
    } catch (e: any) {
      setError(e.message || 'erro desconhecido')
    } finally { setBusy(false) }
  }

  if (done) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-purple-950 via-liberthia-900 to-purple-950 flex items-center justify-center p-4">
        <div className="w-full max-w-md card-glow text-center py-10">
          <div className="text-6xl mb-3">✅</div>
          <h1 className="text-2xl font-bold gradient-text mb-2">Inscrição enviada!</h1>
          <p className="text-sm text-liberthia-300/80 mb-4">
            Recebemos sua inscrição. O admin vai avaliar e, se aprovado, vai te entregar um <b>código de convite</b> (pessoalmente, Discord, etc).
          </p>
          <p className="text-xs text-liberthia-300/60 mb-6">
            Quando receber o código, volta aqui em <Link to="/tester/register" className="text-purple-300 underline">/tester/register</Link> pra criar sua conta.
          </p>
          <Link to="/tester/login" className="btn">Voltar ao login</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-950 via-liberthia-900 to-purple-950 py-8 px-4">
      <div className="w-full max-w-2xl mx-auto">
        <div className="text-center mb-6">
          <div className="text-6xl mb-2">📝</div>
          <h1 className="text-3xl font-bold gradient-text">Play test do Liberthia</h1>
          <p className="text-sm text-liberthia-300/70 mt-1">Inscreva-se pra testar as próximas versões do mod</p>
        </div>

        {/* Recado oficial do admin */}
        <div className="card-glow !bg-gradient-to-br !from-purple-500/15 !to-liberthia-500/10 !border-purple-400/40 mb-4">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-full bg-purple-500/30 flex items-center justify-center text-base">👋</div>
            <div>
              <h2 className="font-bold gradient-text text-sm">Recado do admin</h2>
              <div className="text-[10px] text-liberthia-300/50">leia antes de se inscrever</div>
            </div>
          </div>
          <div className="text-xs text-liberthia-300/85 space-y-2 leading-relaxed">
            <p>
              Oi gente! 👋 Tô pensando em abrir um <b className="text-purple-300">play test</b> pras
              próximas versões do mod Liberthia. Quem quiser participar ganha:
            </p>
            <ul className="ml-2 space-y-0.5 text-liberthia-300/80">
              <li>🎁 <b>Recompensa dentro do jogo</b></li>
              <li>📦 <b>Recursos do mod Liberthia</b> em primeira mão</li>
              <li>🏷 <b>Tag de mod tester</b> (exclusiva)</li>
            </ul>
            <p>
              Nos próximos dias vou organizar isso direito. Caso sobrem vagas, abro pro pessoal em
              geral também.
            </p>
            <p className="p-2 rounded bg-amber-500/15 border border-amber-400/30 text-amber-100">
              ⚡ Por enquanto vou liberar <b>3 vagas</b>.
            </p>
            <p>
              A ideia é que vocês <b>testem os mods</b> — vai ter descrição de cada item explicando
              como funciona. Aí vocês podem ser <b>criativos no jeito de testar</b>. Vou ver se subo
              um <b>servidor exclusivo pra beta test</b> das próximas versões; passo as contas pra
              vocês e a galera reporta no site o que encontrar de bug.
            </p>
            <p>
              As <b>recompensas ainda estão sendo ajustadas</b> — vai ter <b>items</b>, <b>efeitos</b>,
              <b>moedas</b>… tô pensando em algo legal mas que <i className="text-liberthia-300/60">não seja muito
              quebrado</i> pra não dar vantagem injusta no servidor principal.
            </p>
            <p>
              Logo abro as inscrições oficiais. Quem é <b className="text-amber-300">VIP tem
              prioridade</b>; se ninguém dos VIPs quiser, abro pro pessoal geral.
            </p>
          </div>
        </div>

        {/* Benefícios em destaque */}
        <div className="card-glow !bg-purple-500/10 !border-purple-400/40 mb-4">
          <h2 className="font-bold gradient-text mb-2 text-sm">🎁 O que você ganha sendo mod tester</h2>
          <ul className="text-xs text-liberthia-300/80 space-y-1 list-disc list-inside">
            <li>🔑 <b>Acesso antecipado</b> a mods e items em beta antes do servidor oficial</li>
            <li>🌐 <b>Servidor de teste privado</b> só pra testers (quando estiver no ar)</li>
            <li>⭐ <b>Pontos</b> por cada bug confirmado (e ranking competitivo)</li>
            <li>🎁 <b>Recompensas in-game</b>: crowns, items raros, efeitos, títulos exclusivos</li>
            <li>🗳 <b>Direito a voto</b> em sugestões de novos items/blocos/artefatos</li>
            <li>💡 <b>Pode propor</b> items/mecânicas novas — se aprovadas, vão pro mod oficial</li>
            <li>🏆 <b>Crédito no changelog</b> oficial dos mods (seu nick aparece quando seu bug for corrigido)</li>
            <li>🏷 <b>Tag exclusiva</b> de mod tester</li>
          </ul>
        </div>

        {/* Como funciona (passo a passo) */}
        <div className="card mb-4 text-xs">
          <h2 className="font-bold mb-2 text-sm flex items-center gap-2">
            <span>📋</span>
            <span className="gradient-text">Como funciona o processo</span>
          </h2>
          <ol className="space-y-2 text-liberthia-300/80">
            <li className="flex gap-2">
              <span className="font-bold text-purple-300 shrink-0">1.</span>
              <span><b>Inscreva-se</b> aqui (formulário embaixo). Não precisa de código nessa fase.</span>
            </li>
            <li className="flex gap-2">
              <span className="font-bold text-purple-300 shrink-0">2.</span>
              <span>Admin <b>avalia</b> sua inscrição e prioriza VIPs. Se aprovado, recebe um <b>código de convite</b> (Discord/in-game).</span>
            </li>
            <li className="flex gap-2">
              <span className="font-bold text-purple-300 shrink-0">3.</span>
              <span>Com o código, você se <b>registra</b> em <code className="text-purple-300">/tester/register</code> e cria sua senha.</span>
            </li>
            <li className="flex gap-2">
              <span className="font-bold text-purple-300 shrink-0">4.</span>
              <span>Recebe acesso ao <b>servidor de teste</b> e às builds beta do mod. Joga, explora os items novos.</span>
            </li>
            <li className="flex gap-2">
              <span className="font-bold text-purple-300 shrink-0">5.</span>
              <span><b>Reporta bugs</b> direto no site. Cada bug confirmado pelo admin = <b>pontos</b> + crédito no changelog.</span>
            </li>
            <li className="flex gap-2">
              <span className="font-bold text-purple-300 shrink-0">6.</span>
              <span><b>Resgata recompensas</b> com seus pontos (crowns, items, tags) no painel de tester.</span>
            </li>
          </ol>
        </div>

        {/* NDA */}
        <div className="card !bg-amber-900/20 !border-amber-500/40 mb-4 text-xs">
          <div className="flex gap-2">
            <span className="text-lg">⚠</span>
            <div>
              <b className="text-amber-300">AVISO IMPORTANTE</b>
              <p className="mt-1 text-liberthia-300/85">
                Tester que <b>vazar</b> o endereço do servidor de teste, gravações, conteúdo de items
                em beta, ou screenshots não autorizados será <b>banido permanentemente</b> e perderá
                todos os pontos + recompensas acumuladas. Beta = confidencial até virar release oficial.
              </p>
            </div>
          </div>
        </div>

        <form onSubmit={submit} className="card-glow space-y-3">
          <div>
            <label className="label">Seu nome real <span className="text-red-400">*</span></label>
            <input className="input" value={realName} onChange={(e) => setRealName(e.target.value)}
              placeholder="João Silva" required maxLength={128} />
          </div>

          <div>
            <label className="label">Nick do Minecraft <span className="text-red-400">*</span></label>
            <input className="input" value={mcName} onChange={(e) => setMcName(e.target.value)}
              placeholder="JoaoMC2024" required maxLength={64} />
          </div>

          <div>
            <label className="label">Discord ou contato (opcional)</label>
            <input className="input" value={contact} onChange={(e) => setContact(e.target.value)}
              placeholder="joao#1234 ou @joaomc" maxLength={128} />
          </div>

          <div>
            <label className="label">Disponibilidade <span className="text-red-400">*</span></label>
            <div className="text-xs text-liberthia-300/70 mb-1">Você tem disponibilidade de pelo menos <b>1 vez por semana</b> pra testar?</div>
            <div className="flex gap-2">
              <label className={`flex-1 cursor-pointer rounded p-2 text-center text-xs border ${
                availability === 'yes' ? 'border-emerald-400 bg-emerald-500/20' : 'border-liberthia-600'
              }`}>
                <input type="radio" name="avail" value="yes" checked={availability === 'yes'}
                  onChange={() => setAvailability('yes')} className="sr-only" />
                ✅ Sim
              </label>
              <label className={`flex-1 cursor-pointer rounded p-2 text-center text-xs border ${
                availability === 'no' ? 'border-red-400 bg-red-500/20' : 'border-liberthia-600'
              }`}>
                <input type="radio" name="avail" value="no" checked={availability === 'no'}
                  onChange={() => setAvailability('no')} className="sr-only" />
                ❌ Não
              </label>
            </div>
            {availability === 'no' && (
              <div className="text-[10px] text-amber-300 mt-1">
                Sem problemas, mas saiba que esse é um critério importante de aprovação.
              </div>
            )}
          </div>

          <div>
            <label className="label">Por que você gostaria de se inscrever? <span className="text-red-400">*</span></label>
            <textarea className="input" rows={5} value={motivation}
              onChange={(e) => setMotivation(e.target.value)}
              placeholder="Conta sua experiência com mods, jogos de teste, ou só seu interesse em ajudar a desenvolver o Liberthia..."
              required maxLength={2000} />
            <div className="text-[10px] text-liberthia-300/50 mt-1">
              {motivation.length}/2000 · mínimo 30 caracteres
            </div>
          </div>

          {error && (
            <div className="text-xs text-red-400 bg-red-900/20 border border-red-500/30 rounded p-2">{error}</div>
          )}

          <button type="submit" className="btn w-full" disabled={busy}>
            {busy ? 'Enviando...' : '📤 Enviar inscrição'}
          </button>

          <div className="text-center text-[10px] text-liberthia-300/60 mt-2">
            Após análise, o admin entrará em contato com o seu código de acesso.
          </div>
        </form>

        <div className="text-center mt-4 text-[11px] text-liberthia-300/60">
          Já tem código?{' '}
          <Link to="/tester/register" className="text-purple-300 hover:underline">Registrar com código →</Link>
        </div>
      </div>
    </div>
  )
}
