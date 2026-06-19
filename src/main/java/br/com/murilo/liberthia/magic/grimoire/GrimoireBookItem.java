package br.com.murilo.liberthia.magic.grimoire;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.factory.DynamicSpellItem;
import br.com.murilo.liberthia.magic.modifier.CooldownReductionGlyphItem;
import br.com.murilo.liberthia.magic.spell.CastContext;
import br.com.murilo.liberthia.magic.spell.MagicSounds;
import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellLibrary;
import br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem;
import br.com.murilo.liberthia.observation.source.MagicLevelData;
import br.com.murilo.liberthia.observation.source.SourceData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/**
 * v0.1.151 r119: <b>GrimoireBookItem</b> — livro que armazena 9 spell scrolls.
 *
 * <p>Mecânica:
 * <ul>
 *   <li>Right-click no chão → abre GUI com 9 slots</li>
 *   <li>Player coloca scrolls dentro</li>
 *   <li>O slot ATIVO (selecionado via número 1-9 ou GUI) é castado quando
 *       player segura o Grimoire e right-clicka</li>
 *   <li>Sneak+right-click → abre GUI (alternativa)</li>
 * </ul>
 *
 * <p>NBT layout:
 * <pre>
 *   liberthia.grimoire: {
 *     Items: [{Slot:0, ...scroll1}, {Slot:1, ...scroll2}],
 *     active_slot: 0
 *   }
 * </pre>
 */
public class GrimoireBookItem extends Item implements MenuProvider {

    public static final String NBT_GRIMOIRE = "liberthia.grimoire";
    public static final String NBT_ACTIVE_SLOT = "active_slot";
    public static final int SCROLL_SLOTS = 9;

    public GrimoireBookItem(Properties props) {
        super(props);
    }

    public static int getActiveSlot(ItemStack stack) {
        if (!stack.hasTag()) return 0;
        CompoundTag root = stack.getTag().getCompound(NBT_GRIMOIRE);
        if (!root.contains(NBT_ACTIVE_SLOT)) return 0;
        int slot = root.getInt(NBT_ACTIVE_SLOT);
        return Math.max(0, Math.min(SCROLL_SLOTS - 1, slot));
    }

    public static void setActiveSlot(ItemStack stack, int slot) {
        CompoundTag root = stack.getOrCreateTagElement(NBT_GRIMOIRE);
        root.putInt(NBT_ACTIVE_SLOT, Math.max(0, Math.min(SCROLL_SLOTS - 1, slot)));
    }

    /** Pega o scroll no slot ativo (ItemStack, possivelmente vazio). */
    public static ItemStack getActiveScroll(ItemStack grimoire) {
        var inv = GrimoireInventory.from(grimoire);
        return inv.getStackInSlot(getActiveSlot(grimoire));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack grimoire = player.getItemInHand(hand);

        // Sneak+right-click → abre GUI (não casta)
        if (player.isShiftKeyDown()) {
            if (!(level.isClientSide) && player instanceof ServerPlayer sp) {
                NetworkHooks.openScreen(sp, this, buf -> buf.writeBoolean(hand == InteractionHand.MAIN_HAND));
            }
            return InteractionResultHolder.success(grimoire);
        }

        // Casta o scroll ativo
        ItemStack activeScroll = getActiveScroll(grimoire);
        if (activeScroll.isEmpty() || !GrimoireInventory.isSpellScroll(activeScroll)) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.literal("§c⚠ Nenhum feitiço no slot ativo. Use Shift+Click pra abrir."), true);
            }
            return InteractionResultHolder.fail(grimoire);
        }

        // r164: dispatch direto — instant cast pelos dois tipos de scroll (Universal + Dynamic).
        // (Não usa channel/use-item porque player.getItemInHand é o GRIMOIRE, não o scroll;
        // chamar scroll.use() iria ler NBT do grimoire e quebrar pra factory_spell_scroll.)
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(grimoire);
        }

        castSpellFromGrimoire(sp, grimoire, activeScroll, hand);

        // Salva o scroll de volta (durability, etc.) caso tenha sido modificado
        var inv = GrimoireInventory.from(grimoire);
        inv.setStackInSlot(getActiveSlot(grimoire), activeScroll);
        inv.saveTo(grimoire);

        return InteractionResultHolder.success(grimoire);
    }

    /**
     * r164: cast unificado para UniversalSpellScrollItem (spells prontos como
     * great_spell, fireball) e DynamicSpellItem (factory_spell_scroll). Instant
     * cast — pula a fase de channel porque o scroll não está na mão do player
     * (o grimoire está). Aplica todos os modificadores normais (CDR glyphs,
     * Magic Level cost reduction).
     */
    private void castSpellFromGrimoire(ServerPlayer sp, ItemStack grimoire,
                                       ItemStack scroll, InteractionHand hand) {
        // Resolve a SpellDef a partir do scroll
        SpellDef d = resolveSpellDef(scroll);
        if (d == null) {
            sp.displayClientMessage(Component.literal("§c⚠ Pergaminho com feitiço inválido")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        // r173: Cooldown POR FEITIÇO (não pelo item inteiro). Castar um feitiço
        // não trava os outros — trocar o slot ativo pra outro permite usá-lo já.
        if (GrimoireCooldowns.isOnCooldown(sp, d.id)) {
            int secs = Math.max(1, GrimoireCooldowns.remaining(sp, d.id) / 20);
            sp.displayClientMessage(Component.literal(
                    "§7⏳ ").append(d.displayName()).append(Component.literal(" §7em recarga (" + secs + "s)")), true);
            return;
        }

        // Mana / cooldown finais com modifiers (mesma fórmula que DynamicSpellItem.execute)
        float cdMult = CooldownReductionGlyphItem.cooldownMultiplier(sp);
        float manaMult = CooldownReductionGlyphItem.manaMultiplier(sp);
        float levelCostMult = MagicLevelData.getCostMult(sp);
        int finalMana = Math.round(d.manaCost * manaMult * levelCostMult);
        int finalCd = Math.round(d.cooldownTicks * cdMult);

        int source = SourceData.get(sp);
        if (source < finalMana) {
            sp.displayClientMessage(Component.literal(
                    "§c⚠ Source insuficiente §7(" + source + "/" + finalMana + ")"), true);
            MagicSounds.playCastCancel(sp);
            return;
        }

        MagicSounds.playCastFinish(sp, d.school);

        // r164: spell power dos magic accessories também aplica ao cast via grimoire
        float powerMult = br.com.murilo.liberthia.magic.accessory.MagicAccessoryEffects.damageMultiplier(sp);
        SpellDef poweredDef = (powerMult == 1.0F) ? d
                : SpellDef.builder(d.id)
                        .name(d.name).school(d.school).rarity(d.rarity)
                        .mana(d.manaCost).cooldown(d.cooldownTicks)
                        .damage(d.damage * powerMult).range(d.range)
                        .lore(d.lore).cast(d.cast).build();

        ServerLevel sl = sp.serverLevel();
        CastContext ctx = new CastContext(sp, sl, hand, scroll, poweredDef);
        boolean ok;
        try {
            ok = poweredDef.cast.execute(ctx);
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[Grimoire] cast error {}: {}", d.id, t.toString(), t);
            ok = false;
        }
        if (!ok) {
            MagicSounds.playCastCancel(sp);
            return;
        }

        GrimoireCooldowns.set(sp, d.id, finalCd);
        SourceData.forceConsume(sp, finalMana);
        MagicLevelData.recordSpellUse(sp, d.id);

        LiberthiaMod.LOGGER.info("[Grimoire] {} cast by {} (slot {}) — mana {}, cd {}t",
                d.id, sp.getName().getString(), getActiveSlot(grimoire), finalMana, finalCd);
    }

    /** Resolve a SpellDef dos dois tipos de scroll suportados. */
    private static SpellDef resolveSpellDef(ItemStack scroll) {
        if (scroll.getItem() instanceof UniversalSpellScrollItem usi) {
            return usi.def(scroll);
        }
        if (scroll.getItem() instanceof DynamicSpellItem) {
            String id = DynamicSpellItem.spellId(scroll);
            if (id == null) return null;
            return SpellLibrary.get(id);
        }
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.liberthia.grimoire");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // Acha o grimoire na mão do player
        ItemStack grimoire = player.getMainHandItem().getItem() instanceof GrimoireBookItem
                ? player.getMainHandItem() : player.getOffhandItem();
        return new GrimoireMenu(id, inv, grimoire);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        var inv = GrimoireInventory.from(stack);
        int filled = 0;
        for (int i = 0; i < SCROLL_SLOTS; i++) if (!inv.getStackInSlot(i).isEmpty()) filled++;

        tooltip.add(Component.literal("§7Slots: §e" + filled + "§7/§e" + SCROLL_SLOTS));
        int active = getActiveSlot(stack);
        ItemStack activeScroll = inv.getStackInSlot(active);
        if (!activeScroll.isEmpty()) {
            tooltip.add(Component.literal("§7Ativo: ").append(activeScroll.getHoverName()));
        } else {
            tooltip.add(Component.literal("§7Ativo: §8(vazio)"));
        }
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§7§oRight-click: castar §6" + (active + 1)));
        tooltip.add(Component.literal("§7§oSneak+Right-click: abrir"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
