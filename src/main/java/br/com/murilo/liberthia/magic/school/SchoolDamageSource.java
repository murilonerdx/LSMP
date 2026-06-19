package br.com.murilo.liberthia.magic.school;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * v0.1.24 r82: <b>SchoolDamageSource</b> — DamageSource estendido que carrega
 * metadata da escola de magia.
 *
 * <p>Em vez de usar DamageSource.magic() flat, cada spell agora cria um
 * SchoolDamageSource com:
 * <ul>
 *   <li>{@code school} — qual escola (Fire/Ice/etc)</li>
 *   <li>{@code lifestealPct} — % do dano que volta como cura (Blood)</li>
 *   <li>{@code burnTicks} — ticks de fogo após hit (Fire)</li>
 *   <li>{@code freezeTicks} — ticks de freeze (Ice)</li>
 *   <li>{@code ignoreArmor} — true pra ignorar armadura física</li>
 * </ul>
 *
 * <p>A logica de aplicação roda no {@link SchoolResistanceHandler} (LivingHurtEvent).
 */
public final class SchoolDamageSource {

    public final SpellSchool school;
    public final float lifestealPct;
    public final int burnTicks;
    public final int freezeTicks;
    public final boolean ignoreArmor;

    public SchoolDamageSource(SpellSchool school, float lifestealPct, int burnTicks,
                              int freezeTicks, boolean ignoreArmor) {
        this.school = school;
        this.lifestealPct = lifestealPct;
        this.burnTicks = burnTicks;
        this.freezeTicks = freezeTicks;
        this.ignoreArmor = ignoreArmor;
    }

    /** Builder simples — só school. */
    public static SchoolDamageSource of(SpellSchool school) {
        return new SchoolDamageSource(school, 0, 0, 0, false);
    }

    public static SchoolDamageSource fire(int burnTicks) {
        return new SchoolDamageSource(SpellSchool.FIRE, 0, burnTicks, 0, false);
    }

    public static SchoolDamageSource ice(int freezeTicks) {
        return new SchoolDamageSource(SpellSchool.ICE, 0, 0, freezeTicks, false);
    }

    public static SchoolDamageSource blood(float lifesteal) {
        return new SchoolDamageSource(SpellSchool.BLOOD, lifesteal, 0, 0, false);
    }

    public static SchoolDamageSource eldritch() {
        return new SchoolDamageSource(SpellSchool.ELDRITCH, 0, 0, 0, true);
    }

    /** Cria um DamageSource vanilla com a tag MAGIC, mas anexa este meta no map global. */
    public DamageSource toVanilla(Level level, Entity attacker) {
        ResourceKey<DamageType> typeKey = switch (school) {
            case FIRE -> DamageTypes.IN_FIRE;
            case ICE -> DamageTypes.FREEZE;
            case LIGHTNING -> DamageTypes.LIGHTNING_BOLT;
            case BLOOD, ELDRITCH -> DamageTypes.MAGIC;
            case HOLY -> DamageTypes.MAGIC;
            case NATURE -> DamageTypes.MAGIC;
        };
        Holder<DamageType> type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(typeKey);
        DamageSource ds = new DamageSource(type, attacker);
        SchoolDamageRegistry.register(ds, this);
        return ds;
    }
}
