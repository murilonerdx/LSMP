package br.com.murilo.liberthia.observation.item;

import br.com.murilo.liberthia.observation.source.MagicLevelData;
import br.com.murilo.liberthia.observation.source.SourceData;
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
 * v0.1.22 r71: <b>4 itens de upgrade Source</b>.
 *
 * <ul>
 *   <li>{@link SourceCrystalItem} — single-use: +10 max Source permanente</li>
 *   <li>{@link SourceCatalystItem} — refill instantâneo +50 Source</li>
 *   <li>{@link SourceLensItem} — +1 XP magia ao usar</li>
 *   <li>{@link SoulFragmentItem} — material raro de enchant (sem use direto)</li>
 * </ul>
 */
public final class SourceUpgradeItems {

    private SourceUpgradeItems() {}

    /** Source Crystal — upgrade permanente +10 max Source. */
    public static class SourceCrystalItem extends Item {
        public SourceCrystalItem(Properties p) {
            super(p.stacksTo(16).rarity(Rarity.RARE));
        }
        @Override public boolean isFoil(ItemStack s) { return true; }
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            int currentMax = SourceData.getMax(sp);
            if (currentMax >= 2000) {
                sp.displayClientMessage(Component.literal("§7Capacidade máxima de Source atingida (2000)."), true);
                return InteractionResultHolder.fail(stack);
            }
            SourceData.setMax(sp, currentMax + 10);
            SourceData.add(sp, 10); // também enche
            stack.shrink(1);
            sp.displayClientMessage(Component.literal(
                "§5§l+10 Max Source§r §7(novo: §e" + SourceData.getMax(sp) + "§7)"), true);
            return InteractionResultHolder.consume(stack);
        }
        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oCristal de Source").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Right-click: §e+10 max Source permanente"));
            t.add(Component.empty());
            t.add(Component.literal("§8§o\"Mais fundo se observa, mais profundo se cabe.\""));
        }
    }

    /** Source Catalyst — refill instantâneo +50 Source. */
    public static class SourceCatalystItem extends Item {
        public SourceCatalystItem(Properties p) {
            super(p.stacksTo(16).rarity(Rarity.UNCOMMON));
        }
        @Override public boolean isFoil(ItemStack s) { return true; }
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            int before = SourceData.get(sp);
            int max = SourceData.getMax(sp);
            if (before >= max) {
                sp.displayClientMessage(Component.literal("§7Source já está cheio."), true);
                return InteractionResultHolder.fail(stack);
            }
            SourceData.add(sp, 50);
            stack.shrink(1);
            sp.displayClientMessage(Component.literal(
                "§3+50 Source §7(§e" + SourceData.get(sp) + "§7/§e" + max + "§7)"), true);
            return InteractionResultHolder.consume(stack);
        }
        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§3§oCatalisador de Source").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Right-click: §3+50 Source§7 imediato"));
        }
    }

    /** Source Lens — +XP ao usar (ajuda level up). */
    public static class SourceLensItem extends Item {
        public SourceLensItem(Properties p) {
            super(p.stacksTo(8).rarity(Rarity.RARE));
        }
        @Override public boolean isFoil(ItemStack s) { return true; }
        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
            ItemStack stack = user.getItemInHand(hand);
            if (level.isClientSide) return InteractionResultHolder.success(stack);
            if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
            br.com.murilo.liberthia.observation.source.MagicLevelEvents.awardXp(sp, 25, "lens");
            stack.shrink(1);
            return InteractionResultHolder.consume(stack);
        }
        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§e§oLente de Source").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Right-click: §e+25 XP de magia"));
            t.add(Component.literal("§8§oAjuda a subir level mais rápido"));
        }
    }

    /** Soul Fragment — material raro pra Imbuement. */
    public static class SoulFragmentItem extends Item {
        public SoulFragmentItem(Properties p) {
            super(p.stacksTo(64).rarity(Rarity.EPIC));
        }
        @Override public boolean isFoil(ItemStack s) { return true; }
        @Override
        public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
            t.add(Component.literal("§5§oFragmento de Alma").withStyle(ChatFormatting.ITALIC));
            t.add(Component.literal("§7Material raro pra encantar espadas com feitiços."));
            t.add(Component.literal("§8§oDrop apenas em mobs da Spirit World."));
        }
    }
}
