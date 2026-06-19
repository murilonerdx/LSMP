package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.menu.MatterPurifierMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Matter Purifier — purifica matter brutos em ingots estáveis.
 *
 * <p>Aceita 1 dos 3 inputs e gera o ingot purificado correspondente:
 * <ul>
 *   <li>{@code dark_matter_shard} → {@code purified_dark_matter_ingot}</li>
 *   <li>{@code yellow_matter_ingot} → {@code purified_yellow_matter_ingot}</li>
 *   <li>{@code clear_matter_bucket} → {@code purified_clear_matter_ingot}
 *       (devolve {@code bucket} vazio no slot output ou drop)</li>
 * </ul>
 *
 * <p>Custo: 30.000 FE + 100 ticks (5s) por purificação. Output rarity RARE.
 *
 * <p>Os ingots purificados são usados pra craftar as armaduras de matter
 * (Dark/Clear/Yellow) — substituem os crus nas recipes.
 */
public class MatterPurifierBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;

    public static final int FE_BUFFER = 500_000;
    public static final int FE_PER_TICK = 1_500;
    public static final int FE_PER_OP = 30_000;
    /**
     * Custo elevado pra purificar items com tag {@code MatterInfected} (produzidos
     * pelo Dark Matter Alchemizer). Higher cost reflete o esforço extra de
     * "limpar" o resíduo de matéria escura impregnado no item. Slow-ready
     * (sem energia) NÃO completa pra items infectados — força o player a
     * conectar gerador.
     */
    public static final int FE_PER_OP_INFECTED = 100_000;
    public static final int BASE_PROCESS_TICKS = 100;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // v0.1.27 fix definitivo: ANTES `return false` no SLOT_OUTPUT bloqueava
            // QUALQUER insert no slot, incluindo o `insertItem(SLOT_OUTPUT, ...)`
            // chamado pelo próprio tick. Resultado: a máquina mostrava "Purificando"
            // até 100%, consumia energia, deletava o input — mas o ingot purificado
            // NUNCA aparecia. Bug invisível (sem erro no log).
            // O slot OUTPUT é bloqueado pro player via `mayPlace(false)` no menu;
            // aqui aceitamos qualquer item pra que o tick consiga inserir o
            // resultado. Player ainda não consegue colocar manualmente.
            if (slot == SLOT_INPUT) return outputForInput(stack) != null;
            return true;
        }
    };

    private final br.com.murilo.liberthia.energy.TrackedEnergyStorage energy =
            new br.com.murilo.liberthia.energy.TrackedEnergyStorage(this, FE_BUFFER, FE_PER_TICK * 4, 0);
    private LazyOptional<IItemHandler> lazyItem = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();

    private int progress = 0;
    private int feSpent = 0;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            int v = switch (i) {
                case 0, 1 -> energy.getEnergyStored();
                case 2, 3 -> energy.getMaxEnergyStored();
                case 4 -> progress;
                // v0.1.42: max ticks depende do TIPO de input pra que a progress
                // bar não estoure 100%. Antes mostrava 253% porque sword precisa
                // de 400 ticks (600k FE / 1500 FE/tick) mas o max era 100.
                case 5 -> computeMaxProgress();
                default -> 0;
            };
            return switch (i) {
                case 0, 2 -> (v >> 16) & 0xFFFF;
                case 1, 3 -> v & 0xFFFF;
                default -> v;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 6; }
    };

    /**
     * v0.1.42: max ticks dinâmico baseado no input atual.
     * - Sword (600k FE): 400 ticks pra preencher = 100%
     * - Infected (100k FE): 67 ticks
     * - Normal (30k FE): 20 ticks → usa BASE_PROCESS_TICKS (100) como mínimo
     */
    private int computeMaxProgress() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.getItem() == ModItems.CLEAR_MATTER_SWORD.get()) {
            // Pro sword o tempo é dominado por FE — 600k / 1500/tick = 400 ticks
            return Math.max(BASE_PROCESS_TICKS, FE_PER_OP_SWORD / FE_PER_TICK);
        }
        if (input.getTag() != null && input.getTag().getBoolean("MatterInfected")) {
            return Math.max(BASE_PROCESS_TICKS, FE_PER_OP_INFECTED / FE_PER_TICK);
        }
        return BASE_PROCESS_TICKS;
    }

    public MatterPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_PURIFIER.get(), pos, state);
    }

    public IItemHandler getInventory() { return inventory; }

    /**
     * Mapeia input item → output purified. null = input inválido.
     *
     * <p>v0.1.27: receitas expandidas. Agora aceita TODAS as formas crus de
     * cada matter (shard, bucket, ingot) — antes só aceitava 1 forma de cada,
     * o que confundia o user que tinha o material mas na forma "errada".
     */
    /** v0.1.40: custo elevado pra purificar Clear Matter Sword. */
    public static final int FE_PER_OP_SWORD = 600_000;

    public static @Nullable ItemStack outputForInput(ItemStack input) {
        if (input.isEmpty()) return null;

        // ─── CLEAR MATTER SWORD (purifica → não infecta mais) ───
        // v0.1.40: sword infectada vira "purificada" (NBT "Purified"=true).
        // Não consome o item — devolve o MESMO com tag setada. Custo: 600k FE.
        if (input.getItem() == ModItems.CLEAR_MATTER_SWORD.get()
                && (input.getTag() == null || !input.getTag().getBoolean("Purified"))) {
            ItemStack purified = input.copy();
            purified.setCount(1);
            purified.getOrCreateTag().putBoolean("Purified", true);
            return purified;
        }

        // ─── ITEMS INFECTADOS (tag MatterInfected) ───
        // Items produzidos pelo Dark Matter Alchemizer carregam tag NBT
        // "MatterInfected: true". A purificação retorna o MESMO item (qualquer
        // que seja — diamond block, totem, elytra, etc.) SEM a tag — recipe é
        // 1:1 sobre o próprio item.
        // Custo extra de FE (100k vs 30k) é aplicado em `tick()` via checagem
        // dinâmica de `requiredFe`.
        if (input.getTag() != null && input.getTag().getBoolean("MatterInfected")) {
            ItemStack cleaned = input.copy();
            cleaned.setCount(1);
            if (cleaned.getTag() != null) {
                cleaned.getTag().remove("MatterInfected");
                if (cleaned.getTag().isEmpty()) cleaned.setTag(null);
            }
            return cleaned;
        }

        var item = input.getItem();
        // ─── DARK MATTER ───
        // dark_matter_shard (cru) → purified
        if (item == ModItems.DARK_MATTER_SHARD.get())
            return new ItemStack(ModItems.PURIFIED_DARK_MATTER_INGOT.get());
        // v0.1.22 r20: dark_matter_ingot (sólido) → purified (faltava antes)
        if (item == ModItems.DARK_MATTER_INGOT.get())
            return new ItemStack(ModItems.PURIFIED_DARK_MATTER_INGOT.get());
        // dark_matter_bucket (forma fluida) → purified
        if (item == ModItems.DARK_MATTER_BUCKET.get())
            return new ItemStack(ModItems.PURIFIED_DARK_MATTER_INGOT.get());

        // ─── YELLOW MATTER ───
        // yellow_matter_ingot (forma sólida) → purified
        if (item == ModItems.YELLOW_MATTER_INGOT.get())
            return new ItemStack(ModItems.PURIFIED_YELLOW_MATTER_INGOT.get());
        // yellow_matter_bucket (forma fluida) → purified
        if (item == ModItems.YELLOW_MATTER_BUCKET.get())
            return new ItemStack(ModItems.PURIFIED_YELLOW_MATTER_INGOT.get());

        // ─── CLEAR MATTER ───
        // v0.1.22 r20: clear_matter_ingot (sólido) → purified (BUG #29 — faltava)
        if (item == ModItems.CLEAR_MATTER_INGOT.get())
            return new ItemStack(ModItems.PURIFIED_CLEAR_MATTER_INGOT.get());
        // clear_matter_bucket (forma fluida) → purified
        if (item == ModItems.CLEAR_MATTER_BUCKET.get())
            return new ItemStack(ModItems.PURIFIED_CLEAR_MATTER_INGOT.get());

        return null;
    }

    /** True se o input é um bucket de matter (devolve bucket vazio no output). */
    private static boolean isMatterBucket(ItemStack input) {
        var i = input.getItem();
        return i == ModItems.DARK_MATTER_BUCKET.get()
                || i == ModItems.YELLOW_MATTER_BUCKET.get()
                || i == ModItems.CLEAR_MATTER_BUCKET.get();
    }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItem.cast();
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }
    @Override public void onLoad() {
        super.onLoad();
        lazyItem = LazyOptional.of(() -> inventory);
        lazyEnergy = LazyOptional.of(() -> energy);
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        lazyItem.invalidate();
        lazyEnergy.invalidate();
    }

    public void drops() {
        net.minecraft.world.SimpleContainer c = new net.minecraft.world.SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) c.setItem(i, inventory.getStackInSlot(i));
        net.minecraft.world.Containers.dropContents(level, worldPosition, c);
    }

    @Override public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.matter_purifier");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new MatterPurifierMenu(id, inv, this, this.data);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MatterPurifierBlockEntity be) {
        if (level.isClientSide) {
            tickClientParticles(level, pos, be);
            return;
        }
        if (!canProcess(be)) {
            if (be.progress > 0 || be.feSpent > 0) {
                be.progress = 0;
                be.feSpent = 0;
                be.setChanged();
            }
            return;
        }

        // v0.1.50: ENERGIA AGORA É OBRIGATÓRIA (removido o "modo lento" sem FE).
        // Sem energia suficiente no buffer: pausa SEM resetar o progresso —
        // quando voltar FE, continua de onde parou (mesmo padrão do Matter
        // Refiner e do Matter Pill Brewer). Antes (v0.1.27) a máquina
        // purificava sem gerador em 1200t, o que parecia bug.
        int drain = Math.min(FE_PER_TICK, be.energy.getEnergyStored());
        if (drain <= 0) return;
        be.energy.extractEnergy(drain, false);
        be.feSpent += drain;
        be.progress++;

        // ── Custo de FE depende do tipo de input ──
        // Items com tag MatterInfected exigem 100k FE (vs 30k normal); a
        // Clear Matter Sword exige 600k FE.
        ItemStack inputForCost = be.inventory.getStackInSlot(SLOT_INPUT);
        boolean isInfected = inputForCost.getTag() != null
                && inputForCost.getTag().getBoolean("MatterInfected");
        boolean isSword = inputForCost.getItem() == ModItems.CLEAR_MATTER_SWORD.get();
        // v0.1.40: Clear Matter Sword precisa de 600k FE (vs 100k infectado / 30k normal).
        int requiredFe = isSword ? FE_PER_OP_SWORD
                : (isInfected ? FE_PER_OP_INFECTED : FE_PER_OP);

        boolean ready = be.progress >= BASE_PROCESS_TICKS && be.feSpent >= requiredFe;

        if (ready) {
            ItemStack input = be.inventory.getStackInSlot(SLOT_INPUT);
            ItemStack output = outputForInput(input);
            if (output != null) {
                be.inventory.extractItem(SLOT_INPUT, 1, false);
                // Qualquer matter bucket: devolve bucket vazio dropping no chão
                // (não tem slot pra colocar). Simples e claro pro player.
                if (isMatterBucket(input)
                        && level instanceof net.minecraft.server.level.ServerLevel sl) {
                    net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(
                            sl, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                            new ItemStack(net.minecraft.world.item.Items.BUCKET));
                    sl.addFreshEntity(ie);
                }
                be.inventory.insertItem(SLOT_OUTPUT, output, false);
            }
            be.progress = 0;
            be.feSpent = 0;
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        30, 0.3, 0.4, 0.3, 0.08);
            }
        }
        be.setChanged();
        if (level.getGameTime() % 5 == 0) {
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private static void tickClientParticles(Level level, BlockPos pos, MatterPurifierBlockEntity be) {
        if (be.progress <= 0) return;
        float pct = be.progress / (float) BASE_PROCESS_TICKS;
        if (level.random.nextFloat() < 0.7f) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = 0.35;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.getX() + 0.5 + Math.cos(a) * r,
                    pos.getY() + 0.4 + level.random.nextDouble() * 0.6,
                    pos.getZ() + 0.5 + Math.sin(a) * r,
                    0, 0.04 + pct * 0.05, 0);
        }
        if (level.random.nextFloat() < pct * 0.25f) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.GLOW,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    (level.random.nextDouble() - 0.5) * 0.05,
                    0.02, (level.random.nextDouble() - 0.5) * 0.05);
        }
        if (pct > 0.85f && level.random.nextInt(30) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.7f, false);
        }
    }

    private static boolean canProcess(MatterPurifierBlockEntity be) {
        ItemStack input = be.inventory.getStackInSlot(SLOT_INPUT);
        ItemStack expectedOut = outputForInput(input);
        if (expectedOut == null) return false;
        ItemStack output = be.inventory.getStackInSlot(SLOT_OUTPUT);
        if (!output.isEmpty()) {
            if (!ItemStack.isSameItem(output, expectedOut)) return false;
            if (output.getCount() >= output.getMaxStackSize()) return false;
        }
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inv", inventory.serializeNBT());
        tag.put("energy", energy.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("feSpent", feSpent);
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inv")) inventory.deserializeNBT(tag.getCompound("inv"));
        if (tag.contains("energy")) energy.deserializeNBT(tag.get("energy"));
        progress = tag.getInt("progress");
        feSpent = tag.getInt("feSpent");
    }

    @Nullable @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
