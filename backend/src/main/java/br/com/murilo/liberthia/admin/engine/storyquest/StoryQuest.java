package br.com.murilo.liberthia.admin.engine.storyquest;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Questline narrativa. Cada nó tem objetivo, recompensa, e referência ao próximo nó.
 * Pode ser exportada como JSON FTB Quests ou rodada via lógica interna.
 */
@Entity
@Table(name = "story_quests", indexes = {
        @Index(name = "idx_quest_name", columnList = "name")
})
public class StoryQuest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 128) private String name;
    @Column(length = 512) private String summary;
    @Column(length = 32) private String archetype = "main";  // main | side | event | tutorial
    @Column(columnDefinition = "TEXT") private String nodesJson;   // flowchart nodes
    @Column(columnDefinition = "TEXT") private String connectionsJson; // edges
    @Column(length = 32) private String startNodeId = "n_start";
    @Column private boolean published = false;
    @Column private int playersStarted = 0;
    @Column private int playersFinished = 0;
    @Column private Instant createdAt;
    @Column private Instant updatedAt;

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getSummary() { return summary; } public void setSummary(String v) { summary = v; }
    public String getArchetype() { return archetype; } public void setArchetype(String v) { archetype = v; }
    public String getNodesJson() { return nodesJson; } public void setNodesJson(String v) { nodesJson = v; }
    public String getConnectionsJson() { return connectionsJson; } public void setConnectionsJson(String v) { connectionsJson = v; }
    public String getStartNodeId() { return startNodeId; } public void setStartNodeId(String v) { startNodeId = v; }
    public boolean isPublished() { return published; } public void setPublished(boolean v) { published = v; }
    public int getPlayersStarted() { return playersStarted; } public void setPlayersStarted(int v) { playersStarted = v; }
    public int getPlayersFinished() { return playersFinished; } public void setPlayersFinished(int v) { playersFinished = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
