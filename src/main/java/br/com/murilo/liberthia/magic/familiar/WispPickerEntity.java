package br.com.murilo.liberthia.magic.familiar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/**
 * v0.1.24 r95: <b>Wisp Picker</b> — familiar que coleta items dropped num
 * raio e deposita num chest linkado.
 *
 * <p>Linked via Dominion Wand (futuro) ou shift+right-click em chest.
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>A cada 20 ticks, scaneia ItemEntity num raio de 12</li>
 *   <li>Coleta até carregar 4 stacks (inventário interno)</li>
 *   <li>Quando cheio, voa até chest linkado e deposita</li>
 *   <li>Sem AI agressiva — flee de damage</li>
 * </ul>
 */
public class WispPickerEntity extends PathfinderMob {

    private static final EntityDataAccessor<Optional<BlockPos>> LINKED_CHEST =
            SynchedEntityData.defineId(WispPickerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    private final ItemStack[] internalInventory = new ItemStack[4];

    public WispPickerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        for (int i = 0; i < internalInventory.length; i++) internalInventory[i] = ItemStack.EMPTY;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LINKED_CHEST, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    public void setLinkedChest(BlockPos pos) {
        this.entityData.set(LINKED_CHEST, Optional.ofNullable(pos));
    }
    public Optional<BlockPos> getLinkedChest() {
        return this.entityData.get(LINKED_CHEST);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;

        // Particles flutuantes
        if (tickCount % 8 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    getX(), getY() + 0.5, getZ(),
                    2, 0.2, 0.2, 0.2, 0.01);
        }

        // (1) Pickup nearby items
        if (tickCount % 20 == 0) {
            var items = level().getEntitiesOfClass(ItemEntity.class,
                    getBoundingBox().inflate(12));
            for (ItemEntity ie : items) {
                if (ie.hasPickUpDelay()) continue;
                if (!tryInsertItem(ie.getItem())) continue;
                ie.discard();
                if (isInventoryFull()) break;
            }
        }

        // (2) Deposit em chest linkado quando cheio
        if (tickCount % 40 == 0 && isInventoryFull()) {
            var opt = getLinkedChest();
            if (opt.isPresent()) {
                BlockPos chest = opt.get();
                if (level().isLoaded(chest) && distanceToSqr(chest.getX(), chest.getY(), chest.getZ()) < 64) {
                    BlockEntity be = level().getBlockEntity(chest);
                    if (be instanceof Container container) {
                        depositToContainer(container);
                    }
                } else if (opt.isPresent()) {
                    // Voa até o chest
                    getNavigation().moveTo(chest.getX(), chest.getY(), chest.getZ(), 1.0);
                }
            }
        }
    }

    private boolean tryInsertItem(ItemStack stack) {
        for (int i = 0; i < internalInventory.length; i++) {
            if (internalInventory[i].isEmpty()) {
                internalInventory[i] = stack.copy();
                return true;
            }
            if (ItemStack.isSameItemSameTags(internalInventory[i], stack)
                    && internalInventory[i].getCount() + stack.getCount() <= internalInventory[i].getMaxStackSize()) {
                internalInventory[i].grow(stack.getCount());
                return true;
            }
        }
        return false;
    }

    private boolean isInventoryFull() {
        for (ItemStack s : internalInventory) if (s.isEmpty()) return false;
        return true;
    }

    private void depositToContainer(Container c) {
        for (int i = 0; i < internalInventory.length; i++) {
            if (internalInventory[i].isEmpty()) continue;
            for (int slot = 0; slot < c.getContainerSize(); slot++) {
                ItemStack existing = c.getItem(slot);
                if (existing.isEmpty()) {
                    c.setItem(slot, internalInventory[i].copy());
                    internalInventory[i] = ItemStack.EMPTY;
                    break;
                } else if (ItemStack.isSameItemSameTags(existing, internalInventory[i])
                        && existing.getCount() + internalInventory[i].getCount() <= existing.getMaxStackSize()) {
                    existing.grow(internalInventory[i].getCount());
                    internalInventory[i] = ItemStack.EMPTY;
                    break;
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        var opt = getLinkedChest();
        if (opt.isPresent()) {
            BlockPos p = opt.get();
            tag.putInt("ChestX", p.getX());
            tag.putInt("ChestY", p.getY());
            tag.putInt("ChestZ", p.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ChestX")) {
            setLinkedChest(new BlockPos(tag.getInt("ChestX"), tag.getInt("ChestY"), tag.getInt("ChestZ")));
        }
    }
}
