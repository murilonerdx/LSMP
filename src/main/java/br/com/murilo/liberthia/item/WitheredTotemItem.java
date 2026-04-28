package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.BloodKin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Withered Totem — passive aura while held in inventory.
 * - At HP &lt; 30%: nearby non-kin enemies (5 blocks) get Wither II; holder gets
 *   Resistance II + Regeneration I. Pulses every 60 ticks.
 */
public class WitheredTotemItem extends Item {
    public WitheredTotemItem(Properties props) { super(props); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;
        if (player.tickCount % 60 != 0) return;
        float hpRatio = player.getHealth() / player.getMaxHealth();
        if (hpRatio >= 0.30F) return;

        ServerLevel sl = (ServerLevel) level;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));

        AABB box = new AABB(player.blockPosition()).inflate(5.0);
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == player) continue;
            if (BloodKin.is(le)) continue;
            if (le instanceof Player) continue;
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
        }

        sl.sendParticles(ParticleTypes.SOUL,
                player.getX(), player.getY() + 0.4, player.getZ(),
                10, 1.5, 0.6, 1.5, 0.02);
    }
}
