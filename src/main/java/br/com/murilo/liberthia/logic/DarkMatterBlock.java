package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.entity.DarkMatterSporeEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bloco principal da matéria escura.
 *
 * Essa classe herda de Block, então ela define o comportamento do bloco
 * quando ele é colocado, quando recebe tick agendado, quando recebe random tick
 * e como ele espalha a infecção ao redor.
 */
public class DarkMatterBlock extends Block {

    /**
     * Construtor do bloco.
     *
     * Recebe as propriedades do bloco (resistência, som, material, randomTicks etc.)
     * e repassa para a classe pai Block.
     */
    public DarkMatterBlock(Properties properties) {
        super(properties);
    }

    /**
     * Chamado quando o bloco é colocado no mundo.
     *
     * Parâmetros:
     * - state: estado atual desse bloco recém-colocado
     * - level: mundo onde ele foi colocado
     * - pos: posição do bloco
     * - oldState: estado anterior naquela posição
     * - isMoving: indica se a mudança veio de movimentação de bloco
     *
     * O que esse método faz:
     * 1. Chama o onPlace da superclasse.
     * 2. Agenda um tick para este bloco daqui a 12000 ticks.
     *
     * Chamada importante:
     * - level.scheduleTick(pos, this, 12000)
     *   Agenda um tick futuro para esse bloco específico.
     *
     * Efeito prático:
     * - depois de um tempo, esse bloco vai passar pelo método tick(...)
     *   e derreter/transformar em fluido.
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        // Agenda um tick futuro para este bloco.
        // "this" = o próprio bloco DarkMatterBlock.
        // 12000 ticks ≈ 10 minutos em condições normais.
        level.scheduleTick(pos, this, 12000);
    }

    /**
     * Tick agendado do bloco.
     *
     * Esse método NÃO é o random tick; ele é chamado quando o tick
     * agendado em onPlace(...) vence.
     *
     * O que ele faz:
     * 1. Chama o tick da superclasse.
     * 2. Substitui este bloco por DARK_MATTER_FLUID_BLOCK.
     *
     * Chamada importante:
     * - level.setBlock(pos, novoEstado, 3)
     *   Troca o bloco da posição por outro bloco/estado.
     *
     * O flag 3 normalmente indica:
     * - atualização do cliente
     * - update visual e de vizinhança
     *
     * Efeito prático:
     * - o bloco sólido de matéria escura "derrete" e vira fluido.
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);

        // Troca esse bloco pelo bloco de fluido de matéria escura.
        level.setBlock(pos, ModBlocks.DARK_MATTER_FLUID_BLOCK.get().defaultBlockState(), 3);
    }

    /**
     * Tenta lançar um esporo de matéria escura.
     *
     * Esse método é privado e auxiliar.
     * Ele só é chamado dentro de randomTick(...).
     *
     * Fluxo:
     * 1. Tem 5% de chance de continuar.
     * 2. Conta quantos blocos desse mesmo tipo existem numa área 7x7x7 ao redor.
     * 3. Se houver densidade suficiente (>= 10), cria um esporo.
     * 4. Posiciona o esporo acima do bloco.
     * 5. Define um alvo aleatório de 32 a 64 blocos de distância.
     * 6. Adiciona a entidade no mundo.
     *
     * Chamadas importantes:
     * - random.nextFloat()
     *   Gera chance aleatória.
     *
     * - BlockPos.betweenClosed(...)
     *   Percorre uma caixa fechada de posições.
     *
     * - level.getBlockState(p).is(this)
     *   Verifica se o bloco naquela posição é este mesmo tipo de bloco.
     *
     * - ModEntities.DARK_MATTER_SPORE.get().create(level)
     *   Cria a entidade do esporo registrada no mod.
     *
     * - spore.setPos(...)
     *   Define a posição inicial da entidade.
     *
     * - spore.setTarget(...)
     *   Define para onde o esporo vai viajar.
     *
     * - level.addFreshEntity(spore)
     *   Spawn real da entidade no mundo.
     */
    private void attemptSporeLaunch(ServerLevel level, BlockPos pos, RandomSource random) {
        // 5% de chance de prosseguir.
        // Se cair acima de 0.05, sai imediatamente.
        if (random.nextFloat() > 0.05f) return;

        // Conta quantos blocos DarkMatterBlock existem na área ao redor.
        int density = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))) {
            if (level.getBlockState(p).is(this)) {
                density++;
            }
        }

        // Só lança esporo se existir massa crítica local.
        if (density >= 10) {
            DarkMatterSporeEntity spore = ModEntities.DARK_MATTER_SPORE.get().create(level);

            if (spore != null) {
                // Coloca o esporo um pouco acima do bloco.
                spore.setPos(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);

                // Define um alvo aleatório entre 32 e 64 blocos de distância.
                double dist = 32.0 + random.nextDouble() * 32.0;
                double angle = random.nextDouble() * Math.PI * 2;

                double targetX = pos.getX() + Math.cos(angle) * dist;
                double targetZ = pos.getZ() + Math.sin(angle) * dist;

                // O alvo também sobe no eixo Y para parecer um disparo/aéreo.
                spore.setTarget(new Vec3(
                        targetX,
                        pos.getY() + 10.0 + random.nextInt(20),
                        targetZ
                ));

                // Adiciona a entidade no mundo.
                level.addFreshEntity(spore);
            }
        }
    }

    /**
     * Random tick do bloco.
     *
     * Esse é o coração do comportamento "vivo" do bloco.
     * Ele é chamado aleatoriamente pelo jogo, desde que o bloco esteja
     * configurado para receber random ticks.
     *
     * Fluxo:
     * 1. Calcula a densidade de infecção da chunk/região.
     * 2. Converte isso em número de tentativas de espalhamento.
     * 3. Chama spreadInfection(...) várias vezes.
     * 4. Calcula a chance de lançar esporo baseada na densidade.
     * 5. Pode chamar attemptSporeLaunch(...).
     * 6. Pode gerar partículas de fumaça.
     *
     * Chamadas importantes:
     * - InfectionLogic.getChunkInfectionDensity(level, pos)
     *   Mede a densidade de infecção local.
     *
     * - spreadInfection(level, pos, random)
     *   Faz uma tentativa de expansão/corrupção.
     *
     * - level.sendParticles(...)
     *   Envia partículas no mundo servidor para efeito visual.
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Mede a densidade local de infecção.
        float density = InfectionLogic.getChunkInfectionDensity(level, pos);

        // Quanto maior a densidade, mais tentativas de espalhar.
        int attempts = 3 + (int) (density * 10);

        // Tenta espalhar infecção múltiplas vezes no mesmo random tick.
        for (int i = 0; i < attempts; i++) {
            spreadInfection(level, pos, random);
        }

        // Chance de lançar esporo cresce com a densidade.
        float sporeChance = 0.03f + (density * 0.07f);
        if (random.nextFloat() < sporeChance) {
            attemptSporeLaunch(level, pos, random);
        }

        // Efeito visual atmosférico de névoa/fumaça escura.
        if (random.nextFloat() < (0.2f + density * 0.4f)) {
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SMOKE,
                    pos.getX() + 0.5,
                    pos.getY() + 1.1,
                    pos.getZ() + 0.5,
                    6,      // quantidade
                    0.4,    // espalhamento X
                    0.4,    // espalhamento Y
                    0.4,    // espalhamento Z
                    0.06    // velocidade
            );
        }
    }

    /**
     * Faz uma tentativa de espalhar a infecção para um bloco vizinho.
     *
     * Esse método:
     * 1. Escolhe uma direção aleatória.
     * 2. Pega o bloco vizinho naquela direção.
     * 3. Reage de formas diferentes conforme o tipo do bloco vizinho.
     *
     * Regras principais:
     * - Se encontrar CLEAR_MATTER_BLOCK:
     *   explode, remove os blocos, infecta entidades próximas e dropa yellow matter.
     *
     * - Se encontrar YELLOW_MATTER_BLOCK:
     *   para imediatamente, sem espalhar.
     *
     * - Se encontrar terra/grama:
     *   converte em CORRUPTED_SOIL.
     *
     * - Se encontrar pedra/cobblestone/sand/gravel/deepslate/snow:
     *   converte em DARK_MATTER_BLOCK.
     *
     * - Se encontrar logs/leaves/flowers:
     *   converte em DARK_MATTER_FLUID_BLOCK.
     *
     * - Se houver espaço acima e a pilha não estiver muito alta:
     *   cria INFECTION_GROWTH.
     *
     * - Depois disso ainda faz 3 tentativas extras horizontais de corromper chão.
     *
     * Chamadas importantes:
     * - Direction.getRandom(random)
     *   Escolhe uma direção aleatória entre as possíveis.
     *
     * - pos.relative(dir)
     *   Calcula a posição vizinha naquela direção.
     *
     * - level.getBlockState(neighborPos)
     *   Lê o bloco vizinho.
     *
     * - neighborState.is(...)
     *   Verifica se o estado atual pertence ao bloco/tag informado.
     *
     * - level.explode(...)
     *   Cria explosão real no mundo.
     *
     * - level.removeBlock(...)
     *   Remove um bloco da posição.
     *
     * - new AABB(neighborPos).inflate(16)
     *   Cria área de busca em torno da explosão.
     *
     * - level.getEntitiesOfClass(...)
     *   Busca entidades vivas na área.
     *
     * - target.getCapability(...).ifPresent(...)
     *   Acessa a capability de infecção e altera o dado.
     *
     * - level.setBlockAndUpdate(...)
     *   Troca bloco e sincroniza atualização.
     */
    private void spreadInfection(ServerLevel level, BlockPos pos, RandomSource random) {
        // Escolhe uma direção qualquer.
        Direction dir = Direction.getRandom(random);

        // Posição vizinha naquela direção.
        BlockPos neighborPos = pos.relative(dir);

        // Estado do bloco vizinho.
        BlockState neighborState = level.getBlockState(neighborPos);

        // Reação especial contra matéria clara.
        if (neighborState.is(ModBlocks.CLEAR_MATTER_BLOCK.get())) {
            // Explode no ponto do bloco claro.
            level.explode(
                    null,
                    neighborPos.getX(),
                    neighborPos.getY(),
                    neighborPos.getZ(),
                    4.0F,
                    Level.ExplosionInteraction.BLOCK
            );

            // Remove o bloco claro e também o bloco de matéria escura atual.
            level.removeBlock(neighborPos, false);
            level.removeBlock(pos, false);

            // Cria uma área de 16 blocos ao redor para infectar seres vivos.
            AABB area = new AABB(neighborPos).inflate(4);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area);

            for (LivingEntity target : entities) {
                target.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                    data.addInfection(5);
                    data.setDirty(true);
                });
            }

            // Dropa um item de yellow matter no local da reação.
            ItemEntity item = new ItemEntity(
                    level,
                    neighborPos.getX(),
                    neighborPos.getY(),
                    neighborPos.getZ(),
                    new ItemStack(ModItems.YELLOW_MATTER_BLOCK_ITEM.get())
            );
            level.addFreshEntity(item);

            // Encerra o método porque essa reação especial já consumiu a tentativa.
            return;
        }

        // Matéria amarela bloqueia a propagação (hard stop).
        if (ProtectionUtils.isYellowMatterBlock(neighborState)) {
            return;
        }

        // Check if target position is within a protection zone (clear or yellow matter nearby)
        if (ProtectionUtils.isSpreadBlockedByProtectiveBlocks(level, neighborPos)) {
            return;
        }

        // Se for grama ou terra, corrompe o solo.
        if (neighborState.is(Blocks.GRASS_BLOCK) || neighborState.is(Blocks.DIRT)) {
            level.setBlockAndUpdate(neighborPos, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState());
        }
        // Se for materiais "minerais/terreno", transforma em bloco de matéria escura.
        else if (neighborState.is(Blocks.STONE)
                || neighborState.is(Blocks.COBBLESTONE)
                || neighborState.is(Blocks.SAND)
                || neighborState.is(Blocks.GRAVEL)
                || neighborState.is(Blocks.DEEPSLATE)
                || neighborState.is(Blocks.SNOW_BLOCK)) {
            level.setBlockAndUpdate(neighborPos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
        }

        // Se for madeira, folhas ou flores, derrete/contamina para fluido.
        if (neighborState.is(BlockTags.LOGS)
                || neighborState.is(BlockTags.LEAVES)
                || neighborState.is(BlockTags.FLOWERS)) {
            level.setBlockAndUpdate(neighborPos, ModBlocks.DARK_MATTER_FLUID_BLOCK.get().defaultBlockState());
        }

        // Crescimento vertical:
        // só cria INFECTION_GROWTH acima se:
        // 1. a pilha ainda não estiver muito alta
        // 2. o bloco acima for ar
        // 3. o bloco atual/abaixo for sólido para render/suporte
        if (!isTooHigh(level, neighborPos.above())
                && level.getBlockState(neighborPos.above()).isAir()
                && level.getBlockState(neighborPos).isSolidRender(level, neighborPos)) {
            level.setBlockAndUpdate(
                    neighborPos.above(),
                    ModBlocks.INFECTION_GROWTH.get().defaultBlockState()
            );
        }

        // Faz mais 3 tentativas horizontais rápidas para corromper solo ao redor.
        for (int i = 0; i < 1; i++) {
            Direction hDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos hPos = pos.relative(hDir);
            BlockState hState = level.getBlockState(hPos);

            if (hState.is(Blocks.GRASS_BLOCK) || hState.is(Blocks.DIRT)) {
                level.setBlockAndUpdate(hPos, ModBlocks.CORRUPTED_SOIL.get().defaultBlockState());
            }
        }
    }

    /**
     * Verifica se a pilha/crescimento já está alto demais.
     *
     * Como funciona:
     * 1. Começa olhando para baixo da posição recebida.
     * 2. Desce no máximo 6 blocos.
     * 3. Conta quantos blocos consecutivos de infecção existem:
     *    - este próprio bloco (this)
     *    - INFECTION_GROWTH
     *    - CORRUPTED_SOIL
     * 4. Se encontrar 5 ou mais níveis válidos, considera "alto demais".
     *
     * Objetivo:
     * - impedir crescimento vertical infinito ou exagerado.
     *
     * Chamada importante:
     * - current = current.below()
     *   Vai descendo bloco por bloco.
     *
     * Retorno:
     * - true  = já está alto demais
     * - false = ainda pode crescer
     */
    private boolean isTooHigh(ServerLevel level, BlockPos pos) {
        int depth = 0;
        BlockPos current = pos.below();

        while (depth < 6) {
            BlockState b = level.getBlockState(current);

            if (b.is(this)
                    || b.is(ModBlocks.INFECTION_GROWTH.get())
                    || b.is(ModBlocks.CORRUPTED_SOIL.get())) {
                depth++;
                current = current.below();
            } else {
                break;
            }
        }

        return depth >= 5;
    }
}