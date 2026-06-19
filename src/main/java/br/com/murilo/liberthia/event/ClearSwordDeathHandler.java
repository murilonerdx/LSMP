package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.matter.MatterProfile;
import br.com.murilo.liberthia.matter.MatterProfileProvider;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Quando um player morre segurando a Clear Matter Sword (main hand ou off hand),
 * dispara uma "explosão" de matéria clara num raio de 6 blocos:
 *
 * <ul>
 *   <li>Players próximos: adiciona +10% do MAX (100) em white matter no profile.</li>
 *   <li>Mobs sem profile: Weakness III por 10s.</li>
 *   <li>Particles SOUL_FIRE_FLAME + GLOW.</li>
 *   <li>Som ALLAY_AMBIENT_WITH_ITEM.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ClearSwordDeathHandler {

    private static final double RADIUS = 6.0;
    private static final float WHITE_GAIN = 10f; // 10% of max (100)
    private static final int MOB_WEAKNESS_DURATION = 200; // 10s
    private static final int MOB_WEAKNESS_AMP = 2; // III

    private ClearSwordDeathHandler() {}

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        ItemStack main = sp.getMainHandItem();
        ItemStack off = sp.getOffhandItem();
        boolean has = (main.is(ModItems.CLEAR_MATTER_SWORD.get())
                || off.is(ModItems.CLEAR_MATTER_SWORD.get()));
        if (!has) return;
        if (!(sp.level() instanceof ServerLevel sl)) return;

        // FX
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                sp.getX(), sp.getY() + 1.0, sp.getZ(),
                60, 1.5, 1.0, 1.5, 0.1);
        sl.sendParticles(ParticleTypes.GLOW,
                sp.getX(), sp.getY() + 1.0, sp.getZ(),
                40, 1.0, 1.0, 1.0, 0.05);
        sl.playSound(null, sp.blockPosition(),
                SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.PLAYERS, 1.5F, 0.9F);

        AABB area = sp.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> nearby = sl.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != sp && e.isAlive());

        for (LivingEntity ent : nearby) {
            if (ent instanceof Player other) {
                other.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
                    profile.addWhite(WHITE_GAIN);
                });
                if (other instanceof ServerPlayer osp) {
                    br.com.murilo.liberthia.matter.MatterProfileEvents.syncTo(osp);
                }
            } else {
                // Mob — sem profile, aplica Weakness III por 10s
                ent.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                        MOB_WEAKNESS_DURATION, MOB_WEAKNESS_AMP, false, true));
            }
        }
    }
}
