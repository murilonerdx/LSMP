package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.EndPossessionC2SPacket;
import br.com.murilo.liberthia.network.packet.PossessionAttackC2SPacket;
import br.com.murilo.liberthia.network.packet.PossessionMoveC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Estado client-side da possessão. Tem dois lados:
 *
 * <ul>
 *   <li><b>Possessor</b> ({@link #activeAsPossessor}): redireciona câmera para
 *       a entidade possuída, bloqueia inputs do próprio body, encaminha WASD
 *       como packets {@code PossessionMoveC2SPacket} a cada tick.</li>
 *   <li><b>Possessed</b> ({@link #locked}): se este client recebeu
 *       LockedInputS2CPacket(true), o {@link MovementInputUpdateEvent} zera
 *       todos os inputs.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class PossessionClient {

    /** True se este client é o possessor de alguém. */
    private static volatile boolean activeAsPossessor = false;
    /** Entity ID do alvo enquanto possessor. -1 se inativo. */
    private static volatile int targetEntityId = -1;

    /** True se este client é alvo (locked). Setado por LockedInputS2CPacket. */
    private static volatile boolean locked = false;

    /** Último estado do shift, pra detectar "shift toggled on" (= cancel). */
    private static boolean lastShiftDown = false;

    /** r30: keybind pra habilidade do mob possuído (default: G). */
    public static net.minecraft.client.KeyMapping abilityKey;
    /** Detecta press único da ability key. */
    private static boolean lastAbilityDown = false;

    /** v0.1.22 r9: ticks consecutivos sem target válido. Auto-end após 60. */
    private static int orphanTicks = 0;

    /**
     * v0.1.22 r10: anchor de rotação enquanto locked. Capturado no START do
     * tick (antes do mouse turn rodar) e restaurado no END pra ANULAR
     * qualquer rotação local do mouse. O server manda yaw/pitch a cada
     * tick via PossessionMoveC2SPacket, então o anchor é refrescado
     * naturalmente — possuído fica "girando" só para onde o possessor
     * aponta.
     */
    private static float lockedYawAnchor = 0f;
    private static float lockedPitchAnchor = 0f;

    /**
     * v0.1.22 r21: estado do último input enviado ao server. Usado pra
     * THROTTLING: só envia novo packet se input MUDOU ou após HEARTBEAT_TICKS
     * desde o último envio. Antes mandávamos 20 packets/s, agora ~5 médios
     * (input parado) — redução de 4× no tráfego e no work do main thread
     * server. User reportou que servidor freezava com muitos possessors.
     */
    private static float lastSentForward = 0f;
    private static float lastSentStrafe = 0f;
    private static boolean lastSentJump = false;
    private static float lastSentYaw = 0f;
    private static float lastSentPitch = 0f;
    private static int ticksSinceLastSend = 0;
    /** A cada 5 ticks (250ms), envia heartbeat mesmo se input não mudou — server
     *  precisa pra física contínua (movimento continuado). */
    private static final int HEARTBEAT_TICKS = 5;
    /** Tolerância pra "rotação mudou" — micro-shake de mouse não conta. */
    private static final float YAW_THRESHOLD = 0.5f;
    private static final float PITCH_THRESHOLD = 0.5f;

    private PossessionClient() {}

    // -----------------------------------------------------------------------
    // POSSESSOR side
    // -----------------------------------------------------------------------

    /** Aplica setCameraEntity no alvo + liga flag de possession. */
    public static void startAsPossessor(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity target = mc.level.getEntity(entityId);
        targetEntityId = entityId;
        activeAsPossessor = true;
        lastShiftDown = false;
        // r21: reset throttle state pra primeiro packet sair imediatamente
        ticksSinceLastSend = HEARTBEAT_TICKS;
        lastSentForward = lastSentStrafe = lastSentYaw = lastSentPitch = 0f;
        lastSentJump = false;
        if (target != null) {
            mc.setCameraEntity(target);
        }
        // Se target == null, ainda assim travamos input local; tick re-tenta.
    }

    /** Restaura câmera + libera input do possessor. */
    public static void endAsPossessor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        activeAsPossessor = false;
        targetEntityId = -1;
    }

    public static boolean isActiveAsPossessor() {
        return activeAsPossessor;
    }

    // -----------------------------------------------------------------------
    // POSSESSED side
    // -----------------------------------------------------------------------

    public static void setLocked(boolean v) {
        locked = v;
    }

    public static boolean isLocked() {
        return locked;
    }

    // -----------------------------------------------------------------------
    // INPUT HOOKS
    // -----------------------------------------------------------------------

    /**
     * Zera inputs do POSSESSOR (corpo não-mexível) e do POSSESSED (preso).
     * No possessor, capturamos os valores ANTES de zerar e mandamos como
     * packet pra mover o alvo.
     */
    /**
     * v0.1.43: agora só ZERA inputs locais. Captura/envio de inputs movido
     * pra {@link #onClientTick} (que dispara a cada tick, não só quando o
     * input muda).
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (locked) {
            // Lado do possuído: zera tudo pro client target não tentar mover.
            zero(event.getInput());
            return;
        }
        if (!activeAsPossessor) return;
        // Lado do possessor: zera input do PRÓPRIO body pra ele não se mexer.
        // (O movimento do target vai via onClientTick.)
        zero(event.getInput());
    }

    private static void zero(Input input) {
        input.forwardImpulse = 0f;
        input.leftImpulse = 0f;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    /**
     * v0.1.49: input do mouse durante possessão — agora cobre 3 ações:
     * <ul>
     *   <li><b>LMB (left, button=0) press:</b>
     *     <ul>
     *       <li>EntityHitResult → {@link PossessionAttackC2SPacket} (attack mob).</li>
     *       <li>BlockHitResult → {@link br.com.murilo.liberthia.network.packet.PossessionBreakBlockC2SPacket}
     *           (quebra o bloco — instant break, hardness ≤ 5).</li>
     *     </ul>
     *   </li>
     *   <li><b>RMB (right, button=1) press:</b>
     *     {@link br.com.murilo.liberthia.network.packet.PossessionUseItemC2SPacket} —
     *     usa o item da mainhand do possessor (food, pílula, projétil, etc.).</li>
     * </ul>
     *
     * <p>Cancela o input vanilla pra impedir que o player invisível bata/use
     * no próprio corpo (que está parado em outro lugar do mundo).
     */
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        // v0.1.22 r10: se este client é POSSUÍDO (locked), CANCELA TODOS os
        // clicks. Antes só locked movement; agora trava attack/use/drop também.
        if (locked) {
            event.setCanceled(true);
            return;
        }
        if (!activeAsPossessor) return;
        if (event.getAction() != 1) return; // GLFW_PRESS
        Minecraft mc = Minecraft.getInstance();
        int btn = event.getButton();

        if (btn == 0) {
            // LEFT click — ataque ou quebra bloco
            if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr) {
                ModNetwork.CHANNEL.sendToServer(
                        new PossessionAttackC2SPacket(ehr.getEntity().getId()));
                event.setCanceled(true);
            } else if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr
                    && bhr.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                ModNetwork.CHANNEL.sendToServer(
                        new br.com.murilo.liberthia.network.packet.PossessionBreakBlockC2SPacket(bhr.getBlockPos()));
                event.setCanceled(true);
            }
        } else if (btn == 1) {
            // RIGHT click — usa item mainhand do possessor
            ModNetwork.CHANNEL.sendToServer(
                    new br.com.murilo.liberthia.network.packet.PossessionUseItemC2SPacket());
            event.setCanceled(true);
        }
    }

    /**
     * v0.1.22 r10: bloqueia keys do possuído (drop=Q, swap-hand=F, hotbar
     * 1-9, etc). User reportou que o possuído ainda conseguia interagir
     * mesmo sob posse. Cancela tudo no client lockado.
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!locked) return;
        // Não tem como "cancelar" InputEvent.Key (não é Cancelable). Mas
        // como zeramos os inputs via MovementInputUpdateEvent e bloqueamos
        // cliques via MouseButton, o único vector restante seriam hotbar
        // swaps e drop — esses não passam por Input padrão, vão direto via
        // mc.options.keyDrop.consumeClick(). Solução: consumir no início
        // do tick (ver onClientTickLockedStart abaixo).
    }

    /**
     * v0.1.22 r10: lado POSSUÍDO — captura rotação no START do tick (antes
     * do mouse handler aplicar delta) e restaura no END. Anula qualquer
     * mouse turn local. O server manda yaw/pitch a cada tick via
     * PossessionMoveC2SPacket, então o anchor é refrescado naturalmente.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLockedTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!locked) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        lockedYawAnchor = mc.player.getYRot();
        lockedPitchAnchor = mc.player.getXRot();
        // Consome keys que não passam por Input: drop, swap, hotbar.
        // consumeClick() esvazia o buffer interno do KeyMapping.
        while (mc.options.keyDrop.consumeClick()) { /* discard */ }
        while (mc.options.keySwapOffhand.consumeClick()) { /* discard */ }
        while (mc.options.keyAttack.consumeClick()) { /* discard */ }
        while (mc.options.keyUse.consumeClick()) { /* discard */ }
        while (mc.options.keyPickItem.consumeClick()) { /* discard */ }
        for (var hot : mc.options.keyHotbarSlots) {
            while (hot.consumeClick()) { /* discard */ }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLockedTickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!locked) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // Restaura rotação anchor — anula mouse delta deste tick.
        mc.player.setYRot(lockedYawAnchor);
        mc.player.setXRot(lockedPitchAnchor);
        mc.player.yRotO = lockedYawAnchor;
        mc.player.xRotO = lockedPitchAnchor;
        mc.player.yHeadRot = lockedYawAnchor;
        mc.player.yHeadRotO = lockedYawAnchor;
        mc.player.yBodyRot = lockedYawAnchor;
        mc.player.yBodyRotO = lockedYawAnchor;
    }

    /**
     * v0.1.43: tick periódico do client durante possession.
     *
     * <p>Faz DUAS coisas:
     * <ol>
     *   <li>Re-aplica setCameraEntity se a entidade saiu/voltou na área de
     *       view distance.</li>
     *   <li>LÊ RAW INPUT KEYBOARD (vanilla key mappings) e MANDA packet pro
     *       server a cada tick. Antes só mandava em {@code MovementInputUpdateEvent},
     *       que é fired SÓ quando o input MUDA — então segurar W constantemente
     *       só gerava 1 packet, e o target só ganhava 1 setDeltaMovement
     *       (que vanilla physics zera no próximo tick).</li>
     * </ol>
     *
     * <p>Resultado: agora é UM PACKET POR TICK enquanto possession ativo →
     * movimento contínuo e responsivo.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!activeAsPossessor) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // (1) Re-aplica câmera se perdeu o alvo. Se o target sumiu de vez
        // (60 ticks consecutivos sem achar), auto-end pra liberar o state.
        // User reportou que Curios não abria após possession — provavelmente
        // activeAsPossessor ficava preso com target dead/desconectado.
        Entity cam = mc.getCameraEntity();
        Entity target = mc.level.getEntity(targetEntityId);
        if (target == null || target.isRemoved()) {
            orphanTicks++;
            if (orphanTicks >= 60) {
                br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                        "[Possession Client] target {} órfão por 60 ticks — auto-end",
                        targetEntityId);
                endAsPossessor();
                orphanTicks = 0;
                return;
            }
        } else {
            orphanTicks = 0;
            if (cam == null || cam == mc.player || cam.isRemoved() || cam.getId() != targetEntityId) {
                mc.setCameraEntity(target);
            }
        }

        // v0.1.46: se uma screen estiver aberta (inventário, chest, menu),
        // SKIP o envio de packet. User pediu pra poder usar inventário
        // enquanto possui. Vanilla já trata abertura via tecla E. Aqui só
        // garantimos que o movement não interfere.
        if (mc.screen != null) {
            // Reseta lastShiftDown pra não disparar cancel acidental ao fechar
            // a screen com shift segurado.
            lastShiftDown = false;
            return;
        }

        // (2) Lê raw input das key mappings vanilla e manda packet a cada tick
        float forward = 0f;
        float strafe = 0f;
        if (mc.options.keyUp.isDown())    forward += 1f;
        if (mc.options.keyDown.isDown())  forward -= 1f;
        if (mc.options.keyLeft.isDown())  strafe  += 1f;
        if (mc.options.keyRight.isDown()) strafe  -= 1f;
        boolean jump = mc.options.keyJump.isDown();
        boolean sneak = mc.options.keyShift.isDown();

        // Detecta shift "pressionou agora" → cancel (sem repetir)
        if (sneak && !lastShiftDown) {
            ModNetwork.CHANNEL.sendToServer(new EndPossessionC2SPacket());
        }
        lastShiftDown = sneak;

        // v0.1.22 r9: REMOVIDO o consumeClick(keyInventory). Esse método
        // CONSUMIA o evento da tecla E antes do Curios receber, e o estado
        // ainda persistia parcial após endAsPossessor — quebrando o keybind
        // do Curios completamente. Agora deixamos vanilla abrir o inventário
        // naturalmente; setCameraEntity(target) NÃO bloqueia vanilla
        // InventoryScreen.open(), testado funciona.

        // Yaw/pitch do LocalPlayer — mouse vanilla SEMPRE rotaciona ele,
        // mesmo com setCameraEntity ativo.
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();

        // v0.1.22 r21: THROTTLING — só envia packet se input mudou ou se
        // passou HEARTBEAT_TICKS desde o último envio. Antes era 1 packet/tick
        // (20×/s) — agora médio ~4-8 packets/s quando input estável.
        //
        // Lógica:
        //   - Input WASD/jump muda → envia agora (responsivo)
        //   - Yaw/pitch mudou > threshold → envia (segue rotação)
        //   - Nada mudou MAS 5 ticks (250ms) sem packet → heartbeat
        //     (mantém física contínua server-side se segurando W parado)
        ticksSinceLastSend++;
        boolean inputChanged = forward != lastSentForward
                || strafe != lastSentStrafe
                || jump != lastSentJump;
        boolean rotationChanged = Math.abs(yaw - lastSentYaw) > YAW_THRESHOLD
                || Math.abs(pitch - lastSentPitch) > PITCH_THRESHOLD;
        boolean heartbeat = ticksSinceLastSend >= HEARTBEAT_TICKS;

        if (inputChanged || rotationChanged || heartbeat) {
            ModNetwork.CHANNEL.sendToServer(new PossessionMoveC2SPacket(
                    forward, strafe, jump, false, yaw, pitch));
            lastSentForward = forward;
            lastSentStrafe = strafe;
            lastSentJump = jump;
            lastSentYaw = yaw;
            lastSentPitch = pitch;
            ticksSinceLastSend = 0;
        }

        // r30: detecta press único da ability key (G) → manda packet
        if (abilityKey != null) {
            boolean abilityDown = abilityKey.isDown();
            if (abilityDown && !lastAbilityDown) {
                ModNetwork.CHANNEL.sendToServer(
                        new br.com.murilo.liberthia.network.packet.PossessionAbilityC2SPacket());
            }
            lastAbilityDown = abilityDown;
        }
    }

    /** Logout: limpa flags pra não ficar travado em respawn / new server. */
    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        if (activeAsPossessor) endAsPossessor();
        locked = false;
        orphanTicks = 0;
    }

    /**
     * v0.1.22 r13: também limpa flags ao LOGAR. Cobre caso onde client
     * disconectou em estado inconsistente e voltou — sem isso, locked ou
     * activeAsPossessor poderiam ficar presos entre sessões. User reportou
     * que não conseguia bater em players pós-posse — provável causa eram
     * flags presas no client.
     */
    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        activeAsPossessor = false;
        targetEntityId = -1;
        locked = false;
        orphanTicks = 0;
        lastShiftDown = false;
    }
}
