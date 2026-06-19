package br.com.murilo.liberthia.observation.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

/**
 * v0.1.22 r64: <b>LineData</b> — ParticleOptions com start/dest interpolação.
 *
 * <p>Pattern AN's {@code ColoredDynamicTypeData}. A particle lerps de
 * (initX,initY,initZ) → (destX,destY,destZ) durante seu lifetime.
 */
public class LineData implements ParticleOptions {

    public final float r, g, b;
    public final float scale;
    public final int age;
    public final float destX, destY, destZ;

    public LineData(float r, float g, float b, float scale, int age,
                     float destX, float destY, float destZ) {
        this.r = r; this.g = g; this.b = b;
        this.scale = scale; this.age = age;
        this.destX = destX; this.destY = destY; this.destZ = destZ;
    }

    public static LineData rgb(int rgb, float scale, int age, double dx, double dy, double dz) {
        return new LineData(
            ((rgb >> 16) & 0xFF) / 255F,
            ((rgb >> 8) & 0xFF) / 255F,
            (rgb & 0xFF) / 255F,
            scale, age, (float)dx, (float)dy, (float)dz);
    }

    public static final Codec<LineData> CODEC = RecordCodecBuilder.create(
        i -> i.group(
            Codec.FLOAT.fieldOf("r").forGetter(d -> d.r),
            Codec.FLOAT.fieldOf("g").forGetter(d -> d.g),
            Codec.FLOAT.fieldOf("b").forGetter(d -> d.b),
            Codec.FLOAT.fieldOf("scale").forGetter(d -> d.scale),
            Codec.INT.fieldOf("age").forGetter(d -> d.age),
            Codec.FLOAT.fieldOf("dx").forGetter(d -> d.destX),
            Codec.FLOAT.fieldOf("dy").forGetter(d -> d.destY),
            Codec.FLOAT.fieldOf("dz").forGetter(d -> d.destZ)
        ).apply(i, LineData::new));

    public static final ParticleOptions.Deserializer<LineData> DESERIALIZER =
        new ParticleOptions.Deserializer<>() {
            @Override
            public LineData fromCommand(ParticleType<LineData> type, StringReader r) throws CommandSyntaxException {
                r.expect(' '); float red = r.readFloat();
                r.expect(' '); float grn = r.readFloat();
                r.expect(' '); float blu = r.readFloat();
                r.expect(' '); float sc = r.readFloat();
                r.expect(' '); int ag = r.readInt();
                r.expect(' '); float dx = r.readFloat();
                r.expect(' '); float dy = r.readFloat();
                r.expect(' '); float dz = r.readFloat();
                return new LineData(red, grn, blu, sc, ag, dx, dy, dz);
            }
            @Override
            public LineData fromNetwork(ParticleType<LineData> type, FriendlyByteBuf buf) {
                return new LineData(
                    buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readInt(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat());
            }
        };

    @Override
    public ParticleType<?> getType() {
        return br.com.murilo.liberthia.registry.ModParticles.OBSERVATION_LINE.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeFloat(r); buf.writeFloat(g); buf.writeFloat(b);
        buf.writeFloat(scale); buf.writeInt(age);
        buf.writeFloat(destX); buf.writeFloat(destY); buf.writeFloat(destZ);
    }

    @Override
    public String writeToString() {
        return String.format("%s %.2f %.2f %.2f %.2f %d %.2f %.2f %.2f",
            net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getKey(getType()),
            r, g, b, scale, age, destX, destY, destZ);
    }
}
