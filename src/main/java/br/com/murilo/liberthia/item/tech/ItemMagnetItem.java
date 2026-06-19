package br.com.murilo.liberthia.item.tech;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r180c — <b>Ímã de Itens</b> (FE): quando ATIVO, puxa drops próximos pro jogador,
 * consumindo energia. Clique direito p/ ligar/desligar. Sem FE → desliga sozinho.
 */
public class ItemMagnetItem extends Item {
    public static final int CAP = 100_000, COST = 4;
    private static final double RADIUS = 7.0;
    private static final String ACTIVE = "Active";

    public ItemMagnetItem() { super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)); }

    @Override public @Nullable ICapabilityProvider initCapabilities(ItemStack s, @Nullable CompoundTag n) {
        return TechEnergy.provider(s, CAP, 4_000);
    }

    public static boolean isActive(ItemStack s) { return s.getOrCreateTag().getBoolean(ACTIVE); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack s = p.getItemInHand(hand);
        boolean now = !isActive(s);
        s.getOrCreateTag().putBoolean(ACTIVE, now);
        if (level.isClientSide)
            level.playSound(p, p.blockPosition(), now ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.4F, now ? 1.6F : 1.0F);
        return InteractionResultHolder.success(s);
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player p) || !isActive(stack)) return;
        if (!TechEnergy.has(stack)) { stack.getOrCreateTag().putBoolean(ACTIVE, false); return; }
        if (p.tickCount % 2 != 0) return;
        AABB box = p.getBoundingBox().inflate(RADIUS);
        boolean pulled = false;
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (ie.hasPickUpDelay() || !ie.isAlive()) continue;
            Vec3 dir = p.position().add(0, 0.4, 0).subtract(ie.position());
            if (dir.lengthSqr() < 1.2) continue;
            ie.setDeltaMovement(dir.normalize().scale(0.45));
            ie.setPickUpDelay(0);
            pulled = true;
        }
        if (pulled && !TechEnergy.drain(stack, COST, CAP)) stack.getOrCreateTag().putBoolean(ACTIVE, false);
    }

    @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable(isActive(s) ? "tooltip.liberthia.magnet_on" : "tooltip.liberthia.magnet_off")
                .withStyle(isActive(s) ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
        tip.add(Component.literal(TechEnergy.energy(s) + " / " + CAP + " FE").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @Override public boolean isBarVisible(ItemStack s) { return true; }
    @Override public int getBarWidth(ItemStack s) { return TechEnergy.barWidth(s, CAP); }
    @Override public int getBarColor(ItemStack s) { return TechEnergy.barColor(); }
    @Override public boolean isFoil(ItemStack s) { return isActive(s); }
}
