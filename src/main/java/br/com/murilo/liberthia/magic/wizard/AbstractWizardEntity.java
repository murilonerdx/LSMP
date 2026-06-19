package br.com.murilo.liberthia.magic.wizard;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * v0.1.24 r87: Base abstract de wizard caster mob.
 *
 * <p>Subclasses definem:
 * <ul>
 *   <li>{@link #school} — escola do wizard (determina damage source)</li>
 *   <li>{@link #castSpell} — implementação concreta (Iron's Spells AbstractSpellCastingMob pattern)</li>
 *   <li>{@link #getProjectileColor} — cor da spell visual</li>
 * </ul>
 */
public abstract class AbstractWizardEntity extends Monster {

    protected int castCooldown = 0;

    public AbstractWizardEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // r178: não some do mapa (inclusive quando o player morre e o alvo zera).
        this.setPersistenceRequired();
    }

    public abstract SpellSchool school();
    public abstract int getProjectileColor();

    /** r178: o tipo de VFX (sprite animado) que este wizard lança — não partícula vanilla. */
    public br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type vfxType() {
        return br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxRegistry.Type.MAGIC_SPELL;
    }

    /** r178: setPersistenceRequired() + isto = NUNCA despawna por distância. */
    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    /** r178: spawna o VFX de spell (sprite animado) num ponto. Substitui partícula vanilla. */
    protected void spawnSpellVfx(net.minecraft.world.phys.Vec3 pos, float scale) {
        if (!(level() instanceof ServerLevel sl)) return;
        var vfx = new br.com.murilo.liberthia.magic.spell.vfx.SpriteVfxEntity(sl, this, pos, vfxType(), scale);
        sl.addFreshEntity(vfx);
    }

    /** VFX grande no impacto do alvo + rastro do caster até ele. */
    protected void castVfxBeam(LivingEntity target) {
        net.minecraft.world.phys.Vec3 from = position().add(0, 1.3, 0);
        net.minecraft.world.phys.Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        spawnSpellVfx(to, 1.2F);
        for (int i = 1; i <= 3; i++) {
            spawnSpellVfx(from.add(to.subtract(from).scale(i / 4.0)), 0.5F);
        }
    }

    /** Cooldown entre casts em ticks. Default 60 (3s). */
    public int getCastCooldownTicks() { return 60; }

    /** Range máximo de cast. */
    public double getCastRange() { return 16.0; }

    public static AttributeSupplier.Builder createWizardAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 3.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // r87: WizardAttackGoal — mantém distância e casta no tick certo
        this.goalSelector.addGoal(2, new WizardAttackGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (castCooldown > 0) castCooldown--;
        if (level().isClientSide) return;

        // Particles ambient da escola
        if (tickCount % 12 == 0 && level() instanceof ServerLevel sl) {
            int color = getProjectileColor();
            float r = ((color >> 16) & 0xFF) / 255F;
            float g = ((color >> 8) & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;
            sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(r, g, b), 0.8F),
                    getX(), getY() + 1.4, getZ(),
                    2, 0.3, 0.3, 0.3, 0.01);
        }
    }

    /**
     * Casta o spell deste wizard contra o target. Chamado pelo
     * {@link WizardAttackGoal}.
     */
    public abstract void castSpellAt(LivingEntity target);

    /** Aplica damage de school custom. */
    protected void dealSchoolDamage(LivingEntity target, float damage, SchoolDamageSource meta) {
        var ds = meta.toVanilla(level(), this);
        target.hurt(ds, damage);
    }

    public boolean canCast() {
        return castCooldown <= 0;
    }

    public void resetCooldown() {
        castCooldown = getCastCooldownTicks();
    }

    /**
     * r178: ao morrer, o wizard DROPA o pergaminho (feitiço) da sua escola — o mesmo
     * tipo de magia que ele lançava em você. Chance alta (60% + looting), só se morto
     * por player. O scroll é usável (cooldown baixo) pelo jogador.
     */
    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource src, int looting, boolean killedByPlayer) {
        super.dropCustomDeathLoot(src, looting, killedByPlayer);
        net.minecraft.world.item.Item scroll = scrollForSchool(school());
        if (scroll == null || !killedByPlayer) return;
        float chance = 0.60F + 0.12F * Math.max(0, looting);
        if (this.getRandom().nextFloat() < chance) {
            this.spawnAtLocation(new net.minecraft.world.item.ItemStack(scroll));
        }
    }

    /** Mapeia a escola do wizard pro pergaminho dropável correspondente. */
    private static net.minecraft.world.item.Item scrollForSchool(SpellSchool s) {
        try {
            return switch (s) {
                case FIRE -> br.com.murilo.liberthia.registry.ModItems.SCROLL_FIRE.get();
                case ICE -> br.com.murilo.liberthia.registry.ModItems.SCROLL_ICE.get();
                case LIGHTNING -> br.com.murilo.liberthia.registry.ModItems.SCROLL_LIGHTNING.get();
                case BLOOD -> br.com.murilo.liberthia.registry.ModItems.SCROLL_BLOOD.get();
                case ELDRITCH -> br.com.murilo.liberthia.registry.ModItems.SCROLL_ELDRITCH.get();
                case HOLY -> br.com.murilo.liberthia.registry.ModItems.SCROLL_HOLY.get();
                case NATURE -> br.com.murilo.liberthia.registry.ModItems.SCROLL_ELDRITCH.get(); // sem scroll de natureza
            };
        } catch (Throwable t) {
            return null;
        }
    }

    /** Spawn particle line entre wizard e target (visualizar beam). */
    protected void drawBeam(LivingEntity target, ParticleOptions particle) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = position().add(0, 1.4, 0);
        Vec3 to = target.position().add(0, 1.0, 0);
        Vec3 dir = to.subtract(from);
        double dist = dir.length();
        Vec3 norm = dir.normalize();
        int steps = (int) (dist * 2);
        for (int i = 0; i < steps; i++) {
            double f = i / (double) steps;
            sl.sendParticles(particle,
                    from.x + norm.x * dist * f,
                    from.y + norm.y * dist * f,
                    from.z + norm.z * dist * f,
                    1, 0.05, 0.05, 0.05, 0);
        }
    }
}
