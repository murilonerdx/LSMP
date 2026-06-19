package br.com.murilo.liberthia.observation.item;

import br.com.murilo.liberthia.observation.api.*;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

import java.util.ArrayList;
import java.util.List;

/**
 * v0.1.22 r68: <b>SpellParchmentItem</b> — pergaminho que armazena uma
 * {@link ObservationSpell} no NBT.
 *
 * <p>Pattern AN's {@code SpellParchment}: pergaminho é a "memória física" de
 * uma receita. Pode ser lido, inserido em livros, copiado.
 *
 * <h2>Interações</h2>
 * <ul>
 *   <li><b>Hold parchment, hold glyph na offhand, right-click:</b> adiciona o
 *       glyph na recipe do parchment</li>
 *   <li><b>Right-click sem glyph na offhand:</b> mostra recipe atual</li>
 *   <li><b>Shift+right-click:</b> limpa recipe (sneak+rclick)</li>
 *   <li><b>Hold parchment + Grimório/Tomo na mão principal:</b> imprint do
 *       parchment no item (handler no Grimoire/Tome)</li>
 * </ul>
 *
 * <h2>NBT Schema</h2>
 * <pre>
 * spell_recipe: ListTag<StringTag>  // ["liberthia:direct_gaze", "liberthia:tendril_manifestation", ...]
 * spell_name: String                // "Meu Feitiço" — opcional
 * spell_color: Int                  // 0x9d4dd6 — calculado do primeiro Manifestation
 * </pre>
 */
public class SpellParchmentItem extends Item {

    public static final String NBT_RECIPE = "spell_recipe";
    public static final String NBT_NAME = "spell_name";
    public static final String NBT_COLOR = "spell_color";

    public SpellParchmentItem(Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.RARE));
    }

    @Override public boolean isFoil(ItemStack s) {
        return getRecipe(s).size() > 0;
    }

    /** Lê recipe do NBT do parchment. */
    public static List<String> getRecipe(ItemStack stack) {
        List<String> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_RECIPE)) return result;
        ListTag list = tag.getList(NBT_RECIPE, 8); // 8 = StringTag
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return result;
    }

    /** Salva recipe no NBT. */
    public static void setRecipe(ItemStack stack, List<String> recipe) {
        ListTag list = new ListTag();
        for (String id : recipe) list.add(StringTag.valueOf(id));
        stack.getOrCreateTag().put(NBT_RECIPE, list);
        // Auto-color: usa cor do primeiro Manifestation que aparecer
        for (String id : recipe) {
            ObservationPart p = ObservationRegistry.get(new ResourceLocation(id));
            if (p instanceof Manifestation) {
                stack.getOrCreateTag().putInt(NBT_COLOR, p.color());
                break;
            }
        }
    }

    /** Constrói um ObservationSpell a partir do NBT do parchment. Retorna null se inválido. */
    @Nullable
    public static ObservationSpell buildSpell(ItemStack stack) {
        List<String> ids = getRecipe(stack);
        if (ids.isEmpty()) return null;

        CompoundTag tag = stack.getOrCreateTag();
        String name = tag.contains(NBT_NAME) ? tag.getString(NBT_NAME) : "Feitiço";
        int color = tag.contains(NBT_COLOR) ? tag.getInt(NBT_COLOR) : 0x9d4dd6;

        ObservationSpell.Builder builder = ObservationSpell.builder(name, color);
        for (String id : ids) {
            ObservationPart part = ObservationRegistry.get(new ResourceLocation(id));
            if (part != null) builder.add(part);
        }
        return builder.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack parchment = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(parchment);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(parchment);

        // Só funciona se segurando o parchment na MAIN HAND
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(parchment);

        // Shift+rclick: limpa recipe
        if (sp.isShiftKeyDown()) {
            setRecipe(parchment, new ArrayList<>());
            parchment.getOrCreateTag().remove(NBT_COLOR);
            sp.displayClientMessage(Component.literal(
                "§7§oPergaminho limpo."), true);
            return InteractionResultHolder.success(parchment);
        }

        // Verifica se tem glyph na offhand
        ItemStack offhand = sp.getOffhandItem();
        if (offhand.getItem() instanceof GlyphItem glyph) {
            ObservationPart part = glyph.getPart();
            if (part == null) {
                sp.displayClientMessage(Component.literal(
                    "§cGlyph inválido (part não registrado)."), true);
                return InteractionResultHolder.fail(parchment);
            }
            List<String> recipe = getRecipe(parchment);

            // Validação básica AN-style:
            // - Recipe vazio: PRIMEIRO part DEVE ser WatchMethod
            if (recipe.isEmpty() && part.typeIndex() != 1) {
                sp.displayClientMessage(Component.literal(
                    "§c⚠ O feitiço DEVE começar com um Método (glyph azul)."), true);
                return InteractionResultHolder.fail(parchment);
            }
            // - Não vazio: NÃO pode adicionar outro WatchMethod
            if (!recipe.isEmpty() && part.typeIndex() == 1) {
                sp.displayClientMessage(Component.literal(
                    "§c⚠ Já tem um Método. Só 1 por feitiço."), true);
                return InteractionResultHolder.fail(parchment);
            }
            // - Max 8 glyphs
            if (recipe.size() >= 8) {
                sp.displayClientMessage(Component.literal(
                    "§c⚠ Pergaminho cheio (max 8 glyphs)."), true);
                return InteractionResultHolder.fail(parchment);
            }

            recipe.add(part.id().toString());
            // r74: valida via SpellValidator
            List<ObservationPart> partsList = new ArrayList<>();
            for (String id : recipe) {
                var p = ObservationRegistry.get(new ResourceLocation(id));
                if (p != null) partsList.add(p);
            }
            var validationError = br.com.murilo.liberthia.observation.api.SpellValidator.validate(partsList);
            if (validationError != null) {
                sp.displayClientMessage(validationError, true);
                return InteractionResultHolder.fail(parchment);
            }
            setRecipe(parchment, recipe);
            // Consume 1 glyph
            offhand.shrink(1);

            sp.displayClientMessage(Component.literal(
                "§a✓ §rAdicionado: §d" + part.displayComponent().getString()
                + " §7(" + recipe.size() + "/8)"), true);
            return InteractionResultHolder.success(parchment);
        }

        // Sem glyph: mostra recipe atual
        List<String> recipe = getRecipe(parchment);
        if (recipe.isEmpty()) {
            sp.displayClientMessage(Component.literal(
                "§7§oPergaminho vazio. Coloque um Glifo na mão secundária e clique de novo."), true);
        } else {
            sp.displayClientMessage(Component.literal(
                "§5§lRecipe (" + recipe.size() + " glifos):"), false);
            int idx = 1;
            for (String id : recipe) {
                ObservationPart p = ObservationRegistry.get(new ResourceLocation(id));
                String label = p != null ? p.displayComponent().getString() : id;
                sp.displayClientMessage(Component.literal(
                    "§7  " + (idx++) + ". §f" + label), false);
            }
            ObservationSpell spell = buildSpell(parchment);
            if (spell != null) {
                sp.displayClientMessage(Component.literal(
                    "§7Custo total: §e" + spell.totalSourceCost() + " Source§7, §c"
                    + spell.totalSanityCost() + " Sanity"), false);
                String err = spell.validate();
                if (err != null) {
                    sp.displayClientMessage(Component.literal(
                        "§c⚠ Inválido: " + err), false);
                } else {
                    sp.displayClientMessage(Component.literal(
                        "§a✓ Recipe válido. Use com Grimório/Tomo."), false);
                }
            }
        }
        return InteractionResultHolder.success(parchment);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5§oPergaminho de Inscrição").withStyle(ChatFormatting.ITALIC));
        List<String> recipe = getRecipe(stack);
        if (recipe.isEmpty()) {
            tooltip.add(Component.literal("§7§oVazio. Coloque Glyph na offhand + rclick."));
        } else {
            tooltip.add(Component.literal("§7Glyphs: §e" + recipe.size() + "§7/§e8"));
            ObservationSpell spell = buildSpell(stack);
            if (spell != null) {
                tooltip.add(Component.literal("§7Custo: §e" + spell.totalSourceCost()
                    + " §7/ §c" + spell.totalSanityCost()));
                String err = spell.validate();
                if (err != null) {
                    tooltip.add(Component.literal("§c⚠ " + err));
                } else {
                    tooltip.add(Component.literal("§a✓ Válido"));
                }
            }
        }
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§8§oRight-click: inscrever / ver recipe"));
        tooltip.add(Component.literal("§8§oShift+rclick: limpar"));
    }
}
