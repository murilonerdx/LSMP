package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.block.entity.EnergyCableBlockEntity;
import br.com.murilo.liberthia.block.entity.ItemPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Liberthia Wrench — controle silencioso. Sem mensagens em chat.
 *
 * <p>Comportamento:
 * <ul>
 *   <li>Right-click numa face de cabo de energia → toggle DISABLED. Particles
 *       verdes saindo da face = conectado; particles roxas = desconectado.
 *       Sound de "click metálico". Arm visual aparece/some.</li>
 *   <li>Sneak + right-click num Item Pipe → cicla PipeType.</li>
 * </ul>
 *
 * <p>Sem check de "vizinho FE" — toggle qualquer face direto. Feedback é
 * 100% visual + sonoro.
 */
public class WrenchItem extends Item {

    public WrenchItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos pos = ctx.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        Direction face = ctx.getClickedFace();

        // Cabo: toggle face mode (DEFAULT ↔ DISABLED). Sem chat, só partículas + som.
        if (be instanceof EnergyCableBlockEntity cable) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            ServerLevel sl = (ServerLevel) level;

            // Verifica se a face clicada TEM um vizinho FE-compatível.
            // Se não, spawna partículas de AVISO (angry villager) e nada acontece.
            // Isso resolve o caso "user clicou face errada e energia continua passando".
            if (!hasFENeighbor(level, pos, face)) {
                double cx = pos.getX() + 0.5 + face.getStepX() * 0.55;
                double cy = pos.getY() + 0.5 + face.getStepY() * 0.55;
                double cz = pos.getZ() + 0.5 + face.getStepZ() * 0.55;
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                        cx, cy, cz, 12, 0.25, 0.25, 0.25, 0.0);
                level.playSound(null, pos,
                        net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(),
                        SoundSource.BLOCKS, 0.6f, 0.6f);
                br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                        "[Wrench] Face {} de cabo @{} sem vizinho FE — toggle ignorado",
                        face.getName(), pos);
                return InteractionResult.CONSUME;
            }

            EnergyCableBlockEntity.FaceMode m = cable.cycleMode(face);
            boolean connected = m == EnergyCableBlockEntity.FaceMode.DEFAULT;
            spawnFaceFeedback(sl, pos, face, connected);
            level.playSound(null, pos,
                    connected ? SoundEvents.ITEM_FRAME_ADD_ITEM
                              : SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                    SoundSource.BLOCKS, 0.9f, connected ? 1.4f : 0.6f);
            ctx.getItemInHand().hurtAndBreak(1, player, p ->
                    p.broadcastBreakEvent(p.getUsedItemHand()));
            return InteractionResult.CONSUME;
        }

        // Item Pipe: shift+wrench cicla pipe TYPE (Universal/Items/Blocks/Fluids)
        if (be instanceof ItemPipeBlockEntity pipe) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) {
                ItemPipeBlockEntity.PipeType pt = pipe.cyclePipeType();
                ServerLevel sl = (ServerLevel) level;
                sl.sendParticles(ParticleTypes.WAX_ON,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        12, 0.4, 0.4, 0.4, 0.05);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.BLOCKS, 0.6f, 1.0f + pt.ordinal() * 0.2f);
                ctx.getItemInHand().hurtAndBreak(1, player, p ->
                        p.broadcastBreakEvent(p.getUsedItemHand()));
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    /** Helper: a face do cabo tem vizinho FE-compatível? */
    private static boolean hasFENeighbor(Level level, BlockPos pos, Direction d) {
        BlockEntity nbe = level.getBlockEntity(pos.relative(d));
        if (nbe == null) return false;
        if (nbe instanceof EnergyCableBlockEntity) return true;
        return nbe.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY,
                d.getOpposite()).isPresent();
    }

    /**
     * Visual GIGANTE — explosão de partículas IMPOSSÍVEL de perder.
     * Verdes (conectado) ou vermelhas (desconectado) saindo bem da face específica.
     */
    private static void spawnFaceFeedback(ServerLevel level, BlockPos pos, Direction face, boolean connected) {
        double cx = pos.getX() + 0.5 + face.getStepX() * 0.55;
        double cy = pos.getY() + 0.5 + face.getStepY() * 0.55;
        double cz = pos.getZ() + 0.5 + face.getStepZ() * 0.55;

        if (connected) {
            // BURST verde GIGANTE — energia fluindo
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    cx, cy, cz, 30, 0.4, 0.4, 0.4, 0.0);
            level.sendParticles(ParticleTypes.END_ROD,
                    cx, cy, cz, 12, 0.3, 0.3, 0.3, 0.05);
            level.sendParticles(ParticleTypes.GLOW,
                    cx, cy, cz, 8, 0.3, 0.3, 0.3, 0.05);
        } else {
            // BURST vermelho/preto — desconectado
            double dx = face.getStepX() * 0.15;
            double dy = face.getStepY() * 0.15;
            double dz = face.getStepZ() * 0.15;
            for (int i = 0; i < 30; i++) {
                level.sendParticles(ParticleTypes.SMOKE,
                        cx + (level.random.nextDouble() - 0.5) * 0.4,
                        cy + (level.random.nextDouble() - 0.5) * 0.4,
                        cz + (level.random.nextDouble() - 0.5) * 0.4,
                        1, dx, dy, dz, 0.02);
            }
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    cx, cy, cz, 10, 0.3, 0.3, 0.3, 0.08);
            level.sendParticles(ParticleTypes.PORTAL,
                    cx, cy, cz, 15, 0.3, 0.3, 0.3, 0.1);
        }

        // Mod log — pra debug se ainda houver problema
        br.com.murilo.liberthia.LiberthiaMod.LOGGER.info(
                "[Liberthia Wrench] Cable {} face {} -> {}",
                pos, face.getName(), connected ? "CONNECTED" : "DISCONNECTED");
    }
}
