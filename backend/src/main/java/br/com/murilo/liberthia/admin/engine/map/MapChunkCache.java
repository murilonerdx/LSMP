package br.com.murilo.liberthia.admin.engine.map;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Cache de chunks do mapa em PostgreSQL. PNG armazenado como BYTEA.
 * Reduz round-trip ao mod e permite consultar histórico de chunks.
 */
@Entity
@Table(name = "map_chunks")
@IdClass(MapChunkCache.PK.class)
public class MapChunkCache {

    @Id
    @Column(length = 32)
    private String dim;

    @Id
    private int cx;

    @Id
    private int cz;

    @Lob
    @Column(columnDefinition = "BYTEA")
    private byte[] png;

    private Instant updatedAt;

    public MapChunkCache() {}
    public MapChunkCache(String dim, int cx, int cz, byte[] png) {
        this.dim = dim;
        this.cx = cx;
        this.cz = cz;
        this.png = png;
        this.updatedAt = Instant.now();
    }

    public String getDim() { return dim; }
    public int getCx() { return cx; }
    public int getCz() { return cz; }
    public byte[] getPng() { return png; }
    public void setPng(byte[] png) { this.png = png; this.updatedAt = Instant.now(); }
    public Instant getUpdatedAt() { return updatedAt; }

    public static class PK implements Serializable {
        private String dim;
        private int cx;
        private int cz;

        public PK() {}
        public PK(String dim, int cx, int cz) { this.dim = dim; this.cx = cx; this.cz = cz; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK)) return false;
            PK pk = (PK) o;
            return cx == pk.cx && cz == pk.cz && Objects.equals(dim, pk.dim);
        }
        @Override public int hashCode() { return Objects.hash(dim, cx, cz); }
    }
}
