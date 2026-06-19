package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.block.MatterTankBlock;
import br.com.murilo.liberthia.menu.MatterTankMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModFluids;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity do Matter Tank.
 *
 * <p>Armazena até {@link #CAPACITY} mB de fluido (dark/clear/yellow matter). Cada
 * BE é independente, mas o tick auto-equaliza com vizinhos (6 direções) do mesmo
 * tipo de fluido — isso simula um multiblock sem master/slave. Decisão de design:
 * equalização passiva é MUITO mais simples de implementar e debugar do que master/
 * slave (sem invalidação de capabilities cruzadas, sem bug surface de orfanizar
 * slaves, sem rota de update obrigatória). O tradeoff é que o spread leva alguns
 * ticks pra propagar — aceitável.
 *
 * <p>2 slots de inventário:
 * <ul>
 *   <li>{@link #SLOT_BUCKET_IN} (vazio in) — bucket vazio entra, sai cheio com
 *       o fluido do tank (drena 1000 mB).</li>
 *   <li>{@link #SLOT_BUCKET_OUT} (cheio in) — bucket cheio do tipo certo entra,
 *       sai vazio, despeja 1000 mB no tank.</li>
 * </ul>
 */
public class MatterTankBlockEntity extends BlockEntity implements MenuProvider {

    public static final int CAPACITY = 16_000;
    /** mB equalizados por tick por vizinho. Baixo pra não saturar — 100 mB/tick = 5 buckets/s. */
    public static final int EQUALIZE_RATE = 100;

    public static final int SLOT_BUCKET_IN = 0;
    public static final int SLOT_BUCKET_OUT = 1;

    /**
     * Tank Forge padrão. Aceita só os 3 tipos de matter (dark/clear/yellow) via
     * predicate. Quando vazio, qualquer um deles pode entrar; quando tem fluido,
     * só o mesmo tipo (auto-lock por isFluidValid).
     */
    private final FluidTank tank = new FluidTank(CAPACITY, this::isAcceptedFluid) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            // v0.1.48: NÃO atualiza mais blockstate level — o BER renderiza
            // o fluido proporcional direto. Antes os 2 sistemas competiam.
            markUpdated();
        }
    };

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            markUpdated();
        }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == SLOT_BUCKET_IN) {
                // v0.1.44: aceita ambos — bucket vazio (será preenchido) OU
                // matter bucket cheio (será esvaziado). User pediu: "colocando
                // o balde no tanque nada acontece" — provavelmente ele tava
                // pondo matter bucket no IN que só aceitava vazio.
                var i = stack.getItem();
                return stack.is(Items.BUCKET)
                        || i == ModItems.DARK_MATTER_BUCKET.get()
                        || i == ModItems.CLEAR_MATTER_BUCKET.get()
                        || i == ModItems.YELLOW_MATTER_BUCKET.get();
            }
            if (slot == SLOT_BUCKET_OUT) {
                // OUT é só de saída — não aceita inserção do player.
                return false;
            }
            return false;
        }
    };

    private LazyOptional<IFluidHandler> lazyFluid = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyItem = LazyOptional.empty();

    /**
     * ContainerData mínima pra GUI: amount, capacity, fluidId. Encodamos o fluidId
     * pelo nome registry (string) via approach diferente — usamos o NBT update tag
     * pra mandar dados ricos, e ContainerData só pros ints de progresso.
     */
    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> tank.getFluidAmount();
                case 1 -> tank.getCapacity();
                case 2 -> getFluidTypeId();
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 3; }
    };

    public MatterTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_TANK.get(), pos, state);
    }

    /** 0=empty, 1=dark, 2=clear, 3=yellow. */
    public int getFluidTypeId() {
        FluidStack f = tank.getFluid();
        if (f.isEmpty()) return 0;
        Fluid fl = f.getFluid();
        if (fl == ModFluids.DARK_MATTER.get() || fl == ModFluids.FLOWING_DARK_MATTER.get()) return 1;
        if (fl == ModFluids.CLEAR_MATTER.get() || fl == ModFluids.FLOWING_CLEAR_MATTER.get()) return 2;
        if (fl == ModFluids.YELLOW_MATTER.get() || fl == ModFluids.FLOWING_YELLOW_MATTER.get()) return 3;
        return 0;
    }

    public FluidStack getFluidStack() { return tank.getFluid(); }
    public FluidTank getTank() { return tank; }
    public IItemHandler getInventory() { return inventory; }

    /** Filtro de aceitação do tank — só os 3 tipos de matter. */
    private boolean isAcceptedFluid(FluidStack stack) {
        if (stack.isEmpty()) return false;
        Fluid f = stack.getFluid();
        return f == ModFluids.DARK_MATTER.get()
                || f == ModFluids.CLEAR_MATTER.get()
                || f == ModFluids.YELLOW_MATTER.get();
    }

    /**
     * Atualiza o blockstate `level` (0..4) baseado no fillage. Chamado todo
     * onContentsChanged. Não disparado pra valores 0 → mantém pra economia de
     * setBlock no caso comum sem mudança visual.
     */
    private void updateBlockstateLevel() {
        if (level == null || level.isClientSide) return;
        int newLevel = computeVisualLevel();
        BlockState st = getBlockState();
        if (st.hasProperty(MatterTankBlock.LEVEL) && st.getValue(MatterTankBlock.LEVEL) != newLevel) {
            level.setBlock(worldPosition, st.setValue(MatterTankBlock.LEVEL, newLevel), 3);
        }
    }

    private int computeVisualLevel() {
        int amt = tank.getFluidAmount();
        if (amt <= 0) return 0;
        // Buckets de 4000 mB cada. < 25% = 0 (vazio), 25-50% = 1, 50-75% = 2, 75-100% = 3, == cheio = 4
        int pct = (amt * 100) / tank.getCapacity();
        if (pct >= 100) return 4;
        if (pct >= 75) return 3;
        if (pct >= 50) return 2;
        if (pct >= 25) return 1;
        return 0;
    }

    /** Limpa o tank. */
    public void purge() {
        tank.setFluid(FluidStack.EMPTY);
    }

    private void markUpdated() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluid.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItem.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyFluid = LazyOptional.of(() -> tank);
        lazyItem = LazyOptional.of(() -> inventory);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyFluid.invalidate();
        lazyItem.invalidate();
    }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.matter_tank");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new MatterTankMenu(id, inv, this, this.data);
    }

    /**
     * Tick principal. 2 tarefas:
     * <ul>
     *   <li>Auto-equaliza com vizinhos (apenas se tiverem o mesmo tipo de fluido).
     *       Spread limitado a {@link #EQUALIZE_RATE} mB por vizinho por tick.</li>
     *   <li>Processa os slots de bucket in/out a cada 10 ticks.</li>
     * </ul>
     */
    public static void tick(Level level, BlockPos pos, BlockState state, MatterTankBlockEntity be) {
        if (level.isClientSide) return;

        // Equalização passiva — só se temos fluido
        if (!be.tank.getFluid().isEmpty()) {
            equalizeWithNeighbors(level, pos, be);
        }

        // Slots de bucket a cada 10 ticks (eficiência)
        if (level.getGameTime() % 10 == 0) {
            processBuckets(be);
        }
    }

    /**
     * Spreads fluid for adjacent matter tanks of the same fluid type. Move FLUID
     * do tank com MAIS pro tank com MENOS, equalizando.
     */
    private static void equalizeWithNeighbors(Level level, BlockPos pos, MatterTankBlockEntity be) {
        FluidStack ourFluid = be.tank.getFluid();
        if (ourFluid.isEmpty()) return;

        for (Direction d : Direction.values()) {
            BlockEntity nbe = level.getBlockEntity(pos.relative(d));
            if (!(nbe instanceof MatterTankBlockEntity neighbor)) continue;
            FluidStack nFluid = neighbor.tank.getFluid();
            // Vizinho vazio: pode receber qualquer
            // Vizinho com fluido: só se for o mesmo tipo
            if (!nFluid.isEmpty() && !nFluid.isFluidEqual(ourFluid)) continue;

            int ourAmt = be.tank.getFluidAmount();
            int nAmt = neighbor.tank.getFluidAmount();
            if (ourAmt <= nAmt) continue; // só "doamos" pra vizinhos com menos

            int diff = ourAmt - nAmt;
            int transfer = Math.min(EQUALIZE_RATE, diff / 2);
            if (transfer <= 0) continue;

            FluidStack moving = new FluidStack(ourFluid, transfer);
            int filled = neighbor.tank.fill(moving, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                be.tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /**
     * v0.1.44 reescrito: o slot IN agora aceita AMBOS — bucket vazio (será
     * preenchido com fluido do tank) OU matter bucket cheio (será esvaziado
     * no tank). Tick processa e move bucket resultante pro OUT.
     *
     * <p>Fluxo (player só toca no IN):
     * <ul>
     *   <li>Matter bucket no IN + tank tem espaço → drena 1000 mB no tank,
     *       OUT recebe bucket vazio.</li>
     *   <li>Bucket vazio no IN + tank tem ≥1000 mB → tank perde 1000 mB,
     *       OUT recebe matter bucket do tipo correto.</li>
     * </ul>
     *
     * <p><b>v0.1.48 fix crítico:</b> {@code insertItem(SLOT_BUCKET_OUT, ...)}
     * NÃO funciona porque o {@code isItemValid(OUT)} retorna {@code false}
     * (slot é só-saída pro player). O handler rejeita silenciosamente e o item
     * é PERDIDO — user reportou "balde vai diminuindo mas não gera nada".
     * Solução: usar {@code setStackInSlot}, que bypass o {@code isItemValid}.
     */
    private static void processBuckets(MatterTankBlockEntity be) {
        ItemStack inStack = be.inventory.getStackInSlot(SLOT_BUCKET_IN);
        if (inStack.isEmpty()) return;

        // Caso 1: matter bucket cheio → drena no tank, devolve bucket vazio
        Fluid bucketFluid = fluidForBucket(inStack);
        if (bucketFluid != null) {
            FluidStack toFill = new FluidStack(bucketFluid, 1000);
            int simulated = be.tank.fill(toFill, IFluidHandler.FluidAction.SIMULATE);
            if (simulated == 1000) {
                ItemStack out = be.inventory.getStackInSlot(SLOT_BUCKET_OUT);
                // OUT precisa estar vazio OU ter Items.BUCKET com espaço (max=16)
                if (out.isEmpty() || (out.is(Items.BUCKET) && out.getCount() < out.getMaxStackSize())) {
                    be.tank.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
                    be.inventory.extractItem(SLOT_BUCKET_IN, 1, false);
                    // setStackInSlot pula isItemValid → não perde o item
                    ItemStack newOut = out.isEmpty()
                            ? new ItemStack(Items.BUCKET)
                            : copyAndGrow(out, 1);
                    be.inventory.setStackInSlot(SLOT_BUCKET_OUT, newOut);
                }
            }
            return;
        }

        // Caso 2: bucket vazio + tank tem ≥1000 mB → enche bucket do tipo do tank.
        // Bucket SÓ é convertido se houver ≥1000 mB disponível — user pediu pra
        // "não gerar a saída enquanto não encher o bucket certinho".
        if (inStack.is(Items.BUCKET) && be.tank.getFluidAmount() >= 1000) {
            ItemStack filledBucket = bucketForFluid(be.tank.getFluid().getFluid());
            if (!filledBucket.isEmpty()) {
                ItemStack out = be.inventory.getStackInSlot(SLOT_BUCKET_OUT);
                // Matter buckets têm stacksTo(1) — só cabe se OUT estiver vazio.
                if (out.isEmpty()) {
                    be.tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                    be.inventory.extractItem(SLOT_BUCKET_IN, 1, false);
                    be.inventory.setStackInSlot(SLOT_BUCKET_OUT, filledBucket);
                }
                // Se OUT já tem matter bucket: trava aqui até o player remover.
                // Bucket no IN não diminui.
            }
        }
    }

    /** Helper: copia o stack e incrementa o count em {@code amount}. */
    private static ItemStack copyAndGrow(ItemStack stack, int amount) {
        ItemStack copy = stack.copy();
        copy.grow(amount);
        return copy;
    }

    private static ItemStack bucketForFluid(Fluid f) {
        if (f == ModFluids.DARK_MATTER.get()) return new ItemStack(ModItems.DARK_MATTER_BUCKET.get());
        if (f == ModFluids.CLEAR_MATTER.get()) return new ItemStack(ModItems.CLEAR_MATTER_BUCKET.get());
        if (f == ModFluids.YELLOW_MATTER.get()) return new ItemStack(ModItems.YELLOW_MATTER_BUCKET.get());
        return ItemStack.EMPTY;
    }

    private static @Nullable Fluid fluidForBucket(ItemStack stack) {
        var i = stack.getItem();
        if (i == ModItems.DARK_MATTER_BUCKET.get()) return ModFluids.DARK_MATTER.get();
        if (i == ModItems.CLEAR_MATTER_BUCKET.get()) return ModFluids.CLEAR_MATTER.get();
        if (i == ModItems.YELLOW_MATTER_BUCKET.get()) return ModFluids.YELLOW_MATTER.get();
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
        tag.put("inv", inventory.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("tank")) tank.readFromNBT(tag.getCompound("tank"));
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
