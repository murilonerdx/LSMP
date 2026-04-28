package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entity.ai.HuntLowHpGoal;
import br.com.murilo.liberthia.entity.ai.RandomMagicCastGoal;
import br.com.murilo.liberthia.logic.BloodKin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * On spawn, injects "smart evil" goals into Blood-kin mobs so they:
 *   • prioritise the lowest-HP nearby player ({@link HuntLowHpGoal});
 *   • randomly cast debuffs at their target every 5–10 seconds
 *     ({@link RandomMagicCastGoal}).
 *
 * Skips Wolves (BloodHound) since they have their own pack AI and adding
 * spell-cast on top of melee charges feels off.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class EvilAiInjector {
    private EvilAiInjector() {}

    private static final String NBT_FLAG = "liberthia_smart_ai_injected";

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent ev) {
        Entity e = ev.getEntity();
        if (!(e instanceof Mob mob)) return;
        if (mob.level().isClientSide) return;
        if (!BloodKin.is(mob)) return;
        if (mob instanceof Wolf) return; // hounds keep their pack AI
        // idempotency: don't double-inject on chunk reload.
        if (mob.getPersistentData().getBoolean(NBT_FLAG)) return;
        mob.getPersistentData().putBoolean(NBT_FLAG, true);

        // Hunting smart-target — priority 0 wins ties.
        mob.targetSelector.addGoal(0, new HuntLowHpGoal(mob, 24.0));
        // Random magic cast — 100..200 ticks (5-10s) cooldown.
        mob.goalSelector.addGoal(2, new RandomMagicCastGoal(mob, 100, 200, 16.0));
    }
}
