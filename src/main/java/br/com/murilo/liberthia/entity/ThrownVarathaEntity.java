package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * r180b — <b>Retorno Espiritual</b> da {@link br.com.murilo.liberthia.item.VarathaSpearItem}.
 * Lança arremessada estilo tridente/bumerangue: voa reto, causa dano (10 + 3 perfuração
 * mágica), e <b>volta sozinha pro dono</b> (após acertar, bater numa parede, ou ~1,25s),
 * devolvendo a lança ao inventário. Sem gravidade (voo de arma espiritual).
 */
public class ThrownVarathaEntity extends AbstractArrow {

    private ItemStack spearItem = new ItemStack(ModItems.VARATHA_SPEAR.get());
    private boolean dealtDamage;
    private boolean returning;

    public ThrownVarathaEntity(EntityType<? extends ThrownVarathaEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public ThrownVarathaEntity(Level level, LivingEntity thrower, ItemStack stack) {
        super(ModEntities.THROWN_VARATHA.get(), thrower, level);
        this.spearItem = stack.copy();
        this.setNoGravity(true);
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
    }

    @Override
    public void tick() {
        Entity owner = this.getOwner();
        if (!returning && (dealtDamage || this.tickCount > 25)) returning = true;

        if (returning && owner != null && owner.isAlive() && !owner.isSpectator()) {
            this.setNoPhysics(true);
            Vec3 to = owner.getEyePosition().subtract(this.position());
            double dist = to.length();
            if (dist < 2.5) { giveBackTo(owner); return; }
            Vec3 v = to.scale(1.0 / dist).scale(1.4);
            this.setDeltaMovement(this.getDeltaMovement().scale(0.6).add(v.scale(0.4)));
        } else if (returning) {
            this.setNoPhysics(false);   // dono sumiu → deixa cair / dropar
            this.setNoGravity(false);
        }

        super.tick();

        if (this.level() instanceof ServerLevel sl && this.tickCount % 2 == 0) {
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY(), getZ(), 2, 0.05, 0.05, 0.05, 0.0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult res) {
        Entity target = res.getEntity();
        Entity owner = this.getOwner();
        DamageSource src = this.damageSources().trident(this, owner == null ? this : owner);
        dealtDamage = true;
        if (target.hurt(src, 10.0F) && target instanceof LivingEntity le) {
            le.hurt(this.damageSources().magic(), 3.0F);  // Perfuração Infernal
            if (owner instanceof LivingEntity lo) {
                EnchantmentHelper.doPostHurtEffects(le, lo);
                EnchantmentHelper.doPostDamageEffects(lo, le);
            }
        }
        returning = true;
        this.setDeltaMovement(this.getDeltaMovement().scale(-0.05));
        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    @Override
    protected void onHitBlock(BlockHitResult res) {
        returning = true;
        this.setDeltaMovement(this.getDeltaMovement().scale(-0.05));
        this.playSound(SoundEvents.TRIDENT_HIT_GROUND, 0.8F, 1.2F);
    }

    private void giveBackTo(Entity owner) {
        if (!this.level().isClientSide) {
            if (owner instanceof Player p) {
                if (!p.addItem(spearItem.copy())) p.drop(spearItem.copy(), false);
            } else {
                this.spawnAtLocation(spearItem.copy());
            }
            this.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.4F);
        }
        this.discard();
    }

    @Override
    public void playerTouch(Player p) {
        if (this.level().isClientSide) return;
        if ((this.getOwner() == null || this.ownedBy(p)) && (returning || dealtDamage)) {
            giveBackTo(p);
        }
    }

    @Override protected ItemStack getPickupItem() { return spearItem.copy(); }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Spear", spearItem.save(new CompoundTag()));
        tag.putBoolean("Returning", returning);
        tag.putBoolean("Dealt", dealtDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Spear")) spearItem = ItemStack.of(tag.getCompound("Spear"));
        returning = tag.getBoolean("Returning");
        dealtDamage = tag.getBoolean("Dealt");
    }
}
