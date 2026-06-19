package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import br.com.murilo.liberthia.menu.DimensionalAntennaMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModItems;
import br.com.murilo.liberthia.voice.AntennaNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r27: BlockEntity da Dimensional Antenna.
 *
 * <h2>Arquitetura</h2>
 * <ul>
 *   <li><b>2 slots ItemStackHandler</b>: amethyst (slot 0) + dark matter shard (slot 1)</li>
 *   <li><b>Energia FE</b>: buffer 50k, accept 1k/tick, extract 0</li>
 *   <li><b>Frequência string</b>: max 16 chars, salva NBT</li>
 *   <li><b>Estado ATIVO</b>: requer ≥1 amethyst, ≥1 DM shard, ≥1k FE, freq não vazia</li>
 *   <li><b>Consumo</b>: a cada 100t (5s): 1 amethyst + 1 DM + 1000 FE</li>
 *   <li><b>Registry</b>: chama {@link AntennaNetwork#register} quando ATIVO,
 *       {@link AntennaNetwork#unregister} quando INATIVO ou bloco quebrado</li>
 * </ul>
 *
 * <h2>ContainerData (sync com GUI)</h2>
 * <ul>
 *   <li>0: active (0/1)</li>
 *   <li>1: tuned count (quantas antenas na mesma freq)</li>
 *   <li>2-3: energy current (split em 2 shorts)</li>
 *   <li>4-5: energy max (split em 2 shorts)</li>
 * </ul>
 */
public class DimensionalAntennaBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_AMETHYST = 0;
    public static final int SLOT_DARK_MATTER = 1;
    public static final int SLOTS = 2;

    public static final int FE_BUFFER = 50000;
    public static final int FE_PER_OP = 1000;
    /**
     * r39: consumo MUITO mais espaçado — 1 amethyst + 1 dark matter shard + 1k FE
     * a cada 10 MINUTOS (era 5s antes, OP demais e farmava ammo rápido).
     * 12000 ticks = 600 segundos = 10 minutos.
     */
    public static final int CONSUME_INTERVAL_TICKS = 12000;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == SLOT_AMETHYST) {
                return stack.is(net.minecraft.world.item.Items.AMETHYST_SHARD);
            }
            if (slot == SLOT_DARK_MATTER) {
                return stack.is(ModItems.DARK_MATTER_SHARD.get());
            }
            return false;
        }
    };

    private final TrackedEnergyStorage energy =
            new TrackedEnergyStorage(this, FE_BUFFER, FE_PER_OP, 0);

    private LazyOptional<IItemHandler> lazyItem = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    private String frequency = "";
    private boolean active = false;

    /** ContainerData expõe estado pro Menu/Screen.
     * 0: active, 1: tuned count, 2-3: energy cur, 4-5: energy max,
     * 6: best tuning % (0-100), 7: facing ordinal.
     */
    private final ContainerData containerData = new SimpleContainerData(8);

    public DimensionalAntennaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIMENSIONAL_ANTENNA.get(), pos, state);
    }

    public String getFrequency() {
        return frequency;
    }

    /**
     * Atualiza frequência. Re-registra na network se ainda ativa. Chamado
     * pelo packet do client.
     */
    public void setFrequency(String freq) {
        if (freq == null) freq = "";
        // Sanitize: ASCII printable, max length, trim
        freq = freq.trim();
        if (freq.length() > AntennaNetwork.MAX_FREQ_LENGTH) {
            freq = freq.substring(0, AntennaNetwork.MAX_FREQ_LENGTH);
        }
        // Permite só letras/numeros/dash/underscore
        StringBuilder sb = new StringBuilder();
        for (char c : freq.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                sb.append(Character.toUpperCase(c));
            }
        }
        this.frequency = sb.toString();
        // r27 BUGFIX: Se freq vazia, desregistra E desativa. Se ativa com
        // freq nova, re-registra.
        if (level instanceof ServerLevel sl) {
            if (this.frequency.isEmpty()) {
                AntennaNetwork.unregister(getGlobalPos());
                this.active = false;
            } else if (active) {
                AntennaNetwork.register(getGlobalPos(), this.frequency);
            }
        }
        setChanged();
        notifyClient();
    }

    public boolean isActive() {
        return active;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public IEnergyStorage getEnergy() {
        return energy;
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    public GlobalPos getGlobalPos() {
        // r27 BUGFIX: level pode ser null durante load — fallback overworld
        // (only used em logging/registry casos onde nivel deveria existir)
        var dim = (level == null) ? net.minecraft.world.level.Level.OVERWORLD : level.dimension();
        return GlobalPos.of(dim, worldPosition);
    }

    /**
     * Tick server-side — r39: ativa/desativa baseado em recursos a cada
     * SEGUNDO, mas só CONSOME a cada 10 minutos.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   DimensionalAntennaBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;

        // r39: atualiza estado ATIVO a cada 1s (cheap, sem consumir)
        if (level.getGameTime() % 20 == 0) {
            be.updateActiveState(sl);
        }

        // r39: CONSUMO real só a cada 10min
        if (level.getGameTime() % CONSUME_INTERVAL_TICKS == 0) {
            be.tryConsume(sl);
        }

        // Sync ContainerData (atualiza cada tick — barato)
        be.updateContainerData();

        // Particles visuais se ativo (a cada 20t)
        if (be.active && level.getGameTime() % 20 == 0) {
            sl.sendParticles(ParticleTypes.PORTAL,
                    pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    2, 0.2, 0.1, 0.2, 0.02);
        }
    }

    /**
     * r39: verifica se a antena PODE estar ativa (tem recursos suficientes pra
     * próximo consumo). Atualiza estado active/inactive imediatamente sem
     * consumir. Player coloca fuel → antena ativa em 1s, não 10 min.
     */
    private void updateActiveState(ServerLevel sl) {
        boolean canBeActive = hasResources();
        if (canBeActive && !active) {
            active = true;
            AntennaNetwork.register(getGlobalPos(), frequency);
            sl.playSound(null, worldPosition,
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.7F);
            setChanged();
        } else if (!canBeActive && active) {
            active = false;
            AntennaNetwork.unregister(getGlobalPos());
            sl.playSound(null, worldPosition,
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 0.5F);
            setChanged();
        }
    }

    private boolean hasResources() {
        ItemStack amethyst = inventory.getStackInSlot(SLOT_AMETHYST);
        ItemStack darkMatter = inventory.getStackInSlot(SLOT_DARK_MATTER);
        return !amethyst.isEmpty() && !darkMatter.isEmpty()
                && energy.getEnergyStored() >= FE_PER_OP
                && !frequency.isEmpty();
    }

    /**
     * r39: consome 1 amethyst + 1 dark matter + 1k FE. Chamado a cada 10 min.
     * Se faltar algo, antena desativa no próximo updateActiveState.
     */
    private void tryConsume(ServerLevel sl) {
        if (!hasResources()) {
            // Sem recursos pra consumir — antena vai ficar inativa
            if (active) {
                active = false;
                AntennaNetwork.unregister(getGlobalPos());
                setChanged();
            }
            return;
        }
        inventory.extractItem(SLOT_AMETHYST, 1, false);
        inventory.extractItem(SLOT_DARK_MATTER, 1, false);
        energy.setStored(energy.getEnergyStored() - FE_PER_OP);
        setChanged();
        // Som curto sinalizando consumo
        sl.playSound(null, worldPosition,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    private void updateContainerData() {
        containerData.set(0, active ? 1 : 0);
        containerData.set(1, AntennaNetwork.countTuned(frequency));
        int e = energy.getEnergyStored();
        containerData.set(2, e & 0xFFFF);
        containerData.set(3, (e >>> 16) & 0xFFFF);
        int max = energy.getMaxEnergyStored();
        containerData.set(4, max & 0xFFFF);
        containerData.set(5, (max >>> 16) & 0xFFFF);
        // r28: tuning % + facing
        containerData.set(6, (int) calculateBestTuning());
        net.minecraft.core.Direction f = getFacing();
        containerData.set(7, f == null ? 2 : f.get3DDataValue());
    }

    /**
     * r28: calcula o MELHOR tuning entre essa antena e qualquer outra
     * tunada na mesma freq. Retorna 0 se sem outras antenas.
     */
    public double calculateBestTuning() {
        if (!active || frequency.isEmpty() || level == null) return 0;
        net.minecraft.core.Direction myFacing = getFacing();
        if (myFacing == null) return 0;
        net.minecraft.core.GlobalPos myPos = getGlobalPos();
        double best = 0;
        for (net.minecraft.core.GlobalPos other : AntennaNetwork.getTunedAntennas(frequency)) {
            if (other.equals(myPos)) continue;
            // Pra outra antena, precisamos do facing dela. Busca BE no level dela.
            net.minecraft.core.Direction otherFacing = lookupFacing(other);
            if (otherFacing == null) continue;
            double t = AntennaNetwork.calculateTuning(myPos, myFacing, other, otherFacing);
            if (t > best) best = t;
        }
        return best;
    }

    private net.minecraft.core.Direction lookupFacing(net.minecraft.core.GlobalPos gpos) {
        if (!(level instanceof ServerLevel sl)) return null;
        var serverLevel = sl.getServer().getLevel(gpos.dimension());
        if (serverLevel == null) return null;
        // Não force-load — se chunk não carregado, ignora
        var state = serverLevel.getBlockState(gpos.pos());
        if (state.hasProperty(br.com.murilo.liberthia.block.DimensionalAntennaBlock.FACING)) {
            return state.getValue(br.com.murilo.liberthia.block.DimensionalAntennaBlock.FACING);
        }
        return null;
    }

    /** Pega facing do blockstate. */
    public net.minecraft.core.Direction getFacing() {
        if (level == null) return net.minecraft.core.Direction.NORTH;
        var state = level.getBlockState(worldPosition);
        if (state.hasProperty(br.com.murilo.liberthia.block.DimensionalAntennaBlock.FACING)) {
            return state.getValue(br.com.murilo.liberthia.block.DimensionalAntennaBlock.FACING);
        }
        return net.minecraft.core.Direction.NORTH;
    }

    /** Notifica clients que estão olhando o block (sync visual immediate). */
    private void notifyClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItem.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItem = LazyOptional.of(() -> inventory);
        lazyEnergy = LazyOptional.of(() -> energy);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItem.invalidate();
        lazyEnergy.invalidate();
    }

    /** Quando o bloco é quebrado, limpa registry. */
    public void onBroken() {
        if (active && level != null) {
            AntennaNetwork.unregister(getGlobalPos());
            active = false;
        }
    }

    /**
     * r27 BUGFIX: chunk unload → BE.setRemoved() → desregistra do AntennaNetwork
     * pra evitar broadcast pra antena fantasma que vai re-registrar quando
     * chunk re-carregar (via active recheck no tick).
     */
    @Override
    public void setRemoved() {
        if (active && level != null && !level.isClientSide) {
            AntennaNetwork.unregister(getGlobalPos());
        }
        super.setRemoved();
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putString("Frequency", frequency);
        tag.putBoolean("Active", active);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) inventory.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("Energy")) energy.setStored(tag.getInt("Energy"));
        if (tag.contains("Frequency")) frequency = tag.getString("Frequency");
        if (tag.contains("Active")) active = tag.getBoolean("Active");
    }

    // ────────────────────────── Sync packet pra render visual ────────

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    // ────────────────────────── MenuProvider ─────────────────────────

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("§5Antena Dimensional");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player player) {
        return new DimensionalAntennaMenu(containerId, inv, this, containerData);
    }
}
