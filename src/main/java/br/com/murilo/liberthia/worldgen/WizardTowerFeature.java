package br.com.murilo.liberthia.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

/**
 * v0.1.162 r138: <b>WizardTowerFeature</b> — torre mágica procedural 5×5×12.
 *
 * <p>Layout (de baixo pra cima):
 * <ul>
 *   <li>Floor 0-1: chão de stone bricks + sala interna 3×3</li>
 *   <li>Floor 2-3: corredor + portas</li>
 *   <li>Floor 4-7: paredes amethyst com janelas + Source Jar interior</li>
 *   <li>Floor 8-9: room mágica (Scribes Table + Lectern com livro)</li>
 *   <li>Floor 10-12: roof cone amethyst + chest no topo com loot</li>
 * </ul>
 *
 * <p>Loot do chest:
 * <ul>
 *   <li>2-4 Spell Parchments</li>
 *   <li>1-2 Source Crystals</li>
 *   <li>3-6 random Glyphs</li>
 *   <li>Maybe 1 Spell Scroll random</li>
 *   <li>Sometimes 1 Caster Wand vazia</li>
 * </ul>
 */
public class WizardTowerFeature extends Feature<NoneFeatureConfiguration> {

    public WizardTowerFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        var random = ctx.random();

        // Find solid ground
        BlockPos ground = findGround(level, origin);
        if (ground == null) return false;

        // Validate footprint clearance — base 5x5 should be relatively flat
        int variance = computeVariance(level, ground);
        if (variance > 3) return false; // too uneven

        // Place tower
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState bud = Blocks.AMETHYST_CLUSTER.defaultBlockState();
        BlockState glass = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
        BlockState lantern = Blocks.LANTERN.defaultBlockState();

        // Floors 0-1: solid stone base
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(ground.offset(dx, 0, dz),
                        random.nextFloat() < 0.3F ? mossy : stone, 2);
                level.setBlock(ground.offset(dx, 1, dz), stone, 2);
            }
        }
        // Hollow inner 3x3 at y=1
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(ground.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 2);
            }
        }

        // Floors 2-9: hollow tower with walls 5x5
        for (int floor = 2; floor <= 9; floor++) {
            BlockState wall = floor <= 5 ? (random.nextFloat() < 0.4F ? mossy : stone) : amethyst;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                        // Wall position
                        BlockState block = wall;
                        // Window at floor 3,6 - 1 per side
                        if ((floor == 3 || floor == 6) && (dx == 0 || dz == 0) && !(dx == 0 && dz == 0)) {
                            block = glass;
                        }
                        level.setBlock(ground.offset(dx, floor, dz), block, 2);
                    } else {
                        // Interior
                        level.setBlock(ground.offset(dx, floor, dz),
                                Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Floor 0 (top of base) — door way on north
        level.setBlock(ground.offset(0, 1, -2), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(ground.offset(0, 2, -2), Blocks.AIR.defaultBlockState(), 2);

        // Lanterns at corners floor 4 + 8
        for (int floor : new int[]{4, 8}) {
            for (int corner : new int[]{-1, 1}) {
                level.setBlock(ground.offset(corner, floor + 1, -1), lantern, 2);
                level.setBlock(ground.offset(corner, floor + 1, 1), lantern, 2);
            }
        }

        // Floor 8-9: magic room — bookshelves at corners + lectern
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                level.setBlock(ground.offset(dx, 8, dz),
                        Blocks.BOOKSHELF.defaultBlockState(), 2);
            }
        }
        // Lectern centro
        BlockState lecternState = Blocks.LECTERN.defaultBlockState()
                .setValue(LecternBlock.FACING, Direction.NORTH)
                .setValue(LecternBlock.HAS_BOOK, true);
        level.setBlock(ground.offset(0, 8, 0), lecternState, 2);
        // Coloca book mágico no lectern
        BlockEntity lecternBE = level.getBlockEntity(ground.offset(0, 8, 0));
        if (lecternBE instanceof LecternBlockEntity le) {
            le.setBook(new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK));
        }

        // Roof cone at floors 10-12
        for (int floor = 10; floor <= 12; floor++) {
            int shrink = floor - 9;
            int half = 2 - shrink;
            if (half < 0) {
                // Apex single block
                level.setBlock(ground.offset(0, floor, 0), amethyst, 2);
                continue;
            }
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    if (Math.abs(dx) == half || Math.abs(dz) == half) {
                        level.setBlock(ground.offset(dx, floor, dz), amethyst, 2);
                    }
                }
            }
        }

        // Amethyst buds decorativos no topo
        if (level.getBlockState(ground.offset(0, 11, 0)).isAir()) {
            level.setBlock(ground.offset(0, 11, 0), bud, 2);
        }

        // Chest no centro do floor 9 com loot
        BlockState chest = Blocks.CHEST.defaultBlockState();
        BlockPos chestPos = ground.offset(0, 9, 0);
        level.setBlock(chestPos, chest, 2);
        BlockEntity chestBE = level.getBlockEntity(chestPos);
        if (chestBE instanceof ChestBlockEntity cbe) {
            populateChest(cbe, random);
        }

        // Spawn 1 wizard mob ao redor (random arquetype)
        spawnWizard(level, ground.offset(0, 2, 0), random);

        return true;
    }

    private BlockPos findGround(WorldGenLevel level, BlockPos near) {
        BlockPos top = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, near);
        // Go down till solid
        BlockPos pos = top;
        for (int i = 0; i < 32; i++) {
            BlockState s = level.getBlockState(pos.below());
            if (s.isSolid() && !s.is(Blocks.WATER) && !s.is(Blocks.LAVA)) {
                return pos;
            }
            pos = pos.below();
        }
        return null;
    }

    private int computeVariance(LevelAccessor level, BlockPos center) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos top = level.getHeightmapPos(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                        center.offset(dx, 0, dz));
                int y = top.getY();
                if (y < min) min = y;
                if (y > max) max = y;
            }
        }
        return max - min;
    }

    private void populateChest(ChestBlockEntity chest, net.minecraft.util.RandomSource random) {
        var modItems = br.com.murilo.liberthia.registry.ModItems.class;
        // Helper: pick a random RegistryObject<Item> by name via reflection — slow but only 1× per gen
        // Pra simplicidade vou referenciar items conhecidos diretamente:
        java.util.List<net.minecraft.world.item.Item> glyphs = new java.util.ArrayList<>();
        glyphs.add(br.com.murilo.liberthia.registry.ModItems.GLYPH_DIRECT_GAZE.get());
        glyphs.add(br.com.murilo.liberthia.registry.ModItems.GLYPH_TENDRIL.get());
        glyphs.add(br.com.murilo.liberthia.registry.ModItems.GLYPH_AMPLIFY.get());
        glyphs.add(br.com.murilo.liberthia.registry.ModItems.GLYPH_LINGER.get());
        glyphs.add(br.com.murilo.liberthia.registry.ModItems.GLYPH_EFFECT_HEAL.get());
        glyphs.add(br.com.murilo.liberthia.registry.ModItems.GLYPH_EFFECT_IGNITE.get());
        glyphs.add(br.com.murilo.liberthia.registry.ModItems.GLYPH_EFFECT_FREEZE.get());

        int slot = 0;
        // 2-4 Spell Parchments
        chest.setItem(slot++, new ItemStack(
                br.com.murilo.liberthia.registry.ModItems.SPELL_PARCHMENT.get(),
                2 + random.nextInt(3)));
        // 1-2 Source Crystals
        chest.setItem(slot++, new ItemStack(
                br.com.murilo.liberthia.registry.ModItems.SOURCE_CRYSTAL.get(),
                1 + random.nextInt(2)));
        // 1 Source Catalyst
        if (random.nextFloat() < 0.6F) {
            chest.setItem(slot++, new ItemStack(
                    br.com.murilo.liberthia.registry.ModItems.SOURCE_CATALYST.get(), 2));
        }
        // 3-6 random glyphs
        int glyphCount = 3 + random.nextInt(4);
        for (int i = 0; i < glyphCount; i++) {
            var glyph = glyphs.get(random.nextInt(glyphs.size()));
            chest.setItem(slot++, new ItemStack(glyph, 1));
            if (slot >= 27) break;
        }
        // 1 caster wand (40% chance)
        if (random.nextFloat() < 0.4F && slot < 27) {
            chest.setItem(slot++, new ItemStack(
                    br.com.murilo.liberthia.registry.ModItems.CASTER_WAND.get(), 1));
        }
        // 1 random spell scroll (Fireball/Frostbolt/Heal — common)
        if (slot < 27) {
            net.minecraft.world.item.Item scroll;
            int pick = random.nextInt(3);
            switch (pick) {
                case 0 -> scroll = br.com.murilo.liberthia.registry.ModItems.SPELL_FIREBALL.get();
                case 1 -> scroll = br.com.murilo.liberthia.registry.ModItems.SPELL_FROSTBOLT.get();
                default -> scroll = br.com.murilo.liberthia.registry.ModItems.SPELL_GREATER_HEAL.get();
            }
            chest.setItem(slot++, new ItemStack(scroll, 1));
        }
    }

    private void spawnWizard(WorldGenLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        // Spawn um Witch como guardião default da torre (mais simples que escolher wizard custom)
        // O Wizard custom precisaria estar registrado e ter spawn rules — Witch é vanilla seguro.
        try {
            net.minecraft.world.entity.monster.Witch wiz = EntityType.WITCH.create(level.getLevel());
            if (wiz != null) {
                wiz.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                        random.nextFloat() * 360F, 0);
                wiz.finalizeSpawn(level, level.getCurrentDifficultyAt(pos),
                        MobSpawnType.STRUCTURE, null, null);
                level.addFreshEntity(wiz);
            }
        } catch (Throwable t) {
            // soft fail
        }
    }
}
