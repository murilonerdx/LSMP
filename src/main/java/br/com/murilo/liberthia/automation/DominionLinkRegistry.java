package br.com.murilo.liberthia.automation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.24 r88: Registry global de links Dominion (source → target).
 *
 * <p>Em memória only — não persiste entre server restarts. Cada link liga
 * uma BlockPos source a uma BlockPos target. Quando source emite redstone
 * (ex.: SpellSensor pulses), os linked targets também recebem.
 *
 * <p>Layout: {@code Map<DimensionKey, Map<SourcePos, Set<TargetPos>>>}.
 */
public final class DominionLinkRegistry {

    private static final Map<ResourceKey<Level>, Map<BlockPos, Set<BlockPos>>> LINKS = new ConcurrentHashMap<>();

    private DominionLinkRegistry() {}

    public static void addLink(ResourceKey<Level> dim, BlockPos source, BlockPos target) {
        LINKS.computeIfAbsent(dim, k -> new ConcurrentHashMap<>())
             .computeIfAbsent(source, k -> ConcurrentHashMap.newKeySet())
             .add(target);
    }

    public static void removeLink(ResourceKey<Level> dim, BlockPos source, BlockPos target) {
        var map = LINKS.get(dim);
        if (map == null) return;
        var targets = map.get(source);
        if (targets == null) return;
        targets.remove(target);
    }

    public static Set<BlockPos> getTargets(ResourceKey<Level> dim, BlockPos source) {
        var map = LINKS.get(dim);
        if (map == null) return java.util.Collections.emptySet();
        return map.getOrDefault(source, java.util.Collections.emptySet());
    }

    /** Notifica targets que source disparou. Chama callback em cada target. */
    public static void propagatePulse(Level level, BlockPos source) {
        Set<BlockPos> targets = getTargets(level.dimension(), source);
        for (BlockPos target : targets) {
            if (!level.isLoaded(target)) continue;
            var state = level.getBlockState(target);
            if (state.getBlock() instanceof SpellTurretBlock) {
                if (level.getBlockEntity(target) instanceof SpellTurretBlockEntity be) {
                    be.fire();
                }
            }
            // Outros tipos de target podem ser adicionados aqui
        }
    }
}
