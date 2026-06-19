package br.com.murilo.liberthia.fog;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Uma "zona de neblina" — uma esfera no mundo preenchida por fumaça/névoa
 * visível (partículas), estilo filme de terror.
 *
 * <p>Renderizada 100% client-side ({@code client.fog.ClientFogParticles} +
 * {@code ClientFogEvents}), mas a fonte da verdade é o servidor
 * ({@code FogZoneData}, persistido + sincronizado).
 *
 * <h2>Parâmetros visuais ajustáveis</h2>
 * <ul>
 *   <li><b>density</b> (densidade): quantas partículas spawnam (0..2).</li>
 *   <li><b>opacity</b> (opacidade/transparência): opacidade de cada puff
 *       (0 = quase invisível, 1 = bem densa). É o inverso de transparência.</li>
 *   <li><b>thickness</b> (intensidade): tamanho de cada puff de fumaça (0.5..6).</li>
 *   <li><b>viewDistance</b> (distância): a que distância do player a névoa
 *       renderiza (8..120 blocos).</li>
 * </ul>
 */
public final class FogZone {

    // Defaults compartilhados (comando, item, e load de saves antigos sem os campos)
    public static final float  DEFAULT_DENSITY   = 1.0f;
    public static final float  DEFAULT_OPACITY   = 0.20f;  // bem translúcido (sobrepõe pra dar haze)
    public static final float  DEFAULT_THICKNESS = 1.4f;   // puffs menores (não vira bola)
    public static final double DEFAULT_VIEW      = 40.0;

    // Limites (clamp no construtor)
    public static final float  MIN_OPACITY = 0.02f, MAX_OPACITY = 0.85f;
    public static final float  MIN_THICKNESS = 0.5f, MAX_THICKNESS = 6.0f;
    public static final double MIN_VIEW = 8.0, MAX_VIEW = 120.0;
    public static final float  MAX_DENSITY = 2.0f;

    public final int id;
    public final String dim;       // ex: "minecraft:overworld"
    public final double x, y, z;   // centro
    public final double radius;    // raio total (blocos)
    public final int color;        // 0xRRGGBB
    public final float density;       // 0..2  — quantidade de partículas
    public final float opacity;       // 0.02..0.85 — opacidade de cada puff
    public final float thickness;     // 0.5..6 — tamanho dos puffs (intensidade)
    public final double viewDistance; // 8..120 — distância de render
    public final boolean fromItem;    // criada pelo Incensário (item) vs comando

    public FogZone(int id, String dim, double x, double y, double z,
                   double radius, int color, float density,
                   float opacity, float thickness, double viewDistance,
                   boolean fromItem) {
        this.id = id;
        this.dim = dim;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = Math.max(1.0, radius);
        this.color = color & 0xFFFFFF;
        this.density = clampF(density, 0f, MAX_DENSITY);
        this.opacity = clampF(opacity, MIN_OPACITY, MAX_OPACITY);
        this.thickness = clampF(thickness, MIN_THICKNESS, MAX_THICKNESS);
        this.viewDistance = Math.max(MIN_VIEW, Math.min(MAX_VIEW, viewDistance));
        this.fromItem = fromItem;
    }

    private static float clampF(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public double distanceSqTo(double px, double py, double pz) {
        double dx = px - x, dy = py - y, dz = pz - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Cria uma cópia desta zona trocando UMA propriedade numérica (case-insensitive).
     * Aceita aliases PT/EN. {@code transparencia} é invertido (1 - valor) → opacidade.
     * Retorna {@code null} se a propriedade for desconhecida.
     */
    public FogZone withProperty(String prop, double value) {
        String p = prop.toLowerCase();
        double nDensity = this.density, nOpacity = this.opacity, nThickness = this.thickness;
        double nRadius = this.radius, nView = this.viewDistance;
        switch (p) {
            case "densidade": case "density":
                nDensity = value; break;
            case "opacidade": case "opacity": case "alpha":
                nOpacity = value; break;
            case "transparencia": case "transparency": case "transparente":
                nOpacity = 1.0 - value; break; // transparência alta = opacidade baixa
            case "intensidade": case "tamanho": case "size": case "thickness":
                nThickness = value; break;
            case "distancia": case "distance": case "alcance": case "view":
                nView = value; break;
            case "raio": case "radius":
                nRadius = value; break;
            default:
                return null; // propriedade desconhecida
        }
        return new FogZone(id, dim, x, y, z, nRadius, color,
                (float) nDensity, (float) nOpacity, (float) nThickness, nView, fromItem);
    }

    public FogZone withColor(int newColor) {
        return new FogZone(id, dim, x, y, z, radius, newColor,
                density, opacity, thickness, viewDistance, fromItem);
    }

    // ── NBT (persistência) ──────────────────────────────────────────────────
    public CompoundTag toNbt() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("dim", dim);
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("z", z);
        t.putDouble("r", radius);
        t.putInt("color", color);
        t.putFloat("density", density);
        t.putFloat("opacity", opacity);
        t.putFloat("thickness", thickness);
        t.putDouble("view", viewDistance);
        t.putBoolean("item", fromItem);
        return t;
    }

    public static FogZone fromNbt(CompoundTag t) {
        float opacity = t.contains("opacity") ? t.getFloat("opacity") : DEFAULT_OPACITY;
        float thickness = t.contains("thickness") ? t.getFloat("thickness") : DEFAULT_THICKNESS;
        double view = t.contains("view") ? t.getDouble("view") : DEFAULT_VIEW;
        return new FogZone(
                t.getInt("id"), t.getString("dim"),
                t.getDouble("x"), t.getDouble("y"), t.getDouble("z"),
                t.getDouble("r"), t.getInt("color"), t.getFloat("density"),
                opacity, thickness, view,
                t.getBoolean("item"));
    }

    // ── Rede (sincronização) ────────────────────────────────────────────────
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
        buf.writeUtf(dim);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(radius);
        buf.writeInt(color);
        buf.writeFloat(density);
        buf.writeFloat(opacity);
        buf.writeFloat(thickness);
        buf.writeDouble(viewDistance);
        buf.writeBoolean(fromItem);
    }

    public static FogZone read(FriendlyByteBuf buf) {
        return new FogZone(
                buf.readVarInt(), buf.readUtf(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readInt(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readDouble(),
                buf.readBoolean());
    }
}
