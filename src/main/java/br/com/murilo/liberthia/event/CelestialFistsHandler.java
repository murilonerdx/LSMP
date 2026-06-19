package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.compat.CuriosCompat;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * Mecânicas dos <b>Punhos Celestiais</b> (registrado manual em LiberthiaMod):
 * <ul>
 *   <li><b>Combo 3 golpes</b> — hits dentro de 1s: 3 → 5 → 8 (+knockback), cicla.</li>
 *   <li><b>Dash Ofensivo</b> — correndo + golpe: avanço pra frente + dano em área.</li>
 *   <li><b>Fúria Celestial</b> — 10 golpes seguidos: +velocidade de ataque por 5s.</li>
 * </ul>
 * Aplica quando o player SEGURA os Punhos OU os tem no Curios (mãos) e soca de mão livre.
 */
public final class CelestialFistsHandler {

    private CelestialFistsHandler() {}

    private static final String COMBO = "liberthia.cfist_combo";
    private static final String CONSEC = "liberthia.cfist_consec";
    private static final String LAST = "liberthia.cfist_last";
    private static final String DASH = "liberthia.cfist_dash";
    private static final String FURY = "liberthia.cfist_fury_until";

    private static final int WINDOW = 20;      // 1s pra manter o combo
    private static final int DASH_CD = 20;     // 1s entre dashes
    private static final int FURY_TICKS = 100; // 5s
    private static final UUID FURY_UUID = UUID.fromString("c1e57a90-2d44-4f6b-9b3e-7f1a8c6d2e10");

    private static boolean appliesCombo(Player p) {
        ItemStack main = p.getMainHandItem();
        if (main.is(ModItems.CELESTIAL_FISTS.get())) return true;            // segurando
        return main.isEmpty() && CuriosCompat.isWearing(p, ModItems.CELESTIAL_FISTS.get()); // soco de mão livre
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.level().isClientSide) return;
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)) return; // só corpo-a-corpo
        if (!appliesCombo(attacker)) return;

        LivingEntity target = event.getEntity();
        long now = attacker.level().getGameTime();
        CompoundTag data = attacker.getPersistentData();

        long last = data.getLong(LAST);
        int combo = data.getInt(COMBO);
        int consec = data.getInt(CONSEC);
        if (now - last > WINDOW) { combo = 0; consec = 0; } // janela do combo expirou

        combo = (combo % 3) + 1;   // 1,2,3,1,2,3...
        consec++;
        float dmg = combo == 1 ? 3F : (combo == 2 ? 5F : 8F);
        event.setAmount(dmg);

        // 3º golpe: knockback (empurra pra longe do atacante)
        if (combo == 3) {
            target.knockback(0.8F, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
            if (attacker.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(),
                        1, 0, 0, 0, 0);
            }
        }

        // Dash Ofensivo — correndo
        if (attacker.isSprinting() && now - data.getLong(DASH) > DASH_CD) {
            doDash(attacker, target);
            data.putLong(DASH, now);
        }

        // Fúria Celestial — 10 golpes seguidos
        if (consec >= 10) {
            consec = 0;
            triggerFury(attacker, now);
        }

        data.putInt(COMBO, combo);
        data.putInt(CONSEC, consec);
        data.putLong(LAST, now);

        if (attacker instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(comboText(combo)), true);
        }
    }

    private static void doDash(Player attacker, LivingEntity target) {
        Vec3 look = attacker.getLookAngle();
        attacker.setDeltaMovement(attacker.getDeltaMovement().add(look.x * 0.7, 0.15, look.z * 0.7));
        attacker.hurtMarked = true; // sincroniza a velocidade pro cliente
        if (attacker.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, attacker.getX(), attacker.getY() + 0.2, attacker.getZ(),
                    12, 0.3, 0.1, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(),
                    14, 0.6, 0.5, 0.6, 0.06);
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(3.0),
                    e -> e != attacker && e != target && e.isAlive())) {
                le.hurt(attacker.damageSources().playerAttack(attacker), 4.0F);
            }
            sl.playSound(null, attacker.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 1.0F, 1.4F);
        }
    }

    private static void triggerFury(Player attacker, long now) {
        attacker.getPersistentData().putLong(FURY, now + FURY_TICKS);
        AttributeInstance atkSpeed = attacker.getAttribute(Attributes.ATTACK_SPEED);
        if (atkSpeed != null && atkSpeed.getModifier(FURY_UUID) == null) {
            atkSpeed.addTransientModifier(new AttributeModifier(
                    FURY_UUID, "Celestial Fury", 1.6, AttributeModifier.Operation.ADDITION));
        }
        if (attacker instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal("§b✦ Fúria Celestial!"), true);
            if (sp.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1.0, sp.getZ(),
                        30, 0.4, 0.8, 0.4, 0.1);
            }
        }
    }

    /** Expira a Fúria Celestial. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = event.player;
        if (p.level().isClientSide) return;
        long furyUntil = p.getPersistentData().getLong(FURY);
        if (furyUntil > 0 && p.level().getGameTime() >= furyUntil) {
            AttributeInstance atkSpeed = p.getAttribute(Attributes.ATTACK_SPEED);
            if (atkSpeed != null && atkSpeed.getModifier(FURY_UUID) != null) {
                atkSpeed.removeModifier(FURY_UUID);
            }
            p.getPersistentData().putLong(FURY, 0);
        }
    }

    private static String comboText(int combo) {
        return switch (combo) {
            case 1 -> "§7Combo §f1 §8» §f3";
            case 2 -> "§7Combo §e2 §8» §e5";
            default -> "§6§lCombo 3 §r§8» §c8 §7+ knockback";
        };
    }
}
