package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r22 (r29: Curios necklace).
 *
 * <h2>Função</h2>
 * Bloqueia COMPLETAMENTE tentativas de possessão (via {@link PossessionAmuletItem})
 * enquanto presente no inventário OU em slot Curios necklace.
 *
 * <h2>Como funciona</h2>
 * <ul>
 *   <li>Detectado em qualquer slot do inv + slot Curios necklace via tag.</li>
 *   <li>{@link br.com.murilo.liberthia.event.PossessionManager#start} checa via
 *       {@link #hasWard(net.minecraft.world.entity.LivingEntity)} antes de iniciar.</li>
 *   <li>{@link PossessionAmuletItem#interactLivingEntity} também checa (UX rápido).</li>
 *   <li>Quando bloqueia, possessor recebe mensagem + partículas SOUL no target.</li>
 * </ul>
 *
 * <h2>r29 — Curios pendant</h2>
 * Item agora foi adicionado ao tag {@code curios:necklace} via
 * {@code data/curios/tags/items/necklace.json} — equipável como colar.
 * {@link #hasWard} checa também slots Curios via reflection-safe call.
 */
public class MindWardItem extends Item {

    public MindWardItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /**
     * True se o entity (player) tem o Mind Ward em qualquer slot do inventário
     * OU em slot Curios (necklace/charm).
     */
    public static boolean hasWard(net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity instanceof Player p)) return false;
        var ward = br.com.murilo.liberthia.registry.ModItems.MIND_WARD.get();
        // 1) Inventário padrão
        if (p.getMainHandItem().is(ward)) return true;
        if (p.getOffhandItem().is(ward)) return true;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(ward)) return true;
        }
        // 2) Curios necklace slot — usa CuriosApi se mod presente
        try {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                    .findFirstCurio(p, s -> s.is(ward))
                    .isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§5§oResguardo Mental").withStyle(ChatFormatting.ITALIC));
        tip.add(Component.literal("§7Bloqueia qualquer §dtentativa de posse§r§7 enquanto"));
        tip.add(Component.literal("§7estiver em seu inventário §dou colar§r§7."));
        tip.add(Component.empty());
        tip.add(Component.literal("§7• Equipável como §dcolar (Curios)§r§7"));
        tip.add(Component.literal("§8§oUm escudo invisível protege sua mente."));
    }
}
