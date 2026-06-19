package br.com.murilo.liberthia.magic.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.24 r94: <b>Upgrade Orb</b> — consumível que aplica upgrade permanente
 * num item.
 *
 * <h2>5 tipos</h2>
 * <ul>
 *   <li>{@code MANA_BOOST} — +50 max Source no NBT do alvo</li>
 *   <li>{@code COOLDOWN_REDUCTION} — -15% cooldown em staffs/scrolls</li>
 *   <li>{@code SPELL_DAMAGE} — +20% damage de school spells</li>
 *   <li>{@code HEALTH} — +1 absorption permanent na armor</li>
 *   <li>{@code SOURCE_REGEN} — +25% source regen rate</li>
 * </ul>
 *
 * <p>Uso: Shift+Right-click com item válido na offhand → consome orb,
 * aplica NBT bonus no item da offhand. Capped em 3 upgrades por item.
 */
public class UpgradeOrbItem extends Item {

    public enum Type {
        MANA_BOOST("mana_boost", "+50 Max Source"),
        COOLDOWN_REDUCTION("cooldown_reduction", "-15% Cooldown"),
        SPELL_DAMAGE("spell_damage", "+20% Spell Damage"),
        HEALTH("health", "+2 Max HP"),
        SOURCE_REGEN("source_regen", "+25% Source Regen");
        public final String nbtKey;
        public final String description;
        Type(String key, String desc) { this.nbtKey = key; this.description = desc; }
    }

    private final Type type;

    public UpgradeOrbItem(Properties props, Type type) {
        super(props.stacksTo(8));
        this.type = type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(player.getItemInHand(hand));
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        ItemStack target = player.getItemInHand(InteractionHand.OFF_HAND);
        if (target.isEmpty()) {
            player.displayClientMessage(Component.literal(
                    "§7§o✦ Coloque um item na offhand pra aplicar"), true);
            return InteractionResultHolder.fail(stack);
        }

        var tag = target.getOrCreateTag();
        var upgrades = tag.getCompound("Upgrades");
        // Cap total de 3 upgrades por item
        int total = 0;
        for (String k : upgrades.getAllKeys()) total += upgrades.getInt(k);
        if (total >= 3) {
            player.displayClientMessage(Component.literal(
                    "§4§o✦ Limite de upgrades atingido (3)"), true);
            return InteractionResultHolder.fail(stack);
        }

        int current = upgrades.getInt(type.nbtKey);
        if (current >= 3) {
            player.displayClientMessage(Component.literal(
                    "§4§o✦ Upgrade já no max"), true);
            return InteractionResultHolder.fail(stack);
        }
        upgrades.putInt(type.nbtKey, current + 1);
        tag.put("Upgrades", upgrades);

        if (!player.isCreative()) stack.shrink(1);
        player.displayClientMessage(Component.literal(
                "§5§l✦ §r§5" + type.description + " §5aplicado"), false);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§b" + type.description));
        tooltip.add(Component.literal("§7§oShift+R-click com item na offhand"));
        tooltip.add(Component.literal("§8§oMax 3 upgrades por item"));
    }
}
