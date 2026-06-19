package br.com.murilo.liberthia.atmospheric;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * v0.1.24 r89: <b>Weave blocks</b> — 4 tipos de blocos camuflagem com física
 * alterada.
 *
 * <h2>Modos</h2>
 * <ul>
 *   <li>{@code MIRROR} — bloco sólido, sem collision pra mob (pasthrough fantasma)</li>
 *   <li>{@code SKY} — invisible occluder, vê céu mas não ar entre</li>
 *   <li>{@code GHOST} — sem collision pra player + mob (intangível total)</li>
 *   <li>{@code FALSE} — visualmente sólido, mas player atravessa</li>
 * </ul>
 */
public class WeaveBlock extends Block {

    public enum Mode {
        MIRROR(false, true),  // pasthrough mob, mas player NÃO atravessa
        SKY(true, true),       // visual sólido, mas céu passa por trás
        GHOST(false, false),   // intangível total
        FALSE(false, true);    // parece sólido, sem collision pra player

        public final boolean playerSolid;
        public final boolean visible;

        Mode(boolean playerSolid, boolean visible) {
            this.playerSolid = playerSolid;
            this.visible = visible;
        }
    }

    public final Mode mode;

    public WeaveBlock(BlockBehaviour.Properties props, Mode mode) {
        super(props);
        this.mode = mode;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        if (!mode.playerSolid) return Shapes.empty();
        return super.getCollisionShape(s, g, p, c);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState s, BlockGetter g, BlockPos p) {
        return Shapes.empty(); // não bloqueia luz/skylight
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return mode == Mode.SKY || mode == Mode.GHOST;
    }

    @Override
    public float getShadeBrightness(BlockState s, BlockGetter g, BlockPos p) {
        return 1.0F; // sem sombra
    }
}
