package br.com.murilo.liberthia.magic.thread;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
 * r172: <b>Tear de Threads</b> — mapeamento de 20 itens VANILLA → efeito de
 * thread. Coloque até 5 itens diferentes no Tear de Threads e ele monta uma
 * <b>Thread Customizada</b> que concede TODOS os efeitos correspondentes (real,
 * não fictício). Quando dois itens dão o mesmo efeito, fica o maior nível.
 *
 * <p>Itens mais raros = efeitos mais fortes (Nether Star, Echo Shard, Heart of
 * the Sea...). Os efeitos são aplicados passivamente enquanto a thread está no
 * inventário, e os detalhes são escritos no tooltip da thread.
 */
public final class ThreadIngredients {

    private ThreadIngredients() {}

    public record ThreadEffect(MobEffect effect, int amp, String label) {}

    /** Item vanilla → efeito concedido. */
    public static final Map<Item, ThreadEffect> MAP = new LinkedHashMap<>();

    private static void put(Item it, MobEffect e, int amp, String label) {
        MAP.put(it, new ThreadEffect(e, amp, label));
    }

    static {
        put(Items.SUGAR,               MobEffects.MOVEMENT_SPEED,   0, "Velocidade I");
        put(Items.RABBIT_FOOT,         MobEffects.JUMP,             1, "Super Salto II");
        put(Items.GLOWSTONE_DUST,      MobEffects.NIGHT_VISION,     0, "Visão Noturna");
        put(Items.BLAZE_POWDER,        MobEffects.FIRE_RESISTANCE,  0, "Resist. ao Fogo");
        put(Items.PHANTOM_MEMBRANE,    MobEffects.SLOW_FALLING,     0, "Queda Lenta");
        put(Items.GHAST_TEAR,          MobEffects.REGENERATION,     0, "Regeneração I");
        put(Items.GOLDEN_CARROT,       MobEffects.ABSORPTION,       0, "Absorção I");
        put(Items.IRON_INGOT,          MobEffects.DAMAGE_RESISTANCE,0, "Resistência I");
        put(Items.DIAMOND,             MobEffects.DAMAGE_BOOST,     0, "Força I");
        put(Items.EMERALD,             MobEffects.LUCK,             0, "Sorte");
        put(Items.MAGMA_CREAM,         MobEffects.DAMAGE_BOOST,     1, "Força II");
        put(Items.PRISMARINE_CRYSTALS, MobEffects.WATER_BREATHING,  0, "Respiração Aquática");
        put(Items.HEART_OF_THE_SEA,    MobEffects.CONDUIT_POWER,    0, "Poder do Conduíte");
        put(Items.NAUTILUS_SHELL,      MobEffects.DOLPHINS_GRACE,   0, "Graça do Golfinho");
        put(Items.HONEY_BOTTLE,        MobEffects.REGENERATION,     1, "Regeneração II");
        put(Items.NETHER_STAR,         MobEffects.DAMAGE_RESISTANCE,2, "Resistência III");
        put(Items.ENDER_PEARL,         MobEffects.MOVEMENT_SPEED,   1, "Velocidade II");
        put(Items.AMETHYST_SHARD,      MobEffects.HEALTH_BOOST,     0, "Vida Extra");
        put(Items.ECHO_SHARD,          MobEffects.DAMAGE_BOOST,     2, "Força III");
        put(Items.GOLD_INGOT,          MobEffects.DIG_SPEED,        0, "Pressa");
    }

    public static final String NBT_FX = "thread_fx";

    public static ThreadEffect lookup(Item it) { return MAP.get(it); }

    /** Monta a lista NBT de efeitos a partir de até 5 stacks de input. */
    public static ListTag build(List<ItemStack> inputs) {
        Map<MobEffect, Integer> best = new LinkedHashMap<>();
        Map<MobEffect, String> labels = new LinkedHashMap<>();
        for (ItemStack s : inputs) {
            if (s == null || s.isEmpty()) continue;
            ThreadEffect te = MAP.get(s.getItem());
            if (te == null) continue;
            int prev = best.getOrDefault(te.effect(), -1);
            if (te.amp() > prev) {
                best.put(te.effect(), te.amp());
                labels.put(te.effect(), te.label());
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

    public static boolean hasAnyMapped(List<ItemStack> inputs) {
        for (ItemStack s : inputs) if (s != null && !s.isEmpty() && MAP.containsKey(s.getItem())) return true;
        return false;
    }

    /** Aplica os efeitos da thread no ser vivo (passivo). */
    public static void apply(LivingEntity e, ListTag fx, int duration) {
        for (int i = 0; i < fx.size(); i++) {
            CompoundTag c = fx.getCompound(i);
            MobEffect eff = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(c.getString("id")));
            if (eff == null) continue;
            e.addEffect(new MobEffectInstance(eff, duration, c.getInt("amp"), true, false, true));
        }
    }
}
