package br.com.murilo.liberthia.energy;

import br.com.murilo.liberthia.block.entity.EnergyCableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BFS através da rede de {@link EnergyCableBlockEntity}.
 *
 * <p>Coleta todos os {@code IEnergyStorage} que aceitam FE encontrados nas
 * faces de qualquer cabo da rede (excluindo a fonte), e distribui um total
 * de {@code amount} FE entre eles, simulando primeiro e depois aplicando.
 *
 * <p>Isso evita que cada cabo precise gerenciar buffer/repasse: a fonte
 * "olha através" da malha de cabos e entrega direto.
 */
public final class EnergyNetwork {

    private EnergyNetwork() {}

    /**
     * Distribui até {@code maxAmount} FE da fonte {@code source} pelos
     * consumidores acessíveis pela rede de cabos a partir de {@code start}.
     *
     * @param level     mundo
     * @param start     posição da fonte (não recebe energia de volta)
     * @param maxAmount FE máximo a distribuir nesta tick
     * @return FE realmente enviado
     */
    public static int pushThroughNetwork(Level level, BlockPos start, int maxAmount) {
        return distribute(level, start, maxAmount, false);
    }

    /** Igual a pushThroughNetwork mas sem aplicar — só retorna quanto caberia. */
    public static int simulatePush(Level level, BlockPos start, int maxAmount) {
        return distribute(level, start, maxAmount, true);
    }

    /**
     * Inverso de pushThroughNetwork — busca FONTES (canExtract=true) na rede
     * e tenta extrair {@code maxAmount} FE delas. Usado por consumidores
     * "pull-based" como o Laser, que precisam puxar ativamente do gerador
     * via cabos.
     *
     * @return FE realmente puxado.
     */
    public static int pullThroughNetwork(Level level, BlockPos start, int maxAmount) {
        return distributePull(level, start, maxAmount, false);
    }

    /** Igual a pullThroughNetwork mas só simula — retorna quanto SERIA puxado. */
    public static int simulatePull(Level level, BlockPos start, int maxAmount) {
        return distributePull(level, start, maxAmount, true);
    }

    private static int distributePull(Level level, BlockPos start, int maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        List<IEnergyStorage> sources = collectSources(level, start);
        if (sources.isEmpty()) return 0;

        int remaining = maxAmount;
        int got = 0;
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            int slice = Math.max(1, remaining / Math.max(1, sources.size()));
            for (IEnergyStorage s : sources) {
                if (remaining <= 0) break;
                int take = Math.min(slice, remaining);
                int extracted = s.extractEnergy(take, simulate);
                remaining -= extracted;
                got += extracted;
            }
        }
        return got;
    }

    private static List<IEnergyStorage> collectSources(Level level, BlockPos start) {
        List<IEnergyStorage> out = new ArrayList<>();
        Set<BlockPos> visitedCables = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        for (Direction d : Direction.values()) {
            BlockPos npos = start.relative(d);
            BlockEntity nbe = level.getBlockEntity(npos);
            if (nbe == null) continue;
            if (nbe instanceof EnergyCableBlockEntity cable) {
                if (cable.getMode(d.getOpposite()) == EnergyCableBlockEntity.FaceMode.DISABLED) continue;
                if (visitedCables.add(npos)) queue.add(npos);
            } else {
                addSource(nbe, d.getOpposite(), out);
            }
        }
        while (!queue.isEmpty()) {
            BlockPos cur = queue.pollFirst();
            BlockEntity curBe = level.getBlockEntity(cur);
            if (!(curBe instanceof EnergyCableBlockEntity curCable)) continue;
            for (Direction d : Direction.values()) {
                if (curCable.getMode(d) == EnergyCableBlockEntity.FaceMode.DISABLED) continue;
                BlockPos npos = cur.relative(d);
                if (npos.equals(start)) continue;
                BlockEntity nbe = level.getBlockEntity(npos);
                if (nbe == null) continue;
                if (nbe instanceof EnergyCableBlockEntity ncable) {
                    if (ncable.getMode(d.getOpposite()) == EnergyCableBlockEntity.FaceMode.DISABLED) continue;
                    if (visitedCables.add(npos)) queue.add(npos);
                } else {
                    addSource(nbe, d.getOpposite(), out);
                }
            }
        }
        return out;
    }

    private static void addSource(BlockEntity nbe, Direction face, List<IEnergyStorage> out) {
        nbe.getCapability(ForgeCapabilities.ENERGY, face).ifPresent(es -> {
            if (es.canExtract()) out.add(es);
        });
    }

    private static int distribute(Level level, BlockPos start, int maxAmount, boolean simulate) {
        if (maxAmount <= 0) return 0;
        List<IEnergyStorage> consumers = collectConsumers(level, start);
        if (consumers.isEmpty()) return 0;

        int remaining = maxAmount;
        int sent = 0;
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            int slice = Math.max(1, remaining / Math.max(1, consumers.size()));
            for (IEnergyStorage c : consumers) {
                if (remaining <= 0) break;
                int give = Math.min(slice, remaining);
                int accepted = c.receiveEnergy(give, simulate);
                remaining -= accepted;
                sent += accepted;
            }
        }
        return sent;
    }

    private static List<IEnergyStorage> collectConsumers(Level level, BlockPos start) {
        List<IEnergyStorage> out = new ArrayList<>();
        Set<BlockPos> visitedCables = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        // Sementes: cabos vizinhos da fonte (que aceitam conexão pela face oposta).
        for (Direction d : Direction.values()) {
            BlockPos npos = start.relative(d);
            BlockEntity nbe = level.getBlockEntity(npos);
            if (nbe == null) continue;
            if (nbe instanceof EnergyCableBlockEntity cable) {
                // Cabo vizinho: só aceita se a face DELE virada pra nós (d.getOpposite()) não estiver DISABLED
                if (cable.getMode(d.getOpposite()) == EnergyCableBlockEntity.FaceMode.DISABLED) continue;
                if (visitedCables.add(npos)) queue.add(npos);
            } else {
                addConsumer(nbe, d.getOpposite(), out);
            }
        }

        // BFS pelos cabos — respeita DISABLED faces.
        while (!queue.isEmpty()) {
            BlockPos cur = queue.pollFirst();
            BlockEntity curBe = level.getBlockEntity(cur);
            if (!(curBe instanceof EnergyCableBlockEntity curCable)) continue;
            for (Direction d : Direction.values()) {
                // Cabo atual: face d desligada → não atravessa
                if (curCable.getMode(d) == EnergyCableBlockEntity.FaceMode.DISABLED) continue;
                BlockPos npos = cur.relative(d);
                if (npos.equals(start)) continue;
                BlockEntity nbe = level.getBlockEntity(npos);
                if (nbe == null) continue;
                if (nbe instanceof EnergyCableBlockEntity ncable) {
                    // Vizinho cabo: face dele virada pra nós (-d) desligada → não atravessa
                    if (ncable.getMode(d.getOpposite()) == EnergyCableBlockEntity.FaceMode.DISABLED) continue;
                    if (visitedCables.add(npos)) queue.add(npos);
                } else {
                    addConsumer(nbe, d.getOpposite(), out);
                }
            }
        }
        return out;
    }

    private static void addConsumer(BlockEntity nbe, Direction face, List<IEnergyStorage> out) {
        nbe.getCapability(ForgeCapabilities.ENERGY, face).ifPresent(es -> {
            if (es.canReceive()) out.add(es);
        });
    }
}
