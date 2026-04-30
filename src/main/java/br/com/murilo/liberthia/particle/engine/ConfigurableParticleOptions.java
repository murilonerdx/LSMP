package br.com.murilo.liberthia.particle.engine;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;

public class ConfigurableParticleOptions implements ParticleOptions {

    public static Codec<ConfigurableParticleOptions> codec(ParticleType<ConfigurableParticleOptions> type) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("red").forGetter(ConfigurableParticleOptions::red),
                Codec.FLOAT.fieldOf("green").forGetter(ConfigurableParticleOptions::green),
                Codec.FLOAT.fieldOf("blue").forGetter(ConfigurableParticleOptions::blue),
                Codec.FLOAT.fieldOf("alpha").forGetter(ConfigurableParticleOptions::alpha),
                Codec.FLOAT.fieldOf("startSize").forGetter(ConfigurableParticleOptions::startSize),
                Codec.FLOAT.fieldOf("endSize").forGetter(ConfigurableParticleOptions::endSize),
                Codec.INT.fieldOf("lifetime").forGetter(ConfigurableParticleOptions::lifetime),
                Codec.FLOAT.fieldOf("gravity").forGetter(ConfigurableParticleOptions::gravity),
                Codec.FLOAT.fieldOf("friction").forGetter(ConfigurableParticleOptions::friction),
                Codec.FLOAT.fieldOf("spinSpeed").forGetter(ConfigurableParticleOptions::spinSpeed),
                Codec.BOOL.fieldOf("hasPhysics").forGetter(ConfigurableParticleOptions::hasPhysics),
                Codec.BOOL.fieldOf("emissive").forGetter(ConfigurableParticleOptions::emissive),
                Codec.BOOL.optionalFieldOf("colorTint", true).forGetter(ConfigurableParticleOptions::colorTintEnabled)
        ).apply(instance, (
                red,
                green,
                blue,
                alpha,
                startSize,
                endSize,
                lifetime,
                gravity,
                friction,
                spinSpeed,
                hasPhysics,
                emissive,
                colorTint
        ) -> new ConfigurableParticleOptions(
                type,
                red,
                green,
                blue,
                alpha,
                startSize,
                endSize,
                lifetime,
                gravity,
                friction,
                spinSpeed,
                hasPhysics,
                emissive,
                colorTint
        )));
    }

    public static final Deserializer<ConfigurableParticleOptions> DESERIALIZER =
            new Deserializer<>() {
                @Override
                public ConfigurableParticleOptions fromCommand(
                        ParticleType<ConfigurableParticleOptions> type,
                        StringReader reader
                ) {
                    try {
                        reader.expect(' ');
                        float red = reader.readFloat();

                        reader.expect(' ');
                        float green = reader.readFloat();

                        reader.expect(' ');
                        float blue = reader.readFloat();

                        reader.expect(' ');
                        float alpha = reader.readFloat();

                        reader.expect(' ');
                        float startSize = reader.readFloat();

                        reader.expect(' ');
                        float endSize = reader.readFloat();

                        reader.expect(' ');
                        int lifetime = reader.readInt();

                        reader.expect(' ');
                        float gravity = reader.readFloat();

                        reader.expect(' ');
                        float friction = reader.readFloat();

                        reader.expect(' ');
                        float spinSpeed = reader.readFloat();

                        reader.expect(' ');
                        boolean hasPhysics = reader.readBoolean();

                        reader.expect(' ');
                        boolean emissive = reader.readBoolean();

                        boolean colorTint = true;

                        if (reader.canRead()) {
                            reader.expect(' ');
                            colorTint = reader.readBoolean();
                        }

                        return new ConfigurableParticleOptions(
                                type,
                                red,
                                green,
                                blue,
                                alpha,
                                startSize,
                                endSize,
                                lifetime,
                                gravity,
                                friction,
                                spinSpeed,
                                hasPhysics,
                                emissive,
                                colorTint
                        );
                    } catch (Exception exception) {
                        return new ConfigurableParticleOptions(
                                type,
                                1.0F,
                                1.0F,
                                1.0F,
                                1.0F,
                                0.15F,
                                0.45F,
                                24,
                                0.0F,
                                0.90F,
                                0.1F,
                                true,
                                true,
                                false
                        );
                    }
                }

                @Override
                public ConfigurableParticleOptions fromNetwork(
                        ParticleType<ConfigurableParticleOptions> type,
                        FriendlyByteBuf buffer
                ) {
                    return new ConfigurableParticleOptions(
                            type,
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readInt(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readFloat(),
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readBoolean()
                    );
                }
            };

    private final ParticleType<ConfigurableParticleOptions> type;

    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    private final float startSize;
    private final float endSize;
    private final int lifetime;
    private final float gravity;
    private final float friction;
    private final float spinSpeed;
    private final boolean hasPhysics;
    private final boolean emissive;
    private final boolean colorTintEnabled;

    /**
     * Construtor antigo mantido por compatibilidade.
     * Se algum código antigo criar ConfigurableParticleOptions direto,
     * ele mantém o comportamento antigo: aplica tint.
     */
    public ConfigurableParticleOptions(
            ParticleType<ConfigurableParticleOptions> type,
            float red,
            float green,
            float blue,
            float alpha,
            float startSize,
            float endSize,
            int lifetime,
            float gravity,
            float friction,
            float spinSpeed,
            boolean hasPhysics,
            boolean emissive
    ) {
        this(
                type,
                red,
                green,
                blue,
                alpha,
                startSize,
                endSize,
                lifetime,
                gravity,
                friction,
                spinSpeed,
                hasPhysics,
                emissive,
                true
        );
    }

    public ConfigurableParticleOptions(
            ParticleType<ConfigurableParticleOptions> type,
            float red,
            float green,
            float blue,
            float alpha,
            float startSize,
            float endSize,
            int lifetime,
            float gravity,
            float friction,
            float spinSpeed,
            boolean hasPhysics,
            boolean emissive,
            boolean colorTintEnabled
    ) {
        this.type = type;

        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;

        this.startSize = Math.max(0.01F, startSize);
        this.endSize = Math.max(0.01F, endSize);
        this.lifetime = Math.max(1, lifetime);

        this.gravity = gravity;
        this.friction = friction;
        this.spinSpeed = spinSpeed;

        this.hasPhysics = hasPhysics;
        this.emissive = emissive;
        this.colorTintEnabled = colorTintEnabled;
    }

    @Override
    public ParticleType<?> getType() {
        return this.type;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.red);
        buffer.writeFloat(this.green);
        buffer.writeFloat(this.blue);
        buffer.writeFloat(this.alpha);
        buffer.writeFloat(this.startSize);
        buffer.writeFloat(this.endSize);
        buffer.writeInt(this.lifetime);
        buffer.writeFloat(this.gravity);
        buffer.writeFloat(this.friction);
        buffer.writeFloat(this.spinSpeed);
        buffer.writeBoolean(this.hasPhysics);
        buffer.writeBoolean(this.emissive);
        buffer.writeBoolean(this.colorTintEnabled);
    }

    @Override
    public String writeToString() {
        return String.format(
                Locale.ROOT,
                "%s %.3f %.3f %.3f %.3f %.3f %.3f %d %.3f %.3f %.3f %s %s %s",
                this.type,
                this.red,
                this.green,
                this.blue,
                this.alpha,
                this.startSize,
                this.endSize,
                this.lifetime,
                this.gravity,
                this.friction,
                this.spinSpeed,
                this.hasPhysics,
                this.emissive,
                this.colorTintEnabled
        );
    }

    public float red() {
        return this.red;
    }

    public float green() {
        return this.green;
    }

    public float blue() {
        return this.blue;
    }

    public float alpha() {
        return this.alpha;
    }

    public float startSize() {
        return this.startSize;
    }

    public float endSize() {
        return this.endSize;
    }

    public int lifetime() {
        return this.lifetime;
    }

    public float gravity() {
        return this.gravity;
    }

    public float friction() {
        return this.friction;
    }

    public float spinSpeed() {
        return this.spinSpeed;
    }

    public boolean hasPhysics() {
        return this.hasPhysics;
    }

    public boolean emissive() {
        return this.emissive;
    }

    public boolean colorTintEnabled() {
        return this.colorTintEnabled;
    }

    public boolean hasColorTint() {
        return this.colorTintEnabled;
    }
}