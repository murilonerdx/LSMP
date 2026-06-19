package br.com.murilo.liberthia.observation.perk;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * v0.1.22 r62: 6 perks foundationais.
 */
public final class Perks {

    private Perks() {}

    // 1. Source Regen — +1 source/4s per slotValue
    public static final RegenPerk REGEN = PerkRegistry.register(new RegenPerk());
    // 2. Sanity Shield — -25% sanity cost per slotValue
    public static final SanityShield SANITY_SHIELD = PerkRegistry.register(new SanityShield());
    // 3. Source Discount — -20% source cost per slotValue
    public static final SourceDiscount SOURCE_DISCOUNT = PerkRegistry.register(new SourceDiscount());
    // 4. Damage Boost — +15% damage per slotValue
    public static final DamageBoost DAMAGE_BOOST = PerkRegistry.register(new DamageBoost());
    // 5. Observation Speed — Slow Falling + Night Vision constant
    public static final ObservationSpeed OBS_SPEED = PerkRegistry.register(new ObservationSpeed());
    // 6. Glyph Master — Looking glass effect (see hp)
    public static final GlyphMaster GLYPH_MASTER = PerkRegistry.register(new GlyphMaster());

    public static class RegenPerk extends Perk implements Perk.TickablePerk {
        public RegenPerk() { super(new ResourceLocation(LiberthiaMod.MODID, "perk/regen"), "Regenerar"); }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7+1 Source a cada 4s por slot."));
        }
        @Override public void tickPerk(ItemStack stack, ServerPlayer p, int slotValue) {
            if (p.tickCount % 80 == 0) {
                br.com.murilo.liberthia.observation.source.SourceData.add(p, slotValue);
            }
        }
    }

    public static class SanityShield extends Perk implements Perk.SanityShieldPerk {
        public SanityShield() { super(new ResourceLocation(LiberthiaMod.MODID, "perk/sanity_shield"), "Escudo de Sanidade"); }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7-25% custo de sanidade por slot."));
        }
        @Override public int reduceSanityCost(int baseCost, int slotValue) {
            return Math.max(0, baseCost - (baseCost * 25 * slotValue / 100));
        }
    }

    public static class SourceDiscount extends Perk implements Perk.SourceCostPerk {
        public SourceDiscount() { super(new ResourceLocation(LiberthiaMod.MODID, "perk/source_discount"), "Economia de Source"); }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7-20% custo de Source por slot."));
        }
        @Override public int reduceCost(int baseCost, int slotValue) {
            return Math.max(0, baseCost - (baseCost * 20 * slotValue / 100));
        }
    }

    public static class DamageBoost extends Perk implements Perk.DamageMultPerk {
        public DamageBoost() { super(new ResourceLocation(LiberthiaMod.MODID, "perk/damage_boost"), "Reforço de Dano"); }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7+15% damage de spells por slot."));
        }
        @Override public float modifyDamage(float base, int slotValue) {
            return base * (1F + 0.15F * slotValue);
        }
    }

    public static class ObservationSpeed extends Perk implements Perk.TickablePerk {
        public ObservationSpeed() { super(new ResourceLocation(LiberthiaMod.MODID, "perk/obs_speed"), "Velocidade de Observação"); }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Slow Falling + Night Vision contínuos."));
        }
        @Override public void tickPerk(ItemStack stack, ServerPlayer p, int slotValue) {
            if (p.tickCount % 100 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, true, false));
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false));
            }
        }
    }

    public static class GlyphMaster extends Perk implements Perk.TickablePerk {
        public GlyphMaster() { super(new ResourceLocation(LiberthiaMod.MODID, "perk/glyph_master"), "Mestre dos Glyphs"); }
        @Override public List<Component> lore() {
            return List.of(Component.literal("§7Glowing em mobs atingidos +30s."));
        }
        @Override public void tickPerk(ItemStack stack, ServerPlayer p, int slotValue) {
            // Apply on attack hooked elsewhere via NBT flag
            if (p.tickCount % 200 == 0) {
                p.getPersistentData().putBoolean("liberthia.glyph_master_active", true);
            }
        }
    }
}
