package br.com.murilo.liberthia.fog.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * <b>FogMistData</b> — ParticleOptions da névoa: cor RGB + size + alpha + lifetime.
 *
 * <p>Mesmo shape do {@code GlowData}, MAS a partícula correspondente
 * ({@link FogMistParticle}) renderiza com blending <b>translúcido normal</b>
 * (não additive). Isso faz a névoa parecer fumaça de verdade — inclusive cores
 * escuras (ex.: névoa quase preta) aparecem como um véu escuro, ao contrário do
 * additive que some com cores escuras.
 *
 * <p>1.20.1 (pre-1.20.5) usa {@link ParticleOptions.Deserializer} (não StreamCodec).
 */
public class FogMistData implements ParticleOptions {

    public final float r, g, b;
    public final float size;
    public final float alpha;
    public final int age;

    public FogMistData(float r, float g, float b, float size, float alpha, int age) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.size = size;
        this.alpha = alpha;
        this.age = age;
    }

    public FogMistData(int rgb, float size, float alpha, int age) {
        this(((rgb >> 16) & 0xFF) / 255F,
             ((rgb >> 8) & 0xFF) / 255F,
             (rgb & 0xFF) / 255F,
             size, alpha, age);
    }

    public static final Codec<FogMistData> CODEC = RecordCodecBuilder.create(
        i -> i.group(
            Codec.FLOAT.fieldOf("r").forGetter(d -> d.r),
            Codec.FLOAT.fieldOf("g").forGetter(d -> d.g),
            Codec.FLOAT.fieldOf("b").forGetter(d -> d.b),
            Codec.FLOAT.fieldOf("size").forGetter(d -> d.size),
            Codec.FLOAT.fieldOf("alpha").forGetter(d -> d.alpha),
            Codec.INT.fieldOf("age").forGetter(d -> d.age)
        ).apply(i, FogMistData::new));

    public static final ParticleOptions.Deserializer<FogMistData> DESERIALIZER =
        new ParticleOptions.Deserializer<>() {
            @Override
            public FogMistData fromCommand(ParticleType<FogMistData> type, StringReader reader)
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
                return new FogMistData(r, g, b, size, alpha, age);
            }

            @Override
            public FogMistData fromNetwork(ParticleType<FogMistData> type, FriendlyByteBuf buf) {
                return new FogMistData(buf.readFloat(), buf.readFloat(), buf.readFloat(),
                                       buf.readFloat(), buf.readFloat(), buf.readInt());
            }
        };

    @Override
    public ParticleType<?> getType() {
        return br.com.murilo.liberthia.registry.ModParticles.FOG_MIST.get();
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
