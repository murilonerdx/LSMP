package br.com.murilo.liberthia.admin.engine.madness;

import jakarta.persistence.*;

import java.time.Instant;

/** Sanidade atual de cada player. */
@Entity
@Table(name = "madness_state")
public class MadnessState {

    @Id
    @Column(length = 64)
    private String uuid;

    @Column(length = 64)
    private String name;

    private double sanity;

    private Instant lastTick;

    public MadnessState() {}
    public MadnessState(String uuid, String name, double sanity) {
        this.uuid = uuid;
        this.name = name;
        this.sanity = sanity;
        this.lastTick = Instant.now();
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getSanity() { return sanity; }
    public void setSanity(double sanity) { this.sanity = sanity; }
    public Instant getLastTick() { return lastTick; }
    public void setLastTick(Instant lastTick) { this.lastTick = lastTick; }
}
