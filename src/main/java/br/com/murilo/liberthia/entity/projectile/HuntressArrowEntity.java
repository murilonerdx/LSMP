package br.com.murilo.liberthia.entity.projectile;

import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Flecha do <b>Arco da Caçadora</b>. Comportamento de flecha vanilla, mais:
 * <ul>
 *   <li><b>Tiro Marcado</b> — todo acerto aplica o efeito {@code MARKED} (5s).</li>
 *   <li><b>Tiro Fantasma</b> — quando {@code ghost}, fica invisível e o acerto
 *       usa dano mágico indireto (IGNORA armadura).</li>
 * </ul>
 */
public class HuntressArrowEntity extends Arrow {

    private boolean ghost = false;

    public HuntressArrowEntity(EntityType<? extends Arrow> type, Level level) {
        super(type, level);
    }

    /**
     * NÃO usa {@code super(level, owner)} de propósito: aquele construtor do
     * vanilla {@code Arrow} fixa o tipo {@code EntityType.ARROW}, então a flecha
     * reportaria o tipo errado (cliente renderizaria arrow vanilla, tipo
     * registrado HUNTRESS_ARROW ficaria morto). Aqui setamos o tipo correto +
     * posição/owner como o {@code AbstractArrow(type, shooter, level)} faz.
     */
    public HuntressArrowEntity(Level level, LivingEntity owner) {
        super(br.com.murilo.liberthia.registry.ModEntities.HUNTRESS_ARROW.get(), level);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.setOwner(owner);
        if (owner instanceof net.minecraft.world.entity.player.Player) {
            this.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED;
        }
    }

    public void setGhost(boolean g) {
        this.ghost = g;
        this.setInvisible(g);
    }

    public boolean isGhost() {
        return ghost;
    }

    /** Tiro Marcado — marca o alvo por 5s ao acertar. */
    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        target.addEffect(new MobEffectInstance(ModEffects.MARKED.get(), 100, 0, false, true, true));
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1.2, target.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
        }
    }

    /** Tiro Fantasma — flecha invisível que ignora armadura (dano mágico indireto). */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!ghost) {
            super.onHitEntity(result);
            return;
        }
        Entity target = result.getEntity();
        float v = (float) getDeltaMovement().length();
        float dmg = (float) Math.max(1.0, Math.ceil(v * getBaseDamage()));
        Entity owner = getOwner();
        DamageSource src = (owner != null)
                ? damageSources().indirectMagic(this, owner)
                : damageSources().magic();
        boolean hurt = target.hurt(src, dmg);
        if (hurt && target instanceof LivingEntity le) {
            doPostHurtEffects(le);
        }
        playSound(SoundEvents.ARROW_HIT, 1.0F, 1.2F / (random.nextFloat() * 0.2F + 0.9F));
        discard();
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HuntressGhost", ghost);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setGhost(tag.getBoolean("HuntressGhost"));
    }
}
