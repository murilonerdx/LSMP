package br.com.murilo.liberthia.matter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Perfil de matéria — escura (DM), branca (WM), amarela (YM) — em escala 0..100.
 *
 * <p>Anexado a cada {@link net.minecraft.world.entity.player.Player} via
 * Forge Capabilities. Persistido em NBT, sincronizado pro cliente sempre que
 * mudar (via packet S2C).
 *
 * <p>O perfil ATIVO ({@link MatterProfileType}) é derivado pelos valores
 * conforme as combinações da lore — ver {@link MatterProfileType#detect}.
 */
@AutoRegisterCapability
public class MatterProfile implements INBTSerializable<CompoundTag> {

    public static final float MAX = 100f;

    private float darkMatter;
    private float whiteMatter;
    private float yellowMatter;
    /** Cache do perfil ativo (recomputado em cada add/clamp). */
    private MatterProfileType cachedType = MatterProfileType.NONE;

    public float getDark()   { return darkMatter; }
    public float getWhite()  { return whiteMatter; }
    public float getYellow() { return yellowMatter; }

    public void setDark(float v)   { darkMatter   = clamp(v); recompute(); }
    public void setWhite(float v)  { whiteMatter  = clamp(v); recompute(); }
    public void setYellow(float v) { yellowMatter = clamp(v); recompute(); }

    public void addDark(float v)   { setDark(darkMatter + v); }
    public void addYellow(float v) { setYellow(yellowMatter + v); }

    /**
     * Adiciona White Matter — e, se o delta é positivo, EXPELE proporcionalmente
     * Dark e Yellow do perfil.
     *
     * <p><b>Lore</b>: Matéria Clara é purificadora — quando o portador absorve
     * mais WM (proximidade de bloco/fluido, injetor, cura), ela "limpa" as
     * outras duas matérias na mesma taxa. É a justificativa mecânica do bug #8
     * reportado pelo the_developer: <i>"subir WM no perfil deveria diminuir
     * DM/YM, mas atualmente apenas adiciona WM"</i>.
     *
     * <p><b>Taxa</b>: cada +1 WM expele -0.5 DM e -0.5 YM. A taxa < 1 é
     * intencional — se fosse 1:1, qualquer salpicada de WM zeraria o perfil
     * inteiro do player; com 0.5x o WM precisa SUBIR pra valer pra empurrar
     * as outras matérias pra baixo. Player com 100 DM + 100 YM precisa
     * absorver +200 WM (que já satura em 100 antes disso) pra zerar tudo.
     *
     * <p><b>Deltas negativos</b> (i.e. {@code v < 0}, removendo WM) NÃO disparam
     * expulsão — só ganhos positivos. Pílulas que zeram WM via {@link #setWhite}
     * direto também não disparam (setWhite não chama esse método).
     */
    public void addWhite(float v) {
        setWhite(whiteMatter + v);
        if (v > 0) {
            float expel = v * 0.5f;
            if (darkMatter > 0)   setDark(darkMatter - expel);
            if (yellowMatter > 0) setYellow(yellowMatter - expel);
        }
    }

    public MatterProfileType getActiveType() { return cachedType; }

    /** Decai cada um dos 3 valores em {@code amount} (positivo). */
    public void decayAll(float amount) {
        darkMatter   = clamp(darkMatter   - amount);
        whiteMatter  = clamp(whiteMatter  - amount);
        yellowMatter = clamp(yellowMatter - amount);
        recompute();
    }

    public void copyFrom(MatterProfile other) {
        this.darkMatter   = other.darkMatter;
        this.whiteMatter  = other.whiteMatter;
        this.yellowMatter = other.yellowMatter;
        this.cachedType   = other.cachedType;
    }

    private static float clamp(float v) { return Mth.clamp(v, 0f, MAX); }
    private void recompute() { cachedType = MatterProfileType.detect(darkMatter, whiteMatter, yellowMatter); }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("dm", darkMatter);
        tag.putFloat("wm", whiteMatter);
        tag.putFloat("ym", yellowMatter);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        darkMatter   = tag.getFloat("dm");
        whiteMatter  = tag.getFloat("wm");
        yellowMatter = tag.getFloat("ym");
        recompute();
    }
}
