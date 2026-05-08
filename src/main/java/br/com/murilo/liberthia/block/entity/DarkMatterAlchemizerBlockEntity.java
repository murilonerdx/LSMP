package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.DarkMatterAlchemizerMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
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

import java.util.List;

public class DarkMatterAlchemizerBlockEntity extends BlockEntity implements MenuProvider {

    /** Pool of rare items the machine may produce on a successful brew (60%). */
    private static final List<ItemStack> RARE_LOOT = List.of(
            new ItemStack(Items.DIAMOND_BLOCK),
            new ItemStack(Items.EMERALD_BLOCK),
            new ItemStack(Items.NETHERITE_BLOCK),
            new ItemStack(Items.ANCIENT_DEBRIS),
            new ItemStack(Items.BEACON),
            new ItemStack(Items.CONDUIT),
            new ItemStack(Items.NETHER_STAR),
            new ItemStack(Items.DRAGON_EGG),
            new ItemStack(Items.ELYTRA),
            new ItemStack(Items.TOTEM_OF_UNDYING)
    );

    /** 60% success / 40% boom. Tweak here to rebalance. */
    private static final float SUCCESS_CHANCE = 0.60F;
    private static final float EXPLOSION_POWER = 4.0F;
    private static final int BREW_DURATION = 200;

    // Slot 0: dark matter block, Slot 1: lava bucket, Slot 2: output
    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> stack.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem());
                case 1 -> stack.is(Items.LAVA_BUCKET);
                case 2 -> false;
                default -> false;
            };
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private int progress = 0;
    private final int maxProgress = BREW_DURATION;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) progress = value;
        }
        @Override public int getCount() { return 2; }
    };

    public DarkMatterAlchemizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DARK_MATTER_ALCHEMIZER.get(), pos, state);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.dark_matter_alchemizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player player) {
        return new DarkMatterAlchemizerMenu(containerId, inv, this, this.data);
    }

    public IItemHandler getItemHandler() { return inventory; }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override public void onLoad() { super.onLoad(); lazyItemHandler = LazyOptional.of(() -> inventory); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyItemHandler.invalidate(); }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(this.level, this.worldPosition, c);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DarkMatterAlchemizerBlockEntity entity) {
        if (level.isClientSide) return;
        if (entity.hasRecipe()) {
            entity.progress++;
            entity.setChanged();
            // Light atmospheric particles while brewing.
            if (entity.progress % 10 == 0 && level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.PORTAL,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        4, 0.3, 0.2, 0.3, 0.05);
            }
            if (entity.progress >= entity.maxProgress) {
                entity.completeProcess();
            }
        } else if (entity.progress > 0) {
            entity.progress = 0;
            entity.setChanged();
        }
    }

    private boolean hasRecipe() {
        ItemStack input = inventory.getStackInSlot(0);
        ItemStack catalyst = inventory.getStackInSlot(1);
        if (input.isEmpty() || catalyst.isEmpty()) return false;
        if (!input.is(ModBlocks.DARK_MATTER_BLOCK.get().asItem())) return false;
        if (!catalyst.is(Items.LAVA_BUCKET)) return false;
        // Output slot must be empty OR the result must stack — but result is
        // random, so we require empty output for safety.
        return inventory.getStackInSlot(2).isEmpty();
    }

    private void completeProcess() {
        progress = 0;
        if (!(level instanceof ServerLevel sl)) return;

        // Always consume the dark matter block.
        inventory.extractItem(0, 1, false);
        // Replace the lava bucket with an empty bucket (vanilla furnace pattern).
        inventory.setStackInSlot(1, new ItemStack(Items.BUCKET));

        if (sl.random.nextFloat() < SUCCESS_CHANCE) {
            // 60% — generate a random rare item.
            ItemStack reward = RARE_LOOT.get(sl.random.nextInt(RARE_LOOT.size())).copy();
            inventory.setStackInSlot(2, reward);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                    20, 0.4, 0.4, 0.4, 0.3);
            sl.sendParticles(ParticleTypes.END_ROD,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5,
                    16, 0.3, 0.3, 0.3, 0.1);
            sl.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP,
                    SoundSource.BLOCKS, 1.0F, 1.4F);
        } else {
            // 40% — detonate. Inputs are gone; the block itself stays so the
            // player still has the machine afterward (just empty).
            sl.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS, 2.0F, 0.7F);
            sl.explode(null,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.6, worldPosition.getZ() + 0.5,
                    EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", inventory.serializeNBT());
        tag.putInt("progress", progress);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) inventory.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("progress");
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
