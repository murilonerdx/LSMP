package br.com.murilo.liberthia.magic.wizard;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * v0.1.24 r87: Goal de AI para wizard caster.
 *
 * <p>Pattern do Iron's Spells {@code WizardAttackGoal}:
 * <ol>
 *   <li>Encontra target</li>
 *   <li>Mantém distância ideal (não muito perto, não muito longe)</li>
 *   <li>A cada cooldown, casta spell se target visível</li>
 *   <li>Se target muito perto, recua</li>
 * </ol>
 */
public class WizardAttackGoal extends Goal {

    private final AbstractWizardEntity wizard;
    private final double idealDistance = 10.0;
    private final double tooCloseDistance = 5.0;

    public WizardAttackGoal(AbstractWizardEntity wizard) {
        this.wizard = wizard;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = wizard.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        LivingEntity target = wizard.getTarget();
        if (target == null) return;

        double dist = wizard.distanceTo(target);

        // Mantém line of sight pra target
        wizard.getLookControl().setLookAt(target, 30, 30);

        // Posicionamento
        if (dist > idealDistance + 2) {
            // Muito longe — aproxima
            wizard.getNavigation().moveTo(target, 1.0);
        } else if (dist < tooCloseDistance) {
            // Muito perto — recua
            var look = wizard.position().subtract(target.position()).normalize().scale(3);
            wizard.getNavigation().moveTo(
                    wizard.getX() + look.x, wizard.getY(), wizard.getZ() + look.z, 1.2);
        } else {
            // Na distância ideal — para e prepara cast
            wizard.getNavigation().stop();
        }

        // Cast quando pronto
        if (wizard.canCast() && dist <= wizard.getCastRange()
                && wizard.hasLineOfSight(target)) {
            wizard.castSpellAt(target);
            wizard.resetCooldown();
        }
    }
}
