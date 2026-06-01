package br.com.murilo.liberthia.entry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.command.SpiritualCommand;
import br.com.murilo.liberthia.init.SpiritualState;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = LiberthiaMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ModCommonEvents {

    /** r147: chave NBT no persistent data do player que marca "manual já dado". */
    private static final String MANUAL_GIVEN_KEY = "liberthia.manual_given";

    private ModCommonEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SpiritualCommand.register(event.getDispatcher());
        // r148: Spell Factory commands
        br.com.murilo.liberthia.command.SpellFactoryCommand.register(event.getDispatcher());
        // r166: Sprite VFX testing — /liberthia vfx spawn/list/self
        br.com.murilo.liberthia.command.VfxTestCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SpiritualState.syncTo(serverPlayer);
            // r147: dá o Liberthia Manual no primeiro login, uma vez só.
            giveStarterManual(serverPlayer);
            // Neblinas de terror: manda as zonas ativas pro client que entrou.
            br.com.murilo.liberthia.fog.FogZoneData.get(serverPlayer.serverLevel()).syncTo(serverPlayer);
        }
    }

    /**
     * r147: Dá o Liberthia Manual no primeiro login do player.
     * Usa persistent data (NBT) pra garantir que só dá uma vez.
     */
    private static void giveStarterManual(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(MANUAL_GIVEN_KEY)) return; // já recebeu

        ItemStack manual = new ItemStack(ModItems.LIBERTHIA_MANUAL.get());
        if (!player.getInventory().add(manual)) {
            player.drop(manual, false);
        }
        persisted.putBoolean(MANUAL_GIVEN_KEY, true);
        data.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persisted);

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        "§5✦ §dLiberthia Manual §5✦ §7adicionado ao seu inventário"),
                false);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }

        ArrayList<UUID> activePlayers = new ArrayList<>(SpiritualState.snapshot().keySet());

        for (UUID uuid : activePlayers) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(uuid);

            if (player == null) {
                continue;
            }

            if (SpiritualState.findValidConnectionItem(player).isEmpty()) {
                SpiritualState.forceDisable(player);
            }
        }
    }
}