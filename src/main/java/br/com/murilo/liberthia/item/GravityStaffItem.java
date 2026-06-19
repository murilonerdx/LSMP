package br.com.murilo.liberthia.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * r194 — <b>Cajado Gravitacional</b> (do Núcleo da Constelação). Direito = onda de repulsão
 * (lança e fere mobs ao redor). SHIFT+direito = puxa mobs para perto.
 */
public class GravityStaffItem extends Item {
    public GravityStaffItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1).durability(512)); }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            boolean pull = player.isShiftKeyDown();
            AABB box = player.getBoundingBox().inflate(8);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
                Vec3 dir = pull ? player.position().subtract(e.position()) : e.position().subtract(player.position());
                if (dir.lengthSqr() < 1e-3) dir = new Vec3(0, 1, 0);
                dir = dir.normalize().scale(pull ? 1.4 : 2.2);
                e.setDeltaMovement(e.getDeltaMovement().add(dir.x, pull ? 0.2 : 0.8, dir.z));
                e.hurtMarked = true;
                if (!pull) e.hurt(player.damageSources().magic(), 6f);
            }
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY()+1, player.getZ(), 40, 1.5,1,1.5,0.2);
            sl.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, pull ? 1.6f : 0.8f);
            stack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(hand));
            player.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
