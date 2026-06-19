package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.EndPossessionS2CPacket;
import br.com.murilo.liberthia.network.packet.LockedInputS2CPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado server-side da Feature "Possession" (controle remoto via
 * {@code possession_amulet}).
 *
 * <h2>Mecânica</h2>
 * <p>Um possessor (player) toca outra entidade {@link LivingEntity} com o
 * amulet. Server:
 * <ul>
 *   <li>Registra {@code possessor → possessed} aqui</li>
 *   <li>Manda {@code StartPossessionS2CPacket(targetEntityId)} pro possessor →
 *       cliente redireciona câmera para o target</li>
 *   <li>Se o target for player: manda {@code LockedInputS2CPacket(true)} pro
 *       cliente target → ele zera inputs a cada tick</li>
 *   <li>Se o target for {@link Mob}: chama {@code mob.setNoAi(true)} (a IA
 *       já não comanda; movimentação injetada por
 *       {@code PossessionMoveC2SPacket})</li>
 * </ul>
 *
 * <p>Para cancelar:
 * <ul>
 *   <li>Possessor aperta shift → cliente manda {@code EndPossessionC2SPacket}</li>
 *   <li>Target morre (LivingDeathEvent)</li>
 *   <li>Possessor desconecta (PlayerLoggedOutEvent)</li>
 *   <li>Possessor morre (LivingDeathEvent — bloco também cobre players)</li>
 * </ul>
 *
 * <h2>Limitações</h2>
 * <p>Possessão de mob é simplificada: movimento via
 * {@code mob.setDeltaMovement} injetado pelo possessor. Ataque via
 * {@code possessed.doHurtTarget(victim)}. Algumas IAs específicas
 * (pathing complex) podem brigar com o NoAI flag — não tratamos.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class PossessionManager {

    /** Mapa possessor (player UUID) → entity UUID do alvo (player ou mob). */
    private static final Map<UUID, UUID> ACTIVE = new ConcurrentHashMap<>();
    /** Reverso para lookup rápido: possessed (entity UUID) → possessor (player UUID). */
    private static final Map<UUID, UUID> REVERSE = new ConcurrentHashMap<>();

    /**
     * v0.1.22 r21: Sessões — objeto único por par possessor/possuído com cache
     * de entidade + buffer de input + rate-limit de recovery. Substitui
     * ATTACK_BYPASS set + findEntity calls + send-loop de EndPossession.
     * Chaveado por POSSESSOR UUID (1 sessão por possessor).
     */
    private static final Map<UUID, PossessionSession> SESSIONS = new ConcurrentHashMap<>();

    /** Retorna a sessão pra esse possessor, ou null se não tem posse ativa. */
    public static PossessionSession getSession(UUID possessorUuid) {
        return SESSIONS.get(possessorUuid);
    }

    /** Retorna sessão buscando pelo TARGET UUID. */
    public static PossessionSession getSessionByTarget(UUID targetUuid) {
        UUID possessor = REVERSE.get(targetUuid);
        return possessor == null ? null : SESSIONS.get(possessor);
    }

    /** Adiciona/remove UUID do bypass de cancelamento de ataque. */
    public static void setAttackBypass(UUID uuid, boolean allow) {
        // r21: bypass agora vive na PossessionSession. Mantemos API por compat.
        PossessionSession s = getSessionByTarget(uuid);
        if (s != null) s.attackInProgress = allow;
    }

    public static boolean hasAttackBypass(UUID uuid) {
        PossessionSession s = getSessionByTarget(uuid);
        return s != null && s.attackInProgress;
    }

    /**
     * v0.1.22 r21: tenta enviar EndPossession pro client, respeitando
     * rate-limit (1 por 5s). Evita feedback loop quando client está
     * dessincronizado — antes packet handler reenviava EndPossession a CADA
     * packet, gerando flood de 20 packets/sec e ainda piorando o lag.
     */
    public static void requestClientRecoverySync(ServerPlayer sender) {
        if (sender == null) return;
        long now = sender.level().getGameTime();
        PossessionSession s = getSession(sender.getUUID());
        if (s != null) {
            if (!s.tryRecoverySend(now)) return;
        }
        // Sem sessão = não tem rate-limit (raríssimo); permite só uma vez aqui
        ModNetwork.sendToPlayer(sender, new EndPossessionS2CPacket());
        ModNetwork.sendToPlayer(sender, new LockedInputS2CPacket(false));
    }

    private PossessionManager() {}

    /** Inicia possessão. Retorna {@code false} se possessor já está possuindo ou target já é alvo. */
    public static boolean start(ServerPlayer possessor, LivingEntity target) {
        if (ACTIVE.containsKey(possessor.getUUID())) return false;
        if (REVERSE.containsKey(target.getUUID())) return false;
        if (possessor.getUUID().equals(target.getUUID())) return false;

        // v0.1.22 r22: Mind Ward bloqueia posse. Defesa server-side em
        // profundidade (UX já cobre no PossessionAmuletItem, esse aqui é
        // garantia contra qualquer outra route que chamar start()).
        if (br.com.murilo.liberthia.item.MindWardItem.hasWard(target)) {
            return false;
        }

        ACTIVE.put(possessor.getUUID(), target.getUUID());
        REVERSE.put(target.getUUID(), possessor.getUUID());

        // r21: cria sessão com cache de entidade pré-aquecido
        PossessionSession session = new PossessionSession(possessor.getUUID(), target.getUUID());
        // Pré-aquece cache chamando resolveTarget uma vez (entidade já tá em memória)
        session.resolveTarget(possessor.server);
        SESSIONS.put(possessor.getUUID(), session);

        if (target instanceof Mob mob) {
            // Suspende IA do mob — vamos comandar via packets do possessor.
            mob.setNoAi(true);
        }
        if (target instanceof ServerPlayer sp) {
            // Trava inputs do target (cliente vai zerar movement a cada tick).
            ModNetwork.sendToPlayer(sp, new LockedInputS2CPacket(true));
            // Fecha qualquer GUI aberta defensivamente.
            sp.connection.send(new ClientboundContainerClosePacket(sp.containerMenu.containerId));
        }
        return true;
    }

    /** Termina possessão. Idempotente. */
    public static void end(MinecraftServer server, UUID possessorUuid) {
        UUID targetUuid = ACTIVE.remove(possessorUuid);
        if (targetUuid == null) return;
        REVERSE.remove(targetUuid);
        // r21: limpa sessão (libera cache de entidade + GC do objeto)
        SESSIONS.remove(possessorUuid);

        // Restaura o alvo.
        if (server != null) {
            ServerPlayer possessor = server.getPlayerList().getPlayer(possessorUuid);
            if (possessor != null) {
                ModNetwork.sendToPlayer(possessor, new EndPossessionS2CPacket());
                sanityResetPlayer(possessor);
            }
            // Procura entity em todos os levels (target pode ser mob OU player).
            Entity targetEntity = findEntity(server, targetUuid);
            if (targetEntity instanceof Mob mob) {
                mob.setNoAi(false);
                mob.invulnerableTime = 0;
                mob.hurtTime = 0;
            }
            if (targetEntity instanceof ServerPlayer sp) {
                ModNetwork.sendToPlayer(sp, new LockedInputS2CPacket(false));
                sanityResetPlayer(sp);
            } else if (targetEntity instanceof LivingEntity le) {
                le.invulnerableTime = 0;
                le.hurtTime = 0;
            }
        }
    }

    /**
     * v0.1.22 r11: reset defensivo do estado do player após end. User pediu
     * que "possuido e possessor voltem normais — tomar dano, sofrer dano,
     * ataque, etc, e os artefatos voltem a funcionar normalmente".
     *
     * <p>Limpa:
     * <ul>
     *   <li>invulnerableTime / hurtTime — pra dano funcionar imediatamente</li>
     *   <li>force-sync de pos/rot via connection.teleport — corrige raycast
     *       client-server divergente após mouse delta acumulado</li>
     *   <li>closeContainer — fecha qualquer GUI que ficou stuck</li>
     *   <li>resyncia inventory/abilities — Curios slots, mayfly, etc</li>
     *   <li>limpa attack bypass se sobrou</li>
     * </ul>
     */
    private static void sanityResetPlayer(ServerPlayer sp) {
        // Damage state
        sp.invulnerableTime = 0;
        sp.hurtTime = 0;
        sp.hurtDuration = 0;

        // Force-sync pos/rot — corrige client-server divergence pós-mouse
        // delta acumulado.
        sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(),
                sp.getYRot(), sp.getXRot());

        // Fecha qualquer GUI presa (chest, furnace, Curios slot, etc).
        if (sp.containerMenu != sp.inventoryMenu) {
            sp.closeContainer();
        }

        // Reforça abilities sync — se Botas Mercuriais ou algum Curios
        // setou mayfly/flying durante a posse, o client pode ter ficado
        // dessincronizado.
        sp.onUpdateAbilities();

        // Resync de inventário pro client (incluindo Curios slots).
        sp.inventoryMenu.broadcastChanges();

        // r21: bypass de attack agora vive na PossessionSession e some junto
        // quando SESSIONS.remove() é chamado no end(). Sem limpeza explícita aqui.
    }

    /** Retorna o UUID do entity possuído por este possessor, ou null. */
    public static UUID getPossessed(UUID possessorUuid) {
        return ACTIVE.get(possessorUuid);
    }

    /** True se o entity está sendo possuído por alguém. */
    public static boolean isPossessed(UUID entityUuid) {
        return REVERSE.containsKey(entityUuid);
    }

    /** Retorna possessor UUID se este entity está sob possessão. */
    public static UUID getPossessor(UUID entityUuid) {
        return REVERSE.get(entityUuid);
    }

    /** Helper: procura entity por UUID nos levels do server. */
    public static Entity findEntity(MinecraftServer server, UUID uuid) {
        if (server == null) return null;
        // Tenta player primeiro (caminho mais rápido).
        ServerPlayer asPlayer = server.getPlayerList().getPlayer(uuid);
        if (asPlayer != null) return asPlayer;
        for (var lvl : server.getAllLevels()) {
            Entity e = lvl.getEntity(uuid);
            if (e != null) return e;
        }
        return null;
    }

    /**
     * Tick: limpa possessões cujo target morreu/sumiu ou possessor desconectou.
     * Também aplica side-effects de "lock" no target (containerClose periódico).
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ACTIVE.isEmpty()) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;

        Iterator<Map.Entry<UUID, UUID>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> e = it.next();
            UUID possessorUuid = e.getKey();
            UUID targetUuid = e.getValue();

            ServerPlayer possessor = server.getPlayerList().getPlayer(possessorUuid);
            if (possessor == null) {
                // Possessor offline — limpa estado e libera o target.
                it.remove();
                REVERSE.remove(targetUuid);
                Entity t = findEntity(server, targetUuid);
                if (t instanceof Mob mob) mob.setNoAi(false);
                if (t instanceof ServerPlayer sp) {
                    ModNetwork.sendToPlayer(sp, new LockedInputS2CPacket(false));
                }
                continue;
            }

            Entity target = findEntity(server, targetUuid);
            if (target == null || target.isRemoved() || !target.isAlive()) {
                it.remove();
                REVERSE.remove(targetUuid);
                ModNetwork.sendToPlayer(possessor, new EndPossessionS2CPacket());
                continue;
            }
            // Garante que mob continua com IA desligada (mob.tick pode re-habilitar).
            if (target instanceof Mob mob && !mob.isNoAi()) {
                mob.setNoAi(true);
            }
            // Player possuído: a cada 20 ticks força fechamento de GUI extra
            // (mesmo se cliente travou inputs, server-side defesa em profundidade).
            if (target instanceof ServerPlayer sp && server.getTickCount() % 20 == 0) {
                if (sp.containerMenu != sp.inventoryMenu) {
                    sp.connection.send(new ClientboundContainerClosePacket(sp.containerMenu.containerId));
                }
            }
        }
    }

    /** Possessor logout: limpa. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        // Logout do possessor.
        if (ACTIVE.containsKey(sp.getUUID())) {
            end(sp.server, sp.getUUID());
        }
        // Logout do target (player possuído).
        if (REVERSE.containsKey(sp.getUUID())) {
            UUID possessor = REVERSE.get(sp.getUUID());
            if (possessor != null) {
                end(sp.server, possessor);
            }
        }
    }

    /** Morte do possessor OU do alvo: cancela. */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        UUID id = event.getEntity().getUUID();
        // Se é o possessor → end direto.
        if (ACTIVE.containsKey(id)) {
            MinecraftServer server = event.getEntity().getServer();
            end(server, id);
            return;
        }
        // Se é o target → procura quem o possuía e chama end.
        UUID possessor = REVERSE.get(id);
        if (possessor != null) {
            MinecraftServer server = event.getEntity().getServer();
            end(server, possessor);
        }
    }

    /**
     * v0.1.46: mobs hostis ignoram o possessor enquanto ele tá possuindo
     * alguma entidade. Vanilla mob AI faz pathfinding em direção ao player —
     * mas o "verdadeiro" player tá invisível em algum canto enquanto sua
     * câmera tá no mob possuído. User relatou: "quando sou zumbi os monstros
     * sabem que eu sou um player". Solução: intercepta tentativa de setTarget
     * e cancela se o target é um possessor ativo.
     *
     * <p>Não cobre absolutamente tudo (alguns AI goals podem chamar attack
     * direto sem passar por setTarget), por isso também temos o
     * {@link #onLivingAttack} como backup.
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewTarget();
        if (newTarget == null) return;
        // Se o newTarget é um player que está atualmente POSSUINDO alguém,
        // limpa o target. O mob ainda pode atacar a entity possuída se ela
        // for hostil (vanilla behavior).
        if (ACTIVE.containsKey(newTarget.getUUID())) {
            event.setNewTarget(null);
        }
    }

    /**
     * Backup defensivo: cancela dano causado a um possessor por mob hostil.
     * Cobre o caso onde AI já tinha target setado antes do possessor começar
     * a possuir, ou quando AI usa attack direto sem chamar setTarget.
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        // Só cancela ataques DE MOB HOSTIL contra possessor — dano de outras
        // fontes (queda, fogo, comando, /kill, PvP de outro player) continua
        // valendo. PvE attack pelo Mob é o que queremos bloquear.
        if (!(event.getSource().getEntity() instanceof Mob attacker)) return;
        if (ACTIVE.containsKey(victim.getUUID())) {
            // Também limpa target do attacker pra ele não tentar de novo no
            // próximo tick (a IA pode persistir target stale).
            attacker.setTarget(null);
            event.setCanceled(true);
        }
    }

    /** Bloqueia o player possuído de abrir GUIs (chest, furnace, inv). */
    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (event.getEntity() == null) return;
        if (isPossessed(event.getEntity().getUUID())) {
            // Não há setCanceled neste event (não é Cancelable). Força close imediato.
            if (event.getEntity() instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundContainerClosePacket(
                        sp.containerMenu.containerId));
            }
        }
    }

    // -----------------------------------------------------------------------
    // v0.1.22 r10: LOCK do possuído server-side. r13: REMOVIDO o
    // onPossessedAttack porque podia estar cancelando ataques legítimos
    // pós-end. Defesa contra ataque do possuído já é coberta por:
    //   - Client: PossessionClient cancela TODO MouseButton enquanto locked
    //   - Server: para mobs, setNoAi(true) impede attack AI
    //   - Server: para players, container/interact/drop blocks abaixo
    // Mantemos os blocks de drop/interact/container pra defesa em profundidade.
    // -----------------------------------------------------------------------

    /** Possuído não pode DROPAR items (Q ou inventory drag). */
    @SubscribeEvent
    public static void onPossessedToss(ItemTossEvent event) {
        if (event.getPlayer() == null) return;
        if (isPossessed(event.getPlayer().getUUID())) {
            event.setCanceled(true);
            // Devolve o item ao inventário pra não sumir.
            if (event.getEntity() != null && !event.getEntity().getItem().isEmpty()) {
                event.getPlayer().getInventory().add(event.getEntity().getItem());
                event.getEntity().discard();
            }
        }
    }

    /** Possuído não pode interagir com BLOCO (right-click). */
    @SubscribeEvent
    public static void onPossessedRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() == null) return;
        if (isPossessed(event.getEntity().getUUID())) event.setCanceled(true);
    }

    /** Possuído não pode quebrar BLOCO (left-click). */
    @SubscribeEvent
    public static void onPossessedLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() == null) return;
        if (isPossessed(event.getEntity().getUUID())) event.setCanceled(true);
    }

    /** Possuído não pode USAR item (right-click no ar). */
    @SubscribeEvent
    public static void onPossessedRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() == null) return;
        if (isPossessed(event.getEntity().getUUID())) event.setCanceled(true);
    }

    /** Possuído não pode INTERAGIR com entity (trade, mount, etc). */
    @SubscribeEvent
    public static void onPossessedEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() == null) return;
        if (isPossessed(event.getEntity().getUUID())) event.setCanceled(true);
    }
}
