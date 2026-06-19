package br.com.murilo.liberthia.automation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v0.1.24 r88: <b>Spell Turret BE</b> — armazena 1 SpellParchment + casta
 * quando triggerado.
 *
 * <p>2 modos:
 * <ul>
 *   <li>{@code mode=0} (Basic) — casta apenas quando recebe pulso redstone</li>
 *   <li>{@code mode=1} (Timer) — casta a cada N ticks (timerInterval)</li>
 * </ul>
 *
 * <p>Configurável via Dominion Wand. Source consumption: 20 source por cast,
 * drenado do player mais próximo (r133: integrado via SourceConsumer).
 */
public class SpellTurretBlockEntity extends BlockEntity {

    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int mode = 0; // 0 = redstone-trigger; 1 = timer
    private int timerInterval = 40; // 2s default
    private long lastFireTick = 0;
    private boolean powered = false;

    public SpellTurretBlockEntity(BlockPos pos, BlockState state) {
        super(br.com.murilo.liberthia.registry.ModBlockEntities.SPELL_TURRET.get(), pos, state);
    }

    public ItemStack getParchment() { return items.get(0); }
    public void setParchment(ItemStack stack) { items.set(0, stack); setChanged(); }

    public int getMode() { return mode; }
    public void setMode(int mode) { this.mode = mode % 2; setChanged(); }
    public int getTimerInterval() { return timerInterval; }
    public void setTimerInterval(int ticks) { this.timerInterval = Math.max(10, ticks); setChanged(); }

    public void onPowered(boolean nowPowered) {
        if (mode == 0 && !powered && nowPowered) {
            // Rising edge — fire
            fire();
        }
        powered = nowPowered;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpellTurretBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        if (be.mode != 1) return; // só timer mode usa tick
        long now = sl.getGameTime();
        if (now - be.lastFireTick < be.timerInterval) return;
        be.fire();
        be.lastFireTick = now;
    }

    /** Source cost por cast — drenado do player mais próximo. */
    public static final int SOURCE_COST_PER_CAST = 20;

    /** Casta o spell armazenado. Por enquanto: visual placeholder + particles. */
    public void fire() {
        if (!(level instanceof ServerLevel sl)) return;
        if (items.get(0).isEmpty()) return;

        // r133: consome Source de algum player proximo. Se ninguem tem, fizzle.
        if (!SourceConsumer.tryConsume(sl, worldPosition, SOURCE_COST_PER_CAST)) {
            // Fizzle visual — smoke + low sound
            sl.sendParticles(ParticleTypes.SMOKE,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                    8, 0.3, 0.1, 0.3, 0.02);
            sl.playSound(null, worldPosition,
                    net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.4F, 1.8F);
            return;
        }

        // Spawn projétil mock — particles na direção configurada
        Direction facing = sl.getBlockState(worldPosition).hasProperty(SpellTurretBlock.FACING)
                ? sl.getBlockState(worldPosition).getValue(SpellTurretBlock.FACING) : Direction.UP;
        double cx = worldPosition.getX() + 0.5 + facing.getStepX() * 0.6;
        double cy = worldPosition.getY() + 0.5 + facing.getStepY() * 0.6;
        double cz = worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.6;

        // Trail particles forward (10 blocos)
        for (int i = 1; i <= 10; i++) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    cx + facing.getStepX() * i,
                    cy + facing.getStepY() * i,
                    cz + facing.getStepZ() * i,
                    1, 0.05, 0.05, 0.05, 0);
        }
        sl.sendParticles(ParticleTypes.GLOW,
                cx, cy, cz, 5, 0.2, 0.2, 0.2, 0.05);

        // Sound
        sl.playSound(null, worldPosition,
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.5F);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Mode", mode);
        tag.putInt("TimerInterval", timerInterval);
        tag.putLong("LastFire", lastFireTick);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        mode = tag.getInt("Mode");
        timerInterval = Math.max(10, tag.getInt("TimerInterval"));
        lastFireTick = tag.getLong("LastFire");
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
