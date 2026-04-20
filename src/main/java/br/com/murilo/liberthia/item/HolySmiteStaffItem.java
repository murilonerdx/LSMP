package br.com.murilo.liberthia.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Holy Smite Staff — right-click to fire a holy beam that damages monsters in a line. */
public class HolySmiteStaffItem extends Item {
    public HolySmiteStaffItem(Properties p) { super(p.durability(128).rarity(Rarity.EPIC).fireResistant()); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = 18.0;
        Vec3 end = eye.add(look.scale(range));
        if (level instanceof ServerLevel sl) {
            // particle beam
            for (int i = 0; i < 40; i++) {
                double t = i / 40.0;
                Vec3 pt = eye.add(look.scale(range * t));
                sl.sendParticles(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            }
            AABB box = new AABB(eye, end).inflate(1.2);
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (le == player) continue;
                Vec3 rel = le.position().subtract(eye);
                double along = rel.dot(look);
                if (along < 0 || along > range) continue;
                float dmg = le instanceof Monster ? 18.0F : 8.0F;
                le.hurt(level.damageSources().magic(), dmg);
                sl.sendParticles(ParticleTypes.FLASH, le.getX(), le.getY() + 1, le.getZ(), 1, 0, 0, 0, 0);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.5F);
        }
        stack.hurtAndBreak(1, player, pl -> pl.broadcastBreakEvent(hand));
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.consume(stack);
    }
}
