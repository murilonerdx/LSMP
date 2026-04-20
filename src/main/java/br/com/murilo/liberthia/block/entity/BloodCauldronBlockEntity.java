package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Blood Cauldron — holds up to 4 ingredients. Needs Blood fluid below (or blood flag).
 * Simple crafting: right-click with ingredient to add; right-click empty hand with complete
 * recipe + boiling → consumes ingredients and drops output above.
 *
 * Recipes (hardcoded):
 *   1. [CONGEALED_BLOOD x4, PRIEST_SIGIL] → SANGUINE_ESSENCE x2
 *   2. [ROTTEN_FLESH x3, CONGEALED_BLOOD x2, CHALK] → BLOOD_CURE_PILL x1
 *   3. [GLASS x3, IRON_NUGGET] → BLOOD_VIAL x3
 */
public class BloodCauldronBlockEntity extends BlockEntity {

    public static final int MAX_INGREDIENTS = 8;
    private final NonNullList<ItemStack> ingredients = NonNullList.create();

    public BloodCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOOD_CAULDRON.get(), pos, state);
    }

    public boolean addIngredient(ItemStack stack) {
        if (ingredients.size() >= MAX_INGREDIENTS) return false;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        ingredients.add(copy);
        setChangedAndSync();
        return true;
    }

    public List<ItemStack> getIngredients() {
        return ingredients;
    }

    /** Returns output item if ingredients match a known recipe, else ItemStack.EMPTY. */
    public ItemStack tryCraft() {
        // Tally ingredients
        int congealed = 0, sigil = 0, rotten = 0, chalk = 0, glass = 0, nugget = 0;
        for (ItemStack s : ingredients) {
            if (s.is(ModItems.CONGEALED_BLOOD.get())) congealed++;
            else if (s.is(ModItems.PRIEST_SIGIL.get())) sigil++;
            else if (s.is(Items.ROTTEN_FLESH)) rotten++;
            else if (s.is(ModItems.CHALK.get())) chalk++;
            else if (s.is(Items.GLASS)) glass++;
            else if (s.is(Items.IRON_NUGGET)) nugget++;
            else return ItemStack.EMPTY; // unknown ingredient fails
        }
        int total = ingredients.size();

        if (congealed == 4 && sigil == 1 && total == 5) {
            return new ItemStack(ModItems.SANGUINE_ESSENCE.get(), 2);
        }
        if (rotten == 3 && congealed == 2 && chalk == 1 && total == 6) {
            return new ItemStack(ModItems.BLOOD_CURE_PILL.get(), 1);
        }
        if (glass == 3 && nugget == 1 && total == 4) {
            return new ItemStack(ModItems.BLOOD_VIAL.get(), 3);
        }
        return ItemStack.EMPTY;
    }

    public void produce(ItemStack output) {
        if (level == null) return;
        ingredients.clear();
        ItemEntity drop = new ItemEntity(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                output);
        drop.setDeltaMovement(0, 0.15, 0);
        level.addFreshEntity(drop);
        level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 0.8F);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                    20, 0.25, 0.25, 0.25, 0.05);
        }
        setChangedAndSync();
    }

    public void dropAll() {
        if (level == null) return;
        for (ItemStack s : ingredients) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), s);
        }
        ingredients.clear();
        setChangedAndSync();
    }

    public boolean isBoiling() {
        if (level == null) return false;
        BlockState below = level.getBlockState(worldPosition.below());
        return below.is(ModBlocks.BLOOD_FLUID_BLOCK.get());
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T be) {
        if (!(be instanceof BloodCauldronBlockEntity cauldron)) return;
        if (!(level instanceof ServerLevel sl)) return;
        if (cauldron.ingredients.isEmpty()) return;
        if (!cauldron.isBoiling()) return;

        // Bubble particles
        if (level.getGameTime() % 10 == 0) {
            sl.sendParticles(ParticleTypes.SPLASH,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    3, 0.2, 0.0, 0.2, 0.02);
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (ItemStack s : ingredients) {
            list.add(s.save(new CompoundTag()));
        }
        tag.put("Ingredients", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ingredients.clear();
        net.minecraft.nbt.ListTag list = tag.getList("Ingredients", 10);
        for (int i = 0; i < list.size(); i++) {
            ingredients.add(ItemStack.of(list.getCompound(i)));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
