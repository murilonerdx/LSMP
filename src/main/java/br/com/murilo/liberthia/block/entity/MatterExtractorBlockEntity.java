package br.com.murilo.liberthia.block.entity;

import br.com.murilo.liberthia.matter.MatterProfile;
import br.com.murilo.liberthia.matter.MatterProfileProvider;
import br.com.murilo.liberthia.menu.MatterExtractorMenu;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Block entity do Matter Extractor.
 *
 * <p>Mecânica:
 * <ul>
 *   <li>Tem buffer FE 1M (recebe energia de fora).</li>
 *   <li>A cada {@link #PERIOD} ticks, escaneia players num raio de 3 blocos.</li>
 *   <li>Se acha + FE ≥ 200k + alguma matter ≥ 5 no perfil:
 *     <ol>
 *       <li>Drena 200k FE.</li>
 *       <li>Identifica a maior matter (DM/WM/YM).</li>
 *       <li>Tira 20% dessa matter do player profile.</li>
 *       <li>Coloca o equivalente em mB no FluidHandler vizinho:
 *           1 unidade de matter = 50 mB de fluido (4=200mB, 50=2500mB).</li>
 *       <li>Aplica Slowness II (60 ticks) no player.</li>
 *     </ol>
 *   </li>
 * </ul>
 *
 * <p>Nota: WHITE_MATTER (matéria branca) é o mesmo que CLEAR_MATTER no mod —
 * tem fluid registrado em {@link ModFluids#CLEAR_MATTER}, então também é
 * extraível normalmente como DM/YM.
 *
 * <p>Tem buffer FluidTank interno de 4000 mB pra acomodar caso a rede esteja
 * cheia momentaneamente — auto-drena pra vizinhos a cada tick.
 */
public class MatterExtractorBlockEntity extends BlockEntity implements MenuProvider {

    private static final Logger LOG = LoggerFactory.getLogger("Liberthia/MatterExtractor");

    public static final int FE_CAPACITY = 1_000_000;
    public static final int FE_PER_EXTRACTION = 200_000;
    public static final int FE_MAX_RECEIVE = 10_000;
    public static final int PERIOD = 100; // 5s
    public static final int SEARCH_RADIUS = 3;
    public static final float MIN_MATTER = 5f;
    /** Mb de fluido por ponto de matter extraída. 50 mB = drena 4 matter → 200 mB. */
    public static final int MB_PER_MATTER = 50;
    public static final int INTERNAL_TANK = 4000;
    public static final int DRAIN_RATE = 500;

    /** Estados de operação pra UX (HUD do bloco mostra). */
    public static final int STATUS_IDLE = 0;
    public static final int STATUS_NO_ENERGY = 1;
    public static final int STATUS_NO_PLAYER = 2;
    public static final int STATUS_EXTRACTING = 3;
    public static final int STATUS_TANK_FULL = 4;

    private final br.com.murilo.liberthia.energy.TrackedEnergyStorage energy =
            new br.com.murilo.liberthia.energy.TrackedEnergyStorage(this, FE_CAPACITY, FE_MAX_RECEIVE, 0);
    private final FluidTank tank = new FluidTank(INTERNAL_TANK, this::isAcceptedFluid) {
        @Override protected void onContentsChanged() { setChanged(); markUpdated(); }
    };

    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.empty();
    private LazyOptional<IFluidHandler> lazyFluid = LazyOptional.empty();

    private int status = STATUS_IDLE;
    /** Track do último player + matter extraída pra mostrar no GUI. */
    private String lastPlayerName = "";
    private int lastExtractedAmount = 0;
    private int lastFluidId = 0;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) {
            int v = switch (i) {
                case 0, 1 -> energy.getEnergyStored();
                case 2, 3 -> energy.getMaxEnergyStored();
                case 4 -> status;
                case 5 -> lastExtractedAmount;
                case 6 -> lastFluidId;
                case 7 -> tank.getFluidAmount();
                default -> 0;
            };
            return switch (i) {
                case 0, 2 -> (v >> 16) & 0xFFFF;
                case 1, 3 -> v & 0xFFFF;
                default -> v;
            };
        }
        @Override public void set(int i, int v) {}
        @Override public int getCount() { return 8; }
    };

    public MatterExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATTER_EXTRACTOR.get(), pos, state);
    }

    public int getStatus() { return status; }
    public String getLastPlayerName() { return lastPlayerName; }
    public int getLastExtractedAmount() { return lastExtractedAmount; }
    public int getLastFluidId() { return lastFluidId; }

    private boolean isAcceptedFluid(FluidStack stack) {
        if (stack.isEmpty()) return false;
        Fluid f = stack.getFluid();
        return f == ModFluids.DARK_MATTER.get()
                || f == ModFluids.CLEAR_MATTER.get()
                || f == ModFluids.YELLOW_MATTER.get();
    }

    private void markUpdated() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergy.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return lazyFluid.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyEnergy = LazyOptional.of(() -> energy);
        lazyFluid = LazyOptional.of(() -> tank);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyEnergy.invalidate();
        lazyFluid.invalidate();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.liberthia.matter_extractor");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player p) {
        return new MatterExtractorMenu(id, inv, this, this.data);
    }

    /**
     * Tick principal:
     * 1) Auto-drena o tank interno pra qualquer FluidHandler vizinho (pipe/tank).
     * 2) A cada PERIOD ticks tenta executar uma extração se tiver player+energia+matter.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, MatterExtractorBlockEntity be) {
        if (level.isClientSide) {
            tickClientParticles(level, pos, be);
            return;
        }

        // Auto-drena tank interno → vizinhos
        if (!be.tank.getFluid().isEmpty()) {
            drainToNeighbors(level, pos, be);
        }

        if (level.getGameTime() % PERIOD != 0) return;

        Player nearbyDebug = findNearbyPlayer(level, pos);
        LOG.debug("tick start at {} — FE={} (>= {} needed? {}), player nearby? {}",
                pos, be.energy.getEnergyStored(), FE_PER_EXTRACTION,
                be.energy.getEnergyStored() >= FE_PER_EXTRACTION,
                nearbyDebug != null);

        // Decide o status corrente
        if (be.energy.getEnergyStored() < FE_PER_EXTRACTION) {
            updateStatus(be, STATUS_NO_ENERGY);
            return;
        }

        Player target = nearbyDebug;
        if (target == null) {
            updateStatus(be, STATUS_NO_PLAYER);
            return;
        }

        // Verifica perfil do player
        MatterProfile profile = target.getCapability(MatterProfileProvider.CAP).orElse(null);
        if (profile == null) {
            LOG.debug("no MatterProfile capability on player {}", target.getGameProfile().getName());
            updateStatus(be, STATUS_NO_PLAYER);
            return;
        }
        LOG.debug("found player {} (UUID={}) profile WM={} DM={} YM={}",
                target.getGameProfile().getName(), target.getUUID(),
                profile.getWhite(), profile.getDark(), profile.getYellow());

        // Identifica a maior matter EXTRACTIBLE. WM (clear matter) também é
        // extraível porque tem fluid registrado (ModFluids.CLEAR_MATTER).
        char which = pickExtractTarget(profile);
        if (which == 0) {
            LOG.debug("no matter >= {} on player profile — skipping", MIN_MATTER);
            updateStatus(be, STATUS_NO_PLAYER); // sem matter extractível
            return;
        }

        // Calcula quantidade — 20% da matter selecionada
        float amount = switch (which) {
            case 'D' -> profile.getDark() * 0.20f;
            case 'W' -> profile.getWhite() * 0.20f;
            case 'Y' -> profile.getYellow() * 0.20f;
            default -> 0f;
        };
        if (amount < 1f) {
            LOG.debug("amount {} < 1 — skipping", amount);
            updateStatus(be, STATUS_NO_PLAYER);
            return;
        }
        int amountInt = Math.round(amount);
        int mb = amountInt * MB_PER_MATTER;

        // Confere espaço no tank
        Fluid fluid = switch (which) {
            case 'D' -> ModFluids.DARK_MATTER.get();
            case 'W' -> ModFluids.CLEAR_MATTER.get();
            case 'Y' -> ModFluids.YELLOW_MATTER.get();
            default -> null;
        };
        if (fluid == null) return;
        FluidStack toAdd = new FluidStack(fluid, mb);
        int canFill = be.tank.fill(toAdd, IFluidHandler.FluidAction.SIMULATE);
        LOG.debug("extracting 20% of TYPE={} amount={} (mb={}), tank can fit={}",
                which, amountInt, mb, canFill);
        if (canFill < mb) {
            updateStatus(be, STATUS_TANK_FULL);
            return;
        }

        // Executa a extração
        be.energy.extractEnergy(FE_PER_EXTRACTION, false);
        switch (which) {
            case 'D' -> profile.setDark(profile.getDark() - amount);
            case 'W' -> profile.setWhite(profile.getWhite() - amount);
            case 'Y' -> profile.setYellow(profile.getYellow() - amount);
        }
        int filled = be.tank.fill(toAdd, IFluidHandler.FluidAction.EXECUTE);
        LOG.debug("fluid output to tank successful={} (filled {} mb)", filled > 0, filled);

        // Side effect: Slowness II 3s (60 ticks)
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));

        // Sincroniza o profile pro cliente (mexer no profile servidor não auto-syncs)
        if (target instanceof net.minecraft.server.level.ServerPlayer sp) {
            br.com.murilo.liberthia.matter.MatterProfileEvents.syncTo(sp);
        }

        be.lastPlayerName = target.getGameProfile().getName();
        be.lastExtractedAmount = amountInt;
        be.lastFluidId = switch (which) { case 'D' -> 1; case 'W' -> 2; case 'Y' -> 3; default -> 0; };
        updateStatus(be, STATUS_EXTRACTING);

        // FX servidor
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    20, 0.3, 0.5, 0.3, 0.05);
            sl.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    15, 0.4, 0.2, 0.4, 0.05);
        }
    }

    private static void updateStatus(MatterExtractorBlockEntity be, int s) {
        if (be.status != s) {
            be.status = s;
            be.markUpdated();
        }
    }

    /**
     * Pega DM, WM ou YM — whichever é maior E ≥ MIN_MATTER. Retorna 0 se
     * nenhum elegível. WM (clear matter) tem fluid registrado, então também é
     * extraível.
     */
    private static char pickExtractTarget(MatterProfile profile) {
        float dm = profile.getDark();
        float wm = profile.getWhite();
        float ym = profile.getYellow();
        char best = 0;
        float bestVal = 0f;
        if (dm >= MIN_MATTER && dm >= bestVal) { best = 'D'; bestVal = dm; }
        if (wm >= MIN_MATTER && wm >= bestVal) { best = 'W'; bestVal = wm; }
        if (ym >= MIN_MATTER && ym >= bestVal) { best = 'Y'; bestVal = ym; }
        return best;
    }

    /**
     * v0.1.39 fix: SÓ retorna player se ele está EM CIMA do bloco (na pos
     * acima do extractor, no espaço dos pés). User reclamou que extractor
     * continuava drenando matter mesmo com player a 3 blocos de distância.
     *
     * <p>Aceita 2 blocos verticais de tolerância (jumping/agachando ainda
     * conta como "em cima").
     */
    private static @Nullable Player findNearbyPlayer(Level level, BlockPos pos) {
        // Área limitada: exatamente o bloco acima do extractor + 2 blocos verticais.
        // Largura horizontal = bloco inteiro (1.0), nada além disso.
        AABB box = new AABB(
                pos.getX(), pos.getY() + 1, pos.getZ(),
                pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1
        );
        List<Player> players = level.getEntitiesOfClass(Player.class, box,
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative());
        if (players.isEmpty()) return null;
        // Pega o mais próximo (no caso de múltiplos em cima — improvável)
        Player closest = players.get(0);
        double bestDist = pos.distSqr(closest.blockPosition());
        for (Player p : players) {
            double d = pos.distSqr(p.blockPosition());
            if (d < bestDist) { bestDist = d; closest = p; }
        }
        return closest;
    }

    /**
     * v0.1.38 fix: passa por TODAS as 6 direções e procura tanks/pipes vizinhos
     * (inclusive vizinhos do vizinho via pipes do mod). Antes só checava
     * vizinho IMEDIATO via FLUID_HANDLER capability — se o user colocava
     * pipes intermediários, o fluid não fluía.
     *
     * <p>Adiciona LOG de cada attempt pra debug do user: "tried fill into
     * Tank at X.Y.Z: simulated=N actual=M".
     */
    private static void drainToNeighbors(Level level, BlockPos pos, MatterExtractorBlockEntity be) {
        if (be.tank.isEmpty()) return;
        FluidStack source = be.tank.getFluid();
        if (source.isEmpty()) return;

        for (Direction d : Direction.values()) {
            BlockPos npos = pos.relative(d);
            BlockEntity nbe = level.getBlockEntity(npos);
            if (nbe == null) continue;
            LazyOptional<IFluidHandler> cap = nbe.getCapability(ForgeCapabilities.FLUID_HANDLER, d.getOpposite());
            if (!cap.isPresent()) {
                // Vizinho não expõe FLUID_HANDLER nessa direção. Talvez expõe
                // sem direção — tenta null como fallback.
                cap = nbe.getCapability(ForgeCapabilities.FLUID_HANDLER, null);
            }
            cap.ifPresent(handler -> {
                int toMove = Math.min(DRAIN_RATE, be.tank.getFluidAmount());
                if (toMove <= 0) return;
                FluidStack out = new FluidStack(source.getFluid(), toMove);
                int filled = handler.fill(out, IFluidHandler.FluidAction.SIMULATE);
                if (filled <= 0) {
                    LOG.debug("drainToNeighbors: tank {} (fluid={}) → vizinho {} ({}) REJEITOU (fill simulate=0). " +
                                    "Provavelmente tank vizinho tem fluido diferente.",
                            be.tank.getFluidAmount(), source.getFluid().getFluidType(),
                            npos, nbe.getClass().getSimpleName());
                    return;
                }
                FluidStack actual = new FluidStack(source.getFluid(), filled);
                int filledFinal = handler.fill(actual, IFluidHandler.FluidAction.EXECUTE);
                be.tank.drain(filledFinal, IFluidHandler.FluidAction.EXECUTE);
                LOG.debug("drainToNeighbors: drenou {}mB de {} pro vizinho {} ({}); tank agora {}mB",
                        filledFinal, source.getFluid().getFluidType(), npos,
                        nbe.getClass().getSimpleName(), be.tank.getFluidAmount());
            });
        }
    }

    private static void tickClientParticles(Level level, BlockPos pos, MatterExtractorBlockEntity be) {
        // Runa pulsante simples — emite partículas se tem energia suficiente
        if (be.energy.getEnergyStored() < FE_PER_EXTRACTION) return;
        if (level.random.nextFloat() < 0.3f) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double r = 0.3;
            level.addParticle(ParticleTypes.PORTAL,
                    pos.getX() + 0.5 + Math.cos(a) * r,
                    pos.getY() + 1.0 + level.random.nextDouble() * 0.3,
                    pos.getZ() + 0.5 + Math.sin(a) * r,
                    0, 0.03, 0);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("energy", energy.serializeNBT());
        tag.put("tank", tank.writeToNBT(new CompoundTag()));
        tag.putInt("status", status);
        tag.putString("lastPlayer", lastPlayerName);
        tag.putInt("lastAmount", lastExtractedAmount);
        tag.putInt("lastFluidId", lastFluidId);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("energy")) energy.deserializeNBT(tag.get("energy"));
        if (tag.contains("tank")) tank.readFromNBT(tag.getCompound("tank"));
        status = tag.getInt("status");
        lastPlayerName = tag.getString("lastPlayer");
        lastExtractedAmount = tag.getInt("lastAmount");
        lastFluidId = tag.getInt("lastFluidId");
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
