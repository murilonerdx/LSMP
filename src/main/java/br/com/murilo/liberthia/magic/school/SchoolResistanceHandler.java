package br.com.murilo.liberthia.magic.school;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.Map;

/**
 * v0.1.24 r82: <b>SchoolResistanceHandler</b> — aplica modificadores baseados
 * em escola da magia ao receber dano.
 *
 * <p>Fluxo:
 * <ol>
 *   <li>{@link LivingHurtEvent} dispara em {@code HIGHEST} priority</li>
 *   <li>Verifica se o DamageSource tem entry no {@link SchoolDamageRegistry}</li>
 *   <li>Lê resistance do alvo via {@link #computeResistance}</li>
 *   <li>Aplica multiplier ao damage</li>
 *   <li>Aplica side effects (burn/freeze/lifesteal)</li>
 * </ol>
 *
 * <p>Resistance source: NBT do player (futuro: armor perks + Curios bonus).
 * Por enquanto, valor base = 0 (multiplier 1.0).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SchoolResistanceHandler {

    public static final String NBT_RESIST_PREFIX = "liberthia.school_resist.";

    private SchoolResistanceHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event) {
        var source = event.getSource();
        SchoolDamageSource meta = SchoolDamageRegistry.get(source);
        if (meta == null) return; // dano comum, deixa passar

        LivingEntity target = event.getEntity();
        float baseDamage = event.getAmount();

        // (1) Aplica resistance
        float resistance = computeResistance(target, meta.school);
        float multiplier = SpellSchool.computeMultiplier(resistance);
        float modified = baseDamage * multiplier;

        // (2) Bonus de escola oposta (5%)
        float oppositeResist = computeResistance(target, meta.school.opposite());
        if (oppositeResist < 0) {
            modified *= 1.05F; // vulnerável à oposta = bonus pequeno
        }

        event.setAmount(Math.max(0, modified));

        // (3) Burn ticks (Fire)
        if (meta.burnTicks > 0) {
            target.setRemainingFireTicks(target.getRemainingFireTicks() + meta.burnTicks);
        }

        // (4) Freeze ticks (Ice)
        if (meta.freezeTicks > 0) {
            target.setTicksFrozen(target.getTicksFrozen() + meta.freezeTicks);
        }

        // (5) Lifesteal (Blood)
        if (meta.lifestealPct > 0 && source.getEntity() instanceof LivingEntity attacker) {
            float heal = modified * meta.lifestealPct;
            attacker.heal(heal);
        }

        // (6) Cleanup
        SchoolDamageRegistry.remove(source);
    }

    /**
     * Calcula resistance do entity contra uma school específica.
     *
     * <p>Lê {@code liberthia.school_resist.<SCHOOL>} do persistentData NBT.
     * Range: -100 (vulnerável) a +100 (imune).
     *
     * <p>Future: somar resistance de Curios/armor/perks aqui.
     */
    public static float computeResistance(LivingEntity entity, SpellSchool school) {
        var data = entity.getPersistentData();
        String key = NBT_RESIST_PREFIX + school.name();
        if (data.contains(key)) {
            return data.getFloat(key);
        }
        return 0.0F;
    }

    /** Set resistance NBT (usado por Curios/armor/perks). */
    public static void setResistance(LivingEntity entity, SpellSchool school, float value) {
        String key = NBT_RESIST_PREFIX + school.name();
        entity.getPersistentData().putFloat(key,
                Math.max(-100.0F, Math.min(100.0F, value)));
    }

    public static void addResistance(LivingEntity entity, SpellSchool school, float delta) {
        float current = computeResistance(entity, school);
        setResistance(entity, school, current + delta);
    }
}
