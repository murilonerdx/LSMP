package br.com.murilo.liberthia.matter;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Registro estático de quanto de matéria cada item carrega.
 *
 * <p>Inicializado lazy na primeira consulta — registry de items pode não estar
 * pronto ainda durante mod construction.
 */
public final class MatterContentRegistry {

    private static final Map<Item, MatterContent> MAP = new HashMap<>();
    private static volatile boolean initialized = false;

    private MatterContentRegistry() {}

    public static MatterContent of(ItemStack stack) {
        if (stack.isEmpty()) return MatterContent.EMPTY;
        if (!initialized) initialize();
        return MAP.getOrDefault(stack.getItem(), MatterContent.EMPTY);
    }

    private static synchronized void initialize() {
        if (initialized) return;
        try {
            // Matéria escura pura
            put(ModBlocks.DARK_MATTER_BLOCK.get().asItem(), 100, 0, 0);
            put(ModItems.DARK_MATTER_SHARD.get(), 25, 0, 0);
            put(ModItems.DARK_MATTER_BUCKET.get(), 60, 0, 0);

            // Cadeia de refinação
            put(ModItems.INACTIVE_DARK_MATTER.get(), 40, 8, 0);
            put(ModItems.ACTIVE_DARK_MATTER.get(), 80, 0, 0);
            put(ModItems.DARK_MATTER_CATALYST.get(), 30, 5, 5);

            // Minérios
            put(ModBlocks.DARK_MATTER_ORE.get().asItem(), 20, 0, 0);
            put(ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get().asItem(), 25, 0, 0);

            // Matéria branca / clara
            putIfExists("white_matter_ore", 0, 18, 0);
            putIfExists("clear_matter_block", 0, 80, 0);

            // Matéria amarela
            putIfExists("yellow_matter_ingot", 0, 0, 30);
            putIfExists("yellow_matter_block", 0, 0, 100);

            // Lore items adicionados nesta turn
            putIfExists("horus_eye_shard", 90, 0, 0);
            putIfExists("equilibrium_crystal", 0, 50, 50);

            // Outros
            put(ModItems.HOLY_ESSENCE.get(), 0, 30, 0);

            // Matter swords — segurar na mão impregna o jogador com a matéria correspondente
            put(ModItems.DARK_MATTER_SWORD.get(),   40, 0, 0);
            put(ModItems.CLEAR_MATTER_SWORD.get(),  0, 40, 0);
            put(ModItems.YELLOW_MATTER_SWORD.get(), 0, 0, 40);

            // Buckets de matéria
            putIfExists("yellow_matter_bucket", 0, 0, 50);
            putIfExists("clear_matter_bucket", 0, 50, 0);

            // Armaduras (raros mas com matéria, em níveis menores)
            putIfExists("yellow_matter_helmet", 0, 0, 20);
            putIfExists("yellow_matter_chestplate", 0, 0, 25);
            putIfExists("yellow_matter_leggings", 0, 0, 20);
            putIfExists("yellow_matter_boots", 0, 0, 15);
        } catch (Exception ex) {
            // Items podem não estar todos prontos — o que registrou registrou.
        }
        initialized = true;
    }

    private static void put(Item item, float dm, float wm, float ym) {
        MAP.put(item, new MatterContent(dm, wm, ym));
    }

    private static void putIfExists(String name, float dm, float wm, float ym) {
        var ro = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new net.minecraft.resources.ResourceLocation("liberthia", name));
        if (ro != null) MAP.put(ro, new MatterContent(dm, wm, ym));
    }
}
