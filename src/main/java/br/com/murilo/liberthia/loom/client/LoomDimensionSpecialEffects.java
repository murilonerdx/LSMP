package br.com.murilo.liberthia.loom.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;

/**
 * v0.1.22 r33: efeitos visuais da dimensão Loom — sky roxo intenso, sol roxo
 * escuro, lua cinza.
 *
 * <p>Configuração:
 * <ul>
 *   <li>{@code cloudLevel = Float.NaN} — sem clouds</li>
 *   <li>{@code hasGround = false} — floating islands</li>
 *   <li>{@code skyType = NONE} — desenho próprio (sky color do biome)</li>
 *   <li>{@code forceBrightLightmap = false}</li>
 *   <li>{@code constantAmbientLight = true} — luz uniforme</li>
 *   <li>Fog color: roxo escuro intenso</li>
 * </ul>
 *
 * <p><b>Sol/Lua custom:</b> overridden em {@link #renderSky}? — não, em 1.20.1
 * sky type NONE significa que o renderer custom desenha nada (sem sol/lua).
 * Em vez disso usamos NORMAL com effects pra sky color escuro + Heaven cloud
 * tint + texturas customizadas via resource pack override em
 * {@code assets/minecraft/textures/environment/sun.png} (não custom).
 *
 * <p>Pra ter SOL ROXO + LUA CINZA, override via texture pack OR render custom.
 * Aqui usamos sky type NORMAL com tints fortes.
 */
public class LoomDimensionSpecialEffects extends DimensionSpecialEffects {

    public LoomDimensionSpecialEffects() {
        super(Float.NaN, false, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 vec, float f) {
        // Roxo escuro denso — multiplica fog do biome
        return vec.multiply(0.4, 0.2, 0.6);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return true; // sempre fog
    }

    @Override
    public float[] getSunriseColor(float p_108871_, float p_108872_) {
        // Sunrise/sunset: tom roxo intenso
        return new float[]{0.4F, 0.1F, 0.6F, 1.0F};
    }
}
