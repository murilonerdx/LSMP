package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.YellowMatterSwordItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class YellowMatterSwordEvents {

    private YellowMatterSwordEvents() {
    }

    @SubscribeEvent
    public static void onPlayerHurtWhileChanneling(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        ItemStack useItem = player.getUseItem();

        if (!(useItem.getItem() instanceof YellowMatterSwordItem)) {
            return;
        }

        int channelTicks = YellowMatterSwordItem.getChannelTicks(useItem);

        if (channelTicks <= 0) {
            return;
        }

        int seconds = Math.max(1, channelTicks / 20);

        // Backlash: quanto mais tempo canalizando, pior a interrupção.
        float backlashDamage = 8.0F + seconds * 1.5F;

        // Aumenta o dano do hit que ele já ia receber.
        // Isso evita loop/infinite recursion de player.hurt() dentro do próprio evento.
        event.setAmount(event.getAmount() + backlashDamage);

        YellowMatterSwordItem.setChannelTicks(useItem, 0);
        player.stopUsingItem();

        player.displayClientMessage(
                Component.literal("O fluxo da Matéria Clara foi rompido!")
                        .withStyle(ChatFormatting.RED),
                true
        );

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.FLASH,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );

            serverLevel.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    35,
                    0.8D,
                    0.8D,
                    0.8D,
                    0.08D
            );

            serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    25,
                    0.7D,
                    0.7D,
                    0.7D,
                    0.05D
            );

            serverLevel.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS,
                    1.0F,
                    0.7F
            );

            serverLevel.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS,
                    0.8F,
                    1.5F
            );
        }
    }
}
