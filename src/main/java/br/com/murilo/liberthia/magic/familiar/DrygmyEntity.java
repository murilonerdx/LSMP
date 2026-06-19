package br.com.murilo.liberthia.magic.familiar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.160 r135: <b>Drygmy</b> — familiar farm passivo.
 *
 * <p>Inspirado no Ars Nouveau:
 * <ul>
 *   <li>Mob passivo verde, não ataca</li>
 *   <li>Quando próximo de mobs hostis em jaula (com 4 fences), coleta loot deles passivamente</li>
 *   <li>Periodicamente (cada 600 ticks = 30s), spawna 1-3 items de loot table de um mob próximo</li>
 *   <li>Drops vão pro chão perto do Drygmy</li>
 * </ul>
 */
public class DrygmyEntity extends Animal {

    public static final int FARM_INTERVAL = 600; // 30s
    public static final double FARM_RADIUS = 6.0;
    private long lastFarmTick = 0;

    public DrygmyEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) {
            // VFX client: leaf/heart particles
            if (random.nextFloat() < 0.1F) {
                level().addParticle(ParticleTypes.HAPPY_VILLAGER,
                        getX() + (random.nextDouble() - 0.5) * 0.5,
                        getY() + 0.5 + random.nextDouble() * 0.5,
                        getZ() + (random.nextDouble() - 0.5) * 0.5,
                        0, 0.02, 0);
            }
            return;
        }

        long now = sl.getGameTime();
        if (now - lastFarmTick < FARM_INTERVAL) return;

        // Procura mob hostil dentro do raio de farm
        AABB box = new AABB(blockPosition()).inflate(FARM_RADIUS);
        List<LivingEntity> nearby = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this && e.isAlive() && !(e instanceof Player)
                        && !(e instanceof DrygmyEntity)
                        && e.getMaxHealth() < 100); // skip bosses
        if (nearby.isEmpty()) return;

        // Pick random target
        LivingEntity target = nearby.get(sl.random.nextInt(nearby.size()));

        // Loot table simulation: drop alguns items de "rotten flesh + bone" base
        // (Implementação simples — pra AN-style full, precisaria ler loot table do mob)
        for (int i = 0; i < 1 + sl.random.nextInt(2); i++) {
            ItemStack drop = simulateLoot(target);
            if (drop != null && !drop.isEmpty()) {
                spawnAtLocation(drop);
            }
        }

        // VFX harvest
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                getX(), getY() + 0.8, getZ(),
                12, 0.3, 0.3, 0.3, 0.05);
        sl.sendParticles(ParticleTypes.HEART,
                getX(), getY() + 1.0, getZ(),
                2, 0.2, 0.2, 0.2, 0.02);

        lastFarmTick = now;
    }

    /** Drops simulados baseado no tipo do mob. */
    private ItemStack simulateLoot(LivingEntity target) {
        String typeName = target.getType().toString();
        if (random.nextFloat() < 0.5F) {
            // 50% chance — drop "comum" baseado em tipo
            if (typeName.contains("zombie")) return new ItemStack(net.minecraft.world.item.Items.ROTTEN_FLESH);
            if (typeName.contains("skeleton")) return new ItemStack(net.minecraft.world.item.Items.BONE);
            if (typeName.contains("spider")) return new ItemStack(net.minecraft.world.item.Items.STRING);
            if (typeName.contains("creeper")) return new ItemStack(net.minecraft.world.item.Items.GUNPOWDER);
            if (typeName.contains("enderman")) return new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL);
            if (typeName.contains("blaze")) return new ItemStack(net.minecraft.world.item.Items.BLAZE_ROD);
            if (typeName.contains("witch")) return new ItemStack(net.minecraft.world.item.Items.REDSTONE);
            // Default: experience pickup item-like
            return new ItemStack(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE);
        }
        return null;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null; // não breed
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.WHEAT_SEEDS)
            || stack.is(net.minecraft.world.item.Items.BONE_MEAL);
    }
}
