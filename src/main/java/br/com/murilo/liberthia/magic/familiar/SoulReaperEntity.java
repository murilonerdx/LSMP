package br.com.murilo.liberthia.magic.familiar;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * v0.1.24 r95: <b>Soul Reaper</b> — familiar Drygmy-like que farma loot
 * de mobs próximos sem matá-los.
 *
 * <p>A cada 200 ticks, escolhe 1 monster (excluindo boss) num raio de 16
 * e samples sua loot table — spawna 1-2 items do loot no chão sem dar dano.
 * Bonus: aplica WEAKNESS 60t no mob amostrado (pacificado brevemente).
 */
public class SoulReaperEntity extends PathfinderMob {

    public SoulReaperEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel sl)) return;

        // Particles (purpúreas)
        if (tickCount % 8 == 0) {
            sl.sendParticles(ParticleTypes.SOUL,
                    getX(), getY() + 0.6, getZ(),
                    2, 0.2, 0.3, 0.2, 0.02);
        }

        // Sample loot a cada 200 ticks (10s)
        if (tickCount % 200 == 0) {
            sampleMobLoot(sl);
        }
    }

    private void sampleMobLoot(ServerLevel sl) {
        var mobs = sl.getEntitiesOfClass(Monster.class,
                getBoundingBox().inflate(16));
        if (mobs.isEmpty()) return;

        Monster target = mobs.get(random.nextInt(mobs.size()));
        // Skip bosses
        if (target.getMaxHealth() > 100) return;

        // Aplica WEAKNESS visível
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.WEAKNESS, 60, 0, true, true));

        // Sample da loot table — fake death context
        try {
            net.minecraft.resources.ResourceLocation lootKey = target.getLootTable();
            LootTable lt = sl.getServer().getLootData().getLootTable(lootKey);
            LootParams params = new LootParams.Builder(sl)
                    .withParameter(LootContextParams.THIS_ENTITY, target)
                    .withParameter(LootContextParams.ORIGIN, target.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, sl.damageSources().magic())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            var loot = lt.getRandomItems(params);
            int toGive = Math.min(2, loot.size());
            for (int i = 0; i < toGive; i++) {
                ItemStack stack = loot.get(i);
                if (stack.isEmpty()) continue;
                ItemEntity ie = new ItemEntity(sl,
                        getX(), getY() + 0.5, getZ(), stack);
                ie.setDeltaMovement(0, 0.2, 0);
                sl.addFreshEntity(ie);
            }
            // Particles de "drenagem"
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    getX(), getY() + 0.5, getZ(),
                    10, 0.3, 0.3, 0.3, 0.05);
        } catch (Throwable ignored) {}
    }
}
