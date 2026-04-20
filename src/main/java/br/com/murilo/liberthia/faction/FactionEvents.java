package br.com.murilo.liberthia.faction;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Central rule-set for the Order × Blood feud.
 *
 *  - Holy weapons (HolyBlade/HolyHammer/HolySmite/Sanctify): +50% damage vs BLOOD entities.
 *  - Blood weapons (Scythe/Hemomancer/Dagger/Bow): +50% damage vs ORDER entities.
 *  - Order full armor (4 pieces): -30% damage from BLOOD weapons.
 *  - Blood full armor (4 pieces): -30% damage from HOLY weapons, +20% from water/drowning.
 *  - Killing a BLOOD entity: player's BLOOD rep -5, ORDER rep +3.
 *  - Killing an ORDER entity: player's ORDER rep -5, BLOOD rep +3.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public class FactionEvents {

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity src = event.getSource().getEntity();
        if (!(src instanceof LivingEntity attacker)) return;

        ItemStack weapon = attacker.getMainHandItem();
        boolean holy = isHoly(weapon);
        boolean blood = isBloodWeapon(weapon);
        Faction victimFac = FactionTag.get(victim);

        float dmg = event.getAmount();

        // Weapon bonuses
        if (holy && victimFac == Faction.BLOOD) dmg *= 1.5F;
        if (blood && victimFac == Faction.ORDER) dmg *= 1.5F;

        // Armor mitigation
        if (victim instanceof Player vp) {
            int orderPieces = countArmor(vp, "order");
            int bloodPieces = countArmor(vp, "blood");
            if (orderPieces >= 4 && blood) dmg *= 0.7F;
            if (bloodPieces >= 4 && holy) dmg *= 0.7F;
        }

        event.setAmount(dmg);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        Entity src = event.getSource().getEntity();
        if (!(src instanceof ServerPlayer killer)) return;
        if (!(killer.level() instanceof ServerLevel sl)) return;
        Faction killedFac = FactionTag.get(event.getEntity());
        if (killedFac == Faction.NEUTRAL) return;

        FactionReputation rep = FactionReputation.forLevel(sl);
        if (killedFac == Faction.BLOOD) {
            rep.add(killer.getUUID(), Faction.BLOOD, -5);
            rep.add(killer.getUUID(), Faction.ORDER, +3);
        } else if (killedFac == Faction.ORDER) {
            rep.add(killer.getUUID(), Faction.ORDER, -5);
            rep.add(killer.getUUID(), Faction.BLOOD, +3);
        }
    }

    private static boolean isHoly(ItemStack s) {
        return s.is(ModItems.HOLY_BLADE.get())
                || s.is(ModItems.HOLY_HAMMER.get())
                || s.is(ModItems.HOLY_SMITE_STAFF.get())
                || s.is(ModItems.SANCTIFY_ORB.get());
    }

    private static boolean isBloodWeapon(ItemStack s) {
        return s.is(ModItems.BLOOD_SCYTHE.get())
                || s.is(ModItems.HEMOMANCER_STAFF.get())
                || s.is(ModItems.BLOOD_RITUAL_DAGGER.get())
                || s.is(ModItems.BLOOD_BOW.get())
                || s.is(ModItems.RUSTED_DAGGER.get());
    }

    private static int countArmor(Player player, String kind) {
        int n = 0;
        for (ItemStack s : player.getArmorSlots()) {
            if (s.isEmpty() || !(s.getItem() instanceof ArmorItem ai)) continue;
            String matName = ai.getMaterial().getName().toLowerCase();
            if (matName.contains(kind)) n++;
        }
        return n;
    }
}
