import { useRef, useState, useMemo, useEffect } from 'react'
import { toast } from '../store/toast'
import { testerApi } from '../lib/api'

/**
 * Botão reutilizável de "Importar JSON" pros painéis admin.
 *
 * Aceita arquivo via input[type=file] OU paste de JSON em textarea.
 * Chama o endpoint passado e mostra resultado (created/updated/errors).
 *
 * Workflow:
 *   1. Admin gera JSON local com `python tools/generate-items-update.py`
 *      OU pede pra IA gerar o JSON correspondente.
 *   2. Clica "Importar JSON" → modal abre.
 *   3. Upload arquivo OU cola JSON direto.
 *   4. Click "Importar" → backend faz upsert.
 *   5. Modal mostra summary {created, updated, errors[]}.
 */
export type BulkImportButtonProps = {
  /** Função que envia o payload pro backend. */
  importer: (payload: any) => Promise<{
    ok: boolean
    created: number
    updated: number
    errors: number
    errorMessages: string[]
  }>
  /** Callback chamado depois de sucesso (pra invalidar query e atualizar lista). */
  onSuccess?: () => void
  /** Label do botão. Default "Importar JSON". */
  label?: string
  /** Hint do nome de arquivo esperado (ex: "beta-items-update.json"). */
  hint?: string
}

/**
 * Estrutura de um bug dentro do array `bugs` do changelog JSON (schema v2).
 * Cada entry de changelog pode ter um array `bugs` opcional com bugs estruturados,
 * permitindo o usuário EDITAR quem reportou cada um na UI antes de importar.
 * O array é convertido em strings (credits, bugsFixed) antes de mandar pro backend.
 */
type BugEntry = {
  id?: number
  title: string
  severity?: string
  reporterMcName: string
  pointsAwarded?: number
  itemId?: string
  description?: string
  status?: 'fixed' | 'pending'
  fixDetails?: string
  fixedIn?: string
}

export function BulkImportButton({ importer, onSuccess, label = 'Importar JSON', hint }: BulkImportButtonProps) {
  const [open, setOpen] = useState(false)
  const [jsonText, setJsonText] = useState('')
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState<{
    created: number; updated: number; errors: number; errorMessages: string[]
  } | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)
  /**
   * Bugs editáveis extraídos do JSON (se for um changelog com schema v2 com
   * array `bugs`). Cada um pode ter o `reporterMcName` editado via dropdown
   * antes do import. Estado separado pra não re-parsear o JSON inteiro toda
   * vez que o usuário muda um nome.
   */
  const [editableBugs, setEditableBugs] = useState<BugEntry[] | null>(null)
  /** Index da entry que tem os bugs (geralmente 0, mas pode ter várias entries). */
  const [entryIndex, setEntryIndex] = useState<number>(0)
  /** Lista de mcNames dos testers cadastrados, pra popular o dropdown. */
  const [testerNames, setTesterNames] = useState<string[]>([])

  // Carrega lista de testers só uma vez quando o modal abre, e SÓ se a UI de
  // edição de bugs for ativar (changelog import). Pra outros tipos de import
  // (items, rewards, etc) não precisa.
  useEffect(() => {
    if (!open || !editableBugs) return
    if (testerNames.length > 0) return  // já carregou
    testerApi.adminListTesters().then(r => {
      const names = (r?.testers ?? []).map((t: any) => t.mcName).filter(Boolean)
      setTesterNames(names)
    }).catch(() => {
      // se falhar (e.g. sem permissão), o dropdown vira só free-text com
      // os nomes que já estavam no JSON original — ainda usável.
      setTesterNames([])
    })
  }, [open, editableBugs, testerNames.length])

  function reset() {
    setJsonText('')
    setResult(null)
    setEditableBugs(null)
    setEntryIndex(0)
    if (fileRef.current) fileRef.current.value = ''
  }

  function close() {
    setOpen(false)
    setTimeout(reset, 200)
  }

  /**
   * Tenta detectar se o JSON tem o schema v2 de changelog (com array `bugs`
   * estruturado dentro de cada entry). Se sim, extrai pra `editableBugs` pra
   * permitir o admin editar cada reporter na UI antes de importar.
   *
   * Retorna o índice da entry que tem bugs (ou -1 se nenhuma tem).
   */
  function detectAndLoadBugs(parsed: any): number {
    if (!parsed?.entries || !Array.isArray(parsed.entries)) return -1
    for (let i = 0; i < parsed.entries.length; i++) {
      const e = parsed.entries[i]
      if (Array.isArray(e.bugs) && e.bugs.length > 0) {
        // Faz uma cópia editável (não muta o parsed original)
        setEditableBugs(e.bugs.map((b: BugEntry) => ({ ...b })))
        setEntryIndex(i)
        return i
      }
    }
    setEditableBugs(null)
    return -1
  }

  async function onFile(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0]
    if (!f) return
    try {
      const text = await f.text()
      setJsonText(text)
      // Valida que é JSON parseavel logo na seleção pra dar feedback rápido
      const parsed = JSON.parse(text)
      const bugsIdx = detectAndLoadBugs(parsed)
      const bugsMsg = bugsIdx >= 0 ? ` · 🐛 ${parsed.entries[bugsIdx].bugs.length} bugs editáveis` : ''
      toast.ok(`📂 ${f.name} carregado (${(f.size / 1024).toFixed(1)} KB)${bugsMsg}`)
    } catch (err: any) {
      toast.err(`JSON inválido: ${err.message}`)
    }
  }

  /**
   * Quando o usuário cola/edita o JSON manualmente no textarea, re-detecta o
   * schema. Debounced via useEffect-like — mas como onChange é freq, fazemos
   * só na hora do parse explícito (no clique de Importar OU quando muda o
   * textarea — aqui).
   */
  function onJsonTextChange(text: string) {
    setJsonText(text)
    // Tenta detectar bugs sem alarmar com toast a cada keystroke
    try {
      const parsed = JSON.parse(text)
      detectAndLoadBugs(parsed)
    } catch {
      setEditableBugs(null)
    }
  }

  /**
   * Reescreve um campo de um bug específico no array editável.
   * Imutável: cria array novo pra trigger re-render.
   */
  function updateBug(idx: number, patch: Partial<BugEntry>) {
    setEditableBugs(prev => {
      if (!prev) return prev
      const copy = [...prev]
      copy[idx] = { ...copy[idx], ...patch }
      return copy
    })
  }

  /**
   * Reconstrói as strings legadas (credits, bugsFixed) a partir do array
   * `bugs` (possivelmente editado). Backend só conhece esses campos string —
   * o array `bugs` em si é stripado antes do envio.
   */
  function buildCreditsAndBugsFixed(bugs: BugEntry[]): { credits: string; bugsFixed: string } {
    // Sumário por reporter — quem ganhou quantos pontos
    const sumByReporter: Record<string, { count: number; points: number; ids: number[] }> = {}
    for (const b of bugs) {
      const name = (b.reporterMcName || 'anônimo').trim()
      if (!sumByReporter[name]) sumByReporter[name] = { count: 0, points: 0, ids: [] }
      sumByReporter[name].count++
      sumByReporter[name].points += b.pointsAwarded ?? 0
      if (b.id != null) sumByReporter[name].ids.push(b.id)
    }
    const creditsParts = Object.entries(sumByReporter)
      .sort((a, b) => b[1].count - a[1].count)
      .map(([name, s]) => `${name} (${s.count} bug${s.count > 1 ? 's' : ''}, ${s.points}pts, ids: ${s.ids.join(',')})`)
    const credits = `🏆 ${creditsParts.join(' | ')}`

    // Lista de bugs corrigidos em formato compacto
    const bugsFixedParts = bugs
      .filter(b => b.status === 'fixed')
      .map(b => {
        const sev = b.severity ? `[${b.severity}]` : ''
        const idStr = b.id != null ? `#${b.id} ` : ''
        const reporter = b.reporterMcName ? ` (por ${b.reporterMcName}` + (b.pointsAwarded ? ` +${b.pointsAwarded}pts)` : ')') : ''
        return `${idStr}${sev} ${b.title}${reporter}`.trim()
      })
    const bugsFixed = bugsFixedParts.join(' • ')

    return { credits, bugsFixed }
  }

  async function doImport() {
    if (!jsonText.trim()) {
      toast.err('Cole ou faça upload do JSON primeiro')
      return
    }
    let payload: any
    try {
      payload = JSON.parse(jsonText)
    } catch (e: any) {
      toast.err(`JSON inválido: ${e.message}`)
      return
    }
    // Se temos bugs editáveis, aplica as edições do user no payload antes do
    // envio. Reconstrói os campos legados (credits, bugsFixed) a partir do
    // array editado E TAMBÉM envia o array estruturado como `bugs` (backend
    // agora aceita: v0.1.14 backend converte pra bugsJson no entity).
    // Mantém compat: backend antigo ignora `bugs` campo desconhecido.
    if (editableBugs && payload?.entries?.[entryIndex]) {
      const entry = payload.entries[entryIndex]
      const { credits, bugsFixed } = buildCreditsAndBugsFixed(editableBugs)
      entry.credits = credits
      entry.bugsFixed = bugsFixed
      // Manda o array estruturado tb — backend v0.1.14+ persiste em bugsJson
      // pra modal de detalhe renderizar com reporter/severidade/screenshot.
      entry.bugs = editableBugs
      // suggestions: passa direto se vier no JSON (sem edição inline por enquanto)
      // Backend converte pra suggestionsJson automaticamente.
    }
    setBusy(true)
    try {
      const r = await importer(payload)
      setResult({
        created: r.created,
        updated: r.updated,
        errors: r.errors,
        errorMessages: r.errorMessages ?? [],
      })
      if (r.errors === 0) {
        toast.ok(`✅ ${r.created} criados, ${r.updated} atualizados`)
      } else {
        toast.err(`⚠ ${r.errors} erros — ${r.created + r.updated} processados`)
      }
      if (onSuccess) onSuccess()
    } catch (e: any) {
      toast.err(`Falha no import: ${e.message}`)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="btn-ghost btn-sm text-[10px]"
        title="Importa JSON gerado pela IA (upsert por chave natural)"
      >
        📥 {label}
      </button>

      {open && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/70 backdrop-blur-sm p-4"
             onClick={close}>
          <div className="card-glow max-w-2xl w-full max-h-[90vh] overflow-y-auto"
               onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold gradient-text">📥 {label}</h3>
              <button className="btn-ghost btn-sm" onClick={close}>✕</button>
            </div>

            <div className="text-[10px] text-liberthia-300/70 mb-2 space-y-1">
              <p>
                <strong>Como gerar:</strong> peça pra IA "atualizar o JSON" depois de alterar items/blocos/features no mod,
                ou rode <code className="font-mono bg-liberthia-900/60 px-1 rounded">python tools/generate-items-update.py</code>.
              </p>
              {hint && (
                <p>Arquivo esperado: <code className="font-mono bg-liberthia-900/60 px-1 rounded">{hint}</code></p>
              )}
              <p className="text-amber-300/70">
                ⚠ Upsert por chave natural: itens existentes são <strong>atualizados</strong>, não duplicados.
                Itens removidos do JSON <strong>NÃO são deletados</strong> (preserva histórico).
              </p>
            </div>

            <div className="space-y-2">
              <div>
                <label className="label text-[10px]">📂 Upload arquivo .json</label>
                <input ref={fileRef} type="file" accept=".json,application/json"
                       onChange={onFile}
                       className="input text-xs file:mr-2 file:px-2 file:py-1 file:bg-purple-500/30 file:rounded file:border-0 file:text-xs file:cursor-pointer" />
              </div>

              <div className="text-center text-[10px] text-liberthia-300/40">— ou cole o JSON abaixo —</div>

              <div>
                <label className="label text-[10px]">📋 Conteúdo do JSON</label>
                <textarea
                  className="input font-mono text-[10px]"
                  rows={12}
                  placeholder={'{\n  "version": "0.1.12",\n  "items": [...]\n}'}
                  value={jsonText}
                  onChange={(e) => onJsonTextChange(e.target.value)}
                />
              </div>
            </div>

            {/* Preview editável de bugs (só aparece se o JSON tem schema v2
                com array `bugs` numa das entries). Permite admin reassinar
                quem reportou cada bug antes de importar — útil quando o
                generator do JSON cuspiu um nome errado ou faltou anotar
                quem reportou. */}
            {editableBugs && editableBugs.length > 0 && (
              <div className="mt-3 p-3 rounded bg-purple-500/10 border border-purple-400/30">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="font-bold text-xs text-purple-300">
                    🐛 {editableBugs.length} bugs detectados — edite quem reportou se necessário
                  </h4>
                  <span className="text-[9px] text-liberthia-300/50">
                    {editableBugs.filter(b => b.status === 'fixed').length} fixed ·{' '}
                    {editableBugs.filter(b => b.status === 'pending').length} pending
                  </span>
                </div>
                <div className="text-[10px] text-liberthia-300/70 mb-2">
                  Os campos <code className="bg-liberthia-900/60 px-1 rounded">credits</code> e
                  {' '}<code className="bg-liberthia-900/60 px-1 rounded">bugsFixed</code> são
                  RECALCULADOS a partir dessa lista no momento do Importar.
                </div>
                <div className="space-y-1.5 max-h-64 overflow-y-auto">
                  {editableBugs.map((b, idx) => (
                    <div key={idx} className="rounded bg-liberthia-900/40 p-2 text-[11px]">
                      <div className="flex items-baseline gap-1.5 flex-wrap mb-1">
                        {b.id != null && (
                          <span className="font-mono text-[9px] text-liberthia-300/50">#{b.id}</span>
                        )}
                        <span className={`badge text-[8px] ${
                          b.status === 'fixed' ? 'badge-green' : 'badge-yellow'
                        }`}>{b.status ?? 'fixed'}</span>
                        {b.severity && <span className="badge text-[8px]">{b.severity}</span>}
                        {b.pointsAwarded != null && (
                          <span className="text-purple-300 text-[9px]">+{b.pointsAwarded}pts</span>
                        )}
                      </div>
                      <div className="font-bold mb-1">{b.title}</div>
                      <div className="flex items-center gap-1.5">
                        <label className="text-[9px] text-liberthia-300/60 shrink-0">Reporter:</label>
                        {/* Select com testers cadastrados + opção "Outro" pra free-text */}
                        <select
                          className="input text-[10px] py-0.5 flex-1"
                          value={
                            testerNames.includes(b.reporterMcName) ? b.reporterMcName : '__other__'
                          }
                          onChange={(e) => {
                            const v = e.target.value
                            if (v !== '__other__') {
                              updateBug(idx, { reporterMcName: v })
                            }
                          }}>
                          {testerNames.map(n => (
                            <option key={n} value={n}>{n}</option>
                          ))}
                          {!testerNames.includes(b.reporterMcName) && b.reporterMcName && (
                            <option value={b.reporterMcName}>{b.reporterMcName} (não cadastrado)</option>
                          )}
                          <option value="__other__">— outro —</option>
                        </select>
                        <input
                          type="text"
                          className="input text-[10px] py-0.5 w-32"
                          placeholder="Free text MC name"
                          value={b.reporterMcName}
                          onChange={(e) => updateBug(idx, { reporterMcName: e.target.value })}
                          title="Digita o nome MC livre — sobrescreve o select" />
                        {b.pointsAwarded != null && (
                          <input
                            type="number"
                            className="input text-[10px] py-0.5 w-14"
                            value={b.pointsAwarded}
                            min={0}
                            max={100}
                            onChange={(e) => updateBug(idx, { pointsAwarded: Number(e.target.value) || 0 })}
                            title="Pontos awarded" />
                        )}
                      </div>
                      {b.description && (
                        <div className="text-[10px] text-liberthia-300/70 mt-1 line-clamp-2">
                          {b.description}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
                {/* Sumário dos credits que serão gerados — preview ao vivo */}
                <div className="mt-2 text-[10px] text-emerald-300/80 italic break-words">
                  {(() => {
                    const { credits } = buildCreditsAndBugsFixed(editableBugs)
                    return credits.length > 200 ? credits.slice(0, 200) + '...' : credits
                  })()}
                </div>
              </div>
            )}

            {result && (
              <div className={`mt-3 p-2 rounded text-xs ${result.errors > 0 ? 'bg-amber-900/40 border border-amber-500/30' : 'bg-green-900/40 border border-green-500/30'}`}>
                <div className="font-bold mb-1">
                  {result.errors === 0 ? '✅ Sucesso' : '⚠ Concluído com erros'}
                </div>
                <ul className="text-[10px] space-y-0.5">
                  <li>🆕 Criados: <strong>{result.created}</strong></li>
                  <li>✏ Atualizados: <strong>{result.updated}</strong></li>
                  <li>❌ Erros: <strong>{result.errors}</strong></li>
                </ul>
                {result.errorMessages.length > 0 && (
                  <details className="mt-1">
                    <summary className="cursor-pointer text-[10px] text-red-300">
                      Ver {result.errorMessages.length} erro(s)
                    </summary>
                    <ul className="text-[9px] text-red-300/80 mt-1 list-disc list-inside space-y-0.5 max-h-40 overflow-y-auto">
                      {result.errorMessages.map((msg, i) => <li key={i}>{msg}</li>)}
                    </ul>
                  </details>
                )}
              </div>
            )}

            <div className="flex gap-2 mt-3 sticky bottom-0 bg-liberthia-900/80 pt-2">
              <button type="button" className="btn flex-1" onClick={doImport} disabled={busy || !jsonText.trim()}>
                {busy ? '⏳ Importando...' : '📥 Importar'}
              </button>
              <button type="button" className="btn-ghost" onClick={close}>Fechar</button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
