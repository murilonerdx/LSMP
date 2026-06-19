package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.dimension.DimensionalRiftEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * r179: <b>Abridor de Fenda</b> — clique direito abre uma {@link DimensionalRiftEntity}
 * (rasgo animado) ~2.5 blocos à frente, na altura do player. A fenda fica lá
 * <b>até ser apagada</b> por {@code /liberthia rift clear}. Atravessá-la leva ao
 * Mundo Espiritual.
 */
public class RiftOpenerItem extends Item {

    public RiftOpenerItem(Properties props) {
        super(props.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            if (sp.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.fail(stack);
            }
            Vec3 look = sp.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            if (flat.lengthSqr() < 1.0e-4) flat = new Vec3(0, 0, 1);
            flat = flat.normalize();
            double d = 2.5;
            double x = sp.getX() + flat.x * d;
            double y = sp.getY();
            double z = sp.getZ() + flat.z * d;

            DimensionalRiftEntity rift = ModEntities.DIMENSIONAL_RIFT.get().create(sl);
            if (rift != null) {
                rift.moveTo(x, y, z, 0F, 0F);
                sl.addFreshEntity(rift);
                sl.playSound(null, sp.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.9F, 0.6F);
                sp.displayClientMessage(Component.literal("§5Você rasga o tecido da realidade...")
                        .withStyle(ChatFormatting.ITALIC), true);
            }
            sp.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5Abre uma Fenda Dimensional no ar"));
        tooltip.add(Component.literal("§7Atravesse → §dMundo Espiritual"));
        tooltip.add(Component.literal("§8Apague com §7/liberthia rift clear"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
