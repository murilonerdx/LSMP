package br.com.murilo.liberthia.observation.item;

import br.com.murilo.liberthia.observation.ObservationParts;
import br.com.murilo.liberthia.observation.api.ObservationResolver;
import br.com.murilo.liberthia.observation.api.ObservationSpell;
import br.com.murilo.liberthia.observation.perk.Perk;
import br.com.murilo.liberthia.observation.perk.PerkHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r62: <b>Observation Tome</b> — item central de feitiços observation.
 *
 * <p>Pattern AN's spellbook: carrega uma {@link ObservationSpell} no NBT,
 * cast on right-click, tem perk slots (1-3 baseado em tier).
 *
 * <h2>Tier System</h2>
 * <ul>
 *   <li>Tier 1: 1 perk slot</li>
 *   <li>Tier 2: 2 perk slots</li>
 *   <li>Tier 3: 3 perk slots</li>
 * </ul>
 *
 * <p>Default: tier 1, com Tendril padrão.
 */
public class ObservationTomeItem extends Item {

    public ObservationTomeItem(Properties p) {
        super(p.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override public boolean isFoil(ItemStack s) { return true; }

    /** Right-click — casta o spell salvo. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        if (PerkHolder.getTier(stack) == 0) {
            PerkHolder.setTier(stack, 1);
        }

        ObservationSpell spell = loadSpellFrom(stack);
        boolean ok = ObservationResolver.cast(sp, spell);
        if (ok) {
            sp.getCooldowns().addCooldown(this, 30);
            stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    /** Tick — aplica TickablePerks. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                               int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 4 != 0) return;
        for (PerkHolder.PerkSlot ps : PerkHolder.getPerks(stack)) {
            if (ps.perk instanceof Perk.TickablePerk tp) {
                tp.tickPerk(stack, sp, ps.slotValue);
            }
        }
    }

    /** NBT keys r77 — custom spell imprintada via Spell Binding Pedestal. */
    public static final String NBT_CUSTOM_RECIPE = "tome.custom_recipe";
    public static final String NBT_CUSTOM_NAME = "tome.custom_name";
    public static final String NBT_CUSTOM_COLOR = "tome.custom_color";

    /** Lê spell do NBT do stack. Prioriza custom > preset > default. */
    public static ObservationSpell loadSpellFrom(ItemStack stack) {
        var tag = stack.getOrCreateTag();
        // r77: Prefer custom imprintada via Pedestal
        if (tag.contains(NBT_CUSTOM_RECIPE)) {
            var list = tag.getList(NBT_CUSTOM_RECIPE, 8);
            String name = tag.contains(NBT_CUSTOM_NAME) ? tag.getString(NBT_CUSTOM_NAME) : "Custom";
            int color = tag.contains(NBT_CUSTOM_COLOR) ? tag.getInt(NBT_CUSTOM_COLOR) : 0x9d4dd6;
            var b = ObservationSpell.builder(name, color);
            for (int i = 0; i < list.size(); i++) {
                var part = br.com.murilo.liberthia.observation.api.ObservationRegistry.get(
                    new net.minecraft.resources.ResourceLocation(list.getString(i)));
                if (part != null) b.add(part);
            }
            return b.build();
        }
        if (!tag.contains("spell_preset")) {
            return ObservationSpell.builder("Tendril Simples", 0x9d4dd6)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.TENDRIL)
                .build();
        }
        int preset = tag.getInt("spell_preset");
        return buildPreset(preset);
    }

    /** Imprinta um Parchment no Tome (r77). */
    public static void imprintFromParchment(ItemStack tome, ItemStack parchment) {
        var recipe = br.com.murilo.liberthia.observation.item.SpellParchmentItem.getRecipe(parchment);
        if (recipe.isEmpty()) return;
        var spell = br.com.murilo.liberthia.observation.item.SpellParchmentItem.buildSpell(parchment);
        if (spell == null) return;
        var tag = tome.getOrCreateTag();
        var list = new net.minecraft.nbt.ListTag();
        for (String id : recipe) list.add(net.minecraft.nbt.StringTag.valueOf(id));
        tag.put(NBT_CUSTOM_RECIPE, list);
        tag.putString(NBT_CUSTOM_NAME, spell.name());
        tag.putInt(NBT_CUSTOM_COLOR, spell.color());
    }

    /** r70: Total de presets disponíveis (16). */
    public static final int PRESET_COUNT = 16;

    /** r70: Nome bonito por preset (pra UI/cycle). */
    public static String presetName(int preset) {
        return switch (preset) {
            case 0 -> "Tendril";
            case 1 -> "Silence";
            case 2 -> "Mirror";
            case 3 -> "Decay Echo";
            case 4 -> "Whisper Spread";
            case 5 -> "Memory Tendril";
            case 6 -> "Fire Bolt";
            case 7 -> "Ice Lance";
            case 8 -> "Healing Touch";
            case 9 -> "Lightning Strike";
            case 10 -> "Flame Wave";
            case 11 -> "Shadow Lash";
            case 12 -> "Laser Beam";
            case 13 -> "Gravity Well";
            case 14 -> "Fang Storm";
            case 15 -> "Cosmic Burst";
            default -> "Custom #" + preset;
        };
    }

    public static ObservationSpell buildPreset(int preset) {
        return switch (preset) {
            // Originais r62
            case 1 -> ObservationSpell.builder("Silence", 0xCCCCDD)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.SILENCE_MANIFEST)
                .add(ObservationParts.AMPLIFY)
                .build();
            case 2 -> ObservationSpell.builder("Mirror", 0x9988CC)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.MIRROR)
                .add(ObservationParts.LINGER)
                .build();
            case 3 -> ObservationSpell.builder("Decay Echo", 0x3D6010)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.DECAY)
                .add(ObservationParts.ECHO)
                .build();
            case 4 -> ObservationSpell.builder("Whisper Spread", 0x5544AA)
                .add(ObservationParts.PERIPHERAL)
                .add(ObservationParts.WHISPER)
                .add(ObservationParts.LINGER)
                .add(ObservationParts.ECHO)
                .build();
            case 5 -> ObservationSpell.builder("Memory Tendril", 0x880088)
                .add(ObservationParts.MEMORY)
                .add(ObservationParts.TENDRIL)
                .add(ObservationParts.AMPLIFY)
                .add(ObservationParts.SECRET)
                .build();

            // r70: 10 NOVOS presets baseados em AN com nomes da fantasia
            case 6 -> ObservationSpell.builder("Fire Bolt", 0xFF7733)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.IGNITE)
                .add(ObservationParts.AMPLIFY)
                .build();
            case 7 -> ObservationSpell.builder("Ice Lance", 0x88DDFF)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.FREEZE)
                .add(ObservationParts.AMPLIFY)
                .add(ObservationParts.LINGER)
                .build();
            case 8 -> ObservationSpell.builder("Healing Touch", 0x55EE55)
                .add(ObservationParts.TOUCH)
                .add(ObservationParts.HEAL)
                .add(ObservationParts.AMPLIFY)
                .build();
            case 9 -> ObservationSpell.builder("Lightning Strike", 0xFFFF66)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.LIGHTNING)
                .add(ObservationParts.AMPLIFY)
                .build();
            case 10 -> ObservationSpell.builder("Flame Wave", 0xFF8C32)
                .add(ObservationParts.BURST)
                .add(ObservationParts.IGNITE)
                .add(ObservationParts.LINGER)
                .build();
            case 11 -> ObservationSpell.builder("Shadow Lash", 0x6B4D99)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.TENDRIL)
                .add(ObservationParts.DECAY)
                .add(ObservationParts.LINGER)
                .build();
            case 12 -> ObservationSpell.builder("Laser Beam", 0xFF4444)
                .add(ObservationParts.LASER)
                .add(ObservationParts.HARM)
                .add(ObservationParts.AMPLIFY)
                .add(ObservationParts.ECHO)
                .build();
            case 13 -> ObservationSpell.builder("Gravity Well", 0x643282)
                .add(ObservationParts.ORBIT)
                .add(ObservationParts.GRAVITY)
                .add(ObservationParts.LINGER)
                .add(ObservationParts.AMPLIFY)
                .build();
            case 14 -> ObservationSpell.builder("Fang Storm", 0xB43232)
                .add(ObservationParts.CHAIN)
                .add(ObservationParts.FANGS)
                .add(ObservationParts.ECHO)
                .build();
            case 15 -> ObservationSpell.builder("Cosmic Burst", 0xFF501E)
                .add(ObservationParts.WALL)
                .add(ObservationParts.EXPLOSION)
                .add(ObservationParts.AMPLIFY)
                .build();

            default -> ObservationSpell.builder("Tendril", 0x9d4dd6)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.TENDRIL)
                .build();
        };
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§5§oTomo de Observação").withStyle(ChatFormatting.ITALIC));
        int tier = PerkHolder.getTier(s);
        if (tier == 0) tier = 1;
        t.add(Component.literal("§7Tier: §e" + tier + " §7| Perks: §e"
            + PerkHolder.getPerks(s).size() + "§7/§e" + PerkHolder.maxSlotsForTier(tier)));

        int preset = s.getOrCreateTag().getInt("spell_preset");
        ObservationSpell loaded = buildPreset(preset);
        t.add(Component.literal("§7Spell: §d" + loaded.name()));
        t.add(Component.literal("§7Custo: §e" + loaded.totalSourceCost() + " Source §7| §c"
            + loaded.totalSanityCost() + " sanity"));
        t.add(Component.empty());

        if (!PerkHolder.getPerks(s).isEmpty()) {
            t.add(Component.literal("§5§lPerks ativos:"));
            for (PerkHolder.PerkSlot ps : PerkHolder.getPerks(s)) {
                t.add(Component.literal("  §7§o✦ §r§7" + ps.perk.displayName() + " §8(slot " + ps.slotValue + ")"));
            }
            t.add(Component.empty());
        }

        t.add(Component.literal("§8§o\"Cada nome lá dentro foi cobrado de alguém.\""));
    }
}
