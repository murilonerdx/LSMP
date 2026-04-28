package br.com.murilo.liberthia.ritual;

import br.com.murilo.liberthia.block.entity.BloodSacrificialBowlBlockEntity;
import br.com.murilo.liberthia.block.entity.GoldenBloodBowlBlockEntity;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Occultism-inspired blood ritual.
 *
 * Lifecycle: {@link #isValid} → {@link #start} → {@link #update} every tick →
 * {@link #finish} OR {@link #interrupt}.
 *
 * <p>Pentacle is a hardcoded list of relative offsets — each offset must hold
 * the configured anchor block (default: {@link ModBlocks#BLOOD_STONE}).
 *
 * <p>Ingredients are consumed from {@link BloodSacrificialBowlBlockEntity}s
 * within {@link #BOWL_SCAN_RADIUS} blocks horizontally.
 */
public abstract class BloodRitual {

    /** Horizontal radius the central bowl scans for ingredient bowls. */
    public static final int BOWL_SCAN_RADIUS = 4;

    private final String id;
    private final List<BlockPos> pentacleOffsets;
    private final List<Item> ingredients;
    private final Item activator;
    private final int totalDurationTicks;

    protected BloodRitual(String id,
                          List<BlockPos> pentacleOffsets,
                          List<Item> ingredients,
                          Item activator,
                          int totalDurationTicks) {
        this.id = id;
        this.pentacleOffsets = pentacleOffsets;
        this.ingredients = ingredients;
        this.activator = activator;
        this.totalDurationTicks = totalDurationTicks;
    }

    public String id() { return id; }
    public Item activator() { return activator; }
    public int durationTicks() { return totalDurationTicks; }
    public List<Item> ingredients() { return ingredients; }
    public List<BlockPos> pentacleOffsets() { return pentacleOffsets; }

    /** True if pentacle blocks are placed AND all ingredients are present in nearby bowls. */
    public boolean isValid(ServerLevel level, BlockPos centre) {
        if (!isPentacleValid(level, centre)) return false;
        return findIngredientBowls(level, centre).size() >= ingredients.size();
    }

    /** Override to do extra setup at ritual start (sounds, particles, BossBar, etc). */
    public void start(ServerLevel level, BlockPos centre, GoldenBloodBowlBlockEntity ritualBE,
                      @Nullable ServerPlayer caster) {
        if (caster != null) {
            caster.displayClientMessage(
                    Component.literal("§4[" + id + "]§r ritual iniciado.").withStyle(s -> s),
                    true);
        }
    }

    /** Tick callback. Default: no-op. */
    public void update(ServerLevel level, BlockPos centre, GoldenBloodBowlBlockEntity ritualBE,
                       @Nullable ServerPlayer caster, int time) {}

    /** Override to spawn entities, drop items, apply effects, etc. */
    public abstract void finish(ServerLevel level, BlockPos centre, GoldenBloodBowlBlockEntity ritualBE,
                                @Nullable ServerPlayer caster);

    /** Override for interrupt cleanup (sound, message). */
    public void interrupt(ServerLevel level, BlockPos centre, GoldenBloodBowlBlockEntity ritualBE,
                          @Nullable ServerPlayer caster, String reason) {
        if (caster != null) {
            caster.displayClientMessage(
                    Component.literal("§c[" + id + "]§r interrompido: " + reason),
                    true);
        }
    }

    // ---------------------------------------------------------------- pentacle / bowl helpers

    public boolean isPentacleValid(ServerLevel level, BlockPos centre) {
        for (BlockPos off : pentacleOffsets) {
            BlockPos check = centre.offset(off);
            var bs = level.getBlockState(check);
            // Either a blood_stone OR a chalk_glyph is acceptable as anchor.
            boolean ok = bs.is(ModBlocks.BLOOD_STONE.get())
                    || (ModBlocks.CHALK_GLYPH != null
                        && ModBlocks.CHALK_GLYPH.isPresent()
                        && bs.is(ModBlocks.CHALK_GLYPH.get()));
            if (!ok) return false;
        }
        return true;
    }

    public List<BloodSacrificialBowlBlockEntity> findIngredientBowls(ServerLevel level, BlockPos centre) {
        List<BloodSacrificialBowlBlockEntity> out = new ArrayList<>();
        int r = BOWL_SCAN_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos p = centre.offset(dx, dy, dz);
                    if (level.getBlockEntity(p) instanceof BloodSacrificialBowlBlockEntity bowl
                            && !bowl.isEmpty()) {
                        out.add(bowl);
                    }
                }
            }
        }
        return out;
    }

    /** Tries to consume one matching item. Returns true if found and consumed. */
    public boolean consumeOneIngredient(ServerLevel level, BlockPos centre, Item want) {
        for (BloodSacrificialBowlBlockEntity bowl : findIngredientBowls(level, centre)) {
            ItemStack held = bowl.getItem();
            if (held.is(want)) {
                bowl.consumeOne();
                return true;
            }
        }
        return false;
    }
}
