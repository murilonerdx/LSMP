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
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * v0.1.24 r96: <b>Devour Jaw</b> — DOT lifesteal. Aponta num alvo, latched
 * por 5s, drena 2HP/s e cura caster.
 */
public class DevourJawItem extends Item {

    public DevourJawItem(Properties props) {
        super(props.stacksTo(1).durability(40));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        // FIX: Entity.pick() só bate em BLOCOS — usa raycast de entidade de verdade.
        LivingEntity target = br.com.murilo.liberthia.util.EntityRaycast.pickLiving(sp, 20.0);
        if (target == null) {
            sp.displayClientMessage(Component.literal("§8Mire numa criatura pra latchear."), true);
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel sl = sp.serverLevel();
        // Latched DOT — 5 pulses a cada 20t
        for (int i = 1; i <= 5; i++) {
            final int delay = i * 20;
            sl.getServer().tell(new net.minecraft.server.TickTask(sl.getServer().getTickCount() + delay, () -> {
                if (!target.isAlive() || sp.distanceTo(target) > 24) return;
                target.hurt(SchoolDamageSource.blood(1.0F).toVanilla(sl, sp), 2F);
                // 100% lifesteal — cura o caster pelo HP drenado (faltava!)
                if (sp.isAlive()) {
                    sp.heal(2.0F);
                    sl.sendParticles(ParticleTypes.HEART, sp.getX(), sp.getY() + 1.6, sp.getZ(),
                            2, 0.2, 0.2, 0.2, 0.0);
                }
                // Particles "drenagem" do alvo até caster
                for (int j = 0; j < 5; j++) {
                    double f = j / 5.0;
                    sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                            target.getX() + (sp.getX() - target.getX()) * f,
                            target.getY() + 1 + (sp.getY() - target.getY()) * f,
                            target.getZ() + (sp.getZ() - target.getZ()) * f,
                            1, 0.05, 0.05, 0.05, 0);
                }
            }));
        }

        sp.getCooldowns().addCooldown(this, 200);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        sp.displayClientMessage(Component.literal("§4§l✦ §r§4Mandíbula latcheada"), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§4Latches no alvo por §b5s"));
        tooltip.add(Component.literal("§4Drena §c2HP/s §4+ 100% lifesteal"));
    }
}
