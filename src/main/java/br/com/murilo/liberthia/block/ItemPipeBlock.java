package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.ItemPipeBlockEntity;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

/**
 * Item Pipe block — Pipez-style item transport with per-side modes and filters.
 *
 * <p>Interaction model:
 * <ul>
 *   <li><b>Empty hand right-click on a face</b> — cycle that face's
 *       {@link ItemPipeBlockEntity.Mode}.</li>
 *   <li><b>Item right-click on a face</b> — add the held item type to that
 *       face's whitelist filter.</li>
 *   <li><b>Sneak + empty hand right-click on a face</b> — clear that face's
 *       filter.</li>
 *   <li><b>Sneak + item right-click</b> — cycle the pipe's
 *       {@link ItemPipeBlockEntity.Speed}.</li>
 * </ul>
 *
 * <p>Connection sides auto-update based on neighbouring pipes and
 * {@code ITEM_HANDLER}-exposing blocks, identical to {@link EnergyCableBlock}.
 */
public class ItemPipeBlock extends BaseEntityBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;
    public static final BooleanProperty UP    = BlockStateProperties.UP;
    public static final BooleanProperty DOWN  = BlockStateProperties.DOWN;

    private static final VoxelShape CORE    = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape S_NORTH = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape S_SOUTH = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape S_WEST  = Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape S_EAST  = Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape S_DOWN  = Block.box(5, 0, 5, 11, 5, 11);
    private static final VoxelShape S_UP    = Block.box(5, 11, 5, 11, 16, 11);

    public ItemPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST,  false).setValue(WEST,  false)
                .setValue(UP,    false).setValue(DOWN,  false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public boolean propagatesSkylightDown(BlockState s, BlockGetter g, BlockPos p) { return true; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter g, BlockPos p, CollisionContext c) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, S_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, S_SOUTH);
        if (state.getValue(EAST))  shape = Shapes.or(shape, S_EAST);
        if (state.getValue(WEST))  shape = Shapes.or(shape, S_WEST);
        if (state.getValue(UP))    shape = Shapes.or(shape, S_UP);
        if (state.getValue(DOWN))  shape = Shapes.or(shape, S_DOWN);
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return getShape(s, g, p, c);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return computeConnections(super.defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(propertyFor(dir), shouldConnect(level, pos, dir));
    }

    /**
     * Auto-config no momento da colocação:
     * - Se EXATAMENTE 1 inventário vizinho → essa face vira EXTRACT.
     * - Se 2+ inventários vizinhos → primeiro vira EXTRACT, restante DEFAULT
     *   (user pode cyclar manualmente depois).
     * - Se 0 inventários (só pipes ou ar) → tudo DEFAULT.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        if (!(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe)) return;

        Direction firstInv = null;
        int invCount = 0;
        for (Direction d : Direction.values()) {
            BlockEntity nbe = level.getBlockEntity(pos.relative(d));
            if (nbe == null) continue;
            if (nbe instanceof ItemPipeBlockEntity) continue; // outro pipe não conta
            if (nbe.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).isPresent()) {
                if (firstInv == null) firstInv = d;
                invCount++;
            }
        }
        if (firstInv != null) {
            pipe.setMode(firstInv, ItemPipeBlockEntity.Mode.EXTRACT);
            if (placer instanceof Player p) {
                String msg = invCount == 1
                        ? "→ Face " + firstInv.getName() + " auto-configurada como EXTRACT"
                        : "→ Face " + firstInv.getName() + " = EXTRACT (cycle outras manual se quiser)";
                p.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.GOLD), false);
            }
        }
    }

    private static BooleanProperty propertyFor(Direction d) {
        return switch (d) {
            case NORTH -> NORTH; case SOUTH -> SOUTH;
            case EAST -> EAST;   case WEST -> WEST;
            case UP -> UP;       case DOWN -> DOWN;
        };
    }

    private BlockState computeConnections(BlockState base, LevelAccessor level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            base = base.setValue(propertyFor(d), shouldConnect(level, pos, d));
        }
        return base;
    }

    /** Connect to other pipes (block check) or to anything exposing ITEM_HANDLER.
     *  Honors the per-face DISABLED mode on both this pipe and the neighbour pipe. */
    private boolean shouldConnect(LevelAccessor level, BlockPos pos, Direction dir) {
        // Nossa face desligada → não conecta visualmente nem por capabilities
        BlockEntity ourBe = level.getBlockEntity(pos);
        if (ourBe instanceof ItemPipeBlockEntity ourPipe
                && ourPipe.getMode(dir) == ItemPipeBlockEntity.Mode.DISABLED) {
            return false;
        }
        BlockPos npos = pos.relative(dir);
        if (level.getBlockState(npos).getBlock() instanceof ItemPipeBlock) {
            // Pipe vizinho: a face DELE virada pra nós (dir.getOpposite()) precisa não estar DISABLED
            BlockEntity nbe = level.getBlockEntity(npos);
            if (nbe instanceof ItemPipeBlockEntity npipe
                    && npipe.getMode(dir.getOpposite()) == ItemPipeBlockEntity.Mode.DISABLED) {
                return false;
            }
            return true;
        }
        BlockEntity nbe = level.getBlockEntity(npos);
        if (nbe == null) return false;
        return nbe.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).isPresent();
    }

    /**
     * Recomputa propriedades visuais (NORTH/SOUTH/...) do pipe. Chamado pelo
     * BlockEntity quando o jogador cicla o modo de uma face — assim o braço
     * visual aparece/some na hora.
     */
    public static void recomputeConnections(LevelAccessor level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof ItemPipeBlock pipe)) return;
        BlockState newState = pipe.computeConnections(pipe.defaultBlockState(), level, pos);
        if (newState != s && level instanceof Level lvl) {
            lvl.setBlock(pos, newState, 3);
        }
    }

    // ---------------------------------------------------------------- interaction

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe))
            return InteractionResult.PASS;

        Direction face = hit.getDirection();
        ItemStack held = player.getItemInHand(hand);

        // Sneak + held item → cycle pipe speed (whole-pipe setting).
        if (player.isShiftKeyDown() && !held.isEmpty()) {
            ItemPipeBlockEntity.Speed s = pipe.cycleSpeed();
            player.displayClientMessage(Component.literal("Velocidade: " + s.name())
                    .withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }

        // Sneak + empty hand → MOSTRA STATUS de todas as 6 faces no chat.
        // Útil pra debugar setups que parecem não funcionar.
        if (player.isShiftKeyDown() && held.isEmpty()) {
            showStatus(pipe, player, level, pos);
            return InteractionResult.CONSUME;
        }

        // Held item → add to filter.
        if (!held.isEmpty()) {
            boolean added = pipe.addFilter(face, held);
            player.displayClientMessage(Component.literal(added
                            ? "Filtro + " + held.getHoverName().getString() + " (" + face.getName() + ")"
                            : "Filtro cheio (" + face.getName() + ")")
                    .withStyle(added ? ChatFormatting.GREEN : ChatFormatting.RED), true);

            // AUTO-PROMOTE: se adicionou filtro numa face DEFAULT que TEM
            // inventário vizinho, cicla automaticamente pra EXTRACT. Match
            // direto com o mental model do user: "coloquei filtro = puxa".
            if (added && pipe.getMode(face) == ItemPipeBlockEntity.Mode.DEFAULT) {
                BlockEntity nbe = level.getBlockEntity(pos.relative(face));
                if (nbe != null && nbe.getCapability(ForgeCapabilities.ITEM_HANDLER, face.getOpposite()).isPresent()) {
                    pipe.setMode(face, ItemPipeBlockEntity.Mode.EXTRACT);
                    player.displayClientMessage(Component.literal(
                            "→ Face " + face.getName() + " auto-configurada como EXTRACT")
                            .withStyle(ChatFormatting.GOLD), false);
                } else {
                    player.displayClientMessage(Component.literal(
                            "⚠ Face " + face.getName() + " não tem inventário — filtro inativo nessa face")
                            .withStyle(ChatFormatting.YELLOW), false);
                }
            }
            return InteractionResult.CONSUME;
        }

        // Empty hand → cycle mode on this face.
        ItemPipeBlockEntity.Mode m = pipe.cycleMode(face);
        ChatFormatting color = switch (m) {
            case EXTRACT  -> ChatFormatting.GOLD;
            case INSERT   -> ChatFormatting.GREEN;
            case DISABLED -> ChatFormatting.RED;
            case DEFAULT  -> ChatFormatting.GRAY;
        };
        player.displayClientMessage(Component.literal(face.getName() + ": " + m.name())
                .withStyle(color), true);

        // Sanity check imediato em EXTRACT — checa se a face tem inventário válido
        if (m == ItemPipeBlockEntity.Mode.EXTRACT) {
            BlockEntity neighborBE = level.getBlockEntity(pos.relative(face));
            if (neighborBE == null) {
                player.displayClientMessage(Component.literal(
                        "⚠ Face " + face.getName() + " sem bloco de inventário. Coloque o baú/máquina TOCANDO essa face.")
                        .withStyle(ChatFormatting.YELLOW), false);
            } else if (!neighborBE.getCapability(ForgeCapabilities.ITEM_HANDLER, face.getOpposite()).isPresent()) {
                player.displayClientMessage(Component.literal(
                        "⚠ Bloco vizinho (" + neighborBE.getClass().getSimpleName()
                                + ") não expõe inventário pela face " + face.getOpposite().getName() + ".")
                        .withStyle(ChatFormatting.YELLOW), false);
            } else {
                player.displayClientMessage(Component.literal(
                        "✓ Inventário detectado em " + face.getName() + " — extração ativa.")
                        .withStyle(ChatFormatting.AQUA), false);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** Mostra status de cada face no chat com diagnóstico de vizinho. */
    private static void showStatus(ItemPipeBlockEntity pipe, Player player, Level level, BlockPos pos) {
        player.displayClientMessage(Component.literal("§6═══ Pipe Status ═══"), false);
        for (Direction d : Direction.values()) {
            ItemPipeBlockEntity.Mode m = pipe.getMode(d);
            ItemStack[] f = pipe.getFilter(d);
            StringBuilder filterStr = new StringBuilder();
            int nonEmpty = 0;
            for (ItemStack fs : f) {
                if (!fs.isEmpty()) {
                    if (nonEmpty > 0) filterStr.append(", ");
                    filterStr.append(fs.getHoverName().getString());
                    nonEmpty++;
                }
            }
            String filterText = nonEmpty == 0 ? "§7vazio (passa-tudo)§r" : filterStr.toString();
            ChatFormatting color = switch (m) {
                case EXTRACT  -> ChatFormatting.GOLD;
                case INSERT   -> ChatFormatting.GREEN;
                case DISABLED -> ChatFormatting.RED;
                case DEFAULT  -> ChatFormatting.GRAY;
            };

            // Diagnóstico do vizinho da face — vital pra entender por que não funciona
            BlockEntity nbe = level.getBlockEntity(pos.relative(d));
            String neighborInfo;
            boolean broken = false;
            if (nbe == null) {
                neighborInfo = "§8(ar)§r";
                if (m == ItemPipeBlockEntity.Mode.EXTRACT || m == ItemPipeBlockEntity.Mode.INSERT) broken = true;
            } else if (nbe instanceof ItemPipeBlockEntity) {
                neighborInfo = "§b[pipe]§r";
            } else if (nbe.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).isPresent()) {
                neighborInfo = "§a[" + nbe.getClass().getSimpleName().replace("BlockEntity","") + "]§r";
            } else {
                neighborInfo = "§c[" + nbe.getClass().getSimpleName().replace("BlockEntity","") + " sem inv]§r";
                if (m == ItemPipeBlockEntity.Mode.EXTRACT || m == ItemPipeBlockEntity.Mode.INSERT) broken = true;
            }

            String prefix = broken ? "§c❌ " : "  ";
            player.displayClientMessage(
                    Component.literal(prefix + d.getName().toUpperCase() + ": " + m.name() + " " + neighborInfo)
                            .withStyle(color)
                            .append(Component.literal(" | filtro: " + filterText)
                                    .withStyle(ChatFormatting.WHITE)),
                    false);
        }
        player.displayClientMessage(
                Component.literal("Tipo: " + pipe.getPipeType().name()
                        + " | Vel: " + pipe.getSpeed().name())
                        .withStyle(ChatFormatting.AQUA), false);
        player.displayClientMessage(
                Component.literal("§7Legenda: §6EXTRACT§7=puxa do vizinho · §aINSERT§7=manda pro vizinho · §7DEFAULT§7=passa · §cDISABLED§7=corta. §c❌§7 = setup quebrado."),
                false);
    }

    // ---------------------------------------------------------------- BE wiring

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.ITEM_PIPE.get(),
                (lvl, pos, st, be) -> ItemPipeBlockEntity.tick(lvl, pos, st, be));
    }
}
