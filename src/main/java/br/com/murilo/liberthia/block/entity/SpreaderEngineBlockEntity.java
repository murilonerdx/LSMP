package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.config.LiberthiaConfig;
import br.com.murilo.liberthia.logic.entropy.EntropyTracker;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModTech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

/**
 * r183 — <b>Motor Espalhador</b> (Entropy / Black Matter). Enquanto NÃO for destruído,
 * troca os blocos da região por blocos de outras dimensões / matéria escura,
 * expandindo o raio e acelerando com o tempo (entropia cresce). Grava CADA troca no
 * {@link EntropyTracker} (JSON por id na pasta do mundo) e, ao ser destruído, REVERTE tudo.
 * Players na área perdem sanidade (Entropy) ou levam fome + dano (Black Matter).
 */
public class SpreaderEngineBlockEntity extends BlockEntity {

    public enum Type { ENTROPY, BLACK_MATTER }

    private static final int INTENSITY_MAX = 200;
    private String id = "";
    private int intensity = 0;
    private long age = 0;
    private int spreadRadius = 3;     // r186: cresce ilimitado até o teto de config (BLACK_MATTER)
    private boolean enabled = true;   // r186: toggle por comando (não persiste — reseta no reload)
    private int sporeCheckCooldown = 0; // r186: cooldown da checagem de esporos (Black Hole)
    private static final int SPORE_BLOOM_THRESHOLD = 15;

    // r186: registro p/ comandos (engine id -> BE vivo). Limpa via WeakReference.
    private static final ConcurrentHashMap<String, WeakReference<SpreaderEngineBlockEntity>> REGISTRY = new ConcurrentHashMap<>();
    public static java.util.Set<String> activeIds() { return java.util.Set.copyOf(REGISTRY.keySet()); }
    public static SpreaderEngineBlockEntity getActiveById(String id) {
        WeakReference<SpreaderEngineBlockEntity> ref = REGISTRY.get(id);
        return ref != null ? ref.get() : null;
    }
    public void setEnabled(boolean v) { this.enabled = v; }
    public boolean isEnabled() { return this.enabled; }
    public int getSpreadRadius() { return spreadRadius; }
    public void setSpreadRadius(int r) { this.spreadRadius = Math.max(1, r); this.setChanged(); }

    public SpreaderEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModTech.SPREADER_ENGINE_BE.get(), pos, state);
    }

    private Type type() {
        return getBlockState().getBlock() == ModTech.BLACK_MATTER_ENGINE.get() ? Type.BLACK_MATTER : Type.ENTROPY;
    }
    public String getId() { return id; }
    public int getIntensity() { return intensity; }

    private int maxRadius() {
        return type() == Type.BLACK_MATTER ? LiberthiaConfig.SERVER.blackMatterEngineMaxRadius.get() : 26;
    }
    private int radius() {
        if (type() == Type.BLACK_MATTER) return Math.min(spreadRadius, maxRadius());
        return 3 + (int) ((maxRadius() - 3) * (intensity / (float) INTENSITY_MAX));
    }

    public static void tick(Level lvl, BlockPos pos, BlockState st, SpreaderEngineBlockEntity be) {
        if (lvl.isClientSide || !(lvl instanceof ServerLevel sl)) return;
        if (be.id.isEmpty()) { be.id = pos.getX() + "_" + pos.getY() + "_" + pos.getZ(); be.setChanged(); }
        REGISTRY.put(be.id, new WeakReference<>(be)); // r186: registra p/ comandos
        be.age++;

        boolean black = be.type() == Type.BLACK_MATTER;
        // r186: kill-switches (comando + config) só pro Black Matter
        if (black && (!be.enabled || !LiberthiaConfig.SERVER.blackMatterEngineEnabled.get())) return;

        int speedMul = black ? LiberthiaConfig.SERVER.blackMatterEngineSpeedMultiplier.get() : 1;

        // entropia cresce com o tempo (acelera)
        if (be.age % 8 == 0 && be.intensity < INTENSITY_MAX) be.intensity++;

        // r186: raio cresce ILIMITADO (até o teto de config) — trabalho/tick continua pequeno
        if (black) {
            int radiusInterval = Math.max(20, 200 / speedMul);
            if (be.age % radiusInterval == 0 && be.spreadRadius < be.maxRadius()) { be.spreadRadius++; be.setChanged(); }
            // r186: campo de proteção do núcleo
            if (be.age % 20 == 0) be.protectCore(sl, pos);
        }

        int interval = Math.max(1, (16 - be.intensity / 16) / speedMul);
        int batch = (1 + be.intensity / 20) * speedMul;
        if (be.age % interval == 0) {
            for (int i = 0; i < batch; i++) {
                if (black) be.spreadBlackMatter(sl, pos);
                else be.spreadEntropy(sl, pos);
            }
        }

        if (be.age % 20 == 0) be.affectPlayers(sl, pos);

        // r186: gatilho do Buraco Negro de esporos
        if (black) {
            if (be.sporeCheckCooldown > 0) be.sporeCheckCooldown--;
            else {
                be.sporeCheckCooldown = 200; // checa a cada 10s
                int scanR = Math.min(be.radius(), 24); // limita o scan p/ não lagar
                if (!sporeBlackHoleAlreadyExists(sl, pos) && countSporeBloomsNear(sl, pos, scanR) >= SPORE_BLOOM_THRESHOLD)
                    spawnSporeBlackHole(sl, pos, be);
            }
        }

        if (be.age % 100 == 0) { if (sl.getServer() != null) EntropyTracker.flush(sl.getServer()); be.setChanged(); }
    }

    private static int countSporeBloomsNear(ServerLevel sl, BlockPos origin, int radius) {
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                int topY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (int dy = 0; dy <= 3; dy++) {
                    BlockPos p = new BlockPos(x, topY + dy, z);
                    if (!sl.hasChunkAt(p)) continue;
                    if (sl.getBlockState(p).is(ModBlocks.SPORE_BLOOM.get())) count++;
                }
            }
        }
        return count;
    }

    private static boolean sporeBlackHoleAlreadyExists(ServerLevel sl, BlockPos enginePos) {
        String eid = enginePos.getX() + "_" + enginePos.getY() + "_" + enginePos.getZ();
        AABB scan = new AABB(enginePos).inflate(60);
        for (br.com.murilo.liberthia.entity.DarkMatterBlackHoleEntity bh :
                sl.getEntitiesOfClass(br.com.murilo.liberthia.entity.DarkMatterBlackHoleEntity.class, scan)) {
            if (eid.equals(bh.getEngineId())) return true;
        }
        return false;
    }

    private static void spawnSporeBlackHole(ServerLevel sl, BlockPos enginePos, SpreaderEngineBlockEntity be) {
        int topY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, enginePos.getX(), enginePos.getZ());
        BlockPos spawn = new BlockPos(enginePos.getX(), topY + 6, enginePos.getZ());
        br.com.murilo.liberthia.entity.DarkMatterBlackHoleEntity.spawn(sl, spawn, be.id, enginePos);
    }

    /** r186: campo de proteção — mobs hostis perto do núcleo tomam dano e são repelidos. */
    private void protectCore(ServerLevel sl, BlockPos pos) {
        final double PR = 6.0;
        AABB box = new AABB(pos).inflate(PR);
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        for (Mob mob : sl.getEntitiesOfClass(Mob.class, box)) {
            if (mob.distanceToSqr(cx, cy, cz) > PR * PR) continue;
            mob.hurt(mob.damageSources().magic(), 2.0F);
            double dx = mob.getX() - cx, dz = mob.getZ() - cz;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0.01) mob.setDeltaMovement(mob.getDeltaMovement().add((dx / len) * 0.6, 0.3, (dz / len) * 0.6));
            mob.hurtMarked = true;
            sl.sendParticles(ParticleTypes.SQUID_INK, mob.getX(), mob.getY() + 0.5, mob.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
        }
    }

    // ── ENTROPY: superfície vira infecção; 4 camadas abaixo viram rifts dimensionais ──
    private void spreadEntropy(ServerLevel sl, BlockPos origin) {
        int r = radius();
        int dx = sl.random.nextInt(r * 2 + 1) - r, dz = sl.random.nextInt(r * 2 + 1) - r;
        if (dx * dx + dz * dz > r * r) return;
        int x = origin.getX() + dx, z = origin.getZ() + dz;
        int topY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        if (topY <= sl.getMinBuildHeight() + 1) return;

        // superfície
        BlockState surf = pickEntropySurface(sl);
        convert(sl, new BlockPos(x, topY, z), surf);
        // 4 camadas abaixo = rifts (mais fundo = pior)
        convert(sl, new BlockPos(x, topY - 1, z), ModBlocks.DIMENSIONAL_FLUX.get().defaultBlockState());
        convert(sl, new BlockPos(x, topY - 2, z), ModBlocks.WARPED_SPACE.get().defaultBlockState());
        convert(sl, new BlockPos(x, topY - 3, z), ModBlocks.VOID_SCAR.get().defaultBlockState());
        convert(sl, new BlockPos(x, topY - 4, z), ModBlocks.RIFT_RESIDUE.get().defaultBlockState());

        if (sl.random.nextInt(4) == 0)
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, x + 0.5, topY + 1.2, z + 0.5, 2, 0.3, 0.3, 0.3, 0.02);
    }

    private BlockState pickEntropySurface(ServerLevel sl) {
        int r = sl.random.nextInt(100);
        if (r < 45) return ModBlocks.CORRUPTED_SOIL.get().defaultBlockState();
        if (r < 70) return ModBlocks.CORRUPTED_STONE.get().defaultBlockState();
        if (r < 82) return ModBlocks.INFECTION_GROWTH.get().defaultBlockState();
        if (r < 92) return ModBlocks.SPORE_BLOOM.get().defaultBlockState();
        return ModBlocks.DIMENSIONAL_FLUX.get().defaultBlockState();
    }

    // ── BLACK MATTER: blocos ABAIXO viram matéria escura (todos os tipos) ──
    private void spreadBlackMatter(ServerLevel sl, BlockPos origin) {
        int r = radius();
        int dx = sl.random.nextInt(r * 2 + 1) - r, dz = sl.random.nextInt(r * 2 + 1) - r;
        if (dx * dx + dz * dz > r * r) return;
        int x = origin.getX() + dx, z = origin.getZ() + dz;
        int topY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        if (topY <= sl.getMinBuildHeight() + 1) return;

        // superfície + camadas abaixo, mapeadas pelo bloco-fonte (só os blocos permitidos)
        BlockPos surf = new BlockPos(x, topY, z);
        convert(sl, surf, pickBlackMatter(sl, sl.getBlockState(surf)));
        int depth = 2 + intensity / 30; // 2 → ~8
        for (int y = topY - 1; y > topY - depth; y--) {
            BlockPos sub = new BlockPos(x, y, z);
            convert(sl, sub, pickBlackMatter(sl, sl.getBlockState(sub)));
        }

        // esporo raro no ar (gatilho do Black Hole)
        if (sl.random.nextInt(8) == 0) {
            BlockPos sp = new BlockPos(x, topY + 1, z);
            if (sl.getBlockState(sp).isAir()) {
                EntropyTracker.record(sl, id, sp, sl.getBlockState(sp));
                sl.setBlock(sp, ModBlocks.SPORE_BLOOM.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        if (sl.random.nextInt(3) == 0)
            sl.sendParticles(ParticleTypes.SQUID_INK, x + 0.5, topY + 1.0, z + 0.5, 2, 0.3, 0.2, 0.3, 0.0);
    }

    /** r186: escolhe o bloco infectado pelo TIPO do bloco-fonte (só os blocos permitidos) + crystalized RARO. */
    private BlockState pickBlackMatter(ServerLevel sl, BlockState source) {
        // r196: crystalized_dark_matter NÃO entra mais na infecção (pedido do user) — só os blocos infectados.
        Block src = source.getBlock();
        if (src == Blocks.GRASS_BLOCK)
            return ModBlocks.DARK_INFECTED_GRASS.get().defaultBlockState();
        if (src == Blocks.DIRT || src == Blocks.PODZOL || src == Blocks.ROOTED_DIRT
                || src == Blocks.MYCELIUM || src == Blocks.MUD || src == Blocks.COARSE_DIRT
                || source.is(BlockTags.DIRT))
            return ModBlocks.DARK_INFECTED_DIRT.get().defaultBlockState();
        if (src == Blocks.SAND || src == Blocks.RED_SAND || src == Blocks.GRAVEL || source.is(BlockTags.SAND))
            return ModBlocks.DARK_INFECTED_SAND.get().defaultBlockState();
        if (source.is(BlockTags.LOGS) || source.is(BlockTags.PLANKS)) {
            if (source.hasProperty(BlockStateProperties.AXIS)) {
                Direction.Axis axis = source.getValue(BlockStateProperties.AXIS);
                return ModBlocks.CORRUPTED_LOG.get().defaultBlockState().setValue(BlockStateProperties.AXIS, axis);
            }
            return ModBlocks.CORRUPTED_LOG.get().defaultBlockState();
        }
        if (src == Blocks.STONE || src == Blocks.COBBLESTONE || src == Blocks.DEEPSLATE || src == Blocks.COBBLED_DEEPSLATE
                || src == Blocks.ANDESITE || src == Blocks.DIORITE || src == Blocks.GRANITE
                || src == Blocks.TUFF || src == Blocks.CALCITE || src == Blocks.SMOOTH_STONE)
            return ModBlocks.DARK_INFECTED_STONE.get().defaultBlockState();
        if (source.is(BlockTags.STONE_ORE_REPLACEABLES) || source.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || source.is(BlockTags.COAL_ORES) || source.is(BlockTags.IRON_ORES)
                || source.is(BlockTags.GOLD_ORES) || source.is(BlockTags.DIAMOND_ORES))
            return ModBlocks.CORRUPTED_STONE.get().defaultBlockState();
        return ModBlocks.CORRUPTED_SOIL.get().defaultBlockState();
    }

    private void convert(ServerLevel sl, BlockPos pos, BlockState target) {
        if (pos.equals(worldPosition)) return;                          // FIX: nunca troca o próprio motor
        BlockState cur = sl.getBlockState(pos);
        if (cur.isAir() || cur.is(Blocks.BEDROCK) || cur.getDestroySpeed(sl, pos) < 0) return; // pula ar/bedrock/indestrutível
        if (cur.is(target.getBlock()) || isConverted(cur)) return;                              // já trocado
        if (sl.getBlockEntity(pos) != null) return;                     // FIX: protege QUALQUER bloco com BlockEntity (motores/máquinas/baús)
        EntropyTracker.record(sl, id, pos, cur);
        sl.setBlock(pos, target, net.minecraft.world.level.block.Block.UPDATE_ALL);
    }

    private boolean isConverted(BlockState s) {
        return s.is(ModBlocks.CORRUPTED_SOIL.get()) || s.is(ModBlocks.CORRUPTED_STONE.get())
                || s.is(ModBlocks.CORRUPTED_LOG.get())
                || s.is(ModBlocks.INFECTION_GROWTH.get()) || s.is(ModBlocks.SPORE_BLOOM.get())
                || s.is(ModBlocks.DARK_MATTER_BLOCK.get()) || s.is(ModBlocks.DIMENSIONAL_FLUX.get())
                || s.is(ModBlocks.WARPED_SPACE.get()) || s.is(ModBlocks.VOID_SCAR.get())
                || s.is(ModBlocks.RIFT_RESIDUE.get()) || s.is(ModTech.CRYSTALIZED_DARK_MATTER.get())
                || s.is(ModBlocks.DARK_INFECTED_GRASS.get()) || s.is(ModBlocks.DARK_INFECTED_DIRT.get())
                || s.is(ModBlocks.DARK_INFECTED_SAND.get()) || s.is(ModBlocks.DARK_INFECTED_STONE.get());
    }

    // ── efeitos nos players da área ──
    private void affectPlayers(ServerLevel sl, BlockPos pos) {
        int r = radius();
        AABB box = new AABB(pos).inflate(r);
        boolean black = type() == Type.BLACK_MATTER;
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (p.isCreative() || p.isSpectator()) continue;
            if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > (double) r * r) continue;
            p.getCapability(ModCapabilities.INFECTION).ifPresent(d -> { d.addInfection(black ? 1 : 2); d.setDirty(true); });
            if (black) {
                p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0, false, true));
                p.hurt(p.damageSources().magic(), 1.0F);                       // dano da matéria escura
                // r186: os blocos infectados emitem RADIAÇÃO
                p.addEffect(new MobEffectInstance(ModEffects.RADIATION_SICKNESS.get(), 120, 0, false, true));
                double dd = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (dd <= (double) (r / 2) * (r / 2))
                    p.addEffect(new MobEffectInstance(ModEffects.DIMENSIONAL_RADIATION.get(), 80, 0, false, true));
            } else {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, true));
                if (sl.random.nextInt(3) == 0) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, true));
            }
            // r184: exposição prolongada à entropia/matéria escura pode transmitir uma doença dimensional
            if (sl.random.nextInt(1200) == 0)
                br.com.murilo.liberthia.event.DimensionalDiseaseHandler.contractRandom(p);
        }
        // r196: a infecção também FERE mobs na zona (até morrer) — exceto as criaturas cósmicas (imunes à própria infecção)
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        for (net.minecraft.world.entity.LivingEntity le : sl.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, box)) {
            if (le instanceof Player) continue;
            if (le instanceof br.com.murilo.liberthia.cosmic.ICosmicHorror) continue; // criaturas da matéria escura sobrevivem
            if (le.distanceToSqr(cx, cy, cz) > (double) r * r) continue;
            le.hurt(le.damageSources().magic(), black ? 2.0F : 1.0F);
        }
        ParticleOptions amb = black ? ParticleTypes.LARGE_SMOKE : ParticleTypes.PORTAL;
        sl.sendParticles(amb, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, r * 0.3, 0.5, r * 0.3, 0.01);
    }

    /** Chamado quando o bloco é destruído — reverte TUDO que esse motor trocou. */
    public void revertOnDestroy() {
        if (!id.isEmpty()) REGISTRY.remove(id); // r186: tira do registro de comandos
        if (level instanceof ServerLevel sl && sl.getServer() != null && !id.isEmpty()) {
            // r186: mata o Buraco Negro vinculado a este motor
            AABB box = new AABB(worldPosition).inflate(80);
            for (br.com.murilo.liberthia.entity.DarkMatterBlackHoleEntity bh :
                    sl.getEntitiesOfClass(br.com.murilo.liberthia.entity.DarkMatterBlackHoleEntity.class, box)) {
                if (id.equals(bh.getEngineId())) bh.discard();
            }
            EntropyTracker.revert(sl.getServer(), id);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("eid", id);
        tag.putInt("intensity", intensity);
        tag.putLong("age", age);
        tag.putInt("spreadRadius", spreadRadius);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        id = tag.getString("eid");
        intensity = tag.getInt("intensity");
        age = tag.getLong("age");
        spreadRadius = tag.contains("spreadRadius") ? tag.getInt("spreadRadius") : 3;
    }
}
