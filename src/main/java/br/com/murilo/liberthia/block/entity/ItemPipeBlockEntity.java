package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pipez-style item transport pipe.
 *
 * <p>Per-side state stored on the BE:
 * <ul>
 *   <li>{@link Mode} — DISABLED (no connection), EXTRACT (pull from neighbour),
 *       INSERT (push to neighbour), DEFAULT (passive — items can pass through
 *       but this side neither pulls nor offers).</li>
 *   <li>{@code filter[6][5]} — up to 5 whitelist items per side. If any slot
 *       is non-empty, only matching item types may be pulled or inserted via
 *       that side.</li>
 * </ul>
 *
 * <p>Per-pipe (whole BE) state:
 * <ul>
 *   <li>{@link Speed} — SLOW (1 item / 20 ticks), FAST (4 items / 20 ticks),
 *       STACK (whole stack / 20 ticks).</li>
 * </ul>
 *
 * <p>Routing: every {@code OPERATION_PERIOD} ticks, every EXTRACT side pulls
 * up to {@code itemsPerOperation()} items. The pulled stack walks the pipe
 * network via BFS until it lands on an INSERT-mode side whose neighbour
 * accepts it. If nowhere accepts, the items return to the source.
 */
public class ItemPipeBlockEntity extends BlockEntity {

    public enum Mode { DEFAULT, EXTRACT, INSERT, DISABLED }
    public enum Speed { SLOW, FAST, STACK }

    /**
     * Modo do filtro por face — define se o filtro é positivo (== whitelist:
     * passa SÓ os listados) ou negativo (!= blacklist: passa TUDO MENOS os
     * listados).
     *
     * <p>Default é WHITELIST (manter compat com setups antigos onde adicionar
     * filtro implicava "deixar passar só isso"). Quando blank (sem items), os
     * 2 modos têm comportamento idêntico — passa tudo.
     *
     * <p><b>BLACKLIST use case</b>: "puxo cobblestone do baú menos diamond pickaxes
     * que tão lá dentro" → adiciona diamond_pickaxe no filtro + seta modo BLACKLIST.
     */
    public enum FilterMode { WHITELIST, BLACKLIST }

    /**
     * Tipo de transporte do pipe (whole-pipe).
     * <ul>
     *   <li>UNIVERSAL: todos os items (default)</li>
     *   <li>ITEMS_ONLY: itens não-bloco (não BlockItem) e não buckets</li>
     *   <li>BLOCKS_ONLY: apenas {@code BlockItem}</li>
     *   <li>FLUIDS_ONLY: apenas {@code BucketItem} (representação simples de fluidos)</li>
     * </ul>
     */
    public enum PipeType { UNIVERSAL, ITEMS_ONLY, BLOCKS_ONLY, FLUIDS_ONLY }

    /** Operations every 4 ticks (0.2s) — feedback quase instantâneo. */
    public static final int OPERATION_PERIOD = 4;
    public static final int FILTER_SIZE = 5;

    private final Mode[] sideMode = new Mode[6];
    /**
     * v0.1.31: UM filtro por PIPE inteiro (não mais por-face).
     *
     * <p>Decisão tomada após reclamação do user: "filter blacklist com pedra
     * deixava pedra passar". Causa raiz era que o filter era POR FACE e o
     * user editava a face errada (clicava em SOUTH, mas o EXTRACT era NORTH
     * → o filter de SOUTH nunca era consultado pelo tick).
     *
     * <p>Agora: filter unificado, aplica em TODAS as faces extract/insert/
     * default do mesmo pipe. UX prometida pelo user: "se tem filter, ou
     * passa os itens listados ou não passa". Funciona idêntico em ItemPipe,
     * ItemExtractor e ItemInserter.
     *
     * <p>Migration: load() lê NBT antigo (filter[6][5]) e usa filter[0] como
     * o filtro unificado pra preservar configurações de saves antigos.
     */
    private final ItemStack[] filter = new ItemStack[FILTER_SIZE];
    /** WHITELIST (==) ou BLACKLIST (!=) — único pro pipe. */
    private FilterMode filterMode = FilterMode.WHITELIST;
    private Speed speed = Speed.SLOW;
    private PipeType pipeType = PipeType.UNIVERSAL;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_PIPE.get(), pos, state);
        for (int i = 0; i < 6; i++) {
            sideMode[i] = Mode.DEFAULT;
        }
        for (int j = 0; j < FILTER_SIZE; j++) filter[j] = ItemStack.EMPTY;
    }

    // ---------------------------------------------------------------- public API used by the block

    public Mode getMode(Direction d) { return sideMode[d.get3DDataValue()]; }
    public void setMode(Direction d, Mode m) {
        sideMode[d.get3DDataValue()] = m;
        markUpdated();
        if (level != null && !level.isClientSide()) {
            // Visual: refaz arms (DISABLED esconde, demais mostra)
            br.com.murilo.liberthia.block.ItemPipeBlock.recomputeConnections(level, worldPosition);
            // Vizinho pipe também precisa recomputar (sua face oposta pode esconder)
            for (Direction nd : Direction.values()) {
                BlockPos npos = worldPosition.relative(nd);
                if (level.getBlockState(npos).getBlock() instanceof br.com.murilo.liberthia.block.ItemPipeBlock) {
                    br.com.murilo.liberthia.block.ItemPipeBlock.recomputeConnections(level, npos);
                }
            }
        }
    }
    public Mode cycleMode(Direction d) {
        Mode cur = getMode(d);
        Mode next = switch (cur) {
            case DEFAULT  -> Mode.EXTRACT;
            case EXTRACT  -> Mode.INSERT;
            case INSERT   -> Mode.DISABLED;
            case DISABLED -> Mode.DEFAULT;
        };
        setMode(d, next);
        return next;
    }

    public Speed getSpeed() { return speed; }
    public Speed cycleSpeed() {
        speed = switch (speed) {
            case SLOW  -> Speed.FAST;
            case FAST  -> Speed.STACK;
            case STACK -> Speed.SLOW;
        };
        markUpdated();
        return speed;
    }
    public int itemsPerOperation() {
        return switch (speed) {
            case SLOW -> 1;
            case FAST -> 4;
            case STACK -> 64;
        };
    }

    public PipeType getPipeType() { return pipeType; }
    public PipeType cyclePipeType() {
        pipeType = switch (pipeType) {
            case UNIVERSAL    -> PipeType.ITEMS_ONLY;
            case ITEMS_ONLY   -> PipeType.BLOCKS_ONLY;
            case BLOCKS_ONLY  -> PipeType.FLUIDS_ONLY;
            case FLUIDS_ONLY  -> PipeType.UNIVERSAL;
        };
        markUpdated();
        return pipeType;
    }

    /** Verifica se o tipo do pipe permite transportar este item. */
    public boolean pipeTypeAllows(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return switch (pipeType) {
            case UNIVERSAL   -> true;
            case ITEMS_ONLY  -> !(stack.getItem() instanceof net.minecraft.world.item.BlockItem)
                              && !(stack.getItem() instanceof net.minecraft.world.item.BucketItem);
            case BLOCKS_ONLY -> stack.getItem() instanceof net.minecraft.world.item.BlockItem;
            case FLUIDS_ONLY -> stack.getItem() instanceof net.minecraft.world.item.BucketItem;
        };
    }

    /**
     * v0.1.31: filtro unificado por pipe inteiro. Os métodos antigos com
     * {@code Direction} foram preservados pra back-compat — ignoram a direção
     * e delegam pra versão sem-direção.
     */
    public ItemStack[] getFilter() { return filter; }
    public ItemStack[] getFilter(Direction d) { return filter; } // legacy

    /** Adiciona um item ao primeiro slot vazio do filter. */
    public boolean addFilter(ItemStack stack) {
        for (int i = 0; i < FILTER_SIZE; i++) {
            if (filter[i].isEmpty()) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                filter[i] = copy;
                markUpdated();
                return true;
            }
        }
        return false;
    }
    public boolean addFilter(Direction d, ItemStack stack) { return addFilter(stack); } // legacy

    public void clearFilter() {
        for (int i = 0; i < FILTER_SIZE; i++) filter[i] = ItemStack.EMPTY;
        markUpdated();
    }
    public void clearFilter(Direction d) { clearFilter(); } // legacy

    public FilterMode getFilterMode() { return filterMode; }
    public FilterMode getFilterMode(Direction d) { return filterMode; } // legacy

    /** Cicla WHITELIST → BLACKLIST → WHITELIST. */
    public FilterMode cycleFilterMode() {
        filterMode = (filterMode == FilterMode.WHITELIST)
                ? FilterMode.BLACKLIST : FilterMode.WHITELIST;
        markUpdated();
        return filterMode;
    }
    public FilterMode cycleFilterMode(Direction d) { return cycleFilterMode(); } // legacy

    /**
     * True se o stack pode passar pelo pipe. Comportamento:
     * <ul>
     *   <li>filtro vazio: passa-tudo (ambos modos)</li>
     *   <li>WHITELIST (==): passa SE o stack matcha algum item do filtro</li>
     *   <li>BLACKLIST (!=): passa SE o stack NÃO matcha NENHUM item do filtro</li>
     * </ul>
     *
     * <p>Match relaxado: compara apenas {@code Item}, ignora NBT/dano.
     */
    private boolean filterAllows(ItemStack stack) {
        boolean anyConfigured = false;
        boolean matched = false;
        for (ItemStack fs : filter) {
            if (!fs.isEmpty()) {
                anyConfigured = true;
                if (fs.getItem() == stack.getItem()) {
                    matched = true;
                    break;
                }
            }
        }
        if (!anyConfigured) return true;
        return filterMode == FilterMode.WHITELIST
                ? matched      // == : passa só os listados
                : !matched;    // != : passa tudo MENOS os listados
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---------------------------------------------------------------- ticker

    public static void tick(Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity be) {
        if (level.isClientSide) return;
        if (level.getGameTime() % OPERATION_PERIOD != 0) return;

        // Para cada face EXTRACT: pega o handler do baú vizinho, coleta TODOS
        // os destinos da rede de pipes uma vez, e tenta mover items.
        for (Direction extractDir : Direction.values()) {
            if (be.getMode(extractDir) != Mode.EXTRACT) continue;

            BlockEntity sourceBE = level.getBlockEntity(pos.relative(extractDir));
            if (sourceBE == null) continue;
            IItemHandler source = sourceBE
                    .getCapability(ForgeCapabilities.ITEM_HANDLER, extractDir.getOpposite())
                    .orElse(null);
            if (source == null) continue;

            // Coleta destinos da rede UMA vez por tick por face EXTRACT
            List<Destination> destinations = collectDestinations(level, pos);
            if (destinations.isEmpty()) continue;

            // BUDGET POR SLOT — antes era global, e gastava tudo no primeiro
            // slot ocupado. Reclamação histórica: "baú com stone + cobblestone
            // só transfere stone até esvaziar, depois cobblestone". Agora cada
            // slot tem seu próprio budget, então TODOS os tipos de item se
            // movem em paralelo no mesmo tick.
            //
            // Trade-off: throughput total aumenta linearmente com nº de slots
            // ocupados (8 tipos diferentes = 8x mais items/tick que antes).
            // Isso é o comportamento que pipes de outros mods (AE2, Pipez,
            // Modular Pipes) já fazem — usuário esperava isso.
            int budgetPerSlot = be.itemsPerOperation();
            int totalMoved = 0;
            for (int slot = 0; slot < source.getSlots(); slot++) {
                int slotBudget = budgetPerSlot;
                ItemStack peek = source.extractItem(slot, slotBudget, true);
                if (peek.isEmpty()) continue;
                if (!be.pipeTypeAllows(peek)) continue;
                // v0.1.31: filter UNIFICADO do pipe (não mais por-face).
                // Pipe de origem decide se PODE SAIR.
                if (!be.filterAllows(peek)) continue;

                for (Destination dest : destinations) {
                    if (peek.isEmpty() || slotBudget <= 0) break;
                    // Pipe de destino decide se PODE ENTRAR.
                    if (!dest.outputPipe.filterAllows(peek)) continue;

                    ItemStack toInsert = peek.copy();
                    ItemStack remainder = ItemHandlerHelper.insertItem(dest.handler, toInsert, false);
                    int inserted = peek.getCount() - remainder.getCount();
                    if (inserted <= 0) continue;

                    // Extrai DE VERDADE da fonte
                    ItemStack actuallyTaken = source.extractItem(slot, inserted, false);
                    int taken = actuallyTaken.getCount();
                    if (taken < inserted) {
                        // Race-rare: source diminuiu entre simulate e commit.
                        // Forge não tem rollback fácil aqui — aceita a perda.
                    }
                    slotBudget -= taken;
                    totalMoved += taken;
                    if (slotBudget <= 0) break;
                    peek = source.extractItem(slot, slotBudget, true);
                    if (peek.isEmpty()) break;
                }
            }

        }
    }

    /** Destino coletado na rede: o handler do bloco final + qual pipe/face é a saída. */
    private static final class Destination {
        final IItemHandler handler;
        final ItemPipeBlockEntity outputPipe;
        final Direction outputFace;
        Destination(IItemHandler h, ItemPipeBlockEntity p, Direction f) {
            this.handler = h; this.outputPipe = p; this.outputFace = f;
        }
    }

    /**
     * BFS desde o pipe inicial pela rede de pipes. Retorna todos os inventários
     * acessíveis através de qualquer face NÃO-DISABLED + NÃO-EXTRACT.
     *
     * <p>Ordem: BFS = nearest-first. Pipes INSERT são preferenciais — listados
     * primeiro. Pipes DEFAULT viram fallback automático.
     */
    private static List<Destination> collectDestinations(Level level, BlockPos start) {
        List<Destination> insertDests = new ArrayList<>();
        List<Destination> defaultDests = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos cur = queue.pollFirst();
            BlockEntity be = level.getBlockEntity(cur);
            if (!(be instanceof ItemPipeBlockEntity pipe)) continue;

            for (Direction d : Direction.values()) {
                Mode mode = pipe.getMode(d);
                if (mode == Mode.DISABLED) continue;
                BlockPos npos = cur.relative(d);
                BlockEntity nbe = level.getBlockEntity(npos);

                // Vizinho é outro pipe? Atravessa.
                if (nbe instanceof ItemPipeBlockEntity neighborPipe) {
                    if (neighborPipe.getMode(d.getOpposite()) == Mode.DISABLED) continue;
                    if (visited.add(npos)) queue.add(npos);
                    continue;
                }

                // Vizinho não-pipe = candidato a destino. EXTRACT NUNCA é saída.
                if (mode == Mode.EXTRACT) continue;
                if (nbe == null) continue;
                IItemHandler h = nbe.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).orElse(null);
                if (h == null) continue;

                Destination dst = new Destination(h, pipe, d);
                if (mode == Mode.INSERT) insertDests.add(dst);
                else defaultDests.add(dst); // DEFAULT
            }
        }

        // INSERT primeiro, DEFAULT depois (fallback automático)
        insertDests.addAll(defaultDests);
        return insertDests;
    }


    // ---------------------------------------------------------------- NBT

    @Override
    protected void saveAdditional(CompoundTag tag) {
        for (int i = 0; i < 6; i++) tag.putString("mode" + i, sideMode[i].name());
        // v0.1.31: filtro UNIFICADO. Save key "filterMode" (sem sufixo de face).
        tag.putString("filterMode", filterMode.name());
        tag.putString("speed", speed.name());
        tag.putString("pipeType", pipeType.name());

        ListTag list = new ListTag();
        for (int j = 0; j < FILTER_SIZE; j++) {
            ItemStack stk = filter[j];
            CompoundTag entry = new CompoundTag();
            if (!stk.isEmpty()) stk.save(entry);
            list.add(entry);
        }
        tag.put("filter", list);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < 6; i++) {
            String s = tag.contains("mode" + i) ? tag.getString("mode" + i) : "DEFAULT";
            try { sideMode[i] = Mode.valueOf(s); } catch (Exception e) { sideMode[i] = Mode.DEFAULT; }
        }
        try { pipeType = PipeType.valueOf(tag.contains("pipeType") ? tag.getString("pipeType") : "UNIVERSAL"); }
        catch (Exception e) { pipeType = PipeType.UNIVERSAL; }
        try { speed = Speed.valueOf(tag.contains("speed") ? tag.getString("speed") : "SLOW"); }
        catch (Exception e) { speed = Speed.SLOW; }

        // v0.1.31: MIGRATION pra filtro unificado.
        // Save antigo tinha filterMode[6] (keys "filterMode0".."filterMode5") +
        // filter[6][5] (keys "filter0".."filter5"). Save novo tem só "filterMode"
        // (string) + "filter" (list de 5 itens).
        if (tag.contains("filterMode")) {
            // Novo format
            try { filterMode = FilterMode.valueOf(tag.getString("filterMode")); }
            catch (Exception e) { filterMode = FilterMode.WHITELIST; }
        } else if (tag.contains("filterMode0")) {
            // Migration: usa filterMode0 do save velho
            try { filterMode = FilterMode.valueOf(tag.getString("filterMode0")); }
            catch (Exception e) { filterMode = FilterMode.WHITELIST; }
        } else {
            filterMode = FilterMode.WHITELIST;
        }

        if (tag.contains("filter", 9)) { // 9 = TAG_LIST
            // Novo format
            ListTag list = tag.getList("filter", 10);
            for (int j = 0; j < FILTER_SIZE; j++) {
                if (j < list.size()) {
                    CompoundTag entry = list.getCompound(j);
                    filter[j] = entry.isEmpty() ? ItemStack.EMPTY : ItemStack.of(entry);
                } else {
                    filter[j] = ItemStack.EMPTY;
                }
            }
        } else if (tag.contains("filter0")) {
            // Migration: usa filter0 do save velho (filtro da face NORTH)
            ListTag list = tag.getList("filter0", 10);
            for (int j = 0; j < FILTER_SIZE; j++) {
                if (j < list.size()) {
                    CompoundTag entry = list.getCompound(j);
                    filter[j] = entry.isEmpty() ? ItemStack.EMPTY : ItemStack.of(entry);
                } else {
                    filter[j] = ItemStack.EMPTY;
                }
            }
        } else {
            for (int j = 0; j < FILTER_SIZE; j++) filter[j] = ItemStack.EMPTY;
        }
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
