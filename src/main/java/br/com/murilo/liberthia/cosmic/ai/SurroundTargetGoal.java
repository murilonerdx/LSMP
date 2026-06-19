package br.com.murilo.liberthia.cosmic.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * r175: <b>Cercar (caça em grupo)</b>. Em vez de todos os monstros virem em fila
 * pelo mesmo ponto, cada um mira um <b>ângulo distinto ao redor do alvo</b> (pelo
 * id da entidade) e fecha o cerco por lados diferentes — dá a sensação de matilha
 * coordenada. Some quando fica em alcance de ataque (deixa o melee assumir).
 */
public class SurroundTargetGoal extends Goal {

    private final PathfinderMob mob;
    private final double speed;
    private final double ringRadius;
    private int repath;

    public SurroundTargetGoal(PathfinderMob mob, double speed, double ringRadius) {
        this.mob = mob;
        this.speed = speed;
        this.ringRadius = ringRadius;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private LivingEntity target() { return mob.getTarget(); }

    @Override
    public boolean canUse() {
        LivingEntity t = target();
        if (t == null || !t.isAlive()) return false;
        double d2 = mob.distanceToSqr(t);
        // só "cerca" na faixa média: longe demais o pursuit normal aproxima;
        // perto demais o ataque melee assume.
        return d2 > 9.0 && d2 < 36.0 * 36.0;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() { repath = 0; }

    @Override
    public void stop() { mob.getNavigation().stop(); }

    @Override
    public void tick() {
        LivingEntity t = target();
        if (t == null) return;
        mob.getLookControl().setLookAt(t, 30F, 30F);
        if (--repath > 0) return;
        repath = 10;

        // ângulo fixo por entidade → cada mob ocupa um setor diferente do anel
        double ang = (mob.getId() % 8) / 8.0 * (Math.PI * 2.0);
        // gira devagar com o tempo pra "rondar"
        ang += (mob.tickCount % 200) / 200.0 * (Math.PI * 0.5);
        double px = t.getX() + Math.cos(ang) * ringRadius;
        double pz = t.getZ() + Math.sin(ang) * ringRadius;
        mob.getNavigation().moveTo(px, t.getY(), pz, speed);
    }

    private double dist(Vec3 a, double x, double y, double z) {
        double dx = a.x - x, dy = a.y - y, dz = a.z - z;
        return dx * dx + dy * dy + dz * dz;
    }
}
