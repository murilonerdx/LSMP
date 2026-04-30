package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.particle.engine.ConfigurableParticleOptions;
import br.com.murilo.liberthia.registry.ModItems;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = LiberthiaMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class HeldItemParticleEvents {

    private HeldItemParticleEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Player player = event.player;

        if (!player.isAlive() || player.isSpectator()) {
            return;
        }

        /*
         * Não spawna todo tick para não pesar.
         * 2 = a cada 2 ticks.
         * 4 = mais leve.
         */
        if (player.tickCount % 2 != 0) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (mainHand.is(ModItems.DARK_BLOOD_TEST_ITEM.get())) {
            spawnItemParticles(serverLevel, player, InteractionHand.MAIN_HAND);
        }

        if (offHand.is(ModItems.DARK_BLOOD_TEST_ITEM.get())) {
            spawnItemParticles(serverLevel, player, InteractionHand.OFF_HAND);
        }
    }

    private static void spawnItemParticles(ServerLevel level, Player player, InteractionHand hand) {
        RandomSource random = player.getRandom();

        ConfigurableParticleOptions particleOptions = new ConfigurableParticleOptions(
                ModParticles.ENGINE_PARTICLE.get(),

                // cor RGBA
                0.65F,  // red
                0.00F,  // green
                0.04F,  // blue
                0.85F,  // alpha

                // tamanho inicial e final
                0.08F,
                0.28F,

                // duração da partícula
                18,

                // gravidade
                -0.015F,

                // fricção
                0.88F,

                // rotação
                0.06F,

                // física
                false,

                // brilho
                true
        );

        Vec3 look = player.getLookAngle().normalize();

        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        if (right.lengthSqr() < 0.0001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        HumanoidArm mainArm = player.getMainArm();

        boolean rightHand;
        if (hand == InteractionHand.MAIN_HAND) {
            rightHand = mainArm == HumanoidArm.RIGHT;
        } else {
            rightHand = mainArm == HumanoidArm.LEFT;
        }

        double sideMultiplier = rightHand ? 1.0D : -1.0D;

        /*
         * Posição aproximada da mão no mundo.
         * Não é 100% grudado no modelo visual, mas fica bem próximo.
         */
        Vec3 handPos = player.position()
                .add(0.0D, player.getEyeHeight() - 0.35D, 0.0D)
                .add(look.scale(0.55D))
                .add(right.scale(0.32D * sideMultiplier));

        int amount = 3;

        for (int i = 0; i < amount; i++) {
            double ox = (random.nextDouble() - 0.5D) * 0.10D;
            double oy = (random.nextDouble() - 0.5D) * 0.10D;
            double oz = (random.nextDouble() - 0.5D) * 0.10D;

            Vec3 particlePos = handPos.add(ox, oy, oz);

            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.015D + random.nextDouble() * 0.025D;

            Vec3 swirl = right.scale(Math.cos(angle) * radius)
                    .add(0.0D, Math.sin(angle) * radius, 0.0D);

            Vec3 velocity = look.scale(0.012D + random.nextDouble() * 0.015D)
                    .add(swirl)
                    .add(0.0D, 0.012D + random.nextDouble() * 0.018D, 0.0D);

            level.sendParticles(
                    particleOptions,
                    particlePos.x,
                    particlePos.y,
                    particlePos.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    1.0D
            );
        }
    }
}
