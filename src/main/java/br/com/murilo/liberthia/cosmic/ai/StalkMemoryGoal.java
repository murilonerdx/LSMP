package br.com.murilo.liberthia.cosmic.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * r175: <b>Espreitar / lembrar posição / emboscar</b>.
 *
 * <p>Enquanto VÊ o alvo, memoriza a última posição conhecida. Quando o alvo
 * some da linha de visão (vira esquina, sobe escada, apaga a luz), o monstro
 * <b>vai até a última posição vista</b> em vez de "esquecer" — caçando, não
 * desistindo. Chegando lá, fareja um pouco ao redor antes de soltar.
 */
public class StalkMemoryGoal extends Goal {

    private final PathfinderMob mob;
    private final double speed;
    private Vec3 lastSeen;
    private int searchTicks;

    public StalkMemoryGoal(PathfinderMob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        if (t != null && t.isAlive()) {
            if (mob.getSensing().hasLineOfSight(t)) {
                lastSeen = t.position();          // atualiza memória enquanto vê
                return false;                      // vendo → deixa o pursuit normal agir
            }
            // tem alvo mas perdeu de vista → caça a última posição
            return lastSeen != null;
        }
        // sem alvo: se acabou de perder um, ainda investiga a última posição
        return lastSeen != null && searchTicks > 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (lastSeen == null) return false;
        LivingEntity t = mob.getTarget();
        if (t != null && mob.getSensing().hasLineOfSight(t)) return false; // reencontrou
        return searchTicks > 0;
    }

    @Override
    public void start() {
        searchTicks = 140; // ~7s caçando a última posição
        if (lastSeen != null) mob.getNavigation().moveTo(lastSeen.x, lastSeen.y, lastSeen.z, speed);
    }

    @Override
    public void stop() {
        if (mob.getTarget() == null) lastSeen = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        searchTicks--;
        if (lastSeen == null) return;
        if (mob.getNavigation().isDone()) {
            // chegou na última posição: fareja em torno (pequeno passo aleatório)
            double a = mob.getRandom().nextDouble() * Math.PI * 2;
            double r = 2 + mob.getRandom().nextDouble() * 3;
            mob.getNavigation().moveTo(lastSeen.x + Math.cos(a) * r, lastSeen.y,
                    lastSeen.z + Math.sin(a) * r, speed);
        }
        mob.getLookControl().setLookAt(lastSeen.x, lastSeen.y + 1, lastSeen.z);
    }
}
