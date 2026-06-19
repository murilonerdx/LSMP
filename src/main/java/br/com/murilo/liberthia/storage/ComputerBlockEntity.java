package br.com.murilo.liberthia.storage;

import br.com.murilo.liberthia.item.computer.ComputerData;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.items.ItemStackHandler;
import br.com.murilo.liberthia.item.HardDriveItem;
import br.com.murilo.liberthia.energy.TrackedEnergyStorage;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * <b>Computador (bloco)</b> — armazena os relatórios analisados (DNA/sangue/
 * matéria) num {@link ComputerData}. Funciona como "estação" ao lado do Matter
 * Analyzer: o botão "Salvar PC" do analyzer só grava se este bloco estiver
 * adjacente.
 *
 * <p>Recursos:
 * <ul>
 *   <li>Lista de arquivos (relatórios) persistida no NBT + exportada em JSON.</li>
 *   <li>Login opcional com senha (dono ativa/desativa). Quem souber a senha vê.</li>
 *   <li>Backup/restauração via HD (item).</li>
 * </ul>
 */
public class ComputerBlockEntity extends BlockEntity {

    private String password = "";
    private boolean loginEnabled = false;
    private UUID owner = null;
    private String diskId = null;
    /** Quem já autenticou nesta sessão (não persiste). */
    private final Set<UUID> authed = new HashSet<>();

    /** Slot do HD (1) — só aceita HardDriveItem. Sincronizado pro cliente. */
    private final ItemStackHandler hdInv = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, ItemStack s) { return s.getItem() instanceof HardDriveItem; }
        @Override protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide())
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };

    // ── Energia (FE) ─────────────────────────────────────────────────────────────
    // O computador precisa de energia pra ligar (abrir) e pra salvar relatórios.
    // Recebe FE por qualquer lado (cabos / baterias / geradores). Não emite.
    public static final int ENERGY_CAPACITY = 30_000;
    /** FE consumido ao abrir/ligar a tela. */
    public static final int OPEN_COST = 20;
    /** FE consumido ao gravar um relatório (botão "Salvar PC" do Analyzer). */
    public static final int SAVE_COST = 150;

    private final TrackedEnergyStorage energy = new TrackedEnergyStorage(this, ENERGY_CAPACITY, 1024, 0);
    private final LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> energy);

    public ComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPUTER_BLOCK.get(), pos, state);
    }

    // ── Energia ──────────────────────────────────────────────────────────────────
    public int getEnergyStored() { return energy.getEnergyStored(); }
    public int getMaxEnergy() { return energy.getMaxEnergyStored(); }
    public boolean hasEnergy(int amount) { return energy.getEnergyStored() >= amount; }
    /** Consome {@code amount} FE se houver. true se consumiu. */
    public boolean useEnergy(int amount) {
        if (energy.getEnergyStored() < amount) return false;
        energy.setStored(energy.getEnergyStored() - amount);
        return true;
    }

    // ── HD slot ─────────────────────────────────────────────────────────────────
    public ItemStack getHd() { return hdInv.getStackInSlot(0); }
    public void setHd(ItemStack s) { hdInv.setStackInSlot(0, s); }

    // ── Files (vivem no HD inserido) ─────────────────────────────────────────
    // O HD *É* o armazenamento. Sem HD no slot, o computador não tem relatórios
    // (eles somem da tela). Lemos/gravamos direto no disco — não há cópia interna,
    // então tirar e pôr o HD NÃO duplica mais nada.
    public boolean hasHd() { return !getHd().isEmpty(); }

    public ListTag getFiles() {
        ItemStack hd = getHd();
        return hd.isEmpty() ? new ListTag() : HardDriveItem.readFiles(hd);
    }

    /** Anexa um relatório no HD inserido. false se: sem HD, sem energia, ou cheio. */
    public boolean appendReport(String name, String body) {
        ItemStack hd = getHd();
        if (hd.isEmpty()) return false;                          // sem HD
        if (energy.getEnergyStored() < SAVE_COST) return false;  // sem energia
        ListTag files = HardDriveItem.readFiles(hd);
        if (files.size() >= ComputerData.MAX_FILES) return false; // cheio
        CompoundTag c = new CompoundTag();
        c.putString("n", name == null ? "" : name);
        c.putString("b", body == null ? "" : body);
        c.putInt("t", ComputerData.TYPE_TEXT);
        files.add(c);
        HardDriveItem.writeFiles(hd, files);
        useEnergy(SAVE_COST);
        setHd(hd);          // re-set → onContentsChanged → sincroniza o HD pro cliente
        exportJson();
        return true;
    }

    // ── Login / dono ──────────────────────────────────────────────────────────
    public void setOwner(UUID id) { this.owner = id; setChanged(); }
    public boolean isOwner(Player p) { return owner == null || owner.equals(p.getUUID()); }
    public boolean isLoginEnabled() { return loginEnabled; }

    public boolean isAuthed(Player p) {
        return authed.contains(p.getUUID());
    }

    public boolean hasPassword() { return !password.isEmpty(); }

    /** Marca o player como autenticado nesta sessão (ex.: o dono logo após configurar). */
    public void markAuthed(Player p) { authed.add(p.getUUID()); }

    /** Limpa todas as autenticações (ex.: senha mudou → todos precisam logar de novo). */
    public void clearAuth() { authed.clear(); }

    /**
     * Bloqueado pra esse player? Vale pra TODOS (inclusive o dono) quando o login
     * está ligado e há senha — assim a senha realmente protege e dá pra testar
     * sozinho. Cada um autentica uma vez por sessão.
     */
    public boolean isLocked(Player p) {
        return loginEnabled && !password.isEmpty() && !authed.contains(p.getUUID());
    }

    /** Tenta logar. true se a senha bate. */
    public boolean tryLogin(Player p, String pass) {
        if (pass != null && pass.equals(password)) {
            authed.add(p.getUUID());
            return true;
        }
        return false;
    }

    /** Dono configura login + senha. */
    public void setConfig(boolean login, String pass) {
        this.loginEnabled = login;
        if (pass != null) this.password = pass;
        setChanged();
    }

    // ── JSON export (persistência/lore) ─────────────────────────────────────────
    public String getOrCreateId() {
        if (diskId == null || diskId.isEmpty()) {
            diskId = UUID.randomUUID().toString().substring(0, 8);
            setChanged();
        }
        return diskId;
    }

    private void exportJson() {
        if (level == null || level.isClientSide()) return;
        MinecraftServer server = level.getServer();
        if (server == null) return;
        try {
            Path dir = server.getWorldPath(LevelResource.ROOT).resolve("liberthia_computers");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(getOrCreateId() + ".json"),
                    ComputerData.toJson(ComputerData.fromList(getFiles())));
        } catch (Exception ignored) {}
    }

    // ── NBT ─────────────────────────────────────────────────────────────────────
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // Relatórios não ficam mais no bloco — vivem no HD (slot "hd").
        tag.putString("Pass", password);
        tag.putBoolean("Login", loginEnabled);
        if (owner != null) tag.putUUID("Owner", owner);
        if (diskId != null) tag.putString(ComputerData.NBT_ID, diskId);
        tag.put("hd", hdInv.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        password = tag.getString("Pass");
        loginEnabled = tag.getBoolean("Login");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        diskId = tag.contains(ComputerData.NBT_ID) ? tag.getString(ComputerData.NBT_ID) : null;
        if (tag.contains("hd")) hdInv.deserializeNBT(tag.getCompound("hd"));
        energy.setStored(tag.getInt("Energy"));
    }

    // ── Sync (HD + energia vão pro cliente; arquivos/senha vão pelo packet de abertura) ──
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("hd", hdInv.serializeNBT());
        tag.putInt("energy", energy.getEnergyStored());
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null && pkt.getTag().contains("hd")) {
            hdInv.deserializeNBT(pkt.getTag().getCompound("hd"));
        }
        if (pkt.getTag() != null && pkt.getTag().contains("energy")) {
            energy.setStored(pkt.getTag().getInt("energy"));
        }
    }

    // ── Capability de energia (recebe FE de cabos/baterias por qualquer lado) ──
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate();
    }
}
