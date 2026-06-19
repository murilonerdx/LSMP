package br.com.murilo.liberthia.admin.engine.choice;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "choice_decisions")
public class ChoiceDecision {

    @Id
    @Column(length = 64)
    private String id;

    @Column(length = 256)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String optionsJson;  // [{text,color,action,payload,resultMessage}]

    private boolean multiResponse;

    /** Estado ativo: target (uuid ou "all") + responses (uuids JSON). null = não-ativa. */
    @Column(length = 64)
    private String activeTarget;

    @Column(columnDefinition = "TEXT")
    private String activeResponsesJson;

    private Instant activatedAt;

    public String getId() { return id; } public void setId(String v) { this.id = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public String getQuestion() { return question; } public void setQuestion(String v) { this.question = v; }
    public String getOptionsJson() { return optionsJson; } public void setOptionsJson(String v) { this.optionsJson = v; }
    public boolean isMultiResponse() { return multiResponse; } public void setMultiResponse(boolean v) { this.multiResponse = v; }
    public String getActiveTarget() { return activeTarget; } public void setActiveTarget(String v) { this.activeTarget = v; }
    public String getActiveResponsesJson() { return activeResponsesJson; } public void setActiveResponsesJson(String v) { this.activeResponsesJson = v; }
    public Instant getActivatedAt() { return activatedAt; } public void setActivatedAt(Instant v) { this.activatedAt = v; }
}
