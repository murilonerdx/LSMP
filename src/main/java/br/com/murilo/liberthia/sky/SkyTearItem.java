package br.com.murilo.liberthia.sky;

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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r34: item de teste pra demonstrar Sky Tear.
 * Right-click → invoca o ritual de rasgar o céu sobre você.
 */
public class SkyTearItem extends Item {
    public SkyTearItem(Properties p) { super(p.stacksTo(1)); }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        SkyTearSystem.trigger(sp);
        sp.getCooldowns().addCooldown(this, 6000); // 5min CD
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§4§oCorno do Rasgo Dimensional").withStyle(ChatFormatting.ITALIC));
        t.add(Component.literal("§7Right-click: invoca o §4§lSky Tear§r§7 — 15s de"));
        t.add(Component.literal("§7cinemática + spawn do §4Herald do Véu§r§7."));
        t.add(Component.empty());
        t.add(Component.literal("§4§l⚠ CD 5min. Usar só em local seguro."));
    }
}
