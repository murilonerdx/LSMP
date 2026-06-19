package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/**
 * v0.1.144 r111: Client-side ItemColor registration — pinta o layer1 do pergaminho
 * conforme a escola do spell (FIRE→laranja, ICE→ciano, ...).
 *
 * <p>Cada {@link UniversalSpellScrollItem} chama {@link UniversalSpellScrollItem#tintColor(net.minecraft.world.item.ItemStack, int)}
 * para obter a cor do layer.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SpellScrollColorHandler {

    private SpellScrollColorHandler() {}

    @SubscribeEvent
    public static void onRegisterColors(RegisterColorHandlersEvent.Item event) {
        RegistryObject<Item>[] all = new RegistryObject[]{
                ModItems.SPELL_FIREBALL, ModItems.SPELL_BURNING_DASH, ModItems.SPELL_INFERNO,
                ModItems.SPELL_MAGMA_BOMB, ModItems.SPELL_SUN_BEAM, ModItems.SPELL_PHOENIX_REBORN,
                ModItems.SPELL_CAUTERIZE, ModItems.SPELL_HEAT_WAVE,
                ModItems.SPELL_FROSTBOLT, ModItems.SPELL_ICE_SPIKE, ModItems.SPELL_FROST_NOVA,
                ModItems.SPELL_GLACIAL_STORM, ModItems.SPELL_FROST_STEP, ModItems.SPELL_FROZEN_GROUND,
                ModItems.SPELL_RAY_OF_FROST, ModItems.SPELL_ICE_LANCE,
                ModItems.SPELL_LIGHTNING_BOLT, ModItems.SPELL_SPARK_BURST, ModItems.SPELL_CHAIN_LIGHTNING,
                ModItems.SPELL_SHOCK, ModItems.SPELL_THUNDER_STEP, ModItems.SPELL_STORM_CLOUD,
                ModItems.SPELL_STATIC_FIELD, ModItems.SPELL_LIGHTNING_LANCE,
                ModItems.SPELL_BLOOD_STEP, ModItems.SPELL_LIFEDRAIN, ModItems.SPELL_HEARTSTOP,
                ModItems.SPELL_BLOOD_SPEAR, ModItems.SPELL_SANGUINE_BIND, ModItems.SPELL_CRIMSON_MIST,
                ModItems.SPELL_VAMPIRIC_TOUCH, ModItems.SPELL_BLOOD_PACT,
                ModItems.SPELL_VOID_TENTACLE, ModItems.SPELL_MIND_SPIKE, ModItems.SPELL_ELDRITCH_BLAST,
                ModItems.SPELL_SOUL_TEAR, ModItems.SPELL_MADNESS_WAVE, ModItems.SPELL_COSMIC_VOID,
                ModItems.SPELL_GREATER_HEAL, ModItems.SPELL_SMITE, ModItems.SPELL_DIVINE_LIGHT,
                ModItems.SPELL_SUN_STRIKE, ModItems.SPELL_HOLY_LANCE, ModItems.SPELL_HEALING_AURA,
                ModItems.SPELL_SACRED_GROUND, ModItems.SPELL_JUDGMENT,
                ModItems.SPELL_VINE_TANGLE, ModItems.SPELL_EARTH_WALL, ModItems.SPELL_STONE_SHARD,
                ModItems.SPELL_WISPS_HEAL, ModItems.SPELL_ROOTS, ModItems.SPELL_BRAMBLE_STORM,
                ModItems.SPELL_MAGIC_MISSILE, ModItems.SPELL_BONE_SPEAR, ModItems.SPELL_MAGIC_SHIELD,
                ModItems.SPELL_SUMMON_VEX,
                // r172 acessíveis
                ModItems.SPELL_AIR_DASH, ModItems.SPELL_FEATHER_GRACE, ModItems.SPELL_GUST_LEAP,
                ModItems.SPELL_SHADOW_STEP, ModItems.SPELL_SWIFT_CURRENT, ModItems.SPELL_STONE_SKIN,
                ModItems.SPELL_EMBER_SPARK, ModItems.SPELL_FROST_TOUCH, ModItems.SPELL_STATIC_JOLT,
                ModItems.SPELL_MINOR_HEAL, ModItems.SPELL_THORN_WHIP, ModItems.SPELL_GLIDE,
                ModItems.SPELL_BLINK_SHORT, ModItems.SPELL_WARMTH, ModItems.SPELL_NIMBLE_REFLEXES,
                // r172 scrolls de combate
                ModItems.SPELL_ARC_BOLT, ModItems.SPELL_CINDER_BURST, ModItems.SPELL_FROST_SHARD,
                ModItems.SPELL_THUNDER_CLAP, ModItems.SPELL_VENOM_SPIT, ModItems.SPELL_BLOOD_LASH,
                ModItems.SPELL_MINOR_SMITE, ModItems.SPELL_VOID_GRIP,
        };
        for (RegistryObject<Item> ro : all) {
            event.register((stack, layer) -> UniversalSpellScrollItem.tintColor(stack, layer),
                    ro.get());
        }

        // r153: Factory Spell Scroll — tint pela school do spell embarcado em NBT
        event.register((stack, layer) -> {
            String id = br.com.murilo.liberthia.magic.factory.DynamicSpellItem.spellId(stack);
            if (id == null) return 0xFFFFFFFF;
            br.com.murilo.liberthia.magic.spell.SpellDef d = br.com.murilo.liberthia.magic.spell.SpellLibrary.get(id);
            if (d == null) return 0xFFFFFFFF;
            // Aplica color tint só no layer 0 (textura principal)
            if (layer == 0) {
                // RGB do school, alpha 0xFF
                return 0xFF000000 | d.school.colorHex();
            }
            return 0xFFFFFFFF;
        }, ModItems.FACTORY_SPELL_SCROLL.get());
    }
}
