package br.com.murilo.liberthia.network;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entry.AdminActionC2SPacket;
import br.com.murilo.liberthia.entry.AdminInventoryS2CPacket;
import br.com.murilo.liberthia.entry.AdminPlayerListS2CPacket;
import br.com.murilo.liberthia.entry.FieldJournalSaveC2SPacket;
import br.com.murilo.liberthia.entry.TrackerC2SPacket;
import br.com.murilo.liberthia.entry.TrackerDataS2CPacket;
import br.com.murilo.liberthia.entry.WorkerVoicePlayC2SPacket;
import br.com.murilo.liberthia.network.packet.OpenCommandTabletScreenS2CPacket;
import br.com.murilo.liberthia.network.packet.OpenScriptTabletScreenS2CPacket;
import br.com.murilo.liberthia.network.packet.OpenTeleportExecutorScreenS2CPacket;
import br.com.murilo.liberthia.network.packet.OpenWorkerTeleporterScreenS2CPacket;
import br.com.murilo.liberthia.network.packet.SaveCommandTabletC2SPacket;
import br.com.murilo.liberthia.network.packet.SaveScriptTabletC2SPacket;
import br.com.murilo.liberthia.network.packet.TeleportExecutorActionC2SPacket;
import br.com.murilo.liberthia.network.packet.WorkerTeleporterTargetC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static int packetId = 0;
    private static boolean registered = false;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LiberthiaMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        // --- packets que você já tinha ---
        CHANNEL.registerMessage(
                packetId++,
                S2CInfectionSyncPacket.class,
                S2CInfectionSyncPacket::encode,
                S2CInfectionSyncPacket::decode,
                S2CInfectionSyncPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                S2CMatterEnergySyncPacket.class,
                S2CMatterEnergySyncPacket::encode,
                S2CMatterEnergySyncPacket::decode,
                S2CMatterEnergySyncPacket::handle
        );

        // --- admin GUI packets ---
        CHANNEL.registerMessage(
                packetId++,
                AdminActionC2SPacket.class,
                AdminActionC2SPacket::encode,
                AdminActionC2SPacket::decode,
                AdminActionC2SPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                AdminPlayerListS2CPacket.class,
                AdminPlayerListS2CPacket::encode,
                AdminPlayerListS2CPacket::decode,
                AdminPlayerListS2CPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                AdminInventoryS2CPacket.class,
                AdminInventoryS2CPacket::encode,
                AdminInventoryS2CPacket::decode,
                AdminInventoryS2CPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                WorkerVoicePlayC2SPacket.class,
                WorkerVoicePlayC2SPacket::encode,
                WorkerVoicePlayC2SPacket::decode,
                WorkerVoicePlayC2SPacket::handle
        );

        // --- Lore item packets ---
        CHANNEL.registerMessage(
                packetId++,
                FieldJournalSaveC2SPacket.class,
                FieldJournalSaveC2SPacket::encode,
                FieldJournalSaveC2SPacket::decode,
                FieldJournalSaveC2SPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                TrackerC2SPacket.class,
                TrackerC2SPacket::encode,
                TrackerC2SPacket::decode,
                TrackerC2SPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                TrackerDataS2CPacket.class,
                TrackerDataS2CPacket::encode,
                TrackerDataS2CPacket::decode,
                TrackerDataS2CPacket::handle
        );

        // --- Infection toggle sync ---
        CHANNEL.registerMessage(
                packetId++,
                S2CInfectionTogglePacket.class,
                S2CInfectionTogglePacket::encode,
                S2CInfectionTogglePacket::decode,
                S2CInfectionTogglePacket::handle
        );

        // --- Teleport tools packets ---
        CHANNEL.registerMessage(
                packetId++,
                OpenTeleportExecutorScreenS2CPacket.class,
                OpenTeleportExecutorScreenS2CPacket::encode,
                OpenTeleportExecutorScreenS2CPacket::decode,
                OpenTeleportExecutorScreenS2CPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                TeleportExecutorActionC2SPacket.class,
                TeleportExecutorActionC2SPacket::encode,
                TeleportExecutorActionC2SPacket::decode,
                TeleportExecutorActionC2SPacket::handle
        );

        // --- Worker Teleporter (pedra de teletransporte) ---
        CHANNEL.registerMessage(
                packetId++,
                OpenWorkerTeleporterScreenS2CPacket.class,
                OpenWorkerTeleporterScreenS2CPacket::encode,
                OpenWorkerTeleporterScreenS2CPacket::decode,
                OpenWorkerTeleporterScreenS2CPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                WorkerTeleporterTargetC2SPacket.class,
                WorkerTeleporterTargetC2SPacket::encode,
                WorkerTeleporterTargetC2SPacket::decode,
                WorkerTeleporterTargetC2SPacket::handle
        );

        // --- Command Tablet (programmable item) ---
        CHANNEL.registerMessage(
                packetId++,
                OpenCommandTabletScreenS2CPacket.class,
                OpenCommandTabletScreenS2CPacket::encode,
                OpenCommandTabletScreenS2CPacket::decode,
                OpenCommandTabletScreenS2CPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                SaveCommandTabletC2SPacket.class,
                SaveCommandTabletC2SPacket::encode,
                SaveCommandTabletC2SPacket::decode,
                SaveCommandTabletC2SPacket::handle
        );

        // --- Script Tablet (LiberScript) ---
        CHANNEL.registerMessage(
                packetId++,
                OpenScriptTabletScreenS2CPacket.class,
                OpenScriptTabletScreenS2CPacket::encode,
                OpenScriptTabletScreenS2CPacket::decode,
                OpenScriptTabletScreenS2CPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                SaveScriptTabletC2SPacket.class,
                SaveScriptTabletC2SPacket::encode,
                SaveScriptTabletC2SPacket::decode,
                SaveScriptTabletC2SPacket::handle
        );
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}