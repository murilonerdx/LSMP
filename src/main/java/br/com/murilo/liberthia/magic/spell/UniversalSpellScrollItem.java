package br.com.murilo.liberthia.magic.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.143 r111: <b>UniversalSpellScrollItem</b> — UMA classe que serve TODOS
 * os spells da {@link SpellLibrary}.
 *
 * <p><b>r146 — Channeled Cast (Iron's-style wind-up):</b> antes o cast era
 * instantâneo no right-click; agora o player precisa SEGURAR pra carregar
 * (8t-40t conforme mana custom). Durante o channel:
 * <ul>
 *   <li>{@link SpellChargeAura} spawna partículas orbitais na mão</li>
 *   <li>{@link MagicSounds#castStart} toca som de início</li>
 *   <li>Mana é validada na hora do START (não consumida ainda)</li>
 * </ul>
 *
 * <p>Quando o channel completa ({@link #finishUsingItem}):
 * <ul>
 *   <li>Mana é consumida</li>
 *   <li>Cooldown aplicado</li>
 *   <li>Lambda de spell executada</li>
 *   <li>{@link MagicSounds#castFinish} toca som de release</li>
 * </ul>
 *
 * <p>Se o player solta cedo ({@link #releaseUsing}): nada acontece, som
 * de fizzle, sem mana gasta. Combat feel muito melhor que instant-cast.
 */
public class UniversalSpellScrollItem extends Item {

    private final String defaultSpellId;

    public UniversalSpellScrollItem(Properties props, String defaultSpellId) {
        super(props.stacksTo(1).durability(40));
        this.defaultSpellId = defaultSpellId;
    }

    public String spellId(ItemStack stack) {
        var comp = br.com.murilo.liberthia.magic.spell.composition.SpellComposition.fromStack(stack);
        if (comp != null) return comp.baseSpellId;
        if (stack.hasTag() && stack.getTag().contains("liberthia.spell_id")) {
            return stack.getTag().getString("liberthia.spell_id");
        }
        return defaultSpellId;
    }

    public SpellDef def(ItemStack stack) {
        var comp = br.com.murilo.liberthia.magic.spell.composition.SpellComposition.fromStack(stack);
        if (comp != null) return comp.toVirtualDef();
        return SpellLibrary.get(spellId(stack));
    }

    public br.com.murilo.liberthia.magic.spell.composition.SpellComposition composition(ItemStack stack) {
        return br.com.murilo.liberthia.magic.spell.composition.SpellComposition.fromStack(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        SpellDef d = def(stack);
        if (d == null) return super.getName(stack);
        int level = SpellLevels.getLevel(stack);
        if (level > 1) {
            return Component.literal("").append(d.displayName())
                .append(Component.literal(" " + SpellLevels.label(level))
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
        }
        return d.displayName();
    }

    // ─── Channeled Cast (r146) ──────────────────────────────────────────

    /** Anim de braço estilo bow charge — combina com hold-to-cast. */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    /**
     * Duração do channel em ticks. Escala com mana cost da spell:
     * spells fracas carregam rápido (8t = 0.4s), spells fortes lentas (40t = 2s).
     */
    @Override
    public int getUseDuration(ItemStack stack) {
        SpellDef d = def(stack);
        if (d == null) return 0;
        // 8t mínimo (snappy pra spell barata), 40t máximo (lento épico)
        return Math.max(8, Math.min(40, d.manaCost / 3));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            // Client-side: começa channel localmente
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        SpellDef d = def(stack);
        if (d == null) {
            sp.displayClientMessage(Component.literal("§c⚠ Spell desconhecido: " + spellId(stack)), true);
            return InteractionResultHolder.fail(stack);
        }

        // Cooldown check
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        // Pre-flight: valida mana ANTES de começar a carregar (sem consumir)
        int leveledMana = calcLeveledMana(sp, d, stack);
        int sourceCur = br.com.murilo.liberthia.observation.source.SourceData.get(sp);
        if (sourceCur < leveledMana) {
            sp.displayClientMessage(Component.literal(
                    "§c⚠ Source insuficiente §7(" + sourceCur + "/" + leveledMana + ")"), true);
            return InteractionResultHolder.fail(stack);
        }

        // Som de início do channel
        MagicSounds.playCastStart(sp, d.school);

        // Começa a usar (hold right-click)
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * r146: chamado a cada tick enquanto o player segura right-click.
     * Spawna aura de partículas na mão proporcional ao charge.
     */
    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
        if (level.isClientSide || !(living instanceof ServerPlayer sp)) return;
        SpellDef d = def(stack);
        if (d == null) return;

        int totalDuration = getUseDuration(stack);
        int ticksUsed = totalDuration - remaining;

        // Aura de carregamento
        InteractionHand hand = sp.getUsedItemHand();
        SpellChargeAura.tick(sp, d.school, ticksUsed, totalDuration, hand);
    }

    /**
     * r146: channel COMPLETO — executa o cast aqui.
     * (Player segurou o tempo todo, não soltou antes.)
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (level.isClientSide || !(living instanceof ServerPlayer sp)) return stack;
        SpellDef d = def(stack);
        if (d == null) return stack;

        executeCast(sp, stack, d, sp.getUsedItemHand());
        return stack;
    }

    /**
     * r146: player soltou ANTES do channel completar — cancel sem custo.
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide || !(living instanceof ServerPlayer sp)) return;
        int totalDuration = getUseDuration(stack);
        int ticksUsed = totalDuration - timeLeft;

        // Se carregou ao menos 90% considera "completo" (margem pro player)
        if (ticksUsed >= (int)(totalDuration * 0.9F)) {
            SpellDef d = def(stack);
            if (d != null) executeCast(sp, stack, d, sp.getUsedItemHand());
        } else {
            // Cancel — toca fizzle e mensagem leve
            MagicSounds.playCastCancel(sp);
            sp.displayClientMessage(Component.literal("§7⊘ Cast cancelado"), true);
        }
    }

    // ─── Cast execution ─────────────────────────────────────────────────

    private int calcLeveledMana(ServerPlayer sp, SpellDef d, ItemStack stack) {
        int spellLevel = SpellLevels.getLevel(stack);
        float globalMult = 0.70F;
        float armorMult = br.com.murilo.liberthia.magic.armor.ManaArmorEffects.costMultiplierFor(sp);
        // r154: aplica também mana multiplier dos CDR glyphs no inventário
        float cdrCostMult = br.com.murilo.liberthia.magic.modifier.CooldownReductionGlyphItem.manaMultiplier(sp);
        return (int)(d.manaCost * globalMult * armorMult * cdrCostMult * SpellLevels.manaMultiplier(spellLevel));
    }

    private void executeCast(ServerPlayer sp, ItemStack stack, SpellDef d, InteractionHand hand) {
        // Re-check cooldown (player pode ter ficado em cd no meio do channel via outro item)
        if (sp.getCooldowns().isOnCooldown(this)) return;

        int spellLevel = SpellLevels.getLevel(stack);
        int leveledMana = calcLeveledMana(sp, d, stack);
        // r154: aplica CDR mult dos glyphs
        float cdrCdMult = br.com.murilo.liberthia.magic.modifier.CooldownReductionGlyphItem.cooldownMultiplier(sp);
        int leveledCooldown = (int)(d.cooldownTicks * SpellLevels.cooldownMultiplier(spellLevel) * cdrCdMult);

        // Mana re-check (pode ter mudado durante o channel)
        int sourceCur = br.com.murilo.liberthia.observation.source.SourceData.get(sp);
        if (sourceCur < leveledMana) {
            sp.displayClientMessage(Component.literal(
                    "§c⚠ Source insuficiente §7(" + sourceCur + "/" + leveledMana + ")"), true);
            MagicSounds.playCastCancel(sp);
            return;
        }

        // Som de release ANTES de executar (player ouve o "whoosh" e depois o efeito)
        MagicSounds.playCastFinish(sp, d.school);

        // Wrap def com stats leveled — r164: também aplica spell power dos magic accessories
        float powerMult = br.com.murilo.liberthia.magic.accessory.MagicAccessoryEffects.damageMultiplier(sp);
        ServerLevel sl = sp.serverLevel();
        SpellDef levDef = SpellDef.builder(d.id)
                .name(d.name).school(d.school).rarity(d.rarity)
                .mana(leveledMana).cooldown(leveledCooldown)
                .damage(d.damage * SpellLevels.damageMultiplier(spellLevel) * powerMult)
                .range(d.range * SpellLevels.rangeMultiplier(spellLevel))
                .lore(d.lore).cast(d.cast).build();
        CastContext ctx = new CastContext(sp, sl, hand, stack, levDef);
        boolean ok;
        try {
            ok = levDef.cast.execute(ctx);
            // Composition effects
            var compForHit = composition(stack);
            if (ok && compForHit != null && compForHit.isComposed()) {
                br.com.murilo.liberthia.magic.spell.composition.CompositionEffectApplier
                        .applyPostCast(ctx, compForHit);
                LivingEntity tgt = ctx.pickTarget(Math.min(levDef.range, 24));
                if (tgt != null) {
                    br.com.murilo.liberthia.magic.spell.composition.CompositionEffectApplier
                            .applyOnHit(ctx, tgt, compForHit);
                }
            }
        } catch (Throwable t) {
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.warn(
                    "[Spell] cast error for {}: {}", d.id, t.toString());
            ok = false;
        }
        if (!ok) {
            MagicSounds.playCastCancel(sp);
            return;
        }

        // Consume mana + cooldown + durability
        // r153: forceConsume garante dedução mesmo se source caiu durante o channel
        sp.getCooldowns().addCooldown(this, leveledCooldown);
        br.com.murilo.liberthia.observation.source.SourceData.forceConsume(sp, leveledMana);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                "[UniversalSpell] {} cast by {} — consumed {} source, cd {}t",
                d.id, sp.getName().getString(), leveledMana, leveledCooldown);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        SpellDef d = def(stack);
        if (d == null) {
            tooltip.add(Component.literal("§c⚠ Spell inválido"));
            return;
        }
        tooltip.add(d.schoolTooltip());
        tooltip.add(d.statsTooltip());
        tooltip.add(d.loreTooltip());

        // r146: mostra duração do channel
        int channelTicks = getUseDuration(stack);
        tooltip.add(Component.literal("§7⏱ Channel: §f" + String.format("%.1fs", channelTicks / 20F)));
        tooltip.add(Component.literal("§8Segure o botão direito para carregar"));

        var comp = composition(stack);
        if (comp != null && comp.isComposed()) {
            tooltip.add(Component.literal(""));
            tooltip.addAll(comp.tooltip());
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        SpellDef d = def(stack);
        return d != null && d.rarity == net.minecraft.world.item.Rarity.EPIC;
    }

    public static int tintColor(ItemStack stack, int layer) {
        return 0xFFFFFFFF;
    }
}
