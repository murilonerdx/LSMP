package br.com.murilo.liberthia.compat.mna;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r180: <b>Counter ao Mana and Artifice</b> (e mods de magia em geral) sem depender da
 * API deles. Detecta dano/projétil que vem do M&A pelo PACOTE da classe em runtime
 * ({@code com.mna.*}) — então não precisa do M&A no classpath e não quebra o build.
 *
 * <p>Quem porta o <b>Selo Nulo</b> ({@code null_seal}, na mão/inventário/Curios):
 * <ul>
 *   <li>Nega ~90% do dano vindo do M&A (50% de dano mágico genérico) + reflete um pouco no caster.</li>
 *   <li>Projéteis de feitiço do M&A são <b>refletidos de volta</b> (counterspell).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AntiMagicWardHandler {

    private AntiMagicWardHandler() {}

    /** True se o dano vem do Mana and Artifice (classe da entidade/fonte em com.mna). */
    public static boolean isMnaSource(DamageSource src) {
        if (src == null) return false;
        Entity e = src.getEntity();
        Entity d = src.getDirectEntity();
        if (e != null && e.getClass().getName().startsWith("com.mna")) return true;
        if (d != null && d.getClass().getName().startsWith("com.mna")) return true;
        String id = src.getMsgId();
        return id != null && (id.contains("mna") || id.contains("arcane") || id.contains("mana"));
    }

    private static boolean isMagic(DamageSource src) {
        return src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC);
    }

    /** Tem o Selo Nulo na mão/inventário ou num slot Curios? */
    public static boolean hasWard(Player p) {
        if (p.getInventory().contains(new ItemStack(ModItems.NULL_SEAL.get()))) return true;
        try {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                    .findFirstCurio(p, s -> s.is(ModItems.NULL_SEAL.get())).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (victim.level().isClientSide) return;
        if (!hasWard(victim)) return;

        DamageSource src = event.getSource();
        boolean mna = isMnaSource(src);
        boolean magic = isMagic(src);
        if (!mna && !magic) return;

        float factor = mna ? 0.10F : 0.50F;        // M&A: nega 90% · mágico genérico: 50%
        float blocked = event.getAmount() * (1.0F - factor);
        event.setAmount(event.getAmount() * factor);

        if (victim.level() instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                    victim.getX(), victim.getY() + 1.2, victim.getZ(), 14, 0.4, 0.5, 0.4, 0.1);
        }
        victim.level().playSound(null, victim.blockPosition(),
                net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.6F);

        // reflete parte no caster
        if (src.getEntity() instanceof LivingEntity caster && caster != victim && blocked > 1.0F) {
            caster.hurt(victim.damageSources().thorns(victim), Math.min(8.0F, blocked * 0.4F));
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        var proj = event.getProjectile();
        if (proj == null || proj.level().isClientSide) return;
        // só projéteis do M&A
        if (!proj.getClass().getName().startsWith("com.mna")) return;
        // o impacto é num player com o Selo?
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof Player victim) || !hasWard(victim)) return;

        // REFLETE: cancela o impacto e inverte a velocidade (volta pro caster)
        event.setCanceled(true);
        Vec3 v = proj.getDeltaMovement().scale(-1.1);
        proj.setDeltaMovement(v);
        proj.setYRot((float) (Math.atan2(v.x, v.z) * (180.0 / Math.PI)));
        proj.setXRot((float) (Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)) * (180.0 / Math.PI)));
        try { if (proj.getOwner() instanceof LivingEntity le) proj.setOwner(victim); } catch (Throwable ignored) {}

        if (proj.level() instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                    proj.getX(), proj.getY(), proj.getZ(), 12, 0.2, 0.2, 0.2, 0.2);
        }
        victim.displayClientMessage(Component.literal("§b✦ Feitiço refletido!"), true);
    }
}
