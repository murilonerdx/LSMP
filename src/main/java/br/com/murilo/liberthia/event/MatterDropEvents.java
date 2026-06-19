package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Eventos de drop/conversão da economia de matter do Liberthia.
 *
 * <h3>1. Dark Matter Shard drop em mineração</h3>
 * Quando o jogador minera blocos de pedra/deepslate/netherrack/blackstone com
 * uma picareta de ferro+ (ou melhor), há uma chance pequena (0.5%) de dropar
 * um Dark Matter Shard. Quanto mais fundo (Y baixo), maior a chance.
 *
 * <h3>2. TNT + Dark Matter Shard → Yellow Matter Ingot</h3>
 * Quando uma explosão acontece, varremos os itens dropados/no chão num raio
 * de explosão+1. Para cada Dark Matter Shard encontrado, converte em Yellow
 * Matter Ingot (1:1). Ideia: jogador coloca shards no chão + TNT, explode,
 * vira ingots amarelos. Mecânica caseira pra obter Yellow Matter sem precisar
 * encontrar minério raro.
 *
 * <p>Registrado em {@link br.com.murilo.liberthia.LiberthiaMod} via Forge bus.
 */
public class MatterDropEvents {

    private static final Logger LOG = LoggerFactory.getLogger(MatterDropEvents.class);

    /** r166: chance base reduzida (0.5% → 0.12%) — estava dropando demais e desbalanceando. */
    private static final double BASE_SHARD_DROP_CHANCE = 0.0012;
    /** Bonus por nível abaixo do mar (Y < 64) — também reduzido. */
    private static final double DEPTH_BONUS_PER_BLOCK = 0.000015;
    /** Chance máxima (3% → 0.8%). */
    private static final double MAX_SHARD_DROP_CHANCE = 0.008;

    private final Random random = new Random();

    /**
     * Drop de Dark Matter Shard quando minera blocos de pedra com picareta de
     * ferro+. Quanto mais fundo, mais chance.
     */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (event.isCanceled()) return;

        BlockState state = event.getState();
        Block block = state.getBlock();
        BlockPos pos = event.getPos();

        // Só dropa em blocos "rochosos" — evita farmar em dirt/etc
        if (!isStoneFamily(block)) return;

        // Player precisa ter picareta de ferro+ (evita farm com mão/madeira)
        if (event.getPlayer() == null) return;
        ItemStack tool = event.getPlayer().getMainHandItem();
        if (!isQualifiedPickaxe(tool)) return;

        // Calcula chance baseada em profundidade
        double chance = BASE_SHARD_DROP_CHANCE;
        if (pos.getY() < 64) {
            chance += (64 - pos.getY()) * DEPTH_BONUS_PER_BLOCK;
            if (chance > MAX_SHARD_DROP_CHANCE) chance = MAX_SHARD_DROP_CHANCE;
        }

        if (random.nextDouble() >= chance) return;

        // Drop o shard como item entity no mundo (não no inventário direto —
        // jogador precisa coletar, mantém feel de "encontrar")
        ItemStack drop = new ItemStack(ModItems.DARK_MATTER_SHARD.get(), 1);
        ItemEntity entity = new ItemEntity(
                serverLevel,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                drop
        );
        entity.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(entity);

        if (LOG.isDebugEnabled()) {
            LOG.debug("[MatterDrop] DarkMatterShard dropped at {} for {} (chance={})",
                    pos, event.getPlayer().getName().getString(), chance);
        }
    }

    /**
     * Conversão de Dark Matter Shard em Yellow Matter Ingot via explosão de TNT.
     *
     * <p>Triggers em {@link ExplosionEvent.Detonate} (depois da explosão calcular
     * blocos afetados, antes do dano final). Procuramos por ItemEntities de Dark
     * Matter Shard no centro da explosão e os convertemos.
     *
     * <p>Conversão é 1:1 com cooldown — o ItemEntity é removido e um novo de
     * Yellow Matter Ingot é spawnado no mesmo lugar.
     */
    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        // Raio efetivo da explosão (fallback 4.0 se não conseguir ler)
        double radius = 4.0;
        try {
            // ExplosionEvent.Detonate não expõe radius diretamente — usar a posição
            // afetada + centro pra estimar. Mas pra simplificar, usamos 4 (TNT).
        } catch (Exception ignored) {}

        net.minecraft.world.phys.Vec3 center = event.getExplosion().getPosition();
        double r2 = radius * radius;

        java.util.List<ItemEntity> nearby = level.getEntitiesOfClass(
                ItemEntity.class,
                new net.minecraft.world.phys.AABB(
                        center.x - radius, center.y - radius, center.z - radius,
                        center.x + radius, center.y + radius, center.z + radius)
        );

        int converted = 0;
        for (ItemEntity ie : nearby) {
            if (ie.isRemoved()) continue;
            ItemStack stack = ie.getItem();
            if (!stack.is(ModItems.DARK_MATTER_SHARD.get())) continue;
            // Cheque distância real (AABB é cubo, queremos esfera)
            double dx = ie.getX() - center.x;
            double dy = ie.getY() - center.y;
            double dz = ie.getZ() - center.z;
            if (dx*dx + dy*dy + dz*dz > r2) continue;

            int count = stack.getCount();
            // Remove o entity antigo
            ie.discard();

            // Spawna o ingot no mesmo lugar
            ItemStack ingot = new ItemStack(ModItems.YELLOW_MATTER_INGOT.get(), count);
            ItemEntity newEntity = new ItemEntity(level, ie.getX(), ie.getY(), ie.getZ(), ingot);
            newEntity.setDefaultPickUpDelay();
            // Pequeno empurrão pra cima (efeito de transformação)
            newEntity.setDeltaMovement(0, 0.3, 0);
            level.addFreshEntity(newEntity);

            converted += count;
        }

        if (converted > 0) {
            // Particle effect + som de "transmutação" no centro
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        center.x, center.y + 0.5, center.z,
                        20, 0.5, 0.5, 0.5, 0.05
                );
                sl.playSound(null, BlockPos.containing(center),
                        net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f);
            }
            LOG.info("[MatterDrop] Converted {} DarkMatterShard → YellowMatterIngot via explosion at {}",
                    converted, center);
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    /** Família de blocos onde DarkMatterShard pode dropar. */
    private static boolean isStoneFamily(Block block) {
        net.minecraft.resources.ResourceLocation rl =
                net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block);
        if (rl == null) return false;
        String path = rl.getPath();
        // Vanilla stones + deepslate + netherrack + blackstone + bedrock-adjacent
        return path.equals("stone")
                || path.equals("cobblestone")
                || path.equals("deepslate")
                || path.equals("cobbled_deepslate")
                || path.equals("tuff")
                || path.equals("netherrack")
                || path.equals("blackstone")
                || path.equals("basalt")
                || path.equals("end_stone")
                || path.equals("dripstone_block")
                // Liberthia próprios — minerar minérios do mod também tem chance
                || path.startsWith("dark_matter_ore")
                || path.startsWith("deepslate_dark_matter_ore")
                || path.startsWith("scarred_stone")
                || path.startsWith("scarred_earth");
    }

    /** Verifica se é uma picareta de ferro+ (vanilla ou similar via tags). */
    private static boolean isQualifiedPickaxe(ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        if (!(item instanceof net.minecraft.world.item.PickaxeItem)) return false;
        net.minecraft.resources.ResourceLocation rl =
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        if (rl == null) return false;
        String path = rl.getPath();
        // Aceita: iron, gold, diamond, netherite, e qualquer matter pickaxe
        if (path.equals("wooden_pickaxe") || path.equals("stone_pickaxe")) return false;
        return path.contains("pickaxe");
    }
}
