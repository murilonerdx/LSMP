package br.com.murilo.liberthia.capability;

public interface IMatterEnergy {
    int getDarkEnergy();
    void setDarkEnergy(int value);
    void addDarkEnergy(int amount);

    int getClearEnergy();
    void setClearEnergy(int value);
    void addClearEnergy(int amount);

    int getYellowEnergy();
    void setYellowEnergy(int value);
    void addYellowEnergy(int amount);

    int getMaxEnergy();

    boolean isStabilized();
    void setStabilized(boolean stabilized);

    void decay();

    boolean isDirty();
    void setDirty(boolean dirty);
}
