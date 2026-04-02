package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.capability.IInfectionData;
import br.com.murilo.liberthia.capability.IMatterEnergy;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ModCapabilities {
    public static final Capability<IInfectionData> INFECTION = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static final Capability<IMatterEnergy> MATTER_ENERGY = CapabilityManager.get(new CapabilityToken<>() {
    });

    private ModCapabilities() {
    }

    public static void register(IEventBus eventBus) {
        eventBus.addListener(ModCapabilities::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IInfectionData.class);
        event.register(IMatterEnergy.class);
    }
}
