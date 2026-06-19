package br.com.murilo.liberthia.magic.wizard;

import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.24 r106: <b>Keeper</b> — wizard healing+buff que cura outros monsters
 * próximos. Em vez de atacar player diretamente, faz mobs aliados ficarem
 * muito mais perigosos.
 *
 * <p>A cada cast, num raio de 12: cura 4HP em monsters + aplica STRENGTH +
 * REGENERATION.
 */
public class KeeperEntity extends AbstractWizardEntity {

    public KeeperEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override public SpellSchool school() { return SpellSchool.HOLY; }
    @Override public int getProjectileColor() { return 0xFFFFEEAA; }
    @Override public int getCastCooldownTicks() { return 100; }
    @Override public br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type vfxType() {
        return br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.PROTECTION;
    }

    @Override
    public void castSpellAt(LivingEntity target) {
        // Ignora target individual — efeito de área em allies
        if (!(level() instanceof ServerLevel sl)) return;
        spawnSpellVfx(position().add(0, 0.1, 0), 1.6F); // r178: círculo de proteção VFX

        var allies = sl.getEntitiesOfClass(Monster.class,
                getBoundingBox().inflate(12));
        for (Monster m : allies) {
            if (m == this) continue;
            m.heal(4F);
            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, true, false));
            m.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, true, false));
            // Visual beam
            drawBeam(m, ParticleTypes.HEART);
        }

        // Self-heal também
        heal(2F);
        sl.sendParticles(ParticleTypes.END_ROD,
                getX(), getY() + 2, getZ(), 20, 0.5, 0.8, 0.5, 0.05);
    }
}
