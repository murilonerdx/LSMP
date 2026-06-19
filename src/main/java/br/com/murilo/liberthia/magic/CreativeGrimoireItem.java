package br.com.murilo.liberthia.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * v0.1.22 r40: <b>Grimório Onisciente (Creative Grimoire)</b>
 *
 * <p>Variante do {@link GrimoireItem} com TODOS os feitiços já aprendidos
 * automaticamente. Não tem receita de craft — só obtido via creative tab ou
 * comando admin.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li><b>inventoryTick:</b> auto-aprende toda a {@link SpellRegistry#ALL}
 *       no holder a cada 20 ticks (1s) — garante que mesmo se o player perder
 *       knowledge (capability reset, etc.), recupere imediatamente.</li>
 *   <li><b>Mana infinita:</b> ao usar, restaura mana pra MAX antes de castar.</li>
 *   <li><b>Sem cooldown:</b> contorna o {@link Player#getCooldowns()} setting
 *       cd zero pra qualquer feitiço.</li>
 *   <li><b>Right-click:</b> casta o feitiço selecionado (mesmo que normal).</li>
 *   <li><b>Shift+Right-click:</b> cicla pro próximo feitiço.</li>
 * </ul>
 */
public class CreativeGrimoireItem extends Item {

    public CreativeGrimoireItem(Properties p) { super(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)); }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer sp)) return;
        if (level.getGameTime() % 20 != 0) return; // 1×/sec

        // Auto-aprende TUDO + mantém mana cheia
        Set<String> known = PlayerSpellKnowledge.known(sp);
        for (Spell s : SpellRegistry.ALL.values()) {
            if (!known.contains(s.id)) {
                PlayerSpellKnowledge.learn(sp, s.id);
            }
        }
        if (PlayerSpellKnowledge.getMana(sp) < PlayerSpellKnowledge.MAX_MANA) {
            PlayerSpellKnowledge.setMana(sp, PlayerSpellKnowledge.MAX_MANA);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        // Garante que TUDO está aprendido (caso ainda não tenha tickado)
        for (Spell s : SpellRegistry.ALL.values()) {
            if (!PlayerSpellKnowledge.knows(sp, s.id)) {
                PlayerSpellKnowledge.learn(sp, s.id);
            }
        }
        Set<String> known = PlayerSpellKnowledge.known(sp);

        // Shift = cicla
        if (sp.isShiftKeyDown()) {
            cycleSelected(sp, known);
            return InteractionResultHolder.success(stack);
        }

        // Cast selected — mas com mana cheia e sem cooldown
        String selectedId = PlayerSpellKnowledge.getSelected(sp);
        if (selectedId == null || selectedId.isEmpty() || !known.contains(selectedId)) {
            selectedId = known.iterator().next();
            PlayerSpellKnowledge.setSelected(sp, selectedId);
        }
        Spell spell = SpellRegistry.get(selectedId);
        if (spell == null) {
            sp.displayClientMessage(Component.literal("§cFeitiço desconhecido: " + selectedId), true);
            return InteractionResultHolder.fail(stack);
        }

        // Restaura mana cheia ANTES de castar (creative = infinito)
        PlayerSpellKnowledge.setMana(sp, PlayerSpellKnowledge.MAX_MANA);

        boolean ok = SpellExecutor.cast(sp, spell);

        // E restaura DE NOVO depois (compensando o gasto do cast)
        PlayerSpellKnowledge.setMana(sp, PlayerSpellKnowledge.MAX_MANA);

        return ok ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    private void cycleSelected(ServerPlayer sp, Set<String> known) {
        String current = PlayerSpellKnowledge.getSelected(sp);
        java.util.List<String> ordered = new java.util.ArrayList<>(known);
        int idx = ordered.indexOf(current);
        int next = (idx + 1) % ordered.size();
        String nextId = ordered.get(next);
        PlayerSpellKnowledge.setSelected(sp, nextId);
        Spell s = SpellRegistry.get(nextId);
        if (s != null) {
            sp.displayClientMessage(Component.literal("§6✦ §r§e").append(s.displayName())
                    .append(Component.literal(" §7(creative — sem mana/cd)")), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§6§l§oGrimório Onisciente").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        t.add(Component.literal("§7Right-click: §dCASTA§r§7 feitiço selecionado"));
        t.add(Component.literal("§7Shift+Right-click: §eCICLA§r§7 próximo"));
        t.add(Component.empty());
        t.add(Component.literal("§6§l✦ MODO CREATIVE / ADMIN ✦"));
        t.add(Component.literal("§7• §aTodos os " + SpellRegistry.ALL.size() + " feitiços aprendidos"));
        t.add(Component.literal("§7• §aMana infinita §7(refill auto)"));
        t.add(Component.literal("§7• §aSem cooldowns"));
        t.add(Component.empty());
        t.add(Component.literal("§8§o\"Páginas escritas por quem viu Tudo de uma vez."));
        t.add(Component.literal("§8§o Há um custo, mas você esqueceu qual.\""));
        t.add(Component.empty());
        t.add(Component.literal("§c§oCreative-only — sem receita."));
    }
}
