package br.com.murilo.liberthia.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * r164 FIX: utilitário compartilhado pra raycast de entidade.
 *
 * <p><b>Problema original:</b> vários items chamavam
 * {@code player.pick(distance, 0, false)} esperando bater em entidades.
 * Mas {@code Entity.pick()} <strong>SÓ retorna BlockHitResult</strong> —
 * ele ignora entidades completamente. Resultado: todo item que dependia
 * disso pra mirar (Lightning Lance, Tendril Sigil, School Scrolls etc.)
 * NUNCA detectava criaturas — sempre mostrava "Mire em uma criatura"
 * mesmo com o crosshair em cima do mob.
 *
 * <p><b>Solução:</b> usar
 * {@link ProjectileUtil#getEntityHitResult(net.minecraft.world.level.Level,
 *  Entity, Vec3, Vec3, AABB, java.util.function.Predicate)}
 * que é o mesmo método que feitiços/flechas usam — faz raycast real
 * contra hitbox das entidades, não só blocos.
 */
public final class EntityRaycast {

    private EntityRaycast() {}

    /**
     * Faz raycast da câmera do {@code shooter} ao longo da view direction,
     * por até {@code distance} blocos, e retorna a primeira LivingEntity
     * atingida (excluindo o próprio shooter).
     *
     * @return LivingEntity atingida, ou null se nenhuma.
     */
    public static LivingEntity pickLiving(LivingEntity shooter, double distance) {
        Vec3 eye = shooter.getEyePosition(1.0F);
        Vec3 look = shooter.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * distance, look.y * distance, look.z * distance);
        AABB searchBox = shooter.getBoundingBox().expandTowards(look.scale(distance)).inflate(1.0);
        EntityHitResult ehr = ProjectileUtil.getEntityHitResult(
                shooter.level(), shooter, eye, end, searchBox,
                e -> !e.isSpectator() && e.isPickable() && e != shooter && e instanceof LivingEntity
        );
        if (ehr != null && ehr.getEntity() instanceof LivingEntity le) return le;
        return null;
    }
}
