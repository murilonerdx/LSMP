package br.com.murilo.liberthia.loom.entity;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModEntities;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/**
 * r173: <b>/liberthia observer &lt;alvos&gt; [minutos]</b> — amarra um
 * Peripheral Observer "inteligente" a um player específico. Ele aparece só no
 * canto do olho dele por X minutos (default 30), some quando encarado (mas
 * reaparece em outro canto) e finalmente desaparece de vez no fim do ciclo.
 *
 * <p>Também: <b>/liberthia observer stop &lt;alvos&gt;</b> remove os observers
 * que estão assombrando aqueles players.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ObserverCommand {

    private ObserverCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("liberthia")
                .then(Commands.literal("observer")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("alvos", EntityArgument.players())
                                .executes(c -> haunt(c, 30))
                                .then(Commands.argument("minutos", IntegerArgumentType.integer(1, 240))
                                        .executes(c -> haunt(c, IntegerArgumentType.getInteger(c, "minutos")))))
                        .then(Commands.literal("stop")
                                .then(Commands.argument("alvos", EntityArgument.players())
                                        .executes(ObserverCommand::stop)))));
    }

    private static int haunt(CommandContext<CommandSourceStack> c, int minutes) {
        Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(c, "alvos");
        } catch (Exception e) {
            return 0;
        }
        int n = 0;
        for (ServerPlayer target : targets) {
            if (!(target.level() instanceof ServerLevel sl)) continue;
            PeripheralObserverEntity e = ModEntities.LOOM_PERIPHERAL.get().create(sl);
            if (e == null) continue;
            // spawn ~9 blocos atrás do player (ele reposiciona no 1º tick)
            Vec3 look = target.getLookAngle();
            double bx = target.getX() - look.x * 9;
            double bz = target.getZ() - look.z * 9;
            e.moveTo(bx, target.getY(), bz, sl.random.nextFloat() * 360, 0);
            e.startHaunt(target.getUUID(), minutes);
            sl.addFreshEntity(e);
            n++;
        }
        final int count = n;
        final int mins = minutes;
        c.getSource().sendSuccess(() -> Component.literal(
                "§5✦ Observer amarrado a " + count + " player(s) por " + mins + " min."), false);
        return n;
    }

    private static int stop(CommandContext<CommandSourceStack> c) {
        Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(c, "alvos");
        } catch (Exception e) {
            return 0;
        }
        int removed = 0;
        for (ServerPlayer target : targets) {
            if (!(target.level() instanceof ServerLevel sl)) continue;
            for (PeripheralObserverEntity obs : sl.getEntitiesOfClass(PeripheralObserverEntity.class,
                    target.getBoundingBox().inflate(96))) {
                obs.discard();
                removed++;
            }
        }
        final int count = removed;
        c.getSource().sendSuccess(() -> Component.literal(
                "§7✦ " + count + " observer(s) removido(s)."), false);
        return removed;
    }
}
