package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.entity.projectile.MatterBulletEntity;
import br.com.murilo.liberthia.entity.projectile.MatterBulletEntity.Type;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * r180b — <b>Pistola de Matérias</b>. Atira a MATÉRIA que você carrega (consome 1 ingot
 * do inventário): Amarela / Escura / Clara → projétil com o efeito dela. <b>Shift + usar
 * = TIRO FUNDIDO</b>: mistura 1 de cada matéria que você tiver (≥2) num único tiro com
 * TODOS os efeitos. Munição = `*_matter_ingot`. (A "máquina de balas" dedicada fica pra
 * depois — por ora a munição é a matéria crua.)
 */
public class MatterPistolItem extends Item {

    public MatterPistolItem(Properties props) {
        super(props.stacksTo(1).durability(512).rarity(Rarity.RARE));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(gun);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(gun);

        boolean creative = player.getAbilities().instabuild;
        List<Type> avail = new ArrayList<>();
        if (creative || has(player, ModItems.YELLOW_MATTER_INGOT.get())) avail.add(Type.YELLOW);
        if (creative || has(player, ModItems.DARK_MATTER_INGOT.get()))   avail.add(Type.DARK);
        if (creative || has(player, ModItems.CLEAR_MATTER_INGOT.get()))  avail.add(Type.WHITE);
        if (avail.isEmpty()) {
            player.displayClientMessage(Component.literal("§cSem matéria — carregue ingots de matéria."), true);
            return InteractionResultHolder.fail(gun);
        }

        Type shot;
        boolean fused = player.isShiftKeyDown() && avail.size() >= 2;
        if (fused) {
            shot = Type.FUSED;
            if (!creative) for (Type t : avail) consumeOne(player, itemFor(t));
        } else {
            shot = avail.get(0); // prioridade: YELLOW > DARK > WHITE
            if (!creative) consumeOne(player, itemFor(shot));
        }

        MatterBulletEntity b = new MatterBulletEntity(level, player, shot);
        b.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.8F, 0.6F);
        level.addFreshEntity(b);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS,
                0.7F, fused ? 0.7F : 1.3F);
        gun.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        player.getCooldowns().addCooldown(this, fused ? 20 : 6);
        return InteractionResultHolder.success(gun);
    }

    private static Item itemFor(Type t) {
        return switch (t) {
            case YELLOW -> ModItems.YELLOW_MATTER_INGOT.get();
            case WHITE  -> ModItems.CLEAR_MATTER_INGOT.get();
            default     -> ModItems.DARK_MATTER_INGOT.get();
        };
    }

    private static boolean has(Player p, Item item) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) return true;
        }
        return false;
    }

    private static void consumeOne(Player p, Item item) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) { s.shrink(1); return; }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("✦ Pistola de Matérias").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("§7Usar: atira a matéria que você tem §8(amarela/escura/clara)"));
        tip.add(Component.literal("§7§lShift§r§7 + usar: §dTiro Fundido §7(mistura tudo = todos os efeitos)"));
        tip.add(Component.literal("§8§oMunição: ingots de matéria"));
    }
}
