package br.com.murilo.liberthia.magic.spells;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * v0.1.24 r96: <b>Spectral Hammer</b> — telegraphed AOE slam.
 *
 * <p>Right-click → marca posição (raytrace block hit, 30 blocos), spawna
 * <i>warning ring</i> de particles por 40 ticks, depois SLAM com dano
 * em raio 4. Inimigos no raio recebem 12 dano + knockback up.
 */
public class SpectralHammerItem extends Item {

    public SpectralHammerItem(Properties props) {
        super(props.stacksTo(1).durability(64));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (sp.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        HitResult hit = sp.pick(30, 0, false);
        Vec3 target = hit.getLocation();
        BlockPos pos = BlockPos.containing(target);
        ServerLevel sl = sp.serverLevel();

        // Warning ring — 40 ticks de particles
        for (int t = 0; t < 8; t++) {
            int delay = t * 5;
            final int radius = 4;
            sl.getServer().tell(new net.minecraft.server.TickTask(sl.getServer().getTickCount() + delay, () -> {
                for (int i = 0; i < 24; i++) {
                    double a = (i / 24.0) * Math.PI * 2;
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            target.x + Math.cos(a) * radius,
                            target.y + 0.1,
                            target.z + Math.sin(a) * radius,
                            1, 0, 0, 0, 0);
                }
            }));
        }

        // Slam após 40 ticks
        sl.getServer().tell(new net.minecraft.server.TickTask(sl.getServer().getTickCount() + 40, () -> {
            // Explosion-like dano + knockback
            var mobs = sl.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(4));
            for (LivingEntity e : mobs) {
                if (e == sp) continue;
                e.hurt(SchoolDamageSource.of(SpellSchool.HOLY).toVanilla(sl, sp), 12F);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 1.2, 0));
            }
            // Visual slam — particles burst
            for (int i = 0; i < 40; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = Math.random() * 4;
                sl.sendParticles(ParticleTypes.EXPLOSION,
                        target.x + Math.cos(a) * r,
                        target.y + 0.5,
                        target.z + Math.sin(a) * r,
                        1, 0.1, 0.1, 0.1, 0);
            }
            sl.playSound(null, pos,
                    net.minecraft.sounds.SoundEvents.ANVIL_LAND,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.6F);
        }));

        sp.getCooldowns().addCooldown(this, 80);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5Telegraphed slam AOE"));
        tooltip.add(Component.literal("§7Raio: §b4 §7blocos, §b40 §7ticks warning"));
        tooltip.add(Component.literal("§7Dano: §c12 §7+ knockback"));
    }
}
