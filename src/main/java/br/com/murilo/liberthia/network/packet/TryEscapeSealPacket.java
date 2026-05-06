package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.capture.CapturedPlayerManager;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.network.NetworkEvent;

import java.util.Random;
import java.util.function.Supplier;

public class TryEscapeSealPacket {

    private static final Random RANDOM = new Random();

    public static void encode(TryEscapeSealPacket packet, FriendlyByteBuf buffer) {
    }

    public static TryEscapeSealPacket decode(FriendlyByteBuf buffer) {
        return new TryEscapeSealPacket();
    }

    public static void handle(TryEscapeSealPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) {
                return;
            }

            if (!CapturedPlayerManager.isCaptured(player)) {
                return;
            }

            double chance = calculateEscapeChance(player);
            double roll = RANDOM.nextDouble();

            if (roll <= chance) {
                CapturedPlayerManager.release(player);

                player.displayClientMessage(
                        Component.literal("Você rompeu o selo. Chance: ")
                                .append(Component.literal(formatPercent(chance)).withStyle(ChatFormatting.GREEN)),
                        false
                );
            } else {
                player.displayClientMessage(
                        Component.literal("Você tentou fugir, mas o selo resistiu. Chance: ")
                                .append(Component.literal(formatPercent(chance)).withStyle(ChatFormatting.RED)),
                        false
                );

                ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new OpenCapturedScreenPacket()
                );
            }
        });

        context.setPacketHandled(true);
    }

    private static double calculateEscapeChance(ServerPlayer player) {
        double maxHealth = player.getAttributeValue(Attributes.MAX_HEALTH);
        double bestDamage = getBestDamageFromInventory(player.getInventory());

        double raw = bestDamage / Math.max(1.0D, maxHealth + 20.0D);

        double armorBonus = player.getArmorValue() * 0.005D;
        double healthBonus = maxHealth * 0.002D;

        double chance = raw + armorBonus + healthBonus;

        return clamp(chance, 0.03D, 0.75D);
    }

    private static double getBestDamageFromInventory(Inventory inventory) {
        double best = 1.0D;

        for (ItemStack stack : inventory.items) {
            if (stack.isEmpty()) {
                continue;
            }

            double damage = 1.0D;

            var modifiers = stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                    .get(Attributes.ATTACK_DAMAGE);

            for (var modifier : modifiers) {
                damage += modifier.getAmount();
            }

            int sharpness = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, stack);
            damage += sharpness * 0.5D;

            if (damage > best) {
                best = damage;
            }
        }

        return best;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatPercent(double value) {
        return String.format(java.util.Locale.US, "%.1f%%", value * 100.0D);
    }
}
