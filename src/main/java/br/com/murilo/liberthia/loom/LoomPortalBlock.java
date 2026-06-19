package br.com.murilo.liberthia.loom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumSet;

/**
 * v0.1.22 r33: bloco portal LOOM — atravessa pra dimensão Loom (purple matrix).
 *
 * <p>Visual: roxo escuro animado tipo nether portal. Tem state AXIS (X/Z).
 */
public class LoomPortalBlock extends Block {

    /** Eixo horizontal (X ou Z) — define orientação da chama do portal. */
    public static final EnumProperty<Direction.Axis> AXIS_PROP = BlockStateProperties.HORIZONTAL_AXIS;

    private static final VoxelShape X_SHAPE = Shapes.box(0, 0, 0.375, 1, 1, 0.625);
    private static final VoxelShape Z_SHAPE = Shapes.box(0.375, 0, 0, 0.625, 1, 1);

    public LoomPortalBlock(BlockBehaviour.Properties p) {
        super(p);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS_PROP, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AXIS_PROP);
    }

    @Override
    public VoxelShape getShape(BlockState s, net.minecraft.world.level.BlockGetter g, BlockPos p, CollisionContext c) {
        return s.getValue(AXIS_PROP) == Direction.Axis.X ? Z_SHAPE : X_SHAPE;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState s) { return PushReaction.BLOCK; }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.isPassenger() || entity.isVehicle()) return;
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer sp)) return;
        // Cooldown anti-spam
        if (sp.getPortalCooldown() > 0) return;
        sp.setPortalCooldown();

        ServerLevel target;
        double x, y, z;
        if (sp.level().dimension().equals(LoomDimension.LOOM_WORLD)) {
            // Volta pro overworld
            target = sp.server.overworld();
            // r33: marca como visitado pra paranoia/madness on return
            sp.getPersistentData().putBoolean(LoomDimension.NBT_VISITED, true);
            sp.getPersistentData().putLong(LoomDimension.NBT_LAST_RETURN_TICK, target.getGameTime());
            // Posição: spawn original
            net.minecraft.core.BlockPos sp_ = target.getSharedSpawnPos();
            x = sp_.getX() + 0.5; y = sp_.getY(); z = sp_.getZ() + 0.5;
            // Effect: Obsession + Madness começam
            sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    br.com.murilo.liberthia.registry.ModEffects.OBSESSION.get(), 6000, 0));
            sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    br.com.murilo.liberthia.registry.ModEffects.MADNESS.get(), 6000, 0));
        } else {
            // Vai pra LOOM
            target = sp.server.getLevel(LoomDimension.LOOM_WORLD);
            if (target == null) return;
            // Posição: high-altitude floating islands
            x = sp.getX(); y = 80; z = sp.getZ();
        }
        sp.teleportTo(target, x, y, z,
                EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                sp.getYRot(), sp.getXRot());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        for (int i = 0; i < 4; i++) {
            double x = pos.getX() + rand.nextDouble();
            double y = pos.getY() + rand.nextDouble();
            double z = pos.getZ() + rand.nextDouble();
            double vx = (rand.nextFloat() - 0.5) * 0.3;
            double vy = (rand.nextFloat() - 0.5) * 0.3;
            double vz = (rand.nextFloat() - 0.5) * 0.3;
            level.addParticle(ParticleTypes.PORTAL, x, y, z, vx, vy, vz);
        }
    }

    /**
     * r156: Quando um bloco do FRAME é quebrado, o portal central colapsa.
     *
     * <p>Para cada vizinho que muda: se ele está num eixo perpendicular ao "thickness"
     * (i.e. lado do frame, não face do portal), e o vizinho não é {@code DARK_MATTER_BLOCK}
     * nem outro portal block, este portal block vira AIR. Isso cascateia entre os 6
     * portal blocks via {@code updateShape}.
     */
    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                   net.minecraft.world.level.LevelAccessor level,
                                   BlockPos pos, BlockPos neighborPos) {
        Direction.Axis portalAxis = state.getValue(AXIS_PROP);
        Direction.Axis thicknessAxis = (portalAxis == Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
        // Vizinho na direção da "espessura" do portal = faces (frente/trás). Ignora.
        if (dir.getAxis() == thicknessAxis) return state;
        // Vizinho deve ser frame (DARK_MATTER_BLOCK) OU outro portal block. Senão colapsa.
        if (!neighborState.is(br.com.murilo.liberthia.registry.ModBlocks.LOOM_STONE.get())
                && !neighborState.is(this)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return state;
    }
}
