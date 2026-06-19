package br.com.murilo.liberthia.magic;

import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenSpellWheelS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * v0.1.22 r54: <b>Grimoire</b> — item central do sistema de magia.
 *
 * <h2>Controles (r54)</h2>
 * <ul>
 *   <li><b>Right-click</b>: CASTA o feitiço selecionado (custom spell ou
 *       feitiço aprendido). Spell sai da MÃO do player, não dos olhos.</li>
 *   <li><b>Shift+Right-click</b>: ABRE o <b>Spell Wheel</b> (UI radial) pra
 *       escolher feitiço custom. Substituiu o "cycle" antigo.</li>
 *   <li><b>X (keybind)</b>: também abre o Spell Wheel</li>
 * </ul>
 *
 * <p>O grimoire é PESSOAL — feitiços aprendidos ficam no
 * {@link PlayerSpellKnowledge} (player NBT, não no item). Qualquer grimoire
 * que esse player segurar mostra os mesmos feitiços.
 *
 * <p>Foil sempre — visual mystic.
 */
public class GrimoireItem extends Item {

    public GrimoireItem(Properties p) { super(p.stacksTo(1)); }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        // r54: Shift+rclick = ABRE o Spell Wheel (custom spells)
        if (sp.isShiftKeyDown()) {
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new OpenSpellWheelS2CPacket());
            return InteractionResultHolder.success(stack);
        }

        // Right-click normal = casta o feitiço selecionado
        // 1º tenta custom spell selecionado
        var customSelected = br.com.murilo.liberthia.magic.custom.CustomSpellStorage
                .getSelected(sp);
        if (customSelected != null) {
            boolean ok = br.com.murilo.liberthia.magic.custom.CustomSpellExecutor
                    .cast(sp, customSelected);
            return ok ? InteractionResultHolder.consume(stack)
                      : InteractionResultHolder.fail(stack);
        }

        // 2º fallback: feitiços aprendidos (sistema antigo)
        Set<String> known = PlayerSpellKnowledge.known(sp);
        if (known.isEmpty()) {
            sp.displayClientMessage(Component.literal(
                    "§7Grimoire vazio — invoque entidades nos rituais "
                    + "ou crie feitiços na §dMesa de Feitiços§7."), true);
            sp.displayClientMessage(Component.literal(
                    "§8(Shift+Right-click abre o Spell Wheel)"), false);
            return InteractionResultHolder.fail(stack);
        }
        String selectedId = PlayerSpellKnowledge.getSelected(sp);
        if (selectedId == null || selectedId.isEmpty() || !known.contains(selectedId)) {
            // Auto-seleciona primeiro
            selectedId = known.iterator().next();
            PlayerSpellKnowledge.setSelected(sp, selectedId);
        }
        Spell spell = SpellRegistry.get(selectedId);
        if (spell == null) {
            sp.displayClientMessage(Component.literal("§cFeitiço desconhecido: " + selectedId), true);
            return InteractionResultHolder.fail(stack);
        }
        boolean ok = SpellExecutor.cast(sp, spell);
        return ok ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§5§oGrimoire dos Feitiços Roubados").withStyle(ChatFormatting.ITALIC));
        t.add(Component.literal("§7Right-click: §dCASTA§r§7 feitiço selecionado"));
        t.add(Component.literal("§7Shift+Right-click: §6ABRE Spell Wheel§r§7 (custom)"));
        t.add(Component.literal("§7Tecla §6X§7: também abre Spell Wheel"));
        t.add(Component.empty());
        // Client-only: infos do player LOCAL (mana, feitiços conhecidos/selecionado).
        // Isolado num GrimoireClientTooltip chamado via DistExecutor pra NUNCA carregar
        // Minecraft/LocalPlayer (client-only) no servidor dedicado — isso crashava o boot.
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> br.com.murilo.liberthia.client.GrimoireClientTooltip.append(t));
        t.add(Component.empty());
        t.add(Component.literal("§8§o\"Cada nome escrito aqui foi extraído de uma entidade."));
        t.add(Component.literal("§8§o Para cada palavra, paguei o preço.\""));
    }
}
