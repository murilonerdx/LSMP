package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.22 r31: Lágrima de Sandalphon — previne dano fatal uma vez.
 *
 * <p>Quando o player com a Lágrima (Curios necklace) levaria um hit fatal:
 * <ul>
 *   <li>Cancela todo o dano da hit</li>
 *   <li>Cura player pra 50% HP máximo</li>
 *   <li>Aplica Resistance V por 10s + Regeneration III</li>
 *   <li>Consome a Lágrima (item é destruído — uso único)</li>
 *   <li>VFX: pilar de luz dourada + som de levelup</li>
 * </ul>
 *
 * <p>Sandalphon é o arcanjo das orações no folclore judaico/cabalista —
 * transforma súplicas dos mortais em coroas pra Deus. A lágrima é uma
 * resposta de misericórdia.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AngelTearHandler {

    private AngelTearHandler() {}

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.getAmount() < sp.getHealth()) return; // não letal

        // Procura a lágrima nos slots Curios
        ItemStack tear = findTear(sp);
        if (tear.isEmpty()) return;

        // Cancela todo o dano
        event.setCanceled(true);

        // Heal 50% HP máximo
        sp.setHealth(sp.getMaxHealth() * 0.5F);
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 4, true, true));
        sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2, true, true));
        sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0, true, true));

        // Consome a lágrima
        tear.shrink(1);

        if (sp.level() instanceof ServerLevel sl) {
            // VFX pilar dourado
            for (int y = 0; y < 10; y++) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        sp.getX(), sp.getY() + y, sp.getZ(), 8, 0.3, 0.2, 0.3, 0.05);
            }
            sl.playSound(null, sp.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 2.0F);
            sl.playSound(null, sp.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.5F);
        }
        sp.displayClientMessage(Component.literal(
                "§e§l✦ §r§eSandalphon chora por você. A morte foi adiada."), false);
    }

    private static ItemStack findTear(Player p) {
        var tear = ModItems.ANGEL_TEAR_AMULET.get();
        try {
            var opt = top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                    .findFirstCurio(p, s -> s.is(tear));
            if (opt.isPresent()) return opt.get().stack();
        } catch (Throwable ignored) {}
        // Fallback: também aceita no inv principal
        if (p.getMainHandItem().is(tear)) return p.getMainHandItem();
        if (p.getOffhandItem().is(tear)) return p.getOffhandItem();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(tear)) return s;
        }
        return ItemStack.EMPTY;
    }
}
