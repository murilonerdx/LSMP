package br.com.murilo.liberthia.dimension;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r55 (r82 BULLETPROOF REWRITE): <b>Spirit World Death Handler</b>.
 *
 * <p>r82: agora salva os itens em <b>NBT persistente do player</b> (não mais
 * em map em memória), pra que sobrevivam:
 * <ul>
 *   <li>Logout / disconnect entre morte e respawn</li>
 *   <li>Server restart com player offline</li>
 *   <li>Crash recovery</li>
 * </ul>
 *
 * <h2>Fluxo</h2>
 * <ol>
 *   <li>{@code LivingDropsEvent} (HIGHEST) — captura drops, salva no NBT, cancela event</li>
 *   <li>{@code LivingExperienceDropEvent} — preserva XP também</li>
 *   <li>{@code PlayerEvent.Clone} — restaura tudo do NBT no novo player</li>
 * </ol>
 *
 * <p>NBT keys usadas:
 * <ul>
 *   <li>{@code liberthia.spirit_preserved_items} (ListTag) — stacks</li>
 *   <li>{@code liberthia.spirit_preserved_xp} (Int) — total XP</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SpiritWorldDeathHandler {

    public static final String NBT_ITEMS = "liberthia.spirit_preserved_items";
    public static final String NBT_XP = "liberthia.spirit_preserved_xp";

    private SpiritWorldDeathHandler() {}

    /**
     * Cancela drops e salva os items no NBT do player ANTES dele virar zombie.
     * HIGHEST priority pra rodar antes de outros handlers.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!SpiritDimension.isInSpiritWorld(player)) return;

        ListTag listTag = new ListTag();
        int savedCount = 0;
        for (var ie : event.getDrops()) {
            ItemStack stack = ie.getItem();
            if (stack.isEmpty()) continue;
            CompoundTag stackTag = new CompoundTag();
            stack.save(stackTag);
            listTag.add(stackTag);
            savedCount++;
        }

        if (savedCount > 0) {
            player.getPersistentData().put(NBT_ITEMS, listTag);
            LiberthiaMod.LOGGER.info("[SpiritDeath] saved {} stacks to NBT for {} (spirit death)",
                    savedCount, player.getName().getString());
        }
        // SEMPRE cancela drops em spirit, mesmo se vazio
        event.setCanceled(true);

        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5Seus itens foram §lpreservados§r§5 pelo Outro Lado."), false);
            sp.displayClientMessage(Component.literal(
                    "§7§oVoltam quando seu corpo se reformar."), false);
        }
    }

    /** Preserva XP — drop event normal seria perdido. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onXpDrop(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!SpiritDimension.isInSpiritWorld(player)) return;
        int xp = event.getDroppedExperience();
        if (xp > 0) {
            int totalXp = player.totalExperience + xp;
            player.getPersistentData().putInt(NBT_XP, totalXp);
        }
        event.setCanceled(true);
        event.setDroppedExperience(0);
    }

    /**
     * Restaura items + XP do NBT após respawn. Roda em PlayerEvent.Clone,
     * que é disparado quando o player respawna após morte.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        // O Forge não copia getPersistentData automaticamente em clone — precisamos pegar do old
        CompoundTag oldData = oldPlayer.getPersistentData();

        // (1) Restaura items
        if (oldData.contains(NBT_ITEMS)) {
            ListTag listTag = oldData.getList(NBT_ITEMS, Tag.TAG_COMPOUND);
            int restored = 0;
            for (int i = 0; i < listTag.size(); i++) {
                ItemStack stack = ItemStack.of(listTag.getCompound(i));
                if (stack.isEmpty()) continue;
                if (!newPlayer.getInventory().add(stack)) {
                    newPlayer.drop(stack, false);
                }
                restored++;
            }
            oldData.remove(NBT_ITEMS);
            if (restored > 0 && newPlayer instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal(
                        "§5§l✦ §r§5" + restored + " §5itens retornam do Outro Lado."), false);
            }
            LiberthiaMod.LOGGER.info("[SpiritDeath] restored {} stacks to {} on respawn",
                    restored, newPlayer.getName().getString());
        }

        // (2) Restaura XP
        if (oldData.contains(NBT_XP)) {
            int xp = oldData.getInt(NBT_XP);
            newPlayer.giveExperiencePoints(xp);
            oldData.remove(NBT_XP);
        }
    }

    /**
     * Hook adicional em LivingDeathEvent — caso o player morra fora do drop
     * pipeline (kill command, /kill, dimensional damage), ainda preserva.
     * HIGHEST priority pra rodar antes do drop handler.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!SpiritDimension.isInSpiritWorld(player)) return;
        // Pre-save do inventário completo pra garantir
        if (!player.getPersistentData().contains(NBT_ITEMS)) {
            ListTag listTag = new ListTag();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                CompoundTag stackTag = new CompoundTag();
                stack.save(stackTag);
                listTag.add(stackTag);
            }
            if (!listTag.isEmpty()) {
                player.getPersistentData().put(NBT_ITEMS, listTag);
                LiberthiaMod.LOGGER.info(
                        "[SpiritDeath] pre-saved inventory to NBT for {} (death fallback)",
                        player.getName().getString());
            }
        }
    }
}
