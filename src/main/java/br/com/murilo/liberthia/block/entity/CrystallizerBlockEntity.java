package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.CrystallizerMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
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
 * Crystallizer — Estágio 2 de refinação.
 *
 * <p>Slots: 0=input (inactive_dark_matter), 1=output (active_dark_matter).
 *
 * <p>Lasers ({@link LaserEmitterBlockEntity}) que atinjam este bloco
 * chamam {@link #onLaserHit(long)}. Quando há ≥{@link #LASERS_REQUIRED}
 * lasers ativos no mesmo gametick, o progresso avança em 1. Atingindo
 * {@link #PROGRESS_MAX} → consome 1 inactive e produz 1 active.
 */
public class CrystallizerBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    public static final int LASERS_REQUIRED = 2;
    public static final int PROGRESS_MAX = 400; // ~20s sob 2 lasers
    /** Janela em ticks pra contar um laser como "ativo agora". */
    public static final int HIT_WINDOW_TICKS = 4;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != SLOT_INPUT || stack.is(ModItems.INACTIVE_DARK_MATTER.get());
        }
    };
    private LazyOptional<IItemHandler> lazy = LazyOptional.empty();

    private int progress = 0;
    /** Mapa: posição do laser → último gameTick em que ele acertou. */
    private final java.util.Map<BlockPos, Long> recentHits = new java.util.HashMap<>();
    /** Cache do último contagem (sincronizado com cliente). */
    private int activeLasersCache = 0;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> PROGRESS_MAX;
                case 2 -> activeLasersCache;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 3; }
    };

    public CrystallizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTALLIZER.get(), pos, state);
    }

    public IItemHandler getInventory() { return inventory; }
    public int getProgress() { return progress; }

    /** Chamado por LaserEmitterBlockEntity quando o feixe acerta este bloco. */
    public void onLaserHit(BlockPos emitterPos, long gameTime) {
        recentHits.put(emitterPos, gameTime);
        setChanged();
    }

    /** Quantos lasers diferentes acertaram dentro da janela. */
    public int countActiveLasers(long currentTick) {
        int count = 0;
        var it = recentHits.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (currentTick - entry.getValue() <= HIT_WINDOW_TICKS) {
                count++;
            } else {
                it.remove();
            }
        }
        return count;
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() { super.onLoad(); lazy = LazyOptional.of(() -> inventory); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazy.invalidate(); }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.crystallizer");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new CrystallizerMenu(id, inv, this, this.data);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrystallizerBlockEntity be) {
        if (level.isClientSide) {
            // Ambient glow particle quando há progresso.
            if (be.progress > 0 && level.random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.PORTAL,
                        pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6,
                        pos.getY() + 0.5 + level.random.nextDouble() * 0.5,
                        pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6,
                        0, level.random.nextDouble() * 0.05, 0);
            }
            return;
        }

        long t = level.getGameTime();
        be.activeLasersCache = be.countActiveLasers(t);

        ItemStack input = be.inventory.getStackInSlot(SLOT_INPUT);
        ItemStack output = be.inventory.getStackInSlot(SLOT_OUTPUT);
        boolean canOutput = output.isEmpty()
                || (output.is(br.com.murilo.liberthia.registry.ModBlocks.DARK_MATTER_BLOCK.get().asItem())
                        && output.getCount() < output.getMaxStackSize());

        if (input.isEmpty() || !canOutput) {
            if (be.progress > 0) { be.progress = 0; be.setChanged(); }
            return;
        }

        if (be.activeLasersCache >= LASERS_REQUIRED) {
            be.progress++;
            if (be.progress >= PROGRESS_MAX) {
                // Preserva pureza NBT do inactive → bloco resultante
                ItemStack inputStack = be.inventory.getStackInSlot(SLOT_INPUT);
                int purity = br.com.murilo.liberthia.util.Purity.getPurity(inputStack);
                be.inventory.extractItem(SLOT_INPUT, 1, false);
                ItemStack out = new ItemStack(br.com.murilo.liberthia.registry.ModBlocks.DARK_MATTER_BLOCK.get().asItem());
                br.com.murilo.liberthia.util.Purity.setPurity(out, purity);
                be.inventory.insertItem(SLOT_OUTPUT, out, false);
                be.progress = 0;
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                            32, 0.4, 0.4, 0.4, 0.05);
                    sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                            SoundSource.BLOCKS, 1.5f, 0.6f);
                }
            }
            be.setChanged();
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        tag.put("inv", inventory.serializeNBT());
        tag.putInt("progress", progress);
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        progress = tag.getInt("progress");
    }
}
