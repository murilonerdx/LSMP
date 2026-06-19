package br.com.murilo.liberthia.admin.engine.pilgrimage;

import br.com.murilo.liberthia.admin.engine.anchor.SaveAnchor;
import br.com.murilo.liberthia.admin.engine.glyph.Glyph;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * Resolve uma "estação" de pilgrimage (memorial/glyph/anchor/coords) para
 * coordenadas {x,y,z,dim,radius}. Cruza com tabelas existentes via EntityManager
 * pra evitar acoplamento com outros services (Memorial vive no frontend kv).
 *
 * Para `memorial`, lê do kv_configs (frontend localStorage migrado). Pra
 * glyph/anchor lê das tabelas. Pra `coords` usa direto do JSON.
 */
@Component
public class StationResolver {

    @PersistenceContext
    private EntityManager em;

    public static final class Station {
        public double x, y, z;
        public String dim;
        public double radius;
        public String label;
    }

    public Station resolve(JsonNode step) {
        if (step == null || !step.has("type")) return null;
        String type = step.path("type").asText();
        return switch (type) {
            case "memorial" -> resolveMemorial(step.path("refId").asText());
            case "glyph" -> resolveGlyph(step.path("refId").asText());
            case "anchor" -> resolveAnchor(step.path("refId").asText());
            case "coords" -> resolveCoords(step);
            default -> null;
        };
    }

    private Station resolveGlyph(String id) {
        if (id == null || id.isBlank()) return null;
        Glyph g = em.find(Glyph.class, id);
        if (g == null) return null;
        Station s = new Station();
        s.x = g.getPosX(); s.y = g.getPosY(); s.z = g.getPosZ();
        s.dim = g.getPosDim();
        s.radius = Math.max(1, g.getRadius());
        s.label = g.getName();
        return s;
    }

    private Station resolveAnchor(String id) {
        if (id == null || id.isBlank()) return null;
        SaveAnchor a = em.find(SaveAnchor.class, id);
        if (a == null) return null;
        Station s = new Station();
        s.x = a.getPosX(); s.y = a.getPosY(); s.z = a.getPosZ();
        s.dim = a.getPosDim();
        s.radius = Math.max(1, a.getRadius());
        s.label = a.getName();
        return s;
    }

    /**
     * Memorial vive no frontend (localStorage kv). Para resolver, lê o JSON
     * armazenado na key "memorials" do kv_configs e procura por id.
     * Acoplamento aceito porque Memorial ainda não tem backend dedicado.
     */
    private Station resolveMemorial(String id) {
        if (id == null || id.isBlank()) return null;
        try {
            var query = em.createNativeQuery("SELECT data_json FROM kv_configs WHERE key = 'memorials'");
            Object raw = query.getSingleResult();
            if (raw == null) return null;
            JsonNode arr = new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw.toString());
            if (!arr.isArray()) return null;
            for (JsonNode m : arr) {
                if (id.equals(m.path("id").asText())) {
                    Station s = new Station();
                    s.x = m.path("x").asDouble();
                    s.y = m.path("y").asDouble();
                    s.z = m.path("z").asDouble();
                    s.dim = "overworld"; // memorial não armazena dim atualmente
                    s.radius = 4.0;
                    s.label = m.path("title").asText("Memorial");
                    return s;
                }
            }
        } catch (Exception ignored) { /* sem memorials ou parse fail */ }
        return null;
    }

    private Station resolveCoords(JsonNode step) {
        Station s = new Station();
        s.x = step.path("x").asDouble();
        s.y = step.path("y").asDouble();
        s.z = step.path("z").asDouble();
        s.dim = step.path("dim").asText("overworld");
        s.radius = step.path("radius").asDouble(5.0);
        s.label = step.path("label").asText("(custom)");
        return s;
    }
}
