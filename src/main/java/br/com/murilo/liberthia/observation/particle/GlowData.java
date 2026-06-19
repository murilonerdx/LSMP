package br.com.murilo.liberthia.observation.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * v0.1.22 r63: <b>GlowData</b> — ParticleOptions com cor RGB + size + alpha
 * + lifetime. Inspired by AN's ColorParticleTypeData.
 *
 * <h2>Pattern Minecraft 1.20.1</h2>
 * Implementa {@link ParticleOptions} com:
 * <ul>
 *   <li>{@code CODEC} pra JSON/disk serialization</li>
 *   <li>{@code DESERIALIZER} pra commands (/particle liberthia:glow ...)</li>
 *   <li>{@code writeToNetwork(FriendlyByteBuf)} pra network sync</li>
 * </ul>
 *
 * <p>Em 1.20.1 (pre-1.20.5) AINDA usa ParticleOptions.Deserializer (não StreamCodec).
 */
public class GlowData implements ParticleOptions {

    public final float r, g, b;
    public final float size;
    public final float alpha;
    public final int age;

    public GlowData(float r, float g, float b, float size, float alpha, int age) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.size = size;
        this.alpha = alpha;
        this.age = age;
    }

    public GlowData(int rgb, float size, float alpha, int age) {
        this(((rgb >> 16) & 0xFF) / 255F,
             ((rgb >> 8) & 0xFF) / 255F,
             (rgb & 0xFF) / 255F,
             size, alpha, age);
    }

    /** Codec — JSON/datapack serialization. */
    public static final Codec<GlowData> CODEC = RecordCodecBuilder.create(
        i -> i.group(
            Codec.FLOAT.fieldOf("r").forGetter(d -> d.r),
            Codec.FLOAT.fieldOf("g").forGetter(d -> d.g),
            Codec.FLOAT.fieldOf("b").forGetter(d -> d.b),
            Codec.FLOAT.fieldOf("size").forGetter(d -> d.size),
            Codec.FLOAT.fieldOf("alpha").forGetter(d -> d.alpha),
            Codec.INT.fieldOf("age").forGetter(d -> d.age)
        ).apply(i, GlowData::new));

    /** Deserializer pra commands (/particle). */
    public static final ParticleOptions.Deserializer<GlowData> DESERIALIZER =
        new ParticleOptions.Deserializer<>() {
            @Override
            public GlowData fromCommand(ParticleType<GlowData> type, StringReader reader)
                    throws CommandSyntaxException {
                reader.expect(' ');
                float r = reader.readFloat();
                reader.expect(' ');
                float g = reader.readFloat();
                reader.expect(' ');
                float b = reader.readFloat();
                reader.expect(' ');
                float size = reader.readFloat();
                reader.expect(' ');
                float alpha = reader.readFloat();
                reader.expect(' ');
                int age = reader.readInt();
                return new GlowData(r, g, b, size, alpha, age);
            }

            @Override
            public GlowData fromNetwork(ParticleType<GlowData> type, FriendlyByteBuf buf) {
                return new GlowData(buf.readFloat(), buf.readFloat(), buf.readFloat(),
                                    buf.readFloat(), buf.readFloat(), buf.readInt());
            }
        };

    @Override
    public ParticleType<?> getType() {
        return br.com.murilo.liberthia.registry.ModParticles.OBSERVATION_GLOW.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
        buf.writeFloat(size);
        buf.writeFloat(alpha);
        buf.writeInt(age);
    }

    @Override
    public String writeToString() {
        return String.format("%s %.2f %.2f %.2f %.2f %.2f %d",
            net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getKey(getType()),
            r, g, b, size, alpha, age);
    }
}
