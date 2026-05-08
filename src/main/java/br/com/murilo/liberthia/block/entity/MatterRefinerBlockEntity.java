package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.MatterRefinerMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModItems;
import br.com.murilo.liberthia.util.Purity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * Matter Refiner — refina {@code inactive_dark_matter} elevando sua
 * {@link Purity} um nível por operação.
 *
 * <ul>
 *   <li>Slot 0 (INPUT): inactive_dark_matter (qualquer purity 0..4)</li>
 *   <li>Slot 1 (CATALYST): {@code minecraft:diamond} (+1 purity, max 4)
 *       OU {@code minecraft:nether_star} (vai direto pra purity 5).</li>
 *   <li>Slot 2 (OUTPUT): inactive_dark_matter com purity++ (ou =5 se nether)</li>
 * </ul>
 *
 * <p>Custo FE por operação: 25.000 FE base, +25.000 por nível atual.
 * Nether star sempre custa 200.000 FE.
 */
public class MatterRefinerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_CATALYST = 1;
    public static final int SLOT_OUTPUT = 2;

    public static final int FE_BUFFER = 500_000;
    public static final int FE_PER_TICK = 1_000;
    public static final int BASE_PROCESS_TICKS = 100;
    public static final int FE_PER_DIAMOND = 25_000;
    public static final int FE_NETHER_STAR = 200_000;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_INPUT -> stack.is(ModItems.INACTIVE_DARK_MATTER.get());
                case SLOT_CATALYST -> stack.is(Items.DIAMOND) || stack.is(Items.NETHER_STAR);
                case SLOT_OUTPUT -> stack.is(ModItems.INACTIVE_DARK_MATTER.get());
                default -> false;
            };
        }
    };

    private final br.com.murilo.liberthia.energy.TrackedEnergyStorage energy =
            new br.com.murilo.liberthia.energy.TrackedEnergyStorage(this, FE_BUFFER, FE_PER_TICK * 4, 0);
    private LazyOptional<IItemHandler> lazyItem = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    private int progress = 0;
    private int feSpent = 0;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            int v = switch (i) {
                case 0, 1 -> energy.getEnergyStored();
                case 2, 3 -> energy.getMaxEnergyStored();
                case 4 -> progress;
                case 5 -> BASE_PROCESS_TICKS;
                case 6 -> feSpent;
                case 7 -> currentFeRequired();
                default -> 0;
            };
            return switch (i) {
                case 0, 2 -> (v >> 16) & 0xFFFF;
                case 1, 3 -> v & 0xFFFF;
                default -> v;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 8; }
    };

    public MatterRefinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_REFINER.get(), pos, state);
    }

    public IItemHandler getInventory() { return inventory; }

    public int currentFeRequired() {
        ItemStack cat = inventory.getStackInSlot(SLOT_CATALYST);
        if (cat.is(Items.NETHER_STAR)) return FE_NETHER_STAR;
        // Diamond: custo escala com nível atual
        ItemStack in = inventory.getStackInSlot(SLOT_INPUT);
        int cur = Purity.getPurity(in);
        return FE_PER_DIAMOND * (1 + cur);
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItem.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() {
        super.onLoad();
        lazyItem = LazyOptional.of(() -> inventory);
        lazyEnergy = LazyOptional.of(() -> energy);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        lazyItem.invalidate();
        lazyEnergy.invalidate();
    }

    public void drops() {
        net.minecraft.world.SimpleContainer c = new net.minecraft.world.SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        net.minecraft.world.Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.matter_refiner");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new MatterRefinerMenu(id, inv, this, this.data);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MatterRefinerBlockEntity be) {
        if (level.isClientSide) {
            tickClientParticles(level, pos, be);
            return;
        }
        if (!canProcess(be)) {
            be.progress = 0;
            be.feSpent = 0;
            return;
        }
        int feNeeded = be.currentFeRequired();
        int drain = Math.min(FE_PER_TICK, be.energy.getEnergyStored());
        if (drain <= 0) return;
        be.energy.extractEnergy(drain, false);
        be.feSpent += drain;
        be.progress++;

        if (be.progress >= BASE_PROCESS_TICKS && be.feSpent >= feNeeded) {
            ItemStack input = be.inventory.getStackInSlot(SLOT_INPUT);
            ItemStack catalyst = be.inventory.getStackInSlot(SLOT_CATALYST);
            int curPurity = Purity.getPurity(input);
            int newPurity;
            if (catalyst.is(Items.NETHER_STAR)) {
                newPurity = Purity.MAX;
            } else {
                newPurity = Math.min(Purity.MAX - 1, curPurity + 1);
            }

            be.inventory.extractItem(SLOT_INPUT, 1, false);
            be.inventory.extractItem(SLOT_CATALYST, 1, false);
            ItemStack out = new ItemStack(ModItems.INACTIVE_DARK_MATTER.get());
            Purity.setPurity(out, newPurity);
            be.inventory.insertItem(SLOT_OUTPUT, out, false);

            be.progress = 0;
            be.feSpent = 0;
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        20, 0.3, 0.3, 0.3, 0.1);
            }
        }
        be.setChanged();
        // Sync periódico pro client poder animar (a cada 5t)
        if (level.getGameTime() % 5 == 0) {
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /** Animação client-side enquanto refina — partículas roxas + soul fire. */
    private static void tickClientParticles(Level level, BlockPos pos, MatterRefinerBlockEntity be) {
        if (be.progress <= 0) return;
        float pct = be.progress / (float) BASE_PROCESS_TICKS;
        // Espirais de enchant subindo
        if (level.random.nextFloat() < 0.6f) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = 0.35;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    pos.getX() + 0.5 + Math.cos(a) * r,
                    pos.getY() + 1.0 + level.random.nextDouble() * 0.4,
                    pos.getZ() + 0.5 + Math.sin(a) * r,
                    0, 0.05 + pct * 0.1, 0);
        }
        // Soul fire flame quando avança
        if (level.random.nextFloat() < pct * 0.4f) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    (level.random.nextDouble() - 0.5) * 0.05,
                    0.02 + pct * 0.05,
                    (level.random.nextDouble() - 0.5) * 0.05);
        }
        // Som ambiente quando bem perto de completar
        if (pct > 0.8f && level.random.nextInt(40) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, 1.5f, false);
        }
    }

    private static boolean canProcess(MatterRefinerBlockEntity be) {
        ItemStack input = be.inventory.getStackInSlot(SLOT_INPUT);
        ItemStack catalyst = be.inventory.getStackInSlot(SLOT_CATALYST);
        ItemStack output = be.inventory.getStackInSlot(SLOT_OUTPUT);
        if (input.isEmpty() || catalyst.isEmpty()) return false;
        // Diamond só aceita se input ainda não tá no max-1 (pra não desperdiçar)
        int curPurity = Purity.getPurity(input);
        if (catalyst.is(Items.DIAMOND) && curPurity >= Purity.MAX - 1) return false;
        if (curPurity >= Purity.MAX) return false; // já no max
        if (!output.isEmpty()) {
            int outPurity = Purity.getPurity(output);
            int targetPurity = catalyst.is(Items.NETHER_STAR) ? Purity.MAX : curPurity + 1;
            if (outPurity != targetPurity) return false; // pureza diferente, não stacka
            if (output.getCount() >= output.getMaxStackSize()) return false;
        }
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inv", inventory.serializeNBT());
        tag.put("energy", energy.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("feSpent", feSpent);
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        if (tag.contains("energy")) energy.deserializeNBT(tag.get("energy"));
        progress = tag.getInt("progress");
        feSpent = tag.getInt("feSpent");
    }

    @Nullable @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
