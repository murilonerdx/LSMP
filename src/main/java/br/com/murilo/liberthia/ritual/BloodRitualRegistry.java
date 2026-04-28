package br.com.murilo.liberthia.ritual;

import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Static registry of available blood rituals. Lookup by activator item is
 * what GoldenBloodBowlBlockEntity uses to find which ritual the player tried
 * to start.
 *
 * <p>To add a new ritual: implement {@link BloodRitual} and call
 * {@link #register} from a static initializer (here or in your subclass).
 */
public final class BloodRitualRegistry {

    private static final List<Supplier<? extends BloodRitual>> FACTORIES = new ArrayList<>();
    private static List<BloodRitual> instances;

    private BloodRitualRegistry() {}

    static {
        register(SummonFleshMotherRitual::new);
        register(SummonBloodHoundRitual::new);
        register(PurifyZoneRitual::new);
        // CraftRitual example: forge a Blood Scythe.
        register(() -> new CraftRitual(
                "craft_blood_scythe",
                java.util.List.of(
                        new net.minecraft.core.BlockPos( 2, 0,  0),
                        new net.minecraft.core.BlockPos(-2, 0,  0),
                        new net.minecraft.core.BlockPos( 0, 0,  2),
                        new net.minecraft.core.BlockPos( 0, 0, -2)),
                java.util.List.of(
                        br.com.murilo.liberthia.registry.ModItems.SANGUINE_ESSENCE.get(),
                        br.com.murilo.liberthia.registry.ModItems.CONGEALED_BLOOD.get(),
                        br.com.murilo.liberthia.registry.ModItems.PRIEST_SIGIL.get()),
                br.com.murilo.liberthia.registry.ModItems.BLOOD_RITUAL_DAGGER.get(),
                100,
                () -> new net.minecraft.world.item.ItemStack(
                        br.com.murilo.liberthia.registry.ModItems.BLOOD_SCYTHE.get())));
    }

    public static void register(Supplier<? extends BloodRitual> factory) {
        FACTORIES.add(factory);
        instances = null; // invalidate cache
    }

    public static List<BloodRitual> all() {
        if (instances == null) {
            instances = new ArrayList<>(FACTORIES.size());
            for (Supplier<? extends BloodRitual> f : FACTORIES) instances.add(f.get());
        }
        return instances;
    }

    /** Returns the first ritual whose activator matches AND whose pentacle/ingredients are valid. */
    public static BloodRitual findValid(net.minecraft.server.level.ServerLevel level,
                                        net.minecraft.core.BlockPos centre,
                                        Item activator) {
        for (BloodRitual r : all()) {
            if (r.activator() != activator) continue;
            if (r.isValid(level, centre)) return r;
        }
        return null;
    }
}
