package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entity.CorruptedZombieEntity;
import br.com.murilo.liberthia.entity.SporeSpitterEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityEvents {

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CORRUPTED_ZOMBIE.get(), CorruptedZombieEntity.createAttributes().build());
        event.put(ModEntities.SPORE_SPITTER.get(), SporeSpitterEntity.createAttributes().build());
    }
}
