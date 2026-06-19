package br.com.murilo.liberthia.magic.orb;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * r174: <b>Infusor de Orbs</b> — mapeia itens RAROS → efeito do orb. Coloque até
 * 5 itens raros no Infusor e ele forja um <b>Orb Customizado</b> que concede
 * TODOS os efeitos correspondentes (real, não fictício) ao ser usado.
 *
 * <p>Itens mais raros = efeitos mais fortes (Nether Star, Echo Shard, Totem,
 * Maçã Encantada...). Quando dois itens dão o mesmo efeito, fica o maior nível.
 * Os detalhes ficam escritos no tooltip do orb.
 */
public final class OrbIngredients {

    private OrbIngredients() {}

    public record OrbEffect(MobEffect effect, int amp, String label) {}

    public static final Map<Item, OrbEffect> MAP = new LinkedHashMap<>();
    public static final String NBT_FX = "orb_fx";

    private static void put(Item it, MobEffect e, int amp, String label) {
        MAP.put(it, new OrbEffect(e, amp, label));
    }

    static {
        put(Items.NETHER_STAR,             MobEffects.DAMAGE_RESISTANCE, 2, "Resistência III");
        put(Items.ECHO_SHARD,              MobEffects.DAMAGE_BOOST,      2, "Força III");
        put(Items.ENCHANTED_GOLDEN_APPLE,  MobEffects.REGENERATION,      2, "Regeneração III");
        put(Items.GOLDEN_APPLE,            MobEffects.REGENERATION,      0, "Regeneração I");
        put(Items.TOTEM_OF_UNDYING,        MobEffects.ABSORPTION,        2, "Absorção III");
        put(Items.NETHERITE_INGOT,         MobEffects.DAMAGE_BOOST,      1, "Força II");
        put(Items.DIAMOND_BLOCK,           MobEffects.HEALTH_BOOST,      2, "Vida Extra III");
        put(Items.AMETHYST_SHARD,          MobEffects.HEALTH_BOOST,      0, "Vida Extra I");
        put(Items.HEART_OF_THE_SEA,        MobEffects.CONDUIT_POWER,     0, "Poder do Conduíte");
        put(Items.DRAGON_BREATH,           MobEffects.FIRE_RESISTANCE,   0, "Resist. ao Fogo");
        put(Items.PHANTOM_MEMBRANE,        MobEffects.SLOW_FALLING,      0, "Queda Lenta");
        put(Items.GHAST_TEAR,              MobEffects.REGENERATION,      1, "Regeneração II");
        put(Items.PRISMARINE_CRYSTALS,     MobEffects.WATER_BREATHING,   0, "Respiração Aquática");
        put(Items.ENDER_EYE,               MobEffects.NIGHT_VISION,      0, "Visão Noturna");
        put(Items.GLOWSTONE,               MobEffects.DIG_SPEED,         1, "Pressa II");
        put(Items.RABBIT_FOOT,             MobEffects.JUMP,              2, "Super Salto III");
        put(Items.SHULKER_SHELL,           MobEffects.DAMAGE_RESISTANCE, 0, "Resistência I");
        put(Items.EXPERIENCE_BOTTLE,       MobEffects.LUCK,              1, "Sorte II");
    }

    public static OrbEffect lookup(Item it) { return MAP.get(it); }

    /** Monta a lista NBT de efeitos a partir de até 5 stacks de input. */
    public static ListTag build(List<ItemStack> inputs) {
        Map<MobEffect, Integer> best = new LinkedHashMap<>();
        Map<MobEffect, String> labels = new LinkedHashMap<>();
        for (ItemStack s : inputs) {
            if (s == null || s.isEmpty()) continue;
            OrbEffect oe = MAP.get(s.getItem());
            if (oe == null) continue;
            int prev = best.getOrDefault(oe.effect(), -1);
            if (oe.amp() > prev) {
                best.put(oe.effect(), oe.amp());
                labels.put(oe.effect(), oe.label());
            }
        }
        ListTag list = new ListTag();
        for (var e : best.entrySet()) {
            ResourceLocation rl = ForgeRegistries.MOB_EFFECTS.getKey(e.getKey());
            if (rl == null) continue;
            CompoundTag c = new CompoundTag();
            c.putString("id", rl.toString());
            c.putInt("amp", e.getValue());
            c.putString("label", labels.get(e.getKey()));
            list.add(c);
        }
        return list;
    }

    /** Aplica os efeitos do orb (ao usar). */
    public static void apply(LivingEntity e, ListTag fx, int duration) {
        for (int i = 0; i < fx.size(); i++) {
            CompoundTag c = fx.getCompound(i);
            MobEffect eff = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(c.getString("id")));
            if (eff == null) continue;
            e.addEffect(new MobEffectInstance(eff, duration, c.getInt("amp"), true, true, true));
        }
    }
}
