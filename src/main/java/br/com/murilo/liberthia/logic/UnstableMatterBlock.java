package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

/**
 * Matéria Mista Instável — criada quando Dark Matter encontra Clear Matter.
 * AGE 0-7: escala progressiva até explosão.
 * Scheduled ticks a cada 5 ticks, explode em AGE 7.
 */
public class UnstableMatterBlock extends Block {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);

    public UnstableMatterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        level.scheduleTick(pos, this, 5);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age >= 7) {
            detonate(level, pos);
            return;
        }
        level.setBlock(pos, state.setValue(AGE, age + 1), 3);
        level.scheduleTick(pos, this, 5);
    }

    private void detonate(ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                5.0F, Level.ExplosionInteraction.BLOCK);

        // Infect nearby entities
        AABB area = new AABB(pos).inflate(4);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            entity.getCapability(ModCapabilities.INFECTION).ifPresent(data -> data.addInfection(5));
        }

        // Drop yellow matter
        level.addFreshEntity(new ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(ModItems.YELLOW_MATTER_INGOT.get(), 1)));

        // Lore: Dark+Clear fusion creates conscious beings with their own motivations
        // 30% chance to spawn a Dark Consciousness entity from the reaction
        if (level.random.nextFloat() < 0.30f) {
            br.com.murilo.liberthia.entity.DarkConsciousnessEntity consciousness =
                    ModEntities.DARK_CONSCIOUSNESS.get().create(level);
            if (consciousness != null) {
                consciousness.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        level.random.nextFloat() * 360F, 0.0F);
                level.addFreshEntity(consciousness);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (br.com.murilo.liberthia.config.DevMode.ACTIVE) return;
        int age = state.getValue(AGE);
        // Particles scale with age — increasingly violent
        int count = 1 + age;
        for (int i = 0; i < count; i++) {
            level.addParticle(
                    age < 4 ? ParticleTypes.SMOKE : ParticleTypes.FLAME,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5) * 0.1,
                    0.05 + age * 0.02,
                    (random.nextDouble() - 0.5) * 0.1
            );
        }
        if (age >= 3) {
            level.addParticle(ParticleTypes.LAVA,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    0, 0, 0);
        }
    }
}
