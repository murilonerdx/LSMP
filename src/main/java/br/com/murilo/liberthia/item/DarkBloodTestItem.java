package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.effect.ParticleEffectConfig;
import br.com.murilo.liberthia.effect.ParticleEffectEngine;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import br.com.murilo.liberthia.particle.engine.ConfigurableParticleType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DarkBloodTestItem extends Item {

    public DarkBloodTestItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        ParticleEffectConfig config = ParticleEffectConfig.builder()
                .size(0.18F, 0.55F)
                .alpha(0.95F)
                .lifetime(20)
                .count(24)
                .gravity(-0.01F)
                .friction(0.90F)
                .spin(0.04F)
                .physics(false)
                .emissive(true)
                .speed(0.035D)
                .upwardSpeed(0.015D)
                .spread(0.45D)
                .range(4.0D)
                .build();

        ParticleEffectEngine.meteorShowerTimed(
                serverLevel,
                player,
                config,
                120,    // duração
                10,     // quantidade de meteoros
                7.0D,   // distância mínima do player
                18.0D,  // raio de busca/alvo
                18.0D,  // altura
                1.8F,   // explosão
                3,      // fogo no chão
                80
        );

        player.getCooldowns().addCooldown(this, config.cooldownTicks());

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
