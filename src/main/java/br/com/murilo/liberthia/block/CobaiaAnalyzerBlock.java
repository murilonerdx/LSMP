package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.entity.CobaiaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * r180: <b>Analisador de Sujeito</b> — máquina que "liga" numa {@link CobaiaEntity}
 * próxima (≤5b) e lê o estado da infecção de matéria no corpo dela: tipo, nível,
 * efeitos e o prognóstico ao chegar a 100%. Clique direito = scan (feixe + leitura).
 */
public class CobaiaAnalyzerBlock extends Block {

    private static final double RANGE = 5.0;

    public CobaiaAnalyzerBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Vec3 center = Vec3.atCenterOf(pos);
        CobaiaEntity sub = null;
        double best = Double.MAX_VALUE;
        for (CobaiaEntity c : level.getEntitiesOfClass(CobaiaEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(RANGE))) {
            double d = c.position().distanceToSqr(center);
            if (d < best) { best = d; sub = c; }
        }

        if (sub == null) {
            player.displayClientMessage(Component.literal(
                    "§8[Analisador] Nenhum sujeito conectado — aproxime uma Cobaia (≤5 blocos)."), true);
            level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.8F, 0.8F);
            return InteractionResult.CONSUME;
        }

        int lvl = sub.getMatterLevel();
        String type = sub.getMatterType();
        // Feixe de scan do bloco até o sujeito
        if (level instanceof ServerLevel sl) {
            Vec3 from = center.add(0, 0.6, 0);
            Vec3 to = sub.position().add(0, sub.getBbHeight() * 0.5, 0);
            Vec3 diff = to.subtract(from);
            int steps = (int) Math.max(6, diff.length() * 4);
            Vec3 step = diff.scale(1.0 / steps);
            Vec3 p = from;
            for (int i = 0; i <= steps; i++) {
                sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0.0);
                p = p.add(step);
            }
            sl.sendParticles(ParticleTypes.SNEEZE, to.x, to.y, to.z, 10, 0.3, 0.4, 0.3, 0.02);
            sl.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7F, 1.6F);
        }

        // Leitura
        player.displayClientMessage(Component.literal("§5§l═══ Análise de Sujeito ═══"), false);
        player.displayClientMessage(Component.literal("§7Matéria dominante: " + colored(type)), false);
        player.displayClientMessage(Component.literal("§7Infecção no corpo: " + bar(lvl) + " §f" + lvl + "%"), false);
        player.displayClientMessage(Component.literal("§7" + effectOf(type)), false);
        if (lvl >= 100) {
            player.displayClientMessage(Component.literal("§4§l⚠ SATURAÇÃO TOTAL — corpo tomado pela matéria."), false);
        } else {
            player.displayClientMessage(Component.literal("§8Prognóstico aos 100%: o corpo muta (Zumbi Corrompido)."), false);
        }
        return InteractionResult.CONSUME;
    }

    private static String bar(int pct) {
        int filled = Math.round(pct / 10F);
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < 10; i++) {
            if (i == filled) sb.append("§8");
            sb.append('█');
        }
        return sb.toString();
    }

    private static String colored(String type) {
        return switch (type) {
            case "dark" -> "§5Escura";
            case "clear" -> "§bClara";
            case "yellow" -> "§eAmarela";
            case "mixed" -> "§cMista";
            default -> "§8nenhuma";
        };
    }

    private static String effectOf(String type) {
        return switch (type) {
            case "dark" -> "Efeito: anti-criação — corrói o entorno, agressiva.";
            case "clear" -> "Efeito: energia pura, reações imediatas.";
            case "yellow" -> "Efeito: instável, mutação rápida e errática.";
            case "mixed" -> "Efeito: mistura volátil — evolução imprevisível.";
            default -> "Sujeito não exposto à matéria.";
        };
    }

}
