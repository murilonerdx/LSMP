package br.com.murilo.liberthia.occult;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModBlockEntities;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * v0.1.22 r32: BE do Spirit Miner.
 *
 * <p><b>Como funciona:</b>
 * <ul>
 *   <li>Procura um inventário ADJACENTE (6 direções) com BoundFoliotCrystal</li>
 *   <li>Se achar, drena 1 charge do crystal a cada 100 ticks (5s)</li>
 *   <li>Minera o bloco abaixo da posição atual (varre vertical down)</li>
 *   <li>Drops vão pro inventário adjacente OU pro chão se cheio</li>
 *   <li>Quando atinge bedrock/void, reseta pra coluna seguinte</li>
 *   <li>Crystal tem 1000 charges; quando 0 → desliga até recarregar</li>
 * </ul>
 */
public class SpiritMinerBlockEntity extends BlockEntity {

    public static final int TICK_INTERVAL = 100; // 5s
    public static final int MAX_DEPTH = 64;

    private int offsetX = 0;
    private int offsetZ = 0;
    private int currentDepth = 1;
    private boolean active = false;

    public SpiritMinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPIRIT_MINER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   SpiritMinerBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        if (level.getGameTime() % TICK_INTERVAL != 0) return;

        // Acha inv adjacente com crystal
        ItemStack crystal = findCrystal(level, pos);
        if (crystal.isEmpty()) {
            be.active = false;
            return;
        }
        // Drena charge
        int charges = crystal.getOrCreateTag().getInt("Charges");
        if (charges <= 0) {
            be.active = false;
            return;
        }
        crystal.getTag().putInt("Charges", charges - 1);

        // Minera bloco em offset
        int x = pos.getX() + be.offsetX;
        int y = pos.getY() - be.currentDepth;
        int z = pos.getZ() + be.offsetZ;
        BlockPos target = new BlockPos(x, y, z);
        BlockState targetState = level.getBlockState(target);

        if (!targetState.isAir() && targetState.getDestroySpeed(level, target) >= 0
                && !targetState.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE)) {
            // Quebra + drops
            level.destroyBlock(target, false);
            var drops = net.minecraft.world.level.block.Block.getDrops(targetState, sl, target, null);
            // Tenta inserir no mesmo inventário do crystal
            IItemHandler inv = findInventory(level, pos);
            for (ItemStack drop : drops) {
                if (inv != null) {
                    for (int slot = 0; slot < inv.getSlots(); slot++) {
                        drop = inv.insertItem(slot, drop, false);
                        if (drop.isEmpty()) break;
                    }
                }
                if (!drop.isEmpty()) {
                    // Cai no chão acima do miner
                    var ie = new ItemEntity(level,
                            pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, drop);
                    level.addFreshEntity(ie);
                }
            }
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                    8, 0.3, 0.3, 0.3, 0.05);
            sl.playSound(null, target, SoundEvents.STONE_BREAK,
                    SoundSource.BLOCKS, 0.3F, 1.5F);
            be.active = true;
        }

        // Avança pos (espiral 3x3 → 5x5 → expand)
        be.advancePosition();
        be.setChanged();
    }

    private void advancePosition() {
        currentDepth++;
        if (currentDepth > MAX_DEPTH) {
            currentDepth = 1;
            // Próxima posição em padrão espiral pequeno (3x3)
            offsetX++;
            if (offsetX > 1) {
                offsetX = -1;
                offsetZ++;
                if (offsetZ > 1) {
                    offsetZ = -1;
                }
            }
        }
    }

    private static ItemStack findCrystal(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(d));
            if (be == null) continue;
            var capOpt = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).resolve();
            if (capOpt.isEmpty()) continue;
            IItemHandler inv = capOpt.get();
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s.is(ModItems.BOUND_FOLIOT_CRYSTAL.get())) return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static IItemHandler findInventory(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(d));
            if (be == null) continue;
            var capOpt = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).resolve();
            if (capOpt.isPresent()) return capOpt.get();
        }
        return null;
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("OffsetX", offsetX);
        tag.putInt("OffsetZ", offsetZ);
        tag.putInt("Depth", currentDepth);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        offsetX = tag.getInt("OffsetX");
        offsetZ = tag.getInt("OffsetZ");
        currentDepth = tag.getInt("Depth");
        if (currentDepth == 0) currentDepth = 1;
    }
}
