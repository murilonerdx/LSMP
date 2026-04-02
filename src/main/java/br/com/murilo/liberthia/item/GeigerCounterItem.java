package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModFluids;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class GeigerCounterItem extends Item {
    private int tickCounter = 0;

    public GeigerCounterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }

        if (!isSelected && player.getOffhandItem() != stack) {
            return;
        }

        tickCounter++;

        int radiationLevel = scanRadiation(player, level);

        if (radiationLevel > 0) {
            // Intervalo dinâmico: de 20 ticks (1s) a 2 ticks (0.1s)
            int interval = Math.max(2, 20 - (radiationLevel));

            if (tickCounter % interval == 0) {
                float pitch = 0.85F + (radiationLevel / 40.0F) + (level.random.nextFloat() * 0.1F);
                level.playSound(null, player.blockPosition(), ModSounds.GEIGER_TICK.get(),
                        SoundSource.PLAYERS, 0.5F, Math.min(pitch, 2.0F));
                
                // Em níveis críticos, adiciona estalidos extras aleatórios
                if (radiationLevel >= 15 && level.random.nextFloat() > 0.7F) {
                    level.playSound(null, player.blockPosition(), ModSounds.GEIGER_TICK.get(),
                            SoundSource.PLAYERS, 0.4F, pitch + 0.2F);
                }
            }

            if (tickCounter % 20 == 0) {
                String severity;
                if (radiationLevel >= 15) {
                    severity = "§4§l⚠ RADIAÇÃO CRÍTICA ⚠";
                } else if (radiationLevel >= 8) {
                    severity = "§6⚠ Radiação Alta";
                } else if (radiationLevel >= 3) {
                    severity = "§e∿ Radiação Moderada";
                } else {
                    severity = "§a∿ Radiação Baixa";
                }
                player.displayClientMessage(Component.literal(severity + " §7[" + radiationLevel + "]"), true);
            }
        }
    }

    private int scanRadiation(ServerPlayer player, Level level) {
        BlockPos center = player.blockPosition();
        int count = 0;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -4, -8), center.offset(8, 4, 8))) {
            BlockState blockState = level.getBlockState(pos);
            FluidState fluidState = level.getFluidState(pos);

            if (blockState.is(ModBlocks.DARK_MATTER_BLOCK.get())
                    || blockState.is(ModBlocks.CORRUPTED_SOIL.get())
                    || blockState.is(ModBlocks.DARK_MATTER_ORE.get())
                    || blockState.is(ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get())
                    || fluidState.getType().isSame(ModFluids.DARK_MATTER.get())
                    || fluidState.getType().isSame(ModFluids.FLOWING_DARK_MATTER.get())) {
                count++;
            }
        }

        return Math.min(20, count);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            int radiation = scanRadiation(serverPlayer, level);
            String msg = radiation == 0
                    ? "§a[Geiger] Área segura — nenhuma radiação detectada."
                    : "§e[Geiger] Nível de radiação: " + radiation + "/20";
            player.displayClientMessage(Component.literal(msg), false);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
