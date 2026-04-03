package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.InfectionLogic;
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

        RadiationSnapshot radiation = scanRadiation(player, level);
        int radiationLevel = radiation.level();

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
                String icon;
                String color;
                if (radiationLevel >= 15) {
                    icon = "§4§l⚠";
                    color = "§4";
                } else if (radiationLevel >= 8) {
                    icon = "§6⚠";
                    color = "§6";
                } else if (radiationLevel >= 3) {
                    icon = "§e∿";
                    color = "§e";
                } else {
                    icon = "§a∿";
                    color = "§a";
                }
                // Compact format: icon Rad:8/20 | 156p | 12.5%
                String bhWarn = radiation.particles() >= 1500 || radiation.density() >= 0.80f
                        ? " §4§l[BH!]" : "";
                player.displayClientMessage(Component.literal(
                        icon + color + " Rad:" + radiationLevel + "/20 §7| §f"
                                + radiation.particles() + "p §7| §f"
                                + String.format("%.1f%%", radiation.density() * 100.0f)
                                + bhWarn
                ), true);
            }
        }
    }

    private RadiationSnapshot scanRadiation(ServerPlayer player, Level level) {
        BlockPos center = player.blockPosition();
        int particles = InfectionLogic.countDarkMatterParticles(level, center, 16, 6);
        float density = InfectionLogic.getChunkInfectionDensity(level, center);

        int particleLevel = particles > 0 ? Math.min(20, Math.max(1, particles / 40)) : 0;
        int densityLevel = Math.min(20, Math.round(density * 20.0f));
        int combined = Math.max(particleLevel, densityLevel);
        return new RadiationSnapshot(combined, particles, density);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            RadiationSnapshot radiation = scanRadiation(serverPlayer, level);
            boolean safe = radiation.particles() < 20 && radiation.density() < 0.02f;
            String msg = safe
                    ? "§a[Geiger] Área segura — nenhuma radiação detectada."
                    : "§e[Geiger] Nível de radiação: " + radiation.level() + "/20"
                    + " §8| Partículas: §f" + radiation.particles()
                    + " §8| Densidade: §f" + String.format("%.1f%%", radiation.density() * 100.0f);
            player.displayClientMessage(Component.literal(msg), false);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    private record RadiationSnapshot(int level, int particles, float density) {}
}
