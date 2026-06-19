package br.com.murilo.liberthia.cosmic.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * r175: <b>Reagir ao olhar (estilo Anjo Chorão)</b>.
 *
 * <p>Quando um player está <b>olhando direto pro monstro</b> (no cone de visão +
 * linha de visão), ele <b>congela</b> — para a navegação e zera o movimento
 * horizontal, ficando imóvel e perturbador. Assim que o player desvia o olhar,
 * o goal solta e a IA normal (perseguir/cercar) volta a correr, deixando o monstro
 * "pular" pra mais perto sempre que você pisca.
 *
 * <p>Ocupa os flags MOVE+LOOK com prioridade alta pra preemptar os goals de
 * movimento enquanto observado.
 */
public class GazeFreezeGoal extends Goal {

    private final PathfinderMob mob;
    private final double range;

    public GazeFreezeGoal(PathfinderMob mob, double range) {
        this.mob = mob;
        this.range = range;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    private boolean watched() {
        Player p = mob.level().getNearestPlayer(mob, range);
        if (p == null || p.isCreative() || p.isSpectator()) return false;
        if (mob.distanceToSqr(p) > range * range) return false;
        Vec3 toMob = mob.position().add(0, mob.getBbHeight() * 0.6, 0)
                .subtract(p.getEyePosition());
        if (toMob.lengthSqr() < 1.0e-4) return false;
        double dot = toMob.normalize().dot(p.getLookAngle());
        return dot > 0.55 && mob.hasLineOfSight(p);
    }

    @Override public boolean canUse() { return watched(); }
    @Override public boolean canContinueToUse() { return watched(); }
    @Override public boolean isInterruptable() { return true; }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() { mob.getNavigation().stop(); }

    @Override
    public void tick() {
        mob.getNavigation().stop();
        mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
        mob.setXxa(0);
        mob.setZza(0);
        // encara o player de volta — fica te observando, imóvel
        Player p = mob.level().getNearestPlayer(mob, range);
        if (p != null) mob.getLookControl().setLookAt(p, 30F, 30F);
    }
}
