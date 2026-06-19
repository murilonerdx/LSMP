import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { modConfigApi, ModTokenTestResult } from '../lib/api'
import { toast } from '../store/toast'

/**
 * Mod Token Manager — gerencia o token compartilhado mod↔backend sem precisar
 * editar docker-compose + restart.
 *
 * Por que existe: o env var MOD_TOKEN do compose vive divergindo do token que
 * o mod gera em world/serverconfig/liberthia-server.toml. O backend agora
 * persiste o token "real" no DB (tabela backend_config) e essa página deixa o
 * operador inspecionar/atualizar isso direto. Boot do backend lê DB primeiro,
 * env var só como fallback inicial.
 */

const UUID_REGEX = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/

export function ModConfigPage() {
  const qc = useQueryClient()
  const [draftToken, setDraftToken] = useState('')
  const [pendingConfirm, setPendingConfirm] = useState(false)
  const [testResult, setTestResult] = useState<ModTokenTestResult | null>(null)
  const [testing, setTesting] = useState(false)

  const tokenQ = useQuery({
    queryKey: ['mod-config-token'],
    queryFn: modConfigApi.getToken,
    refetchInterval: 5000,
  })

  const saveMut = useMutation({
    mutationFn: modConfigApi.setToken,
    onSuccess: (r) => {
      toast.ok(`Token salvo (${r.tokenFingerprint})`)
      setDraftToken('')
      setPendingConfirm(false)
      qc.invalidateQueries({ queryKey: ['mod-config-token'] })
    },
    onError: (e: any) => {
      toast.err(e?.message || 'falha ao salvar')
      setPendingConfirm(false)
    },
  })

  async function runTest() {
    setTesting(true)
    setTestResult(null)
    try {
      const r = await modConfigApi.testConnection()
      setTestResult(r)
      if (r.ok) toast.ok('Conexão OK com o mod')
      else toast.err('Falha na conexão — veja detalhes abaixo')
    } catch (e: any) {
      setTestResult({
        ok: false,
        error: e?.message || 'erro desconhecido',
        tokenFingerprint: tokenQ.data?.tokenFingerprint ?? '(?)',
      })
      toast.err(e?.message || 'falha no teste')
    } finally {
      setTesting(false)
    }
  }

  const info = tokenQ.data
  const looksValid = draftToken.trim().length === 0 || UUID_REGEX.test(draftToken.trim())

  function handleSaveClick() {
    const tok = draftToken.trim()
    if (!UUID_REGEX.test(tok)) {
      toast.err('Token precisa ter formato UUID (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)')
      return
    }
    if (!pendingConfirm) {
      setPendingConfirm(true)
      return
    }
    saveMut.mutate(tok)
  }

  return (
    <div className="route-fade max-w-3xl">
      <header className="mb-4">
        <h1 className="page-title">🔑 Mod Token</h1>
        <p className="text-sm text-liberthia-300/70 mt-1">
          Token compartilhado entre o backend e o mod (Forge). Esta tela é a <b>ÚNICA</b> fonte de
          verdade — o auto-register do mod (POST <code className="text-purple-300/90">/api/mod/register</code> a
          cada 60 s) <b>NUNCA</b> mais sobrescreve. Salvou aqui, fica aqui. Só muda quando você editar
          de novo. Sobrevive restart do backend.
        </p>
      </header>

      <div className="flex flex-col gap-4">
        {/* === Card 1: Token Atual === */}
        <section className="card-glow">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-bold text-base flex items-center gap-2">
              <span>📋</span> Token Atual
            </h2>
            <div className="flex items-center gap-2 text-[11px] text-liberthia-300/60">
              <div className="w-2 h-2 rounded-full bg-liberthia-400 animate-pulse" />
              auto-refresh 5s
            </div>
          </div>

          {tokenQ.isLoading ? (
            <div className="text-sm text-liberthia-300/60">Carregando…</div>
          ) : tokenQ.error ? (
            <div className="text-sm text-red-300/80">
              Falha ao ler token: {(tokenQ.error as Error).message}
            </div>
          ) : info ? (
            <div className="flex flex-col gap-2 text-sm">
              <div className="flex items-center justify-between gap-3 py-2 px-3 rounded-lg bg-black/30 border border-purple-500/20 font-mono">
                <span className="text-purple-200/90 truncate">{info.tokenFingerprint}</span>
                <div className="flex items-center gap-2 shrink-0">
                  <SourceBadge source={info.source} />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[12px]">
                <Row label="Última atualização (DB)" value={fmtTs(info.lastUpdatedAt)} />
                <Row label="Último registro do mod" value={fmtTs(info.lastRegisteredAt)} />
                <Row
                  label="Override persistido?"
                  value={info.hasOverride ? 'Sim' : 'Não (usando env var)'}
                  tone={info.hasOverride ? 'ok' : 'warn'}
                />
                <Row
                  label="Token em uso = persistido?"
                  value={info.currentMatchesRegistered ? 'Sim' : 'Não'}
                  tone={info.currentMatchesRegistered ? 'ok' : 'warn'}
                />
              </div>

              <div className="mt-1 px-3 py-2 rounded-lg bg-emerald-500/10 border border-emerald-500/40 text-[12px] text-emerald-100/90">
                🔒 <b>Painel é a única fonte</b> — o auto-register do mod (a cada 60s) NUNCA
                sobrescreve. Salvou aqui, fica aqui. Só muda quando você salvar de novo.
              </div>

              {!info.hasOverride && (
                <div className="text-[11px] text-amber-300/80">
                  ⚠ Nenhum token salvo no DB ainda — backend tá usando o do env var. Assim que o mod
                  se registrar (ou você salvar aqui), ele vira a fonte de verdade.
                </div>
              )}
            </div>
          ) : null}
        </section>

        {/* === Card 2: Atualizar Token === */}
        <section className="card-glow">
          <h2 className="font-bold text-base flex items-center gap-2 mb-3">
            <span>✏️</span> Atualizar Token
          </h2>
          <p className="text-[12px] text-liberthia-300/60 mb-3">
            Cole aqui o token que o mod usa (encontre em{' '}
            <code className="text-purple-300/90">world/serverconfig/liberthia-server.toml</code>,
            chave <code className="text-purple-300/90">admin_api.token</code>).
          </p>

          <div className="flex flex-col gap-2">
            <input
              type="text"
              className={`input font-mono text-sm ${
                draftToken && !looksValid ? 'border-red-500/60' : ''
              }`}
              placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
              value={draftToken}
              onChange={(e) => {
                setDraftToken(e.target.value)
                setPendingConfirm(false)
              }}
              spellCheck={false}
              autoComplete="off"
            />
            {draftToken && !looksValid && (
              <div className="text-[11px] text-red-300/90">
                Formato inválido — esperado UUID (8-4-4-4-12 chars hex).
              </div>
            )}

            {pendingConfirm && looksValid && info && (
              <div className="px-3 py-2 rounded-lg bg-amber-500/10 border border-amber-500/40 text-[12px] text-amber-100/90 flex items-center justify-between gap-2">
                <span>
                  Vai substituir o token atual ({info.tokenFingerprint}) pelo novo. Confirmar?
                </span>
                <div className="flex gap-1">
                  <button className="btn-ghost btn-sm" onClick={() => setPendingConfirm(false)}>
                    Cancelar
                  </button>
                  <button
                    className="btn btn-sm"
                    onClick={handleSaveClick}
                    disabled={saveMut.isPending}
                  >
                    {saveMut.isPending ? 'Salvando…' : 'Confirmar'}
                  </button>
                </div>
              </div>
            )}

            {!pendingConfirm && (
              <div className="flex justify-end">
                <button
                  className="btn"
                  onClick={handleSaveClick}
                  disabled={!draftToken.trim() || !looksValid || saveMut.isPending}
                >
                  💾 Salvar
                </button>
              </div>
            )}
          </div>
        </section>

        {/* === Card 3: Testar Conexão === */}
        <section className="card-glow">
          <h2 className="font-bold text-base flex items-center gap-2 mb-3">
            <span>🔌</span> Testar Conexão
          </h2>
          <p className="text-[12px] text-liberthia-300/60 mb-3">
            Faz uma chamada real <code className="text-purple-300/90">/api/server/info</code> no mod
            usando o token atual — pula cache e circuit breaker.
          </p>

          <div className="flex items-center justify-between gap-2">
            <button className="btn" onClick={runTest} disabled={testing}>
              {testing ? 'Testando…' : '🔌 Testar agora'}
            </button>
            {testResult && (
              <span
                className={`text-[11px] font-mono px-2 py-1 rounded ${
                  testResult.ok
                    ? 'bg-emerald-500/15 text-emerald-200 border border-emerald-500/30'
                    : 'bg-red-500/15 text-red-200 border border-red-500/30'
                }`}
              >
                {testResult.tokenFingerprint}
              </span>
            )}
          </div>

          {testResult && (
            <div
              className={`mt-3 px-3 py-2 rounded-lg text-[12px] border ${
                testResult.ok
                  ? 'bg-emerald-500/10 border-emerald-500/40 text-emerald-100/90'
                  : 'bg-red-500/10 border-red-500/40 text-red-100/90'
              }`}
            >
              {testResult.ok ? (
                <div className="flex flex-col gap-1">
                  <div className="font-bold">✓ Conexão OK</div>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 mt-1 text-[11px]">
                    <span>TPS: <b>{testResult.tps?.toFixed(1) ?? '—'}</b></span>
                    <span>Players: <b>{testResult.playerCount ?? 0}/{testResult.maxPlayers ?? 0}</b></span>
                    <span className="col-span-2 truncate">MOTD: <b>{testResult.motd || '—'}</b></span>
                  </div>
                </div>
              ) : (
                <div className="flex flex-col gap-1">
                  <div className="font-bold">✗ Falha na conexão</div>
                  <div className="font-mono text-[11px] break-all">{testResult.error}</div>
                </div>
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}

function SourceBadge({ source }: { source: 'DB' | 'ENV' }) {
  if (source === 'DB') {
    return (
      <span className="text-[10px] uppercase tracking-widest px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-200 border border-emerald-500/40">
        DB · persistido
      </span>
    )
  }
  return (
    <span className="text-[10px] uppercase tracking-widest px-2 py-0.5 rounded bg-amber-500/20 text-amber-200 border border-amber-500/40">
      ENV · não-persistido
    </span>
  )
}

function Row({
  label,
  value,
  tone,
}: {
  label: string
  value: string
  tone?: 'ok' | 'warn'
}) {
  const valColor =
    tone === 'ok'
      ? 'text-emerald-300/90'
      : tone === 'warn'
      ? 'text-amber-300/90'
      : 'text-liberthia-100/90'
  return (
    <div className="flex items-center justify-between gap-3 py-1.5 px-2 rounded bg-black/20 border border-purple-500/10">
      <span className="text-liberthia-300/60 text-[11px]">{label}</span>
      <span className={`font-mono text-[11px] ${valColor}`}>{value}</span>
    </div>
  )
}

function fmtTs(iso: string | null): string {
  if (!iso) return '—'
  try {
    const d = new Date(iso)
    return d.toLocaleString('pt-BR', { hour12: false })
  } catch {
    return iso
  }
}
