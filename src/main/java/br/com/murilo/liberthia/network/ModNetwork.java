package br.com.murilo.liberthia.network;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entry.AdminActionC2SPacket;
import br.com.murilo.liberthia.entry.AdminInventoryS2CPacket;
import br.com.murilo.liberthia.entry.AdminPlayerListS2CPacket;
import br.com.murilo.liberthia.entry.FieldJournalSaveC2SPacket;
import br.com.murilo.liberthia.entry.TrackerC2SPacket;
import br.com.murilo.liberthia.entry.TrackerDataS2CPacket;
import br.com.murilo.liberthia.entry.WorkerVoicePlayC2SPacket;
import br.com.murilo.liberthia.network.packet.*;
import br.com.murilo.liberthia.packet.CreateImageBookPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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

        CHANNEL.registerMessage(
                packetId++,
                CreateImageBookPacket.class,
                CreateImageBookPacket::encode,
                CreateImageBookPacket::decode,
                CreateImageBookPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                OpenCapturedScreenPacket.class,
                OpenCapturedScreenPacket::encode,
                OpenCapturedScreenPacket::decode,
                OpenCapturedScreenPacket::handle
        );

        CHANNEL.registerMessage(
                packetId++,
                TryEscapeSealPacket.class,
                TryEscapeSealPacket::encode,
                TryEscapeSealPacket::decode,
                TryEscapeSealPacket::handle
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

        // --- Spiritual connection sync ---
        CHANNEL.registerMessage(
                packetId++,
                ClientboundSpiritualSyncPacket.class,
                ClientboundSpiritualSyncPacket::encode,
                ClientboundSpiritualSyncPacket::decode,
                ClientboundSpiritualSyncPacket::handle
        );

        // --- Matter Profile sync ---
        CHANNEL.registerMessage(
                packetId++,
                MatterProfileSyncS2CPacket.class,
                MatterProfileSyncS2CPacket::encode,
                MatterProfileSyncS2CPacket::decode,
                MatterProfileSyncS2CPacket::handle
        );

        // v1: cliente pode pedir re-sync imediato do profile (usado pela
        // MatterAnalyzerScreen pra evitar tab Perfil em branco).
        CHANNEL.registerMessage(
                packetId++,
                RequestMatterProfileSyncC2SPacket.class,
                RequestMatterProfileSyncC2SPacket::encode,
                RequestMatterProfileSyncC2SPacket::decode,
                RequestMatterProfileSyncC2SPacket::handle
        );

        // --- Dimensional Chest channel ---
        CHANNEL.registerMessage(
                packetId++,
                OpenDimensionalChannelScreenS2CPacket.class,
                OpenDimensionalChannelScreenS2CPacket::encode,
                OpenDimensionalChannelScreenS2CPacket::decode,
                OpenDimensionalChannelScreenS2CPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                SetDimensionalChannelC2SPacket.class,
                SetDimensionalChannelC2SPacket::encode,
                SetDimensionalChannelC2SPacket::decode,
                SetDimensionalChannelC2SPacket::handle
        );

        // v0.1.27: teleport da Clear Matter armor via shift+right-click no AR
        // (RightClickEmpty é client-only, então cliente manda esse packet).
        CHANNEL.registerMessage(
                packetId++,
                ClearMatterTeleportC2SPacket.class,
                ClearMatterTeleportC2SPacket::encode,
                ClearMatterTeleportC2SPacket::decode,
                ClearMatterTeleportC2SPacket::handle
        );

        // --- v1: Vision Swap (White Matter ≥ 50 random pick) ---
        CHANNEL.registerMessage(
                packetId++,
                StartVisionSwapS2CPacket.class,
                StartVisionSwapS2CPacket::encode,
                StartVisionSwapS2CPacket::decode,
                StartVisionSwapS2CPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                EndVisionSwapS2CPacket.class,
                EndVisionSwapS2CPacket::encode,
                EndVisionSwapS2CPacket::decode,
                EndVisionSwapS2CPacket::handle
        );

        // --- v1: Possession (mind-control via amulet) ---
        CHANNEL.registerMessage(
                packetId++,
                StartPossessionS2CPacket.class,
                StartPossessionS2CPacket::encode,
                StartPossessionS2CPacket::decode,
                StartPossessionS2CPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                EndPossessionS2CPacket.class,
                EndPossessionS2CPacket::encode,
                EndPossessionS2CPacket::decode,
                EndPossessionS2CPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                LockedInputS2CPacket.class,
                LockedInputS2CPacket::encode,
                LockedInputS2CPacket::decode,
                LockedInputS2CPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                PossessionMoveC2SPacket.class,
                PossessionMoveC2SPacket::encode,
                PossessionMoveC2SPacket::decode,
                PossessionMoveC2SPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                PossessionAttackC2SPacket.class,
                PossessionAttackC2SPacket::encode,
                PossessionAttackC2SPacket::decode,
                PossessionAttackC2SPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                EndPossessionC2SPacket.class,
                EndPossessionC2SPacket::encode,
                EndPossessionC2SPacket::decode,
                EndPossessionC2SPacket::handle
        );

        // v0.1.49: Possession actions — quebrar bloco e usar item enquanto possui
        CHANNEL.registerMessage(
                packetId++,
                PossessionBreakBlockC2SPacket.class,
                PossessionBreakBlockC2SPacket::encode,
                PossessionBreakBlockC2SPacket::decode,
                PossessionBreakBlockC2SPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                PossessionUseItemC2SPacket.class,
                PossessionUseItemC2SPacket::encode,
                PossessionUseItemC2SPacket::decode,
                PossessionUseItemC2SPacket::handle
        );

        // v0.1.22: Boss Crown — rename de bossbar custom
        CHANNEL.registerMessage(
                packetId++,
                OpenBossCrownNameScreenS2CPacket.class,
                OpenBossCrownNameScreenS2CPacket::encode,
                OpenBossCrownNameScreenS2CPacket::decode,
                OpenBossCrownNameScreenS2CPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                SetBossCrownNameC2SPacket.class,
                SetBossCrownNameC2SPacket::encode,
                SetBossCrownNameC2SPacket::decode,
                SetBossCrownNameC2SPacket::handle
        );
        // v0.1.22 r23: Cosmic Horror — 3 packets de alucinação/overlay
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.network.packet.MadnessHallucinationS2CPacket.class,
                br.com.murilo.liberthia.network.packet.MadnessHallucinationS2CPacket::encode,
                br.com.murilo.liberthia.network.packet.MadnessHallucinationS2CPacket::decode,
                br.com.murilo.liberthia.network.packet.MadnessHallucinationS2CPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.network.packet.MaddenedTargetS2CPacket.class,
                br.com.murilo.liberthia.network.packet.MaddenedTargetS2CPacket::encode,
                br.com.murilo.liberthia.network.packet.MaddenedTargetS2CPacket::decode,
                br.com.murilo.liberthia.network.packet.MaddenedTargetS2CPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.network.packet.MirrorInsanityS2CPacket.class,
                br.com.murilo.liberthia.network.packet.MirrorInsanityS2CPacket::encode,
                br.com.murilo.liberthia.network.packet.MirrorInsanityS2CPacket::decode,
                br.com.murilo.liberthia.network.packet.MirrorInsanityS2CPacket::handle
        );
        // v0.1.22 r24: Spirit World sanity sync
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket.class,
                br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket::encode,
                br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket::decode,
                br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket::handle
        );
        // v0.1.22 r27: Dimensional Antenna — set frequency
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.network.packet.SetAntennaFrequencyC2SPacket.class,
                br.com.murilo.liberthia.network.packet.SetAntennaFrequencyC2SPacket::encode,
                br.com.murilo.liberthia.network.packet.SetAntennaFrequencyC2SPacket::decode,
                br.com.murilo.liberthia.network.packet.SetAntennaFrequencyC2SPacket::handle
        );

        // v0.1.22 r28: Quantum Terminal — action (set freq / send msg)
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.network.packet.TerminalActionC2SPacket.class,
                br.com.murilo.liberthia.network.packet.TerminalActionC2SPacket::encode,
                br.com.murilo.liberthia.network.packet.TerminalActionC2SPacket::decode,
                br.com.murilo.liberthia.network.packet.TerminalActionC2SPacket::handle
        );

        // v0.1.22 r30: Possession Ability (mob-specific habilidades)
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.network.packet.PossessionAbilityC2SPacket.class,
                br.com.murilo.liberthia.network.packet.PossessionAbilityC2SPacket::encode,
                br.com.murilo.liberthia.network.packet.PossessionAbilityC2SPacket::decode,
                br.com.murilo.liberthia.network.packet.PossessionAbilityC2SPacket::handle
        );

        // v0.1.22 r30: Crown of Mass Possession — selecionar player + voice
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.network.packet.CrownVoiceTargetC2SPacket.class,
                br.com.murilo.liberthia.network.packet.CrownVoiceTargetC2SPacket::encode,
                br.com.murilo.liberthia.network.packet.CrownVoiceTargetC2SPacket::decode,
                br.com.murilo.liberthia.network.packet.CrownVoiceTargetC2SPacket::handle
        );

        // r35: Cosmic Horror sync (server → client phase + intensity)
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.cosmic.CosmicSyncS2CPacket.class,
                br.com.murilo.liberthia.cosmic.CosmicSyncS2CPacket::encode,
                br.com.murilo.liberthia.cosmic.CosmicSyncS2CPacket::decode,
                br.com.murilo.liberthia.cosmic.CosmicSyncS2CPacket::handle
        );

        // r38: Cosmic Horror — força rotação da câmera (mouse girando sozinho)
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.cosmic.CosmicForceRotationS2CPacket.class,
                br.com.murilo.liberthia.cosmic.CosmicForceRotationS2CPacket::encode,
                br.com.murilo.liberthia.cosmic.CosmicForceRotationS2CPacket::decode,
                br.com.murilo.liberthia.cosmic.CosmicForceRotationS2CPacket::handle
        );

        // r40: Hallucination — server injeta hallucination client-only
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.cosmic.hallucination.HallucinationS2CPacket.class,
                br.com.murilo.liberthia.cosmic.hallucination.HallucinationS2CPacket::encode,
                br.com.murilo.liberthia.cosmic.hallucination.HallucinationS2CPacket::decode,
                br.com.murilo.liberthia.cosmic.hallucination.HallucinationS2CPacket::handle
        );

        // r50: Caretaker Console packets — start/stop fake msg session
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.cosmic.observatory.console.StartCaretakerSessionC2SPacket.class,
                br.com.murilo.liberthia.cosmic.observatory.console.StartCaretakerSessionC2SPacket::encode,
                br.com.murilo.liberthia.cosmic.observatory.console.StartCaretakerSessionC2SPacket::decode,
                br.com.murilo.liberthia.cosmic.observatory.console.StartCaretakerSessionC2SPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.cosmic.observatory.console.StopCaretakerSessionC2SPacket.class,
                br.com.murilo.liberthia.cosmic.observatory.console.StopCaretakerSessionC2SPacket::encode,
                br.com.murilo.liberthia.cosmic.observatory.console.StopCaretakerSessionC2SPacket::decode,
                br.com.murilo.liberthia.cosmic.observatory.console.StopCaretakerSessionC2SPacket::handle
        );

        // r42: Custom Spells — craft / cast / select / delete / sync
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.magic.custom.CraftSpellC2SPacket.class,
                br.com.murilo.liberthia.magic.custom.CraftSpellC2SPacket::encode,
                br.com.murilo.liberthia.magic.custom.CraftSpellC2SPacket::decode,
                br.com.murilo.liberthia.magic.custom.CraftSpellC2SPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.magic.custom.CastCustomSpellC2SPacket.class,
                br.com.murilo.liberthia.magic.custom.CastCustomSpellC2SPacket::encode,
                br.com.murilo.liberthia.magic.custom.CastCustomSpellC2SPacket::decode,
                br.com.murilo.liberthia.magic.custom.CastCustomSpellC2SPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.magic.custom.SelectSpellC2SPacket.class,
                br.com.murilo.liberthia.magic.custom.SelectSpellC2SPacket::encode,
                br.com.murilo.liberthia.magic.custom.SelectSpellC2SPacket::decode,
                br.com.murilo.liberthia.magic.custom.SelectSpellC2SPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.magic.custom.DeleteSpellC2SPacket.class,
                br.com.murilo.liberthia.magic.custom.DeleteSpellC2SPacket::encode,
                br.com.murilo.liberthia.magic.custom.DeleteSpellC2SPacket::decode,
                br.com.murilo.liberthia.magic.custom.DeleteSpellC2SPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.magic.custom.SyncCustomSpellsS2CPacket.class,
                br.com.murilo.liberthia.magic.custom.SyncCustomSpellsS2CPacket::encode,
                br.com.murilo.liberthia.magic.custom.SyncCustomSpellsS2CPacket::decode,
                br.com.murilo.liberthia.magic.custom.SyncCustomSpellsS2CPacket::handle
        );

        // r54: Open Spell Wheel — Grimoire shift+rclick chama isso server-side
        CHANNEL.registerMessage(
                packetId++,
                OpenSpellWheelS2CPacket.class,
                OpenSpellWheelS2CPacket::encode,
                OpenSpellWheelS2CPacket::decode,
                OpenSpellWheelS2CPacket::handle
        );

        // r65: Observation Casting — Composition GUI + Save + Animation packets
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.observation.network.OpenCompositionScreenS2CPacket.class,
                br.com.murilo.liberthia.observation.network.OpenCompositionScreenS2CPacket::encode,
                br.com.murilo.liberthia.observation.network.OpenCompositionScreenS2CPacket::decode,
                br.com.murilo.liberthia.observation.network.OpenCompositionScreenS2CPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.observation.network.SaveCompositionC2SPacket.class,
                br.com.murilo.liberthia.observation.network.SaveCompositionC2SPacket::encode,
                br.com.murilo.liberthia.observation.network.SaveCompositionC2SPacket::decode,
                br.com.murilo.liberthia.observation.network.SaveCompositionC2SPacket::handle
        );
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.observation.network.CastAnimationS2CPacket.class,
                br.com.murilo.liberthia.observation.network.CastAnimationS2CPacket::encode,
                br.com.murilo.liberthia.observation.network.CastAnimationS2CPacket::decode,
                br.com.murilo.liberthia.observation.network.CastAnimationS2CPacket::handle
        );
        // r71: HUD position S2C
        CHANNEL.registerMessage(
                packetId++,
                br.com.murilo.liberthia.observation.network.HudPositionS2CPacket.class,
                br.com.murilo.liberthia.observation.network.HudPositionS2CPacket::encode,
                br.com.murilo.liberthia.observation.network.HudPositionS2CPacket::decode,
                br.com.murilo.liberthia.observation.network.HudPositionS2CPacket::handle
        );
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(MinecraftServer server, Object packet) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToPlayer(player, packet);
        }
    }
}