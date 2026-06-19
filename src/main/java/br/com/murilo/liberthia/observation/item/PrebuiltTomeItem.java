package br.com.murilo.liberthia.observation.item;

import br.com.murilo.liberthia.observation.ObservationParts;
import br.com.murilo.liberthia.observation.api.ObservationPart;
import br.com.murilo.liberthia.observation.api.ObservationResolver;
import br.com.murilo.liberthia.observation.api.ObservationSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
import java.util.function.Supplier;

/**
 * v0.1.22 r72: <b>Prebuilt Spell Tome</b> — item que carrega um combo pré-feito.
 *
 * <p>Right-click com Grimório na offhand: imprinta o combo no Grimório.
 * Right-click sem grimório: imprinta no PRÓPRIO item (vira parchment-like).
 */
public class PrebuiltTomeItem extends Item {

    public final Supplier<ObservationSpell> spellSupplier;

    public PrebuiltTomeItem(Properties p, Supplier<ObservationSpell> spellSupplier) {
        super(p.stacksTo(1).rarity(Rarity.RARE));
        this.spellSupplier = spellSupplier;
    }

    @Override public boolean isFoil(ItemStack s) { return true; }

    /**
     * r164: <b>BUG FIX — Tome agora é INDEPENDENTE</b>.
     *
     * <p>Comportamento antigo: forçava Grimório de Observação na outra mão,
     * senão falhava com "Coloque o Grimório na outra mão". User reclamou que
     * "não consegue usar o tome sem livro na outra mão".
     *
     * <p>Novo comportamento (prioridade):
     * <ul>
     *   <li><b>Shift + RClick</b>: se tem Grimório na outra mão → imprinta nele
     *       (comportamento legacy preservado pra quem usa)</li>
     *   <li><b>RClick simples</b>: §lCASTA O FEITIÇO DIRETO§r usando
     *       {@link ObservationResolver}, igual o ObservationTomeItem. Tome
     *       funciona sozinho como spell scroll de uso único reutilizável.</li>
     * </ul>
     *
     * <p>Cooldown 30t pra evitar spam.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        ObservationSpell spell = spellSupplier.get();
        ItemStack other = (hand == InteractionHand.MAIN_HAND) ? sp.getOffhandItem() : sp.getMainHandItem();

        // r164: Shift+RClick + Grimório na outra mão → imprinta (legacy)
        if (user.isShiftKeyDown() && other.getItem() instanceof GrimoireOfObservationItem) {
            var tag = other.getOrCreateTag();
            ListTag list = new ListTag();
            for (var part : spell.recipe()) list.add(StringTag.valueOf(part.id().toString()));
            tag.put(GrimoireOfObservationItem.NBT_CUSTOM_RECIPE, list);
            tag.putString(GrimoireOfObservationItem.NBT_CUSTOM_NAME, spell.name());
            tag.putInt(GrimoireOfObservationItem.NBT_CUSTOM_COLOR, spell.color());
            sp.displayClientMessage(Component.literal(
                "§5§l✦ Grimório imprintado: §r§e" + spell.name()
                + " §7(" + spell.totalSourceCost() + " Source)"), false);
            return InteractionResultHolder.success(stack);
        }

        // r164: cast DIRETO — tome funciona sozinho como uma spell scroll
        if (sp.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        boolean ok = ObservationResolver.cast(sp, spell);
        if (ok) {
            sp.getCooldowns().addCooldown(this, 30);
            sp.displayClientMessage(Component.literal(
                "§5§l✦ §e" + spell.name() + " §7lançado"), true);
            return InteractionResultHolder.consume(stack);
        }
        // r165: ObservationResolver já exibiu a mensagem de erro correta — não duplicar
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        ObservationSpell spell = spellSupplier.get();
        t.add(Component.literal("§5§oLivro Pronto").withStyle(ChatFormatting.ITALIC));
        t.add(Component.literal("§5§l✦ §e" + spell.name()));
        t.add(Component.literal("§7Glyphs: §f" + spell.recipe().size()
            + " §7| Custo: §c" + spell.totalSourceCost() + " Source"));
        t.add(Component.empty());
        t.add(Component.literal("§8§oRecipe:"));
        for (var part : spell.recipe()) {
            t.add(Component.literal("  §7• §f" + part.displayComponent().getString()));
        }
        t.add(Component.empty());
        // r164: instruções de uso atualizadas — agora INDEPENDENTE
        t.add(Component.literal("§a§lRClick§r §7→ §flança o feitiço§r §7(30t cooldown)"));
        t.add(Component.literal("§7§oShift+RClick com Grimório na outra mão → imprinta"));
    }

    /** Definições dos 8 livros prontos. */
    public static final class Tomes {
        public static ObservationSpell pyromancer() {
            return ObservationSpell.builder("§cChama do Piromante", 0xFF4400)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.FIREBALL)
                .add(ObservationParts.IGNITE)
                .add(ObservationParts.AMPLIFY)
                .add(ObservationParts.ECHO)
                .build();
        }
        public static ObservationSpell frostbinder() {
            return ObservationSpell.builder("§bLigador de Gelo", 0x88DDFF)
                .add(ObservationParts.DIRECT_GAZE)
                .add(ObservationParts.FROST_LANCE)
                .add(ObservationParts.ROOTS)
                .add(ObservationParts.LINGER)
                .add(ObservationParts.AMPLIFY)
                .build();
        }
        public static ObservationSpell skywalker() {
            return ObservationSpell.builder("§fAndarilho do Céu", 0xEEFFFF)
                .add(ObservationParts.SELF)
                .add(ObservationParts.SKY_STEP)
                .add(ObservationParts.SLOWFALL)
                .add(ObservationParts.VELOCITY)
                .add(ObservationParts.LINGER)
                .build();
        }
        public static ObservationSpell webweaver() {
            return ObservationSpell.builder("§7Teia da Aranha", 0x888899)
                .add(ObservationParts.BURST)
                .add(ObservationParts.SNARE)
                .add(ObservationParts.ROOTS)
                .add(ObservationParts.LINGER)
                .add(ObservationParts.AOE)
                .build();
        }
        public static ObservationSpell deathBeam() {
            return ObservationSpell.builder("§4Feixe da Morte", 0xCC0033)
                .add(ObservationParts.LASER)
                .add(ObservationParts.HARM)
                .add(ObservationParts.DECAY)
                .add(ObservationParts.AMPLIFY)
                .add(ObservationParts.PIERCE)
                .build();
        }
        public static ObservationSpell healingLight() {
            return ObservationSpell.builder("§aLuz Curativa", 0x88FF88)
                .add(ObservationParts.SELF)
                .add(ObservationParts.HEAL)
                .add(ObservationParts.CLEANSING_FLAME)
                .add(ObservationParts.AMPLIFY)
                .add(ObservationParts.ECHO)
                .build();
        }
        public static ObservationSpell dash() {
            return ObservationSpell.builder("§eImpulso", 0xFFFF66)
                .add(ObservationParts.SELF)
                .add(ObservationParts.VELOCITY)
                .add(ObservationParts.MIST_VEIL)
                .add(ObservationParts.LINGER)
                .build();
        }
        public static ObservationSpell singularityCombo() {
            return ObservationSpell.builder("§5Singularidade Cósmica", 0x441166)
                .add(ObservationParts.ORBIT)
                .add(ObservationParts.SINGULARITY)
                .add(ObservationParts.GRAVITY)
                .add(ObservationParts.AMPLIFY)
                .add(ObservationParts.LINGER)
                .build();
        }
    }
}
