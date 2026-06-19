package br.com.murilo.liberthia.magic.spells;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * v0.1.24 r108: <b>Heat Surge</b> — cone fire AOE pra frente do player.
 *
 * <p>30° cone, raio 8 blocos. Damage 6 + burn 100t pra cada inimigo no cone.
 * Knockback fraco backward.
 */
public class HeatSurgeSpellItem extends Item {

    public HeatSurgeSpellItem(Properties props) {
        super(props.stacksTo(1).durability(60));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        ServerLevel sl = sp.serverLevel();
        Vec3 origin = sp.position().add(0, 1.5, 0);
        Vec3 look = sp.getLookAngle();
        double range = 8;

        // Cone particles + damage
        int hits = 0;
        var entities = sl.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(range));
        for (LivingEntity e : entities) {
            if (e == sp) continue;
            Vec3 toEntity = e.position().add(0, e.getBbHeight() / 2, 0).subtract(origin);
            double dist = toEntity.length();
            if (dist > range) continue;
            Vec3 normalized = toEntity.normalize();
            double dot = normalized.dot(look);
            // 30° cone = cos(15) ≈ 0.966
            if (dot < 0.85) continue;

            e.setRemainingFireTicks(e.getRemainingFireTicks() + 100);
            e.hurt(SchoolDamageSource.fire(100).toVanilla(sl, sp), 6F);
            // Knockback
            Vec3 push = look.scale(0.5);
            e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.3, push.z));
            hits++;
        }

        // Cone visual — particles em arco
        for (double r = 0.5; r <= range; r += 0.5) {
            for (int j = -2; j <= 2; j++) {
                double a = Math.toRadians(j * 6);
                Vec3 dir = rotateAroundY(look, a);
                sl.sendParticles(ParticleTypes.FLAME,
                        origin.x + dir.x * r,
                        origin.y + dir.y * r,
                        origin.z + dir.z * r,
                        1, 0.05, 0.05, 0.05, 0.02);
            }
        }
        sl.playSound(null, sp.blockPosition(),
                net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,
                net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 0.7F);

        sp.getCooldowns().addCooldown(this, 80);
        stack.hurtAndBreak(2, sp, p -> p.broadcastBreakEvent(hand));
        sp.displayClientMessage(Component.literal(
                "§c§l✦ Heat Surge §7- " + hits + " hits"), true);
        return InteractionResultHolder.success(stack);
    }

    private Vec3 rotateAroundY(Vec3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§cCone fire 30° forward"));
        tooltip.add(Component.literal("§7Raio §b8 §7+ §c6 §7damage + §c100t §7burn"));
    }
}
