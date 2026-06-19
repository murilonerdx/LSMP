package br.com.murilo.liberthia.atmospheric;

import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * v0.1.24 r89: <b>Magic Fire</b> — chamas coloridas, uma por SpellSchool.
 *
 * <p>Não espalham, não causam dano por padrão (configurable). Particles
 * tingidas pela escola (FIRE=laranja, ICE=azul, LIGHTNING=amarelo etc).
 */
public class MagicFireBlock extends Block {

    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 0.5, 1);

    public final SpellSchool school;

    public MagicFireBlock(BlockBehaviour.Properties props, SpellSchool school) {
        super(props);
        this.school = school;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        // r156: precisa retornar shape "visível" pra raycast detectar e player conseguir quebrar.
        // Collision ainda é vazia porque ModBlocks.magicFireProps() usa .noCollission().
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos,
                                   net.minecraft.world.level.pathfinder.PathComputationType type) {
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int color = school.colorHex();
        float r = ((color >> 16) & 0xFF) / 255F;
        float g = ((color >> 8) & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;

        for (int i = 0; i < 4; i++) {
            level.addParticle(new DustParticleOptions(new Vector3f(r, g, b), 1.0F),
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble() * 0.5,
                    pos.getZ() + random.nextDouble(),
                    0, 0.03, 0);
        }

        // Chama specifica por escola
        if (school == SpellSchool.FIRE) {
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 0, 0.02, 0);
        } else if (school == SpellSchool.ICE) {
            level.addParticle(ParticleTypes.SNOWFLAKE,
                    pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 0, 0.02, 0);
        } else if (school == SpellSchool.HOLY) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 0, 0.02, 0);
        } else if (school == SpellSchool.ELDRITCH) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 0, 0.02, 0);
        }
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Não causa dano — só visual
    }
}
