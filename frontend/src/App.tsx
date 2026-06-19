import { Link, NavLink, Route, Routes } from 'react-router-dom'
import { Dashboard } from './pages/Dashboard'
import { PlayerDetail } from './pages/PlayerDetail'
import { UnifiedPlayerProfilePage } from './pages/UnifiedPlayerProfilePage'
import { TesterLoginPage } from './pages/tester/TesterLoginPage'
import { TesterRegisterPage } from './pages/tester/TesterRegisterPage'
import { TesterClaimPage } from './pages/tester/TesterClaimPage'
import { TesterDashboardPage } from './pages/tester/TesterDashboardPage'
import { TesterApplyPage } from './pages/tester/TesterApplyPage'
import { TesterAdminPage } from './pages/TesterAdminPage'
import { ChangelogPublicPage } from './pages/public/ChangelogPublicPage'
import { RoadmapPublicPage } from './pages/public/RoadmapPublicPage'
import { BugLeaderboardPublicPage } from './pages/public/BugLeaderboardPublicPage'
import { WhisperQueuePage } from './pages/WhisperQueuePage'
import { TelemetryPage } from './pages/TelemetryPage'
import { HateSpeechPage } from './pages/HateSpeechPage'
import { PlayersPage } from './pages/PlayersPage'
import { ItemsPage } from './pages/ItemsPage'
import { ConsolePage } from './pages/ConsolePage'
import { WorldPage } from './pages/WorldPage'
import { OperatorsPage } from './pages/OperatorsPage'
import { PowerToolsPage } from './pages/PowerToolsPage'
import { ScriptsPage } from './pages/ScriptsPage'
import { BroadcasterPage } from './pages/BroadcasterPage'
import { AutomationPage } from './pages/AutomationPage'
import { MapPage } from './pages/MapPage'
import { ModConfigPage } from './pages/ModConfigPage'
import { HistoryPage } from './pages/HistoryPage'
import { SnapshotsPage } from './pages/SnapshotsPage'
import { FunToolsPage } from './pages/FunToolsPage'
import { LoginPage } from './pages/LoginPage'
import { CinematicPage } from './pages/CinematicPage'
import { DialogPage } from './pages/DialogPage'
import { LoreBookPage } from './pages/LoreBookPage'
import { NpcActorPage } from './pages/NpcActorPage'
import { RegionMarkersPage } from './pages/RegionMarkersPage'
import { ParticlePathPage } from './pages/ParticlePathPage'
import { MusicDirectorPage } from './pages/MusicDirectorPage'
import { FramePainterPage } from './pages/FramePainterPage'
import { InterviewPage } from './pages/InterviewPage'
import { BossbarPage } from './pages/BossbarPage'
import { StoryBeatsPage } from './pages/StoryBeatsPage'
import { TreasureHuntPage } from './pages/TreasureHuntPage'
import { SoundsPage } from './pages/SoundsPage'
import { ChroniclesPage } from './pages/ChroniclesPage'
import { GuardianPage } from './pages/GuardianPage'
import { PlayerAuraPage } from './pages/PlayerAuraPage'
import { ProphecyPage } from './pages/ProphecyPage'
import { TimeCapsulePage } from './pages/TimeCapsulePage'
import { MemorialPage } from './pages/MemorialPage'
import { PilgrimagePage } from './pages/PilgrimagePage'
import { VoiceArchivePage } from './pages/VoiceArchivePage'
import { VoiceLibraryPage } from './pages/VoiceLibraryPage'
import { VoiceConversationsPage } from './pages/VoiceConversationsPage'
import { VoiceRetentionPage } from './pages/VoiceRetentionPage'
import { PlayerVisitsPage } from './pages/PlayerVisitsPage'
import { VideoEditorPage } from './pages/VideoEditorPage'
import { VoiceMapPage } from './pages/VoiceMapPage'
import { SavedVideosPage } from './pages/SavedVideosPage'
import { WatchVideoPage, WatchGalleryPage } from './pages/WatchVideoPage'
import { PehkuiPage } from './pages/PehkuiPage'
import { BannedItemsPage } from './pages/BannedItemsPage'
import { EtchedDiscsPage } from './pages/EtchedDiscsPage'
import { SecurityCraftPage } from './pages/SecurityCraftPage'
import { CameraModPage } from './pages/CameraModPage'
import { StructuresPage } from './pages/StructuresPage'
import { BalloonsDirectorPage } from './pages/BalloonsDirectorPage'
import { PaintingsGalleryPage } from './pages/PaintingsGalleryPage'
import { VoiceRankingsPage } from './pages/VoiceRankingsPage'
import { AutoGiftsPage } from './pages/AutoGiftsPage'
import { LootTablePage } from './pages/LootTablePage'
import { EchoWhispersPage } from './pages/EchoWhispersPage'
import { BackupManagerPage } from './pages/BackupManagerPage'
import { DiceRollerPage } from './pages/DiceRollerPage'
import { HorrorPage } from './pages/HorrorPage'
import { ChoicePage } from './pages/ChoicePage'
import { RandomWheelPage } from './pages/RandomWheelPage'
import { VoiceModulatorPage } from './pages/VoiceModulatorPage'
import { DivineAtmospherePage } from './pages/DivineAtmospherePage'
import { ApocalypticAtmospherePage } from './pages/ApocalypticAtmospherePage'
import { MagicalAtmospherePage } from './pages/MagicalAtmospherePage'
import { ParticleDesignerPage } from './pages/ParticleDesignerPage'
import { CosmicHorrorPage } from './pages/CosmicHorrorPage'
import { MagicControlPage } from './pages/MagicControlPage'
import { CleanupPage } from './pages/CleanupPage'
import { EldritchSigilPage } from './pages/EldritchSigilPage'
import { SoulTetherPage } from './pages/SoulTetherPage'
import { EchoChamberPage } from './pages/EchoChamberPage'
import { ForbiddenTomePage } from './pages/ForbiddenTomePage'
import { MirrorWorldPage } from './pages/MirrorWorldPage'
import { PlayerAnalyticsPage } from './pages/PlayerAnalyticsPage'
// === novas ferramentas ===
import { ShadowWalkerPage } from './pages/ShadowWalkerPage'
import { GhostFootprintsPage } from './pages/GhostFootprintsPage'
import { SoulRecorderPage } from './pages/SoulRecorderPage'
import { SaveAnchorPage } from './pages/SaveAnchorPage'
import { CosmicPhonePage } from './pages/CosmicPhonePage'
import { MacroRecorderPage } from './pages/MacroRecorderPage'
import { ActivityTimelinePage } from './pages/ActivityTimelinePage'
import { PlayerHeatmapPage } from './pages/PlayerHeatmapPage'
import { ResourcePackBuilderPage } from './pages/ResourcePackBuilderPage'
import { ServerManagerPage } from './pages/ServerManagerPage'
// === Cosmic horror v2 ===
import { MadnessMeterPage } from './pages/MadnessMeterPage'
import { EldritchPossessionPage } from './pages/EldritchPossessionPage'
import { CursedItemForgePage } from './pages/CursedItemForgePage'
import { DimensionalRiftPage } from './pages/DimensionalRiftPage'
import { NightmareSequencePage } from './pages/NightmareSequencePage'
// === Lore v3 ===
import { ForbiddenWordsPage } from './pages/ForbiddenWordsPage'
import { GlyphDiscoveryPage } from './pages/GlyphDiscoveryPage'
import { TimeLockedBoxesPage } from './pages/TimeLockedBoxesPage'
import { MemoryEchoesPage } from './pages/MemoryEchoesPage'
import { ScheduledCutscenesPage } from './pages/ScheduledCutscenesPage'
import { QuotesWallPage } from './pages/QuotesWallPage'
import { BackroomsTrackerPage } from './pages/BackroomsTrackerPage'
import { PhotoGalleryPage } from './pages/PhotoGalleryPage'
import { KubeJsMarketplacePage } from './pages/KubeJsMarketplacePage'
import { CinemaSessionsPage } from './pages/CinemaSessionsPage'
import { WardrobePage } from './pages/WardrobePage'
import { SecurityAuditPage } from './pages/SecurityAuditPage'
import { CemeteryPage } from './pages/CemeteryPage'
import { CutsceneDirectorPage } from './pages/CutsceneDirectorPage'
import { DialogTreeBuilderPage } from './pages/DialogTreeBuilderPage'
import { NpcCostumeLibraryPage } from './pages/NpcCostumeLibraryPage'
import { MassPlayerDirectorPage } from './pages/MassPlayerDirectorPage'
import { MobEncounterEditorPage } from './pages/MobEncounterEditorPage'
import { StoryQuestDesignerPage } from './pages/StoryQuestDesignerPage'
import { ReplayStudioPage } from './pages/ReplayStudioPage'
import { useEffect, useState } from 'react'
import { useEvents } from './store/events'
import { useQuery } from '@tanstack/react-query'
import { api } from './lib/api'
import { ToastContainer } from './components/ToastContainer'
import { ConnectionStatusBanner } from './components/ConnectionStatusBanner'
import { useAuth } from './store/auth'
import { WsStatus } from './components/WsStatus'
import { useFavorites } from './store/favorites'

// Menu reorganizado em 9 categorias coesas — todos os 100+ itens preservados,
// só reagrupados por função. Ordem das categorias = fluxo natural de admin:
// começa em overview de server, passa por comando, depois ferramentas pra
// player, depois conteúdo (áudio/mídia/lore), depois sistemas dark, e
// finaliza com mundo/comunidade.
const NAV_GROUPS: { title: string; items: { to: string; icon: string; label: string; exact?: boolean }[] }[] = [
  {
    title: '📊 Servidor & Monitor',
    items: [
      { to: '/', icon: '📊', label: 'Dashboard', exact: true },
      { to: '/players', icon: '👥', label: 'Players' },
      { to: '/map', icon: '🗺', label: 'Live Map' },
      { to: '/world', icon: '🌍', label: 'Mundo' },
      { to: '/history', icon: '📚', label: 'Histórico' },
      { to: '/snapshots', icon: '📸', label: 'Snapshots' },
      { to: '/analytics', icon: '📊', label: 'Analytics' },
      { to: '/player-visits', icon: '👥', label: 'Visitas' },
      { to: '/timeline', icon: '📅', label: 'Timeline (atividade)' },
      { to: '/heatmap', icon: '🗺', label: 'Heatmap' },
      { to: '/telemetry', icon: '🧠', label: 'Telemetria (sistema nervoso)' },
    ],
  },
  {
    title: '⌨ Comando & Automação',
    items: [
      { to: '/console', icon: '⌨', label: 'Console' },
      { to: '/scripts', icon: '📜', label: 'Scripts' },
      { to: '/broadcaster', icon: '📡', label: 'Broadcaster' },
      { to: '/automation', icon: '⚙', label: 'Automation' },
      { to: '/macros', icon: '⚙', label: 'Macros' },
      { to: '/auto-gifts', icon: '🎁', label: 'Auto-Gifts' },
      { to: '/loot', icon: '🎲', label: 'Loot Table' },
      { to: '/scheduled-cutscenes', icon: '⏰', label: 'Cutscenes Agendadas' },
      { to: '/dice', icon: '🎲', label: 'Dice Roller' },
      { to: '/choice', icon: '⚖', label: 'Decisões' },
      { to: '/wheel', icon: '🎰', label: 'Roleta' },
    ],
  },
  {
    title: '🛡 Moderação & Sistema',
    items: [
      { to: '/hate-speech', icon: '🚨', label: 'Hate Speech Detector' },
      { to: '/banned-items', icon: '⛔', label: 'Item Banlist' },
      { to: '/security-audit', icon: '🛡', label: 'Security Audit' },
      { to: '/securitycraft', icon: '🔐', label: 'SecurityCraft' },
      { to: '/operators', icon: '🛡', label: 'OPs / Bans' },
      { to: '/tester-admin', icon: '🧪', label: 'Mod Testers' },
      { to: '/guardian', icon: '🛡', label: 'Guardian' },
      { to: '/cleanup', icon: '🧹', label: 'Cleanup' },
      { to: '/backups', icon: '💾', label: 'Backups' },
      { to: '/anchors', icon: '🪦', label: 'Save Anchor' },
      { to: '/server-mgr', icon: '🖥', label: 'Server Manager' },
      { to: '/pack-builder', icon: '📦', label: 'Pack Builder' },
      { to: '/mod-config', icon: '🔑', label: 'Mod Token' },
    ],
  },
  {
    title: '👥 Players & Power Tools',
    items: [
      { to: '/items', icon: '📦', label: 'Items & Encantos' },
      { to: '/tools', icon: '⚡', label: 'Power Tools' },
      { to: '/pehkui', icon: '📏', label: 'Pehkui Scaler' },
      { to: '/aura', icon: '✨', label: 'Player Aura' },
      { to: '/director', icon: '👥', label: 'Mass Director' },
      { to: '/wardrobe', icon: '👕', label: 'Wardrobe' },
      { to: '/fun', icon: '🎉', label: 'Fun Zone' },
    ],
  },
  {
    title: '🎙 Áudio & Voz',
    items: [
      { to: '/voice-modulator', icon: '🎚', label: 'Voice Modulator' },
      { to: '/voice', icon: '🎙', label: 'Voice Archive' },
      { to: '/voice-library', icon: '📚', label: 'Voice Library' },
      { to: '/voice-conversations', icon: '💬', label: 'Voice Conversations' },
      { to: '/voice-rankings', icon: '🏆', label: 'Voice Rankings' },
      { to: '/voice-retention', icon: '🗑', label: 'Voice Retention' },
      { to: '/voice-map', icon: '🗺', label: 'Voice Map' },
      { to: '/whisper-queue', icon: '⚙', label: 'Whisper Queue' },
      { to: '/etched', icon: '💿', label: 'Music Discs' },
      { to: '/music', icon: '🎼', label: 'Music' },
      { to: '/sounds', icon: '🎵', label: 'Custom Sounds' },
      { to: '/phone', icon: '📞', label: 'Cosmic Phone' },
    ],
  },
  {
    title: '📷 Galeria & Mídia',
    items: [
      { to: '/video-editor', icon: '🎬', label: 'Video Editor' },
      { to: '/videos', icon: '📺', label: 'Vídeos Salvos' },
      { to: '/photo-gallery', icon: '📷', label: 'Photo Gallery' },
    ],
  },
  {
    title: '🎬 Lore & RP Studio',
    items: [
      { to: '/cinematic', icon: '🎬', label: 'Cutscenes' },
      { to: '/cutscene-director', icon: '🎞', label: 'Cutscene Director' },
      { to: '/story-beats', icon: '🌌', label: 'Story Beats' },
      { to: '/dialog', icon: '💭', label: 'Dialogs' },
      { to: '/dialog-trees', icon: '💬', label: 'Dialog Trees' },
      { to: '/interview', icon: '🗣', label: 'Interview' },
      { to: '/npcs', icon: '🎭', label: 'NPC Actors' },
      { to: '/costumes', icon: '🎭', label: 'NPC Costumes' },
      { to: '/encounters', icon: '🐺', label: 'Mob Encounters' },
      { to: '/story-quests', icon: '📖', label: 'Story Quests' },
      { to: '/replays', icon: '🎬', label: 'Replay Studio' },
      { to: '/balloons', icon: '💬', label: 'Balloons Director' },
      { to: '/books', icon: '📖', label: 'Lore Books' },
      { to: '/bossbars', icon: '📊', label: 'Bossbars' },
      { to: '/treasure', icon: '🏴‍☠', label: 'Treasure Hunt' },
      { to: '/chronicles', icon: '📚', label: 'Chronicles' },
      { to: '/prophecy', icon: '🔮', label: 'Profecias' },
      { to: '/capsules', icon: '⏳', label: 'Cápsulas' },
      { to: '/memorials', icon: '🗿', label: 'Memoriais' },
      { to: '/cemetery', icon: '⚰️', label: 'Cemitério' },
      { to: '/pilgrimages', icon: '⚔', label: 'Pilgrimages' },
    ],
  },
  {
    title: '👁 Eldritch & Horror',
    items: [
      { to: '/horror', icon: '👻', label: 'Horror' },
      { to: '/cosmic', icon: '🜲', label: 'Cosmic Horror' },
      { to: '/magic-control', icon: '✦', label: 'Magic Control' },
      { to: '/divine', icon: '✨', label: 'Divine' },
      { to: '/apocalyptic', icon: '🌋', label: 'Apocalyptic' },
      { to: '/magical', icon: '🔮', label: 'Magical' },
      { to: '/madness', icon: '🧠', label: 'Madness Meter' },
      { to: '/possession', icon: '👁', label: 'Eldritch Possession' },
      { to: '/curse', icon: '🩸', label: 'Cursed Forge' },
      { to: '/sigil', icon: '⛤', label: 'Eldritch Sigil' },
      { to: '/tether', icon: '⛓', label: 'Soul Tether' },
      { to: '/echoes', icon: '🕯', label: 'Echo Whispers' },
      { to: '/echo-chamber', icon: '🌀', label: 'Echo Chamber' },
      { to: '/echoes-mem', icon: '🕯', label: 'Memory Echoes' },
      { to: '/mirror', icon: '🪞', label: 'Mirror World' },
      { to: '/tome', icon: '📖', label: 'Forbidden Tome' },
      { to: '/forbidden', icon: '🔮', label: 'Forbidden Words' },
      { to: '/rift', icon: '🌀', label: 'Dimensional Rift' },
      { to: '/nightmare', icon: '😱', label: 'Nightmare Sequence' },
      { to: '/shadow', icon: '🌑', label: 'Shadow Walker' },
      { to: '/footprints', icon: '👣', label: 'Ghost Footprints' },
      { to: '/souls', icon: '👤', label: 'Soul Recorder' },
      { to: '/glyphs', icon: '✦', label: 'Glyph Discovery' },
      { to: '/boxes', icon: '🎁', label: 'Time-Locked Boxes' },
    ],
  },
  {
    title: '🌍 Mundo, FX & Comunidade',
    items: [
      { to: '/structures', icon: '🗺', label: 'Structures' },
      { to: '/regions', icon: '📍', label: 'Regions' },
      { to: '/backrooms-tracker', icon: '🕳', label: 'Backrooms Tracker' },
      { to: '/particle-designer', icon: '🎨', label: 'Particle Designer' },
      { to: '/particle-path', icon: '🎨', label: 'Particle Path' },
      { to: '/kubejs-market', icon: '⚗️', label: 'KubeJS Scripts' },
    ],
  },
]

export default function App() {
  const token = useAuth((s) => s.token)
  const connect = useEvents((s) => s.connect)
  const [mobileOpen, setMobileOpen] = useState(false)

  useEffect(() => { if (token) connect() }, [token])

  // Rotas PÚBLICAS — não exigem login admin, layout standalone (sem sidebar).
  // /watch          → galeria
  // /watch/:id      → player do vídeo
  // /tester ou /tester/*  → área dos mod testers (auth próprio, separado do admin)
  //   ATENÇÃO: usar `=== '/tester'` ou `startsWith('/tester/')` (com barra) pra
  //   NÃO casar com /tester-admin que é rota admin normal.
  // /changelog      → release notes públicas (sem auth)
  // /roadmap        → roadmap público com votação (sem auth)
  // /leaderboard    → ranking de caçadores de bugs (sem auth)
  const path = typeof window !== 'undefined' ? window.location.pathname : ''
  const publicPath = path.startsWith('/watch')
    || path === '/tester'
    || path.startsWith('/tester/')
    || path.startsWith('/changelog')
    || path.startsWith('/roadmap')
    || path.startsWith('/leaderboard')
  if (publicPath) {
    return (
      <>
        <ConnectionStatusBanner />
        <Routes>
          <Route path="/watch" element={<WatchGalleryPage />} />
          <Route path="/watch/:id" element={<WatchVideoPage />} />
          <Route path="/tester" element={<TesterLoginPage />} />
          <Route path="/tester/login" element={<TesterLoginPage />} />
          <Route path="/tester/apply" element={<TesterApplyPage />} />
          <Route path="/tester/register" element={<TesterRegisterPage />} />
          <Route path="/tester/claim" element={<TesterClaimPage />} />
          <Route path="/tester/dashboard" element={<TesterDashboardPage />} />
          <Route path="/changelog" element={<ChangelogPublicPage />} />
          <Route path="/roadmap" element={<RoadmapPublicPage />} />
          <Route path="/leaderboard" element={<BugLeaderboardPublicPage />} />
        </Routes>
        <ToastContainer />
      </>
    )
  }

  if (!token) {
    return (
      <>
        <ConnectionStatusBanner />
        <div className="fixed top-0 left-0 right-0 z-[60] px-3 py-1 text-center text-[11px] font-bold text-amber-100 bg-gradient-to-r from-amber-700/95 via-amber-600/95 to-amber-700/95 border-b border-amber-400/40 shadow-lg backdrop-blur-sm">
          ⚠ VERSÃO DE TESTE — instável, dados podem ser perdidos a qualquer momento
        </div>
        <LoginPage />
        <ToastContainer />
      </>
    )
  }

  return (
    <div className="min-h-screen md:flex pt-7">
      <ConnectionStatusBanner />
      {/* Banner versão teste — fixo no topo, sobreposto ao resto via z-index */}
      <div className="fixed top-0 left-0 right-0 z-[60] px-3 py-1 text-center text-[11px] font-bold text-amber-100 bg-gradient-to-r from-amber-700/95 via-amber-600/95 to-amber-700/95 border-b border-amber-400/40 shadow-lg backdrop-blur-sm">
        ⚠ VERSÃO DE TESTE — instável, dados podem ser perdidos a qualquer momento
      </div>

      {/* Top bar mobile */}
      <div className="md:hidden sticky top-7 z-30 flex items-center gap-3 px-3 py-2 bg-liberthia-900/90 backdrop-blur-md border-b border-liberthia-600/40">
        <button className="btn-ghost btn-sm" onClick={() => setMobileOpen(true)} title="Menu">☰</button>
        <div className="flex items-center gap-2 flex-1 min-w-0">
          <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-liberthia-400 to-liberthia-600 flex items-center justify-center text-sm shadow shadow-liberthia-500/40 shrink-0">⚛</div>
          <span className="font-bold gradient-text truncate">Liberthia</span>
        </div>
      </div>

      <Sidebar mobileOpen={mobileOpen} onMobileClose={() => setMobileOpen(false)} />

      <main className="flex-1 p-3 md:p-6 overflow-auto md:max-h-screen min-w-0">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/players" element={<PlayersPage />} />
          <Route path="/items" element={<ItemsPage />} />
          <Route path="/console" element={<ConsolePage />} />
          <Route path="/world" element={<WorldPage />} />
          <Route path="/operators" element={<OperatorsPage />} />
          <Route path="/tools" element={<PowerToolsPage />} />
          <Route path="/scripts" element={<ScriptsPage />} />
          <Route path="/broadcaster" element={<BroadcasterPage />} />
          <Route path="/automation" element={<AutomationPage />} />
          <Route path="/map" element={<MapPage />} />
          <Route path="/history" element={<HistoryPage />} />
          <Route path="/snapshots" element={<SnapshotsPage />} />
          <Route path="/fun" element={<FunToolsPage />} />
          <Route path="/cinematic" element={<CinematicPage />} />
          <Route path="/dialog" element={<DialogPage />} />
          <Route path="/npcs" element={<NpcActorPage />} />
          <Route path="/regions" element={<RegionMarkersPage />} />
          <Route path="/books" element={<LoreBookPage />} />
          <Route path="/music" element={<MusicDirectorPage />} />
          <Route path="/particle-path" element={<ParticlePathPage />} />
          <Route path="/frames" element={<FramePainterPage />} />
          <Route path="/interview" element={<InterviewPage />} />
          <Route path="/bossbars" element={<BossbarPage />} />
          <Route path="/story-beats" element={<StoryBeatsPage />} />
          <Route path="/treasure" element={<TreasureHuntPage />} />
          <Route path="/sounds" element={<SoundsPage />} />
          <Route path="/chronicles" element={<ChroniclesPage />} />
          <Route path="/guardian" element={<GuardianPage />} />
          <Route path="/aura" element={<PlayerAuraPage />} />
          <Route path="/prophecy" element={<ProphecyPage />} />
          <Route path="/capsules" element={<TimeCapsulePage />} />
          <Route path="/memorials" element={<MemorialPage />} />
          <Route path="/pilgrimages" element={<PilgrimagePage />} />
          <Route path="/voice" element={<VoiceArchivePage />} />
          <Route path="/voice-library" element={<VoiceLibraryPage />} />
          <Route path="/voice-conversations" element={<VoiceConversationsPage />} />
          <Route path="/voice-retention" element={<VoiceRetentionPage />} />
          <Route path="/player-visits" element={<PlayerVisitsPage />} />
          <Route path="/video-editor" element={<VideoEditorPage />} />
          <Route path="/voice-map" element={<VoiceMapPage />} />
          <Route path="/videos" element={<SavedVideosPage />} />
          <Route path="/pehkui" element={<PehkuiPage />} />
          <Route path="/banned-items" element={<BannedItemsPage />} />
          <Route path="/etched" element={<EtchedDiscsPage />} />
          <Route path="/securitycraft" element={<SecurityCraftPage />} />
          <Route path="/camera-mod" element={<CameraModPage />} />
          <Route path="/structures" element={<StructuresPage />} />
          <Route path="/balloons" element={<BalloonsDirectorPage />} />
          <Route path="/paintings" element={<PaintingsGalleryPage />} />
          <Route path="/voice-rankings" element={<VoiceRankingsPage />} />
          <Route path="/auto-gifts" element={<AutoGiftsPage />} />
          <Route path="/loot" element={<LootTablePage />} />
          <Route path="/echoes" element={<EchoWhispersPage />} />
          <Route path="/backups" element={<BackupManagerPage />} />
          <Route path="/dice" element={<DiceRollerPage />} />
          <Route path="/horror" element={<HorrorPage />} />
          <Route path="/choice" element={<ChoicePage />} />
          <Route path="/wheel" element={<RandomWheelPage />} />
          <Route path="/voice-modulator" element={<VoiceModulatorPage />} />
          <Route path="/divine" element={<DivineAtmospherePage />} />
          <Route path="/apocalyptic" element={<ApocalypticAtmospherePage />} />
          <Route path="/magical" element={<MagicalAtmospherePage />} />
          <Route path="/particle-designer" element={<ParticleDesignerPage />} />
          <Route path="/cosmic" element={<CosmicHorrorPage />} />
          <Route path="/magic-control" element={<MagicControlPage />} />
          <Route path="/cleanup" element={<CleanupPage />} />
          <Route path="/sigil" element={<EldritchSigilPage />} />
          <Route path="/tether" element={<SoulTetherPage />} />
          <Route path="/echo-chamber" element={<EchoChamberPage />} />
          <Route path="/tome" element={<ForbiddenTomePage />} />
          <Route path="/mirror" element={<MirrorWorldPage />} />
          <Route path="/analytics" element={<PlayerAnalyticsPage />} />
          {/* === ferramentas novas === */}
          <Route path="/shadow" element={<ShadowWalkerPage />} />
          <Route path="/footprints" element={<GhostFootprintsPage />} />
          <Route path="/souls" element={<SoulRecorderPage />} />
          <Route path="/anchors" element={<SaveAnchorPage />} />
          <Route path="/phone" element={<CosmicPhonePage />} />
          <Route path="/macros" element={<MacroRecorderPage />} />
          <Route path="/timeline" element={<ActivityTimelinePage />} />
          <Route path="/heatmap" element={<PlayerHeatmapPage />} />
          <Route path="/pack-builder" element={<ResourcePackBuilderPage />} />
          <Route path="/server-mgr" element={<ServerManagerPage />} />
          <Route path="/mod-config" element={<ModConfigPage />} />
          {/* === Cosmic Horror v2 === */}
          <Route path="/madness" element={<MadnessMeterPage />} />
          <Route path="/possession" element={<EldritchPossessionPage />} />
          <Route path="/curse" element={<CursedItemForgePage />} />
          <Route path="/rift" element={<DimensionalRiftPage />} />
          <Route path="/nightmare" element={<NightmareSequencePage />} />
          {/* === Lore v3 === */}
          <Route path="/forbidden" element={<ForbiddenWordsPage />} />
          <Route path="/glyphs" element={<GlyphDiscoveryPage />} />
          <Route path="/boxes" element={<TimeLockedBoxesPage />} />
          <Route path="/echoes-mem" element={<MemoryEchoesPage />} />
          <Route path="/scheduled-cutscenes" element={<ScheduledCutscenesPage />} />
          <Route path="/quotes-wall" element={<QuotesWallPage />} />
          <Route path="/backrooms-tracker" element={<BackroomsTrackerPage />} />
          <Route path="/photo-gallery" element={<PhotoGalleryPage />} />
          <Route path="/kubejs-market" element={<KubeJsMarketplacePage />} />
          <Route path="/cinema" element={<CinemaSessionsPage />} />
          <Route path="/wardrobe" element={<WardrobePage />} />
          <Route path="/security-audit" element={<SecurityAuditPage />} />
          <Route path="/cemetery" element={<CemeteryPage />} />
          <Route path="/cutscene-director" element={<CutsceneDirectorPage />} />
          <Route path="/dialog-trees" element={<DialogTreeBuilderPage />} />
          <Route path="/costumes" element={<NpcCostumeLibraryPage />} />
          <Route path="/director" element={<MassPlayerDirectorPage />} />
          <Route path="/encounters" element={<MobEncounterEditorPage />} />
          <Route path="/story-quests" element={<StoryQuestDesignerPage />} />
          <Route path="/replays" element={<ReplayStudioPage />} />
          <Route path="/player/:uuid" element={<PlayerDetail />} />
          <Route path="/profile/:uuid" element={<UnifiedPlayerProfilePage />} />
          <Route path="/tester-admin" element={<TesterAdminPage />} />
          <Route path="/whisper-queue" element={<WhisperQueuePage />} />
          <Route path="/telemetry" element={<TelemetryPage />} />
          <Route path="/hate-speech" element={<HateSpeechPage />} />
        </Routes>
      </main>
      <ToastContainer />
    </div>
  )
}

function Sidebar({ mobileOpen, onMobileClose }: { mobileOpen: boolean; onMobileClose: () => void }) {
  const logout = useAuth((s) => s.logout)
  const { favorites, toggle: toggleFavorite, isFavorite } = useFavorites()
  const [search, setSearch] = useState('')
  const serverQ = useQuery({
    queryKey: ['serverInfo'],
    queryFn: api.serverInfo,
    retry: false,
    refetchInterval: 4000,
  })
  const errMsg = (serverQ.error as Error | undefined)?.message ?? ''
  const modOffline = !!serverQ.error && (errMsg.includes('mod_offline') || errMsg.includes('503'))

  // Flat list de TODOS items pra busca + lookup
  const allItems = NAV_GROUPS.flatMap(g => g.items)
  const allItemsByPath = Object.fromEntries(allItems.map(it => [it.to, it]))

  // Itens favoritos preservando a ordem que foram favoritados
  const favoriteItems = favorites
    .map(path => allItemsByPath[path])
    .filter(Boolean) as typeof allItems

  // Search filter — case insensitive, matches label OR rota
  const q = search.trim().toLowerCase()
  const searching = q.length > 0
  const filteredGroups = searching
    ? NAV_GROUPS.map(g => ({
        ...g,
        items: g.items.filter(it =>
          it.label.toLowerCase().includes(q) ||
          it.to.toLowerCase().includes(q)
        ),
      })).filter(g => g.items.length > 0)
    : NAV_GROUPS

  return (
    <>
      {/* Backdrop mobile */}
      {mobileOpen && (
        <div className="md:hidden fixed inset-0 z-40 bg-black/60 backdrop-blur-sm" onClick={onMobileClose} />
      )}

      <aside className={`
        fixed md:sticky top-0 left-0 z-50
        w-64 max-w-[85vw]
        bg-gradient-to-b from-liberthia-800/95 to-liberthia-900/95 md:from-liberthia-800/60 md:to-liberthia-900/80
        border-r border-liberthia-600/40 backdrop-blur-md
        flex flex-col p-4 gap-1
        max-h-screen overflow-y-auto
        transition-transform duration-300 ease-out
        ${mobileOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}
      `}>
        <div className="flex items-center justify-between mb-2">
          <Link to="/" className="flex items-center gap-2 px-2 py-3" onClick={onMobileClose}>
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-liberthia-400 to-liberthia-600 flex items-center justify-center text-xl shadow-lg shadow-liberthia-500/40 ring-glow">⚛</div>
            <div>
              <div className="font-bold text-base gradient-text">Liberthia</div>
              <div className="text-[10px] text-liberthia-300/70 uppercase tracking-widest">Admin Panel</div>
            </div>
          </Link>
          <button className="md:hidden btn-ghost btn-sm" onClick={onMobileClose} title="Fechar">✕</button>
        </div>

        {/* 🔍 Search */}
        <div className="relative mb-2">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="🔍 Buscar tela…"
            className="input w-full text-xs pr-7" />
          {search && (
            <button
              onClick={() => setSearch('')}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-liberthia-300/50 hover:text-liberthia-100"
              title="Limpar">✕</button>
          )}
        </div>

        <nav className="flex-1 flex flex-col gap-3">
          {/* ⭐ Favoritos (só mostra se não tá buscando E tem favoritos) */}
          {!searching && favoriteItems.length > 0 && (
            <div>
              <div className="text-[10px] uppercase tracking-widest text-amber-300/70 font-bold px-3 py-1 flex items-center gap-1">
                ⭐ Favoritos
              </div>
              <div className="flex flex-col gap-0.5">
                {favoriteItems.map((it) => (
                  <NavItemWithStar key={it.to} item={it} onClose={onMobileClose}
                    isFavorite={true} onToggleFavorite={toggleFavorite} />
                ))}
              </div>
            </div>
          )}

          {/* Grupos normais (filtrados se buscando) */}
          {filteredGroups.map((g) => (
            <div key={g.title}>
              <div className="text-[10px] uppercase tracking-widest text-liberthia-300/40 font-bold px-3 py-1">{g.title}</div>
              <div className="flex flex-col gap-0.5">
                {g.items.map((it) => (
                  <NavItemWithStar key={it.to} item={it} onClose={onMobileClose}
                    isFavorite={isFavorite(it.to)} onToggleFavorite={toggleFavorite} />
                ))}
              </div>
            </div>
          ))}

          {/* Empty state da busca */}
          {searching && filteredGroups.length === 0 && (
            <div className="text-center py-8 text-[11px] text-liberthia-300/50 italic">
              Nenhuma tela encontrada pra "{search}"
            </div>
          )}
        </nav>

        <div className="mt-auto pt-3 border-t border-liberthia-600/30 space-y-2">
          <StatusRow label="Mod" ok={!modOffline} okLabel="online" offLabel="offline" />
          <WsStatus />
          {!modOffline && serverQ.data && (
            <div className="text-[10px] text-liberthia-300/60 px-1 pt-1 space-y-0.5">
              <div className="flex justify-between"><span>TPS</span><span className="font-mono">{serverQ.data.tps?.toFixed(1)}</span></div>
              <div className="flex justify-between"><span>Players</span><span className="font-mono">{serverQ.data.playerCount}/{serverQ.data.maxPlayers}</span></div>
            </div>
          )}
          <button className="btn-ghost btn-sm w-full mt-2" onClick={logout}>🔒 Logout</button>
        </div>
      </aside>
    </>
  )
}

/**
 * Nav link com botão de estrela ⭐ pra favoritar.
 * Estrela aparece em hover (ou sempre se já favoritado) à direita.
 */
function NavItemWithStar({
  item, onClose, isFavorite, onToggleFavorite,
}: {
  item: { to: string; icon: string; label: string; exact?: boolean }
  onClose: () => void
  isFavorite: boolean
  onToggleFavorite: (path: string) => void
}) {
  return (
    <div className="group relative flex items-center">
      <NavLink
        to={item.to}
        end={item.exact}
        onClick={onClose}
        className={({ isActive }) => `nav-link flex-1 pr-7 ${isActive ? 'nav-link-active' : ''}`}
      >
        <span className="text-base">{item.icon}</span>
        <span>{item.label}</span>
      </NavLink>
      <button
        onClick={(e) => {
          e.preventDefault()
          e.stopPropagation()
          onToggleFavorite(item.to)
        }}
        className={`absolute right-1 top-1/2 -translate-y-1/2 w-5 h-5 flex items-center justify-center rounded text-xs transition-opacity ${
          isFavorite ? 'opacity-100 text-amber-300' : 'opacity-0 group-hover:opacity-60 text-liberthia-300/60 hover:text-amber-300'
        }`}
        title={isFavorite ? 'Remover dos favoritos' : 'Favoritar'}>
        {isFavorite ? '⭐' : '☆'}
      </button>
    </div>
  )
}

function StatusRow({ label, ok, okLabel, offLabel }: { label: string; ok: boolean; okLabel: string; offLabel: string }) {
  return (
    <div className="flex items-center justify-between text-[11px] px-1">
      <span className="text-liberthia-300/70">{label}</span>
      <span className={`badge ${ok ? 'badge-green' : 'badge-red'}`}>
        <span className={`w-1.5 h-1.5 rounded-full ${ok ? 'bg-emerald-400' : 'bg-red-400'} animate-pulse`} />
        {ok ? okLabel : offLabel}
      </span>
    </div>
  )
}
