package br.com.murilo.liberthia.magic.perk;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * v0.1.24 r90: <b>Perk Armor Events</b> — tick handler que aplica perks
 * de armor equipada.
 *
 * <h2>Storage</h2>
 * Cada armor item tem NBT {@code Perks} (ListTag de Strings — perk IDs com
 * level suffix). Ex.: {@code ["jump:2", "magic_resist:1"]}.
 *
 * <p>Cada 20t, scaneia armor slots e chama Perk.onTick com level somado.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class PerkArmorEvents {

    public static final String NBT_PERKS = "Perks";

    private PerkArmorEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = event.player;
        if (p.level().isClientSide) return;
        if (p.tickCount % 20 != 0) return;

        // Map perk ID → soma de levels em todas armor pieces
        Map<String, Integer> active = new HashMap<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = p.getItemBySlot(slot);
            if (!(armor.getItem() instanceof ArmorItem)) continue;
            CompoundTag tag = armor.getTag();
            if (tag == null || !tag.contains(NBT_PERKS)) continue;
            ListTag list = tag.getList(NBT_PERKS, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String entry = list.getString(i);
                String[] parts = entry.split(":");
                String id = parts[0];
                int level = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                active.merge(id, level, Integer::sum);
            }
        }

        // Tick cada perk ativo
        for (var e : active.entrySet()) {
            Perk perk = Perks.get(e.getKey());
            if (perk == null) continue;
            int lvl = Math.min(perk.getMaxStack(), e.getValue());
            try {
                perk.onTick(p, lvl);
            } catch (Throwable t) {
                LiberthiaMod.LOGGER.error("[Perk] erro em {}: {}", perk.getId(), t.getMessage());
            }
        }
    }

    /** Instala um perk numa armor (chamado externamente via item right-click). */
    public static boolean installPerk(ItemStack armor, String perkId, int level) {
        if (!(armor.getItem() instanceof ArmorItem)) return false;
        Perk perk = Perks.get(perkId);
        if (perk == null) return false;
        CompoundTag tag = armor.getOrCreateTag();
        ListTag list = tag.getList(NBT_PERKS, Tag.TAG_STRING);
        // Verifica se já tem este perk e soma level (capped)
        for (int i = 0; i < list.size(); i++) {
            String entry = list.getString(i);
            String[] parts = entry.split(":");
            if (parts[0].equals(perkId)) {
                int current = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                int newLvl = Math.min(perk.getMaxStack(), current + level);
                list.set(i, net.minecraft.nbt.StringTag.valueOf(perkId + ":" + newLvl));
                tag.put(NBT_PERKS, list);
                return true;
            }
        }
        // Novo perk
        list.add(net.minecraft.nbt.StringTag.valueOf(perkId + ":" + level));
        tag.put(NBT_PERKS, list);
        return true;
    }
}
