package br.com.murilo.liberthia.magic.affinity;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.24 r93 / fix r179: lifesteal do Affinity Ring BLOODBORN.
 *
 * <p>Quando o player ataca uma entity e está usando o anel BLOODBORN
 * (inventário OU slot Curios), cura 10% do dano causado.
 *
 * <p>r179 FIX: antes dependia da NBT {@code bloodborn_active} setada pelo
 * {@code inventoryTick}, que não roda em slots Curios — então o anel equipado
 * não curava nada. Agora checa {@link AffinityRings#isWorn} direto no hit.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class BloodbornHandler {

    private BloodbornHandler() {}

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p)) return;
        if (p.level().isClientSide) return;
        if (!AffinityRings.isWorn(p, AffinityRingItem.Type.BLOODBORN)) return;

        float heal = event.getAmount() * 0.1F;
        if (heal <= 0F) return;
        p.heal(heal);

        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART,
                    p.getX(), p.getY() + 1.7, p.getZ(),
                    1, 0.25, 0.25, 0.25, 0.0);
        }
    }
}
