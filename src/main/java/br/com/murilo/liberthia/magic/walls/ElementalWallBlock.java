package br.com.murilo.liberthia.magic.walls;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * v0.1.24 r99: <b>Elemental Wall</b> — bloco solid-visual que aplica efeito
 * de school ao tocar. Sem collision (atravessar custa dano).
 *
 * <p>Variações: Fire/Ice/Lightning/Holy. Cada um aplica seu damage source +
 * efeito de status. Despawn em 200t (10s).
 */
public class ElementalWallBlock extends Block {

    public final SpellSchool school;
    private final ParticleOptions particle;

    public ElementalWallBlock(BlockBehaviour.Properties props, SpellSchool school, ParticleOptions particle) {
        super(props);
        this.school = school;
        this.particle = particle;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return Shapes.empty(); // sem collision — passa através mas leva dano
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (!(entity instanceof LivingEntity le)) return;
        if (!(level instanceof ServerLevel sl)) return;

        // Aplica school damage cada 10t enquanto está dentro
        if (level.getGameTime() % 10 != 0) return;

        SchoolDamageSource sds = switch (school) {
            case FIRE -> SchoolDamageSource.fire(40);
            case ICE -> SchoolDamageSource.ice(60);
            case LIGHTNING -> SchoolDamageSource.of(SpellSchool.LIGHTNING);
            case HOLY -> SchoolDamageSource.of(SpellSchool.HOLY);
            default -> SchoolDamageSource.of(school);
        };
        le.hurt(sds.toVanilla(sl, null), 2.0F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 3; i++) {
            level.addParticle(particle,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0, 0.05, 0);
        }
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Despawn aleatório — duração média 10s (200t / 30% chance / tick = avg 666t)
        if (random.nextFloat() < 0.05F) {
            level.removeBlock(pos, false);
            level.sendParticles(particle,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8, 0.3, 0.3, 0.3, 0.02);
        }
    }
}
