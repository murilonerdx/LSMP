package br.com.murilo.liberthia.client;

import br.com.murilo.liberthia.magic.PlayerSpellKnowledge;
import br.com.murilo.liberthia.magic.Spell;
import br.com.murilo.liberthia.magic.SpellRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;

/**
 * Client-only: enriquece o tooltip do Grimoire com infos do player LOCAL
 * (mana, feitiços conhecidos, feitiço selecionado).
 *
 * <p>Carregado EXCLUSIVAMENTE via {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)}
 * a partir de {@link br.com.murilo.liberthia.magic.GrimoireItem}. Isso evita que o
 * servidor dedicado tente carregar {@code Minecraft}/{@code LocalPlayer} (classes
 * client-only) durante o registro do item — o que crashava o boot do server.
 */
public final class GrimoireClientTooltip {
    private GrimoireClientTooltip() {}

    public static void append(List<Component> t) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Set<String> known = PlayerSpellKnowledge.known(mc.player);
        String selectedId = PlayerSpellKnowledge.getSelected(mc.player);
        t.add(Component.literal("§7Feitiços aprendidos: §b" + known.size()
                + "§7/§7" + SpellRegistry.ALL.size()));
        t.add(Component.literal("§7Mana: §b" + PlayerSpellKnowledge.getMana(mc.player)
                + "§7/§7" + PlayerSpellKnowledge.MAX_MANA));
        if (selectedId != null && !selectedId.isEmpty()) {
            Spell sp = SpellRegistry.get(selectedId);
            if (sp != null) {
                t.add(Component.empty());
                t.add(Component.literal("§7Selecionado: ").append(sp.displayName()));
                t.add(Component.literal("§8§o" + sp.lore));
                t.add(Component.literal("§7Mana: §b" + sp.manaCost
                        + "§7 | CD: §e" + (sp.cooldownTicks / 20) + "s"
                        + "§7 | Range: §a" + sp.range + "b"));
            }
        }
    }
}
