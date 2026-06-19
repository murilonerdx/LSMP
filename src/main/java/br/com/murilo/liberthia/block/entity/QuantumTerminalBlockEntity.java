package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.voice.AntennaNetwork;
import br.com.murilo.liberthia.voice.TerminalChannelStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * v0.1.22 r39: Quantum Terminal — bloco "computador" que recebe mensagens
 * de antenas tunadas + permite enviar mensagens.
 *
 * <h2>r39 CHANGES</h2>
 * <ul>
 *   <li>Mensagens NÃO vão mais pro chat ambiente — ficam no display do
 *       terminal e SALVAS em {@link TerminalChannelStorage} per-canal</li>
 *   <li>Trocar frequência LIMPA o display local e CARREGA mensagens do novo
 *       canal salvas no storage (se houver)</li>
 *   <li>Log local é só CACHE — fonte de verdade é o storage server-side</li>
 * </ul>
 */
public class QuantumTerminalBlockEntity extends BlockEntity implements MenuProvider, AntennaNetwork.BroadcastListener {

    public static final int FE_BUFFER = 10000;
    public static final int FE_PER_SEC = 1;
    public static final int MAX_LOG_ENTRIES = 100;

    /** Entrada de log (mensagem recebida). */
    public static class LogEntry {
        public final long gameTime;
        public final String sender;
        public final String frequency;
        public final String message;

        public LogEntry(long gameTime, String sender, String frequency, String message) {
            this.gameTime = gameTime;
            this.sender = sender;
            this.frequency = frequency;
            this.message = message;
        }

        public CompoundTag toNbt() {
            CompoundTag t = new CompoundTag();
            t.putLong("Time", gameTime);
            t.putString("Sender", sender);
            t.putString("Freq", frequency);
            t.putString("Msg", message);
            return t;
        }

        public static LogEntry fromNbt(CompoundTag t) {
            return new LogEntry(t.getLong("Time"), t.getString("Sender"),
                    t.getString("Freq"), t.getString("Msg"));
        }
    }

    private final TrackedEnergyStorage energy =
            new TrackedEnergyStorage(this, FE_BUFFER, 100, 0);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    private String frequency = "";
    private boolean active = false;
    /**
     * r39: cache local do log (sincronizado com TerminalChannelStorage).
     * NÃO mais salvo em NBT do BE — o storage server-side é a fonte da verdade.
     */
    private final LinkedList<LogEntry> log = new LinkedList<>();
    private boolean registeredAsListener = false;

    /** ContainerData: 0=active, 1-2=energy cur, 3-4=energy max, 5=log size */
    private final ContainerData containerData = new SimpleContainerData(6);

    public QuantumTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUANTUM_TERMINAL.get(), pos, state);
    }

    public String getFrequency() { return frequency; }
    public boolean isActive() { return active; }
    public ContainerData getContainerData() { return containerData; }

    /** Cópia thread-safe do log pra UI. */
    public List<LogEntry> getLogCopy() {
        synchronized (log) {
            return new ArrayList<>(log);
        }
    }

    public void setFrequency(String f) {
        if (f == null) f = "";
        f = f.trim();
        if (f.length() > AntennaNetwork.MAX_FREQ_LENGTH) {
            f = f.substring(0, AntennaNetwork.MAX_FREQ_LENGTH);
        }
        StringBuilder sb = new StringBuilder();
        for (char c : f.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                sb.append(Character.toUpperCase(c));
            }
        }
        String newFreq = sb.toString();
        if (newFreq.equals(this.frequency)) return; // no-op

        this.frequency = newFreq;
        // r39: LIMPA display local e RECARREGA do storage do novo canal
        reloadLogFromStorage();

        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * r39: recarrega o cache local de mensagens do canal atual a partir do
     * storage server-side. Chamado quando freq muda, quando recebe broadcast,
     * ou quando vira ativo.
     */
    public void reloadLogFromStorage() {
        synchronized (log) {
            log.clear();
            if (frequency.isEmpty() || !(level instanceof ServerLevel sl)) {
                return;
            }
            TerminalChannelStorage storage = TerminalChannelStorage.get(sl);
            List<LogEntry> saved = storage.getMessages(frequency);
            // saved já está em ordem newest-first; mantém
            int n = Math.min(saved.size(), MAX_LOG_ENTRIES);
            for (int i = 0; i < n; i++) log.add(saved.get(i));
        }
    }

    /**
     * Envia uma mensagem pelo terminal. r39: salva no storage + notifica
     * listeners via AntennaNetwork. Sem chat broadcast.
     */
    public boolean sendMessage(net.minecraft.server.level.ServerPlayer sender, String message) {
        if (!active || frequency.isEmpty()) return false;
        if (energy.getEnergyStored() < 10) return false;
        if (message == null || message.trim().isEmpty()) return false;
        if (!(level instanceof ServerLevel sl)) return false;

        energy.setStored(energy.getEnergyStored() - 10);

        String trimmed = message.trim();
        if (trimmed.length() > AntennaNetwork.MAX_MSG_LENGTH) {
            trimmed = trimmed.substring(0, AntennaNetwork.MAX_MSG_LENGTH);
        }

        // r39: salva DIRETO no storage (fonte de verdade)
        LogEntry entry = new LogEntry(level.getGameTime(), sender.getName().getString(),
                frequency, trimmed);
        TerminalChannelStorage storage = TerminalChannelStorage.get(sl);
        storage.addMessage(frequency, entry);

        // Broadcast via network — NÃO vai pro chat (r39), apenas notifica listeners
        AntennaNetwork.broadcastMessage(sender.server, frequency, sender,
                trimmed, getGlobalPos());

        // Recarrega cache local (próxima refresh GUI vê a msg nova)
        reloadLogFromStorage();
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    public net.minecraft.core.GlobalPos getGlobalPos() {
        var dim = (level == null) ? net.minecraft.world.level.Level.OVERWORLD : level.dimension();
        return net.minecraft.core.GlobalPos.of(dim, worldPosition);
    }

    /** Tick server-side — consome FE, atualiza active state. */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   QuantumTerminalBlockEntity be) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel)) return;
        // FE consumption
        if (level.getGameTime() % 20 == 0) {
            if (be.energy.getEnergyStored() >= FE_PER_SEC) {
                be.energy.setStored(be.energy.getEnergyStored() - FE_PER_SEC);
                if (!be.active) {
                    be.active = true;
                    if (!be.registeredAsListener) {
                        AntennaNetwork.addListener(be);
                        be.registeredAsListener = true;
                    }
                    // r39: quando ativa, recarrega log do canal atual
                    be.reloadLogFromStorage();
                    be.setChanged();
                }
            } else {
                if (be.active) {
                    be.active = false;
                    if (be.registeredAsListener) {
                        AntennaNetwork.removeListener(be);
                        be.registeredAsListener = false;
                    }
                    be.setChanged();
                }
            }
        }
        be.updateContainerData();
    }

    private void updateContainerData() {
        containerData.set(0, active ? 1 : 0);
        int e = energy.getEnergyStored();
        containerData.set(1, e & 0xFFFF);
        containerData.set(2, (e >>> 16) & 0xFFFF);
        int max = energy.getMaxEnergyStored();
        containerData.set(3, max & 0xFFFF);
        containerData.set(4, (max >>> 16) & 0xFFFF);
        containerData.set(5, log.size());
    }

    // ───────────────── AntennaNetwork.BroadcastListener ─────────────────

    @Override
    public void onBroadcast(String broadcastFreq, String senderName, String message) {
        if (!active) return;
        if (level == null) return;
        if (frequency.isEmpty() || !frequency.equals(broadcastFreq)) return;
        if (!(level instanceof ServerLevel sl)) return;

        // r39: storage é fonte de verdade. Se o broadcast veio de OUTRO terminal
        // ele já salvou. Se veio de uma antena externa (via outro código),
        // adicionamos aqui. Pra evitar duplicação, usamos a heurística:
        // verifica se a última mensagem do storage já é idêntica (mesma freq,
        // sender, message, dentro de 2s) — se sim, skip add.
        TerminalChannelStorage storage = TerminalChannelStorage.get(sl);
        List<LogEntry> recent = storage.getMessages(broadcastFreq);
        boolean alreadySaved = false;
        long now = level.getGameTime();
        if (!recent.isEmpty()) {
            LogEntry last = recent.get(0);
            if (last.sender.equals(senderName) && last.message.equals(message)
                    && Math.abs(last.gameTime - now) < 40) {
                alreadySaved = true;
            }
        }
        if (!alreadySaved) {
            storage.addMessage(broadcastFreq,
                    new LogEntry(now, senderName, broadcastFreq, message));
        }

        // Refresh cache local
        reloadLogFromStorage();
        setChanged();
        if (!level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public String listeningFrequency() {
        return frequency.isEmpty() ? null : frequency;
    }

    // ───────────────── Lifecycle ─────────────────

    @Override
    public void setRemoved() {
        if (registeredAsListener) {
            AntennaNetwork.removeListener(this);
            registeredAsListener = false;
        }
        super.setRemoved();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergy = LazyOptional.of(() -> energy);
        // r39: ao carregar (server-side), recarrega o cache do storage
        if (level instanceof ServerLevel) {
            reloadLogFromStorage();
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate();
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putString("Frequency", frequency);
        tag.putBoolean("Active", active);
        // r39: log local NÃO salvo no BE (storage server-side é fonte de verdade)
        // Mantém compat: descarta log antigo do NBT se houver
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Energy")) energy.setStored(tag.getInt("Energy"));
        if (tag.contains("Frequency")) frequency = tag.getString("Frequency");
        if (tag.contains("Active")) active = tag.getBoolean("Active");
        // r39: log antigo ignorado — vamos carregar do storage no onLoad
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        // r39: pro client, incluímos o log local (cache) pra GUI renderizar
        ListTag list = new ListTag();
        synchronized (log) {
            for (LogEntry e : log) list.add(e.toNbt());
        }
        tag.put("ClientLog", list);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        // Client lê o log do tag e popula cache local pra GUI
        if (tag.contains("ClientLog")) {
            synchronized (log) {
                log.clear();
                ListTag list = tag.getList("ClientLog", 10);
                for (int i = 0; i < list.size(); i++) {
                    log.add(LogEntry.fromNbt(list.getCompound(i)));
                }
            }
        }
    }

    // ───────────────── MenuProvider ─────────────────

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("§bQuantum Terminal");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player player) {
        return new br.com.murilo.liberthia.menu.QuantumTerminalMenu(containerId, inv, this, containerData);
    }
}
