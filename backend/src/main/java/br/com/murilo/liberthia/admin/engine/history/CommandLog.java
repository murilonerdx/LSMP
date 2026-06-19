package br.com.murilo.liberthia.admin.engine.history;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "command_log", indexes = {
        @Index(name = "idx_cmd_ts", columnList = "ts DESC"),
        @Index(name = "idx_cmd_uuid", columnList = "uuid")
})
public class CommandLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant ts;

    @Column(length = 64)
    private String uuid;

    @Column(length = 64)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String command;

    private boolean isPlayer;

    public CommandLog() {}
    public CommandLog(String uuid, String name, String command, boolean isPlayer) {
        this.ts = Instant.now();
        this.uuid = uuid;
        this.name = name;
        this.command = command;
        this.isPlayer = isPlayer;
    }

    public Long getId() { return id; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public boolean isPlayer() { return isPlayer; }
    public void setPlayer(boolean player) { isPlayer = player; }
}
