package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager;
import br.com.murilo.liberthia.event.FearMoonEvents;
import br.com.murilo.liberthia.world.FearMoonData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
 * <b>Tambor da Lua do Medo</b> — invoca a Lua do Medo (ver {@code FearMoonEvents}).
 *
 * <ul>
 *   <li>Clique direito À NOITE: ergue a Lua do Medo imediatamente.</li>
 *   <li>Clique direito de DIA: jura a próxima noite como Lua do Medo.</li>
 * </ul>
 * Cooldown de 1 min. Só funciona no Mundo Superior.
 */
public class FearMoonDrumItem extends Item {

    public FearMoonDrumItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        ServerLevel sl = sp.serverLevel();
        if (sl.dimension() != Level.OVERWORLD) {
            sp.displayClientMessage(Component.literal("§cO tambor só ressoa no Mundo Superior."), true);
            return InteractionResultHolder.fail(stack);
        }

        FearMoonData data = FearMoonData.get(sl.getServer());
        if (data.isActive()) {
            sp.displayClientMessage(Component.literal("§4A Lua do Medo já está erguida."), true);
            return InteractionResultHolder.fail(stack);
        }

        // batida grave pra todos
        for (ServerPlayer p : sl.players()) {
            CosmicSoundManager.playPositional(p, SoundEvents.WARDEN_HEARTBEAT,
                    p.getX(), p.getY(), p.getZ(), 1.6F, 0.35F);
        }

        if (sl.isNight()) {
            FearMoonEvents.startFearMoon(sl.getServer(), sl);
            data.setLastRolledDay(sl.getDayTime() / 24000L); // não re-sorteia esta noite
        } else {
            data.setForceNext(true);
            sp.displayClientMessage(
                    Component.literal("§5O tambor jura: §4a próxima noite será de medo."), false);
        }

        sp.getCooldowns().addCooldown(this, 1200); // 1 min
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("☾ Tambor da Lua do Medo").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§7À noite: §cergue a Lua do Medo agora"));
        tooltip.add(Component.literal("§7De dia: §5jura a próxima noite"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§8§oNa Lua do Medo: não durma, não confie em ninguém,"));
        tooltip.add(Component.literal("§8§ofique abrigado — a rua é morte."));
    }
}
