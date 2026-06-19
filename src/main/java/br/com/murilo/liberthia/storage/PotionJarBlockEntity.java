package br.com.murilo.liberthia.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v0.1.24 r92: <b>Potion Jar BE</b> — storage de 10000 mB de UMA potion.
 *
 * <p>Use: right-click com potion → +250mB (consume potion → glass bottle).
 * Right-click com glass bottle vazio + jar cheio → -250mB (recebe potion).
 */
public class PotionJarBlockEntity extends BlockEntity {

    public static final int CAPACITY = 10000;
    public static final int POTION_AMOUNT = 250;

    // r173: além de potions, o jarro agora guarda SOURCE (líquido roxo mágico).
    // É o uso principal — o BER renderiza o líquido animado enchendo por dentro.
    public static final int SOURCE_CAPACITY = 10000;
    public static final int SOURCE_STEP = 50;
    private int source = 0;

    private Potion storedPotion = Potions.EMPTY;
    private int amount = 0;

    public PotionJarBlockEntity(BlockPos pos, BlockState state) {
        super(br.com.murilo.liberthia.registry.ModBlockEntities.POTION_JAR.get(), pos, state);
    }

    public Potion getPotion() { return storedPotion; }
    public int getAmount() { return amount; }

    // --- Source API (r173) ---
    public int getSource() { return source; }
    public int getSourceCapacity() { return SOURCE_CAPACITY; }
    public int getSourceRoom() { return Math.max(0, SOURCE_CAPACITY - source); }

    /** Adiciona Source. Returns quanto foi aceito (0 se cheio). */
    public int addSource(int amt) {
        int accepted = Math.min(amt, getSourceRoom());
        if (accepted > 0) {
            source += accepted;
            syncAndSave();
        }
        return accepted;
    }

    /** Remove Source. Returns quanto foi extraído (0 se vazio). */
    public int drawSource(int amt) {
        int taken = Math.min(amt, source);
        if (taken > 0) {
            source -= taken;
            syncAndSave();
        }
        return taken;
    }

    private void syncAndSave() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean addPotion(Potion p) {
        if (amount >= CAPACITY) return false;
        if (storedPotion != Potions.EMPTY && storedPotion != p) return false;
        storedPotion = p;
        amount = Math.min(CAPACITY, amount + POTION_AMOUNT);
        setChanged();
        return true;
    }

    public Potion drawPotion() {
        if (amount < POTION_AMOUNT) return null;
        Potion result = storedPotion;
        amount -= POTION_AMOUNT;
        if (amount <= 0) {
            storedPotion = Potions.EMPTY;
            amount = 0;
        }
        setChanged();
        return result;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Potion", BuiltInRegistries.POTION.getKey(storedPotion).toString());
        tag.putInt("Amount", amount);
        tag.putInt("Source", source);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ResourceLocation rl = ResourceLocation.tryParse(tag.getString("Potion"));
        if (rl != null) {
            storedPotion = BuiltInRegistries.POTION.get(rl);
        }
        amount = tag.getInt("Amount");
        source = tag.getInt("Source");
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
