package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r194 — bônus de set das armaduras dos chefes (set completo, renovado a cada 0,5s):
 * Astral = visão noturna + queda lenta + resistência; Parasítica = regeneração + resistência + absorção.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class BossGearHandler {
    private BossGearHandler() {}

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide) return;
        if (e.player.tickCount % 20 != 0) return;
        Player p = e.player;
        if (fullSet(p, ModItems.ASTRAL_HELMET.get(), ModItems.ASTRAL_CHESTPLATE.get(), ModItems.ASTRAL_LEGGINGS.get(), ModItems.ASTRAL_BOOTS.get())) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 40, 0, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, true, false));
        }
        if (fullSet(p, ModItems.PARASITIC_HELMET.get(), ModItems.PARASITIC_CHESTPLATE.get(), ModItems.PARASITIC_LEGGINGS.get(), ModItems.PARASITIC_BOOTS.get())) {
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1, true, false));
        }
    }

    private static boolean fullSet(Player p, Item head, Item chest, Item legs, Item feet) {
        return p.getItemBySlot(EquipmentSlot.HEAD).is(head)
                && p.getItemBySlot(EquipmentSlot.CHEST).is(chest)
                && p.getItemBySlot(EquipmentSlot.LEGS).is(legs)
                && p.getItemBySlot(EquipmentSlot.FEET).is(feet);
    }
}
