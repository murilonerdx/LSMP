package br.com.murilo.liberthia.magic.glyph;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.Random;

/**
 * v0.1.148 r116: <b>SpiritWorldLootHandler</b> — hook em {@link LivingDeathEvent}
 * que faz mobs mortos no Spirit World droparem Spirit Reagents.
 *
 * <h2>Drop table</h2>
 * <ul>
 *   <li>Mob comum (zombie/skeleton/spider/etc): 40% chance de dropar 1 reagent random</li>
 *   <li>Mob específico do Spirit World (loom, empty_man, observer): 100% chance + reagent específico do mob</li>
 *   <li>Killed by player only (anti-farm via spawner)</li>
 * </ul>
 *
 * <p>Original code. Uso simples de events Forge — boilerplate padrão.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SpiritWorldLootHandler {

    private static final Random RNG = new Random();

    private SpiritWorldLootHandler() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();
        if (!(killed.level() instanceof ServerLevel sl)) return;
        if (!killed.level().dimension().equals(SpiritDimension.SPIRIT_WORLD)) return;
        if (!(event.getSource().getEntity() instanceof Player)) return; // anti-farm

        String typeKey = net.minecraft.world.entity.EntityType.getKey(killed.getType()).getPath();

        // Drop específico por tipo
        RegistryObject<Item> specific = pickSpecific(typeKey);
        if (specific != null) {
            spawnDrop(sl, killed, specific.get(), 1 + RNG.nextInt(2));
            return;
        }

        // Drop random pra mob comum (40%)
        if (RNG.nextFloat() < 0.40F) {
            RegistryObject<Item> random = pickRandomReagent();
            if (random != null) {
                spawnDrop(sl, killed, random.get(), 1);
            }
        }
    }

    /** Mobs específicos têm drops temáticos. Retorna null se não tem associação. */
    private static RegistryObject<Item> pickSpecific(String entityKey) {
        return switch (entityKey) {
            case "empty_man" -> ModItems.PHANTOM_INK;
            case "observer" -> ModItems.MEMORY_SHARD;
            case "absence" -> ModItems.ECTOPLASM_STRAND;
            case "remembered" -> ModItems.PHANTOM_INK;
            case "weaving_shade", "loom_watcher", "loom_screamer", "loom_peripheral",
                 "loom_worm" -> ModItems.VEIL_FRAGMENT;
            case "pale_spirit", "shade_watcher" -> ModItems.WISP_ESSENCE;
            case "dark_consciousness" -> ModItems.ASTRAL_DUST;
            default -> null;
        };
    }

    private static RegistryObject<Item> pickRandomReagent() {
        RegistryObject<Item>[] pool = new RegistryObject[]{
                ModItems.WISP_ESSENCE,
                ModItems.ASTRAL_DUST,
                ModItems.WHISPERWOOD_RESIN,
                ModItems.MEMORY_SHARD,
                ModItems.ECTOPLASM_STRAND,
                ModItems.PHANTOM_INK,
                ModItems.VEIL_FRAGMENT
        };
        return pool[RNG.nextInt(pool.length)];
    }

    private static void spawnDrop(ServerLevel sl, LivingEntity killed, Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        ItemEntity drop = new ItemEntity(sl,
                killed.getX(), killed.getY() + 0.5, killed.getZ(), stack);
        drop.setDefaultPickUpDelay();
        sl.addFreshEntity(drop);
    }
}
