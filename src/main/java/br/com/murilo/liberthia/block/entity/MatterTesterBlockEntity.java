package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.item.SampleVialItem;
import br.com.murilo.liberthia.matter.MatterContent;
import br.com.murilo.liberthia.matter.MatterContentRegistry;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.storage.ComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>Matter Tester</b> (Testador de Matéria) — uma máquina de bancada que combina
 * DUAS amostras de matéria e analisa a reação resultante (ex.: Branca + Amarela →
 * Estrategista; Escura + Amarela → Instável). Não consome as amostras: é um scanner.
 *
 * <p>Cada teste:
 * <ul>
 *   <li>roda um "scan" de ~5s (barra de progresso);</li>
 *   <li>classifica a {@link MatterContent.Mutation} da combinação;</li>
 *   <li>guarda um log local (últimos {@value #MAX_LOGS});</li>
 *   <li>marca §lNOVA DESCOBERTA§r na primeira vez que uma mutação aparece;</li>
 *   <li>se houver um <b>Computador adjacente</b> (com HD + energia), envia o relatório
 *       completo via {@link ComputerBlockEntity#appendReport} — fica salvo no HD/JSON.</li>
 * </ul>
 * Só re-testa quando as amostras mudam (não fica spammando log).
 */
public class MatterTesterBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_A = 0;
    public static final int SLOT_B = 1;
    public static final int MAX_LOGS = 16;
    private static final int SCAN_TIME = 100; // 5s

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                // amostras mudaram → libera novo teste e empurra update pro cliente
                lastSignature = "";
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        @Override public int getSlotLimit(int slot) { return 64; }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private int progress = 0;
    /** Assinatura do par de amostras já testado — evita re-testar o mesmo. */
    private String lastSignature = "";

    // resultado do ÚLTIMO teste (sincronizado pro cliente via ContainerData)
    private int resMutation = 0;   // ordinal+1 (0 = nenhum teste ainda)
    private int resDark = 0, resWhite = 0, resYellow = 0;
    private boolean resNew = false;
    private boolean computerLinked = false;

    /** Mutações já descobertas (persistido) — pra marcar NOVA DESCOBERTA. */
    private final Set<Integer> discovered = new HashSet<>();
    /** Log de texto dos últimos testes (persistido + sincronizado). */
    private final Deque<String> logs = new ArrayDeque<>();

    private final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> SCAN_TIME;
                case 2 -> resMutation;
                case 3 -> resDark;
                case 4 -> resWhite;
                case 5 -> resYellow;
                case 6 -> resNew ? 1 : 0;
                case 7 -> computerLinked ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {
            switch (i) {
                case 0 -> progress = v;
                case 2 -> resMutation = v;
                case 3 -> resDark = v;
                case 4 -> resWhite = v;
                case 5 -> resYellow = v;
                case 6 -> resNew = v != 0;
                case 7 -> computerLinked = v != 0;
            }
        }
        @Override public int getCount() { return 8; }
    };

    public MatterTesterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_TESTER.get(), pos, state);
    }

    public IItemHandler getItemHandler() { return inventory; }
    public ContainerData getData() { return data; }

    /** Lista (mais recente primeiro) dos logs — lida pela tela no cliente. */
    public List<String> getLogs() { return List.copyOf(logs); }

    // ── leitura de matéria de um slot (item de matéria OU sample vial) ──
    private static MatterContent contentOf(ItemStack s) {
        if (s.isEmpty()) return MatterContent.EMPTY;
        if (s.getItem() instanceof SampleVialItem) {
            MatterContent v = SampleVialItem.contentOf(s);
            if (v.total() > 0) return v;
        }
        return MatterContentRegistry.of(s);
    }

    private static String sig(ItemStack s, MatterContent c) {
        return s.isEmpty() ? "-" : s.getItem().toString()
                + ":" + (int) c.dark() + "," + (int) c.white() + "," + (int) c.yellow();
    }

    // ── TICK ──
    public static void tick(Level level, BlockPos pos, BlockState state, MatterTesterBlockEntity be) {
        if (level.isClientSide) return;

        ItemStack a = be.inventory.getStackInSlot(SLOT_A);
        ItemStack b = be.inventory.getStackInSlot(SLOT_B);
        MatterContent ca = contentOf(a);
        MatterContent cb = contentOf(b);

        // precisa de matéria nos DOIS slots
        boolean ready = ca.total() > 0 && cb.total() > 0;
        String signature = sig(a, ca) + "|" + sig(b, cb);

        if (!ready || signature.equals(be.lastSignature)) {
            if (be.progress != 0) { be.progress = 0; be.setChanged(); }
            return;
        }

        be.progress++;
        be.setChanged();
        if (be.progress >= SCAN_TIME) {
            be.progress = 0;
            be.lastSignature = signature;
            be.runTest(level, a, b, ca, cb);
        }
    }

    private void runTest(Level level, ItemStack a, ItemStack b, MatterContent ca, MatterContent cb) {
        MatterContent combo = new MatterContent(
                ca.dark() + cb.dark(), ca.white() + cb.white(), ca.yellow() + cb.yellow());
        MatterContent.Mutation m = combo.dominantMutation();

        resMutation = m.ordinal() + 1;
        resDark = (int) combo.dark();
        resWhite = (int) combo.white();
        resYellow = (int) combo.yellow();
        resNew = discovered.add(m.ordinal()); // add() = true se era inédita

        long day = level.getDayTime() / 24000L;
        String nameA = a.getHoverName().getString();
        String nameB = b.getHoverName().getString();
        String logLine = (resNew ? "§e✦ " : "§8• ")
                + m.color.toString() + m.displayName + " §7(" + nameA + " + " + nameB + ")";
        logs.addFirst(logLine);
        while (logs.size() > MAX_LOGS) logs.removeLast();

        // tenta enviar relatório pro Computador adjacente
        computerLinked = pushToComputer(level, m, combo, nameA, nameB, day);

        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        level.playSound(null, worldPosition,
                net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.4F, resNew ? 1.6F : 1.1F);
    }

    /** Procura um Computador nos 6 vizinhos e anexa o relatório. */
    private boolean pushToComputer(Level level, MatterContent.Mutation m, MatterContent combo,
                                   String nameA, String nameB, long day) {
        for (Direction d : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(d));
            if (be instanceof ComputerBlockEntity comp) {
                String title = "Teste: " + nameA + " + " + nameB;
                String body = "=== RELATORIO DE TESTE DE MATERIA ===\n"
                        + "Dia: " + day + "\n"
                        + "Amostra A: " + nameA + "\n"
                        + "Amostra B: " + nameB + "\n"
                        + "------------------------------------\n"
                        + "Composicao combinada:\n"
                        + "  Escura (DM):  " + (int) combo.dark() + "\n"
                        + "  Branca (WM):  " + (int) combo.white() + "\n"
                        + "  Amarela (YM): " + (int) combo.yellow() + "\n"
                        + "  Energia equiv.: " + combo.energyEquivalent() + " FE\n"
                        + "------------------------------------\n"
                        + "RESULTADO: " + m.displayName + "\n"
                        + m.description + "\n"
                        + (resNew ? "\n*** NOVA DESCOBERTA ***\n" : "");
                return comp.appendReport(title, body);
            }
        }
        return false;
    }

    // ── capabilities ──
    @Override public void onLoad() { super.onLoad(); lazyItemHandler = LazyOptional.of(() -> inventory); }
    @Override public void invalidateCaps() { super.invalidateCaps(); lazyItemHandler.invalidate(); }
    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    public void drops() {
        SimpleContainer c = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        Containers.dropContents(this.level, this.worldPosition, c);
    }

    // ── menu ──
    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.matter_tester");
    }
    @Nullable @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new br.com.murilo.liberthia.menu.MatterTesterMenu(id, inv, this, this.data);
    }

    // ── NBT ──
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        tag.putInt("progress", progress);
        tag.putString("sig", lastSignature);
        tag.putInt("resMutation", resMutation);
        tag.putInt("resDark", resDark);
        tag.putInt("resWhite", resWhite);
        tag.putInt("resYellow", resYellow);
        tag.putBoolean("resNew", resNew);
        tag.putBoolean("compLinked", computerLinked);
        int[] disc = discovered.stream().mapToInt(Integer::intValue).toArray();
        tag.putIntArray("discovered", disc);
        ListTag logTag = new ListTag();
        for (String s : logs) {
            CompoundTag ct = new CompoundTag();
            ct.putString("l", s);
            logTag.add(ct);
        }
        tag.put("logs", logTag);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) inventory.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("progress");
        lastSignature = tag.getString("sig");
        resMutation = tag.getInt("resMutation");
        resDark = tag.getInt("resDark");
        resWhite = tag.getInt("resWhite");
        resYellow = tag.getInt("resYellow");
        resNew = tag.getBoolean("resNew");
        computerLinked = tag.getBoolean("compLinked");
        discovered.clear();
        for (int i : tag.getIntArray("discovered")) discovered.add(i);
        logs.clear();
        ListTag logTag = tag.getList("logs", Tag.TAG_COMPOUND);
        for (int i = 0; i < logTag.size(); i++) logs.addLast(logTag.getCompound(i).getString("l"));
    }

    // sincroniza tudo (inclui logs) pro cliente
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
