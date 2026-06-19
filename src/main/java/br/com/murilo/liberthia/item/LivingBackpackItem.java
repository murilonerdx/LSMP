package br.com.murilo.liberthia.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * r194 — <b>Mochila Biológica Viva</b> (do Coração da Colmeia): armazenamento pessoal portátil
 * (acessa o seu baú de ender de qualquer lugar). Direito p/ abrir.
 */
public class LivingBackpackItem extends Item {
    public LivingBackpackItem(Properties p) { super(p.rarity(Rarity.EPIC).stacksTo(1)); }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            PlayerEnderChestContainer ec = sp.getEnderChestInventory();
            sp.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> ChestMenu.threeRows(id, inv, ec),
                    Component.literal("§2Mochila Viva")));
            level.playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.8f, 0.8f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
