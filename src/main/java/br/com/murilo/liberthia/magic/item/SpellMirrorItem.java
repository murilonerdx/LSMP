package br.com.murilo.liberthia.magic.item;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * v0.1.160 r135: <b>Spell Mirror</b> — item que reflete magic damage de volta no atacante.
 *
 * <p>Quando equipado (presente no inventário do player), reduz dano mágico em 50%
 * e devolve esse valor pro atacante. Consome 1 durability por reflexo.
 */
public class SpellMirrorItem extends Item {

    public SpellMirrorItem(Properties props) {
        super(props.stacksTo(1).durability(100));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5✦ Espelho Mágico ✦"));
        tooltip.add(Component.literal("§7Reflete §a50%§7 de dano mágico"));
        tooltip.add(Component.literal("§7Usos restantes: §a" + (stack.getMaxDamage() - stack.getDamageValue())
                + "§7/§a" + stack.getMaxDamage()));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§8§oAtiva automaticamente no inventário"));
    }

    /** Event handler — checa magic damage e aplica reflexo. */
    @Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
    public static class Handler {
        @SubscribeEvent
        public static void onDamage(LivingDamageEvent e) {
            if (!(e.getEntity() instanceof Player player)) return;
            if (player.level().isClientSide) return;

            DamageSource src = e.getSource();
            // Só reflete magic (MAGIC ou INDIRECT_MAGIC)
            if (!src.is(DamageTypes.MAGIC) && !src.is(DamageTypes.INDIRECT_MAGIC)) {
                return;
            }
            // Acha mirror no inventário
            ItemStack mirror = ItemStack.EMPTY;
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.getItem() instanceof SpellMirrorItem) {
                    mirror = s;
                    break;
                }
            }
            if (mirror.isEmpty()) return;

            // r137 fix #4: guard contra loop infinito.
            // Se 2 players com Spell Mirror se atacam, o reflect pode recursar.
            // Set flag NBT pra evitar reflexão durante reflexão.
            if (player.getPersistentData().getBoolean("liberthia.mirror_reflecting")) return;
            player.getPersistentData().putBoolean("liberthia.mirror_reflecting", true);

            try {
                float amount = e.getAmount();
                float reflected = amount * 0.5F;

                // Reduz dano recebido em 50%
                e.setAmount(amount - reflected);

                // Aplica reflected no atacante (se for LivingEntity)
                Entity attacker = src.getEntity();
                if (attacker instanceof LivingEntity le && le != player) {
                    le.hurt(player.damageSources().magic(), reflected);
                }

                // r137 fix #3: mirror está no inventário (não na mão).
                // Skip broadcastBreakEvent — não há slot/hand específico pra animar.
                mirror.hurtAndBreak(1, player, p -> {});
            } finally {
                player.getPersistentData().remove("liberthia.mirror_reflecting");
            }

            // VFX
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        20, 0.4, 0.5, 0.4, 0.1);
            }
            player.playSound(net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE, 0.5F, 1.5F);
        }
    }
}
