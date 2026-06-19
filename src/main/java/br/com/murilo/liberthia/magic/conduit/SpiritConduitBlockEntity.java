package br.com.murilo.liberthia.magic.conduit;

import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v0.1.149 r117: <b>SpiritConduitBlockEntity</b> — fica no Spirit World
 * acumulando "Spirit Charge" do ambiente. Cada conduit acumula até 1000
 * charge max. Pareado com {@link SourceTransmuterBlockEntity} via NBT pos
 * pra transferir overworld→drain.
 *
 * <h2>Tick logic</h2>
 * <ul>
 *   <li>A cada 20t: +5 charge SE estiver no Spirit World</li>
 *   <li>A cada 40t (no Spirit): +10 charge se houver Whisperwood tree em 5b</li>
 *   <li>Spawn 1 partícula END_ROD por tick (visual de acumulação)</li>
 * </ul>
 *
 * <p>Original code.
 */
public class SpiritConduitBlockEntity extends BlockEntity {

    public static final int MAX_CHARGE = 1000;
    public static final String NBT_CHARGE = "spirit_charge";

    private int charge = 0;
    private int tickCounter = 0;

    public SpiritConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPIRIT_CONDUIT.get(), pos, state);
    }

    public int getCharge() { return charge; }
    public void setCharge(int c) { this.charge = Math.max(0, Math.min(MAX_CHARGE, c)); setChanged(); }
    public int drain(int amount) {
        int take = Math.min(charge, amount);
        charge -= take;
        setChanged();
        return take;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpiritConduitBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        be.tickCounter++;

        // Só acumula no Spirit World
        boolean inSpirit = level.dimension().equals(SpiritDimension.SPIRIT_WORLD);
        if (!inSpirit) return;

        // +5 charge a cada 20t
        if (be.tickCounter % 20 == 0 && be.charge < MAX_CHARGE) {
            be.charge = Math.min(MAX_CHARGE, be.charge + 5);
            be.setChanged();
        }

        // Visual: partícula END_ROD subindo do bloco
        if (be.tickCounter % 4 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    1, 0.05, 0.1, 0.05, 0.01);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(NBT_CHARGE, charge);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.charge = tag.getInt(NBT_CHARGE);
    }
}
