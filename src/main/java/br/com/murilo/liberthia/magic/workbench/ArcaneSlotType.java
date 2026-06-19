package br.com.murilo.liberthia.magic.workbench;

import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.function.Predicate;

/**
 * r159: Tipos de slot do Arcane Workbench — cada slot só aceita um tipo específico.
 *
 * <h2>Slots</h2>
 * <ol start="0">
 *   <li><b>TABLET</b> — Magic Tablet (material base)</li>
 *   <li><b>WEAVE</b> — Fine Weave (false/ghost/mirror/sky)</li>
 *   <li><b>THREAD</b> — Pale Thread / Flesh Thread / Thread of Distance</li>
 *   <li><b>SCROLL</b> — Factory Spell Scroll (define o feitiço base)</li>
 *   <li><b>ORB</b> — Arcane Orb / Veiling Orb / Sanctify Orb / Void Seer Orb</li>
 *   <li><b>FOCUS</b> — Focus de escola (focus_fire / focus_ice / ...)</li>
 *   <li><b>SCHOOL</b> — Runa de Escola (define mutação de escola)</li>
 *   <li><b>MODIFIER_GLYPH</b> — SpellModifierItem (modifier_amplify etc.)</li>
 *   <li><b>GLYPH</b> — GlyphItem (qualquer um dos 23+ glyphs)</li>
 *   <li><b>PARCHMENT</b> — SpellParchmentItem (consumível, "escreve" o feitiço)</li>
 *   <li><b>OUTPUT</b> — feitiço composto final (não aceita input)</li>
 * </ol>
 */
public enum ArcaneSlotType {

    // r165: RitualTabletItem removido — ritual system foi removido
    TABLET("Tablet", stack -> {
        Item it = stack.getItem();
        return it instanceof ArcaneComponentItems.MagicTablet;
    }),

    WEAVE("Weave", stack -> {
        Item it = stack.getItem();
        return it == ModItems.MIRROR_WEAVE_ITEM.get()
                || it == ModItems.SKY_WEAVE_ITEM.get()
                || it == ModItems.GHOST_WEAVE_ITEM.get()
                || it == ModItems.FALSE_WEAVE_ITEM.get();
    }),

    // r164: THREAD expandido — aceita threads especiais (pale/flesh/distance) +
    // os 15 PerkThreadItem (looting/jump/vampiric/etc).
    THREAD("Thread", stack -> {
        Item it = stack.getItem();
        return it == ModItems.PALE_THREAD.get()
                || it == ModItems.FLESH_THREAD.get()
                || it == ModItems.THREAD_OF_DISTANCE.get()
                || it instanceof br.com.murilo.liberthia.magic.perk.PerkThreadItem
                || it instanceof br.com.murilo.liberthia.magic.thread.CustomThreadItem;
    }),

    // r166: aceita só scrolls que REALMENTE viram feitiço (factory/universal/dinâmico).
    // Os scrolls de escola (Scroll Forge) não são itens castáveis, então aceitá-los
    // só causava "entra mas não gera output" — por isso ficaram de fora.
    SCROLL("Scroll", stack ->
            stack.getItem() == ModItems.FACTORY_SPELL_SCROLL.get()
                    || stack.getItem() instanceof br.com.murilo.liberthia.magic.spell.UniversalSpellScrollItem
                    || stack.getItem() instanceof br.com.murilo.liberthia.magic.factory.DynamicSpellItem),

    // r164: ORB expandido — aceita Arcane/Sanctify/Veiling + os 5 UpgradeOrbItem
    // (mana_boost/cooldown/spell_damage/health/source_regen).
    ORB("Orb", stack -> {
        Item it = stack.getItem();
        return it instanceof ArcaneComponentItems.ArcaneOrb
                || it instanceof br.com.murilo.liberthia.item.SanctifyOrbItem
                || it instanceof br.com.murilo.liberthia.item.VeilingOrbItem
                || it instanceof br.com.murilo.liberthia.magic.weapon.UpgradeOrbItem
                || it instanceof br.com.murilo.liberthia.magic.weapon.ArcaneOrbConsumable
                || it instanceof br.com.murilo.liberthia.magic.orb.CustomOrbItem;
    }),

    FOCUS("Focus", stack -> {
        Item it = stack.getItem();
        return it == ModItems.FOCUS_FIRE.get() || it == ModItems.FOCUS_ICE.get()
                || it == ModItems.FOCUS_LIGHTNING.get() || it == ModItems.FOCUS_BLOOD.get()
                || it == ModItems.FOCUS_ELDRITCH.get() || it == ModItems.FOCUS_HOLY.get()
                || it == ModItems.FOCUS_NATURE.get()
                || it == ModItems.FOCUS_ARCANE.get() || it == ModItems.FOCUS_VOID.get()
                || it == ModItems.FOCUS_STORM.get() || it == ModItems.FOCUS_VERDANT.get()
                || it == ModItems.FOCUS_RADIANT.get();
    }),

    SCHOOL("School Rune", stack ->
            stack.getItem() instanceof ArcaneComponentItems.SchoolRune),

    MODIFIER_GLYPH("Modifier Glyph", stack ->
            stack.getItem() instanceof br.com.murilo.liberthia.magic.spell.composition.SpellModifierItem),

    GLYPH("Glyph", stack ->
            stack.getItem() instanceof br.com.murilo.liberthia.observation.item.GlyphItem),

    PARCHMENT("Parchment", stack ->
            stack.getItem() instanceof br.com.murilo.liberthia.observation.item.SpellParchmentItem),

    OUTPUT("Output", stack -> false);

    public final String displayName;
    public final Predicate<ItemStack> validator;

    ArcaneSlotType(String name, Predicate<ItemStack> v) {
        this.displayName = name;
        this.validator = v;
    }

    public boolean accepts(ItemStack stack) {
        return stack.isEmpty() || validator.test(stack);
    }

    /** Slot indices em ordem definida. OUTPUT é o último. */
    public static final ArcaneSlotType[] ORDER = {
            TABLET, WEAVE, THREAD, SCROLL, ORB, FOCUS, SCHOOL,
            MODIFIER_GLYPH, GLYPH, PARCHMENT, OUTPUT
    };

    public static final int SLOT_TABLET = 0;
    public static final int SLOT_WEAVE = 1;
    public static final int SLOT_THREAD = 2;
    public static final int SLOT_SCROLL = 3;
    public static final int SLOT_ORB = 4;
    public static final int SLOT_FOCUS = 5;
    public static final int SLOT_SCHOOL = 6;
    public static final int SLOT_MODIFIER_GLYPH = 7;
    public static final int SLOT_GLYPH = 8;
    public static final int SLOT_PARCHMENT = 9;
    public static final int SLOT_OUTPUT = 10;
    public static final int TOTAL_SLOTS = 11;
    /** Slots considerados "input" (que consomem no craft). */
    public static final Set<Integer> INPUT_SLOTS = Set.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
}
