package br.com.murilo.liberthia.admin.engine.dialog;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Árvore de diálogo. nodesJson é um grafo {nodeId -> {text, balloon, choices:[{label, nextId, action}]}}.
 * NPC com tag específica (npcTag) usa essa árvore.
 */
@Entity
@Table(name = "dialog_trees", indexes = {
        @Index(name = "idx_dlg_name", columnList = "name"),
        @Index(name = "idx_dlg_tag", columnList = "npcTag")
})
public class DialogTree {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(length = 128) private String name;
    @Column(length = 64) private String npcTag;          // entidades com essa tag disparam
    @Column(length = 32) private String rootNodeId = "start";
    @Column(columnDefinition = "TEXT") private String nodesJson;
    @Column(length = 16) private String balloonStyle = "talk";  // "talk" | "comic" | "title"
    @Column private boolean active = true;
    @Column private Instant updatedAt;

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getNpcTag() { return npcTag; } public void setNpcTag(String v) { npcTag = v; }
    public String getRootNodeId() { return rootNodeId; } public void setRootNodeId(String v) { rootNodeId = v; }
    public String getNodesJson() { return nodesJson; } public void setNodesJson(String v) { nodesJson = v; }
    public String getBalloonStyle() { return balloonStyle; } public void setBalloonStyle(String v) { balloonStyle = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { active = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { updatedAt = v; }
}
