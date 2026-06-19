package br.com.murilo.liberthia.dimension;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModEntities;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r179: <b>/liberthia rift clear [raio]</b> — fecha (remove) Fendas Dimensionais.
 * Sem raio = todas no mundo atual; com raio = só as dentro do raio em blocos.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class RiftCommand {

    private RiftCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("liberthia")
                .then(Commands.literal("rift")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("clear")
                                .executes(c -> clear(c, -1))
                                .then(Commands.argument("raio", IntegerArgumentType.integer(1, 512))
                                        .executes(c -> clear(c, IntegerArgumentType.getInteger(c, "raio")))))));
    }

    private static int clear(CommandContext<CommandSourceStack> c, int radius) {
        CommandSourceStack src = c.getSource();
        ServerLevel sl = src.getLevel();
        Vec3 origin = src.getPosition();
        int[] removed = {0};
        for (DimensionalRiftEntity r : sl.getEntities(ModEntities.DIMENSIONAL_RIFT.get(), e -> true)) {
            if (radius > 0 && r.position().distanceTo(origin) > radius) continue;
            r.discard();
            removed[0]++;
        }
        final int n = removed[0];
        src.sendSuccess(() -> Component.literal("§5✦ " + n + " fenda(s) dimensional(is) fechada(s)."), true);
        return n;
    }
}
