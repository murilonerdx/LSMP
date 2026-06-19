package br.com.murilo.liberthia.admin.tester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lógica consolidada de MVP-2 (mod packages), MVP-3 (beta items),
 * MVP-4 (bug reports) e MVP-5 (rewards) do sistema de mod testers.
 *
 * Storage de ZIP fica em data/tester-packages/<id>.zip.
 */
@Service
public class TesterContentService {

    private static final Logger LOG = LoggerFactory.getLogger(TesterContentService.class);

    public interface PackageRepo extends JpaRepository<ModPackage, Long> {
        List<ModPackage> findByEnabledTrueOrderByUploadedAtDesc();
        List<ModPackage> findAllByOrderByUploadedAtDesc();
    }

    public interface BetaItemRepo extends JpaRepository<BetaItem, Long> {
        List<BetaItem> findByEnabledTrueOrderByCreatedAtDesc();
        List<BetaItem> findAllByOrderByCreatedAtDesc();
    }

    public interface BugReportRepo extends JpaRepository<BugReport, Long> {
        List<BugReport> findByTesterMcNameIgnoreCaseOrderByCreatedAtDesc(String mcName);
        List<BugReport> findByStatusOrderByCreatedAtAsc(BugReport.Status status);
        List<BugReport> findAllByOrderByCreatedAtDesc();
    }

    public interface RewardRepo extends JpaRepository<Reward, Long> {
        List<Reward> findByEnabledTrueOrderByCostPointsAsc();
        List<Reward> findAllByOrderByCostPointsAsc();
    }

    public interface RedemptionRepo extends JpaRepository<RewardRedemption, Long> {
        List<RewardRedemption> findByTesterMcNameIgnoreCaseOrderByRedeemedAtDesc(String mcName);
        List<RewardRedemption> findAllByOrderByRedeemedAtDesc();
        long countByTesterMcNameAndRewardId(String mcName, Long rewardId);
    }

    public interface Model3DRepo extends JpaRepository<Model3D, Long> {
        List<Model3D> findByEnabledTrueOrderByUploadedAtDesc();
        List<Model3D> findAllByOrderByUploadedAtDesc();
    }

    public interface BetaAudioRepo extends JpaRepository<BetaAudio, Long> {
        List<BetaAudio> findByEnabledTrueOrderByUploadedAtDesc();
        List<BetaAudio> findAllByOrderByUploadedAtDesc();
    }

    public interface BetaVoteRepo extends JpaRepository<BetaVote, Long> {
        Optional<BetaVote> findByVoterMcNameAndTargetAndTargetId(String voterMcName, BetaVote.Target target, Long targetId);
        long countByTargetAndTargetIdAndVote(BetaVote.Target target, Long targetId, BetaVote.Vote vote);
        List<BetaVote> findByTargetAndTargetIdIn(BetaVote.Target target, List<Long> targetIds);
    }

    private final PackageRepo packageRepo;
    private final BetaItemRepo betaItemRepo;
    private final BugReportRepo bugRepo;
    private final RewardRepo rewardRepo;
    private final RedemptionRepo redemptionRepo;
    private final Model3DRepo modelRepo;
    private final BetaAudioRepo audioRepo;
    private final BetaVoteRepo voteRepo;
    private final ModTesterService testerSvc;
    private final org.springframework.beans.factory.ObjectProvider<TesterExtensionsService> extProvider;

    @Value("${tester.packages-dir:data/tester-packages}")
    private String packagesDir;

    @Value("${tester.models-dir:data/tester-models}")
    private String modelsDir;

    @Value("${tester.audios-dir:data/tester-audios}")
    private String audiosDir;

    public TesterContentService(PackageRepo packageRepo, BetaItemRepo betaItemRepo,
                                BugReportRepo bugRepo, RewardRepo rewardRepo,
                                RedemptionRepo redemptionRepo, Model3DRepo modelRepo,
                                BetaAudioRepo audioRepo, BetaVoteRepo voteRepo,
                                ModTesterService testerSvc,
                                org.springframework.beans.factory.ObjectProvider<TesterExtensionsService> extProvider) {
        this.packageRepo = packageRepo;
        this.betaItemRepo = betaItemRepo;
        this.bugRepo = bugRepo;
        this.rewardRepo = rewardRepo;
        this.redemptionRepo = redemptionRepo;
        this.modelRepo = modelRepo;
        this.audioRepo = audioRepo;
        this.voteRepo = voteRepo;
        this.testerSvc = testerSvc;
        this.extProvider = extProvider;
    }

    /** Notifica o tester usando TesterExtensionsService (lazy via ObjectProvider pra evitar ciclo). */
    private void notifyTester(String mc, TesterNotification.Type type, String title, String body, String link) {
        try {
            TesterExtensionsService ext = extProvider.getIfAvailable();
            if (ext != null) ext.notify(mc, type, title, body, link);
        } catch (Exception ignored) {}
    }

    // ============================================================ //
    // MVP-2: MOD PACKAGES
    // ============================================================ //

    @Transactional
    public ModPackage uploadPackage(String name, String version, String description,
                                    MultipartFile file, String uploadedBy) throws IOException {
        Path dir = Paths.get(packagesDir);
        Files.createDirectories(dir);

        ModPackage pkg = new ModPackage();
        pkg.setName(name);
        pkg.setVersion(version);
        pkg.setDescription(description);
        pkg.setFilename(file.getOriginalFilename() == null ? "package.zip" : file.getOriginalFilename());
        pkg.setSizeBytes(file.getSize());
        pkg.setUploadedBy(uploadedBy);

        // Bug histórico: o save() inicial falhava porque storagePath é
        // @Column(nullable=false) mas a gente queria usar o ID gerado pelo
        // banco pra montar o nome. Fix: usa UUID antes do save, eliminando
        // o chicken-and-egg.
        String storagePath = java.util.UUID.randomUUID().toString().replace("-", "") + ".zip";
        pkg.setStoragePath(storagePath);
        pkg = packageRepo.save(pkg);

        Path target = dir.resolve(storagePath);
        try {
            Files.write(target, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            // Se a gravação do ZIP em disco falhar, desfaz o INSERT pra não
            // deixar registro órfão apontando pra arquivo inexistente.
            packageRepo.delete(pkg);
            throw e;
        }
        return pkg;
    }

    public byte[] readPackage(Long id) throws IOException {
        ModPackage pkg = packageRepo.findById(id).orElse(null);
        if (pkg == null || pkg.getStoragePath() == null) return null;
        Path file = Paths.get(packagesDir, pkg.getStoragePath());
        if (!Files.exists(file)) return null;
        // Incrementa contador (best-effort)
        try {
            pkg.setDownloadCount(pkg.getDownloadCount() + 1);
            packageRepo.save(pkg);
        } catch (Exception ignored) {}
        return Files.readAllBytes(file);
    }

    /**
     * Caminho físico do ZIP no disco, pra streaming via Spring sem carregar
     * tudo na memória. Retorna null se não existir.
     *
     * Antes o download usava {@code readPackage()} que faz
     * {@code Files.readAllBytes()} — pra um mod pack de 100MB, alocava 100MB
     * de heap e o front-end tinha que fazer {@code blob()} que aloca outros
     * 100MB no browser. Resultado: OOM no Spring (heap limit 1.28GB) ou
     * "Failed to fetch" no browser quando a memória apertava.
     *
     * Streaming via {@link java.io.FileInputStream} resolve: zero buffer no
     * server, browser pega bytes conforme chegam e escreve direto no disco.
     */
    public Path getPackagePath(Long id) {
        ModPackage pkg = packageRepo.findById(id).orElse(null);
        if (pkg == null || pkg.getStoragePath() == null) return null;
        Path file = Paths.get(packagesDir, pkg.getStoragePath());
        if (!Files.exists(file)) return null;
        // Best-effort increment do contador de downloads
        try {
            pkg.setDownloadCount(pkg.getDownloadCount() + 1);
            packageRepo.save(pkg);
        } catch (Exception ignored) {}
        return file;
    }

    public Optional<ModPackage> getPackage(Long id) { return packageRepo.findById(id); }

    /**
     * Checa rápido se o ZIP do package ainda existe em disco. Usado pelos DTOs
     * pra expor `fileExists` na listagem — o front desabilita o botão de
     * download quando o arquivo sumiu (raro mas acontece se o volume foi
     * pruned, ou se o pacote foi uploadado antes do fix de volume persistente
     * em v0.1.24).
     *
     * Não incrementa download counter (diferente de {@link #getPackagePath}).
     */
    public boolean packageFileExists(ModPackage pkg) {
        if (pkg == null || pkg.getStoragePath() == null) return false;
        try {
            return Files.exists(Paths.get(packagesDir, pkg.getStoragePath()));
        } catch (Exception e) {
            return false;
        }
    }

    public List<ModPackage> listPackages(boolean onlyEnabled) {
        return onlyEnabled ? packageRepo.findByEnabledTrueOrderByUploadedAtDesc()
                : packageRepo.findAllByOrderByUploadedAtDesc();
    }

    @Transactional
    public void deletePackage(Long id) {
        ModPackage pkg = packageRepo.findById(id).orElse(null);
        if (pkg == null) return;
        if (pkg.getStoragePath() != null) {
            try { Files.deleteIfExists(Paths.get(packagesDir, pkg.getStoragePath())); }
            catch (Exception ignored) {}
        }
        packageRepo.deleteById(id);
    }

    @Transactional
    public ModPackage togglePackageEnabled(Long id) {
        ModPackage pkg = packageRepo.findById(id).orElseThrow();
        pkg.setEnabled(!pkg.isEnabled());
        return packageRepo.save(pkg);
    }

    // ============================================================ //
    // MVP-3: BETA ITEMS
    // ============================================================ //

    @Transactional
    public BetaItem createBetaItem(BetaItem in) {
        // @PrePersist seta createdAt automaticamente
        return betaItemRepo.save(in);
    }

    @Transactional
    public BetaItem updateBetaItem(Long id, BetaItem in) {
        BetaItem existing = betaItemRepo.findById(id).orElseThrow();
        if (in.getName() != null) existing.setName(in.getName());
        if (in.getItemId() != null) existing.setItemId(in.getItemId());
        if (in.getKind() != null) existing.setKind(in.getKind());
        if (in.getDescription() != null) existing.setDescription(in.getDescription());
        if (in.getLore() != null) existing.setLore(in.getLore());
        if (in.getPropertiesJson() != null) existing.setPropertiesJson(in.getPropertiesJson());
        if (in.getRecipeJson() != null) existing.setRecipeJson(in.getRecipeJson());
        if (in.getEffectsJson() != null) existing.setEffectsJson(in.getEffectsJson());
        if (in.getGiveCommand() != null) existing.setGiveCommand(in.getGiveCommand());
        if (in.getImageUrl() != null) existing.setImageUrl(in.getImageUrl());
        if (in.getCategory() != null) existing.setCategory(in.getCategory());
        existing.setEnabled(in.isEnabled());
        return betaItemRepo.save(existing);
    }

    public List<BetaItem> listBetaItems(boolean onlyEnabled) {
        return onlyEnabled ? betaItemRepo.findByEnabledTrueOrderByCreatedAtDesc()
                : betaItemRepo.findAllByOrderByCreatedAtDesc();
    }

    public Optional<BetaItem> getBetaItem(Long id) { return betaItemRepo.findById(id); }

    @Transactional
    public void deleteBetaItem(Long id) { betaItemRepo.deleteById(id); }

    // ============================================================ //
    // MVP-4: BUG REPORTS
    // ============================================================ //

    @Transactional
    public BugReport createBug(BugReport in) {
        in.setStatus(BugReport.Status.PENDING);
        in.setPointsAwarded(0);
        return bugRepo.save(in);
    }

    public List<BugReport> listBugsForTester(String mcName) {
        return bugRepo.findByTesterMcNameIgnoreCaseOrderByCreatedAtDesc(mcName);
    }

    public List<BugReport> listAllBugs() {
        return bugRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<BugReport> listPendingBugs() {
        return bugRepo.findByStatusOrderByCreatedAtAsc(BugReport.Status.PENDING);
    }

    public Optional<BugReport> getBug(Long id) { return bugRepo.findById(id); }

    /**
     * Tester edita o PRÓPRIO bug. Só funciona se:
     *   - o bug existe
     *   - foi reportado pelo mcName que tá tentando editar (anti-impersonation)
     *   - status ainda é PENDING (depois de admin confirmar/rejeitar, lock — admin
     *     já valeu o esforço de triar, não pode mexer mais)
     *
     * Aceita campos parcial — qualquer campo NULL no `in` mantém o valor atual.
     * Retorna o bug atualizado, ou lança IllegalStateException se não pode editar.
     */
    @Transactional
    public BugReport updateBugFromTester(Long id, String mcName, BugReport in) {
        BugReport b = bugRepo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("bug não existe"));
        if (b.getTesterMcName() == null
                || !b.getTesterMcName().equalsIgnoreCase(mcName)) {
            throw new IllegalStateException("você só pode editar seus próprios bugs");
        }
        if (b.getStatus() != BugReport.Status.PENDING) {
            throw new IllegalStateException(
                    "bug já foi triado pelo admin (" + b.getStatus() + ") — não pode mais editar");
        }
        // Campos editáveis pelo tester (não mexe em status/pointsAwarded/triagedBy/etc).
        if (in.getTitle() != null && !in.getTitle().isBlank()) b.setTitle(in.getTitle().trim());
        if (in.getDescription() != null && !in.getDescription().isBlank())
            b.setDescription(in.getDescription().trim());
        if (in.getStepsToReproduce() != null) b.setStepsToReproduce(in.getStepsToReproduce());
        if (in.getSeverity() != null) b.setSeverity(in.getSeverity());
        if (in.getItemId() != null) b.setItemId(in.getItemId());
        if (in.getHowFound() != null) b.setHowFound(in.getHowFound());
        if (in.getModVersion() != null) b.setModVersion(in.getModVersion());
        if (in.getMcVersion() != null) b.setMcVersion(in.getMcVersion());
        if (in.getWorldContext() != null) b.setWorldContext(in.getWorldContext());
        if (in.getScreenshotUrl() != null) b.setScreenshotUrl(in.getScreenshotUrl());
        if (in.getFrequency() != null) b.setFrequency(in.getFrequency());
        if (in.getPriority() != null) b.setPriority(in.getPriority());
        if (in.getExpectedBehavior() != null) b.setExpectedBehavior(in.getExpectedBehavior());
        if (in.getWorkaround() != null) b.setWorkaround(in.getWorkaround());
        if (in.getTags() != null) b.setTags(in.getTags());
        // canReplicate / affectsOthers são primitivos — sempre atualizam
        b.setCanReplicate(in.isCanReplicate());
        b.setAffectsOthers(in.isAffectsOthers());
        return bugRepo.save(b);
    }

    /**
     * Tester deleta o PRÓPRIO bug. Mesma validação do update (PENDING + dono).
     */
    @Transactional
    public void deleteBugFromTester(Long id, String mcName) {
        BugReport b = bugRepo.findById(id).orElseThrow(() ->
                new IllegalArgumentException("bug não existe"));
        if (b.getTesterMcName() == null
                || !b.getTesterMcName().equalsIgnoreCase(mcName)) {
            throw new IllegalStateException("você só pode deletar seus próprios bugs");
        }
        if (b.getStatus() != BugReport.Status.PENDING) {
            throw new IllegalStateException(
                    "bug já foi triado (" + b.getStatus() + ") — não pode deletar");
        }
        bugRepo.delete(b);
    }

    /**
     * Admin confirma o bug: muda status pra CONFIRMED + dá pontos pro tester.
     * Se points <= 0, usa default de 10.
     */
    @Transactional
    public BugReport confirmBug(Long id, int points, String adminNote, String triagedBy) {
        BugReport b = bugRepo.findById(id).orElseThrow();
        if (b.getStatus() == BugReport.Status.CONFIRMED) return b;
        int award = points > 0 ? points : 10;
        b.setStatus(BugReport.Status.CONFIRMED);
        b.setPointsAwarded(award);
        b.setAdminNote(adminNote);
        b.setTriagedBy(triagedBy);
        b.setTriagedAt(Instant.now());
        BugReport saved = bugRepo.save(b);
        // Soma pontos no tester
        try {
            testerSvc.addPoints(b.getTesterMcName(), award);
        } catch (Exception e) {
            LOG.warn("[Tester] não conseguiu somar pontos pra {}: {}", b.getTesterMcName(), e.getMessage());
        }
        // Notifica tester
        notifyTester(b.getTesterMcName(), TesterNotification.Type.BUG_CONFIRMED,
                "🐛 Bug confirmado · +" + award + " pts",
                b.getTitle(),
                "/tester/dashboard?tab=bugs&id=" + b.getId());
        return saved;
    }

    /**
     * Outro tester confirma que conseguiu replicar o bug. Adiciona mcName
     * à lista (deduplicado) no JSON do confirmationsJson. Útil pra admin
     * priorizar bugs com mais "+1"s da comunidade.
     */
    @Transactional
    public BugReport confirmBugReplication(Long bugId, String mcName) {
        BugReport b = bugRepo.findById(bugId).orElse(null);
        if (b == null) return null;
        // Não pode confirmar próprio bug — fica chato
        if (b.getTesterMcName() != null && b.getTesterMcName().equalsIgnoreCase(mcName)) return b;

        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Set<String> set = new java.util.LinkedHashSet<>();
        if (b.getConfirmationsJson() != null && !b.getConfirmationsJson().isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                java.util.List<String> arr = json.readValue(b.getConfirmationsJson(), java.util.List.class);
                set.addAll(arr);
            } catch (Exception ignored) {}
        }
        set.add(mcName);
        try { b.setConfirmationsJson(json.writeValueAsString(set)); }
        catch (Exception ignored) {}
        return bugRepo.save(b);
    }

    @Transactional
    public BugReport rejectBug(Long id, String adminNote, String triagedBy) {
        BugReport b = bugRepo.findById(id).orElseThrow();
        b.setStatus(BugReport.Status.REJECTED);
        b.setPointsAwarded(0);
        b.setAdminNote(adminNote);
        b.setTriagedBy(triagedBy);
        b.setTriagedAt(Instant.now());
        BugReport saved = bugRepo.save(b);
        notifyTester(b.getTesterMcName(), TesterNotification.Type.BUG_REJECTED,
                "❌ Bug rejeitado",
                b.getTitle() + (adminNote != null && !adminNote.isBlank() ? " — " + adminNote : ""),
                "/tester/dashboard?tab=bugs&id=" + b.getId());
        return saved;
    }

    // ============================================================ //
    // MVP-5: REWARDS
    // ============================================================ //

    @Transactional
    public Reward createReward(Reward in) { return rewardRepo.save(in); }

    @Transactional
    public Reward updateReward(Long id, Reward in) {
        Reward r = rewardRepo.findById(id).orElseThrow();
        if (in.getName() != null) r.setName(in.getName());
        if (in.getDescription() != null) r.setDescription(in.getDescription());
        if (in.getCostPoints() > 0) r.setCostPoints(in.getCostPoints());
        if (in.getGiveCommand() != null) r.setGiveCommand(in.getGiveCommand());
        if (in.getImageUrl() != null) r.setImageUrl(in.getImageUrl());
        if (in.getCategory() != null) r.setCategory(in.getCategory());
        r.setEnabled(in.isEnabled());
        if (in.getPerTesterLimit() >= 0) r.setPerTesterLimit(in.getPerTesterLimit());
        return rewardRepo.save(r);
    }

    public List<Reward> listRewards(boolean onlyEnabled) {
        return onlyEnabled ? rewardRepo.findByEnabledTrueOrderByCostPointsAsc()
                : rewardRepo.findAllByOrderByCostPointsAsc();
    }

    public Optional<Reward> getReward(Long id) { return rewardRepo.findById(id); }

    @Transactional
    public void deleteReward(Long id) { rewardRepo.deleteById(id); }

    /**
     * Tester resgata recompensa: valida pontos suficientes, valida limit per-tester,
     * cria redemption (delivered=false), debita pontos.
     */
    @Transactional
    public RewardRedemption redeem(String mcName, Long rewardId) {
        Reward r = rewardRepo.findById(rewardId)
                .orElseThrow(() -> new IllegalArgumentException("recompensa não encontrada"));
        if (!r.isEnabled()) throw new IllegalStateException("recompensa desabilitada");

        ModTester t = testerSvc.findByMcName(mcName)
                .orElseThrow(() -> new IllegalArgumentException("tester não encontrado"));
        if (t.getPoints() < r.getCostPoints()) {
            throw new IllegalStateException("pontos insuficientes: você tem " + t.getPoints() + ", custa " + r.getCostPoints());
        }
        if (r.getPerTesterLimit() > 0) {
            long count = redemptionRepo.countByTesterMcNameAndRewardId(mcName, rewardId);
            if (count >= r.getPerTesterLimit()) {
                throw new IllegalStateException("limite atingido: você já resgatou " + count + "/" + r.getPerTesterLimit() + " vezes");
            }
        }

        // Debita pontos
        testerSvc.addPoints(mcName, -r.getCostPoints());

        // Cria registro
        RewardRedemption rr = new RewardRedemption();
        rr.setTesterMcName(mcName);
        rr.setRewardId(rewardId);
        rr.setRewardSnapshot(r.getName() + " (custo: " + r.getCostPoints() + ")");
        rr.setPointsSpent(r.getCostPoints());
        rr.setDelivered(false);
        rr.setCommandRun(substitutePlayer(r.getGiveCommand(), mcName));
        return redemptionRepo.save(rr);
    }

    private String substitutePlayer(String cmd, String player) {
        if (cmd == null) return null;
        return cmd.replace("{player}", player).replace("@s", player).replace("@p", player);
    }

    @Transactional
    public RewardRedemption markDelivered(Long id, String note) {
        RewardRedemption rr = redemptionRepo.findById(id).orElseThrow();
        rr.setDelivered(true);
        rr.setDeliveredAt(Instant.now());
        if (note != null) rr.setDeliveryNote(note);
        RewardRedemption saved = redemptionRepo.save(rr);
        notifyTester(rr.getTesterMcName(), TesterNotification.Type.REDEMPTION_DELIVERED,
                "🎁 Recompensa entregue!",
                rr.getRewardSnapshot() + (note != null && !note.isBlank() ? " — " + note : ""),
                "/tester/dashboard?tab=rewards");
        return saved;
    }

    public List<RewardRedemption> listRedemptionsForTester(String mcName) {
        return redemptionRepo.findByTesterMcNameIgnoreCaseOrderByRedeemedAtDesc(mcName);
    }

    public List<RewardRedemption> listAllRedemptions() {
        return redemptionRepo.findAllByOrderByRedeemedAtDesc();
    }

    // ============================================================ //
    // MVP-6: 3D MODELS (BlockBench .bbmodel)
    // ============================================================ //

    /**
     * Detecta o formato do modelo pela extensão. Retorna null se desconhecido.
     */
    private static String detectModelFormat(String filename) {
        if (filename == null) return null;
        String low = filename.toLowerCase();
        if (low.endsWith(".bbmodel")) return "bbmodel";
        if (low.endsWith(".gltf")) return "gltf";
        if (low.endsWith(".glb")) return "glb";
        if (low.endsWith(".obj")) return "obj";
        return null;
    }

    /**
     * Validação leve por formato — pega arquivos com extensão errada antes
     * de salvar em disco e dar erro feio no viewer.
     */
    private static void validateModelContent(String format, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("arquivo vazio");
        }
        switch (format) {
            case "bbmodel": {
                String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                if (!text.contains("\"elements\"")) {
                    throw new IllegalArgumentException(".bbmodel inválido (não tem \"elements\")");
                }
                break;
            }
            case "gltf": {
                String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                if (!text.contains("\"asset\"") && !text.contains("\"meshes\"") && !text.contains("\"nodes\"")) {
                    throw new IllegalArgumentException(".gltf inválido (não tem \"asset\"/\"meshes\"/\"nodes\")");
                }
                break;
            }
            case "glb": {
                // GLB header: magic "glTF" (0x46546C67 little-endian)
                if (bytes.length < 12) throw new IllegalArgumentException(".glb truncado");
                if (bytes[0] != 'g' || bytes[1] != 'l' || bytes[2] != 'T' || bytes[3] != 'F') {
                    throw new IllegalArgumentException(".glb inválido (magic bytes errado)");
                }
                break;
            }
            case "obj": {
                String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                // OBJ tem linhas tipo "v 1.0 2.0 3.0" pra vertex e "f 1 2 3" pra face
                if (!text.contains("\nv ") && !text.startsWith("v ")
                        && !text.contains("\nf ") && !text.startsWith("f ")) {
                    throw new IllegalArgumentException(".obj inválido (sem vertices/faces)");
                }
                break;
            }
            default:
                throw new IllegalArgumentException("formato desconhecido: " + format);
        }
    }

    /**
     * Salva o modelo em disco (UUID.<ext> pra evitar colisão) e cria registro.
     * Suporta múltiplos formatos: bbmodel, gltf, glb, obj.
     *
     * Validação leve por formato pega arquivos corrompidos antes do upload
     * efetivar — frontend recebe erro claro em vez de viewer quebrar.
     *
     * Limites: 30MB pra glb (binary), 15MB pros demais (text-based).
     */
    @Transactional
    public Model3D uploadModel(String name, String description, String category,
                                MultipartFile file, String uploadedBy) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("arquivo vazio");
        }
        String orig = file.getOriginalFilename();
        String format = detectModelFormat(orig);
        if (format == null) {
            throw new IllegalArgumentException("formato não suportado (use .bbmodel, .gltf, .glb ou .obj)");
        }
        // Limites diferentes por formato — text é mais leve do que binary
        long maxBytes = format.equals("glb") ? 30L * 1024 * 1024 : 15L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("arquivo muito grande (máx " + (maxBytes / 1024 / 1024) + "MB)");
        }

        byte[] bytes = file.getBytes();
        validateModelContent(format, bytes);

        Path dir = Paths.get(modelsDir);
        Files.createDirectories(dir);

        Model3D m = new Model3D();
        m.setName(name);
        m.setDescription(description);
        m.setCategory(category);
        m.setFormat(format);
        m.setFilename(orig);
        m.setSizeBytes(file.getSize());
        m.setUploadedBy(uploadedBy);
        // UUID elimina o chicken-and-egg do "preciso do ID pra montar storagePath"
        String storagePath = java.util.UUID.randomUUID().toString().replace("-", "") + "." + format;
        m.setStoragePath(storagePath);
        m = modelRepo.save(m);

        Path target = dir.resolve(storagePath);
        try {
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            modelRepo.delete(m);
            throw e;
        }
        return m;
    }

    /** Lê o JSON cru do .bbmodel (frontend parsea e renderiza). */
    public byte[] readModelFile(Long id) throws IOException {
        Model3D m = modelRepo.findById(id).orElse(null);
        if (m == null || m.getStoragePath() == null) return null;
        Path file = Paths.get(modelsDir, m.getStoragePath());
        if (!Files.exists(file)) return null;
        // Incrementa view count (best-effort)
        try {
            m.setViewCount(m.getViewCount() + 1);
            modelRepo.save(m);
        } catch (Exception ignored) {}
        return Files.readAllBytes(file);
    }

    public Optional<Model3D> getModel(Long id) { return modelRepo.findById(id); }

    public List<Model3D> listModels(boolean onlyEnabled) {
        return onlyEnabled ? modelRepo.findByEnabledTrueOrderByUploadedAtDesc()
                : modelRepo.findAllByOrderByUploadedAtDesc();
    }

    @Transactional
    public void deleteModel(Long id) throws IOException {
        Model3D m = modelRepo.findById(id).orElse(null);
        if (m == null) return;
        try {
            Path file = Paths.get(modelsDir, m.getStoragePath());
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // arquivo já não existe ou disco read-only — segue com delete do DB
        }
        modelRepo.delete(m);
    }

    @Transactional
    public Model3D toggleModelEnabled(Long id) {
        Model3D m = modelRepo.findById(id).orElseThrow();
        m.setEnabled(!m.isEnabled());
        return modelRepo.save(m);
    }

    @Transactional
    public Model3D updateModelMeta(Long id, String name, String description, String category) {
        Model3D m = modelRepo.findById(id).orElseThrow();
        if (name != null) m.setName(name);
        if (description != null) m.setDescription(description);
        if (category != null) m.setCategory(category);
        return modelRepo.save(m);
    }

    // ============================================================ //
    // MVP-7: BETA AUDIOS (criaturas/instrumentos/ambient)
    // ============================================================ //

    private static final java.util.Set<String> ALLOWED_AUDIO_EXTS = java.util.Set.of(
            ".mp3", ".wav", ".ogg", ".oga", ".flac", ".m4a"
    );

    private static String guessMimeType(String filename) {
        String low = filename == null ? "" : filename.toLowerCase();
        if (low.endsWith(".mp3")) return "audio/mpeg";
        if (low.endsWith(".wav")) return "audio/wav";
        if (low.endsWith(".ogg") || low.endsWith(".oga")) return "audio/ogg";
        if (low.endsWith(".flac")) return "audio/flac";
        if (low.endsWith(".m4a")) return "audio/mp4";
        return "application/octet-stream";
    }

    @Transactional
    public BetaAudio uploadAudio(String name, String description, String category, String creatureId,
                                  Integer durationSec, MultipartFile file, String uploadedBy) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("arquivo vazio");
        String orig = file.getOriginalFilename();
        if (orig == null) throw new IllegalArgumentException("nome do arquivo ausente");
        String low = orig.toLowerCase();
        String ext = null;
        for (String e : ALLOWED_AUDIO_EXTS) if (low.endsWith(e)) { ext = e; break; }
        if (ext == null) throw new IllegalArgumentException("formato não suportado (use mp3, wav, ogg, flac ou m4a)");
        if (file.getSize() > 25L * 1024 * 1024) {
            throw new IllegalArgumentException("arquivo muito grande (máx 25MB)");
        }

        Path dir = Paths.get(audiosDir);
        Files.createDirectories(dir);

        BetaAudio a = new BetaAudio();
        a.setName(name);
        a.setDescription(description);
        a.setCategory(category);
        a.setCreatureId(creatureId);
        a.setFilename(orig);
        a.setSizeBytes(file.getSize());
        a.setMimeType(guessMimeType(orig));
        a.setDurationSec(durationSec);
        a.setUploadedBy(uploadedBy);
        String storagePath = java.util.UUID.randomUUID().toString().replace("-", "") + ext;
        a.setStoragePath(storagePath);
        a = audioRepo.save(a);

        Path target = dir.resolve(storagePath);
        try {
            Files.write(target, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            audioRepo.delete(a);
            throw e;
        }
        return a;
    }

    public byte[] readAudioFile(Long id) throws IOException {
        BetaAudio a = audioRepo.findById(id).orElse(null);
        if (a == null || a.getStoragePath() == null) return null;
        Path file = Paths.get(audiosDir, a.getStoragePath());
        if (!Files.exists(file)) return null;
        try {
            a.setPlayCount(a.getPlayCount() + 1);
            audioRepo.save(a);
        } catch (Exception ignored) {}
        return Files.readAllBytes(file);
    }

    public Optional<BetaAudio> getAudio(Long id) { return audioRepo.findById(id); }

    public List<BetaAudio> listAudios(boolean onlyEnabled) {
        return onlyEnabled ? audioRepo.findByEnabledTrueOrderByUploadedAtDesc()
                : audioRepo.findAllByOrderByUploadedAtDesc();
    }

    @Transactional
    public void deleteAudio(Long id) {
        BetaAudio a = audioRepo.findById(id).orElse(null);
        if (a == null) return;
        try {
            Path file = Paths.get(audiosDir, a.getStoragePath());
            Files.deleteIfExists(file);
        } catch (IOException ignored) {}
        audioRepo.delete(a);
    }

    @Transactional
    public BetaAudio toggleAudioEnabled(Long id) {
        BetaAudio a = audioRepo.findById(id).orElseThrow();
        a.setEnabled(!a.isEnabled());
        return audioRepo.save(a);
    }

    @Transactional
    public BetaAudio updateAudioMeta(Long id, String name, String description, String category, String creatureId) {
        BetaAudio a = audioRepo.findById(id).orElseThrow();
        if (name != null) a.setName(name);
        if (description != null) a.setDescription(description);
        if (category != null) a.setCategory(category);
        if (creatureId != null) a.setCreatureId(creatureId);
        return audioRepo.save(a);
    }

    // ============================================================ //
    // MVP-7b: VOTES (genérico — BETA_ITEM | BETA_AUDIO | MODEL_3D)
    // ============================================================ //

    /**
     * Aplica/atualiza/remove voto:
     *  - sem voto anterior → cria
     *  - voto anterior == novo voto → remove (toggle off)
     *  - voto anterior != novo voto → atualiza (LIKE ↔ DISLIKE)
     *
     * Retorna o voto resultante (null se foi removido).
     */
    @Transactional
    public BetaVote vote(String voterMcName, BetaVote.Target target, Long targetId, BetaVote.Vote vote) {
        if (voterMcName == null || target == null || targetId == null || vote == null) {
            throw new IllegalArgumentException("voterMcName, target, targetId, vote são obrigatórios");
        }
        Optional<BetaVote> existing = voteRepo.findByVoterMcNameAndTargetAndTargetId(voterMcName, target, targetId);
        if (existing.isPresent()) {
            BetaVote v = existing.get();
            if (v.getVote() == vote) {
                voteRepo.delete(v);
                return null;
            }
            v.setVote(vote);
            v.setVotedAt(Instant.now());
            return voteRepo.save(v);
        }
        BetaVote v = new BetaVote();
        v.setVoterMcName(voterMcName);
        v.setTarget(target);
        v.setTargetId(targetId);
        v.setVote(vote);
        return voteRepo.save(v);
    }

    public Map<String, Object> voteCounts(BetaVote.Target target, Long targetId, String myMcName) {
        long likes = voteRepo.countByTargetAndTargetIdAndVote(target, targetId, BetaVote.Vote.LIKE);
        long dislikes = voteRepo.countByTargetAndTargetIdAndVote(target, targetId, BetaVote.Vote.DISLIKE);
        String myVote = null;
        if (myMcName != null) {
            Optional<BetaVote> mine = voteRepo.findByVoterMcNameAndTargetAndTargetId(myMcName, target, targetId);
            if (mine.isPresent()) myVote = mine.get().getVote().name();
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("likes", likes);
        r.put("dislikes", dislikes);
        r.put("myVote", myVote);
        return r;
    }

    /**
     * Bulk vote counts pra evitar N+1 ao listar vários targets. Retorna
     * map de targetId → {likes, dislikes, myVote}.
     */
    public Map<Long, Map<String, Object>> bulkVoteCounts(BetaVote.Target target, List<Long> targetIds, String myMcName) {
        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        if (targetIds == null || targetIds.isEmpty()) return result;
        for (Long id : targetIds) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("likes", 0L);
            r.put("dislikes", 0L);
            r.put("myVote", null);
            result.put(id, r);
        }
        List<BetaVote> votes = voteRepo.findByTargetAndTargetIdIn(target, targetIds);
        for (BetaVote v : votes) {
            Map<String, Object> r = result.get(v.getTargetId());
            if (r == null) continue;
            if (v.getVote() == BetaVote.Vote.LIKE) r.put("likes", ((Long) r.get("likes")) + 1);
            else r.put("dislikes", ((Long) r.get("dislikes")) + 1);
            if (myMcName != null && myMcName.equalsIgnoreCase(v.getVoterMcName())) {
                r.put("myVote", v.getVote().name());
            }
        }
        return result;
    }
}
