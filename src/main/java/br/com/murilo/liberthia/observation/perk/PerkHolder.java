package br.com.murilo.liberthia.observation.perk;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * v0.1.22 r62: <b>Perk Holder</b> — utility para items que carregam perks.
 *
 * <p>Items implementam isso pra rastrear seus perks via NBT. Perks são
 * salvos como list of (slotValue + perkId).
 *
 * <h2>NBT format</h2>
 * <pre>
 * stack.tag = {
 *   liberthia.perks: [
 *     {slot: 1, id: "liberthia:perk/regen"},
 *     {slot: 2, id: "liberthia:perk/damage_boost"},
 *   ],
 *   liberthia.tier: 2
 * }
 * </pre>
 */
public final class PerkHolder {

    public static final String NBT_PERKS = "liberthia.perks";
    public static final String NBT_TIER = "liberthia.tier";

    private PerkHolder() {}

    public static int getTier(ItemStack stack) {
        return stack.getOrCreateTag().getInt(NBT_TIER);
    }

    public static void setTier(ItemStack stack, int tier) {
        stack.getOrCreateTag().putInt(NBT_TIER, Math.max(0, Math.min(3, tier)));
    }

    public static List<PerkSlot> getPerks(ItemStack stack) {
        List<PerkSlot> out = new ArrayList<>();
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(NBT_PERKS)) return out;
        ListTag list = tag.getList(NBT_PERKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("slot");
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            if (id == null) continue;
            Perk p = PerkRegistry.get(id);
            if (p != null) out.add(new PerkSlot(slot, p));
        }
        return out;
    }

    public static void setPerks(ItemStack stack, List<PerkSlot> perks) {
        ListTag list = new ListTag();
        for (PerkSlot ps : perks) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", ps.slotValue);
            entry.putString("id", ps.perk.id().toString());
            list.add(entry);
        }
        stack.getOrCreateTag().put(NBT_PERKS, list);
    }

    /** Adds a perk to first empty slot of given tier (slotValue 1-3). */
    public static boolean addPerk(ItemStack stack, Perk perk, int slotValue) {
        List<PerkSlot> current = getPerks(stack);
        int maxSlots = maxSlotsForTier(getTier(stack));
        if (current.size() >= maxSlots) return false;
        current.add(new PerkSlot(slotValue, perk));
        setPerks(stack, current);
        return true;
    }

    public static int maxSlotsForTier(int tier) {
        return switch (tier) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> 0;
        };
    }

    public static class PerkSlot {
        public final int slotValue;
        public final Perk perk;
        public PerkSlot(int slotValue, Perk perk) {
            this.slotValue = slotValue;
            this.perk = perk;
        }
    }
}
