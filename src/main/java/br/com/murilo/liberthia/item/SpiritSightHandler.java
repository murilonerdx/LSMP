package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * r173: <b>Spirit Sight</b> — a parte "real" do Mapa Invertido.
 *
 * <p>Enquanto a visão invertida está ativa, o player <b>enxerga o mundo
 * espiritual sem ir pra lá</b>:
 * <ul>
 *   <li><b>Entidades espirituais</b> (loom/watcher/peripheral/ghost/shade/...)
 *       ganham GLOWING — contorno visível através das paredes.</li>
 *   <li><b>Blocos espirituais</b> (whisperwood, spirit ores, soul*, candle, chalk,
 *       rune...) ao redor são revelados com partículas de alma — você vê o que
 *       sempre esteve colado à nossa realidade.</li>
 * </ul>
 *
 * <p>O shader de inversão (cliente) continua rodando por cima; isto adiciona a
 * substância: ver, de fato, o lado de lá.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SpiritSightHandler {

    private SpiritSightHandler() {}

    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();
    private static final int REVEAL_RADIUS = 10;
    private static final int MAX_BLOCKS_PER_PASS = 48;

    /** Palavras-chave que marcam um bloco/entidade como "espiritual". */
    private static final String[] SPIRIT_KEYS = {
            "spirit", "whisper", "soul", "ghost", "shade", "wraith", "pale",
            "loom", "watcher", "peripheral", "observer", "rune", "chalk",
            "candle", "ectoplasm", "phantom", "void", "cosmic"
    };

    public static void activate(ServerPlayer sp, int ticks) {
        ACTIVE.put(sp.getUUID(), ticks);
        revealAround(sp); // pulso imediato
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;

        Integer t = ACTIVE.get(sp.getUUID());
        if (t == null) return;
        t--;
        if (t <= 0) { ACTIVE.remove(sp.getUUID()); return; }
        ACTIVE.put(sp.getUUID(), t);

        if (sp.tickCount % 12 == 0) {
            revealAround(sp);
        }
    }

    private static void revealAround(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel sl)) return;

        // 1) Entidades espirituais → GLOWING (visíveis através das paredes)
        AABB box = sp.getBoundingBox().inflate(40);
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box, l -> l != sp)) {
            if (isSpiritual(le)) {
                le.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
            }
        }

        // 2) Blocos espirituais ao redor → partículas de alma
        BlockPos c = sp.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        for (int dx = -REVEAL_RADIUS; dx <= REVEAL_RADIUS; dx++) {
            for (int dy = -REVEAL_RADIUS; dy <= REVEAL_RADIUS; dy++) {
                for (int dz = -REVEAL_RADIUS; dz <= REVEAL_RADIUS; dz++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (isSpiritualBlock(sl.getBlockState(p))) {
                        found.add(p);
                    }
                }
            }
        }
        // limita pra não floodar partículas
        int step = Math.max(1, found.size() / MAX_BLOCKS_PER_PASS);
        for (int i = 0; i < found.size(); i += step) {
            BlockPos p = found.get(i);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5,
                    2, 0.25, 0.25, 0.25, 0.005);
        }
    }

    private static boolean isSpiritual(LivingEntity le) {
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(le.getType());
        if (rl == null) return false;
        String path = rl.getPath();
        for (String k : SPIRIT_KEYS) if (path.contains(k)) return true;
        return false;
    }

    private static boolean isSpiritualBlock(BlockState state) {
        if (state.isAir()) return false;
        ResourceLocation rl = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (rl == null) return false;
        String path = rl.getPath();
        for (String k : SPIRIT_KEYS) if (path.contains(k)) return true;
        return false;
    }
}
