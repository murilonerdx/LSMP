package br.com.murilo.liberthia.magic.factory;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.spell.CastContext;
import br.com.murilo.liberthia.magic.spell.MagicSounds;
import br.com.murilo.liberthia.magic.spell.SpellChargeAura;
import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellLibrary;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r148: Item ÚNICO que pode segurar QUALQUER spell criada pela {@link SpellFactory}.
 *
 * <p>Diferente do {@link br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem}
 * que tem um {@code defaultSpellId} fixo no construtor, este item lê o ID via NBT.
 *
 * <p>Permite ter UM item registrado em {@link br.com.murilo.liberthia.registry.ModItems}
 * que serve TODAS as spells JSON-driven. Foi feito pra ser dado via comando
 * {@code /liberthia spell give <id>} ou recipe especial.
 *
 * <h2>NBT</h2>
 * <pre>{@code
 *   { "liberthia.factory_spell_id": "factory_fireball" }
 * }</pre>
 */
public class DynamicSpellItem extends Item {

    private static final String NBT_KEY = "liberthia.factory_spell_id";

    public DynamicSpellItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    /** Cria stack pra spell {@code id}. */
    public static ItemStack stackFor(Item item, String spellId) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString(NBT_KEY, spellId);
        return stack;
    }

    /** Retorna o id de spell embarcado, ou null. */
    public static String spellId(ItemStack stack) {
        if (!stack.hasTag()) return null;
        var tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_KEY)) return null;
        return tag.getString(NBT_KEY);
    }

    private SpellDef def(ItemStack stack) {
        String id = spellId(stack);
        if (id == null) return null;
        return SpellLibrary.get(id);
    }

    @Override
    public Component getName(ItemStack stack) {
        SpellDef d = def(stack);
        if (d == null) return Component.literal("§7Pergaminho Vazio");
        return d.displayName();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public int getUseDuration(ItemStack stack) {
        SpellDef d = def(stack);
        if (d == null) return 0;
        return Math.max(8, Math.min(40, d.manaCost / 3));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        SpellDef d = def(stack);
        if (d == null) {
            sp.displayClientMessage(Component.literal("§c⚠ Pergaminho sem spell").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }

        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        int source = br.com.murilo.liberthia.observation.source.SourceData.get(sp);
        if (source < d.manaCost) {
            sp.displayClientMessage(Component.literal(
                    "§c⚠ Source insuficiente §7(" + source + "/" + d.manaCost + ")"), true);
            return InteractionResultHolder.fail(stack);
        }

        MagicSounds.playCastStart(sp, d.school);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
        if (level.isClientSide || !(living instanceof ServerPlayer sp)) return;
        SpellDef d = def(stack);
        if (d == null) return;
        int total = getUseDuration(stack);
        int used = total - remaining;
        SpellChargeAura.tick(sp, d.school, used, total, sp.getUsedItemHand());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (level.isClientSide || !(living instanceof ServerPlayer sp)) return stack;
        SpellDef d = def(stack);
        if (d != null) execute(sp, stack, d);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide || !(living instanceof ServerPlayer sp)) return;
        int total = getUseDuration(stack);
        int used = total - timeLeft;
        if (used >= (int)(total * 0.9F)) {
            SpellDef d = def(stack);
            if (d != null) execute(sp, stack, d);
        } else {
            MagicSounds.playCastCancel(sp);
        }
    }

    private void execute(ServerPlayer sp, ItemStack stack, SpellDef d) {
        if (sp.getCooldowns().isOnCooldown(this)) {
            LiberthiaMod.LOGGER.debug("[DynamicSpell] {} on cooldown — skip", d.id);
            return;
        }

        // r154: Aplica CDR + cost multipliers dos glyphs no inventário
        float cdMult = br.com.murilo.liberthia.magic.modifier.CooldownReductionGlyphItem.cooldownMultiplier(sp);
        float manaMult = br.com.murilo.liberthia.magic.modifier.CooldownReductionGlyphItem.manaMultiplier(sp);
        // r157: Apply Magic Level cost reduction (5% per level, max -50% at level 10)
        float levelCostMult = br.com.murilo.liberthia.observation.source.MagicLevelData.getCostMult(sp);
        int finalMana = Math.round(d.manaCost * manaMult * levelCostMult);
        int finalCd = Math.round(d.cooldownTicks * cdMult);

        int source = br.com.murilo.liberthia.observation.source.SourceData.get(sp);
        if (source < finalMana) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§c⚠ Source insuficiente §7(" + source + "/" + finalMana + ")"), true);
            MagicSounds.playCastCancel(sp);
            return;
        }

        MagicSounds.playCastFinish(sp, d.school);

        // r164: spell power dos magic accessories — multiplica damage do def
        float powerMult = br.com.murilo.liberthia.magic.accessory.MagicAccessoryEffects.damageMultiplier(sp);
        SpellDef poweredDef = (powerMult == 1.0F) ? d
                : SpellDef.builder(d.id)
                        .name(d.name).school(d.school).rarity(d.rarity)
                        .mana(d.manaCost).cooldown(d.cooldownTicks)
                        .damage(d.damage * powerMult).range(d.range)
                        .lore(d.lore).cast(d.cast).build();

        ServerLevel sl = sp.serverLevel();
        CastContext ctx = new CastContext(sp, sl, sp.getUsedItemHand(), stack, poweredDef);
        boolean ok;
        try {
            ok = poweredDef.cast.execute(ctx);
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[DynamicSpell] cast error {}: {}", d.id, t.toString(), t);
            ok = false;
        }
        if (!ok) { MagicSounds.playCastCancel(sp); return; }

        // r153: SEMPRE cobra mana + cooldown após cast sucesso
        sp.getCooldowns().addCooldown(this, finalCd);
        br.com.murilo.liberthia.observation.source.SourceData.forceConsume(sp, finalMana);
        // r157: Magic Level — registra uso do spell (cada 30 usos = +20 kill credits)
        br.com.murilo.liberthia.observation.source.MagicLevelData.recordSpellUse(sp, d.id);
        LiberthiaMod.LOGGER.info("[DynamicSpell] {} by {} — mana {} (×{}), cd {}t (×{})",
                d.id, sp.getName().getString(), finalMana, manaMult, finalCd, cdMult);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        SpellDef d = def(stack);
        if (d == null) {
            tooltip.add(Component.literal("§7Use §e/liberthia spell give <id>§7 pra preencher"));
            return;
        }
        tooltip.add(d.schoolTooltip());
        tooltip.add(d.statsTooltip());
        tooltip.add(d.loreTooltip());
        tooltip.add(Component.literal("§8✦ Factory Spell"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        SpellDef d = def(stack);
        return d != null && d.rarity == Rarity.EPIC;
    }
}
