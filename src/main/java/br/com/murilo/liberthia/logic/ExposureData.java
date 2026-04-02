package br.com.murilo.liberthia.logic;

public class ExposureData {
    private final int rawDarkPressure;
    private final int effectiveDarkPressure;
    private final int blockedExposure;
    private final int clearRelief;
    private final int clearPressure;
    private final int yellowPressure;
    private final boolean immersedInDark;
    private final boolean touchingClear;
    private final boolean touchingYellow;
    private final boolean carryingDarkMatter;
    private final int armorPieces;
    private final int armorProtectionPercent;

    public ExposureData(
            int rawDarkPressure,
            int effectiveDarkPressure,
            int blockedExposure,
            int clearRelief,
            int clearPressure,
            int yellowPressure,
            boolean immersedInDark,
            boolean touchingClear,
            boolean touchingYellow,
            boolean carryingDarkMatter,
            int armorPieces,
            int armorProtectionPercent
    ) {
        this.rawDarkPressure = rawDarkPressure;
        this.effectiveDarkPressure = effectiveDarkPressure;
        this.blockedExposure = blockedExposure;
        this.clearRelief = clearRelief;
        this.clearPressure = clearPressure;
        this.yellowPressure = yellowPressure;
        this.immersedInDark = immersedInDark;
        this.touchingClear = touchingClear;
        this.touchingYellow = touchingYellow;
        this.carryingDarkMatter = carryingDarkMatter;
        this.armorPieces = armorPieces;
        this.armorProtectionPercent = armorProtectionPercent;
    }

    public int rawDarkPressure() { return rawDarkPressure; }
    public int effectiveDarkPressure() { return effectiveDarkPressure; }
    public int blockedExposure() { return blockedExposure; }
    public int clearRelief() { return clearRelief; }
    public int clearPressure() { return clearPressure; }
    public int yellowPressure() { return yellowPressure; }
    public boolean immersedInDark() { return immersedInDark; }
    public boolean touchingClear() { return touchingClear; }
    public boolean touchingYellow() { return touchingYellow; }
    public boolean carryingDarkMatter() { return carryingDarkMatter; }
    public int armorPieces() { return armorPieces; }
    public int armorProtectionPercent() { return armorProtectionPercent; }
}
