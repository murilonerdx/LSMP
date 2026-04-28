package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.effect.BloodInfectionEffect;
import br.com.murilo.liberthia.item.BloodWardCharmItem;
import br.com.murilo.liberthia.item.SanguineWardArmorItem;
import br.com.murilo.liberthia.item.SanguineWardPickaxeItem;
import br.com.murilo.liberthia.logic.BloodKin;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Centralised protection logic for the Sanguine Ward set + Blood Ward Charm:
 *
 * <ul>
 *   <li>{@link MobEffectEvent.Applicable}: rolls a chance per protection
 *       source to deny FRESH Blood Infection applications.
 *   <li>{@link LivingHurtEvent}: when an infected wearer takes magic damage,
 *       reduces the amount by 25% per armor piece and 50% from the charm
 *       (additive, capped at 100%). This is the actual damage-mitigation
 *       layer that makes the armor feel strong.
 *   <li>{@link LivingEvent.LivingTickEvent}: full set wearers get periodic
 *       cleansing pulses; pickaxe holders heal a smaller amount of drain.
 *   <li>{@link LivingDeathEvent}: when a player kills a {@link BloodKin}
 *       creature ("vermes" — worms/hounds/cultists/etc), there's a 35%
 *       chance to drop {@code tainted_essence} (consumable cure) so killing
 *       infected mobs becomes a way to deal with infection.
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SanguineWardEvents {
    private SanguineWardEvents() {}

    // ----------------------------------------------------------- application gate
    @SubscribeEvent
    public static void onEffectApply(MobEffectEvent.Applicable ev) {
        if (ModEffects.BLOOD_INFECTION.get() == null) return;
        if (ev.getEffectInstance().getEffect() != ModEffects.BLOOD_INFECTION.get()) return;
        if (!(ev.getEntity() instanceof Player p)) return;

        float resistance = wardResistance(p);
        if (resistance <= 0F) return;
        if (resistance >= 1F || p.getRandom().nextFloat() < resistance) {
            ev.setResult(Event.Result.DENY);
        }
    }

    // ----------------------------------------------------------- damage layer
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent ev) {
        if (!(ev.getEntity() instanceof Player p)) return;
        // We want to mitigate damage that is plausibly from BloodInfection.
        // Heuristic: source is magic AND player has the effect active.
        if (!ev.getSource().is(DamageTypes.MAGIC)) return;
        if (ModEffects.BLOOD_INFECTION.get() == null) return;
        if (!p.hasEffect(ModEffects.BLOOD_INFECTION.get())) return;

        float reduction = wardResistance(p);
        if (reduction <= 0F) return;
        reduction = Math.min(1F, reduction);
        float newAmount = ev.getAmount() * (1F - reduction);
        ev.setAmount(newAmount);
    }

    // ----------------------------------------------------------- periodic cleanse
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent ev) {
        if (!(ev.getEntity() instanceof Player p)) return;
        if (p.level().isClientSide) return;
        if (p.tickCount % 100 != 0) return; // every 5s
        if (ModEffects.BLOOD_INFECTION.get() == null) return;

        if (SanguineWardArmorItem.hasFullSet(p)) {
            if (p.hasEffect(ModEffects.BLOOD_INFECTION.get())) {
                p.removeEffect(ModEffects.BLOOD_INFECTION.get());
            }
            healDrain(p, 1.5D);
            return;
        }

        boolean pick = p.getMainHandItem().getItem() instanceof SanguineWardPickaxeItem
                || p.getOffhandItem().getItem() instanceof SanguineWardPickaxeItem;
        if (pick) {
            healDrain(p, 0.5D);
        }
    }

    // ----------------------------------------------------------- kin kill drops
    @SubscribeEvent
    public static void onKinDeath(LivingDeathEvent ev) {
        LivingEntity killed = ev.getEntity();
        if (killed.level().isClientSide) return;
        if (!BloodKin.is(killed)) return;
        if (!(ev.getSource().getEntity() instanceof Player killer)) return;
        if (ModItems.TAINTED_ESSENCE.get() == null) return;

        if (killer.getRandom().nextFloat() < 0.35F) {
            ItemStack drop = new ItemStack(ModItems.TAINTED_ESSENCE.get(),
                    1 + killer.getRandom().nextInt(2));
            ItemEntity entity = new ItemEntity(killed.level(),
                    killed.getX(), killed.getY() + 0.4, killed.getZ(), drop);
            entity.setDefaultPickUpDelay();
            killed.level().addFreshEntity(entity);
        }
    }

    // ----------------------------------------------------------- helpers
    private static float wardResistance(Player p) {
        float r = SanguineWardArmorItem.countPieces(p) * 0.25F;
        ItemStack offhand = p.getOffhandItem();
        if (offhand.getItem() instanceof BloodWardCharmItem) {
            r += 0.5F;
        }
        return r;
    }

    private static void healDrain(Player p, double amount) {
        CompoundTag data = p.getPersistentData();
        double cur = data.getDouble(BloodInfectionEffect.NBT_DRAIN);
        if (cur <= 0) return;
        double next = Math.max(0, cur - amount);
        // Use the official applier so the attribute modifier + saved-data sync.
        br.com.murilo.liberthia.effect.BloodInfectionApplier.apply(p, next);
    }
}
