package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.tech.TechEnergy;
import br.com.murilo.liberthia.item.tech.WardedArmorItem;
import br.com.murilo.liberthia.item.tech.WardedShieldItem;
import br.com.murilo.liberthia.magic.antimagic.AntiMagic;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * r182 — <b>Tecno-Arcano</b>: (1) decai os marcadores anti-magia a cada tick;
 * (2) reduz dano mágico de quem usa a Armadura Bastião (−15%/peça, FE) e o Escudo
 * Dissonante (−90% bloqueando, FE), drenando energia; e (3) aplica <b>Antimagia</b> no
 * mago que atacou um portador protegido (nerf ao atacante).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AntiMagicTechHandler {
    private AntiMagicTechHandler() {}

    private static final int FE_PER_DMG_ARMOR = 250;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && !e.player.level().isClientSide)
            AntiMagic.tickDecay(e.player);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (!AntiMagic.isMagicDamage(e.getSource())) return;
        if (!(e.getEntity() instanceof Player victim) || victim.level().isClientSide) return;

        float reduction = 0f;
        List<ItemStack> armor = new ArrayList<>();
        for (ItemStack st : victim.getArmorSlots())
            if (st.getItem() instanceof WardedArmorItem && TechEnergy.has(st)) { reduction += 0.15f; armor.add(st); }

        ItemStack use = victim.getUseItem();
        boolean shieldBlock = victim.isBlocking() && use.getItem() instanceof WardedShieldItem && TechEnergy.has(use);
        if (shieldBlock) reduction = Math.max(reduction, 0.90f);

        if (reduction <= 0f) return;
        reduction = Math.min(0.90f, reduction);

        float before = e.getAmount();
        float after = before * (1f - reduction);
        float absorbed = before - after;
        e.setAmount(after);

        if (!armor.isEmpty()) {
            int each = Math.max(1, Math.round(absorbed * FE_PER_DMG_ARMOR / armor.size()));
            for (ItemStack st : armor) TechEnergy.drain(st, each, ((WardedArmorItem) st.getItem()).cap);
        }
        if (shieldBlock)
            TechEnergy.drain(use, Math.max(1, Math.round(absorbed * WardedShieldItem.COST_PER_DMG)), WardedShieldItem.CAP);

        // nerfar o mago atacante (conjunto/escudo carregado vira contra-magia)
        if ((armor.size() >= 3 || shieldBlock) && e.getSource().getEntity() instanceof LivingEntity attacker && attacker != victim)
            attacker.addEffect(new MobEffectInstance(ModEffects.ANTIMAGIA.get(), 100, 0, false, true));
    }
}
