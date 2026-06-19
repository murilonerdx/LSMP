package br.com.murilo.liberthia.loom;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r33: handlers do mundo Loom:
 * <ul>
 *   <li>Cama EXPLODE quando colocada na dim Loom → vira liquid dark matter</li>
 *   <li>Player sem Dark Matter armor toma dano contínuo na Loom (raw matter dmg)</li>
 *   <li>Mobs adicionais não geram aqui (handled em biome JSON)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class LoomEventHandlers {

    private LoomEventHandlers() {}

    @SubscribeEvent
    public static void onSleep(SleepingTimeCheckEvent event) {
        if (!event.getEntity().level().dimension().equals(LoomDimension.LOOM_WORLD)) return;
        // Player tentou usar cama em Loom — explode!
        event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        Player p = event.getEntity();
        var bedPos = event.getSleepingLocation().orElse(p.blockPosition());
        Level level = p.level();
        if (!level.isClientSide) {
            level.removeBlock(bedPos, false);
            level.explode(null, bedPos.getX() + 0.5, bedPos.getY() + 0.5, bedPos.getZ() + 0.5,
                    4.0F, Level.ExplosionInteraction.NONE);
            // Coloca dark_matter liquid block (placeholder — usa Blocks.SOUL_FIRE)
            level.setBlock(bedPos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState(), 3);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        bedPos.getX() + 0.5, bedPos.getY() + 0.5, bedPos.getZ() + 0.5,
                        50, 1, 1, 1, 0.2);
            }
            p.displayClientMessage(Component.literal(
                    "§5§l✦ §r§5O sono é proibido aqui. A cama se converteu."), false);
        }
    }

    /** Player na Loom sem armor: dano contínuo de matéria pura. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!sp.level().dimension().equals(LoomDimension.LOOM_WORLD)) return;
        if (sp.isCreative() || sp.isSpectator()) return;
        if (sp.tickCount % LoomDimension.RAW_DAMAGE_INTERVAL != 0) return;

        if (!hasDarkMatterProtection(sp)) {
            sp.hurt(sp.damageSources().wither(), LoomDimension.RAW_DAMAGE_PER_TICK);
            sp.addEffect(new MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WITHER, 60, 0, false, true));
        }
    }

    private static boolean hasDarkMatterProtection(ServerPlayer sp) {
        // Conta peças de Dark Matter armor equipadas
        int count = 0;
        for (ItemStack s : sp.getArmorSlots()) {
            if (s.getItem() == br.com.murilo.liberthia.registry.ModItems.DARK_MATTER_HELMET.get() ||
                s.getItem() == br.com.murilo.liberthia.registry.ModItems.DARK_MATTER_CHESTPLATE.get() ||
                s.getItem() == br.com.murilo.liberthia.registry.ModItems.DARK_MATTER_LEGGINGS.get() ||
                s.getItem() == br.com.murilo.liberthia.registry.ModItems.DARK_MATTER_BOOTS.get()) {
                count++;
            }
        }
        return count >= 3; // 3+ peças = protegido
    }
}
