package br.com.murilo.liberthia.compat;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integração opcional com Curios API.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CuriosCompat {

    private static final Logger LOG = LoggerFactory.getLogger("liberthia/curios");
    private static final String CURIOS_MODID = "curios";

    private static Boolean cachedLoaded;

    private CuriosCompat() {}

    public static boolean isCuriosLoaded() {
        if (cachedLoaded == null) {
            cachedLoaded = ModList.get().isLoaded(CURIOS_MODID);
            if (cachedLoaded) LOG.info("Curios detected — items equipáveis ativos.");
            else LOG.info("Curios não presente — items só funcionam no inventário.");
        }
        return cachedLoaded;
    }

    @SubscribeEvent
    public static void onAttachItemCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        if (!isCuriosLoaded()) return;
        ItemStack stack = event.getObject();
        try {
            if (stack.is(ModItems.CONTAINMENT_GLOVE.get())) {
                CuriosBridge.attach(event, stack);
            } else if (stack.is(ModItems.WHITE_MATTER_PENDANT.get())) {
                CuriosBridge.attachPendant(event, stack);
            } else if (stack.is(ModItems.REFINED_CONTAINMENT_PENDANT.get())) {
                CuriosBridge.attachRefinedPendant(event, stack);
            } else if (stack.is(ModItems.REFINED_CONTAINMENT_GLOVE.get())) {
                CuriosBridge.attachRefinedGlove(event, stack);
            } else if (stack.is(ModItems.DARK_MATTER_PENDANT.get())) {
                CuriosBridge.attachDarkMatterPendant(event, stack);
            } else if (stack.is(ModItems.CLEAR_MATTER_PENDANT.get())) {
                CuriosBridge.attachClearMatterPendant(event, stack);
            } else if (stack.is(ModItems.YELLOW_MATTER_PENDANT.get())) {
                CuriosBridge.attachYellowMatterPendant(event, stack);
            } else if (stack.is(ModItems.RELIQUIA_PROTECAO_ASTARON.get())) {
                CuriosBridge.attachReliquiaAstaron(event, stack);
            } else if (stack.is(ModItems.PES_QUEIMANTES_ASTARON.get())) {
                CuriosBridge.attachPesQueimantes(event, stack);
            } else if (stack.is(ModItems.BOTAS_MERCURIAIS.get())) {
                CuriosBridge.attachBotasMercuriais(event, stack);
            }
            // r164: Magic accessories — qualquer MagicAccessoryItem ganha curio capability
            else if (stack.getItem() instanceof br.com.murilo.liberthia.magic.accessory.MagicAccessoryItem) {
                CuriosBridge.attachMagicAccessory(event, stack);
            }
            // r179: Affinity Rings — equipáveis no slot ring (antes só na tag, sem capability)
            else if (stack.getItem() instanceof br.com.murilo.liberthia.magic.affinity.AffinityRingItem) {
                CuriosBridge.attachMagicAccessory(event, stack);
            }
            // r179: Enchanter's Gauntlet — curio de mãos (efeitos no golpe forte quando equipada)
            else if (stack.is(ModItems.ENCHANTERS_GAUNTLET.get())) {
                CuriosBridge.attachMagicAccessory(event, stack);
            }
            // r179: Blood Pact Amulet — curio de necklace (+3 ATK / -4 MAX_HEALTH)
            else if (stack.is(ModItems.BLOOD_PACT_AMULET.get())) {
                CuriosBridge.attachBloodPact(event, stack);
            }
            // r180: artefatos divinos equipáveis (efeito vem do DivineArtifactHandler)
            else if (stack.is(ModItems.ETERNITY_CROWN.get()) || stack.is(ModItems.ASCENSION_SEAL.get())) {
                CuriosBridge.attachMagicAccessory(event, stack);
            }
            // r180: Selo Nulo (ward anti-M&A) equipável no colar
            else if (stack.is(ModItems.NULL_SEAL.get())) {
                CuriosBridge.attachMagicAccessory(event, stack);
            }
            // r195: Relíquias de Astaron — qualquer AstaronRelicItem ganha curio capability
            else if (stack.getItem() instanceof br.com.murilo.liberthia.item.AstaronRelicItem) {
                CuriosBridge.attach(event, stack);
            }
            // Punhos Celestiais — curio de mãos (buffa socos quando equipado)
            else if (stack.is(ModItems.CELESTIAL_FISTS.get())) {
                CuriosBridge.attachMagicAccessory(event, stack);
            }
        } catch (Throwable t) {
            LOG.warn("Falha ao anexar capability Curios: {}", t.getMessage());
        }
    }

    /** r195 — true se o jogador está com a relíquia (item) equipada num slot Curios. */
    public static boolean isWearing(Player player, net.minecraft.world.item.Item item) {
        if (!isCuriosLoaded()) return false;
        try { return !CuriosBridge.findByItem(player, item).isEmpty(); }
        catch (Throwable t) { return false; }
    }

    // ── Containment Glove ──
    public static ItemStack findEquippedGlove(Player player) {
        if (!isCuriosLoaded()) return ItemStack.EMPTY;
        try { return CuriosBridge.findGlove(player); }
        catch (Throwable t) { LOG.debug("findEquippedGlove falhou: {}", t.getMessage()); return ItemStack.EMPTY; }
    }

    // ── White Matter Pendant ──
    public static ItemStack findEquippedPendant(Player player) {
        if (!isCuriosLoaded()) return ItemStack.EMPTY;
        try { return CuriosBridge.findPendant(player); }
        catch (Throwable t) { LOG.debug("findEquippedPendant falhou: {}", t.getMessage()); return ItemStack.EMPTY; }
    }

    public static boolean isPendantActive(Player player) {
        ItemStack pendant = findEquippedPendant(player);
        if (pendant.isEmpty()) return false;
        return pendant.getMaxDamage() > 0 && pendant.getDamageValue() < pendant.getMaxDamage();
    }

    public static void damagePendant(Player player, int amount) {
        if (!isCuriosLoaded() || amount <= 0) return;
        try { CuriosBridge.damagePendant(player, amount); }
        catch (Throwable t) { LOG.debug("damagePendant falhou: {}", t.getMessage()); }
    }

    // ── Refined Artifacts ──
    public static ItemStack findEquippedRefinedPendant(Player player) {
        if (!isCuriosLoaded()) return ItemStack.EMPTY;
        try { return CuriosBridge.findRefinedPendant(player); }
        catch (Throwable t) { LOG.debug("findRefinedPendant falhou: {}", t.getMessage()); return ItemStack.EMPTY; }
    }

    public static ItemStack findEquippedRefinedGlove(Player player) {
        if (!isCuriosLoaded()) return ItemStack.EMPTY;
        try { return CuriosBridge.findRefinedGlove(player); }
        catch (Throwable t) { LOG.debug("findRefinedGlove falhou: {}", t.getMessage()); return ItemStack.EMPTY; }
    }

    public static boolean isRefinedPendantActive(Player player) {
        ItemStack p = findEquippedRefinedPendant(player);
        if (p.isEmpty()) return false;
        return p.getMaxDamage() > 0 && p.getDamageValue() < p.getMaxDamage();
    }

    public static boolean isRefinedGloveActive(Player player) {
        ItemStack g = findEquippedRefinedGlove(player);
        if (g.isEmpty()) return false;
        return g.getMaxDamage() > 0 && g.getDamageValue() < g.getMaxDamage();
    }

    public static void damageRefinedPendant(Player player, int amount) {
        if (!isCuriosLoaded() || amount <= 0) return;
        try { CuriosBridge.damageRefinedPendant(player, amount); }
        catch (Throwable t) { LOG.debug("damageRefinedPendant falhou: {}", t.getMessage()); }
    }

    public static void damageRefinedGlove(Player player, int amount) {
        if (!isCuriosLoaded() || amount <= 0) return;
        try { CuriosBridge.damageRefinedGlove(player, amount); }
        catch (Throwable t) { LOG.debug("damageRefinedGlove falhou: {}", t.getMessage()); }
    }

    // ── Dark/Clear/Yellow Matter Pendants (v1) ──
    public static ItemStack findEquippedDarkMatterPendant(Player player) {
        if (!isCuriosLoaded()) return ItemStack.EMPTY;
        try { return CuriosBridge.findDarkMatterPendant(player); }
        catch (Throwable t) { return ItemStack.EMPTY; }
    }
    public static ItemStack findEquippedClearMatterPendant(Player player) {
        if (!isCuriosLoaded()) return ItemStack.EMPTY;
        try { return CuriosBridge.findClearMatterPendant(player); }
        catch (Throwable t) { return ItemStack.EMPTY; }
    }
    public static ItemStack findEquippedYellowMatterPendant(Player player) {
        if (!isCuriosLoaded()) return ItemStack.EMPTY;
        try { return CuriosBridge.findYellowMatterPendant(player); }
        catch (Throwable t) { return ItemStack.EMPTY; }
    }

    public static boolean isDarkPendantActive(Player player) {
        ItemStack p = findEquippedDarkMatterPendant(player);
        if (p.isEmpty()) return false;
        return p.getMaxDamage() > 0 && p.getDamageValue() < p.getMaxDamage();
    }
    public static boolean isClearPendantActive(Player player) {
        ItemStack p = findEquippedClearMatterPendant(player);
        if (p.isEmpty()) return false;
        return p.getMaxDamage() > 0 && p.getDamageValue() < p.getMaxDamage();
    }
    public static boolean isYellowPendantActive(Player player) {
        ItemStack p = findEquippedYellowMatterPendant(player);
        if (p.isEmpty()) return false;
        return p.getMaxDamage() > 0 && p.getDamageValue() < p.getMaxDamage();
    }

    public static void damageDarkMatterPendant(Player player, int amount) {
        if (!isCuriosLoaded() || amount <= 0) return;
        try { CuriosBridge.damageDarkMatterPendant(player, amount); }
        catch (Throwable t) { LOG.debug("damageDarkMatterPendant falhou: {}", t.getMessage()); }
    }
    public static void damageClearMatterPendant(Player player, int amount) {
        if (!isCuriosLoaded() || amount <= 0) return;
        try { CuriosBridge.damageClearMatterPendant(player, amount); }
        catch (Throwable t) { LOG.debug("damageClearMatterPendant falhou: {}", t.getMessage()); }
    }
    public static void damageYellowMatterPendant(Player player, int amount) {
        if (!isCuriosLoaded() || amount <= 0) return;
        try { CuriosBridge.damageYellowMatterPendant(player, amount); }
        catch (Throwable t) { LOG.debug("damageYellowMatterPendant falhou: {}", t.getMessage()); }
    }

    // ── Astaron items (v1) ──
    public static ItemStack findEquippedReliquiaAstaron(Player player) {
        if (!isCuriosLoaded()) return ItemStack.EMPTY;
        try { return CuriosBridge.findReliquiaAstaron(player); }
        catch (Throwable t) { return ItemStack.EMPTY; }
    }

    public static ItemStack findEquippedPesQueimantes(Player player) {
        if (!isCuriosLoaded()) return ItemStack.EMPTY;
        try { return CuriosBridge.findPesQueimantes(player); }
        catch (Throwable t) { return ItemStack.EMPTY; }
    }

    public static ItemStack findEquippedBotasMercuriais(Player player) {
        if (!isCuriosLoaded()) return ItemStack.EMPTY;
        try { return CuriosBridge.findBotasMercuriais(player); }
        catch (Throwable t) { return ItemStack.EMPTY; }
    }

    public static boolean isBotasMercuriaisActive(Player player) {
        ItemStack b = findEquippedBotasMercuriais(player);
        if (b.isEmpty()) return false;
        return b.getMaxDamage() > 0 && b.getDamageValue() < b.getMaxDamage();
    }

    public static void damageBotasMercuriais(Player player, int amount) {
        if (!isCuriosLoaded() || amount <= 0) return;
        try { CuriosBridge.damageBotasMercuriais(player, amount); }
        catch (Throwable t) { LOG.debug("damageBotasMercuriais falhou: {}", t.getMessage()); }
    }

    /** v0.1.35: drena durabilidade dos Pés Queimantes equipados. */
    public static void damagePesQueimantes(Player player, int amount) {
        if (!isCuriosLoaded() || amount <= 0) return;
        try { CuriosBridge.damagePesQueimantes(player, amount); }
        catch (Throwable t) { LOG.debug("damagePesQueimantes falhou: {}", t.getMessage()); }
    }
}
