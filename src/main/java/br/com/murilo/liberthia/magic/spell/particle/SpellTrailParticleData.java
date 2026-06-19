package br.com.murilo.liberthia.magic.spell.particle;

import br.com.murilo.liberthia.magic.school.SpellSchool;
import br.com.murilo.liberthia.registry.ModParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;

/**
 * v0.1.145 r113: <b>SpellTrailParticleData</b> — payload de partícula que
 * carrega a escola + scale animado.
 *
 * <p>Original code — não derivado de outros mods. Padrão {@link ParticleOptions}
 * vanilla MC, codec via mojang serialization.
 *
 * <h2>Fields</h2>
 * <ul>
 *   <li>{@code schoolIndex} — ordinal da {@link SpellSchool} (0-6). Determina
 *       qual spritesheet vai usar no client (spell_trail_fire, spell_trail_ice...)</li>
 *   <li>{@code scale} — 0.0–2.0, tamanho inicial da partícula</li>
 *   <li>{@code lifeTicks} — duração total em ticks</li>
 * </ul>
 */
public class SpellTrailParticleData implements ParticleOptions {

    public static final Codec<SpellTrailParticleData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("school").forGetter(d -> d.schoolIndex),
            Codec.FLOAT.fieldOf("scale").forGetter(d -> d.scale),
            Codec.INT.fieldOf("life").forGetter(d -> d.lifeTicks)
    ).apply(i, SpellTrailParticleData::new));

    public static final ParticleOptions.Deserializer<SpellTrailParticleData> DESERIALIZER =
            new ParticleOptions.Deserializer<>() {
                @Override
                public SpellTrailParticleData fromCommand(
                        ParticleType<SpellTrailParticleData> type, StringReader r) throws CommandSyntaxException {
                    r.expect(' '); int s = r.readInt();
                    r.expect(' '); float sc = r.readFloat();
                    r.expect(' '); int life = r.readInt();
                    return new SpellTrailParticleData(s, sc, life);
                }
                @Override
                public SpellTrailParticleData fromNetwork(
                        ParticleType<SpellTrailParticleData> type, FriendlyByteBuf buf) {
                    return new SpellTrailParticleData(buf.readInt(), buf.readFloat(), buf.readInt());
                }
            };

    public final int schoolIndex;
    public final float scale;
    public final int lifeTicks;

    public SpellTrailParticleData(int schoolIndex, float scale, int lifeTicks) {
        this.schoolIndex = Math.max(0, Math.min(SpellSchool.values().length - 1, schoolIndex));
        this.scale = Math.max(0.05F, Math.min(3.0F, scale));
        this.lifeTicks = Math.max(2, Math.min(80, lifeTicks));
    }

    public SpellTrailParticleData(SpellSchool school, float scale, int lifeTicks) {
        this(school.ordinal(), scale, lifeTicks);
    }

    public SpellSchool school() {
        return SpellSchool.values()[schoolIndex];
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.SPELL_TRAIL.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(schoolIndex);
        buf.writeFloat(scale);
        buf.writeInt(lifeTicks);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %d %.3f %d",
                getType(), schoolIndex, scale, lifeTicks);
    }
}
