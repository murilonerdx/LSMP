package br.com.murilo.liberthia.cosmic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r38: <b>Forbidden Tome</b> — item que dispara o Cosmic Horror System.
 *
 * <h2>r38 CHANGES</h2>
 * <ul>
 *   <li><b>Duração:</b> 4 minutos (era 2 + manifestation pra sempre).</li>
 *   <li><b>End:</b> Após 4 min, TELEPORTA player pro Spirit World com
 *       flag tome_cursed. Só sai via §6Livro do Êxodo§r (PrayerBook).</li>
 *   <li><b>Cooldown:</b> 6 min (era 10 min) — alinha com duração+recovery.</li>
 *   <li><b>Feedback:</b> abre toast com mensagem clara explicando o que vai
 *       acontecer (já que não tem GUI per se — o "menu" é o horror que se
 *       desenrola).</li>
 * </ul>
 *
 * <p>Right-click = inicia evento com auto-escalation (Phase 1 → 2 → 3 → 4
 * → Spirit World TP).
 * <p>Shift+Right-click = força reset (limpa o evento — só funciona em creative
 * ou se admin).
 */
public class CosmicTriggerItem extends Item {

    /** r38: cooldown = 6 min (duração 4 + 2 de recovery). */
    private static final int COOLDOWN_TICKS = 7200;

    public CosmicTriggerItem(Properties p) { super(p.stacksTo(1)); }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        // SHIFT+RCLICK = reset (apenas creative/op pra emergência)
        if (sp.isShiftKeyDown()) {
            if (!sp.hasPermissions(2) && !sp.isCreative()) {
                sp.displayClientMessage(Component.literal(
                        "§c§oVocê não pode reverter o ritual sem ajuda divina. "
                                + "Use um §6Livro do Êxodo§r§c§o."), true);
                return InteractionResultHolder.fail(stack);
            }
            CosmicHorrorManager.reset(sp);
            // Limpa também o tome_cursed se tiver
            sp.getPersistentData().remove(
                    br.com.murilo.liberthia.dimension.SpiritDimension.NBT_TOME_CURSED);
            sp.displayClientMessage(Component.literal(
                    "§a✓ Cosmic Horror dissipado (admin override)."), true);
            return InteractionResultHolder.success(stack);
        }

        if (sp.getCooldowns().isOnCooldown(this)) {
            sp.displayClientMessage(Component.literal(
                    "§c§oA página ainda arde nas suas mãos. Aguarde."), true);
            return InteractionResultHolder.fail(stack);
        }

        // Bloqueia se já tem horror ativo
        if (CosmicHorrorManager.isActive(sp)) {
            sp.displayClientMessage(Component.literal(
                    "§4§o§lEles já te ouviram. Não chame de novo."), true);
            return InteractionResultHolder.fail(stack);
        }

        // r40: ler o Tomo aumenta forbiddenKnowledge + obsession permanente
        br.com.murilo.liberthia.cosmic.insanity.InsanityData.addForbiddenKnowledge(sp, 15);
        br.com.murilo.liberthia.cosmic.insanity.InsanityData.addObsession(sp, 10);
        br.com.murilo.liberthia.cosmic.insanity.InsanityData.addInsanity(sp, 20);

        // INICIA HORROR
        CosmicHorrorManager.trigger(sp, "forbidden_tome");
        sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        // Feedback inicial (não tem GUI — o horror É o "menu")
        sp.displayClientMessage(Component.literal(
                "§4§l✦ §r§4§oA página se abre. Eles olham de volta."), false);
        sp.displayClientMessage(Component.literal(
                "§c§l[ §r§cTomo Proibido — §e4 minutos§c até o exílio §c§l]"), true);
        sp.level().playSound(null, sp.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 2.0F, 0.4F);

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§4§oTomo Proibido — Convocador Cósmico").withStyle(ChatFormatting.ITALIC));
        t.add(Component.empty());
        t.add(Component.literal("§7§lEfeito: §r§7horror cósmico §o4 minutos§r"));
        t.add(Component.literal("§7  Phase 1 §8(0:00-1:00) §7presença sutil"));
        t.add(Component.literal("§7  Phase 2 §8(1:00-2:00) §5corrupção dimensional"));
        t.add(Component.literal("§7  Phase 3 §8(2:00-3:00) §4ruptura da realidade"));
        t.add(Component.literal("§7  Phase 4 §8(3:00-4:00) §4§lmanifestação plena"));
        t.add(Component.literal("§4§l  → §r§4exílio ao §dMundo Espiritual§r"));
        t.add(Component.empty());
        t.add(Component.literal("§7Pra sair do Mundo Espiritual: §6Livro do Êxodo§r§7."));
        t.add(Component.empty());
        t.add(Component.literal("§8§o\"A leitura desta página deve ser feita uma vez."));
        t.add(Component.literal("§8§o A segunda, eles te lerão.\""));
        t.add(Component.empty());
        t.add(Component.literal("§c§oCD 6min."));
    }
}
