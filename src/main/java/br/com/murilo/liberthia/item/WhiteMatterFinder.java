package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WhiteMatterFinder extends Item {
    public WhiteMatterFinder(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide || !selected || !(entity instanceof Player player)) {
            return;
        }

        if (player.tickCount % 20 == 0) {
            BlockPos playerPos = player.blockPosition();
            BlockPos nearest = null;
            double minDist = Double.MAX_VALUE;

            int radius = 64;
            for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-radius, -32, -radius), playerPos.offset(radius, 32, radius))) {
                if (level.getBlockState(pos).is(ModBlocks.WHITE_MATTER_ORE.get())) {
                    double dist = pos.distSqr(playerPos);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = pos.immutable();
                    }
                }
            }

            if (nearest != null) {
                int directDist = (int) Math.sqrt(minDist);
                player.displayClientMessage(Component.literal("§b✥ Matéria Branca Próxima: §f" + directDist + "m §7(X:" + nearest.getX() + " Y:" + nearest.getY() + " Z:" + nearest.getZ() + ")"), true);
            } else {
                player.displayClientMessage(Component.literal("§c⚠ Nenhum sinal detectado..."), true);
            }
        }
    }
}
