package br.com.murilo.liberthia.admin.engine.history;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "chat_log", indexes = {
        @Index(name = "idx_chat_ts", columnList = "ts DESC"),
        @Index(name = "idx_chat_uuid", columnList = "uuid")
})
public class ChatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant ts;

    @Column(length = 64)
    private String uuid;

    @Column(length = 64)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String message;

    public ChatLog() {}

    public ChatLog(String uuid, String name, String message) {
        this.ts = Instant.now();
        this.uuid = uuid;
        this.name = name;
        this.message = message;
    }

    public Long getId() { return id; }
    public Instant getTs() { return ts; }
    public void setTs(Instant ts) { this.ts = ts; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
