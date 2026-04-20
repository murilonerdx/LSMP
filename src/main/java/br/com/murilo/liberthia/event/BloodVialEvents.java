package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Right-click on Blood Fluid with empty Blood Vial → filled vial. */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public class BloodVialEvents {

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!stack.is(ModItems.BLOOD_VIAL.get())) return;

        HitResult hit = player.pick(5.0D, 1.0F, true);
        if (hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        Level level = player.level();
        BlockState state = level.getBlockState(pos);

        if (!state.is(ModBlocks.BLOOD_FLUID_BLOCK.get())) return;
        if (level.isClientSide) {
            event.setCanceled(true);
            return;
        }

        // Consume 1 vial, give filled
        if (!player.getAbilities().instabuild) stack.shrink(1);
        ItemStack filled = new ItemStack(ModItems.BLOOD_VIAL_FILLED.get());
        if (!player.getInventory().add(filled)) {
            player.drop(filled, false);
        }
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
        event.setCanceled(true);
    }
}
