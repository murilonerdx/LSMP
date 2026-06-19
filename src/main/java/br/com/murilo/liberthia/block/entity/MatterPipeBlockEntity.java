package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.block.MatterPipeBlock;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * BlockEntity de Matter Pipe.
 *
 * <p>Pipe atua como um buffer pequeno (250 mB) que move fluido pra qualquer
 * FLUID_HANDLER vizinho com espaço. Mesma equalização passiva do tank — não
 * usa BFS / routing inteligente porque overhead, basta o fluido se espalhar
 * por proximidade.
 *
 * <p>Vantagem: simples, sem bug surface, conecta com tanks/extractor/pipes
 * indiscriminadamente desde que sejam do mesmo tipo de fluido. Spread limit
 * de 200 mB/tick por vizinho.
 */
public class MatterPipeBlockEntity extends BlockEntity {

    public static final int CAPACITY = 250;
    public static final int SPREAD_RATE = 200;

    private final Supplier<Fluid> allowedFluid;
    private final FluidTank tank;
    private LazyOptional<IFluidHandler> lazyFluid = LazyOptional.empty();

    /** Construtor usado pelo BlockEntityType (sem fluid setter). Resolve via state. */
    public MatterPipeBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, () -> {
            if (state.getBlock() instanceof MatterPipeBlock p) return p.getAllowedFluid();
            return null;
        });
    }

    public MatterPipeBlockEntity(BlockPos pos, BlockState state, Supplier<Fluid> allowedFluid) {
        super(ModBlockEntities.MATTER_PIPE.get(), pos, state);
        this.allowedFluid = allowedFluid;
        this.tank = new FluidTank(CAPACITY, s -> {
            if (s.isEmpty()) return false;
            Fluid allowed = allowedFluid.get();
            return allowed != null && s.getFluid() == allowed;
        }) {
            @Override protected void onContentsChanged() { setChanged(); markUpdated(); }
        };
    }

    private void markUpdated() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluid.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyFluid = LazyOptional.of(() -> tank);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFluid.invalidate();
    }

    /**
     * Tick: tenta empurrar fluido pra QUALQUER vizinho com FluidHandler que aceite
     * (não só pipes — também tanks/extractor). Para cada vizinho, transfere até
     * SPREAD_RATE mB. Sem ordem específica (Direction.values).
     */
    public static void tick(Level level, BlockPos pos, BlockState state, MatterPipeBlockEntity be) {
        if (level.isClientSide) return;
        FluidStack content = be.tank.getFluid();
        if (content.isEmpty()) return;

        for (Direction d : Direction.values()) {
            BlockEntity nbe = level.getBlockEntity(pos.relative(d));
            if (nbe == null) continue;

            // Se vizinho é outro pipe, só transfere se for do mesmo tipo
            if (nbe instanceof MatterPipeBlockEntity npipe) {
                Fluid ours = be.allowedFluid.get();
                Fluid theirs = npipe.allowedFluid.get();
                if (ours != theirs) continue;
            }

            LazyOptional<IFluidHandler> cap = nbe.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite());
            cap.ifPresent(handler -> {
                int toMove = Math.min(SPREAD_RATE, be.tank.getFluidAmount());
                if (toMove <= 0) return;
                FluidStack out = new FluidStack(be.tank.getFluid(), toMove);
                int filled = handler.fill(out, IFluidHandler.FluidAction.SIMULATE);
                if (filled <= 0) return;
                FluidStack actual = new FluidStack(be.tank.getFluid(), filled);
                handler.fill(actual, IFluidHandler.FluidAction.EXECUTE);
                be.tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            });
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("tank")) tank.readFromNBT(tag.getCompound("tank"));
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
