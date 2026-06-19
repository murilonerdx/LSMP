package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Sugestão de "splash" — frase curta estilo loading screen do menu vanilla
 * do Minecraft ("Also try Minecraft Dungeons!", "Made in Sweden!"). Aparece
 * randomicamente na tela inicial do mod. Tester propõe, outros votam, admin
 * aprova / rejeita.
 *
 * Separado de TesterSuggestion (que é pra items/blocos/mecânicas) porque o
 * fluxo é diferente: splash não tem detalhes técnicos, recipe, etc. É só
 * texto curto + cor + categoria.
 */
@Entity
@Table(name = "splash_suggestions", indexes = {
        @Index(name = "idx_splash_author", columnList = "authorMcName"),
        @Index(name = "idx_splash_status", columnList = "status")
})
public class SplashSuggestion {

    public enum Status { PENDING, APPROVED, REJECTED, RETIRED }
    public enum Category {
        FUNNY,        // piadas/memes
        LORE,         // refs ao universo do mod
        WARNING,      // dicas pro player
        TECHNICAL,    // tipo "now with 100% more dark matter"
        EVENT,        // sazonal (natal, dia das bruxas, aniversário)
        META          // refs à comunidade/devs
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    private String authorMcName;

    @Column(length = 200, nullable = false)
    private String text;

    /** Cor hex opcional pra deixar o splash colorido (ex: "#FFAA00"). */
    @Column(length = 16)
    private String colorHex;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Category category = Category.FUNNY;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Status status = Status.PENDING;

    /** Peso pra weighted random — splash com weight maior aparece mais. Default 100. */
    @Column(nullable = false)
    private int weight = 100;

    /** Cache de votos pra ordenação rápida. */
    @Column(nullable = false)
    private int upvotes = 0;

    @Column(nullable = false)
    private int downvotes = 0;

    /** JSON {mcName: 1|-1, ...} */
    @Column(columnDefinition = "TEXT")
    private String votesJson;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @Column(length = 64)
    private String triagedBy;

    @Column
    private Instant triagedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getAuthorMcName() { return authorMcName; } public void setAuthorMcName(String v) { authorMcName = v; }
    public String getText() { return text; } public void setText(String v) { text = v; }
    public String getColorHex() { return colorHex; } public void setColorHex(String v) { colorHex = v; }
    public Category getCategory() { return category; } public void setCategory(Category v) { category = v; }
    public Status getStatus() { return status; } public void setStatus(Status v) { status = v; }
    public int getWeight() { return weight; } public void setWeight(int v) { weight = v; }
    public int getUpvotes() { return upvotes; } public void setUpvotes(int v) { upvotes = v; }
    public int getDownvotes() { return downvotes; } public void setDownvotes(int v) { downvotes = v; }
    public String getVotesJson() { return votesJson; } public void setVotesJson(String v) { votesJson = v; }
    public String getAdminNote() { return adminNote; } public void setAdminNote(String v) { adminNote = v; }
    public String getTriagedBy() { return triagedBy; } public void setTriagedBy(String v) { triagedBy = v; }
    public Instant getTriagedAt() { return triagedAt; } public void setTriagedAt(Instant v) { triagedAt = v; }
    public Instant getCreatedAt() { return createdAt; }
}
