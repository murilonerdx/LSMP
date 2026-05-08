package br.com.murilo.liberthia.matter;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Mecânicas concretas dos 5 perfis (versão básica).
 *
 * <p>Diferente de {@link MatterProfileEvents} que apenas anexa o MobEffect
 * marcador, esta classe altera comportamento real:
 *
 * <ul>
 *   <li><b>DARK:</b> partícula de raiva no jogador, aplica STRENGTH vanilla
 *       enquanto o perfil ativo (sem floodar pacotes — só refresca a cada 60t).</li>
 *   <li><b>DARK_WHITE:</b> aplica HASTE (crafting/mining mais rápido) +
 *       fraqueza pra controle.</li>
 *   <li><b>WHITE:</b> aplica SPEED + chance de teleport involuntário curto
 *       quando toma dano (e perde XP).</li>
 *   <li><b>YELLOW:</b> aplica HUNGER + SLOW_FALLING + partículas erráticas.</li>
 *   <li><b>YELLOW_WHITE:</b> aplica STRENGTH + RESISTANCE — estrategista frio.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MatterProfileMechanics {

    /** Período de aplicação dos efeitos vanilla derivados (3s). */
    private static final int APPLY_PERIOD = 60;

    private MatterProfileMechanics() {}

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % APPLY_PERIOD != 0) return;

        sp.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            int amp = amplifier(profile);
            switch (profile.getActiveType()) {
                case DARK -> {
                    sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, APPLY_PERIOD + 20, amp, true, false));
                    if (amp >= 1)
                        sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, APPLY_PERIOD + 20, 0, true, false));
                    spawnAura(sp, ParticleTypes.ANGRY_VILLAGER, 4);
                }
                case DARK_WHITE -> {
                    sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, APPLY_PERIOD + 20, amp, true, false));
                    sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, APPLY_PERIOD + 20, 0, true, false));
                    spawnAura(sp, ParticleTypes.ENCHANT, 6);
                }
                case WHITE -> {
                    sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, APPLY_PERIOD + 20, amp, true, false));
                    sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, APPLY_PERIOD + 20, 0, true, false));
                    spawnAura(sp, ParticleTypes.END_ROD, 4);
                }
                case YELLOW -> {
                    sp.addEffect(new MobEffectInstance(MobEffects.HUNGER, APPLY_PERIOD + 20, amp, true, false));
                    sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, APPLY_PERIOD + 20, 0, true, false));
                    spawnAura(sp, ParticleTypes.NOTE, 3);
                    // Mood swing aleatório
                    if (sp.level().random.nextInt(4) == 0) randomEmotionalEvent(sp);
                }
                case YELLOW_WHITE -> {
                    sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, APPLY_PERIOD + 20, amp, true, false));
                    sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, APPLY_PERIOD + 20, 0, true, false));
                    spawnAura(sp, ParticleTypes.SOUL_FIRE_FLAME, 3);
                }
                default -> {}
            }
        });
    }

    /** Quando o jogador WHITE toma dano, ~25% chance de blink curto + perda de XP. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        sp.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            if (profile.getActiveType() != MatterProfileType.WHITE) return;
            if (sp.level().random.nextFloat() > 0.25f) return;
            // Blink: teleporta 4-8 blocos numa direção aleatória
            double angle = sp.level().random.nextDouble() * Math.PI * 2;
            double dist = 4 + sp.level().random.nextDouble() * 4;
            double tx = sp.getX() + Math.cos(angle) * dist;
            double tz = sp.getZ() + Math.sin(angle) * dist;
            sp.teleportTo(tx, sp.getY(), tz);
            sp.level().playSound(null, sp.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.4f);
            // Perde uns XP — matéria branca consome lembranças
            int loss = Math.min(10, sp.totalExperience / 10);
            if (loss > 0) sp.giveExperiencePoints(-loss);
        });
    }

    private static int amplifier(MatterProfile p) {
        float v = switch (p.getActiveType()) {
            case DARK         -> p.getDark();
            case WHITE        -> p.getWhite();
            case YELLOW       -> p.getYellow();
            case DARK_WHITE   -> Math.min(p.getDark(), p.getWhite());
            case YELLOW_WHITE -> Math.min(p.getYellow(), p.getWhite());
            default           -> 0;
        };
        if (v >= 75) return 2;
        if (v >= 50) return 1;
        return 0;
    }

    private static void spawnAura(ServerPlayer sp, net.minecraft.core.particles.ParticleOptions p, int count) {
        if (sp.level() instanceof ServerLevel sl) {
            sl.sendParticles(p, sp.getX(), sp.getY() + 1.0, sp.getZ(),
                    count, 0.5, 0.8, 0.5, 0.02);
        }
    }

    /** Pequenos eventos aleatórios pro perfil YELLOW. */
    private static void randomEmotionalEvent(ServerPlayer sp) {
        int kind = sp.level().random.nextInt(4);
        switch (kind) {
            case 0 -> sp.level().playSound(null, sp.blockPosition(),
                    SoundEvents.VILLAGER_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.6f);
            case 1 -> sp.level().playSound(null, sp.blockPosition(),
                    SoundEvents.GHAST_AMBIENT, SoundSource.PLAYERS, 0.6f, 1.2f);
            case 2 -> sp.addEffect(new MobEffectInstance(MobEffects.JUMP, 80, 1, true, false));
            case 3 -> sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
        }
    }
}
