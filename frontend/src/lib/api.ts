/**
 * Cliente HTTP simples (fetch). Aponta pra /api proxiado pelo Vite (dev) ou
 * pelo backend Spring Boot (prod).
 */
export type Player = {
  uuid: string
  name: string
  dimension: string
  health: number
  maxHealth: number
  food: number
  xp: number
  level: number
  gameMode: string
  position: { x: number; y: number; z: number; yaw: number; pitch: number }
}

export type ItemStack = {
  empty?: boolean
  id?: string
  count?: number
  name?: string
  nbt?: string
  slot?: number
  enchantments?: { id: string; level: number }[]
}

export type Inventory = {
  uuid: string
  name: string
  main: ItemStack[]
  armor: ItemStack[]
  offhand: ItemStack
}

export type ServerInfo = {
  motd: string
  tickCount: number
  playerCount: number
  maxPlayers: number
  tps: number
  dimensions: { id: string; dayTime: number; loadedChunks: number }[]
}

export type ItemDef = { id: string; name: string }
export type RegistryDef = { id: string; name: string }
export type EnchantmentDef = {
  id: string
  name: string
  maxLevel: number
  minLevel: number
  isCurse: boolean
  isTreasure: boolean
}

export type MatterProfile = { dm: number; wm: number; ym: number; type: string }

/** Snapshot da config runtime do Whisper (controle de transcrição). */
export type WhisperConfigSnapshot = {
  enabled: boolean
  scheduleEnabled: boolean
  scheduleHourFrom: number
  scheduleHourTo: number
  threads: number
  beamSize: number
  bestOf: number
  workers: number
  cpuPriority: number
  shouldProcessNow: boolean
  pausedReason: string
  updatedAt: string | null
}

const BASE = '/api'

import { getAuthToken } from '../store/auth'

async function req(path: string, init?: RequestInit): Promise<any> {
  const token = getAuthToken()
  // charset=utf-8 EXPLÍCITO. Sem isso o Spring Boot no Windows interpreta
  // o body como Windows-1252/ISO-8859-1 e quebra em qualquer caractere
  // multi-byte: § em §5§l (cores Minecraft), acentos, emojis, etc.
  // Sintoma: "JSON parse error: Invalid UTF-8 start byte 0xa7" no POST.
  const headers: Record<string, string> = {
    'Content-Type': 'application/json; charset=utf-8',
    ...(init?.headers as any || {}),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`
  const r = await fetch(BASE + path, { ...init, headers })
  if (r.status === 401) {
    // Token inválido → força logout
    localStorage.removeItem('liberthia.token')
    window.location.reload()
    throw new Error('unauthorized')
  }
  if (!r.ok) {
    const txt = await r.text().catch(() => '')
    throw new Error(`HTTP ${r.status}: ${txt}`)
  }
  return r.json()
}

// ============ Public API ============
// Endpoints sem auth (Changelog, Roadmap, Bug Hunter Leaderboard).
// Backend libera /api/public/* automaticamente via AuthFilter.
async function publicReq(path: string, init?: RequestInit): Promise<any> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json; charset=utf-8',
    ...(init?.headers as any || {}),
  }
  const r = await fetch(BASE + path, { ...init, headers })
  if (!r.ok) {
    const txt = await r.text().catch(() => '')
    throw new Error(`HTTP ${r.status}: ${txt}`)
  }
  return r.json()
}

export type ChangelogEntryDto = {
  id: number
  version: string
  title: string
  summary: string | null
  itemsAdded: string | null
  bugsFixed: string | null
  buffs: string | null
  debuffs: string | null
  integrations: string | null
  credits: string | null
  notes: string | null
  /** JSON-array string com bugs estruturados (schema v2). Parse com JSON.parse */
  bugsJson: string | null
  /** JSON-array string com sugestões estruturadas (schema v2) */
  suggestionsJson: string | null
  highlighted: boolean
  releaseDate: string
  createdBy: string | null
  createdAt: string
  updatedAt: string | null
}

/**
 * Bug estruturado dentro do changelog (parsed do `bugsJson`).
 * Mesmo schema usado pelo BulkImportButton no preview editável.
 */
export type ChangelogBugDto = {
  id?: number
  title: string
  severity?: string
  priority?: string
  reporterMcName?: string
  pointsAwarded?: number
  itemId?: string
  description?: string
  screenshotUrl?: string
  status?: 'fixed' | 'pending'
  fixDetails?: string
  fixedIn?: string
  triagedAt?: string
  createdAt?: string
}

export type ChangelogSuggestionDto = {
  id?: number
  title: string
  type?: string
  authorMcName?: string
  score?: number
  upvotes?: number
  downvotes?: number
  description?: string
  suggestedItemId?: string
  iconUrl?: string
  status?: string
  adminNote?: string
  createdAt?: string
  statusChangedAt?: string
}

export type RoadmapItemDto = {
  id: number
  title: string
  description: string | null
  category: 'IDEA' | 'PLANNED' | 'IN_DEV' | 'NEXT' | 'DONE' | 'CANCELLED'
  emoji: string | null
  tag: string | null
  votes: number
  priority: number
  targetVersion: string | null
  createdBy: string | null
  createdAt: string
}

export type BugLeaderboardEntry = {
  rank: number
  mcName: string
  points: number
  bugsConfirmed: number
  tier: string
  memberSince: string
}

export type BugLeaderboardResponse = {
  ranking: BugLeaderboardEntry[]
  count: number
  totalBugsConfirmed: number
  totalTesters: number
  tiers: { name: string; minBugs: number; emoji: string; color: string }[]
}

export const publicApi = {
  // ===== Changelog =====
  changelog: (): Promise<{ entries: ChangelogEntryDto[]; count: number }> =>
    publicReq('/public/changelog'),
  changelogOne: (id: number): Promise<ChangelogEntryDto> =>
    publicReq(`/public/changelog/${id}`),

  // ===== Roadmap =====
  roadmap: (): Promise<{ grouped: Record<string, RoadmapItemDto[]> }> =>
    publicReq('/public/roadmap'),
  roadmapFlat: (): Promise<{ items: RoadmapItemDto[]; count: number }> =>
    publicReq('/public/roadmap/flat'),
  roadmapVote: (id: number): Promise<{ ok: boolean; votes: number }> =>
    publicReq(`/public/roadmap/${id}/vote`, { method: 'POST' }),
  roadmapUnvote: (id: number): Promise<{ ok: boolean; votes: number }> =>
    publicReq(`/public/roadmap/${id}/unvote`, { method: 'POST' }),

  // ===== Bug Hunter Leaderboard =====
  leaderboard: (): Promise<BugLeaderboardResponse> =>
    publicReq('/public/leaderboard'),
}

// ============ Telemetry (sistema nervoso do mod) ============
export type TelemetryFeatures = {
  exploration: number
  aggression: number
  efficiency: number
  confusion: number
  frustration: number
  social: number
  risk: number
  windowMs: number
  computedAt: number
}

export type TelemetryInference = {
  goal: 'mining' | 'exploring' | 'fighting' | 'building' | 'socializing' | 'idle' | 'unknown' | string
  goalConfidence: number
  mood: 'focused' | 'frustrated' | 'confused' | 'satisfied' | 'neutral' | string
  moodConfidence: number
  suggestedAction: string
  actionConfidence: number
  computedAt: number
}

export type TelemetrySnapshot = {
  uuid: string
  name: string
  online: boolean
  stale: boolean
  ageMs: number
  lastUpdate: number
  session: {
    startedAt: number
    durationMs: number
    idleMs: number
    dimension: string
    pos: { x: number; y: number; z: number }
    rotation: { yaw: number; pitch: number }
  }
  counters: {
    deaths: number
    kills: number
    damageDealt: number
    damageTaken: number
    blocksBroken: number
    blocksPlaced: number
    chatMessages: number
    inventoryOpens: number
    craftCount: number
    totalDistanceXZ: number
  }
  features?: TelemetryFeatures
  inference?: TelemetryInference
  timeline: { size: number; capacity: number }
}

export const telemetryApi = {
  status: (): Promise<{
    enabled: boolean; playerCount: number; lastBatchReceived: number;
    lastBatchSize: number; ageOfLastBatchMs: number; ts: number
  }> => req('/telemetry/status'),
  players: (): Promise<{ players: TelemetrySnapshot[]; count: number; enabled: boolean }> =>
    req('/telemetry/players'),
  player: (uuid: string): Promise<TelemetrySnapshot> => req(`/telemetry/player/${uuid}`),
  setConfig: (enabled: boolean): Promise<{ ok: boolean; enabled: boolean }> =>
    req('/telemetry/config', { method: 'PUT', body: JSON.stringify({ enabled }) }),
}

export const contentAdminApi = {
  // ===== Changelog admin =====
  createChangelog: (body: Partial<ChangelogEntryDto>): Promise<ChangelogEntryDto> =>
    req('/admin/changelog', { method: 'POST', body: JSON.stringify(body) }),
  updateChangelog: (id: number, body: Partial<ChangelogEntryDto>): Promise<ChangelogEntryDto> =>
    req(`/admin/changelog/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteChangelog: (id: number): Promise<{ ok: boolean }> =>
    req(`/admin/changelog/${id}`, { method: 'DELETE' }),
  /**
   * Pega um DRAFT da próxima entry de changelog com bugs CONFIRMED desde a
   * última release + sugestões aprovadas/implementadas no período. Backend
   * calcula o "corte" baseado no mais recente entre o último changelog e o
   * último mod package uploaded. Admin recebe a estrutura pronta pra editar
   * + salvar via createChangelog().
   */
  autoGenerateNextChangelog: (): Promise<{
    version: string
    title: string
    summary: string
    releaseDate: string
    bugs: ChangelogBugDto[]
    suggestions: ChangelogSuggestionDto[]
    bugsFixed: string
    credits: string
    _meta: { cutoffAt: string | null; cutoffSource: string; bugsCount: number; suggestionsCount: number }
    [key: string]: any
  }> => req('/admin/changelog/auto-generate-next'),

  // ===== Roadmap admin =====
  createRoadmap: (body: Partial<RoadmapItemDto>): Promise<RoadmapItemDto> =>
    req('/admin/roadmap', { method: 'POST', body: JSON.stringify(body) }),
  updateRoadmap: (id: number, body: Partial<RoadmapItemDto>): Promise<RoadmapItemDto> =>
    req(`/admin/roadmap/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteRoadmap: (id: number): Promise<{ ok: boolean }> =>
    req(`/admin/roadmap/${id}`, { method: 'DELETE' }),
}

// ============ Mod Config (token override no DB) ============
// Permite ao operador inspecionar e atualizar o token de auth com o mod sem
// editar docker-compose.env + restart. Endpoint backend grava em backend_config
// e força o ModRegistry a usar o novo token.
export type ModTokenInfo = {
  tokenFingerprint: string
  source: 'DB' | 'ENV'
  lastRegisteredAt: string | null
  lastUpdatedAt: string | null
  currentMatchesRegistered: boolean
  hasOverride: boolean
  /**
   * v0.1.48+: sempre `true`. O painel é a única fonte do token, o auto-register
   * do mod NUNCA sobrescreve. Campo mantido pra compat de payload.
   */
  manualLock: boolean
}

export type ModTokenTestResult = {
  ok: boolean
  error?: string
  tps?: number
  playerCount?: number
  maxPlayers?: number
  motd?: string
  tokenFingerprint: string
}

export const modConfigApi = {
  getToken: (): Promise<ModTokenInfo> => req('/admin/mod-config/token'),
  setToken: (token: string): Promise<{ ok: boolean; tokenFingerprint: string; source: string; at: string }> =>
    req('/admin/mod-config/token', { method: 'POST', body: JSON.stringify({ token }) }),
  testConnection: (): Promise<ModTokenTestResult> =>
    req('/admin/mod-config/test-connection', { method: 'POST' }),
  // v0.1.48+: unlock REMOVIDO — o painel é a única fonte. Auto-register do mod
  // sempre é ignorado pro token.
}

// ============ Tester API ============
// Cliente separado pro tester pq o token vai num header diferente.
async function testerReq(path: string, init?: RequestInit, withAuth = true): Promise<any> {
  const token = withAuth ? localStorage.getItem('liberthia.tester.token') : null
  const headers: Record<string, string> = {
    'Content-Type': 'application/json; charset=utf-8',
    ...(init?.headers as any || {}),
  }
  if (token) headers['Authorization'] = `Bearer ${token}`
  const r = await fetch(BASE + path, { ...init, headers })

  // Parse body antes — backend manda mensagem específica em JSON
  const txt = await r.text().catch(() => '')
  let parsed: any = null
  try { parsed = JSON.parse(txt) } catch {}

  if (r.status === 401) {
    const isLoginAttempt = path.includes('/auth/login') || path.includes('/auth/register') || path.includes('/auth/claim')
    // Em login/register/claim: NÃO limpa token nem redireciona — usuário tá tentando entrar
    if (!isLoginAttempt) {
      const pathname = typeof window !== 'undefined' ? window.location.pathname : ''
      // SÓ limpa o token + redireciona se o user TÁ NUMA PÁGINA DE TESTER.
      // Antes redirecionava em qualquer 401, deslogando o admin quando ele
      // acessava /tester-admin e algum widget fazia uma chamada tester.
      const onTesterApp = pathname.startsWith('/tester/dashboard') ||
                          pathname.startsWith('/tester/claim')
      if (onTesterApp) {
        localStorage.removeItem('liberthia.tester.token')
        localStorage.removeItem('liberthia.tester.dto')
        setTimeout(() => {
          if (!window.location.pathname.includes('/tester/login') &&
              !window.location.pathname.includes('/tester/claim')) {
            window.location.href = '/tester/login?expired=1'
          }
        }, 800)
      }
      // Se estamos em outra página (ex: admin no /tester-admin), não desloga —
      // só joga o erro, o componente lida com isso (mostra toast, oculta widget, etc).
    }
    const msg = parsed?.error || 'Sessão expirada ou credenciais inválidas.'
    const err: any = new Error(msg)
    err.status = 401
    err.reason = parsed?.reason
    throw err
  }
  if (!r.ok) {
    const msg = parsed?.error || `HTTP ${r.status}: ${txt || r.statusText}`
    const err: any = new Error(msg)
    err.status = r.status
    err.reason = parsed?.reason
    throw err
  }
  return parsed ?? {}
}

export const testerApi = {
  register: (body: { mcName: string; code: string; password: string }) =>
    testerReq('/tester/auth/register', { method: 'POST', body: JSON.stringify(body) }, false),
  login: (body: { mcName: string; password: string }) =>
    testerReq('/tester/auth/login', { method: 'POST', body: JSON.stringify(body) }, false),
  me: () => testerReq('/tester/auth/me'),

  // PÚBLICO — inscrição no processo seletivo (sem código)
  apply: (body: { realName: string; mcName: string; contact?: string; weeklyAvailability: boolean; motivation: string }) =>
    testerReq('/tester/apply', { method: 'POST', body: JSON.stringify(body) }, false),
  // PÚBLICO — config (frontend usa pra mostrar/esconder o form de apply)
  publicConfig: (): Promise<{ applicationsEnabled: boolean }> =>
    testerReq('/tester/config', undefined, false),
  // ADMIN — toggle do form de inscrição
  adminSetApplicationsEnabled: (enabled: boolean) =>
    req('/admin/tester/config/applications-enabled', {
      method: 'POST', body: JSON.stringify({ enabled }),
    }),

  // Tester: servidor de teste + sugestões/votos
  serverInfo: () => testerReq('/tester/server-info'),
  listSuggestions: (status?: string) =>
    testerReq(`/tester/suggestions${status ? `?status=${status}` : ''}`),
  createSuggestion: (body: {
    type: string; title: string; description: string;
    technicalDetails?: string; referenceUrl?: string;
    // v96 — completo
    suggestedItemId?: string; iconUrl?: string;
    recipeJson?: string; effectsJson?: string;
  }) => testerReq('/tester/suggestions', { method: 'POST', body: JSON.stringify(body) }),
  mySuggestions: () => testerReq('/tester/suggestions/mine'),
  voteSuggestion: (id: number, vote: 1 | 0 | -1) =>
    testerReq(`/tester/suggestions/${id}/vote`, { method: 'POST', body: JSON.stringify({ vote }) }),

  // Admin: applications + server-info + suggestions
  adminListApplications: (status?: string): Promise<{ applications: any[]; count: number }> =>
    req(`/admin/tester/applications${status ? `?status=${status}` : ''}`),
  adminApproveApplication: (id: number, note?: string) =>
    req(`/admin/tester/applications/${id}/approve`, { method: 'POST', body: JSON.stringify({ note: note ?? '' }) }),
  adminRejectApplication: (id: number, note?: string) =>
    req(`/admin/tester/applications/${id}/reject`, { method: 'POST', body: JSON.stringify({ note: note ?? '' }) }),
  adminGetServerInfo: () => req('/admin/tester/server-info'),
  adminUpdateServerInfo: (body: any) =>
    req('/admin/tester/server-info', { method: 'PUT', body: JSON.stringify(body) }),
  adminListSuggestions: (status?: string): Promise<{ suggestions: any[]; count: number }> =>
    req(`/admin/tester/suggestions${status ? `?status=${status}` : ''}`),
  adminUpdateSuggestion: (id: number, status: string, note?: string) =>
    req(`/admin/tester/suggestions/${id}/status`, { method: 'POST', body: JSON.stringify({ status, note: note ?? '' }) }),

  // Tester endpoints (auth com Bearer tester-token)
  packages: (): Promise<{ packages: any[]; count: number }> => testerReq('/tester/packages'),
  /**
   * URL pra ser usada em `<a href="...">` ou `window.location = ...`.
   * NÃO dá pra mandar Authorization header numa navegação direta, então
   * o backend aceita `?token=...` como fallback do Bearer em endpoints
   * de download de tester (TesterContentController.requireTester).
   *
   * Anterior usava fetch() + blob() — quebrava com "Failed to fetch" em
   * packs grandes (memória do browser apertava montando o Blob inteiro).
   * Anchor direto faz o browser stream-ar o ZIP pro disco sem buffer.
   */
  packageDownloadUrl: (id: number): string => {
    const token = localStorage.getItem('liberthia.tester.token') ?? ''
    return `${BASE}/tester/packages/${id}/dl?token=${encodeURIComponent(token)}`
  },
  betaItems: (): Promise<{ items: any[]; count: number }> => testerReq('/tester/beta-items'),
  createBug: (body: {
    betaItemId?: number; title: string; description: string;
    stepsToReproduce?: string; severity?: string;
    // novos campos (v90+): identifica o item, contexto, evidência
    itemId?: string; howFound?: string; modVersion?: string;
    mcVersion?: string; worldContext?: string; screenshotUrl?: string;
    // v96+: triage detalhada
    frequency?: string; priority?: string;
    canReplicate?: boolean; affectsOthers?: boolean;
    expectedBehavior?: string; workaround?: string; tags?: string;
  }) => testerReq('/tester/bugs', { method: 'POST', body: JSON.stringify(body) }),
  /** v96: outro tester confirma que conseguiu replicar (+1). */
  confirmBugReplication: (id: number) =>
    testerReq(`/tester/bugs/${id}/confirm-replication`, { method: 'POST' }),
  /** Tester edita o próprio bug — só funciona enquanto status=PENDING. */
  updateBug: (id: number, body: any) =>
    testerReq(`/tester/bugs/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  /** Tester deleta o próprio bug — só funciona enquanto status=PENDING. */
  deleteBug: (id: number) =>
    testerReq(`/tester/bugs/${id}`, { method: 'DELETE' }),
  /** v96: lista bugs públicos da comunidade pra triagem cruzada. */
  communityBugs: (limit = 20): Promise<{ bugs: any[]; count: number }> =>
    testerReq(`/tester/bugs/community?limit=${limit}`),
  myBugs: (): Promise<{ bugs: any[]; count: number }> => testerReq('/tester/bugs/mine'),
  rewards: (): Promise<{ rewards: any[]; count: number }> => testerReq('/tester/rewards'),
  redeem: (id: number) => testerReq(`/tester/rewards/${id}/redeem`, { method: 'POST' }),
  myRedemptions: (): Promise<{ redemptions: any[]; count: number }> => testerReq('/tester/redemptions/mine'),

  // Admin endpoints (usa o req com Bearer admin)
  adminCreateInvite: (note?: string): Promise<{ ok: boolean; code: string; id: number; note: string }> =>
    req('/admin/tester/invites', { method: 'POST', body: JSON.stringify({ note: note ?? '' }) }),
  adminListInvites: (onlyUnused = false): Promise<{ invites: any[]; count: number }> =>
    req(`/admin/tester/invites?onlyUnused=${onlyUnused}`),
  adminDeleteInvite: (id: number) =>
    req(`/admin/tester/invites/${id}`, { method: 'DELETE' }),
  adminRanking: (): Promise<{ ranking: any[]; count: number }> =>
    req('/admin/tester/ranking'),
  // v94: gerenciamento de contas tester
  adminListTesters: (): Promise<{ testers: any[]; count: number }> =>
    req('/admin/tester/testers'),
  adminBanTester: (mcName: string, reason?: string) =>
    req(`/admin/tester/testers/${encodeURIComponent(mcName)}/ban`, {
      method: 'POST', body: JSON.stringify({ reason: reason ?? '' })
    }),
  adminUnbanTester: (mcName: string) =>
    req(`/admin/tester/testers/${encodeURIComponent(mcName)}/unban`, { method: 'POST' }),
  adminDeleteTester: (mcName: string) =>
    req(`/admin/tester/testers/${encodeURIComponent(mcName)}`, { method: 'DELETE' }),

  // Admin: packages
  adminListPackages: (): Promise<{ packages: any[]; count: number }> => req('/admin/tester/packages'),
  adminUploadPackage: async (file: File, name: string, version?: string, description?: string) => {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('name', name)
    if (version) fd.append('version', version)
    if (description) fd.append('description', description)
    const token = getAuthToken()
    const r = await fetch(BASE + '/admin/tester/packages', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: fd,
    })
    if (!r.ok) throw new Error(`HTTP ${r.status}: ${await r.text()}`)
    return r.json()
  },
  adminDeletePackage: (id: number) => req(`/admin/tester/packages/${id}`, { method: 'DELETE' }),
  adminTogglePackage: (id: number) => req(`/admin/tester/packages/${id}/toggle`, { method: 'POST' }),

  // Admin: beta items
  adminListBetaItems: (): Promise<{ items: any[]; count: number }> => req('/admin/tester/beta-items'),
  adminCreateBetaItem: (body: any) => req('/admin/tester/beta-items', { method: 'POST', body: JSON.stringify(body) }),
  adminUpdateBetaItem: (id: number, body: any) => req(`/admin/tester/beta-items/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  adminDeleteBetaItem: (id: number) => req(`/admin/tester/beta-items/${id}`, { method: 'DELETE' }),
  /**
   * "Destrava tudo": liga todos os beta items que tão com enabled=false.
   * Usar quando tester reclama "tá vazio" mas o admin viu items na lista
   * (sintoma de items importados com enabled=false ou cadastros antigos
   * com flag travada). Retorna quantos foram ligados.
   */
  adminEnableAllBetaItems: (): Promise<{ ok: boolean; updated: number }> =>
    req('/admin/tester/beta-items/enable-all', { method: 'POST' }),

  // Admin: bugs
  adminListBugs: (onlyPending = false): Promise<{ bugs: any[]; count: number }> =>
    req(`/admin/tester/bugs?onlyPending=${onlyPending}`),
  adminConfirmBug: (id: number, points: number, note?: string) =>
    req(`/admin/tester/bugs/${id}/confirm`, { method: 'POST', body: JSON.stringify({ points, note: note ?? '' }) }),
  adminRejectBug: (id: number, note?: string) =>
    req(`/admin/tester/bugs/${id}/reject`, { method: 'POST', body: JSON.stringify({ note: note ?? '' }) }),

  // Admin: rewards
  adminListRewards: (): Promise<{ rewards: any[]; count: number }> => req('/admin/tester/rewards'),
  adminCreateReward: (body: any) => req('/admin/tester/rewards', { method: 'POST', body: JSON.stringify(body) }),
  adminUpdateReward: (id: number, body: any) => req(`/admin/tester/rewards/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  adminDeleteReward: (id: number) => req(`/admin/tester/rewards/${id}`, { method: 'DELETE' }),
  adminListRedemptions: (): Promise<{ redemptions: any[]; count: number }> => req('/admin/tester/redemptions'),
  adminDeliverRedemption: (id: number, note?: string) =>
    req(`/admin/tester/redemptions/${id}/deliver`, { method: 'POST', body: JSON.stringify({ note: note ?? '' }) }),

  // Bulk import (JSONs gerados pela IA via tools/generate-items-update.py).
  // Backend faz upsert por chave natural (itemId/name/title). Ver:
  // backend BulkImportController + tools/MOD_UPDATE_PROTOCOL.md
  adminBulkImportBetaItems: (payload: any): Promise<{ ok: boolean; created: number; updated: number; errors: number; errorMessages: string[] }> =>
    req('/admin/tester/beta-items/bulk-import', { method: 'POST', body: JSON.stringify(payload) }),
  adminBulkImportRewards: (payload: any): Promise<{ ok: boolean; created: number; updated: number; errors: number; errorMessages: string[] }> =>
    req('/admin/tester/rewards/bulk-import', { method: 'POST', body: JSON.stringify(payload) }),
  adminBulkImportChangelog: (payload: any): Promise<{ ok: boolean; created: number; updated: number; errors: number; errorMessages: string[] }> =>
    req('/admin/tester/changelog/bulk-import', { method: 'POST', body: JSON.stringify(payload) }),
  adminBulkImportRoadmap: (payload: any): Promise<{ ok: boolean; created: number; updated: number; errors: number; errorMessages: string[] }> =>
    req('/admin/tester/roadmap/bulk-import', { method: 'POST', body: JSON.stringify(payload) }),

  // ============ SPLASH SUGGESTIONS ============
  listSplashes: (status?: string): Promise<{ splashes: any[] }> =>
    testerReq(`/tester/splashes${status && status !== 'all' ? `?status=${status}` : ''}`),
  createSplash: (body: { text: string; colorHex?: string; category?: string }) =>
    testerReq('/tester/splashes', { method: 'POST', body: JSON.stringify(body) }),
  voteSplash: (id: number, vote: 1 | 0 | -1) =>
    testerReq(`/tester/splashes/${id}/vote`, { method: 'POST', body: JSON.stringify({ vote }) }),
  adminTriageSplash: (id: number, status: string, adminNote?: string) =>
    req(`/admin/tester/splashes/${id}/triage`, { method: 'POST', body: JSON.stringify({ status, adminNote: adminNote ?? '' }) }),
  adminDeleteSplash: (id: number) => req(`/admin/tester/splashes/${id}`, { method: 'DELETE' }),

  // ============ BALANCE REQUESTS (buff/nerf) ============
  listBalance: (status?: string): Promise<{ requests: any[] }> =>
    testerReq(`/tester/balance${status && status !== 'all' ? `?status=${status}` : ''}`),
  myBalance: (): Promise<{ requests: any[] }> => testerReq('/tester/balance/mine'),
  createBalance: (body: any) =>
    testerReq('/tester/balance', { method: 'POST', body: JSON.stringify(body) }),
  adminTriageBalance: (id: number, status: string, adminNote?: string, awardPoints?: number) =>
    req(`/admin/tester/balance/${id}/triage`, {
      method: 'POST',
      body: JSON.stringify({ status, adminNote: adminNote ?? '', awardPoints: awardPoints ?? null }),
    }),
  adminDeleteBalance: (id: number) => req(`/admin/tester/balance/${id}`, { method: 'DELETE' }),

  // ============ NOTIFICATIONS ============
  notifications: (): Promise<{ notifications: any[]; unread: number }> =>
    testerReq('/tester/notifications'),
  markNotifRead: (id: number) =>
    testerReq(`/tester/notifications/${id}/read`, { method: 'PATCH' }),
  markAllNotifRead: () =>
    testerReq('/tester/notifications/read-all', { method: 'POST' }),
  deleteNotif: (id: number) =>
    testerReq(`/tester/notifications/${id}`, { method: 'DELETE' }),

  // ============ AUTO-CREATE (admin gera conta + claim link) ============
  adminAutoCreate: (mcName: string, note?: string): Promise<{ ok: boolean; mcName: string; claimCode: string; claimUrl: string }> =>
    req('/admin/tester/auto-create', { method: 'POST', body: JSON.stringify({ mcName, note: note ?? '' }) }),

  // ============ 3D MODELS (BlockBench) ============
  // Tester: lista pública (apenas habilitados) + URL pra baixar o .bbmodel
  testerListModels: (): Promise<{ models: any[]; count: number }> =>
    testerReq('/tester/models'),
  testerModelFileUrl: (id: number) => `${BASE}/tester/models/${id}/file`,
  // Admin: gerencia (upload/list/delete/toggle/rename)
  adminListModels: (): Promise<{ models: any[]; count: number }> =>
    req('/admin/tester/models'),
  adminUploadModel: async (file: File, name: string, description?: string, category?: string) => {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('name', name)
    if (description) fd.append('description', description)
    if (category) fd.append('category', category)
    const token = getAuthToken()
    const r = await fetch(BASE + '/admin/tester/models', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: fd,
    })
    if (!r.ok) throw new Error(`HTTP ${r.status}: ${await r.text()}`)
    return r.json()
  },
  adminDeleteModel: (id: number) => req(`/admin/tester/models/${id}`, { method: 'DELETE' }),
  adminToggleModel: (id: number) => req(`/admin/tester/models/${id}/toggle`, { method: 'POST' }),
  adminUpdateModel: (id: number, body: { name?: string; description?: string; category?: string }) =>
    req(`/admin/tester/models/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  adminModelFileUrl: (id: number) => `${BASE}/admin/tester/models/${id}/file`,

  // ============ BETA AUDIOS (criaturas/instrumentos/ambient) ============
  testerListAudios: (): Promise<{ audios: any[]; count: number }> =>
    testerReq('/tester/audios'),
  testerAudioStreamUrl: (id: number) => `${BASE}/tester/audios/${id}/stream`,
  adminListAudios: (): Promise<{ audios: any[]; count: number }> =>
    req('/admin/tester/audios'),
  adminAudioStreamUrl: (id: number) => `${BASE}/admin/tester/audios/${id}/stream`,
  adminUploadAudio: async (file: File, name: string, opts?: {
    description?: string; category?: string; creatureId?: string; durationSec?: number
  }) => {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('name', name)
    if (opts?.description) fd.append('description', opts.description)
    if (opts?.category) fd.append('category', opts.category)
    if (opts?.creatureId) fd.append('creatureId', opts.creatureId)
    if (opts?.durationSec != null) fd.append('durationSec', String(opts.durationSec))
    const token = getAuthToken()
    const r = await fetch(BASE + '/admin/tester/audios', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: fd,
    })
    if (!r.ok) throw new Error(`HTTP ${r.status}: ${await r.text()}`)
    return r.json()
  },
  adminDeleteAudio: (id: number) => req(`/admin/tester/audios/${id}`, { method: 'DELETE' }),
  adminToggleAudio: (id: number) => req(`/admin/tester/audios/${id}/toggle`, { method: 'POST' }),
  adminUpdateAudio: (id: number, body: { name?: string; description?: string; category?: string; creatureId?: string }) =>
    req(`/admin/tester/audios/${id}`, { method: 'PUT', body: JSON.stringify(body) }),

  // ============ VOTES (genérico — BETA_ITEM | BETA_AUDIO | MODEL_3D) ============
  // Toggle: clicar no mesmo vote remove. Clicar no oposto troca.
  // Resposta inclui counts atualizados + myVote.
  vote: (targetType: 'BETA_ITEM' | 'BETA_AUDIO' | 'MODEL_3D', targetId: number, vote: 'LIKE' | 'DISLIKE') =>
    testerReq('/tester/vote', {
      method: 'POST',
      body: JSON.stringify({ targetType, targetId, vote }),
    }),
  getVotes: (targetType: 'BETA_ITEM' | 'BETA_AUDIO' | 'MODEL_3D', targetId: number) =>
    testerReq(`/tester/votes/${targetType}/${targetId}`),
}

// ============ FEATURE WIKI (público + admin) ============
// Rotas usam /feature-wiki porque /api/wiki já existe no backend pro
// WikiPage genérico do projeto. feature-wiki é específico do tester
// (docs por item/bloco do mod, com créditos pros testers que ajudaram).
// ─── Hate Speech Detector ──────────────────────────────────────────────
// Alertas gerados pelo HateSpeechDetectorService quando o Whisper transcreve
// um clipe e o pipeline regex+Ollama detecta discurso de ódio.
export type HateSpeechAlert = {
  id: number
  voiceClipId: number
  playerUuid: string
  playerName: string
  ts: number
  transcription: string
  categories: string             // CSV: "transfobia,racismo"
  triggers: string               // CSV: "traveco,morre"
  severity: 'LOW' | 'MEDIUM' | 'HIGH'
  confidence: number             // 0.0-1.0
  reason: string
  detectionMode: 'HYBRID' | 'QUICK_ONLY' | 'LLM_FALSE_POSITIVE'
  reviewed: boolean
  actionTaken: string | null     // IGNORE | WARN | MUTE | KICK | BAN
  reviewedBy: string | null
  reviewedAt: string | null
  createdAt: string
}
export type HateSpeechStats = {
  total: number
  pendingReview: number
  bySeverity: Record<string, number>
  topPlayers: Array<{ uuid: string; name: string; count: number }>
}

export const hateSpeechApi = {
  list: (params: { reviewed?: boolean; playerUuid?: string; severity?: string; limit?: number } = {}): Promise<HateSpeechAlert[]> => {
    const qs = new URLSearchParams()
    if (params.reviewed !== undefined) qs.set('reviewed', String(params.reviewed))
    if (params.playerUuid) qs.set('playerUuid', params.playerUuid)
    if (params.severity) qs.set('severity', params.severity)
    if (params.limit) qs.set('limit', String(params.limit))
    return req(`/admin/hate-speech/alerts${qs.toString() ? '?' + qs.toString() : ''}`)
  },
  stats: (): Promise<HateSpeechStats> => req('/admin/hate-speech/stats'),
  review: (id: number, action: 'IGNORE' | 'WARN' | 'MUTE' | 'KICK' | 'BAN', reason?: string, by?: string) =>
    req(`/admin/hate-speech/alerts/${id}/review`, {
      method: 'POST',
      body: JSON.stringify({ action, reason: reason || '', by: by || 'admin' }),
    }),
  testScan: (text: string): Promise<{ text: string; matchedTriggers: string[]; categories: string[]; wouldAnalyze: boolean }> =>
    req(`/admin/hate-speech/test-scan?text=${encodeURIComponent(text)}`),
}

export const wikiApi = {
  list: (category?: string): Promise<{ entries: any[] }> =>
    req(`/feature-wiki${category && category !== 'all' ? `?category=${category}` : ''}`),
  get: (slug: string): Promise<any> => req(`/feature-wiki/${slug}`),
  adminList: (): Promise<{ entries: any[] }> => req('/admin/feature-wiki'),
  adminSave: (body: any) => req('/admin/feature-wiki', { method: 'POST', body: JSON.stringify(body) }),
  adminDelete: (id: number) => req(`/admin/feature-wiki/${id}`, { method: 'DELETE' }),
  // Bulk import: upsert por slug. Payload: {entries: [{slug,title,category,itemId?,
  //   imageUrl?,summary?,contentMd,creditsJson?,recipeJson?,addedInVersion?,tags?,published?}]}
  bulkImport: (payload: any): Promise<{ ok: boolean; created: number; updated: number; errors: number; errorMessages: string[] }> =>
    req('/admin/feature-wiki/bulk-import', { method: 'POST', body: JSON.stringify(payload) }),
}

export const api = {
  serverInfo: (): Promise<ServerInfo> => req('/server/info'),
  players: async (): Promise<Player[]> => (await req('/players')).players,
  inventory: (uuid: string): Promise<Inventory> => req(`/player/${uuid}/inventory`),
  items: async (): Promise<ItemDef[]> => (await req('/items')).items,
  enchantments: async (): Promise<EnchantmentDef[]> => (await req('/enchantments')).enchantments,
  // /api/sounds e /api/particles já são usados pelo upload de resource pack;
  // o registry do MC fica em /registry/* pra evitar conflito.
  spawnPlayerClone: (body: { x: number; y: number; z: number; dimension?: string; playerName?: string; playerUuid?: string; rotation?: number; tag?: string; customName?: string }) =>
    req('/world/spawn-player-clone', { method: 'POST', body: JSON.stringify(body) }),
  killByTag: (tag: string) =>
    req('/world/kill-by-tag', { method: 'POST', body: JSON.stringify({ tag }) }),
  sounds: async (): Promise<RegistryDef[]> => (await req('/registry/sounds')).sounds,
  particles: async (): Promise<RegistryDef[]> => (await req('/registry/particles')).particles,
  effects: async (): Promise<RegistryDef[]> => (await req('/registry/effects')).effects,
  entities: async (): Promise<RegistryDef[]> => (await req('/registry/entities')).entities,
  matter: (uuid: string): Promise<MatterProfile> => req(`/matter/${uuid}`),
  setMatter: (uuid: string, body: Partial<MatterProfile>) =>
    req(`/matter/${uuid}`, { method: 'POST', body: JSON.stringify(body) }),
  give: (uuid: string, item: string, count = 1, enchantments: { id: string; level: number }[] = []) =>
    req(`/player/${uuid}/give`, {
      method: 'POST',
      body: JSON.stringify({ item, count, enchantments }),
    }),
  remove: (uuid: string, slot: number, count = 64) =>
    req(`/player/${uuid}/remove`, { method: 'POST', body: JSON.stringify({ slot, count }) }),
  clearInv: (uuid: string) => req(`/player/${uuid}/clear`, { method: 'POST' }),
  effect: (uuid: string, effect: string, duration = 600, amplifier = 0) =>
    req(`/player/${uuid}/effect`, {
      method: 'POST',
      body: JSON.stringify({ effect, duration, amplifier }),
    }),
  teleport: (uuid: string, x: number, y: number, z: number, dimension?: string) =>
    req(`/player/${uuid}/teleport`, {
      method: 'POST',
      body: JSON.stringify({ x, y, z, dimension }),
    }),
  kick: (uuid: string, reason: string) =>
    req(`/player/${uuid}/kick`, { method: 'POST', body: JSON.stringify({ reason }) }),
  freeze: (uuid: string) => req(`/player/${uuid}/freeze`, { method: 'POST' }),
  unfreeze: (uuid: string) => req(`/player/${uuid}/unfreeze`, { method: 'POST' }),
  freezeStatus: (uuid: string): Promise<{ frozen: boolean; anchorX?: number; anchorY?: number; anchorZ?: number; moveAttempts?: number }> =>
    req(`/player/${uuid}/freeze-status`),

  // Pilgrimages — peregrinações que costuram memorial/glyph/anchor em ordem
  pilgrimagesList: (): Promise<{ pilgrimages: PilgrimageDto[]; progress: PilgrimageProgressDto[] }> =>
    req('/pilgrimages'),
  pilgrimageSave: (p: PilgrimageDto): Promise<PilgrimageDto> =>
    req('/pilgrimages', { method: 'POST', body: JSON.stringify(p) }),
  pilgrimageDelete: (id: string) => req(`/pilgrimages/${id}`, { method: 'DELETE' }),
  pilgrimageToggle: (id: string): Promise<PilgrimageDto> =>
    req(`/pilgrimages/${id}/toggle`, { method: 'POST' }),
  pilgrimageResetProgress: (id: string, playerUuid: string) =>
    req(`/pilgrimages/${id}/reset/${playerUuid}`, { method: 'POST' }),

  // Voice clips — captura de áudio dos players via Simple Voice Chat
  voiceList: (playerUuid?: string, limit = 100): Promise<{ clips: VoiceClipDto[]; count: number }> =>
    req(`/voice/clips?limit=${limit}${playerUuid ? `&playerUuid=${playerUuid}` : ''}`),
  voiceGet: (id: number): Promise<VoiceClipDto> => req(`/voice/clips/${id}`),
  voicePatch: (id: number, body: { transcription?: string; tagsJson?: string; language?: string }) =>
    req(`/voice/clips/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),
  voiceDelete: (id: number) => req(`/voice/clips/${id}`, { method: 'DELETE' }),
  voiceBulkDelete: (ids: number[]): Promise<{ ok: boolean; deleted: number }> =>
    req(`/voice/clips/bulk-delete`, { method: 'POST', body: JSON.stringify({ ids }) }),
  voiceDeleteByPlayer: (playerUuid: string): Promise<{ ok: boolean; deleted: number }> =>
    req(`/voice/clips/by-player/${playerUuid}`, { method: 'DELETE' }),
  voiceDeleteAll: (): Promise<{ ok: boolean; deleted: number; error?: string }> =>
    req(`/voice/clips/all`, { method: 'DELETE', headers: { 'X-Confirm-Purge': 'yes' } }),
  voicePlay: (id: number, body: { x: number; y: number; z: number; dimension?: string; volume?: number; category?: string }) =>
    req(`/voice/clips/${id}/play`, { method: 'POST', body: JSON.stringify(body) }),
  // Voice Library — agregação por player + busca avançada
  // v92: sempre passa fresh=true pra trigger orphan cleanup no backend antes da agg.
  // Antes era /voice/library/players sem param → backend retornava counts incluindo
  // arquivos já deletados do disco.
  voiceLibraryPlayers: (): Promise<{ players: VoicePlayerSummary[]; count: number }> =>
    req(`/voice/library/players?fresh=true`),
  voiceLibrarySearch: (filters: {
    playerUuid?: string; minDurationMs?: number; maxDurationMs?: number;
    minBytes?: number; maxBytes?: number; fromTs?: number; toTs?: number;
    sort?: 'recent' | 'oldest' | 'longest' | 'shortest' | 'biggest' | 'smallest';
    limit?: number;
  }): Promise<{ clips: VoiceClipDto[]; count: number }> => {
    const qs = new URLSearchParams()
    Object.entries(filters).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.append(k, String(v))
    })
    return req(`/voice/library/search?${qs.toString()}`)
  },
  /** Helper: URL pra usar em <audio src=...>. Inclui token. */
  voiceAudioUrl: (id: number): string => {
    const token = localStorage.getItem('liberthia.token') ?? ''
    return `${BASE}/voice/clips/${id}/audio?token=${encodeURIComponent(token)}`
  },

  // Photo Library — agregação por player de fotos do Exposure
  photoLibraryPlayers: (): Promise<{ players: PhotoPlayerSummary[]; count: number }> =>
    req('/photos/library/players'),
  photoLibrarySearch: (filters: {
    authorUuid?: string; minBytes?: number; maxBytes?: number;
    fromMs?: number; toMs?: number; onlyCursed?: boolean;
    sort?: 'recent' | 'oldest' | 'biggest' | 'smallest';
    limit?: number;
  }): Promise<{ photos: any[]; count: number }> => {
    const qs = new URLSearchParams()
    Object.entries(filters).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '' && v !== false) qs.append(k, String(v))
    })
    return req(`/photos/library/search?${qs.toString()}`)
  },

  // Structures — catálogo de DungeonsArise/YungsBetter/etc + /locate
  structuresCatalog: (): Promise<{ groups: Array<{ name: string; emoji: string; count: number; structures: string[] }>; total: number }> =>
    req('/structures/catalog'),
  structuresLocate: (structureId: string, origin?: string): Promise<{ ok: boolean; cmd?: string; note?: string }> =>
    req('/structures/locate', { method: 'POST', body: JSON.stringify({ structureId, origin: origin ?? '' }) }),
  structuresTeleport: (body: { playerName: string; x: number; y: number; z: number; dimension?: string }): Promise<{ ok: boolean }> =>
    req('/structures/teleport', { method: 'POST', body: JSON.stringify(body) }),

  // Balloons — TalkBalloons + Comics + Emojiful
  balloonsList: (params?: { playerUuid?: string; source?: string; page?: number; size?: number }): Promise<{ content: BalloonDto[]; total: number }> => {
    const qs = new URLSearchParams()
    if (params?.playerUuid) qs.append('playerUuid', params.playerUuid)
    if (params?.source) qs.append('source', params.source)
    qs.append('page', String(params?.page ?? 0))
    qs.append('size', String(params?.size ?? 100))
    return req(`/balloons?${qs.toString()}`)
  },
  balloonsStats: (): Promise<{ topPlayers: Array<{ playerUuid: string; playerName: string; count: number; lastTs: number }>; captured: number; admin: number; total: number }> =>
    req('/balloons/stats'),
  balloonsSay: (body: { playerName: string; text: string; type?: 'talk' | 'comic' | 'shout' | 'thought' | 'image'; imageUrl?: string; createdBy?: string }): Promise<{ ok: boolean; id?: number; error?: string }> =>
    req('/balloons/say', { method: 'POST', body: JSON.stringify(body) }),
  balloonsDelete: (id: number): Promise<{ ok: boolean }> =>
    req(`/balloons/${id}`, { method: 'DELETE' }),
  balloonsBulkDelete: (ids: number[]): Promise<{ ok: boolean; deleted: number }> =>
    req('/balloons/bulk-delete', { method: 'POST', body: JSON.stringify({ ids }) }),

  // Paintings — galeria de xercapaint (upload manual + curadoria)
  paintingsList: (params?: { category?: string; featured?: boolean; authorUuid?: string; page?: number; size?: number }) => {
    const qs = new URLSearchParams()
    if (params?.category) qs.append('category', params.category)
    if (params?.featured) qs.append('featured', 'true')
    if (params?.authorUuid) qs.append('authorUuid', params.authorUuid)
    qs.append('page', String(params?.page ?? 0))
    qs.append('size', String(params?.size ?? 60))
    return req(`/paintings?${qs.toString()}`)
  },
  paintingUpload: (form: FormData) => fetch(`${BASE}/paintings`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}` },
    body: form,
  }).then(r => { if (!r.ok) throw new Error(`${r.status}`); return r.json() }),
  paintingFeature: (id: number, on = true): Promise<any> =>
    req(`/paintings/${id}/feature?on=${on}`, { method: 'POST' }),
  paintingDelete: (id: number): Promise<{ ok: boolean }> =>
    req(`/paintings/${id}`, { method: 'DELETE' }),
  paintingImageUrl: (id: number): string => {
    const token = localStorage.getItem('liberthia.token') ?? ''
    return `${BASE}/paintings/${id}/image?token=${encodeURIComponent(token)}`
  },

  // Storage Rankings — top players por backpacks/storage
  storageTop: (limit = 10): Promise<{ ranking: Array<{ uuid: string; name: string; dimension: string; total: number; backpacks: number; storage: number; byItem: Record<string, number> }>; count: number }> =>
    req(`/storage-rankings/top?limit=${limit}`),

  // Loot Table (random items with rarity weights)
  lootItems: (filter?: { category?: string; rarity?: string }): Promise<LootItemDto[]> => {
    const qs = new URLSearchParams()
    if (filter?.category) qs.append('category', filter.category)
    if (filter?.rarity) qs.append('rarity', filter.rarity)
    return req(`/loot/items${qs.toString() ? '?' + qs : ''}`)
  },
  lootCreate: (body: Partial<LootItemDto>): Promise<LootItemDto> =>
    req('/loot/items', { method: 'POST', body: JSON.stringify(body) }),
  lootUpdate: (id: number, body: Partial<LootItemDto>): Promise<LootItemDto> =>
    req(`/loot/items/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  lootDelete: (id: number): Promise<{ ok: boolean }> =>
    req(`/loot/items/${id}`, { method: 'DELETE' }),
  lootDraw: (body?: { category?: string; rarity?: string }): Promise<{ ok: boolean; item?: LootItemDto; error?: string }> =>
    req('/loot/draw', { method: 'POST', body: JSON.stringify(body ?? {}) }),
  lootGive: (body: { playerName: string; category?: string; rarity?: string }):
    Promise<{ ok: boolean; item?: LootItemDto; error?: string }> =>
    req('/loot/give', { method: 'POST', body: JSON.stringify(body) }),
  lootStats: (): Promise<{ total: number; totalWeight: number; byRarity: Record<string, number>; byCategory: Record<string, number> }> =>
    req('/loot/stats'),
  lootSimulate: (n: number): Promise<{ n: number; hits: Record<string, number>; rarityHits: Record<string, number> }> =>
    req(`/loot/simulate?n=${n}`),
  lootPresets: (): Promise<Partial<LootItemDto>[]> => req('/loot/presets'),
  lootImportPresets: (): Promise<{ ok: boolean; added: number; skipped: number; total: number }> =>
    req('/loot/import-presets', { method: 'POST' }),

  // Voice Conversation Mode — escutar N players em ordem cronológica
  voiceConversation: (params: {
    playerUuids: string[]
    lastMinutes?: number
    fromTs?: number
    toTs?: number
    limit?: number
  }): Promise<{
    clips: VoiceClipDto[]
    count: number
    fromTs: number
    toTs: number
    totalDurationMs: number
    totalBytes: number
    playerUuids: string[]
  }> => {
    const qs = new URLSearchParams()
    qs.append('playerUuids', params.playerUuids.join(','))
    if (params.lastMinutes != null) qs.append('lastMinutes', String(params.lastMinutes))
    if (params.fromTs) qs.append('fromTs', String(params.fromTs))
    if (params.toTs) qs.append('toTs', String(params.toTs))
    if (params.limit) qs.append('limit', String(params.limit))
    return req(`/voice/sessions/conversation?${qs.toString()}`)
  },

  // Voice transcription (Whisper)
  voiceTranscribe: (id: number): Promise<{ ok: boolean; status?: string }> =>
    req(`/voice/clips/${id}/transcribe`, { method: 'POST' }),
  voiceTranscriptionStatus: (): Promise<{ total: number; done: number; pending: number; processing: number; failed: number; skipped: number; pctDone: number }> =>
    req('/voice/transcription/status'),
  /** Status do scheduler do Whisper — ticksRun, lastInfo, runtime config etc. */
  voiceWhisperStatus: (): Promise<{
    enabled: boolean; modelPath: string; language: string
    threads: number; workers: number; activeWorkers: number
    beamSize: number; bestOf: number; cpuPriority: number
    ticksRun: number; clipsProcessed: number
    lastTickTs: number; lastTickInfo: string; currentlyRunning: boolean
    runtimeConfig: WhisperConfigSnapshot
  }> => req('/voice/whisper/status'),
  /** Config runtime do Whisper (persistido em DB, aplica em ~5s sem restart). */
  voiceWhisperConfigGet: (): Promise<WhisperConfigSnapshot> =>
    req('/voice/whisper/config'),
  voiceWhisperConfigPut: (patch: Partial<WhisperConfigSnapshot>): Promise<WhisperConfigSnapshot> =>
    req('/voice/whisper/config', { method: 'PUT', body: JSON.stringify(patch) }),
  voiceWhisperPause: (): Promise<WhisperConfigSnapshot> =>
    req('/voice/whisper/pause', { method: 'POST' }),
  voiceWhisperResume: (): Promise<WhisperConfigSnapshot> =>
    req('/voice/whisper/resume', { method: 'POST' }),

  // ===== Voice Retention (cleanup automático) =====
  voiceRetentionGet: (): Promise<{
    enabled: boolean; retentionDays: number; maxClipsPerPlayer: number
    updatedBy: string; updatedAt: string | null; nextRunCron: string
  }> => req('/voice/retention'),
  voiceRetentionUpdate: (body: {
    enabled?: boolean; retentionDays?: number;
    maxClipsPerPlayer?: number; updatedBy?: string
  }): Promise<{ ok: boolean; enabled: boolean; retentionDays: number; maxClipsPerPlayer: number }> =>
    req('/voice/retention', { method: 'PUT', body: JSON.stringify(body) }),
  voiceRetentionPreview: (days: number): Promise<{
    days: number; cutoff: number; wouldDelete: number
    totalBytes: number; totalDurationMs: number
  }> => req(`/voice/retention/preview?days=${days}`),
  voiceRetentionRun: (days?: number): Promise<{ ok: boolean; removed: number; daysUsed: number }> =>
    req(`/voice/retention/run${days != null ? `?days=${days}` : ''}`, { method: 'POST' }),
  voiceRetentionEnforcePerPlayer: (max?: number): Promise<{ ok: boolean; removed: number; maxUsed: number }> =>
    req(`/voice/retention/enforce-per-player${max != null ? `?max=${max}` : ''}`, { method: 'POST' }),

  // ===== Video Editor (ffmpeg compositor) =====
  videoUploadAsset: async (file: File, kind: 'image' | 'music'): Promise<{
    ok: boolean; assetId?: string; size?: number; name?: string; error?: string
  }> => {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('kind', kind)
    const token = getAuthToken()
    const r = await fetch(BASE + '/video/upload-asset', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: fd,
    })
    if (!r.ok) throw new Error(`HTTP ${r.status}: ${await r.text()}`)
    return r.json()
  },
  videoRender: (project: {
    backgroundImageId?: string | null
    musicId?: string | null
    musicVolume?: number
    musicLoop?: boolean
    voiceClips: { clipId: number; startMs: number; volume?: number }[]
    title?: string
  }): Promise<{ ok: boolean; jobId?: string; status?: string; error?: string }> =>
    req('/video/render', { method: 'POST', body: JSON.stringify(project) }),
  videoStatus: (jobId: string): Promise<{
    ok: boolean
    id: string
    status: 'QUEUED' | 'RENDERING' | 'DONE' | 'FAILED'
    progress: number
    message: string
    outputPath: string | null
    startedAt: string
    finishedAt?: string
    error?: string
  }> => req(`/video/status/${jobId}`),
  videoDownloadUrl: (jobId: string): string => `${BASE}/video/download/${jobId}`,
  videoJobs: (): Promise<{
    jobs: { id: string; status: string; progress: number; startedAt: string; outputName: string | null }[]
  }> => req('/video/jobs'),

  // ===== Saved videos (CRUD + público) =====
  savedVideosList: (): Promise<{ videos: SavedVideoDto[]; count: number }> =>
    req('/videos?limit=200'),
  savedVideoGet: (id: number): Promise<SavedVideoDto> => req(`/videos/${id}`),
  savedVideoUpdate: (id: number, body: { title?: string; description?: string; isPublic?: boolean }):
    Promise<SavedVideoDto> =>
    req(`/videos/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  savedVideoDelete: (id: number): Promise<{ ok: boolean }> =>
    req(`/videos/${id}`, { method: 'DELETE' }),
  savedVideoFromRender: (jobId: string, body: { title: string; description?: string; isPublic?: boolean }):
    Promise<SavedVideoDto> =>
    req(`/videos/save-render/${jobId}`, { method: 'POST', body: JSON.stringify(body) }),
  savedVideoUpload: async (file: File, title: string, description: string, isPublic: boolean):
    Promise<SavedVideoDto> => {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('title', title)
    fd.append('description', description)
    fd.append('isPublic', String(isPublic))
    const token = getAuthToken()
    const r = await fetch(BASE + '/videos/upload', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: fd,
    })
    if (!r.ok) throw new Error(`HTTP ${r.status}: ${await r.text()}`)
    return r.json()
  },
  /** URL pública do MP4 (no auth) — usar em <video src=...>. */
  savedVideoStreamUrl: (id: number): string => `${BASE}/public/videos/${id}/stream`,

  // ===== Public (sem auth) — usado pela página /watch/:id =====
  publicVideosList: (): Promise<{ videos: SavedVideoDto[]; count: number }> => {
    // bypass do req() pra não mandar Authorization
    return fetch(BASE + '/public/videos?limit=50').then(r => r.json())
  },
  publicVideoGet: (id: number): Promise<SavedVideoDto> =>
    fetch(BASE + `/public/videos/${id}`).then(r => {
      if (!r.ok) throw new Error('Video não encontrado ou privado')
      return r.json()
    }),

  // ===== Player Visits (frequência de visita) =====
  playerVisitsList: (sort: 'rare' | 'active' | 'recent' | 'inactive' | 'newest' = 'rare',
                     limit = 500): Promise<{
    players: PlayerVisitDto[]
    count: number
    sort: string
    stats: { rare: number; casual: number; regular: number; veteran: number }
  }> => req(`/players/visits?sort=${sort}&limit=${limit}`),
  playerVisitsDetail: (uuid: string): Promise<{
    uuid: string
    snapshots: { ts_ms: number; ts: string; name: string }[]
    count: number
  }> => req(`/players/visits/${uuid}`),

  /**
   * Voice Modulator → tocar pra player específico no jogo.
   * Envia o WAV gerado + UUID do alvo. Backend salva como clip, pega
   * posição do player, e toca via Simple Voice Chat positional na pos.
   */
  /** Tocar em coordenada específica (independente de player). */
  voiceModulatePlayAt: async (audioBlob: Blob, x: number, y: number, z: number,
                              dimension = 'minecraft:overworld', volume = 1.5,
                              deleteAfter = true): Promise<{
    ok: boolean; clipId?: number
    position?: { x: number; y: number; z: number; dim: string }
    volume?: number; error?: string
  }> => {
    const fd = new FormData()
    fd.append('audio', audioBlob, 'modulated.wav')
    fd.append('x', String(x)); fd.append('y', String(y)); fd.append('z', String(z))
    fd.append('dimension', dimension)
    fd.append('volume', String(volume))
    fd.append('deleteAfter', String(deleteAfter))
    const token = getAuthToken()
    const r = await fetch(BASE + '/voice/modulate-play-at', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: fd,
    })
    if (!r.ok) throw new Error(`HTTP ${r.status}: ${await r.text()}`)
    return r.json()
  },

  /** Tocar pra TODOS players online (cada um na própria posição). */
  voiceModulatePlayAll: async (audioBlob: Blob, volume = 1.5,
                               deleteAfter = true): Promise<{
    ok: boolean; clipId?: number; playedCount?: number; totalPlayers?: number
    perPlayer?: { name: string; played: boolean }[]; error?: string
  }> => {
    const fd = new FormData()
    fd.append('audio', audioBlob, 'modulated.wav')
    fd.append('volume', String(volume))
    fd.append('deleteAfter', String(deleteAfter))
    const token = getAuthToken()
    const r = await fetch(BASE + '/voice/modulate-play-all', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: fd,
    })
    if (!r.ok) throw new Error(`HTTP ${r.status}: ${await r.text()}`)
    return r.json()
  },

  voiceModulatePlay: async (audioBlob: Blob, playerUuid: string, volume = 1.5,
                            deleteAfter = true): Promise<{
    ok: boolean
    clipId?: number
    playerName?: string
    position?: { x: number; y: number; z: number; dim: string }
    volume?: number
    willDelete?: boolean
    error?: string
  }> => {
    // FormData needs the browser to set Content-Type (with boundary), so
    // bypassa o req() helper que força application/json.
    const fd = new FormData()
    fd.append('audio', audioBlob, 'modulated.wav')
    fd.append('playerUuid', playerUuid)
    fd.append('volume', String(volume))
    fd.append('deleteAfter', String(deleteAfter))
    const token = getAuthToken()
    const r = await fetch(BASE + '/voice/modulate-play', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: fd,
    })
    if (!r.ok) throw new Error(`HTTP ${r.status}: ${await r.text()}`)
    return r.json()
  },

  // ===== Voice Map (espacial + temporal) =====
  voiceMap: (params: {
    fromTs?: number; toTs?: number; dimension?: string
    playerUuid?: string; limit?: number
  }): Promise<{
    clips: VoiceClipDto[]
    clipsWithoutPos: VoiceClipDto[]
    onlinePlayers: { uuid: string; name: string; posX: number; posY: number; posZ: number; dimension: string; health: number; nearbyCount?: number; nearbyNames?: string[] }[]
    count: number
    fromTs: number
    toTs: number
    distinctPlayers: number
    dimensions: string[]
    bounds: { minX: number; maxX: number; minZ: number; maxZ: number }
  }> => {
    const qs = new URLSearchParams()
    if (params.fromTs) qs.append('fromTs', String(params.fromTs))
    if (params.toTs) qs.append('toTs', String(params.toTs))
    if (params.dimension) qs.append('dimension', params.dimension)
    if (params.playerUuid) qs.append('playerUuid', params.playerUuid)
    if (params.limit) qs.append('limit', String(params.limit))
    return req(`/voice/map?${qs.toString()}`)
  },
  voiceNearby: (clipId: number, radius = 30, timeMinutes = 5): Promise<{
    anchor?: VoiceClipDto
    nearby: { clip: VoiceClipDto; distance: number; timeDeltaMs: number }[]
    count: number
    radius: number
    timeMinutes: number
    error?: string
  }> => req(`/voice/nearby?clipId=${clipId}&radius=${radius}&timeMinutes=${timeMinutes}`),
  /** Clipes próximos da posição ATUAL de um player online. */
  voiceAroundPlayer: (uuid: string, radius = 50, minutes = 30): Promise<{
    playerName?: string
    playerUuid?: string
    playerPos?: { x: number; z: number; dim: string }
    radius?: number
    minutes?: number
    clips: { clip: VoiceClipDto; distance: number }[]
    count?: number
    error?: string
  }> => req(`/voice/around-player/${uuid}?radius=${radius}&minutes=${minutes}`),

  /** Busca por texto em transcrições. Retorna players agrupados + clipes. */
  voiceSearchTranscription: (query: string, limit = 200): Promise<{
    clips: VoiceClipDto[]
    players: { playerUuid: string; playerName: string; matchCount: number; clipIds: number[]; latestTs: number }[]
    count: number
    query: string
  }> => req(`/voice/search?q=${encodeURIComponent(query)}&limit=${limit}`),

  // Auto-Gifts (cron-based rewards)
  autoGiftRules: (): Promise<AutoGiftRuleDto[]> => req('/auto-gifts/rules'),
  autoGiftCreate: (body: Partial<AutoGiftRuleDto>): Promise<AutoGiftRuleDto> =>
    req('/auto-gifts/rules', { method: 'POST', body: JSON.stringify(body) }),
  autoGiftUpdate: (id: number, body: Partial<AutoGiftRuleDto>): Promise<AutoGiftRuleDto> =>
    req(`/auto-gifts/rules/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  autoGiftDelete: (id: number): Promise<{ ok: boolean }> =>
    req(`/auto-gifts/rules/${id}`, { method: 'DELETE' }),
  autoGiftRunNow: (id: number): Promise<{ ok: boolean; winners?: number; status?: string; error?: string }> =>
    req(`/auto-gifts/rules/${id}/run-now`, { method: 'POST' }),
  autoGiftRuns: (id: number, page = 0): Promise<{ content: AutoGiftRunDto[]; totalElements: number }> =>
    req(`/auto-gifts/rules/${id}/runs?page=${page}&size=30`),
  autoGiftAllRuns: (page = 0): Promise<{ content: AutoGiftRunDto[]; totalElements: number }> =>
    req(`/auto-gifts/runs?page=${page}&size=50`),
  autoGiftMetrics: (): Promise<Array<{ id: string; label: string; desc: string; unit: string }>> =>
    req('/auto-gifts/metrics'),
  autoGiftSchedules: (): Promise<Array<{ id: string; label: string }>> =>
    req('/auto-gifts/schedules'),
  autoGiftTemplates: (): Promise<AutoGiftRuleDto[]> => req('/auto-gifts/templates'),
  autoGiftPreview: (metric: string, topN = 10, minValue = 0):
    Promise<{ metric: string; entries: Array<{ playerUuid: string; playerName: string; value: number }> }> =>
    req(`/auto-gifts/preview?metric=${metric}&topN=${topN}&minValue=${minValue}`),

  // Voice clips — proteção & ZIP
  voiceProtect: (id: number, reason = '', by = ''): Promise<any> =>
    req(`/voice/clips/${id}/protect`, { method: 'POST', body: JSON.stringify({ reason, by }) }),
  voiceUnprotect: (id: number): Promise<any> =>
    req(`/voice/clips/${id}/unprotect`, { method: 'POST' }),
  voiceBulkProtect: (ids: number[], protect = true, reason = '', by = ''): Promise<{ ok: boolean; updated: number }> =>
    req('/voice/clips/bulk-protect', { method: 'POST', body: JSON.stringify({ ids, protect, reason, by }) }),
  voiceZipUrl: (playerUuid: string): string => {
    const token = localStorage.getItem('liberthia.token') ?? ''
    return `${BASE}/voice/clips/by-player/${playerUuid}/zip?token=${encodeURIComponent(token)}`
  },

  // Etched — Music Disc Library (URLs de YouTube → disco no jogo)
  etchedList: (category?: string): Promise<EtchedDiscDto[]> =>
    req(`/etched/discs${category ? `?category=${encodeURIComponent(category)}` : ''}`),
  etchedCreate: (body: Partial<EtchedDiscDto>): Promise<EtchedDiscDto> =>
    req('/etched/discs', { method: 'POST', body: JSON.stringify(body) }),
  etchedUpdate: (id: number, body: Partial<EtchedDiscDto>): Promise<EtchedDiscDto> =>
    req(`/etched/discs/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  etchedDelete: (id: number): Promise<{ ok: boolean }> =>
    req(`/etched/discs/${id}`, { method: 'DELETE' }),
  etchedGive: (id: number, playerName: string, count = 1): Promise<{ ok: boolean; cmd?: string; error?: string }> =>
    req(`/etched/discs/${id}/give`, { method: 'POST', body: JSON.stringify({ playerName, count }) }),
  etchedGiveLabel: (id: number, playerName: string): Promise<{ ok: boolean; note?: string; error?: string }> =>
    req(`/etched/discs/${id}/give-label`, { method: 'POST', body: JSON.stringify({ playerName }) }),
  etchedGiveKit: (id: number, playerName: string, includeTable = true): Promise<{ ok: boolean; note?: string; error?: string }> =>
    req(`/etched/discs/${id}/give-kit`, { method: 'POST', body: JSON.stringify({ playerName, includeTable }) }),
  etchedGiveRadio: (id: number, playerName: string): Promise<{ ok: boolean; note?: string; error?: string }> =>
    req(`/etched/discs/${id}/give-radio`, { method: 'POST', body: JSON.stringify({ playerName }) }),
  etchedGiveBackpack: (id: number, playerName: string,
    opts: { advanced?: boolean; vanillaDisc?: string; discType?: 'vanilla' | 'etched' } = {}):
    Promise<{ ok: boolean; note?: string; error?: string }> =>
    req(`/etched/discs/${id}/give-backpack`, { method: 'POST',
      body: JSON.stringify({ playerName, ...opts }) }),
  etchedExtract: (id: number): Promise<{ ok: boolean; status?: string; error?: string }> =>
    req(`/etched/discs/${id}/extract`, { method: 'POST' }),
  etchedUploadAudio: (id: number, file: File): Promise<{ ok: boolean; size?: number; audioUrl?: string; error?: string }> => {
    const fd = new FormData()
    fd.append('file', file)
    return fetch(`${BASE}/etched/discs/${id}/upload-audio`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}` },
      body: fd,
    }).then(r => r.json())
  },
  etchedCookiesStatus: (): Promise<{ present: boolean; size?: number; lastModified?: number }> =>
    req('/etched/cookies/status'),
  etchedUploadCookies: (file: File): Promise<{ ok: boolean; size?: number; error?: string }> => {
    const fd = new FormData()
    fd.append('file', file)
    return fetch(`${BASE}/etched/cookies`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${localStorage.getItem('liberthia.token') ?? ''}` },
      body: fd,
    }).then(r => { if (!r.ok) throw new Error(`${r.status}`); return r.json() })
  },
  etchedDeleteCookies: (): Promise<{ ok: boolean }> =>
    req('/etched/cookies', { method: 'DELETE' }),

  // SecurityCraft — chaves, câmeras, portas
  scItems: (): Promise<{ items: string[] }> => req('/securitycraft/items'),
  scPlayers: (): Promise<{ players: ScPlayerInfo[]; count: number }> =>
    req('/securitycraft/players'),
  scListBlocks: (playerName: string): Promise<{ ok: boolean; note?: string }> =>
    req('/securitycraft/list-blocks', { method: 'POST', body: JSON.stringify({ playerName }) }),
  scResetBlocks: (playerName: string): Promise<{ ok: boolean }> =>
    req('/securitycraft/reset-blocks', { method: 'POST', body: JSON.stringify({ playerName }) }),
  scGiveItem: (playerName: string, itemId: string, count = 1): Promise<{ ok: boolean; error?: string }> =>
    req('/securitycraft/give-item', { method: 'POST', body: JSON.stringify({ playerName, itemId, count }) }),

  // Camera Mod — kits + POV cinemático
  cameraModItems: (): Promise<{ items: Array<{ id: string; label: string }> }> => req('/camera-mod/items'),
  cameraGiveKit: (body: { playerName: string; withAlbum?: boolean; photoCount?: number }): Promise<{ ok: boolean }> =>
    req('/camera-mod/give-kit', { method: 'POST', body: JSON.stringify(body) }),
  cameraPov: (adminName: string, targetName: string): Promise<{ ok: boolean }> =>
    req('/camera-mod/pov', { method: 'POST', body: JSON.stringify({ adminName, targetName }) }),
  cameraPovExit: (playerName: string): Promise<{ ok: boolean }> =>
    req('/camera-mod/pov-exit', { method: 'POST', body: JSON.stringify({ playerName }) }),
  cameraCinematic: (playerName: string, enable: boolean): Promise<{ ok: boolean }> =>
    req('/camera-mod/cinematic', { method: 'POST', body: JSON.stringify({ playerName, enable }) }),

  // Banned items — banlist gerenciada pelo painel
  bannedItemsList: (): Promise<BannedItemDto[]> => req('/banned-items'),
  bannedItemSearch: (q: string, limit = 50): Promise<{ items: Array<{ id: string; displayName: string }>; count: number }> =>
    req(`/banned-items/search-items?q=${encodeURIComponent(q)}&limit=${limit}`),
  bannedItemBan: (body: { itemId: string; displayName?: string; reason?: string;
    mode?: 'auto_clear' | 'broadcast' | 'silent'; bannedBy?: string; active?: boolean
  }): Promise<BannedItemDto> =>
    req('/banned-items', { method: 'POST', body: JSON.stringify(body) }),
  bannedItemUpdate: (id: number, body: Partial<BannedItemDto>): Promise<BannedItemDto> =>
    req(`/banned-items/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  bannedItemUnban: (id: number): Promise<{ ok: boolean }> =>
    req(`/banned-items/${id}`, { method: 'DELETE' }),
  bannedItemToggle: (id: number): Promise<BannedItemDto> =>
    req(`/banned-items/${id}/toggle`, { method: 'POST' }),
  bannedItemEnforce: (id: number): Promise<{ ok: boolean }> =>
    req(`/banned-items/${id}/enforce`, { method: 'POST' }),

  // Pehkui — escalas de player
  pehkuiTypes: (): Promise<{ types: string[] }> => req('/pehkui/types'),
  pehkuiEligible: (minDm = 0): Promise<{ players: PehkuiPlayerInfo[]; count: number }> =>
    req(`/pehkui/eligible?minDm=${minDm}`),
  pehkuiScale: (body: {
    playerName: string; scaleType: string; scale: number;
    persistent?: boolean; delayTicks?: number;
  }): Promise<{ ok: boolean; cmd?: string; error?: string }> =>
    req('/pehkui/scale', { method: 'POST', body: JSON.stringify(body) }),
  pehkuiBulk: (body: {
    playerName: string; scales: Record<string, number>; persistent?: boolean;
  }): Promise<{ ok: boolean; applied?: string[]; count?: number; error?: string }> =>
    req('/pehkui/bulk', { method: 'POST', body: JSON.stringify(body) }),
  pehkuiReset: (body: { playerName: string; scaleType?: string }): Promise<{ ok: boolean }> =>
    req('/pehkui/reset', { method: 'POST', body: JSON.stringify(body) }),
  pehkuiPresets: (): Promise<PehkuiPresetDto[]> => req('/pehkui/presets'),
  pehkuiPresetCreate: (body: Partial<PehkuiPresetDto>): Promise<PehkuiPresetDto> =>
    req('/pehkui/presets', { method: 'POST', body: JSON.stringify(body) }),
  pehkuiPresetUpdate: (id: number, body: Partial<PehkuiPresetDto>): Promise<PehkuiPresetDto> =>
    req(`/pehkui/presets/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  pehkuiPresetDelete: (id: number): Promise<{ ok: boolean }> =>
    req(`/pehkui/presets/${id}`, { method: 'DELETE' }),
  pehkuiPresetApply: (id: number, playerName: string): Promise<{ ok: boolean; applied?: number; duration?: number; error?: string }> =>
    req(`/pehkui/presets/${id}/apply`, { method: 'POST', body: JSON.stringify({ playerName }) }),

  command: (command: string, origin = 'site') =>
    req('/command', { method: 'POST', body: JSON.stringify({ command, origin }) }),
  worldTime: (time: number) =>
    req('/world/time', { method: 'POST', body: JSON.stringify({ time }) }),
  worldWeather: (type: 'clear' | 'rain' | 'thunder', duration = 6000) =>
    req('/world/weather', { method: 'POST', body: JSON.stringify({ type, duration }) }),
  worldDifficulty: (difficulty: 'peaceful' | 'easy' | 'normal' | 'hard') =>
    req('/world/difficulty', { method: 'POST', body: JSON.stringify({ difficulty }) }),
  operators: (): Promise<{ operators: { name: string }[] }> => req('/server/operators'),
  bans: (): Promise<{ playerBans: { name: string }[]; ipBans: { ip: string }[] }> => req('/server/bans'),
  whitelist: (): Promise<{ enabled: boolean; whitelist: { name: string }[] }> => req('/server/whitelist'),
  saveAll: () => req('/server/save', { method: 'POST' }),
  broadcast: (message: string) =>
    req('/server/broadcast', { method: 'POST', body: JSON.stringify({ message }) }),
  // ===== Power tools =====
  title: (uuid: string, title: string, subtitle?: string, fadeIn = 10, stay = 70, fadeOut = 20) =>
    req(`/player/${uuid}/title`, { method: 'POST', body: JSON.stringify({ title, subtitle, fadeIn, stay, fadeOut }) }),
  sound: (uuid: string, sound: string, volume = 1, pitch = 1) =>
    req(`/player/${uuid}/sound`, { method: 'POST', body: JSON.stringify({ sound, volume, pitch }) }),
  lightning: (uuid: string) => req(`/player/${uuid}/lightning`, { method: 'POST' }),
  heal: (uuid: string) => req(`/player/${uuid}/heal`, { method: 'POST' }),
  feed: (uuid: string) => req(`/player/${uuid}/feed`, { method: 'POST' }),
  xp: (uuid: string, levels = 0, points = 0, mode: 'set' | 'add' = 'add') =>
    req(`/player/${uuid}/xp`, { method: 'POST', body: JSON.stringify({ levels, points, mode }) }),
  gamemode: (uuid: string, mode: 'survival' | 'creative' | 'adventure' | 'spectator') =>
    req(`/player/${uuid}/gamemode`, { method: 'POST', body: JSON.stringify({ mode }) }),
  tpTo: (uuid: string, targetUuid: string) =>
    req(`/player/${uuid}/tp-to`, { method: 'POST', body: JSON.stringify({ targetUuid }) }),
  spawnEntity: (entity: string, x: number, y: number, z: number, count = 1, dimension?: string) =>
    req('/world/spawn-entity', { method: 'POST', body: JSON.stringify({ entity, x, y, z, count, dimension }) }),
  particle: (particle: string, x: number, y: number, z: number, count = 30) =>
    req('/world/particle', { method: 'POST', body: JSON.stringify({ particle, x, y, z, count }) }),
  explosion: (x: number, y: number, z: number, power = 4, fire = false, blockDamage = false) =>
    req('/world/explosion', { method: 'POST', body: JSON.stringify({ x, y, z, power, fire, blockDamage }) }),
  whisper: (body: { x: number; y: number; z: number; radius?: number; dimension?: string; message?: string; title?: string; subtitle?: string; sound?: string; speaker?: string }) =>
    req('/world/whisper', { method: 'POST', body: JSON.stringify(body) }) as Promise<{ ok: boolean; recipients: { uuid: string; name: string; distance: number }[] }>,
  healAll: () => req('/players/heal-all', { method: 'POST' }),
  backup: () => req('/server/backup', { method: 'POST' }),

  // ===== History =====
  chatHistory: (uuid = '', since = 0, limit = 200) =>
    req(`/history/chat?uuid=${uuid}&since=${since}&limit=${limit}`).then((r: any) => r.entries as ChatEntry[]),
  cmdHistory: (uuid = '', since = 0, limit = 200) =>
    req(`/history/commands?uuid=${uuid}&since=${since}&limit=${limit}`).then((r: any) => r.entries as CommandEntry[]),

  // ===== Snapshots =====
  snapshotRunNow: () => req('/snapshot/run-now', { method: 'POST' }),
  snapshotList: (uuid: string) => req(`/snapshot/list/${uuid}`).then((r: any) => r.snapshots as number[]),
  snapshotGet: (uuid: string, ts: number) => req(`/snapshot/get/${uuid}/${ts}`) as Promise<Snapshot>,
  snapshotRestore: (uuid: string, ts: number, opts: { restoreInventory?: boolean; restoreStats?: boolean; restorePosition?: boolean } = {}) =>
    req(`/player/${uuid}/restore`, {
      method: 'POST',
      body: JSON.stringify({ ts, restoreInventory: true, ...opts }),
    }),
  siteCmdHistory: (since = 0, limit = 200) =>
    req(`/history/site-commands?since=${since}&limit=${limit}`).then((r: any) => r.entries as SiteCommandEntry[]),

  // ===== Custom Sounds =====
  soundsList: (): Promise<{ sounds: SoundEntry[]; packSha1: string; packSize: number }> => req('/sounds'),
  soundDelete: (namespace: string, key: string) =>
    req(`/sounds/${namespace}/${key}`, { method: 'DELETE' }),
  // Upload usa fetch direto (multipart) — não passa pelo wrapper req()
  soundUpload: async (file: File, namespace: string, key: string) => {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('namespace', namespace)
    fd.append('key', key)
    const token = getAuthToken()
    const r = await fetch('/api/sounds/upload', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: fd,
    })
    if (!r.ok) throw new Error(`HTTP ${r.status}: ${await r.text()}`)
    return r.json()
  },

  // ===== Custom Particles (texture overrides) =====
  particlesList: (): Promise<{ particles: ParticleEntry[]; packSha1: string; packSize: number }> => req('/particles'),
  particleDelete: (key: string) => req(`/particles/${key}`, { method: 'DELETE' }),
  particleUpload: async (file: File, key: string) => {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('key', key)
    const token = getAuthToken()
    const r = await fetch('/api/particles/upload', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: fd,
    })
    if (!r.ok) throw new Error(`HTTP ${r.status}: ${await r.text()}`)
    return r.json()
  },
  particlePreviewUrl: (key: string) => {
    const t = getAuthToken()
    return `/api/particles/preview/${key}${t ? `?token=${encodeURIComponent(t)}` : ''}`
  },

  // ===== Map =====
  mapChunks: (dim = '') => req(`/map/chunks${dim ? `?dim=${encodeURIComponent(dim)}` : ''}`) as Promise<MapChunksResponse>,
  mapChunkPngUrl: (cx: number, cz: number, dim = '') => {
    const t = getAuthToken()
    const tokenParam = t ? `${dim ? '&' : '?'}token=${encodeURIComponent(t)}` : ''
    return `${BASE}/map/chunk/${cx}/${cz}${dim ? `?dim=${encodeURIComponent(dim)}` : ''}${tokenParam}`
  },

  // ===== Engines de backend (PostgreSQL) =====
  // Forbidden Words
  forbiddenRules: (): Promise<{ rules: ForbiddenRule[] }> => req('/forbidden/rules'),
  forbiddenSave: (r: ForbiddenRule): Promise<ForbiddenRule> =>
    req('/forbidden/rules', { method: 'POST', body: JSON.stringify(r) }),
  forbiddenDelete: (id: string) => req(`/forbidden/rules/${id}`, { method: 'DELETE' }),
  forbiddenToggle: (id: string): Promise<ForbiddenRule> =>
    req(`/forbidden/rules/${id}/toggle`, { method: 'POST' }),
  forbiddenSimulate: (id: string, playerUuid: string) =>
    req(`/forbidden/rules/${id}/simulate`, { method: 'POST', body: JSON.stringify({ playerUuid }) }),
  forbiddenInvocations: (limit = 50): Promise<{ invocations: ForbiddenInvocation[] }> =>
    req(`/forbidden/invocations?limit=${limit}`),

  // Madness Meter
  madnessState: (): Promise<{ state: MadnessStateRow[]; config: MadnessConfigDto }> =>
    req('/madness/state'),
  madnessConfig: (): Promise<MadnessConfigDto> => req('/madness/config'),
  madnessSaveConfig: (c: MadnessConfigDto): Promise<MadnessConfigDto> =>
    req('/madness/config', { method: 'POST', body: JSON.stringify(c) }),
  madnessSetSanity: (uuid: string, sanity: number) =>
    req(`/madness/state/${uuid}`, { method: 'POST', body: JSON.stringify({ sanity }) }),
  madnessDelta: (uuid: string, delta: number) =>
    req(`/madness/state/${uuid}/delta`, { method: 'POST', body: JSON.stringify({ delta }) }),
  madnessResetAll: (sanity = 100) =>
    req('/madness/reset', { method: 'POST', body: JSON.stringify({ sanity }) }),
  madnessManifest: (uuid: string) =>
    req(`/madness/manifest/${uuid}`, { method: 'POST' }),
  madnessToggle: (): Promise<MadnessConfigDto> =>
    req('/madness/toggle', { method: 'POST' }),

  // Possession
  possessionList: (): Promise<{ entities: PossessionEntityDto[]; active: PossessionActiveDto[] }> =>
    req('/possession/entities'),
  possessionSave: (e: PossessionEntityDto): Promise<PossessionEntityDto> =>
    req('/possession/entities', { method: 'POST', body: JSON.stringify(e) }),
  possessionDelete: (id: string) => req(`/possession/entities/${id}`, { method: 'DELETE' }),
  possessionStart: (entityId: string, playerUuid: string, durationSec: number): Promise<PossessionActiveDto> =>
    req('/possession/start', { method: 'POST', body: JSON.stringify({ entityId, playerUuid, durationSec }) }),
  possessionEnd: (id: number) => req(`/possession/end/${id}`, { method: 'POST' }),

  // Cursed
  cursedList: (): Promise<{ curses: CurseDto[]; bindings: CurseBindingDto[] }> => req('/cursed'),
  cursedSave: (c: CurseDto): Promise<CurseDto> =>
    req('/cursed/curses', { method: 'POST', body: JSON.stringify(c) }),
  cursedDelete: (id: string) => req(`/cursed/curses/${id}`, { method: 'DELETE' }),
  cursedForge: (curseId: string, playerUuid: string): Promise<CurseBindingDto> =>
    req('/cursed/forge', { method: 'POST', body: JSON.stringify({ curseId, playerUuid }) }),
  cursedUnbind: (id: number) => req(`/cursed/unbind/${id}`, { method: 'POST' }),

  // Rifts
  riftsList: (): Promise<{ rifts: RiftDto[] }> => req('/rifts'),
  riftsSave: (r: RiftDto): Promise<RiftDto> =>
    req('/rifts', { method: 'POST', body: JSON.stringify(r) }),
  riftsDelete: (id: string) => req(`/rifts/${id}`, { method: 'DELETE' }),
  riftsToggle: (id: string): Promise<RiftDto> => req(`/rifts/${id}/toggle`, { method: 'POST' }),

  // Glyphs
  glyphsList: (): Promise<{ glyphs: GlyphDto[] }> => req('/glyphs'),
  glyphsSave: (g: GlyphDto): Promise<GlyphDto> =>
    req('/glyphs', { method: 'POST', body: JSON.stringify(g) }),
  glyphsDelete: (id: string) => req(`/glyphs/${id}`, { method: 'DELETE' }),
  glyphsToggle: (id: string): Promise<GlyphDto> => req(`/glyphs/${id}/toggle`, { method: 'POST' }),
  glyphsResetDiscoveries: (id: string) => req(`/glyphs/${id}/reset`, { method: 'POST' }),

  // Time-Locked Boxes
  boxesList: (): Promise<{ boxes: TimeBoxDto[] }> => req('/boxes'),
  boxesSave: (b: TimeBoxDto): Promise<TimeBoxDto> =>
    req('/boxes', { method: 'POST', body: JSON.stringify(b) }),
  boxesDelete: (id: string) => req(`/boxes/${id}`, { method: 'DELETE' }),
  boxesForceOpen: (id: string): Promise<TimeBoxDto> => req(`/boxes/${id}/force-open`, { method: 'POST' }),
  boxesReset: (id: string): Promise<TimeBoxDto> => req(`/boxes/${id}/reset`, { method: 'POST' }),

  // Memory Echoes
  memoryEchoesList: (): Promise<{ echoes: MemoryEchoDto[]; active: MemoryEchoActiveDto[] }> =>
    req('/memory-echoes'),
  memoryEchoesSave: (m: MemoryEchoDto): Promise<MemoryEchoDto> =>
    req('/memory-echoes', { method: 'POST', body: JSON.stringify(m) }),
  memoryEchoesDelete: (id: string) => req(`/memory-echoes/${id}`, { method: 'DELETE' }),
  memoryEchoesToggle: (id: string): Promise<MemoryEchoDto> => req(`/memory-echoes/${id}/toggle`, { method: 'POST' }),
  memoryEchoesPurge: () => req('/memory-echoes/purge', { method: 'POST' }),

  // Cosmic Calendar
  calendarGet: (): Promise<{ config: CalendarConfigDto; specialDays: SpecialDayDto[]; today: number }> => req('/calendar'),
  calendarSaveConfig: (c: CalendarConfigDto): Promise<CalendarConfigDto> =>
    req('/calendar/config', { method: 'POST', body: JSON.stringify(c) }),
  calendarSaveDay: (d: SpecialDayDto): Promise<SpecialDayDto> =>
    req('/calendar/days', { method: 'POST', body: JSON.stringify(d) }),
  calendarDeleteDay: (id: string) => req(`/calendar/days/${id}`, { method: 'DELETE' }),
  calendarFireDay: (id: string) => req(`/calendar/days/${id}/fire`, { method: 'POST' }),

  // Cutscenes (scheduled)
  cutscenesList: (): Promise<{ cutscenes: CutsceneDto[] }> => req('/cutscenes'),
  cutscenesSave: (c: CutsceneDto): Promise<CutsceneDto> =>
    req('/cutscenes', { method: 'POST', body: JSON.stringify(c) }),
  cutscenesDelete: (id: string) => req(`/cutscenes/${id}`, { method: 'DELETE' }),
  cutscenesToggle: (id: string): Promise<CutsceneDto> => req(`/cutscenes/${id}/toggle`, { method: 'POST' }),
  cutscenesRunNow: (id: string) => req(`/cutscenes/${id}/run-now`, { method: 'POST' }),

  // Decisões (Choice) — backend processa cliques de chat
  choiceList: (): Promise<{ decisions: ChoiceDecisionDto[]; history: ChoiceHistoryEntry[] }> => req('/choice'),
  choiceSave: (d: ChoiceDecisionDto): Promise<ChoiceDecisionDto> =>
    req('/choice/decisions', { method: 'POST', body: JSON.stringify(d) }),
  choiceDelete: (id: string) => req(`/choice/decisions/${id}`, { method: 'DELETE' }),
  choicePresent: (id: string, target: string): Promise<ChoiceDecisionDto> =>
    req(`/choice/decisions/${id}/present`, { method: 'POST', body: JSON.stringify({ target }) }),
  choiceCancel: (id: string): Promise<ChoiceDecisionDto> =>
    req(`/choice/decisions/${id}/cancel`, { method: 'POST' }),

  // Save Anchors
  anchorsList: (): Promise<{ anchors: SaveAnchorDto[] }> => req('/anchors'),
  anchorsSave: (a: SaveAnchorDto): Promise<SaveAnchorDto> =>
    req('/anchors', { method: 'POST', body: JSON.stringify(a) }),
  anchorsDelete: (id: string) => req(`/anchors/${id}`, { method: 'DELETE' }),
  anchorsToggle: (id: string): Promise<SaveAnchorDto> => req(`/anchors/${id}/toggle`, { method: 'POST' }),
  anchorsResetUses: (id: string): Promise<SaveAnchorDto> => req(`/anchors/${id}/reset`, { method: 'POST' }),

  // KV genérico (substitui localStorage pra páginas sem engine)
  kvGet: <T = any>(key: string): Promise<{ key: string; dataJson: string | null; updatedAt: string | null; data?: T }> =>
    req(`/kv/${encodeURIComponent(key)}`).then((r: any) => ({
      ...r,
      data: r.dataJson ? (() => { try { return JSON.parse(r.dataJson) } catch { return null } })() : null,
    })),
  kvPut: (key: string, data: any) =>
    req(`/kv/${encodeURIComponent(key)}`, {
      method: 'PUT',
      body: JSON.stringify({ dataJson: JSON.stringify(data) }),
    }),
  kvDelete: (key: string) => req(`/kv/${encodeURIComponent(key)}`, { method: 'DELETE' }),
  kvList: (prefix?: string): Promise<{ configs: { key: string; dataJson: string; updatedAt: string }[] }> =>
    req(`/kv${prefix ? `?prefix=${encodeURIComponent(prefix)}` : ''}`),

  // Version (diagnostico)
  version: (): Promise<{ service: string; startedAt: string; classFileTimestamp: string; endpointCount: number; endpoints: string[] }> =>
    req('/version'),

  // Nightmare Sequence (backend-driven)
  nightmareList: (): Promise<{ sequences: NightmareSequenceDto[]; active: NightmareRunStatus[] }> => req('/nightmares'),
  nightmareSave: (s: NightmareSequenceDto): Promise<NightmareSequenceDto> =>
    req('/nightmares/sequences', { method: 'POST', body: JSON.stringify(s) }),
  nightmareDelete: (id: string) => req(`/nightmares/sequences/${id}`, { method: 'DELETE' }),
  nightmareRun: (seqId: string, playerUuid: string) =>
    req('/nightmares/run', { method: 'POST', body: JSON.stringify({ seqId, playerUuid }) }),
  nightmareCancel: (runId: string) => req(`/nightmares/cancel/${runId}`, { method: 'POST' }),

  // r161: Spell / Magic / Observer endpoints
  spellsList: (): Promise<{ spells: SpellDto[]; total: number }> => req('/spells/list'),
  spellGive: (playerUuid: string, spellId: string): Promise<{ ok: boolean; status: string }> =>
    req('/spells/give', { method: 'POST', body: JSON.stringify({ playerUuid, spellId }) }),
  spellCast: (playerUuid: string, spellId: string): Promise<{ ok: boolean; status: string }> =>
    req('/spells/cast', { method: 'POST', body: JSON.stringify({ playerUuid, spellId }) }),
  spellCreate: (spellJson: any): Promise<{ ok: boolean; id?: string; note?: string }> =>
    req('/spells/create', { method: 'POST', body: JSON.stringify(spellJson) }),
  magicStatsGet: (uuid: string): Promise<MagicStatsDto> => req(`/magic/stats/${uuid}`),
  magicStatsSet: (uuid: string, body: Partial<MagicStatsDto>): Promise<{ ok: boolean }> =>
    req(`/magic/stats/${uuid}`, { method: 'POST', body: JSON.stringify(body) }),
  spawnObserverClone: (body: { x: number; y: number; z: number; dimension?: string; playerName?: string; playerUuid?: string }) =>
    req('/world/spawn-observer-clone', { method: 'POST', body: JSON.stringify(body) }),
}

export type SpellDto = {
  id: string
  name: string
  school: string
  rarity: string
  manaCost: number
  cooldownTicks: number
  damage: number
  range: number
  lore: string
  schoolColor: string
}

export type MagicStatsDto = {
  level?: number
  kills?: number
  progress?: number
  source?: number
  sourceMax?: number
}

export type NightmareSequenceDto = {
  id?: string
  emoji: string
  name: string
  description: string
  stepsJson: string
  snapshotBefore: boolean
  restoreAfter: boolean
}

export type NightmareRunStatus = {
  runId: string
  seqId: string
  seqName: string
  playerUuid: string
  playerName: string
  startedAt: number
  currentStep: number
  totalSteps: number
  finished: boolean
  cancelled: boolean
  currentStepType: string
}

export type SaveAnchorDto = {
  id?: string
  emoji: string
  name: string
  description: string
  posX: number
  posY: number
  posZ: number
  posDim: string
  radius: number
  usesJson: string
  enabled: boolean
}

export type ChoiceDecisionDto = {
  id?: string
  title: string
  question: string
  optionsJson: string
  multiResponse: boolean
  activeTarget?: string | null
  activeResponsesJson?: string | null
  activatedAt?: string | null
}

export type ChoiceHistoryEntry = {
  id: number
  ts: string
  decisionId: string
  decisionTitle: string
  playerUuid: string
  playerName: string
  optionIdx: number
  optionText: string
}

// === Tipos engines V2 ===
export type PossessionEntityDto = {
  id?: string; emoji: string; displayName: string; particle: string; sound: string
  voiceLinesJson: string; enterMsg: string; exitMsg: string; speakIntervalSec: number
  effectsJson: string; auraColor: string
}
export type PossessionActiveDto = {
  id: number; playerUuid: string; playerName: string; entityId: string
  startedAt: string; endsAt: string; lastSpeak: string
}

export type CurseDto = {
  id?: string; emoji: string; itemId: string; displayName: string
  loreJson: string; enchantmentsJson: string; curseEffectsJson: string
  whispersJson: string; particle: string; sound: string; tickIntervalSec: number
}
export type CurseBindingDto = {
  id: number; playerUuid: string; playerName: string; curseId: string
  startedAt: string; lastTick: string
}

export type VoiceClipDto = {
  id: number
  playerUuid: string
  playerName: string
  ts: number
  durationMs: number
  sizeBytes: number
  filePath: string | null
  posX: number | null
  posY: number | null
  posZ: number | null
  dimension: string | null
  transcription: string | null
  uploadStatus: 'PENDING' | 'DONE' | 'FAILED'
  transcriptionStatus: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED' | 'SKIPPED'
  language: string
  tagsJson: string
  protectedFromCleanup?: boolean
  protectionReason?: string
  protectedBy?: string
  protectedAt?: string
  createdAt: string
}

export type LootItemDto = {
  id?: number
  name: string
  itemId: string
  count: number
  nbt?: string
  rarity: 'common' | 'uncommon' | 'rare' | 'epic' | 'legendary' | 'mythic'
  weight: number
  category: string
  description?: string
  enabled: boolean
  drawCount?: number
  createdAt?: string
}

export type AutoGiftRuleDto = {
  id?: number
  name: string
  description?: string
  active: boolean
  metric: string
  topN: number
  schedule: string
  rewardsJson: string
  scaleByRank: boolean
  minValue: number
  lastRunAt?: string
  nextRunAt?: string
  runCount?: number
  createdBy?: string
  createdAt?: string
}

export type AutoGiftRunDto = {
  id: number
  ruleId: number
  ruleName: string
  ranAt: string
  winnersCount: number
  winnersJson: string
  commandsJson: string
  status: 'SUCCESS' | 'NO_WINNERS' | 'ERROR'
  errorMessage?: string
}

export type PhotoPlayerSummary = {
  authorUuid: string
  authorName: string
  photoCount: number
  totalBytes: number
  firstPhotoAt: string | null
  lastPhotoAt: string | null
  cursedCount: number
}

export type BalloonDto = {
  id: number
  playerUuid: string
  playerName: string
  text: string
  type: 'talk' | 'comic' | 'shout' | 'thought' | 'image'
  imageUrl?: string
  source: 'captured' | 'admin'
  ts: number
  dimension?: string
  posX?: number; posY?: number; posZ?: number
  createdBy?: string
  createdAt?: string
}

export type EtchedDiscDto = {
  id?: number
  title: string
  author: string
  url: string
  discColor: string
  durationSec: number
  category: string
  description: string
  addedBy: string
  playCount: number
  audioStatus?: 'NONE' | 'EXTRACTING' | 'READY' | 'FAILED'
  audioError?: string
  audioSizeBytes?: number
  createdAt?: string
}

export type ScPlayerInfo = {
  uuid: string
  name: string
  dimension: string
  scItemCount: number
  scItems: Record<string, number>
}

export type BannedItemDto = {
  id?: number
  itemId: string
  displayName: string
  reason: string
  mode: 'auto_clear' | 'broadcast' | 'silent'
  bannedBy: string
  bannedAt: string
  active: boolean
  clearCount: number
}

/** Player com info de matter — usado pelo Pehkui pra mostrar quem é elegível. */
export type PehkuiPlayerInfo = {
  uuid: string
  name: string
  dimension: string
  health: number
  posX: number; posY: number; posZ: number
  dm: number  // matéria escura
  wm: number  // matéria de wisdom
  ym: number  // matéria amarela (yang?)
}

export type PehkuiPresetDto = {
  id?: number
  name: string
  emoji: string
  description: string
  /** JSON: { "pehkui:width": 2.0, "pehkui:height": 0.5, ... } */
  scalesJson: string
  dmCost: number
  durationSec: number
  createdBy?: string
  createdAt?: string
}

/** Vídeo salvo (público ou privado). */
export type SavedVideoDto = {
  id: number
  title: string
  description: string
  filename: string
  sizeBytes: number
  durationMs: number
  isPublic: boolean
  uploadedBy: string
  source: 'rendered' | 'uploaded'
  renderJobId: string | null
  viewCount: number
  createdAt: string
  updatedAt: string
}

/** Stats agregadas de visita de um player (computado de player_snapshots). */
export type PlayerVisitDto = {
  uuid: string
  name: string
  total_snapshots: number   // ≈ horas jogadas (1 snapshot/h)
  distinct_days: number     // dias únicos em que apareceu
  estimated_sessions: number // sessões inferidas por gap > 2h
  first_seen_ts: number
  last_seen_ts: number
  days_since_last_seen: number
  tier: 'rare' | 'casual' | 'regular' | 'veteran'
}

/** Resumo agregado de um player na Voice Library. */
export type VoicePlayerSummary = {
  playerUuid: string
  playerName: string
  clipCount: number
  totalDurationMs: number
  totalBytes: number
  firstClipTs: number
  lastClipTs: number
}

export type PilgrimageStep =
  | { type: 'memorial'; refId: string }
  | { type: 'glyph'; refId: string }
  | { type: 'anchor'; refId: string }
  | { type: 'coords'; x: number; y: number; z: number; dim: string; radius?: number; label?: string }

export type PilgrimageDto = {
  id?: string
  emoji: string
  name: string
  description: string
  /** JSON.stringify(PilgrimageStep[]) */
  stepsJson: string
  timeLimitSec: number
  rewardCmd: string
  startMsg: string
  stepMsg: string
  completeMsg: string
  stepSound: string
  completeSound: string
  enabled: boolean
  createdAt?: string
}

export type PilgrimageProgressDto = {
  id: number
  playerUuid: string
  playerName: string
  pilgrimageId: string
  currentStep: number
  startedAt: string
  completedAt: string | null
}

export type RiftDto = {
  id?: string; emoji: string; name: string
  posX: number; posY: number; posZ: number; posDim: string; radius: number
  destX?: number | null; destY?: number | null; destZ?: number | null; destDim?: string | null
  randomDestRadius: number
  particle: string; particleCount: number; visualEvery: number; ambientSound: string
  preMsg: string; postMsg: string
  preEffectsJson: string; postEffectsJson: string
  cooldownSec: number; enabled: boolean; visualize: boolean; triggers?: number
}

export type GlyphDto = {
  id?: string; symbol: string; emoji: string; name: string
  posX: number; posY: number; posZ: number; posDim: string; radius: number
  loreFragment: string; rewardsJson: string
  discoverSound: string; discoverParticle: string; hintParticle: string
  hintEvery: number; hintCount: number
  discoveredByJson: string; enabled: boolean
}

export type TimeBoxDto = {
  id?: string; emoji: string; name: string; description: string
  unlockAt: string; recipient: string
  itemsJson: string; effectsJson: string
  unlockMessage: string; unlockSound: string; unlockParticle: string
  countdownEnabled: boolean; countdownEveryMin: number; countdownMsg: string
  delivered?: boolean; lastCountdownTs?: string
}

export type MemoryEchoDto = {
  id?: string; emoji: string; name: string; color: string
  posX: number; posY: number; posZ: number; posDim: string; radius: number
  ghostsJson: string; whispersJson: string
  whisperEverySec: number; ambientSound: string; ambientParticle: string
  playDurationSec: number; cooldownSec: number; enabled: boolean
}
export type MemoryEchoActiveDto = {
  id: number; echoId: string; playerUuid: string
  startedAt: string; endsAt: string; lastWhisper: string; whisperIdx: number
}

export type CalendarConfigDto = {
  id?: number; yearName: string; dayZeroTs: number; mode: 'real' | 'ingame'
  monthsJson: string; paused: boolean
}
export type SpecialDayDto = {
  id?: string; dayOfYear: number; name: string; emoji: string; color: string
  description: string; tellraw: string; sound: string
  weather: '' | 'clear' | 'rain' | 'thunder'
  setTimeTo: number; effectsJson: string; customCmd: string
  lastTriggerTs?: number
}

export type CutsceneDto = {
  id?: string; emoji: string; name: string; description: string
  stepsJson: string; targetSelector: string
  scheduledAt?: string | null
  scheduleMode: 'once' | 'daily' | 'interval'
  intervalSec?: number | null
  dailyHour?: number | null; dailyMinute?: number | null
  enabled: boolean
  lastRunAt?: string | null; runCount?: number
}

export type ForbiddenRule = {
  id?: string
  emoji: string
  name: string
  pattern: string
  matchMode: 'exact' | 'contains' | 'regex'
  caseSensitive: boolean
  target: 'speaker' | 'everyone' | 'both'
  consequencesJson: string
  cooldownSec: number
  enabled: boolean
  triggered?: number
  updatedAt?: string
}

export type ForbiddenInvocation = {
  id: number
  ts: string
  speakerUuid: string
  speakerName: string
  ruleId: string
  ruleName: string
  word: string
}

export type MadnessStateRow = {
  uuid: string
  name: string
  sanity: number
  lastTick: string
}

export type MadnessConfigDto = {
  id?: number
  enabled: boolean
  tickIntervalSec: number
  decayPerMinute: number
  whisperBroadcastMode: 'player' | 'everyone'
  worldFxEnabled: boolean
  worldFxChance: number
  zoneMessagesJson?: string
  zoneSoundsJson?: string
  /** JSON Record<zoneKey, string[]> — comandos MC executados quando o player entra na zona. */
  zoneCommandsJson?: string
  worldFxTypesJson?: string
}

export type ChatEntry = { ts: number; uuid: string; name: string; message: string }
export type CommandEntry = { ts: number; uuid: string; name: string; command: string; isPlayer: boolean }
export type SiteCommandEntry = { ts: number; origin: string; command: string; result: number }
export type SoundEntry = { namespace: string; key: string; sizeBytes: number; playId: string }
export type ParticleEntry = { key: string; sizeBytes: number; particleId: string }
export type SnapshotItem = {
  slot: number; empty?: boolean; id?: string; count?: number; name?: string;
  nbt?: string; enchantments?: { id: string; level: number }[]; contents?: SnapshotItem[]
}
export type Snapshot = {
  ts: number; uuid: string; name: string; dimension: string;
  position: { x: number; y: number; z: number };
  health: number; maxHealth: number; food: number;
  xpLevel: number; xpTotal: number; gameMode: string;
  main: SnapshotItem[]; armor: SnapshotItem[]; offhand: SnapshotItem;
  ender: SnapshotItem[]; matter?: { dm: number; wm: number; ym: number; type: string };
}
export type MapChunksResponse = {
  chunks: { x: number; z: number }[];
  dimension: string;
  viewRadius?: number;
  bbox?: { minCx: number; minCz: number; maxCx: number; maxCz: number };
}
