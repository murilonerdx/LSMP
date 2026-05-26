package br.com.murilo.liberthia.matter;

import br.com.murilo.liberthia.event.VisionSwapManager;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.StartVisionSwapS2CPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Comandos {@code /liberthia matter ...} pra debug e testes.
 *
 * <ul>
 *   <li>{@code /liberthia matter set <player> dark|white|yellow <value>}</li>
 *   <li>{@code /liberthia matter add <player> dark|white|yellow <value>}</li>
 *   <li>{@code /liberthia matter get <player>}</li>
 *   <li>{@code /liberthia matter clear <player>}</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = br.com.murilo.liberthia.LiberthiaMod.MODID)
public final class MatterCommand {

    private MatterCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("liberthia")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("matter")
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("dark")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100))
                                                        .executes(c -> set(c, "dark"))))
                                        .then(Commands.literal("white")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100))
                                                        .executes(c -> set(c, "white"))))
                                        .then(Commands.literal("yellow")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(0, 100))
                                                        .executes(c -> set(c, "yellow"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.literal("dark")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(-100, 100))
                                                        .executes(c -> add(c, "dark"))))
                                        .then(Commands.literal("white")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(-100, 100))
                                                        .executes(c -> add(c, "white"))))
                                        .then(Commands.literal("yellow")
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(-100, 100))
                                                        .executes(c -> add(c, "yellow"))))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(MatterCommand::get)))
                        .then(Commands.literal("clear")
                                .executes(MatterCommand::clearSelf)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(MatterCommand::clear)))
                        // /liberthia matter visionswap — equivale a usar Vision Swap Lens
                        // (escolhe random um player com WM ≥ 50 e ativa swap por 8s).
                        .then(Commands.literal("visionswap")
                                .executes(MatterCommand::visionSwap)))
                // v0.1.41: /liberthia boss on|off — vira boss com bossbar global
                .then(Commands.literal("boss")
                        .then(Commands.literal("on").executes(MatterCommand::bossOn))
                        .then(Commands.literal("off").executes(MatterCommand::bossOff)))
                // v0.1.41: /liberthia immortal on|off — imortal em survival
                .then(Commands.literal("immortal")
                        .then(Commands.literal("on").executes(MatterCommand::immortalOn))
                        .then(Commands.literal("off").executes(MatterCommand::immortalOff)))
                // v0.1.22 r14: /liberthia reset [player] — desbuga qualquer
                // estado preso (posse, dano travado, flags client/server).
                // Self por default. Op-only.
                .then(Commands.literal("reset")
                        .executes(MatterCommand::resetSelf)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(MatterCommand::resetOther)))
                // v0.1.22 r19: /liberthia unseal [player] — libera player
                // capturado por Netherite Seal. User reportou: "não consigo
                // sair pq me selaram e me fudeu". Comando dedicado pro fix.
                .then(Commands.literal("unseal")
                        .executes(MatterCommand::unsealSelf)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(MatterCommand::unsealOther)))
                // v0.1.22 r22: /liberthia poss inv — abre inv do possuído pro
                // possessor mexer (mover items, equipar armor, dropar). NÃO é
                // op-only (qualquer player com posse ativa pode usar). Free
                // ability do Possession Amulet.
                .then(Commands.literal("poss")
                        .requires(s -> true) // sem permission check — qualquer player
                        .then(Commands.literal("inv")
                                .executes(MatterCommand::openPossessedInv)));
        dispatcher.register(root);
    }

    /**
     * v0.1.22 r22: abre o inventário do PLAYER POSSUÍDO pro possessor mexer.
     * Funciona via ChestMenu vanilla apontando pra um PossessedInventoryProxy
     * que mapeia slots 0-35 (inv) + 36-39 (armor) + 40 (offhand).
     *
     * <p>Falha se:
     * <ul>
     *   <li>Executor não tem posse ativa</li>
     *   <li>Target não é player (mob possuído não tem inv editável)</li>
     *   <li>Target sumiu/morreu</li>
     * </ul>
     */
    private static int openPossessedInv(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer possessor = ctx.getSource().getPlayerOrException();
        br.com.murilo.liberthia.event.PossessionSession session =
                br.com.murilo.liberthia.event.PossessionManager.getSession(possessor.getUUID());
        if (session == null) {
            possessor.displayClientMessage(Component.literal(
                    "§cVocê precisa estar possuindo alguém pra usar isso."), false);
            return 0;
        }
        var entity = session.resolveTarget(possessor.server);
        if (!(entity instanceof ServerPlayer targetPlayer)) {
            possessor.displayClientMessage(Component.literal(
                    "§cSó funciona em players possuídos (mobs não têm inventário editável)."), false);
            return 0;
        }
        // Abre ChestMenu 5-row (45 slots = inv 36 + armor 4 + offhand 1 + 4 filler)
        br.com.murilo.liberthia.event.PossessedInventoryProxy proxy =
                new br.com.murilo.liberthia.event.PossessedInventoryProxy(targetPlayer);
        possessor.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> new net.minecraft.world.inventory.ChestMenu(
                        net.minecraft.world.inventory.MenuType.GENERIC_9x5,
                        id, inv, proxy, 5),
                Component.literal("§5Inventário de §d" + targetPlayer.getName().getString())
        ));
        possessor.displayClientMessage(Component.literal(
                "§7Inv aberto. Primeira linha = hotbar | última = §6helmet§r§7/§6chest§r§7/"
                        + "§6legs§r§7/§6boots§r§7/§6offhand§r§7."), false);
        return 1;
    }

    /**
     * v0.1.22 r19: libera player capturado pelo Netherite Seal.
     * Remove invisibility/invulnerability + Blindness/Slowness/Weakness 255
     * + estado do CapturedPlayerManager.
     */
    private static int unsealSelf(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return doUnseal(ctx, ctx.getSource().getPlayerOrException());
    }

    private static int unsealOther(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return doUnseal(ctx, EntityArgument.getPlayer(ctx, "player"));
    }

    private static int doUnseal(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                                 ServerPlayer sp) {
        boolean wasCaptured = br.com.murilo.liberthia.capture.CapturedPlayerManager.isCaptured(sp);
        if (wasCaptured) {
            br.com.murilo.liberthia.capture.CapturedPlayerManager.release(sp);
        }
        // Defesa em profundidade: limpa flags mesmo se não estava em CAPTURED_PLAYERS
        sp.setInvisible(false);
        sp.setInvulnerable(false);
        sp.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        sp.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        sp.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
        // Força fechar a tela CapturedBySealScreen se aberta
        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerClosePacket(
                sp.containerMenu.containerId));

        sp.displayClientMessage(Component.literal(
                wasCaptured ? "§a✓ Selo rompido — você está livre"
                            : "§7Você não estava selado. Flags limpas defensivamente."),
                false);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Unseal aplicado em " + sp.getName().getString()).withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    /**
     * v0.1.22 r14: comando de emergência pra desbugar. User pediu:
     * "se continuar não recebendo dano de player um comando que reset
     * essas configurações manualmente". Limpa:
     *
     * <ul>
     *   <li>PossessionManager: termina qualquer posse ativa (como possessor
     *       ou como possuído)</li>
     *   <li>ATTACK_BYPASS, ACTIVE, REVERSE — força limpeza por UUID</li>
     *   <li>invulnerableTime, hurtTime, hurtDuration zerados</li>
     *   <li>Force-sync de pos/rot via connection.teleport</li>
     *   <li>Reset abilities (mayfly, flying)</li>
     *   <li>onUpdateAbilities — sync pro client</li>
     *   <li>Container fechado se aberto</li>
     *   <li>FreezeManager.unfreeze (caso esteja congelado)</li>
     *   <li>VisionSwapManager: termina swap ativo</li>
     *   <li>PlayerImmortalityHandler.disable (zera flag)</li>
     *   <li>Manda EndPossessionS2CPacket + LockedInputS2CPacket(false) pro
     *       client pra garantir reset visual</li>
     * </ul>
     */
    private static int resetSelf(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        return doReset(ctx, p);
    }

    private static int resetOther(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        return doReset(ctx, target);
    }

    private static int doReset(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                                ServerPlayer sp) {
        java.util.UUID uuid = sp.getUUID();
        var server = sp.server;

        // (1) Possessão — termina como possessor E como possuído
        if (br.com.murilo.liberthia.event.PossessionManager.getPossessed(uuid) != null) {
            br.com.murilo.liberthia.event.PossessionManager.end(server, uuid);
        }
        java.util.UUID possessor =
                br.com.murilo.liberthia.event.PossessionManager.getPossessor(uuid);
        if (possessor != null) {
            br.com.murilo.liberthia.event.PossessionManager.end(server, possessor);
        }
        // Limpa bypass
        br.com.murilo.liberthia.event.PossessionManager.setAttackBypass(uuid, false);

        // (2) Força client a sair de qualquer modo de posse (defesa contra
        // packets perdidos)
        br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                new br.com.murilo.liberthia.network.packet.EndPossessionS2CPacket());
        br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                new br.com.murilo.liberthia.network.packet.LockedInputS2CPacket(false));

        // (3) Damage state
        sp.invulnerableTime = 0;
        sp.hurtTime = 0;
        sp.hurtDuration = 0;
        sp.fallDistance = 0;
        sp.clearFire();

        // (4) Imortalidade — desativa
        br.com.murilo.liberthia.event.PlayerImmortalityHandler.disable(uuid);

        // (5) VisionSwap — termina se ativo
        if (br.com.murilo.liberthia.event.VisionSwapManager.isUserSwapping(uuid)) {
            br.com.murilo.liberthia.event.VisionSwapManager.end(sp);
        }

        // (5b) v0.1.22 r19: libera Netherite Seal capture se ativo
        if (br.com.murilo.liberthia.capture.CapturedPlayerManager.isCaptured(sp)) {
            br.com.murilo.liberthia.capture.CapturedPlayerManager.release(sp);
        }
        // Sempre limpa flags defensivamente (mesmo se não estava captured)
        sp.setInvisible(false);
        sp.setInvulnerable(false);
        sp.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        sp.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        sp.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);

        // (6) Force-sync pos/rot — corrige client-server divergence
        sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(),
                sp.getYRot(), sp.getXRot());

        // (7) Container open? Fecha
        if (sp.containerMenu != sp.inventoryMenu) {
            sp.closeContainer();
        }

        // (8) Abilities + inventory sync
        sp.onUpdateAbilities();
        sp.inventoryMenu.broadcastChanges();

        sp.displayClientMessage(Component.literal(
                "§a§l✓ Reset aplicado §r§a— posse, dano e flags client/server limpos"),
                false);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Reset aplicado em " + sp.getName().getString()).withStyle(
                        ChatFormatting.GREEN), true);
        return 1;
    }

    private static int bossOn(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        boolean started = br.com.murilo.liberthia.event.PlayerBossFightManager.start(p);
        if (started) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "☠ BOSS MODE ATIVADO — bossbar visível pra todos no servidor")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("Você já está em modo boss.")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int bossOff(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        boolean stopped = br.com.murilo.liberthia.event.PlayerBossFightManager.stop(p.server, p.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal(
                stopped ? "Boss mode desligado" : "Você não estava em modo boss")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int immortalOn(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        boolean changed = br.com.murilo.liberthia.event.PlayerImmortalityHandler.enable(p.getUUID());
        // v0.1.22: 100% silencioso — só o player vê. User pediu: "só eu posso
        // ver não apareça nada no chat dos outros". displayClientMessage
        // bypassa o sistema de broadcast pra admins/ops do sendSuccess.
        p.displayClientMessage(Component.literal(changed
                        ? "§b§l✦ IMORTAL ATIVADO §r§b— nada pode te matar"
                        : "§7Você já estava imortal."),
                false);
        return 1;
    }

    private static int immortalOff(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        boolean changed = br.com.murilo.liberthia.event.PlayerImmortalityHandler.disable(p.getUUID());
        p.displayClientMessage(Component.literal(changed
                        ? "§7Imortalidade desligada"
                        : "§7Você não estava imortal"),
                false);
        return 1;
    }

    /**
     * Equivale ao right-click no {@code vision_swap_lens}. Usado pra debug
     * sem ter que craftar o item. Sem cooldown — comando é admin-only.
     */
    private static int visionSwap(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer user = ctx.getSource().getPlayerOrException();
        MinecraftServer server = user.server;
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getUUID().equals(user.getUUID())) continue;
            if (p.level() != user.level()) continue;
            float wm = p.getCapability(MatterProfileProvider.CAP)
                    .map(MatterProfile::getWhite)
                    .orElse(0f);
            if (wm >= VisionSwapManager.MIN_WHITE_FOR_TARGET) candidates.add(p);
        }
        if (candidates.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Nenhum player com WM ≥ 50 encontrado").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        ServerPlayer target = candidates.get(user.getRandom().nextInt(candidates.size()));
        if (!VisionSwapManager.start(user, target)) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Vision swap já ativo").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        ModNetwork.sendToPlayer(user, new StartVisionSwapS2CPacket(target.getUUID()));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Vision swap iniciado (8s)").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int set(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String type)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        float v = FloatArgumentType.getFloat(ctx, "value");
        p.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            switch (type) {
                case "dark"   -> profile.setDark(v);
                case "white"  -> profile.setWhite(v);
                case "yellow" -> profile.setYellow(v);
            }
            MatterProfileEvents.syncTo(p);
        });
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Matéria " + type + " de " + p.getName().getString() + " = " + v)
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int add(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String type)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        float v = FloatArgumentType.getFloat(ctx, "value");
        p.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            switch (type) {
                case "dark"   -> profile.addDark(v);
                case "white"  -> profile.addWhite(v);
                case "yellow" -> profile.addYellow(v);
            }
            MatterProfileEvents.syncTo(p);
        });
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Matéria " + type + " " + (v >= 0 ? "+" : "") + v + " em " + p.getName().getString())
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int get(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        p.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "%s — DM:%.1f WM:%.1f YM:%.1f → %s",
                    p.getName().getString(), profile.getDark(), profile.getWhite(),
                    profile.getYellow(), profile.getActiveType().name()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        });
        return 1;
    }

    private static int clear(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        p.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            profile.setDark(0); profile.setWhite(0); profile.setYellow(0);
            MatterProfileEvents.syncTo(p);
        });
        ctx.getSource().sendSuccess(() -> Component.literal("Perfil zerado")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int clearSelf(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        p.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            profile.setDark(0); profile.setWhite(0); profile.setYellow(0);
            MatterProfileEvents.syncTo(p);
        });
        ctx.getSource().sendSuccess(() -> Component.literal("✓ Seu perfil de matter foi zerado")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
