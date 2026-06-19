package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.enchantment.WardEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** r180b — encantamentos do mod. Hoje: Dissonância (anti-magia). */
public final class ModEnchantments {

    private ModEnchantments() {}

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, LiberthiaMod.MODID);

    /** Dissonância — armadura, -40% dano mágico recebido. */
    public static final RegistryObject<Enchantment> WARD =
            ENCHANTMENTS.register("ward", WardEnchantment::new);

    public static void register(IEventBus bus) { ENCHANTMENTS.register(bus); }
}
