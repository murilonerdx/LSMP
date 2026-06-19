package br.com.murilo.liberthia.magic.weapon;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.effect.SynergyEffects;
import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r179: <b>Enchanter's Gauntlet</b> como CURIO de mãos.
 *
 * <p>Quando equipada num slot Curios (hands) e o player dá um golpe
 * <b>melee FORTE</b> (ataque carregado, {@code getAttackStrengthScale > 0.9}),
 * aplica efeitos eldritch no alvo + dano mágico bônus + VFX.
 *
 * <p>O dano bônus usa {@code SchoolDamageSource.eldritch} (NÃO {@code PLAYER_ATTACK}),
 * então não re-dispara este handler (sem loop).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class EnchantersGauntletHandler {

    private static final float STRONG_THRESHOLD = 0.9F;

    private EnchantersGauntletHandler() {}

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        // Só golpe melee DIRETO de player (exclui projéteis/magia/dano bônus)
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;

        LivingEntity target = event.getEntity();
        if (target == sp) return;

        // Gauntlet equipada num slot Curios?
        if (!isGauntletWorn(sp)) return;
        // "Bate forte" = golpe carregado
        if (sp.getAttackStrengthScale(0.5F) < STRONG_THRESHOLD) return;

        ServerLevel sl = sp.serverLevel();

        // Efeitos eldritch do encantador
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, false, true));
        try {
            target.addEffect(new MobEffectInstance(SynergyEffects.HEARTSTOP.get(), 40, 0, false, true));
        } catch (Throwable ignored) {
            // efeito ausente em algum contexto — ignora
        }

        // Dano mágico bônus (não é PLAYER_ATTACK → não re-dispara)
        target.hurt(SchoolDamageSource.eldritch().toVanilla(sl, sp), 3F);

        // VFX
        double cy = target.getY() + target.getBbHeight() / 2.0;
        sl.sendParticles(ParticleTypes.SOUL, target.getX(), cy, target.getZ(), 14, 0.3, 0.4, 0.3, 0.04);
        sl.sendParticles(ParticleTypes.PORTAL, target.getX(), cy, target.getZ(), 10, 0.3, 0.4, 0.3, 0.1);
        sl.playSound(null, target.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8F, 0.7F);
    }

    private static boolean isGauntletWorn(Player p) {
        try {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                    .findFirstCurio(p, s -> s.is(ModItems.ENCHANTERS_GAUNTLET.get()))
                    .isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
