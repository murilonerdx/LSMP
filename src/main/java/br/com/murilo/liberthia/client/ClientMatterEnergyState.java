package br.com.murilo.liberthia.client;

public class ClientMatterEnergyState {
    private static int darkEnergy = 0;
    private static int clearEnergy = 0;
    private static int yellowEnergy = 0;
    private static boolean stabilized = false;

    public static void set(int dark, int clear, int yellow, boolean stab) {
        darkEnergy = dark;
        clearEnergy = clear;
        yellowEnergy = yellow;
        stabilized = stab;
    }

    public static int getDarkEnergy() { return darkEnergy; }
    public static int getClearEnergy() { return clearEnergy; }
    public static int getYellowEnergy() { return yellowEnergy; }
    public static boolean isStabilized() { return stabilized; }
}
