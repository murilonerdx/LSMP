package br.com.murilo.liberthia.automation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * v0.1.24 r88: <b>Spell Sensor</b> — emite redstone signal quando um spell
 * projectile passa num raio próximo (3 blocos).
 *
 * <p>Pulse de 4 ticks. Throttle interno: max 1 pulse a cada 10 ticks pra
 * evitar oscilação infinita em circuits.
 *
 * <p>Sensor → trigger turret (link via Dominion Wand) = circuit fechado:
 * spell hits sensor → emite redstone → turret casta de novo.
 */
public class SpellSensorBlock extends Block {

    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public SpellSensorBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(POWERED);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level,
                          BlockPos pos, net.minecraft.core.Direction side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    /** r164: info screen explica como o sensor funciona. */
    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer sp))
            return net.minecraft.world.InteractionResult.PASS;
        java.util.List<String> stats = new java.util.ArrayList<>();
        stats.add("§7Função: §cemite redstone quando feitiço passa perto");
        stats.add("§7Raio de detecção: §a3 blocos");
        stats.add("§7Pulse: §6redstone level 15 §7por §e4 ticks");
        stats.add("§7Throttle: §amax 1 pulse §7a cada §e10 ticks");
        stats.add("§7Status: " + (state.getValue(POWERED) ? "§a§lATIVO" : "§7inativo"));
        java.util.List<String> instr = new java.util.ArrayList<>();
        instr.add("§7• Coloque numa §droute de feitiço§7 (onde projétil passa)");
        instr.add("§7• Liga §6redstone wire§7 em qualquer face");
        instr.add("§7• Quando spell proj passa → §csignal 15§7 por 4t");
        instr.add("§7• Use pra trigger §6Spell Turret§7 (link via Dominion Wand)");
        instr.add("§7• Circuit: §dSpell→Sensor→Redstone→Turret→Spell§7 (loop)");
        br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                new br.com.murilo.liberthia.network.packet.OpenBlockInfoS2CPacket(
                        "👁 Spell Sensor", stats, instr, 0xFF6666));
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    /** Chamado externamente quando um spell proj é detectado próximo. */
    public static void triggerPulse(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SpellSensorBlock)) return;
        if (state.getValue(POWERED)) return; // já powered, throttle
        level.setBlock(pos, state.setValue(POWERED, true), 3);
        level.scheduleTick(pos, state.getBlock(), 4); // unpower em 4 ticks
    }

    @Override
    public void tick(BlockState state, net.minecraft.server.level.ServerLevel level,
                      BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), 3);
        }
    }
}
