package br.com.murilo.liberthia.loom.entity;

import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * v0.1.22 r36 → <b>r173 (intelligence rework)</b>: Peripheral Observer.
 *
 * <p><b>Filosofia:</b> sempre "no canto do olho". Se o player encara → some de vista
 * (mas <b>não morre</b>): teleporta pra um novo canto escondido. Persegue UM player
 * específico durante ~30 min, então PARA e some de vez.
 *
 * <h2>Inteligência r173 → r176</h2>
 * <ul>
 *   <li><b>Alvo fixo</b>: pode ser amarrado a um player (UUID). Aparece só pra ele.</li>
 *   <li><b>Lurk spots</b>: procura cantos com chão sólido <i>no nível do player</i>
 *       (funciona dentro de casa/caverna, não só na superfície), preferindo pontos
 *       <i>atrás de paredes</i> (occlusos) — o clássico "vulto no canto".</li>
 *   <li><b>r176 — Caça em ciclos</b>: alterna fases de <i>espreita</i> (30–90s,
 *       presente e assombrando) e <i>trégua</i> (20–60s, recuado e invisível). Dá o
 *       intervalo entre sustos em vez de assediar sem parar.</li>
 *   <li><b>r176 — Atrás do vidro</b>: ao manifestar, se o alvo está em casa com
 *       janela, aparece no ponto <i>mais distante do outro lado do vidro</i>,
 *       encarando pra dentro; sem janela, cai no canto escondido.</li>
 *   <li><b>Ciclo de 30 min</b>: depois disso, desaparece permanentemente.</li>
 *   <li><b>Som sutil</b> ao reposicionar.</li>
 * </ul>
 */
public class PeripheralObserverEntity extends Monster {

    /** 30 minutos = 36000 ticks. */
    public static final int MAX_HAUNT = 36000;

    private int whisperCooldown = 200;
    private int lifeTicks = 0;
    private int hauntDuration = MAX_HAUNT;
    private UUID targetPlayer = null;

    // ── r176: ciclo de caça (espreita ↔ trégua) ──
    private boolean dormant = false; // em trégua: recuado, invisível, não assombra
    private int phaseTicks = 0;
    private int phaseDuration = 0;   // 0 → (re)inicia no 1º tick
    private int prowlCooldown = 0;   // reposiciona em volta do player (ronda)
    private int noTargetTicks = 0;   // carência antes de sumir quando não há alvo
    private int revealCooldown = 0;  // r177: fica invisível alguns ticks após teleportar
                                     // (mata o "deslize" — não dá pra ver o caminho do TP)

    /** Raio em que a presença de OUTRO player faz o observador não aparecer. */
    private static final double SOLO_RANGE = 200.0;
    /** r177: modo "encarar AFK" — fica parado de frente pro player, sem rondar. */
    private boolean afkStare = false;

    public PeripheralObserverEntity(EntityType<? extends PeripheralObserverEntity> type, Level level) {
        super(type, level);
        this.setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3) // r178: agora ANDA (persegue de verdade)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** Amarra o observer a um player específico por {@code minutes} minutos. */
    public void startHaunt(UUID player, int minutes) {
        this.targetPlayer = player;
        this.hauntDuration = Math.max(1, minutes) * 60 * 20;
        this.lifeTicks = 0;
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    // Nunca despawna por distância/regra vanilla — nós controlamos o ciclo de vida.
    @Override
    public void checkDespawn() {
        if (targetPlayer == null) super.checkDespawn();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (afkStare) { tickAfkStare(); return; }

        lifeTicks++;
        // Só assombros AMARRADOS por comando expiram (30 min). Um prowler livre ronda
        // sem prazo — fica sempre por perto do player.
        if (targetPlayer != null && lifeTicks >= hauntDuration) { vanish(); return; }

        Player target = resolveTarget();
        if (target == null) {
            // Não some à toa: só desiste depois de ~60s SEM ninguém por perto.
            if (targetPlayer == null && ++noTargetTicks > 1200) discard();
            return;
        }
        noTargetTicks = 0;

        // r177: SÓ aparece quando o player está SOZINHO (nenhum outro player num raio
        // de 200 blocos). Tem mais gente por perto → some e espera.
        if (!targetIsAlone(target)) {
            if (!isInvisible()) setInvisible(true);
            getNavigation().stop();
            return;
        }
        // r177: revela depois do teleporte (anti-deslize) quando já estiver posicionado
        if (revealCooldown > 0 && --revealCooldown == 0 && !dormant) setInvisible(false);

        // ── ciclo de caça: espreita um tempo, recua um intervalo, volta a aparecer ──
        if (phaseDuration <= 0) beginStalk(target);
        phaseTicks++;
        if (phaseTicks >= phaseDuration) {
            if (dormant) beginStalk(target); else beginDormant(target);
        }
        if (dormant) {
            // trégua: recuado e invisível, deixa o jogador "respirar" antes do próximo susto
            return;
        }

        // r177: SOZINHO + observador ativo → os ANIMAIS da região param e te ENCARAM.
        makeRegionStare(target);

        double dist = target.distanceTo(this);
        if (dist > 56) {
            // longe demais — reposiciona perto do alvo (segue ele pelo mundo)
            relocate(target, true);
            return;
        }

        Vec3 toMe = this.position().subtract(target.getEyePosition()).normalize();
        Vec3 playerLook = target.getLookAngle();
        double dot = toMe.dot(playerLook);
        boolean watched = dot > 0.55 && this.hasLineOfSight(target);

        if (watched) {
            // r178: ANJO CHORÃO — sendo observado, CONGELA no lugar (visível, parado,
            // te encarando). Não desliza nem some: o terror é ele estar ali.
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            this.getLookControl().setLookAt(target.position());
            // encarado BEM no centro e perto → susto curto e pisca pra outro canto.
            if (dot > 0.93 && dist < 9) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 24, 0.4, 0.6, 0.4, 0.05);
                }
                level().playSound(null, blockPosition(), ModSounds.PERIPHERAL_BLIND.get(),
                        SoundSource.HOSTILE, 0.6F, 0.8F);
                relocate(target, false);
            }
            return;
        }

        // r178: NÃO observado → PERSEGUE DE VERDADE. Anda na direção do player (você
        // o vê se aproximando pelo canto do olho). Só pisca pra trás se colar demais.
        this.getLookControl().setLookAt(target.position());
        if (dist > 3.5) {
            this.getNavigation().moveTo(target, 1.15);
        } else {
            relocate(target, false); // colado e sem ser visto → pisca pro canto
        }
        // tremor de estática na cabeça enquanto espreita (glitch ocasional)
        if (this.getRandom().nextFloat() < 0.30F) {
            setYHeadRot(getYHeadRot() + (this.getRandom().nextFloat() - 0.5F) * 60F);
        }

        // RONDA ocasional: às vezes pisca pra um novo canto ao redor (circula você),
        // mas só quando NÃO está na sua linha de visão (pra você não ver o teleporte).
        if (--prowlCooldown <= 0) {
            prowlCooldown = 200 + level().random.nextInt(200); // 10–20s
            if (!this.hasLineOfSight(target)) relocate(target, false);
        }

        whisperCooldown--;
        if (whisperCooldown <= 0) {
            whisperCooldown = 300 + level().random.nextInt(400);
            level().playSound(null, blockPosition(), ModSounds.PERIPHERAL_WHISPER.get(),
                    SoundSource.HOSTILE, 0.25F, 0.6F + level().random.nextFloat() * 0.4F);
        }
    }

    private Player resolveTarget() {
        if (targetPlayer != null) {
            Player p = level().getPlayerByUUID(targetPlayer);
            return (p != null && p.isAlive() && !p.isSpectator()) ? p : null;
        }
        return level().getNearestPlayer(this, 160.0); // largo: não perde o player tão fácil
    }

    /** Some de vez (fim do ciclo). */
    private void vanish() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 30, 0.4, 0.6, 0.4, 0.06);
            sl.sendParticles(ParticleTypes.ASH, getX(), getY() + 1, getZ(), 20, 0.4, 0.6, 0.4, 0.02);
        }
        discard();
    }

    /**
     * Acha um novo canto escondido e teleporta pra lá. {@code force} ignora a
     * preferência por occlusão (usado quando o alvo está longe e precisa seguir).
     */
    private void relocate(Player target, boolean force) {
        BlockPos spot = findLurkSpot(target, force);
        if (spot != null) placeAt(spot, target);
    }

    /** Teleporta pro {@code spot} com fumaça/sopro sutil e encara o alvo. */
    private void placeAt(BlockPos spot, Player target) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 4, 0.2, 0.3, 0.2, 0.01);
        }
        moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level().random.nextFloat() * 360, 0);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 4, 0.2, 0.3, 0.2, 0.01);
            // som bem sutil de reposição (sopro)
            sl.playSound(null, blockPosition(), ModSounds.PERIPHERAL_WHISPER.get(),
                    SoundSource.HOSTILE, 0.18F, 0.5F + level().random.nextFloat() * 0.2F);
        }
        getLookControl().setLookAt(target.position());
        // r177: invisível por ~8 ticks enquanto a nova posição sincroniza → o cliente
        // termina de interpolar o trajeto ESCONDIDO e só "aparece" parado no destino
        // (não dá mais pra ver o caminho do teleporte).
        setInvisible(true);
        revealCooldown = 8;
    }

    /** r177: animais num raio de 24 blocos param de vagar e ficam ENCARANDO o player. */
    private void makeRegionStare(Player target) {
        for (net.minecraft.world.entity.animal.Animal a : level().getEntitiesOfClass(
                net.minecraft.world.entity.animal.Animal.class, target.getBoundingBox().inflate(24.0))) {
            double dx = target.getX() - a.getX();
            double dz = target.getZ() - a.getZ();
            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            a.setYRot(yaw);
            a.setYHeadRot(yaw);
            a.setYBodyRot(yaw);
            a.getLookControl().setLookAt(target, 30F, 30F);
            a.getNavigation().stop();        // param de andar — só ficam te encarando
        }
    }

    /** r177: ativa o modo AFK — fica imóvel de frente pro player, encarando. O
     *  {@code AfkObserverManager} cuida do despawn quando o player se mexe. */
    public void startAfkStare(UUID player) {
        this.targetPlayer = player;
        this.afkStare = true;
        this.hauntDuration = Integer.MAX_VALUE;
        setInvisible(false);
        setPersistenceRequired();
    }

    private void tickAfkStare() {
        Player t = (targetPlayer != null) ? level().getPlayerByUUID(targetPlayer)
                : level().getNearestPlayer(this, 32.0);
        if (t == null || !t.isAlive()) { discard(); return; }
        double dx = t.getX() - getX(), dz = t.getZ() - getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        setYRot(yaw); setYBodyRot(yaw);
        staticHeadTwitch(yaw);
    }

    /** r177: cabeça tremendo como ESTÁTICA — snaps erráticos de yaw/pitch a cada tick. */
    private void staticHeadTwitch(float baseYaw) {
        var r = this.getRandom();
        if (r.nextFloat() < 0.45F) {           // ~45% dos ticks dá um "glitch"
            setYHeadRot(baseYaw + (r.nextFloat() - 0.5F) * 75F);
            setXRot((r.nextFloat() - 0.5F) * 55F);
        } else {
            setYHeadRot(baseYaw);
            setXRot(0F);
        }
    }

    /** True se {@code target} é o ÚNICO player (não-espectador) num raio de {@link #SOLO_RANGE}. */
    private boolean targetIsAlone(Player target) {
        for (Player p : level().getEntitiesOfClass(Player.class,
                target.getBoundingBox().inflate(SOLO_RANGE))) {
            if (p != target && !p.isSpectator()) return false;
        }
        return true;
    }

    /** Começa a fase de espreita (30–90s): manifesta atrás do vidro/no canto. */
    private void beginStalk(Player target) {
        dormant = false;
        setInvisible(false);
        phaseTicks = 0;
        phaseDuration = 600 + level().random.nextInt(1200);
        whisperCooldown = 60 + level().random.nextInt(120);
        prowlCooldown = 100 + level().random.nextInt(120);
        manifest(target);
    }

    /** r178: respiro curto (4–10s): PAUSA a perseguição um instante, mas continua
     *  VISÍVEL e por perto (não some mais por 25s — era isso que parecia "parou"). */
    private void beginDormant(Player target) {
        dormant = true;
        phaseTicks = 0;
        phaseDuration = 80 + level().random.nextInt(120); // 4–10s
        getNavigation().stop();
    }

    /**
     * Manifestação: aparece <b>atrás de um vidro</b> (se o alvo está em casa com
     * janela) no ponto mais distante do outro lado; senão, num canto escondido.
     */
    private void manifest(Player target) {
        BlockPos spot = findWindowSpot(target);
        if (spot == null) spot = findLurkSpot(target, false);
        if (spot != null) placeAt(spot, target);
    }

    /** Recua pra ~22–36 blocos do alvo (some por perto durante a breve trégua). */
    private void retreat(Player target) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < 12; i++) {
            double ang = sl.random.nextDouble() * Math.PI * 2;
            double d = 22 + sl.random.nextDouble() * 14;
            int x = (int) (target.getX() + Math.cos(ang) * d);
            int z = (int) (target.getZ() + Math.sin(ang) * d);
            BlockPos stand = findStanding(sl, x, target.getBlockY(), z);
            if (stand != null) { placeAt(stand, target); return; }
        }
    }

    /**
     * Procura um <b>vidro</b> perto do alvo e devolve o ponto de pé mais distante
     * do <b>outro lado</b> dele (te encarando pela janela). {@code null} se não há
     * janela por perto (aí cai no canto escondido).
     */
    private BlockPos findWindowSpot(Player target) {
        if (!(level() instanceof ServerLevel sl)) return null;
        BlockPos pp = target.blockPosition();
        BlockPos bestGlass = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos bp = pp.offset(dx, dy, dz);
                    if (!isGlass(sl.getBlockState(bp))) continue;
                    double d = bp.distSqr(pp);
                    if (d > 4 && d < bestD) { bestD = d; bestGlass = bp.immutable(); }
                }
            }
        }
        if (bestGlass == null) return null;

        Vec3 g = Vec3.atCenterOf(bestGlass);
        Vec3 dir = new Vec3(g.x - target.getX(), 0, g.z - target.getZ());
        if (dir.lengthSqr() < 1e-4) return null;
        dir = dir.normalize();
        // do mais longe pro mais perto → "o lugar mais distante do outro lado"
        for (int ext = 6; ext >= 2; ext--) {
            int x = (int) Math.floor(g.x + dir.x * ext);
            int z = (int) Math.floor(g.z + dir.z * ext);
            BlockPos stand = findStanding(sl, x, bestGlass.getY(), z);
            if (stand != null) return stand;
        }
        return null;
    }

    private static boolean isGlass(BlockState state) {
        return state.getBlock() instanceof AbstractGlassBlock
                || state.getBlock() instanceof IronBarsBlock; // vidro, painéis e grades
    }

    /**
     * Procura um ponto de espreita: chão sólido perto do alvo, fora do cone de
     * visão (atrás/lado), de preferência ESCONDIDO atrás de blocos.
     */
    private BlockPos findLurkSpot(Player target, boolean force) {
        if (!(level() instanceof ServerLevel sl)) return null;
        Vec3 look = target.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        Vec3 eye = target.getEyePosition();
        int baseY = target.getBlockY();

        BlockPos best = null;
        boolean bestOccluded = false;
        for (int i = 0; i < 28; i++) {
            double ang = sl.random.nextDouble() * Math.PI * 2;
            double d = 5 + sl.random.nextDouble() * 9; // 5–14 blocos
            double dx = Math.cos(ang) * d, dz = Math.sin(ang) * d;
            Vec3 dir = new Vec3(dx, 0, dz).normalize();
            if (dir.dot(flat) > 0.2 && !force) continue; // tem que estar atrás/lado

            int x = (int) Math.floor(target.getX() + dx);
            int z = (int) Math.floor(target.getZ() + dz);
            BlockPos stand = findStanding(sl, x, baseY, z);
            if (stand == null) continue;

            Vec3 center = new Vec3(stand.getX() + 0.5, stand.getY() + 1.0, stand.getZ() + 0.5);
            boolean occluded = isOccluded(sl, eye, center);
            // preferimos um spot escondido; se achar um occluso, fecha negócio.
            if (best == null || (occluded && !bestOccluded)) {
                best = stand;
                bestOccluded = occluded;
                if (occluded) break;
            }
        }
        if (best == null) {
            // fallback: superfície atrás do player (modo antigo)
            for (int i = 0; i < 8; i++) {
                double ang = Math.atan2(-flat.z, -flat.x) + (sl.random.nextDouble() - 0.5) * Math.PI;
                double d = 10 + sl.random.nextDouble() * 6;
                int x = (int) (target.getX() + Math.cos(ang) * d);
                int z = (int) (target.getZ() + Math.sin(ang) * d);
                int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (y > sl.getMinBuildHeight() && y < sl.getMaxBuildHeight() - 2) {
                    return new BlockPos(x, y, z);
                }
            }
        }
        return best;
    }

    /** Procura um ponto com chão sólido + 2 blocos de ar, perto de y0. */
    private BlockPos findStanding(ServerLevel sl, int x, int y0, int z) {
        for (int dy = 3; dy >= -4; dy--) {
            int y = y0 + dy;
            BlockPos feet = new BlockPos(x, y, z);
            BlockState floor = sl.getBlockState(feet.below());
            if (floor.blocksMotion()
                    && sl.getBlockState(feet).isAir()
                    && sl.getBlockState(feet.above()).isAir()) {
                return feet;
            }
        }
        return null;
    }

    /** True se há um bloco entre os dois pontos (alvo NÃO vê esse spot). */
    private boolean isOccluded(ServerLevel sl, Vec3 from, Vec3 to) {
        HitResult hit = sl.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() != HitResult.Type.MISS;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", lifeTicks);
        tag.putInt("HauntDuration", hauntDuration);
        tag.putBoolean("Dormant", dormant);
        tag.putInt("PhaseTicks", phaseTicks);
        tag.putInt("PhaseDuration", phaseDuration);
        tag.putBoolean("AfkStare", afkStare);
        if (targetPlayer != null) tag.putUUID("TargetPlayer", targetPlayer);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifeTicks = tag.getInt("LifeTicks");
        if (tag.contains("HauntDuration")) hauntDuration = tag.getInt("HauntDuration");
        dormant = tag.getBoolean("Dormant");
        phaseTicks = tag.getInt("PhaseTicks");
        phaseDuration = tag.getInt("PhaseDuration");
        afkStare = tag.getBoolean("AfkStare");
        setInvisible(false); // r178: nunca recarrega preso invisível
        if (tag.hasUUID("TargetPlayer")) targetPlayer = tag.getUUID("TargetPlayer");
    }

    @Override public boolean canBeAffected(MobEffectInstance e) { return false; }
}
