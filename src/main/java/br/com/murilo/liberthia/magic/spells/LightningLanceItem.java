package br.com.murilo.liberthia.magic.spells;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * v0.1.24 r96: <b>Lightning Lance</b> — chain-target spell. Hit primário +
 * jumps até 4 targets adjacentes (raio 4 cada chain).
 */
public class LightningLanceItem extends Item {

    public LightningLanceItem(Properties props) {
        super(props.stacksTo(1).durability(80));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        // r164 FIX: usar entity raycast real (sp.pick só pega bloco, nunca entidade)
        LivingEntity initial = br.com.murilo.liberthia.util.EntityRaycast.pickLiving(sp, 20.0);
        if (initial == null) {
            sp.displayClientMessage(Component.literal("§7§o✦ Aponte pra um alvo"), true);
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel sl = sp.serverLevel();
        Set<LivingEntity> hitSet = new HashSet<>();
        chainTo(sl, sp, initial, hitSet, 4);

        sp.getCooldowns().addCooldown(this, 40);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.success(stack);
    }

    private void chainTo(ServerLevel sl, ServerPlayer caster, LivingEntity target,
                          Set<LivingEntity> hit, int remaining) {
        if (remaining <= 0 || hit.contains(target)) return;
        hit.add(target);

        // Damage
        target.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(sl, caster),
                6F + remaining);

        // Visual chain particles
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                20, 0.4, 0.4, 0.4, 0.1);

        // Find next target
        var nearby = sl.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(4));
        for (LivingEntity n : nearby) {
            if (hit.contains(n)) continue;
            if (n == caster) continue;
            chainTo(sl, caster, n, hit, remaining - 1);
            break;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§eChain Lightning — até §b4 §etargets"));
        tooltip.add(Component.literal("§7Dano §c6-10 §7por chain"));
    }
}
