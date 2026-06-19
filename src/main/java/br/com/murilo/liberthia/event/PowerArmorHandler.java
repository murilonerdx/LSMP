package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.tech.PowerArmorItem;
import br.com.murilo.liberthia.item.tech.TechEnergy;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * r180c — <b>Power Armor</b>: cada peça carregada ABSORVE 12% do dano (até 48% no set
 * completo), consumindo FE proporcional ao dano absorvido (dividido entre as peças
 * carregadas). Dano que ignora armadura (void, fome, /kill) não é reduzido. Quando uma
 * peça fica sem FE ela para de absorver — vira armadura netherite normal.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class PowerArmorHandler {
    private PowerArmorHandler() {}

    private static final float PER_PIECE = 0.12F;
    private static final float MAX_REDUCTION = 0.48F;
    private static final int FE_PER_DMG = 250;

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) return;

        List<ItemStack> charged = new ArrayList<>();
        for (ItemStack st : p.getArmorSlots())
            if (st.getItem() instanceof PowerArmorItem && TechEnergy.has(st)) charged.add(st);
        if (charged.isEmpty()) return;

        float reduction = Math.min(MAX_REDUCTION, PER_PIECE * charged.size());
        float before = e.getAmount();
        float after = before * (1F - reduction);
        float absorbed = before - after;
        e.setAmount(after);

        int costEach = Math.max(1, Math.round(absorbed * FE_PER_DMG / charged.size()));
        for (ItemStack st : charged)
            TechEnergy.drain(st, costEach, ((PowerArmorItem) st.getItem()).cap);
    }
}
