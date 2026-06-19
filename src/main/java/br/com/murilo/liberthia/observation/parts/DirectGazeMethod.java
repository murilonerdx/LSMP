package br.com.murilo.liberthia.observation.parts;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.api.ObservationContext;
import br.com.murilo.liberthia.observation.api.WatchMethod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import java.util.List;

/**
 * v0.1.22 r60: <b>Direct Gaze</b> — observa o que o player está olhando direto.
 *
 * r165 FIX: Usa ProjectileUtil.getEntityHitResult para entidades (caster.pick()
 * NUNCA retorna EntityHitResult — só BlockHitResult). Retorna o hit mais próximo
 * entre entidade e bloco, para que feitiços de dano funcionem contra mobs.
 */
public class DirectGazeMethod extends WatchMethod {

    public static final double RANGE = 32.0;

    public DirectGazeMethod() {
        super(new ResourceLocation(LiberthiaMod.MODID, "watch/direct_gaze"), "Olhar Direto");
    }

    @Override public int sanityCost() { return 1; }

    @Override
    public HitResult observe(ServerPlayer caster, ServerLevel level, ObservationContext ctx) {
        // Tenta entity hit PRIMEIRO (mais próximo wins)
        Vec3 eye  = caster.getEyePosition(1.0f);
        Vec3 look = caster.getViewVector(1.0f);
        Vec3 end  = eye.add(look.scale(RANGE));
        AABB box  = caster.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0);

        EntityHitResult ehr = ProjectileUtil.getEntityHitResult(
            level, caster, eye, end, box,
            e -> !e.isSpectator() && e.isPickable() && e != caster
        );

        HitResult blockHit = caster.pick(RANGE, 0, false);

        // Prefere entity hit se mais próxima que o bloco hit
        if (ehr != null) {
            double entityDist = ehr.getLocation().distanceToSqr(eye);
            double blockDist  = blockHit.getLocation().distanceToSqr(eye);
            if (entityDist <= blockDist) return ehr;
        }
        return blockHit;
    }

    @Override
    public List<Component> lore() {
        return List.of(
            Component.literal("§5§oO mais simples dos atos."),
            Component.literal("§8§oOlhe. Confirme.")
        );
    }
}
