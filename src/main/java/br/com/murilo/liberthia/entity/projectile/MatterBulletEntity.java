package br.com.murilo.liberthia.entity.projectile;

import br.com.murilo.liberthia.data.ChunkInfectionData;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * r180b — projétil da {@link br.com.murilo.liberthia.item.MatterPistolItem}. Efeito por
 * MATÉRIA: <b>Escura</b> (dano + poço gravitacional + Escuridão), <b>Amarela</b> (dano +
 * explosão sem grief + fogo), <b>Clara/Branca</b> (cura aliados / fere hostis + purifica
 * a corrupção do chunk), <b>Fundido</b> (mistura = todos os efeitos + dano alto). Renderiza
 * como o ingot da matéria voando (ThrownItemRenderer).
 */
public class MatterBulletEntity extends ThrowableItemProjectile {

    public enum Type { DARK, YELLOW, WHITE, FUSED }

    private static final EntityDataAccessor<Byte> MTYPE =
            SynchedEntityData.defineId(MatterBulletEntity.class, EntityDataSerializers.BYTE);

    public MatterBulletEntity(EntityType<? extends MatterBulletEntity> type, Level level) {
        super(type, level);
    }

    public MatterBulletEntity(Level level, LivingEntity thrower, Type t) {
        super(ModEntities.MATTER_BULLET.get(), thrower, level);
        setMatterType(t);
        setItem(new ItemStack(itemFor(t)));
    }

    @Override protected void defineSynchedData() { super.defineSynchedData(); this.entityData.define(MTYPE, (byte) 0); }
    public void setMatterType(Type t) { this.entityData.set(MTYPE, (byte) t.ordinal()); }
    public Type getMatterType() { int i = this.entityData.get(MTYPE); return Type.values()[(i >= 0 && i < 4) ? i : 0]; }

    @Override protected Item getDefaultItem() { return ModItems.DARK_MATTER_INGOT.get(); }

    private static Item itemFor(Type t) {
        return switch (t) {
            case YELLOW -> ModItems.YELLOW_MATTER_INGOT.get();
            case WHITE  -> ModItems.CLEAR_MATTER_INGOT.get();
            case FUSED  -> ModItems.DARK_MATTER_CATALYST.get();
            default     -> ModItems.DARK_MATTER_INGOT.get();
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel sl && this.tickCount % 2 == 0) {
            sl.sendParticles(trail(getMatterType()), getX(), getY(), getZ(), 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide) return;
        ServerLevel sl = (ServerLevel) this.level();
        Vec3 p = result.getLocation();
        Entity owner = getOwner();
        Type t = getMatterType();
        boolean dark = t == Type.DARK || t == Type.FUSED;
        boolean yellow = t == Type.YELLOW || t == Type.FUSED;
        boolean white = t == Type.WHITE || t == Type.FUSED;
        float base = switch (t) { case FUSED -> 10F; case DARK -> 7F; case YELLOW -> 5F; default -> 6F; };

        DamageSource src = this.damageSources().thrown(this, owner);
        LivingEntity direct = (result.getType() == HitResult.Type.ENTITY
                && ((EntityHitResult) result).getEntity() instanceof LivingEntity le) ? le : null;
        if (direct != null) {
            boolean friendly = direct instanceof Player || direct.getType().getCategory() == MobCategory.CREATURE;
            if (white && friendly) direct.heal(4F);
            else direct.hurt(src, base);
            if (dark) direct.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
            if (yellow) direct.setSecondsOnFire(3);
            if (white && !friendly) direct.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
        }

        // ── efeitos de área ──
        if (yellow) sl.explode(owner, p.x, p.y, p.z, 1.6F, Level.ExplosionInteraction.NONE);
        if (dark) { // poço gravitacional: puxa entidades pro impacto
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(4.0))) {
                if (e == owner) continue;
                Vec3 pull = new Vec3(p.x - e.getX(), 0, p.z - e.getZ());
                if (pull.lengthSqr() > 0.01) { pull = pull.normalize().scale(0.7); e.push(pull.x, 0.1, pull.z); e.hasImpulse = true; }
            }
        }
        if (white) { // purifica corrupção + cura aliados na área
            try {
                ChunkInfectionData cd = ChunkInfectionData.get(sl);
                ChunkPos cp = new ChunkPos(BlockPos.containing(p));
                int v = cd.getContamination(cp);
                if (v > 0) cd.setContamination(cp, Math.max(0, v - 2));
            } catch (Throwable ignored) {}
            for (Player pl : sl.getEntitiesOfClass(Player.class, new AABB(p, p).inflate(4.0))) pl.heal(2F);
        }

        sl.sendParticles(trail(t), p.x, p.y, p.z, t == Type.FUSED ? 30 : 14, 0.3, 0.3, 0.3, 0.05);
        this.discard();
    }

    private static ParticleOptions trail(Type t) {
        return switch (t) {
            case YELLOW -> ParticleTypes.FLAME;
            case WHITE  -> ParticleTypes.END_ROD;
            case FUSED  -> ParticleTypes.GLOW;
            default     -> ParticleTypes.REVERSE_PORTAL;
        };
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("MType", (byte) getMatterType().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int i = tag.getByte("MType");
        setMatterType(Type.values()[(i >= 0 && i < 4) ? i : 0]);
    }
}
