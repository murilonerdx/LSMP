package br.com.murilo.liberthia.admin.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Map;

/**
 * Helpers de serialização Gson + ItemStack/Player → JSON.
 *
 * <p>Não armazena state — métodos estáticos.
 */
public final class AdminApiSerializer {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private AdminApiSerializer() {}

    public static JsonObject playerSummary(ServerPlayer p) {
        JsonObject o = new JsonObject();
        o.addProperty("uuid", p.getUUID().toString());
        o.addProperty("name", p.getGameProfile().getName());
        o.addProperty("dimension", p.level().dimension().location().toString());
        o.addProperty("health", p.getHealth());
        o.addProperty("maxHealth", p.getMaxHealth());
        o.addProperty("food", p.getFoodData().getFoodLevel());
        o.addProperty("xp", p.totalExperience);
        o.addProperty("level", p.experienceLevel);
        o.addProperty("gameMode", p.gameMode.getGameModeForPlayer().getName());
        JsonObject pos = new JsonObject();
        pos.addProperty("x", p.getX());
        pos.addProperty("y", p.getY());
        pos.addProperty("z", p.getZ());
        pos.addProperty("yaw", p.getYRot());
        pos.addProperty("pitch", p.getXRot());
        o.add("position", pos);
        return o;
    }

    public static JsonObject itemStack(ItemStack stack) {
        JsonObject o = new JsonObject();
        if (stack.isEmpty()) {
            o.addProperty("empty", true);
            return o;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        o.addProperty("id", id == null ? "minecraft:air" : id.toString());
        o.addProperty("count", stack.getCount());
        o.addProperty("name", stack.getHoverName().getString());
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            o.addProperty("nbt", tag.toString());
        }
        // Enchantments expostos pra GUI
        Map<Enchantment, Integer> enchs = EnchantmentHelper.getEnchantments(stack);
        if (!enchs.isEmpty()) {
            JsonArray arr = new JsonArray();
            for (var e : enchs.entrySet()) {
                ResourceLocation eid = ForgeRegistries.ENCHANTMENTS.getKey(e.getKey());
                if (eid == null) continue;
                JsonObject eo = new JsonObject();
                eo.addProperty("id", eid.toString());
                eo.addProperty("level", e.getValue());
                arr.add(eo);
            }
            o.add("enchantments", arr);
        }
        return o;
    }

    public static JsonObject inventorySnapshot(ServerPlayer p) {
        Inventory inv = p.getInventory();
        JsonObject o = new JsonObject();
        o.addProperty("uuid", p.getUUID().toString());
        o.addProperty("name", p.getGameProfile().getName());
        // 36 main slots (hotbar 0-8, main 9-35)
        JsonArray main = new JsonArray();
        for (int i = 0; i < 36; i++) {
            JsonObject slot = itemStack(inv.getItem(i));
            slot.addProperty("slot", i);
            main.add(slot);
        }
        o.add("main", main);
        // Armor (4 slots)
        JsonArray armor = new JsonArray();
        for (int i = 0; i < inv.armor.size(); i++) {
            JsonObject slot = itemStack(inv.armor.get(i));
            slot.addProperty("slot", i);
            armor.add(slot);
        }
        o.add("armor", armor);
        // Offhand
        JsonObject offhand = itemStack(inv.offhand.get(0));
        offhand.addProperty("slot", 0);
        o.add("offhand", offhand);
        return o;
    }

    public static JsonObject serverInfo(MinecraftServer server) {
        JsonObject o = new JsonObject();
        o.addProperty("motd", server.getMotd());
        o.addProperty("tickCount", server.getTickCount());
        o.addProperty("playerCount", server.getPlayerCount());
        o.addProperty("maxPlayers", server.getMaxPlayers());
        o.addProperty("tps", Math.min(20, 1000.0 / Math.max(1, server.getAverageTickTime())));
        JsonArray dims = new JsonArray();
        for (ServerLevel level : server.getAllLevels()) {
            JsonObject d = new JsonObject();
            d.addProperty("id", level.dimension().location().toString());
            d.addProperty("dayTime", level.getDayTime());
            d.addProperty("loadedChunks", level.getChunkSource().getLoadedChunksCount());
            dims.add(d);
        }
        o.add("dimensions", dims);
        return o;
    }

    /** Lista todos items registrados (id + nome localizado). ~1800 items em servers cheios. */
    public static JsonArray allItems() {
        JsonArray arr = new JsonArray();
        for (var entry : ForgeRegistries.ITEMS.getEntries()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", entry.getKey().location().toString());
            try {
                o.addProperty("name", entry.getValue().getDescription().getString());
            } catch (Exception ex) {
                o.addProperty("name", entry.getKey().location().getPath());
            }
            arr.add(o);
        }
        return arr;
    }

    public static JsonArray allEnchantments() {
        JsonArray arr = new JsonArray();
        for (var entry : ForgeRegistries.ENCHANTMENTS.getEntries()) {
            JsonObject o = new JsonObject();
            ResourceLocation id = entry.getKey().location();
            Enchantment ench = entry.getValue();
            o.addProperty("id", id.toString());
            try {
                o.addProperty("name", net.minecraft.network.chat.Component
                        .translatable(ench.getDescriptionId()).getString());
            } catch (Exception ex) {
                o.addProperty("name", id.getPath());
            }
            o.addProperty("maxLevel", ench.getMaxLevel());
            o.addProperty("minLevel", ench.getMinLevel());
            o.addProperty("isCurse", ench.isCurse());
            o.addProperty("isTreasure", ench.isTreasureOnly());
            arr.add(o);
        }
        return arr;
    }

    /**
     * Lista TODOS os sounds registrados (vanilla + mods carregados).
     * Em servers com mods de música/ambient pode ter ~3000 entries.
     */
    public static JsonArray allSounds() {
        JsonArray arr = new JsonArray();
        for (var entry : ForgeRegistries.SOUND_EVENTS.getEntries()) {
            JsonObject o = new JsonObject();
            ResourceLocation id = entry.getKey().location();
            o.addProperty("id", id.toString());
            o.addProperty("name", id.getPath()); // não há translation key padronizada para sounds
            arr.add(o);
        }
        return arr;
    }

    /** Lista TODAS as particles (vanilla + mods). */
    public static JsonArray allParticles() {
        JsonArray arr = new JsonArray();
        for (var entry : ForgeRegistries.PARTICLE_TYPES.getEntries()) {
            JsonObject o = new JsonObject();
            ResourceLocation id = entry.getKey().location();
            o.addProperty("id", id.toString());
            o.addProperty("name", id.getPath());
            arr.add(o);
        }
        return arr;
    }

    /** Lista TODOS os MobEffects (vanilla + mods). */
    public static JsonArray allEffects() {
        JsonArray arr = new JsonArray();
        for (var entry : ForgeRegistries.MOB_EFFECTS.getEntries()) {
            JsonObject o = new JsonObject();
            ResourceLocation id = entry.getKey().location();
            o.addProperty("id", id.toString());
            try {
                o.addProperty("name", net.minecraft.network.chat.Component
                        .translatable(entry.getValue().getDescriptionId()).getString());
            } catch (Exception ex) {
                o.addProperty("name", id.getPath());
            }
            arr.add(o);
        }
        return arr;
    }

    /** Lista TODOS os EntityTypes (vanilla + mods). */
    public static JsonArray allEntities() {
        JsonArray arr = new JsonArray();
        for (var entry : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            JsonObject o = new JsonObject();
            ResourceLocation id = entry.getKey().location();
            o.addProperty("id", id.toString());
            try {
                o.addProperty("name", net.minecraft.network.chat.Component
                        .translatable(entry.getValue().getDescriptionId()).getString());
            } catch (Exception ex) {
                o.addProperty("name", id.getPath());
            }
            arr.add(o);
        }
        return arr;
    }

    /** Cria ItemStack a partir de id + count + lista de encantamentos. */
    public static ItemStack buildItem(String id, int count, JsonArray enchantments) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return ItemStack.EMPTY;
        var item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item, Math.max(1, Math.min(count, 99)));
        if (enchantments != null) {
            for (var el : enchantments) {
                JsonObject eo = el.getAsJsonObject();
                ResourceLocation eid = ResourceLocation.tryParse(eo.get("id").getAsString());
                if (eid == null) continue;
                Enchantment ench = ForgeRegistries.ENCHANTMENTS.getValue(eid);
                if (ench == null) continue;
                int lvl = eo.has("level") ? eo.get("level").getAsInt() : 1;
                stack.enchant(ench, Math.max(1, lvl));
            }
        }
        return stack;
    }

    public static JsonObject errorResponse(String msg) {
        JsonObject o = new JsonObject();
        o.addProperty("error", msg);
        return o;
    }

    public static JsonObject okResponse() {
        JsonObject o = new JsonObject();
        o.addProperty("ok", true);
        return o;
    }
}
