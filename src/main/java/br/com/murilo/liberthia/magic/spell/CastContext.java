package br.com.murilo.liberthia.magic.spell;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * v0.1.143 r111: <b>CastContext</b> — passa estado completo do cast pra função
 * lambda que executa o feitiço. Permite manter SpellLibrary stateless.
 *
 * <p>Fields imutáveis após criação:
 * <ul>
 *   <li>{@code caster} — ServerPlayer que castou</li>
 *   <li>{@code level} — mundo server-side</li>
 *   <li>{@code hand} — qual mão segurando o spell</li>
 *   <li>{@code stack} — ItemStack do spell (pra durability/cooldown)</li>
 *   <li>{@code def} — definição do spell (damage, range, etc)</li>
 * </ul>
 *
 * <p>Métodos helper retornam vetores/alvos comumente usados em casts.
 */
public final class CastContext {

    public final ServerPlayer caster;
    public final ServerLevel level;
    public final InteractionHand hand;
    public final ItemStack stack;
    public final SpellDef def;

    public CastContext(ServerPlayer caster, ServerLevel level,
                       InteractionHand hand, ItemStack stack, SpellDef def) {
        this.caster = caster;
        this.level = level;
        this.hand = hand;
        this.stack = stack;
        this.def = def;
    }

    /**
     * Posição da mão do caster (offset visual pra spells saírem do braço).
     *
     * <p>r165: PUSHED FORWARD — antes ficava a 0.4 blocos da câmera, então o VFX
     * aparecia ENORME bloqueando a visão. Agora puxa 1.4 blocos pra frente, 0.3
     * blocos pra baixo, e levemente pro lado (braço do caster). VFX agora
     * aparece à frente do player, não em cima da câmera.
     */
    public Vec3 handOrigin() {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        double side = caster.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 0.35 : -0.35;
        return eye.add(right.scale(side)).add(0, -0.3, 0).add(look.scale(1.4));
    }

    /**
     * r165: Posição "à frente" do caster — usada pro spawn-burst de VFX dos
     * feitiços (mais longe que handOrigin pra não bloquear a visão).
     * <p>2.0 blocos à frente da câmera, na altura dos olhos.
     */
    public Vec3 castOrigin() {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        return eye.add(look.scale(2.0));
    }

    public Vec3 lookVec() {
        return caster.getLookAngle();
    }

    /**
     * r165: Picka entidade na mira do caster até `range` blocos.
     * Combina entity raycast (ProjectileUtil) + block raycast (caster.pick),
     * retorna a entidade se ela estiver mais perto que o bloco.
     * Null se nenhum entidade na linha de visão.
     */
    public LivingEntity pickTarget(double range) {
        Vec3 eye  = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end  = eye.add(look.scale(range));
        net.minecraft.world.phys.AABB box = caster.getBoundingBox()
                .expandTowards(look.scale(range)).inflate(1.0);
        net.minecraft.world.phys.EntityHitResult ehr =
                net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                        level, caster, eye, end, box,
                        e -> !e.isSpectator() && e.isPickable() && e != caster && e instanceof LivingEntity);
        if (ehr == null) return null;
        HitResult blockHit = caster.pick(range, 0, false);
        double entityDist = ehr.getLocation().distanceToSqr(eye);
        double blockDist  = blockHit.getLocation().distanceToSqr(eye);
        if (entityDist > blockDist) return null;       // bloco bloqueia a visão
        if (ehr.getEntity() instanceof LivingEntity le) return le;
        return null;
    }

    /** Pickear bloco/alvo até `range`. Retorna a posição de impacto. */
    public Vec3 pickHit(double range) {
        // r165: tenta entity primeiro, depois bloco
        Vec3 eye  = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end  = eye.add(look.scale(range));
        net.minecraft.world.phys.AABB box = caster.getBoundingBox()
                .expandTowards(look.scale(range)).inflate(1.0);
        net.minecraft.world.phys.EntityHitResult ehr =
                net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                        level, caster, eye, end, box,
                        e -> !e.isSpectator() && e.isPickable() && e != caster);
        HitResult blockHit = caster.pick(range, 0, false);
        if (ehr != null) {
            double entityDist = ehr.getLocation().distanceToSqr(eye);
            double blockDist  = blockHit.getLocation().distanceToSqr(eye);
            if (entityDist <= blockDist) return ehr.getLocation();
        }
        return blockHit.getLocation();
    }
}
