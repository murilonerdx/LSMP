package br.com.murilo.liberthia.cosmic.observatory;

import br.com.murilo.liberthia.LiberthiaMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r48: <b>Caretaker Commands</b> — admin interface pra Observatory.
 *
 * <pre>
 * /liberthia caretaker mark &lt;player&gt;
 *   Marca player pra observation injection (escala anomalias)
 *
 * /liberthia caretaker unmark &lt;player&gt;
 *   Remove a marca
 *
 * /liberthia caretaker tier &lt;player&gt; &lt;1-5&gt;
 *   Força tier de injection (skip escalation)
 *
 * /liberthia caretaker status &lt;player&gt;
 *   Mostra estado completo do player (marked, tier, sanity, insanity)
 *
 * /liberthia caretaker reflect &lt;player&gt;
 *   Spawn 1 ReflectionEntity perto do player target sem usar Reflection Seed
 *
 * /liberthia caretaker dismiss
 *   Remove TODOS os ReflectionEntity ativos no mundo atual
 * </pre>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CaretakerCommand {

    private CaretakerCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(Commands.literal("liberthia")
                .then(Commands.literal("caretaker")
                        .requires(s -> s.hasPermission(2))

                        .then(Commands.literal("mark")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(CaretakerCommand::markPlayer)))

                        .then(Commands.literal("unmark")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(CaretakerCommand::unmarkPlayer)))

                        .then(Commands.literal("tier")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                                                .executes(CaretakerCommand::setTier))))

                        .then(Commands.literal("status")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(CaretakerCommand::status)))

                        .then(Commands.literal("reflect")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(CaretakerCommand::reflect)))

                        .then(Commands.literal("dismiss")
                                .executes(CaretakerCommand::dismissAll))));
    }

    private static int markPlayer(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ObservationInjection.mark(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§4§l✦ " + target.getName().getString() + " §4§omarcado pelos Caretakers."), true);
        return 1;
    }

    private static int unmarkPlayer(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ObservationInjection.unmark(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Marca removida de " + target.getName().getString()), true);
        return 1;
    }

    private static int setTier(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int tier = IntegerArgumentType.getInteger(ctx, "level");
        if (!ObservationInjection.isMarked(target)) ObservationInjection.mark(target);
        target.getPersistentData().putInt(ObservationInjection.NBT_INTENSITY, tier);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Tier " + tier + " setado em " + target.getName().getString()), true);
        return 1;
    }

    private static int status(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        boolean marked = ObservationInjection.isMarked(target);
        int tier = ObservationInjection.getIntensity(target);
        String snap = br.com.murilo.liberthia.cosmic.insanity.InsanityData.snapshot(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                String.format("§7[%s]\n  §fMarked: %s§r §7tier %d\n  §f%s",
                        target.getName().getString(),
                        marked ? "§4§lYES" : "§7no",
                        tier, snap)), false);
        return 1;
    }

    private static int reflect(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        var sl = target.serverLevel();

        double angle = Math.random() * Math.PI * 2;
        double dist = 16 + Math.random() * 16;
        double spawnX = target.getX() + Math.cos(angle) * dist;
        double spawnZ = target.getZ() + Math.sin(angle) * dist;
        int spawnY = sl.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                (int) spawnX, (int) spawnZ);

        ReflectionEntity clone = br.com.murilo.liberthia.registry.ModEntities.REFLECTION_ENTITY.get().create(sl);
        if (clone == null) return 0;
        clone.setOwnerUuid(target.getUUID());
        clone.setOwnerName(target.getName().getString());
        // r49: copia equipamento exato do target (armor + hands)
        clone.copyEquipmentFrom(target);
        clone.spawnedTick = sl.getGameTime();
        clone.moveTo(spawnX + 0.5, spawnY, spawnZ + 0.5,
                (float)(Math.random() * 360), 0);
        sl.addFreshEntity(clone);
        CloneChatSimulator.startSession(target, clone);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§4§l✦ §r§4Reflexo plantado em " + (int)spawnX + ", " + spawnY + ", " + (int)spawnZ
                        + " (modelo: " + target.getName().getString() + ")"), false);
        return 1;
    }

    private static int dismissAll(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var sl = ctx.getSource().getLevel();
        int n = 0;
        for (ReflectionEntity re : sl.getEntitiesOfClass(ReflectionEntity.class,
                new net.minecraft.world.phys.AABB(-30000000, -100, -30000000,
                        30000000, 400, 30000000))) {
            re.vanish();
            n++;
        }
        int count = n;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a✓ " + count + " reflexos dispersados"), true);
        return n;
    }
}
