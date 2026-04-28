package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds a single ItemStack — used as an ingredient holder for blood rituals
 * (Occultism-inspired sacrificial bowl pattern).
 */
public class BloodSacrificialBowlBlockEntity extends BlockEntity {

    private static final String NBT_ITEM = "Item";
    private ItemStack held = ItemStack.EMPTY;

    public BloodSacrificialBowlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOOD_SACRIFICIAL_BOWL.get(), pos, state);
    }

    public ItemStack getItem() {
        return held;
    }

    public boolean isEmpty() {
        return held.isEmpty();
    }

    public boolean tryPlace(ItemStack stack) {
        if (!isEmpty()) return false;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        held = copy;
        sync();
        return true;
    }

    public ItemStack takeItem() {
        ItemStack out = held;
        held = ItemStack.EMPTY;
        sync();
        return out;
    }

    /** Consumes 1 from the held stack (used during a ritual). Returns the consumed copy. */
    public ItemStack consumeOne() {
        if (held.isEmpty()) return ItemStack.EMPTY;
        ItemStack consumed = held.copy();
        consumed.setCount(1);
        held.shrink(1);
        if (held.isEmpty()) held = ItemStack.EMPTY;
        sync();
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                    8, 0.2, 0.3, 0.2, 0.02);
        }
        return consumed;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!held.isEmpty()) {
            tag.put(NBT_ITEM, held.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NBT_ITEM)) {
            held = ItemStack.of(tag.getCompound(NBT_ITEM));
        } else {
            held = ItemStack.EMPTY;
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

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        if (pkt.getTag() != null) load(pkt.getTag());
    }
}
