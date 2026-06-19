package br.com.murilo.liberthia.magic.conduit;

import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.observation.source.SourceData;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * v0.1.149 r117: <b>SourceTransmuterBlockEntity</b> — fica no Overworld.
 * Bind via right-click do player com Spiritual Link (or empty hand at Spirit
 * Conduit position record). Mantém pos do conduit em NBT.
 *
 * <p>Quando player encosta (1.5b radius), drena charge do conduit dimensional
 * → converte 1 charge = 1 Source no player. Limita 50 source por chamada
 * pra ter feel gradual, não instant.
 *
 * <h2>Bind via SpiritualLink</h2>
 * Player que tem SpiritualLink (item r19) marcado com pos de um SpiritConduit
 * pode bater right-click no Transmuter pra fazer o bind. Daí em diante o
 * Transmuter sabe qual conduit drenar.
 *
 * <p>Original code.
 */
public class SourceTransmuterBlockEntity extends BlockEntity {

    public static final String NBT_BOUND_POS = "bound_conduit_pos";

    private BlockPos boundConduit = null;
    private int tickCounter = 0;

    public SourceTransmuterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOURCE_TRANSMUTER.get(), pos, state);
    }

    public BlockPos getBoundConduit() { return boundConduit; }
    public void setBoundConduit(BlockPos p) { this.boundConduit = p; setChanged(); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SourceTransmuterBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        be.tickCounter++;
        if (be.boundConduit == null) return;
        if (be.tickCounter % 40 != 0) return;  // check a cada 2s

        // Buscar players próximos
        var players = sl.getEntitiesOfClass(Player.class,
                new net.minecraft.world.phys.AABB(pos).inflate(2));
        if (players.isEmpty()) return;

        // Pega o conduit no Spirit World
        MinecraftServer server = sl.getServer();
        ServerLevel spiritLevel = server.getLevel(SpiritDimension.SPIRIT_WORLD);
        if (spiritLevel == null) return;
        // Force load chunk
        BlockEntity conduitBe = spiritLevel.getBlockEntity(be.boundConduit);
        if (!(conduitBe instanceof SpiritConduitBlockEntity conduit)) return;
        if (conduit.getCharge() <= 0) return;

        // Drena até 25 charge por tick batch
        int drained = conduit.drain(25);
        if (drained > 0) {
            for (Player p : players) {
                if (!(p instanceof ServerPlayer sp)) continue;
                int max = SourceData.getMax(sp);
                int cur = SourceData.get(sp);
                int give = Math.min(drained, max - cur);
                if (give > 0) {
                    SourceData.add(sp, give);
                    drained -= give;
                }
                if (drained <= 0) break;
            }
            // Devolve o excedente
            if (drained > 0) conduit.setCharge(conduit.getCharge() + drained);

            // VFX: feixe vertical de partículas
            for (int i = 0; i < 8; i++) {
                sl.sendParticles(ParticleTypes.ENCHANT,
                        pos.getX() + 0.5, pos.getY() + 0.5 + i * 0.2, pos.getZ() + 0.5,
                        1, 0.1, 0.05, 0.1, 0.02);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (boundConduit != null) {
            tag.putLong(NBT_BOUND_POS, boundConduit.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NBT_BOUND_POS)) {
            this.boundConduit = BlockPos.of(tag.getLong(NBT_BOUND_POS));
        }
    }
}
