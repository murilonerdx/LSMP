package br.com.murilo.liberthia.storage;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * v0.1.24 r92: <b>Void Jar</b> — item portátil que apaga itens whitelisted
 * do inventário automaticamente.
 *
 * <h2>Uso</h2>
 * <ul>
 *   <li>Shift+right-click no chão = entra em "configure mode"</li>
 *   <li>Right-click em qualquer item no inventário → adiciona à whitelist</li>
 *   <li>Quando você pega items, se eles tão na whitelist, são apagados na hora</li>
 * </ul>
 *
 * <p>NBT: {@code Whitelist} (ListTag of Strings — item registry IDs).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public class VoidJarItem extends Item {

    public static final String NBT_WHITELIST = "Whitelist";

    public VoidJarItem(Properties props) {
        super(props.stacksTo(1));
    }

    /** Verifica se item está na whitelist de algum Void Jar no inventário do player. */
    public static boolean isVoided(Player player, ItemStack stack) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof VoidJarItem && stack.getItem() != s.getItem()) {
                CompoundTag tag = s.getTag();
                if (tag == null || !tag.contains(NBT_WHITELIST)) continue;
                ListTag list = tag.getList(NBT_WHITELIST, Tag.TAG_STRING);
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                for (int j = 0; j < list.size(); j++) {
                    if (list.getString(j).equals(id)) return true;
                }
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        // Shift+right-click: clear whitelist
        if (player.isShiftKeyDown()) {
            stack.getOrCreateTag().remove(NBT_WHITELIST);
            player.displayClientMessage(Component.literal("§5✦ Void Jar resetado."), true);
            return InteractionResultHolder.success(stack);
        }

        // Right-click: adiciona o item da OFFHAND à whitelist
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!other.isEmpty()) {
            CompoundTag tag = stack.getOrCreateTag();
            ListTag list = tag.getList(NBT_WHITELIST, Tag.TAG_STRING);
            String id = BuiltInRegistries.ITEM.getKey(other.getItem()).toString();
            // Check duplicate
            for (int i = 0; i < list.size(); i++) {
                if (list.getString(i).equals(id)) {
                    player.displayClientMessage(Component.literal(
                            "§7§o✦ Já na whitelist: " + id), true);
                    return InteractionResultHolder.fail(stack);
                }
            }
            list.add(StringTag.valueOf(id));
            tag.put(NBT_WHITELIST, list);
            player.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5Adicionado à whitelist: §b" + id), false);
            return InteractionResultHolder.success(stack);
        }

        // Sem item — mostra lista
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_WHITELIST)) {
            ListTag list = tag.getList(NBT_WHITELIST, Tag.TAG_STRING);
            player.displayClientMessage(Component.literal(
                    "§5✦ §b" + list.size() + " §5items na whitelist"), true);
        } else {
            player.displayClientMessage(Component.literal(
                    "§7§o✦ Whitelist vazia (offhand + right-click pra adicionar)"), true);
        }
        return InteractionResultHolder.success(stack);
    }

    /** Hook em pickup — se item entrar no inv e for whitelisted, cancela. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemPickup(EntityItemPickupEvent event) {
        Player p = event.getEntity();
        ItemStack picked = event.getItem().getItem();
        if (picked.isEmpty()) return;
        if (isVoided(p, picked)) {
            event.getItem().discard();
            event.setCanceled(true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_WHITELIST)) {
            ListTag list = tag.getList(NBT_WHITELIST, Tag.TAG_STRING);
            tooltip.add(Component.literal("§7Items voided: §c" + list.size()));
        }
        tooltip.add(Component.literal("§8§oOffhand + R-click = add to whitelist"));
        tooltip.add(Component.literal("§8§oShift+R-click = reset"));
    }
}
