package br.com.murilo.liberthia.registry;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Os 5 efeitos correspondentes aos perfis de matéria.
 *
 * <p>Cada efeito é apenas um marcador visual (ícone, cor, nome) — a
 * lógica real (modificação de stats, animações, etc.) é aplicada pelo
 * tick handler em {@code MatterProfileEvents}.
 */
public final class ModMobEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, LiberthiaMod.MODID);

    /** DM puro — selvagem, agressivo, coceira, tamanho variável. */
    public static final RegistryObject<MobEffect> AGGRESSION = EFFECTS.register("aggression",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x9C2C77));

    /** DM + WM — contido, manipulável. */
    public static final RegistryObject<MobEffect> CONTAINED = EFFECTS.register("contained",
            () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 0x6E40C9));

    /** WM puro — inteligente mas com lapsos. */
    public static final RegistryObject<MobEffect> FORGETFULNESS = EFFECTS.register("forgetfulness",
            () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 0xE6E6FF));

    /** YM puro — caótico, alucinações, descontrole emocional. */
    public static final RegistryObject<MobEffect> EMOTIONAL_CHAOS = EFFECTS.register("emotional_chaos",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0xFFD23F));

    /** YM + WM — estrategista frio. */
    public static final RegistryObject<MobEffect> COLD_FOCUS = EFFECTS.register("cold_focus",
            () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFFEC8B));

    private ModMobEffects() {}

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private static final class SimpleEffect extends MobEffect {
        public SimpleEffect(MobEffectCategory cat, int color) {
            super(cat, color);
        }
        @Override public boolean isInstantenous() { return false; }
    }
}
