package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.capability.InfectionProvider;
import br.com.murilo.liberthia.capability.MatterEnergyProvider;
import br.com.murilo.liberthia.logic.InfectionLogic;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.S2CMatterEnergySyncPacket;
import br.com.murilo.liberthia.registry.ModCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import br.com.murilo.liberthia.command.PurgeInfectionCommand;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Classe responsável por ouvir eventos do Forge relacionados a:
 * - anexar capabilities em entidades/jogadores
 * - clonar dados do player ao morrer/renascer
 * - aplicar lógica de infecção por tick
 * - reagir à morte, pulo, login, respawn e troca de dimensão
 * - registrar comandos do mod
 */
public class InfectionEvents {

    /**
     * ID único da capability de infecção.
     * Esse ResourceLocation identifica a capability "infection" do mod.
     */
    private static final ResourceLocation CAPABILITY_ID =
            new ResourceLocation(LiberthiaMod.MODID, "infection");

    /**
     * ID único da capability de energia/matéria.
     * Essa capability é usada somente em jogadores.
     */
    private static final ResourceLocation ENERGY_CAPABILITY_ID =
            new ResourceLocation(LiberthiaMod.MODID, "matter_energy");

    /**
     * Evento disparado quando uma entidade recebe capabilities.
     *
     * O que esse método faz:
     * 1. Se a entidade for um LivingEntity, adiciona a capability de infecção.
     *    Isso inclui players, mobs, etc.
     * 2. Se a entidade for especificamente um Player, adiciona também
     *    a capability de energia/matéria.
     *
     * Resultado:
     * - Todo ser vivo pode armazenar dados de infecção.
     * - Apenas jogadores armazenam dark/clear/yellow energy.
     */
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            event.addCapability(CAPABILITY_ID, new InfectionProvider());
        }

        if (event.getObject() instanceof net.minecraft.world.entity.player.Player) {
            event.addCapability(ENERGY_CAPABILITY_ID, new MatterEnergyProvider());
        }
    }

    /**
     * Evento chamado quando o jogador é clonado.
     *
     * Isso normalmente acontece no respawn após morrer, ou em algumas trocas internas.
     *
     * O que esse método faz:
     * 1. Reativa temporariamente as capabilities da entidade original.
     * 2. Copia a capability de infecção do player antigo para o novo.
     * 3. Copia a capability de matter energy do player antigo para o novo.
     * 4. Invalida novamente as capabilities do player antigo.
     *
     * Na prática:
     * - o jogador não perde o progresso da infecção ao morrer
     * - também não perde os valores de energia acumulados
     */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        // Reativa as capabilities do jogador original para que possamos ler seus dados
        event.getOriginal().reviveCaps();

        // Copia os dados completos de infecção do jogador antigo para o novo
        event.getOriginal().getCapability(ModCapabilities.INFECTION).ifPresent(oldStore ->
                event.getEntity().getCapability(ModCapabilities.INFECTION).ifPresent(newStore ->
                        newStore.deserializeNBT(oldStore.serializeNBT())));

        // Copia manualmente os dados de energia/matéria no respawn
        event.getOriginal().getCapability(ModCapabilities.MATTER_ENERGY).ifPresent(oldEnergy ->
                event.getEntity().getCapability(ModCapabilities.MATTER_ENERGY).ifPresent(newEnergy -> {
                    newEnergy.setDarkEnergy(oldEnergy.getDarkEnergy());
                    newEnergy.setClearEnergy(oldEnergy.getClearEnergy());
                    newEnergy.setYellowEnergy(oldEnergy.getYellowEnergy());
                    newEnergy.setStabilized(oldEnergy.isStabilized());
                }));

        // Invalida novamente as capabilities do original após a cópia
        event.getOriginal().invalidateCaps();
    }

    /**
     * Evento chamado quando uma entidade viva morre.
     *
     * O que esse método faz:
     * 1. Ignora execução no client, roda apenas no servidor.
     * 2. Obtém a posição da entidade morta.
     * 3. Lê a capability de infecção dela.
     * 4. Se a infecção for >= 50 OU houver blocos infectados próximos,
     *    tenta corromper o chão embaixo da entidade.
     * 5. Se o bloco do chão for GRASS_BLOCK ou DIRT, ele vira CORRUPTED_SOIL.
     *
     * Efeito prático:
     * - mortes em áreas contaminadas ou de entidades muito infectadas
     *   ajudam a espalhar corrupção no terreno.
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        // Não executa no client, apenas no servidor
        if (event.getEntity().level().isClientSide) return;

        LivingEntity entity = event.getEntity();
        net.minecraft.world.level.Level level = entity.level();
        BlockPos pos = entity.blockPosition();

        entity.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
            // Só tenta corromper o chão se:
            // - a entidade estiver bastante infectada
            // OU
            // - já existir contaminação perto
            if (data.getInfection() >= 50 || isNearbyInfected(level, pos)) {
                BlockPos ground = pos;

                // Se a posição exata estiver no ar, desce um bloco para achar o chão
                if (level.getBlockState(ground).isAir()) {
                    ground = ground.below();
                }

                // Só corrompe se o bloco for grama ou terra comum
                if (level.getBlockState(ground).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) ||
                        level.getBlockState(ground).is(net.minecraft.world.level.block.Blocks.DIRT)) {
                    level.setBlockAndUpdate(
                            ground,
                            br.com.murilo.liberthia.registry.ModBlocks.CORRUPTED_SOIL.get().defaultBlockState()
                    );
                }
            }
        });
    }

    /**
     * Método auxiliar para verificar se existe infecção perto de uma posição.
     *
     * Região verificada:
     * - X: de -2 até +2
     * - Y: de -1 até +1
     * - Z: de -2 até +2
     *
     * Ou seja, uma caixa 5x3x5 ao redor da posição.
     *
     * Retorna true se encontrar qualquer um desses blocos:
     * - CORRUPTED_SOIL
     * - DARK_MATTER_BLOCK
     * - INFECTION_GROWTH
     *
     * Caso contrário, retorna false.
     */
    private boolean isNearbyInfected(net.minecraft.world.level.Level level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
            if (level.getBlockState(p).is(br.com.murilo.liberthia.registry.ModBlocks.CORRUPTED_SOIL.get()) ||
                    level.getBlockState(p).is(br.com.murilo.liberthia.registry.ModBlocks.DARK_MATTER_BLOCK.get()) ||
                    level.getBlockState(p).is(br.com.murilo.liberthia.registry.ModBlocks.INFECTION_GROWTH.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Evento executado a cada tick para entidades vivas.
     *
     * O que esse método faz:
     * 1. Ignora execução no client.
     * 2. Se a entidade for um ServerPlayer:
     *    - executa InfectionLogic.tick(player, data)
     * 3. Se for qualquer outro LivingEntity:
     *    - executa InfectionLogic.tickLiving(entity, data)
     *
     * Objetivo:
     * - atualizar continuamente a lógica da infecção
     * - aplicar efeitos, progressão, dano, mutações, exposição etc.
     *
     * Observação:
     * Essa classe só dispara a lógica.
     * O comportamento real está dentro de InfectionLogic.
     */
    @SubscribeEvent
    public void onEntityTick(LivingEvent.LivingTickEvent event) {
        // Não executa no client
        if (event.getEntity().level().isClientSide) return;
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;

        LivingEntity entity = event.getEntity();

        if (entity instanceof ServerPlayer player) {
            // Lógica específica para player
            player.getCapability(ModCapabilities.INFECTION)
                    .ifPresent(data -> InfectionLogic.tick(player, data));

            // Matter Energy decay and sync
            player.getCapability(ModCapabilities.MATTER_ENERGY).ifPresent(energy -> {
                // Sync to client every 20 ticks or when dirty
                if (energy.isDirty() || player.tickCount % 20 == 0) {
                    ModNetwork.sendToPlayer(player, new S2CMatterEnergySyncPacket(
                            energy.getDarkEnergy(), energy.getClearEnergy(),
                            energy.getYellowEnergy(), energy.isStabilized()));
                    energy.setDirty(false);
                }
            });
        } else {
            // Lógica para qualquer outro ser vivo
            entity.getCapability(ModCapabilities.INFECTION)
                    .ifPresent(data -> InfectionLogic.tickLiving(entity, data));
        }
    }

    /**
     * Evento chamado quando uma entidade viva pula.
     *
     * Aqui a lógica só vale para ServerPlayer.
     *
     * O que esse método faz:
     * 1. Verifica se o player tem capability de infecção.
     * 2. Descobre:
     *    - se ele está carregando dark matter
     *    - se possui a mutação "HEAVY_STEPS"
     *    - qual o nível de infecção atual
     * 3. Se qualquer condição relevante for verdadeira:
     *    - reduz a velocidade vertical do pulo
     *
     * Regras:
     * - Se infecção >= 20, já começa a pesar o pulo.
     * - Se estiver carregando dark matter ou tiver HEAVY_STEPS,
     *   o pulo fica ainda mais fraco.
     *
     * Como funciona o factor:
     * - carrying || hasHeavySteps => factor = 0.35
     * - senão => factor vai diminuindo conforme a infecção aumenta
     *            com mínimo de 0.55
     *
     * Efeito prático:
     * - jogador infectado ou carregando matéria escura pula menos
     */
    @SubscribeEvent
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        // Só processa se for player no servidor
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        player.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
            boolean carrying = InfectionLogic.isCarryingDarkMatter(player);
            boolean hasHeavySteps = data.hasMutation("HEAVY_STEPS");
            int infection = data.getInfection();

            if (infection >= 20 || carrying || hasHeavySteps) {
                // Se estiver carregando dark matter ou tiver mutação de passos pesados,
                // o pulo é bem mais afetado.
                // Caso contrário, a redução depende do nível de infecção.
                double factor = (carrying || hasHeavySteps)
                        ? 0.35D
                        : Math.max(0.55D, 1.0D - (infection / 140.0D));

                player.setDeltaMovement(
                        player.getDeltaMovement().x,
                        player.getDeltaMovement().y * factor,
                        player.getDeltaMovement().z
                );

                // Marca que o movimento foi alterado para sincronização/atualização
                player.hurtMarked = true;
            }
        });
    }

    /**
     * Evento chamado quando o jogador entra no mundo.
     *
     * O que esse método faz:
     * - sincroniza os dados de infecção do servidor para o cliente
     *
     * Isso é importante para HUD, efeitos visuais, barra de infecção etc.
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(ModCapabilities.INFECTION)
                    .ifPresent(data -> InfectionLogic.sync(serverPlayer, data));

            // Load persistent infection toggle and sync to this client
            br.com.murilo.liberthia.data.InfectionToggleData toggleData =
                    br.com.murilo.liberthia.data.InfectionToggleData.get(serverPlayer.serverLevel());
            boolean enabled = toggleData.isInfectionEnabled();
            br.com.murilo.liberthia.config.DevMode.ACTIVE = !enabled;
            ModNetwork.sendToPlayer(serverPlayer,
                    new br.com.murilo.liberthia.network.S2CInfectionTogglePacket(enabled));
        }
    }

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        br.com.murilo.liberthia.logic.MatterHistoryManager.registerProtectionBlock(serverLevel, event.getPos(), event.getPlacedBlock());

        if (event.getPlacedBlock().is(br.com.murilo.liberthia.registry.ModBlocks.DARK_MATTER_BLOCK.get())) {
            for (BlockPos nearby : BlockPos.betweenClosed(event.getPos().offset(-1, -1, -1), event.getPos().offset(1, 1, 1))) {
                net.minecraft.world.level.block.state.BlockState state = serverLevel.getBlockState(nearby);
                if (state.is(br.com.murilo.liberthia.registry.ModBlocks.CLEAR_MATTER_BLOCK.get())
                        || state.is(br.com.murilo.liberthia.registry.ModBlocks.YELLOW_MATTER_BLOCK.get())) {
                    serverLevel.setBlockAndUpdate(nearby, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    br.com.murilo.liberthia.logic.MatterHistoryManager.unregisterProtectionBlock(serverLevel, nearby);
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        br.com.murilo.liberthia.logic.MatterHistoryManager.unregisterProtectionBlock(serverLevel, event.getPos());
    }

    /**
     * Evento chamado quando o jogador renasce.
     *
     * O que esse método faz:
     * 1. reaplica efeitos derivados da infecção
     * 2. sincroniza os dados com o cliente
     *
     * "applyDerivedEffects" provavelmente recalcula:
     * - atributos
     * - penalidades
     * - mutações ativas
     * - efeitos persistentes
     */
    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                InfectionLogic.applyDerivedEffects(serverPlayer, data);
                InfectionLogic.sync(serverPlayer, data);
            });
        }
    }

    /**
     * Evento de registro de comandos.
     *
     * O que esse método faz:
     * - registra o comando PurgeInfectionCommand no dispatcher do Minecraft/Forge
     *
     * Esse comando provavelmente serve para limpar, reduzir ou resetar a infecção.
     */
    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        PurgeInfectionCommand.register(event.getDispatcher());
        br.com.murilo.liberthia.command.InfectionToggleCommand.register(event.getDispatcher());
    }

    /**
     * Carrega o estado persistente do toggle de infecção quando o servidor inicia.
     * Isso garante que DevMode.ACTIVE reflita o estado salvo no mundo.
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        br.com.murilo.liberthia.data.InfectionToggleData toggleData =
                br.com.murilo.liberthia.data.InfectionToggleData.get(event.getServer().overworld());
        br.com.murilo.liberthia.config.DevMode.ACTIVE = !toggleData.isInfectionEnabled();
    }

    /**
     * Evento chamado quando o jogador troca de dimensão.
     *
     * O que esse método faz:
     * 1. reaplica efeitos derivados da infecção
     * 2. sincroniza os dados novamente
     *
     * Isso evita problemas ao mudar de dimensão, como:
     * - atributos perdidos
     * - HUD dessincronizada
     * - efeitos que somem ao trocar de mundo
     */
    @SubscribeEvent
    public void onPlayerDimensionChange(PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                InfectionLogic.applyDerivedEffects(serverPlayer, data);
                InfectionLogic.sync(serverPlayer, data);

                // Lore: Horus Island is accessed via the Nether — survivors reported lasting trauma.
                // Entering the Nether with infection >= 50 triggers HORUS_TRAUMA mutation.
                if (event.getTo() == net.minecraft.world.level.Level.NETHER && data.getInfection() >= 50) {
                    if (!data.hasMutation("HORUS_TRAUMA")) {
                        data.addMutation("HORUS_TRAUMA");
                        serverPlayer.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("chat.liberthia.horus_trauma")
                                        .withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD),
                                false);
                    }
                    // Immediate debuffs — flashbacks of the Eye's presence
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.DARKNESS, 200, 0, true, false, true));
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.CONFUSION, 200, 0, true, false, true));
                    serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 1, true, false, true));
                }
            });
        }
    }

    /**
     * Atualiza dados de contaminação por chunk a cada 100 ticks.
     * Percorre chunks carregados perto de players e registra densidade no SavedData.
     */
    @SubscribeEvent
    public void onLevelTick(net.minecraftforge.event.TickEvent.LevelTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        // Gravity trap tick — every tick for smooth pulling
        br.com.murilo.liberthia.item.GravityTrapItem.tickTraps(serverLevel);
        br.com.murilo.liberthia.item.GravityAnchorItem.tickGrounded(serverLevel);
        br.com.murilo.liberthia.item.FreezeStaffItem.tickFrozen(serverLevel);

        if (serverLevel.getServer().getTickCount() % 100 != 0) return;

        br.com.murilo.liberthia.data.ChunkInfectionData chunkData =
                br.com.murilo.liberthia.data.ChunkInfectionData.get(serverLevel);

        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(player.blockPosition());
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    net.minecraft.world.level.ChunkPos target = new net.minecraft.world.level.ChunkPos(cp.x + dx, cp.z + dz);
                    if (!serverLevel.hasChunk(target.x, target.z)) continue;
                    float density = InfectionLogic.getChunkInfectionDensityDirect(serverLevel, target);
                    chunkData.setContamination(target, (int) (density * 100));
                }
            }
        }
    }
}
