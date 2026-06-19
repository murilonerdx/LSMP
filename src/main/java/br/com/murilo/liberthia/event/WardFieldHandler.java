package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.compat.mna.AntiMagicWardHandler;
import br.com.murilo.liberthia.compat.mna.MagicDetect;
import br.com.murilo.liberthia.item.AntiMagicArmorItem;
import br.com.murilo.liberthia.item.AntiMagicShieldItem;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModEnchantments;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * r180b — campo do encantamento <b>Dissonância</b> ({@link ModEnchantments#WARD}):
 * <ul>
 *   <li><b>-40% dano mágico recebido</b> se a vítima usa a armadura encantada.</li>
 *   <li><b>Quebra de feitiços por proximidade</b>: quem carrega itens de magia é
 *       REPELIDO ao chegar perto de um player anti-magia (Dissonância OU Spellbreaker
 *       na mão) + leva o efeito Antimagia. Identidade visual ciano.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class WardFieldHandler {

    private static final DustParticleOptions CYAN =
            new DustParticleOptions(new Vector3f(0.17F, 0.84F, 0.84F), 1.0F);
    private static final double REPEL_RADIUS = 4.0;

    private WardFieldHandler() {}

    /** Dissonância (encantamento) + conjunto anti-magia + escudo: reduzem dano mágico. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        if (!isMagicDamage(e.getSource())) return;
        float reduction = 0F;
        if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.WARD.get(), victim) > 0) reduction += 0.40F;
        if (victim instanceof Player vp) {
            reduction += 0.10F * antiMagicArmorCount(vp);            // conjunto completo (4) = -40%
            if (vp.isBlocking() && isAntiMagicShield(vp.getUseItem())) reduction = Math.max(reduction, 0.80F);
        }
        if (reduction > 0F) {
            e.setAmount(e.getAmount() * (1F - Math.min(0.90F, reduction)));
        }
    }

    /** Repulsão por proximidade: anti-magia empurra os portadores de magia. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer wp) || wp.tickCount % 5 != 0) return;
        if (!isWarded(wp) || !(wp.level() instanceof ServerLevel sl)) return;

        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, wp.getBoundingBox().inflate(REPEL_RADIUS))) {
            if (le == wp || !le.isAlive()) continue;
            if (le instanceof Player op && (op.isCreative() || op.isSpectator())) continue;
            if (!isMagicCarrier(le)) continue;

            Vec3 away = new Vec3(le.getX() - wp.getX(), 0, le.getZ() - wp.getZ());
            if (away.lengthSqr() < 1.0e-3) {
                away = new Vec3(le.getRandom().nextDouble() - 0.5, 0, le.getRandom().nextDouble() - 0.5);
            }
            away = away.normalize();
            le.push(away.x * 0.6, 0.15, away.z * 0.6);
            if (le instanceof ServerPlayer spp) {
                spp.hurtMarked = true;
                if (spp.tickCount % 30 == 0) {
                    spp.displayClientMessage(Component.literal("§b✦ Um campo anti-magia te repele."), true);
                }
            } else {
                le.hasImpulse = true;
            }
            try {
                le.addEffect(new MobEffectInstance(ModEffects.ANTIMAGIA.get(), 30, 0, false, false));
            } catch (Throwable ignored) {}
            sl.sendParticles(CYAN, le.getX(), le.getY() + 1.0, le.getZ(), 4, 0.3, 0.5, 0.3, 0.0);
        }
    }

    private static boolean isMagicDamage(DamageSource src) {
        return src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC)
                || AntiMagicWardHandler.isMnaSource(src);
    }

    /** Player anti-magia = Dissonância OU peça do conjunto OU escudo/Spellbreaker na mão. */
    private static boolean isWarded(Player p) {
        if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.WARD.get(), p) > 0) return true;
        if (antiMagicArmorCount(p) > 0) return true;
        if (isAntiMagicShield(p.getMainHandItem()) || isAntiMagicShield(p.getOffhandItem())) return true;
        return p.getMainHandItem().is(ModItems.SPELLBREAKER.get())
                || p.getOffhandItem().is(ModItems.SPELLBREAKER.get());
    }

    private static int antiMagicArmorCount(Player p) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (p.getItemBySlot(slot).getItem() instanceof AntiMagicArmorItem) n++;
        }
        return n;
    }

    private static boolean isAntiMagicShield(ItemStack s) {
        return s.getItem() instanceof AntiMagicShieldItem;
    }

    /** Carrega magia = player com item de magia, ou mob segurando item de magia. */
    private static boolean isMagicCarrier(LivingEntity le) {
        if (le instanceof Player p) return MagicDetect.isMagicUser(p);
        return MagicDetect.isMagicItem(le.getMainHandItem())
                || MagicDetect.isMagicItem(le.getOffhandItem());
    }
}
