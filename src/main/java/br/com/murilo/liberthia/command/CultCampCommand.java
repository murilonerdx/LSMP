package br.com.murilo.liberthia.command;

import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * /liberthia cultcamp [abandoned]
 * Builds a small Cult Camp structure around the executor. Because we don't
 * ship a Jigsaw structure, this command is the officially supported way to
 * spawn a camp in-world for Fase 1.
 *
 *  - default camp: 1 Blood Altar, 4 Chalk Symbols, 3 Cultists, 1 Priest.
 *  - abandoned variant: Blood Volcano instead of altar, 1 WoundedPilgrim,
 *    no live priest/cultists, richer chest (not implemented — chest placed
 *    but left empty; future Fase will fill via loot table).
 */
public class CultCampCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("liberthia")
                .then(Commands.literal("cultcamp")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> build(ctx.getSource(), false))
                        .then(Commands.literal("abandoned")
                                .executes(ctx -> build(ctx.getSource(), true)))
                )
        );
    }

    private static int build(CommandSourceStack src, boolean abandoned) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = src.getLevel();
        BlockPos center = player.blockPosition();

        // Clear a 11x6x11 area
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = 0; dy < 5; dy++) {
                    level.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        // Soul campfire centerpiece (shifted)
        level.setBlockAndUpdate(center.offset(3, 0, 3), Blocks.SOUL_CAMPFIRE.defaultBlockState());

        // Altar or volcano at center
        BlockPos altarPos = center.offset(0, 0, 0);
        BlockState altarState = abandoned
                ? ModBlocks.BLOOD_VOLCANO.get().defaultBlockState()
                : ModBlocks.BLOOD_ALTAR.get().defaultBlockState();
        level.setBlockAndUpdate(altarPos, altarState);

        // 4 Chalk symbols in a diamond around altar (not containing — 4 chalks
        // would make it contained; we place 3 so the altar stays active).
        if (!abandoned) {
            level.setBlockAndUpdate(altarPos.offset(3, 0, 0),
                    ModBlocks.CHALK_SYMBOL.get().defaultBlockState());
            level.setBlockAndUpdate(altarPos.offset(-3, 0, 0),
                    ModBlocks.CHALK_SYMBOL.get().defaultBlockState());
            level.setBlockAndUpdate(altarPos.offset(0, 0, 3),
                    ModBlocks.CHALK_SYMBOL.get().defaultBlockState());
        }

        // 3 wool tents (corners): black wool 2x2
        placeTent(level, center.offset(-4, 1, -4));
        placeTent(level, center.offset(4, 1, -4));
        placeTent(level, center.offset(-4, 1, 4));

        // Chest (empty placeholder for now)
        level.setBlockAndUpdate(center.offset(4, 1, 4), Blocks.CHEST.defaultBlockState());

        Random rng = new Random();
        if (abandoned) {
            // WoundedPilgrim spawns alone in the camp
            Entity pilgrim = ModEntities.WOUNDED_PILGRIM.get().create(level);
            if (pilgrim != null) {
                pilgrim.moveTo(center.getX() + 1.5, center.getY() + 1, center.getZ() + 1.5, 0, 0);
                level.addFreshEntity(pilgrim);
            }
        } else {
            // Priest at center-adjacent
            Entity priest = ModEntities.BLOOD_PRIEST.get().create(level);
            if (priest != null) {
                priest.moveTo(center.getX() + 0.5, center.getY() + 1, center.getZ() + 1.5, 0, 0);
                level.addFreshEntity(priest);
            }
            // 3 cultists scattered
            for (int i = 0; i < 3; i++) {
                Entity c = ModEntities.BLOOD_CULTIST.get().create(level);
                if (c != null) {
                    double ox = (rng.nextDouble() - 0.5) * 6;
                    double oz = (rng.nextDouble() - 0.5) * 6;
                    c.moveTo(center.getX() + 0.5 + ox, center.getY() + 1, center.getZ() + 0.5 + oz,
                            rng.nextFloat() * 360F, 0);
                    level.addFreshEntity(c);
                }
            }
        }

        String msg = abandoned
                ? "§4[Liberthia] §fAcampamento abandonado construído."
                : "§4[Liberthia] §fAcampamento do Culto construído.";
        src.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static void placeTent(ServerLevel level, BlockPos base) {
        BlockState wool = Blocks.BLACK_WOOL.defaultBlockState();
        // 3x3 base with open front
        level.setBlockAndUpdate(base, wool);
        level.setBlockAndUpdate(base.offset(1, 0, 0), wool);
        level.setBlockAndUpdate(base.offset(0, 0, 1), wool);
        level.setBlockAndUpdate(base.offset(0, 1, 0), wool);
        level.setBlockAndUpdate(base.offset(1, 1, 0), wool);
        level.setBlockAndUpdate(base.offset(0, 1, 1), wool);
    }
}
