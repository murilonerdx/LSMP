package br.com.murilo.liberthia.observation.parts;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.api.Distortion;
import br.com.murilo.liberthia.observation.api.ObservationPart;
import br.com.murilo.liberthia.observation.api.ObservationStats;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r60: <b>Amplify Distortion</b> — intensifica o efeito modificado.
 * intensity ×1.5, reach +1.
 */
public class AmplifyDistortion extends Distortion {

    public AmplifyDistortion() {
        super(new ResourceLocation(LiberthiaMod.MODID, "distort/amplify"), "Amplificar");
    }

    @Override public int sanityCost() { return 2; }

    @Override
    public int sanityCostForPart(@Nullable ObservationPart parent) {
        // Custa mais em Manifestations AOE
        return 2;
    }

    @Override
    public void applyToStats(ObservationStats.Builder builder,
                              @Nullable ObservationPart augmented) {
        builder.intensity(builder.intensity * 1.5);
        builder.addReach(1.0);
        builder.addSanityCost(2);
    }

    @Override
    public List<Component> lore() {
        return List.of(
            Component.literal("§5§oMais forte. Mais custoso."),
            Component.literal("§8§o×1.5 intensity, +1 reach")
        );
    }
}
