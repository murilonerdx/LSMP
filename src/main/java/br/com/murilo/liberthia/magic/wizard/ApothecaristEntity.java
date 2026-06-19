package br.com.murilo.liberthia.magic.wizard;

import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * v0.1.24 r106: <b>Apothecarist</b> — wizard que arremessa potions hostis
 * (harming/poison/slowness/weakness). Mantém distância média.
 */
public class ApothecaristEntity extends AbstractWizardEntity {

    public ApothecaristEntity(EntityType<? extends net.minecraft.world.entity.monster.Monster> type, Level level) {
        super(type, level);
    }

    @Override public SpellSchool school() { return SpellSchool.NATURE; }
    @Override public int getProjectileColor() { return 0xFF66CC66; }
    @Override public int getCastCooldownTicks() { return 80; }
    @Override public double getCastRange() { return 14.0; }
    @Override public br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type vfxType() {
        return br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.MAGIC_BUBBLES;
    }

    @Override
    public void castSpellAt(LivingEntity target) {
        Level level = level();
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return;
        castVfxBeam(target); // r178: VFX de spell animado

        // Cria thrown potion com efeito random hostile
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        var pot = switch (random.nextInt(4)) {
            case 0 -> Potions.STRONG_HARMING;
            case 1 -> Potions.STRONG_POISON;
            case 2 -> Potions.STRONG_SLOWNESS;
            default -> Potions.WEAKNESS;
        };
        PotionUtils.setPotion(potion, pot);

        ThrownPotion thrown = new ThrownPotion(level, this);
        thrown.setItem(potion);
        // Arc toward target
        double dx = target.getX() - getX();
        double dy = target.getEyeY() - getY();
        double dz = target.getZ() - getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        thrown.shoot(dx, dy + horiz * 0.2, dz, 0.75F, 8.0F);
        level.addFreshEntity(thrown);

        // Particle "throwing" effect
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                getX(), getY() + 1.5, getZ(), 5, 0.2, 0.2, 0.2, 0.05);
    }
}
