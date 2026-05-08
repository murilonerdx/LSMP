package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.DimensionalExtractorMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dimensional Extractor — coletor passivo de matéria escura "dimensional".
 *
 * <p>NÃO precisa de energia (passivo, alimentado pelo próprio rift dimensional).
 * Você insere um {@link Items#BUCKET balde vazio} no slot de input; quando o
 * progresso completa, o balde é convertido em {@code dark_matter_bucket} no slot
 * de output.
 *
 * <p>Cada {@link #PERIOD} ticks reescaneia minérios de matéria escura próximos
 * (até {@link #RADIUS} blocos) + bônus do rift mais próximo. Mais minérios e
 * proximidade ao rift = produção mais rápida.
 */
public class DimensionalExtractorBlockEntity extends BlockEntity implements MenuProvider {

    public static final int PERIOD = 20;
    public static final int RADIUS = 8;
    public static final int MAX_NEARBY = 32;
    public static final int PROGRESS_PER_BUCKET = 600;

    public static final int SLOT_BUCKET_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); markUpdated(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != SLOT_BUCKET_INPUT || stack.is(Items.BUCKET);
        }
    };
    private LazyOptional<IItemHandler> lazyItem = LazyOptional.empty();

    private int progress = 0;
    private int nearbyCount = 0;
    private int riftDistance = 0;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> PROGRESS_PER_BUCKET;
                case 2 -> nearbyCount;
                case 3 -> riftDistance;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 4; }
    };

    public DimensionalExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIMENSIONAL_EXTRACTOR.get(), pos, state);
    }

    public IItemHandler getInventory() { return inventory; }
    public int getNearbyCount() { return nearbyCount; }
    public int getProgress()    { return progress; }
    public int getRiftDistance(){ return riftDistance; }

    private void markUpdated() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItem.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazyItem = LazyOptional.of(() -> inventory); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyItem.invalidate(); }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.dimensional_extractor");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new DimensionalExtractorMenu(id, inv, this, this.data);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DimensionalExtractorBlockEntity be) {
        if (level.isClientSide) {
            tickClientParticles(level, pos, be);
            return;
        }

        int oldNearby = be.nearbyCount;
        int oldRiftDist = be.riftDistance;
        int oldProgress = be.progress;

        // Reescaneia periódicamente: minérios + rifts
        if (level.getGameTime() % PERIOD == 0) {
            int oreCount = countNearbyOres(level, pos);
            int riftBonus = 0;
            int riftDist = 0;
            if (level instanceof ServerLevel sl) {
                var rifts = br.com.murilo.liberthia.world.RiftSavedData.get(sl);
                BlockPos nearestRift = rifts.findNearest(pos);
                if (nearestRift != null) {
                    double dist = Math.sqrt(nearestRift.distSqr(pos));
                    riftDist = (int) dist;
                    if (dist <= 8) riftBonus = 16;
                    else if (dist <= 32) riftBonus = (int) (16 - (dist - 8) * 14 / 24);
                    else if (dist <= 64) riftBonus = 2;
                }
            }
            be.nearbyCount = Math.min(MAX_NEARBY, oreCount + riftBonus);
            be.riftDistance = riftDist;
        }

        // Progresso só avança se houver bucket vazio E fontes nearby
        ItemStack bucketIn = be.inventory.getStackInSlot(SLOT_BUCKET_INPUT);
        ItemStack output   = be.inventory.getStackInSlot(SLOT_OUTPUT);
        boolean canOutput = output.isEmpty()
                || (output.is(ModItems.DARK_MATTER_BUCKET.get())
                        && output.getCount() < output.getMaxStackSize());

        if (be.nearbyCount > 0 && !bucketIn.isEmpty() && canOutput) {
            be.progress += be.nearbyCount;
            if (be.progress >= PROGRESS_PER_BUCKET) {
                // extractItem dispara onContentsChanged → setChanged → chunk save
                be.inventory.extractItem(SLOT_BUCKET_INPUT, 1, false);
                be.inventory.insertItem(SLOT_OUTPUT,
                        new ItemStack(ModItems.DARK_MATTER_BUCKET.get()), false);
                be.progress = 0;
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                            20, 0.3, 0.3, 0.3, 0.05);
                }
            }
            be.setChanged();
        }

        if (oldNearby != be.nearbyCount || oldRiftDist != be.riftDistance
                || Math.abs(oldProgress - be.progress) > 5) {
            be.markUpdated();
        }
    }

    private static void tickClientParticles(Level level, BlockPos pos, DimensionalExtractorBlockEntity be) {
        if (be.nearbyCount <= 0) return;
        float pct = be.progress / (float) PROGRESS_PER_BUCKET;
        if (level.random.nextFloat() < 0.3f + pct * 0.5f) {
            level.addParticle(ParticleTypes.PORTAL,
                    pos.getX() + level.random.nextDouble(),
                    pos.getY() + 1.0 + level.random.nextDouble() * 0.6,
                    pos.getZ() + level.random.nextDouble(),
                    0, 0.05 + pct * 0.05, 0);
        }
        if (be.riftDistance > 0 && be.riftDistance <= 8 && level.random.nextInt(20) == 0) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    (level.random.nextDouble() - 0.5) * 0.1, 0.05,
                    (level.random.nextDouble() - 0.5) * 0.1);
        }
    }

    private static int countNearbyOres(Level level, BlockPos pos) {
        int count = 0;
        var dmOre = ModBlocks.DARK_MATTER_ORE.get();
        var dmDeep = ModBlocks.DEEPSLATE_DARK_MATTER_ORE.get();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx += 2)
            for (int dy = -RADIUS / 2; dy <= RADIUS / 2; dy += 2)
                for (int dz = -RADIUS; dz <= RADIUS; dz += 2) {
                    m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    var b = level.getBlockState(m).getBlock();
                    if (b == dmOre || b == dmDeep) {
                        count++;
                        if (count >= MAX_NEARBY) return count;
                    }
                }
        return count;
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        tag.put("inv", inventory.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("nearby", nearbyCount);
        tag.putInt("riftDist", riftDistance);
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        progress = tag.getInt("progress");
        nearbyCount = tag.getInt("nearby");
        riftDistance = tag.getInt("riftDist");
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
