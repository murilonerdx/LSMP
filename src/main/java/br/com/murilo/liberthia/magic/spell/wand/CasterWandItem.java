package br.com.murilo.liberthia.magic.spell.wand;

import br.com.murilo.liberthia.magic.spell.CastContext;
import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellLevels;
import br.com.murilo.liberthia.magic.spell.SpellLibrary;
import br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem;
import br.com.murilo.liberthia.magic.spell.composition.SpellComposition;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.162 r138: <b>CasterWandItem</b> — wand customizável que bind 1 spell
 * via Spell Parchment ou via scroll na offhand.
 *
 * <p>Diferenças vs UniversalSpellScrollItem:
 * <ul>
 *   <li><b>Durabilidade alta</b> (1000 vs 40) — pra ser arma principal de mago</li>
 *   <li><b>Sem spell hard-coded</b> — vazio até bind</li>
 *   <li><b>Bind via shift+right-click</b> com scroll na offhand</li>
 *   <li><b>Bind via Spell Parchment</b> imprintado também funciona</li>
 *   <li><b>−20% mana cost</b> comparado ao scroll equivalente (recompensa equipamento)</li>
 *   <li><b>Cooldown desbloqueado por item</b> (não compartilha com scrolls)</li>
 * </ul>
 *
 * <p>Visual: foil quando bound, displayname inclui spell name.
 */
public class CasterWandItem extends Item {

    public static final String NBT_SPELL_ID = "liberthia.bound_spell";
    public static final float MANA_DISCOUNT = 0.80F; // −20%

    public CasterWandItem(Properties props) {
        super(props.stacksTo(1).durability(1000).rarity(Rarity.RARE));
    }

    public static String boundSpellId(ItemStack stack) {
        // r138: Composition tem prioridade sobre spell_id base
        var comp = SpellComposition.fromStack(stack);
        if (comp != null) return comp.baseSpellId;
        if (stack.hasTag() && stack.getTag().contains(NBT_SPELL_ID)) {
            return stack.getTag().getString(NBT_SPELL_ID);
        }
        return null;
    }

    public static SpellDef boundDef(ItemStack stack) {
        var comp = SpellComposition.fromStack(stack);
        if (comp != null) return comp.toVirtualDef();
        String id = boundSpellId(stack);
        return id == null ? null : SpellLibrary.get(id);
    }

    public static void bind(ItemStack wand, String spellId) {
        wand.getOrCreateTag().putString(NBT_SPELL_ID, spellId);
    }

    public static void clearBind(ItemStack wand) {
        if (wand.hasTag()) {
            wand.getTag().remove(NBT_SPELL_ID);
            wand.getTag().remove(SpellComposition.NBT_COMPOSITION);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        SpellDef d = boundDef(stack);
        if (d == null) return super.getName(stack);
        return Component.literal("Varinha · ")
                .append(d.displayName());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return boundSpellId(stack) != null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack wand = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(wand);
        }

        // r138: Shift = bind/clear mode
        if (player.isShiftKeyDown()) {
            return handleBindMode(sp, wand, hand);
        }

        // Cast mode
        SpellDef d = boundDef(wand);
        if (d == null) {
            sp.displayClientMessage(Component.literal(
                "§e⚠ Varinha vazia — shift+right-click com scroll/parchment na offhand pra bindar"), true);
            return InteractionResultHolder.fail(wand);
        }

        // Cooldown próprio
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(wand);

        // Mana cost com 20% discount + armor bonus + level scaling
        int spellLevel = SpellLevels.getLevel(wand);
        float globalMult = 0.70F; // r117 base discount
        float armorMult = br.com.murilo.liberthia.magic.armor.ManaArmorEffects.costMultiplierFor(sp);
        float wandMult = MANA_DISCOUNT;
        int finalMana = (int)(d.manaCost * globalMult * armorMult * wandMult
                * SpellLevels.manaMultiplier(spellLevel));
        int finalCd = (int)(d.cooldownTicks * SpellLevels.cooldownMultiplier(spellLevel));

        int sourceCur = br.com.murilo.liberthia.observation.source.SourceData.get(sp);
        if (sourceCur < finalMana) {
            sp.displayClientMessage(Component.literal(
                "§c⚠ Source insuficiente §7(" + sourceCur + "/" + finalMana + ")"), true);
            return InteractionResultHolder.fail(wand);
        }

        // Execute spell
        ServerLevel sl = sp.serverLevel();
        SpellDef levDef = SpellDef.builder(d.id)
                .name(d.name).school(d.school).rarity(d.rarity)
                .mana(finalMana).cooldown(finalCd)
                .damage(d.damage * SpellLevels.damageMultiplier(spellLevel))
                .range(d.range * SpellLevels.rangeMultiplier(spellLevel))
                .lore(d.lore).cast(d.cast).build();
        CastContext ctx = new CastContext(sp, sl, hand, wand, levDef);
        boolean ok;
        try {
            ok = levDef.cast.execute(ctx);
            // Composition post-cast (se a varinha tem composition imprintada)
            var comp = SpellComposition.fromStack(wand);
            if (ok && comp != null && comp.isComposed()) {
                br.com.murilo.liberthia.magic.spell.composition.CompositionEffectApplier
                        .applyPostCast(ctx, comp);
                LivingEntity tgt = ctx.pickTarget(Math.min(levDef.range, 24));
                if (tgt != null) {
                    br.com.murilo.liberthia.magic.spell.composition.CompositionEffectApplier
                            .applyOnHit(ctx, tgt, comp);
                }
            }
        } catch (Throwable t) {
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.warn(
                "[CasterWand] cast error for {}: {}", d.id, t.toString());
            ok = false;
        }
        if (!ok) return InteractionResultHolder.fail(wand);

        sp.getCooldowns().addCooldown(this, finalCd);
        br.com.murilo.liberthia.observation.source.SourceData.consume(sp, finalMana);
        wand.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        sl.playSound(null, sp.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.3F);
        return InteractionResultHolder.success(wand);
    }

    /** Shift+right-click: bind/unbind. */
    private InteractionResultHolder<ItemStack> handleBindMode(
            ServerPlayer sp, ItemStack wand, InteractionHand hand) {
        InteractionHand off = (hand == InteractionHand.MAIN_HAND)
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack offStack = sp.getItemInHand(off);

        // Offhand empty + shift = clear bind
        if (offStack.isEmpty()) {
            if (boundSpellId(wand) == null) {
                sp.displayClientMessage(Component.literal(
                    "§7Varinha já está vazia"), true);
                return InteractionResultHolder.fail(wand);
            }
            clearBind(wand);
            sp.displayClientMessage(Component.literal(
                "§e✓ Bind removido — varinha agora está vazia"), true);
            return InteractionResultHolder.success(wand);
        }

        // Offhand = scroll → copia bind
        if (offStack.getItem() instanceof UniversalSpellScrollItem scroll) {
            String spellId = scroll.spellId(offStack);
            if (spellId == null) return InteractionResultHolder.fail(wand);
            bind(wand, spellId);
            // Copia composition se houver
            var comp = SpellComposition.fromStack(offStack);
            if (comp != null) comp.writeToStack(wand);
            else if (wand.hasTag()) wand.getTag().remove(SpellComposition.NBT_COMPOSITION);
            SpellDef d = SpellLibrary.get(spellId);
            String name = d == null ? spellId : d.displayName().getString();
            sp.displayClientMessage(Component.literal(
                "§a✓ Bindou §e" + name + "§a na varinha"), true);
            return InteractionResultHolder.success(wand);
        }

        // Offhand = parchment imprintado → bind via primeira metadata (próximo escopo)
        // Pattern: SpellParchmentItem armazena recipe AN-style — varinha não consome
        // ainda. Por enquanto exige scroll.
        if (offStack.getItem() instanceof br.com.murilo.liberthia.observation.item.SpellParchmentItem) {
            sp.displayClientMessage(Component.literal(
                "§e⚠ Parchments AN ainda não vinculam direto — use um Spell Scroll"), true);
            return InteractionResultHolder.fail(wand);
        }

        sp.displayClientMessage(Component.literal(
            "§e⚠ Offhand precisa ter Spell Scroll pra bindar"), true);
        return InteractionResultHolder.fail(wand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        SpellDef d = boundDef(stack);
        if (d == null) {
            tooltip.add(Component.literal("§7Varinha vazia").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("§8§oShift+right-click com scroll na offhand"));
            tooltip.add(Component.literal("§8§opra bindar um feitiço"));
            return;
        }
        tooltip.add(d.schoolTooltip());
        tooltip.add(d.statsTooltip());
        tooltip.add(d.loreTooltip());
        // Composition info
        var comp = SpellComposition.fromStack(stack);
        if (comp != null && comp.isComposed()) {
            tooltip.add(Component.literal(""));
            tooltip.addAll(comp.tooltip());
        }
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§a§l−20% Source cost§r §7(bônus da varinha)"));
        tooltip.add(Component.literal("§8§oShift+right-click pra trocar feitiço"));
    }
}
