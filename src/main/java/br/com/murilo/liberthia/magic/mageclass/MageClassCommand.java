package br.com.murilo.liberthia.magic.mageclass;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;

/**
 * r162: Sub-comando {@code /liberthia mageclass <className>} pra setar classe.
 */
@Mod.EventBusSubscriber(modid = br.com.murilo.liberthia.LiberthiaMod.MODID)
public final class MageClassCommand {

    private MageClassCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("liberthia")
                        .then(Commands.literal("mageclass")
                                .then(Commands.argument("class", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            Arrays.stream(MageClass.values()).forEach(c -> builder.suggest(c.name()));
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "class");
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            try {
                                                MageClass cls = MageClass.valueOf(name);
                                                MageClassData.set(p, cls);
                                                p.sendSystemMessage(Component.literal(
                                                        "§a✓ Você agora é " + cls.colorCode + "§l"
                                                                + cls.displayName + "§r §7(nível §e"
                                                                + MageClassData.getLevel(p) + "§7/10)"));
                                                if (p.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                                                    sl.playSound(null, p.blockPosition(),
                                                            net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                                                            net.minecraft.sounds.SoundSource.PLAYERS, 1F, 1.5F);
                                                }
                                                return 1;
                                            } catch (Exception e) {
                                                p.sendSystemMessage(Component.literal(
                                                        "§cClasse inválida: " + name));
                                                return 0;
                                            }
                                        })))
        );
    }
}
