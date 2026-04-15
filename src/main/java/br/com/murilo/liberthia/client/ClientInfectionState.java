package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.network.S2CInfectionSyncPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientInfectionState {
    private static int infection;
    private static int permanentHealthPenalty;
    private static int stage;
    private static int rawExposure;
    private static int blockedExposure;
    private static int armorProtectionPercent;
    private static String mutations = "";
    private static float chunkDensity;

    private ClientInfectionState() {
    }

    public static void apply(S2CInfectionSyncPacket packet) {
        infection = packet.infection();
        permanentHealthPenalty = packet.permanentHealthPenalty();
        stage = packet.stage();
        rawExposure = packet.rawExposure();
        blockedExposure = packet.blockedExposure();
        armorProtectionPercent = packet.armorProtectionPercent();
        mutations = packet.mutations();
        chunkDensity = packet.chunkDensity();
    }

    public static int getInfection() {
        return infection;
    }

    public static int getPermanentHealthPenalty() {
        return permanentHealthPenalty;
    }

    public static int getStage() {
        return stage;
    }

    public static int getRawExposure() {
        return rawExposure;
    }

    public static int getBlockedExposure() {
        return blockedExposure;
    }
    
    public static int getEffectiveExposure() {
        return Math.max(0, rawExposure - blockedExposure);
    }

    public static int getArmorProtectionPercent() {
        return armorProtectionPercent;
    }

    public static boolean hasYellowArmorProtection() {
        return armorProtectionPercent > 0;
    }

    public static String getMutations() {
        return mutations != null ? mutations : "";
    }

    public static float getChunkDensity() {
        return chunkDensity;
    }
}