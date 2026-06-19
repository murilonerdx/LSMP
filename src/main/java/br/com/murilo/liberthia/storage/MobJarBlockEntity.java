package br.com.murilo.liberthia.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v0.1.24 r92: <b>Mob Jar BE</b> — armazena 1 mob como display animado.
 *
 * <p>NBT contém o EntityType ID + entity full NBT pra reproduzir visual.
 * O mob no jar não tem AI ativa, apenas anima.
 */
public class MobJarBlockEntity extends BlockEntity {

    private CompoundTag storedEntityNbt = null;
    private String storedEntityType = null;
    /** Entity cliente cached pra render — null se nada armazenado. */
    private Entity cachedDisplay = null;

    public MobJarBlockEntity(BlockPos pos, BlockState state) {
        super(br.com.murilo.liberthia.registry.ModBlockEntities.MOB_JAR.get(), pos, state);
    }

    public boolean hasMob() {
        return storedEntityType != null;
    }

    public String getStoredType() {
        return storedEntityType;
    }

    /** Captura um entity vivo do mundo. Salva NBT + tipo. Retorna true se OK. */
    public boolean captureMob(Entity entity) {
        if (storedEntityType != null) return false; // já cheio
        CompoundTag nbt = new CompoundTag();
        if (!entity.save(nbt)) return false;
        // Limpa dados que não queremos no jar
        nbt.remove("Pos");
        nbt.remove("Motion");
        nbt.remove("Rotation");
        storedEntityNbt = nbt;
        storedEntityType = EntityType.getKey(entity.getType()).toString();
        cachedDisplay = null;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return true;
    }

    /** Libera o mob no mundo. */
    public Entity releaseMob() {
        if (storedEntityType == null || storedEntityNbt == null || level == null) return null;
        EntityType<?> type = EntityType.byString(storedEntityType).orElse(null);
        if (type == null) return null;
        Entity e = type.create(level);
        if (e == null) return null;
        e.load(storedEntityNbt);
        e.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5);
        level.addFreshEntity(e);
        storedEntityType = null;
        storedEntityNbt = null;
        cachedDisplay = null;
        setChanged();
        if (!level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return e;
    }

    /** Pega entity pra rendering. Cria 1 vez e cacheia. */
    public Entity getDisplayEntity() {
        if (cachedDisplay != null) return cachedDisplay;
        if (storedEntityType == null || level == null) return null;
        EntityType<?> type = EntityType.byString(storedEntityType).orElse(null);
        if (type == null) return null;
        Entity e = type.create(level);
        if (e == null) return null;
        if (storedEntityNbt != null) {
            try { e.load(storedEntityNbt); } catch (Throwable ignored) {}
        }
        cachedDisplay = e;
        return e;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (storedEntityType != null) {
            tag.putString("EntityType", storedEntityType);
            tag.put("EntityNbt", storedEntityNbt);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("EntityType")) {
            storedEntityType = tag.getString("EntityType");
            storedEntityNbt = tag.getCompound("EntityNbt");
            cachedDisplay = null;
        } else {
            storedEntityType = null;
            storedEntityNbt = null;
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
