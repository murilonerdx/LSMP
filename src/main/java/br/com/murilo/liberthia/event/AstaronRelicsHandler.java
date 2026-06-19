package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.compat.CuriosCompat;
import br.com.murilo.liberthia.cosmic.ICosmicHorror;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.UUID;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r195 — efeitos das 10 Relíquias de Astaron (anti-cósmico). PlayerTick aplica buffs passivos;
 * LivingHurt trata dano extra vs cósmicos (Luva) e reflexão (Anel).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AstaronRelicsHandler {
    private AstaronRelicsHandler() {}

    private static boolean has(Player p, Item relic) { return CuriosCompat.isWearing(p, relic); }
    private static MobEffectInstance fx(net.minecraft.world.effect.MobEffect e, int dur, int amp) {
        return new MobEffectInstance(e, dur, amp, true, false);
    }

    /** #79: +8 de vida máxima (4 corações) estável enquanto o Warden's Sash está equipado. */
    private static final UUID SASH_HP_ID = UUID.fromString("5e9d7a14-3c2b-4f81-9a6e-2b1c0d4f7a33");
    private static final double SASH_HP_BONUS = 8.0;

    private static void manageSashHealth(ServerPlayer p) {
        AttributeInstance maxHp = p.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp == null) return;
        boolean worn = has(p, ModItems.ASTARON_WARDEN_SASH.get());
        boolean hasMod = maxHp.getModifier(SASH_HP_ID) != null;
        if (worn && !hasMod) {
            // transient: não salva em disco (re-aplicado no tick ao logar) e some
            // limpo ao desequipar — sem flicker, sem dano.
            maxHp.addTransientModifier(new AttributeModifier(
                    SASH_HP_ID, "Astaron Warden Sash", SASH_HP_BONUS,
                    AttributeModifier.Operation.ADDITION));
        } else if (!worn && hasMod) {
            maxHp.removeModifier(SASH_HP_ID);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || ev.player.level().isClientSide) return;
        if (!(ev.player instanceof ServerPlayer p)) return;
        if (p.tickCount % 20 != 0) return;

        // #79: Warden's Sash — +vida máxima via AttributeModifier estável (NÃO
        // mais HEALTH_BOOST reaplicado, que piscava: corações entrando/saindo).
        manageSashHealth(p);

        if (has(p, ModItems.ASTARON_EYE_RELIC.get())) {
            p.addEffect(fx(MobEffects.NIGHT_VISION, 40, 0));
            for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(20), e -> e instanceof ICosmicHorror))
                e.addEffect(fx(MobEffects.GLOWING, 40, 0));
        }
        if (has(p, ModItems.ASTARON_MIND_AMULET.get())) {
            if (SpiritDimension.getSanity(p) < 100) SpiritDimension.addSanity(p, 1);
            p.addEffect(fx(MobEffects.DAMAGE_RESISTANCE, 40, 0));
        }
        if (has(p, ModItems.ASTARON_WARDEN_SASH.get())) {
            // vida máxima é tratada em manageSashHealth (modifier); aqui só resistência
            p.addEffect(fx(MobEffects.DAMAGE_RESISTANCE, 40, 0));
        }
        if (has(p, ModItems.ASTARON_VOID_TREADS.get())) {
            p.addEffect(fx(MobEffects.SLOW_FALLING, 40, 0));
            for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), e -> e instanceof ICosmicHorror))
                if (m.getTarget() == p) { m.setTarget(null); m.setAggressive(false); }
        }
        if (has(p, ModItems.ASTARON_FACELESS_CROWN.get())) {
            for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(10), e -> e instanceof ICosmicHorror))
                if (m.getTarget() == p) { m.setTarget(null); m.setAggressive(false); }
        }
        if (has(p, ModItems.ASTARON_ESSENCE_RELIC.get())) {
            p.addEffect(fx(MobEffects.REGENERATION, 60, 0));
            p.addEffect(fx(MobEffects.ABSORPTION, 120, 1));
        }
        if (has(p, ModItems.ASTARON_VOID_CORE.get())) {
            p.addEffect(fx(MobEffects.DAMAGE_BOOST, 60, 0));
            p.addEffect(fx(MobEffects.DIG_SPEED, 60, 0));
        }
        if (has(p, ModItems.ASTARON_STAR_PENDANT.get())) {
            p.addEffect(fx(MobEffects.FIRE_RESISTANCE, 60, 0));
            p.addEffect(fx(MobEffects.WATER_BREATHING, 60, 0));
            p.removeEffect(MobEffects.WITHER);
            p.removeEffect(MobEffects.POISON);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        // Luva do Caçador: dano extra contra cósmicos (NÃO no dano refletido do Anel = sem double-dip)
        if (e.getSource().getEntity() instanceof Player atk && victim instanceof ICosmicHorror
                && !e.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)
                && has(atk, ModItems.ASTARON_HUNTER_GAUNTLET.get())) {
            e.setAmount(e.getAmount() * 2.5F);
        }
        // Anel de Reflexão: reflete 40% do dano cósmico de volta
        if (victim instanceof Player p && e.getSource().getEntity() instanceof LivingEntity src
                && src instanceof ICosmicHorror && has(p, ModItems.ASTARON_MIRROR_RING.get())) {
            float reflected = e.getAmount() * 0.4F;
            e.setAmount(e.getAmount() * 0.8F);
            if (reflected > 0) src.hurt(p.damageSources().thorns(p), reflected);
        }
    }
}
