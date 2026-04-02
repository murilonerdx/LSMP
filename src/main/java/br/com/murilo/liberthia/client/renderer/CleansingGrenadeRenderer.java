package br.com.murilo.liberthia.client.renderer;

import br.com.murilo.liberthia.entity.CleansingGrenadeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/**
 * Dedicated renderer for CleansingGrenadeEntity.
 * This explicit class avoids generic type-matching issues during Forge's entity renderer registration.
 */
public class CleansingGrenadeRenderer extends ThrownItemRenderer<CleansingGrenadeEntity> {
    public CleansingGrenadeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
