package br.com.murilo.liberthia.entity;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

public class CleansingGrenadeEntity extends ThrowableItemProjectile {
    
    public CleansingGrenadeEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }
    
    public CleansingGrenadeEntity(Level level, LivingEntity shooter) {
        super(ModEntities.CLEANSING_GRENADE.get(), shooter, level);
    }

    public CleansingGrenadeEntity(Level level, double x, double y, double z) {
        super(ModEntities.CLEANSING_GRENADE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CLEANSING_GRENADE.get();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        
        if (!level().isClientSide) {
            BlockPos impactPos = BlockPos.containing(result.getLocation());
            int radius = 2; // 5x5 area
            
            level().playSound(null, impactPos, ModSounds.CLEAR_HUM.get(), SoundSource.NEUTRAL, 1.0F, 1.5F);
            
            for (BlockPos p : BlockPos.betweenClosed(impactPos.offset(-radius, -radius, -radius), impactPos.offset(radius, radius, radius))) {
                if (p.distManhattan(impactPos) <= radius) {
                    net.minecraft.world.level.block.state.BlockState state = level().getBlockState(p);
                    
                    if (state.is(ModBlocks.DARK_MATTER_BLOCK.get()) || state.is(ModBlocks.INFECTION_GROWTH.get())) {
                        level().setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                        ((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.01);
                    } else if (state.is(ModBlocks.CORRUPTED_SOIL.get())) {
                        level().setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
                        ((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.01);
                    } else if (state.getFluidState().is(br.com.murilo.liberthia.registry.ModFluids.DARK_MATTER.get())) {
                        level().setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
            }

            // Also heal entities in blast radius
            for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, new AABB(impactPos).inflate(radius + 1))) {
                entity.getCapability(br.com.murilo.liberthia.registry.ModCapabilities.INFECTION).ifPresent(data -> {
                    data.reduceInfection(50);
                    data.setMutations("");
                    data.setDirty(true);
                    br.com.murilo.liberthia.logic.InfectionLogic.applyDerivedEffects((net.minecraft.server.level.ServerPlayer) (entity instanceof net.minecraft.server.level.ServerPlayer ? entity : null), data);
                });
                entity.removeAllEffects(); // Clears all debuffs
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(br.com.murilo.liberthia.registry.ModEffects.CLEAR_SHIELD.get(), 1200)); // 1 min shield
            }
            
            this.discard();
        } else {
            for(int i = 0; i < 8; ++i) {
                level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
