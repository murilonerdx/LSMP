package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.entity.projectile.HuntressArrowEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * <b>Arco da Caçadora</b> — comporta-se como um arco normal (carrega, mira,
 * consome flechas), com três habilidades:
 * <ul>
 *   <li><b>Tiro Marcado</b> — toda flecha marca o alvo por 5s; marcado recebe
 *       +30% de dano de qualquer fonte (efeito {@code MARKED} +
 *       {@code HuntressMarkHandler}).</li>
 *   <li><b>Rajada Divina</b> — segure por 2s e solte → dispara 3 flechas em
 *       leque (consome 1 flecha).</li>
 *   <li><b>Tiro Fantasma</b> — 10% de chance por flecha de sair invisível e
 *       ignorar armadura.</li>
 * </ul>
 */
public class HuntressBowItem extends BowItem {

    /** Tempo de carga (ticks) pra disparar a Rajada Divina ao soltar. 40t = 2s. */
    private static final int RAJADA_CHARGE = 40;
    private static final float GHOST_CHANCE = 0.10F;

    public HuntressBowItem(Properties props) {
        super(props.stacksTo(1).durability(480));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;

        boolean infinite = player.getAbilities().instabuild
                || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) > 0;
        ItemStack ammo = player.getProjectile(stack);
        if (ammo.isEmpty() && !infinite) return;

        int charge = this.getUseDuration(stack) - timeLeft;
        float power = getPowerForTime(charge);
        if (power < 0.1F) return;

        boolean burst = charge >= RAJADA_CHARGE; // Rajada Divina

        if (!level.isClientSide) {
            if (burst) {
                // 3 flechas em leque (full power); cada uma sorteia Tiro Fantasma.
                // Custa 1 flecha — as extras não são recuperáveis (CREATIVE_ONLY).
                fireArrow(level, player, stack, 1.0F, -8.0F, true, true);
                fireArrow(level, player, stack, 1.0F, 0.0F, true, true);
                fireArrow(level, player, stack, 1.0F, 8.0F, true, true);
                stack.hurtAndBreak(3, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
                if (player instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.literal("§6✦ Rajada Divina!"), true);
                }
            } else {
                fireArrow(level, player, stack, power, 0.0F, power >= 1.0F, infinite);
                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
            }
            if (!infinite) consumeOneArrow(player, stack);
        }

        float pitch = burst ? 1.5F
                : (1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, pitch);
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private void fireArrow(Level level, Player player, ItemStack bow, float power,
                           float yawOffset, boolean crit, boolean noPickup) {
        HuntressArrowEntity arrow = new HuntressArrowEntity(level, player);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset,
                0.0F, power * 3.0F, 1.0F);
        if (crit) arrow.setCritArrow(true);

        int pw = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
        if (pw > 0) arrow.setBaseDamage(arrow.getBaseDamage() + pw * 0.5 + 0.5);
        int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, bow);
        if (punch > 0) arrow.setKnockback(punch);
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, bow) > 0) {
            arrow.setSecondsOnFire(100);
        }

        // Tiro Fantasma — 10% invisível ignora armadura
        if (player.getRandom().nextFloat() < GHOST_CHANCE) {
            arrow.setGhost(true);
        }
        if (noPickup) arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        level.addFreshEntity(arrow);
    }

    private static void consumeOneArrow(Player player, ItemStack bow) {
        if (player.getAbilities().instabuild) return;
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow) > 0) return;
        ItemStack ammo = player.getProjectile(bow);
        if (!ammo.isEmpty()) {
            ammo.shrink(1);
            if (ammo.isEmpty()) player.getInventory().removeItem(ammo);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§2✦ §aArco da Caçadora"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§7Tiro Marcado: §fmarca o alvo §e5s §7(+30% dano recebido)"));
        tooltip.add(Component.literal("§7Rajada Divina: §fsegure §e2s §fe solte → §63 flechas"));
        tooltip.add(Component.literal("§7Tiro Fantasma: §f10% §7→ flecha invisível que §dignora armadura"));
    }
}
