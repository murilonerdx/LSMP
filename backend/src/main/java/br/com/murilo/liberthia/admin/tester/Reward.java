package br.com.murilo.liberthia.admin.tester;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Recompensa cadastrada pelo admin. Tester resgata gastando pontos.
 * Quando resgatado, o comando `giveCommand` é enviado pro mod executar.
 */
@Entity
@Table(name = "tester_rewards")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Custo em pontos. */
    @Column(nullable = false)
    private int costPoints;

    /** Comando executado in-game quando tester resgata. Ex: /give {player} numismatics:crown 5. */
    @Column(length = 512)
    private String giveCommand;

    @Column(length = 512)
    private String imageUrl;

    /** Categoria: crown, item, title, etc. */
    @Column(length = 32)
    private String category;

    @Column(nullable = false)
    private boolean enabled = true;

    /** Limite de quantos cada tester pode resgatar (0 = ilimitado). */
    @Column(nullable = false)
    private int perTesterLimit = 0;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String v) { name = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public int getCostPoints() { return costPoints; } public void setCostPoints(int v) { costPoints = v; }
    public String getGiveCommand() { return giveCommand; } public void setGiveCommand(String v) { giveCommand = v; }
    public String getImageUrl() { return imageUrl; } public void setImageUrl(String v) { imageUrl = v; }
    public String getCategory() { return category; } public void setCategory(String v) { category = v; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { enabled = v; }
    public int getPerTesterLimit() { return perTesterLimit; } public void setPerTesterLimit(int v) { perTesterLimit = v; }
    public Instant getCreatedAt() { return createdAt; }
}
