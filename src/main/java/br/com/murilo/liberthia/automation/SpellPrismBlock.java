package br.com.murilo.liberthia.automation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v0.1.24 r88: <b>Spell Prism</b> — bloco que redireciona projéteis spell 90°
 * ao bater.
 *
 * <p>Subscribe em {@link ProjectileImpactEvent} — se o hit result for um bloco
 * que é Spell Prism, intercepta + rotaciona velocidade do projétil em 90°
 * baseado no DirectionProperty FACING do prism.
 *
 * <p>Configurável: orient com player look ao colocar.
 */
@Mod.EventBusSubscriber(modid = br.com.murilo.liberthia.LiberthiaMod.MODID)
public class SpellPrismBlock extends Block {

    public static final DirectionProperty FACING = DirectionProperty.create("facing",
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN);

    public SpellPrismBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    /** r164: right-click abre info screen explicando como o prism funciona. */
    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp))
            return net.minecraft.world.InteractionResult.PASS;
        Direction facing = state.getValue(FACING);
        java.util.List<String> stats = new java.util.ArrayList<>();
        stats.add("§7Função: §dredireciona projéteis de feitiço");
        stats.add("§7Direção atual: §e" + facing.getName().toUpperCase());
        stats.add("§7Velocidade preservada: §a100%");
        stats.add("§7Visual: §dpartículas END_ROD no impacto");
        java.util.List<String> instr = new java.util.ArrayList<>();
        instr.add("§7• §dProjétil mágico§7 bate no prism → §6muda direção 90°§7");
        instr.add("§7• Nova direção = §eorientação do prism§7 (FACING)");
        instr.add("§7• Olhe pra direção desejada §lao colocar§r§7 — define FACING");
        instr.add("§7• Use pra fazer §dpuzzle de mira§7 (espelhar bola de fogo, etc)");
        instr.add("§7• Empilhe múltiplos pra criar §dcircuit§7 de feitiço");
        br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                new br.com.murilo.liberthia.network.packet.OpenBlockInfoS2CPacket(
                        "✦ Spell Prism", stats, instr, 0xAA66FF));
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile p = event.getProjectile();
        Level level = p.level();
        if (level.isClientSide) return;
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.BlockHitResult bhr)) return;

        BlockPos hitPos = bhr.getBlockPos();
        BlockState state = level.getBlockState(hitPos);
        if (!(state.getBlock() instanceof SpellPrismBlock)) return;

        Direction facing = state.getValue(FACING);
        Vec3 vel = p.getDeltaMovement();
        double speed = vel.length();
        if (speed < 0.01) return;

        // Reflexão: nova direção = FACING (ortogonal ao plano do prism)
        Vec3 newDir = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ()).scale(speed);
        p.setDeltaMovement(newDir);
        p.setPos(hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5);

        // Particles + cancelar hit
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5,
                    10, 0.2, 0.2, 0.2, 0.05);
        }
        event.setCanceled(true);
    }
}
