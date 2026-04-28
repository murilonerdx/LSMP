package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.ChalkGlyphBlock;
import br.com.murilo.liberthia.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Right-click on a sturdy floor to draw a {@link ChalkGlyphBlock}. Subsequent
 * clicks on the SAME glyph cycle its {@code SIGN} property so the player can
 * pick the typeface they want without spamming inventory items.
 */
public class BloodChalkItem extends Item {
    public BloodChalkItem(Properties props) {
        super(props.durability(64));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockState clickedState = level.getBlockState(clicked);

        // Cycle SIGN if clicking an existing glyph.
        if (clickedState.is(ModBlocks.CHALK_GLYPH.get())) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            int next = (clickedState.getValue(ChalkGlyphBlock.SIGN) + 1)
                    % (ChalkGlyphBlock.MAX_SIGN + 1);
            level.setBlockAndUpdate(clicked, clickedState.setValue(ChalkGlyphBlock.SIGN, next));
            level.playSound(null, clicked, SoundEvents.AMETHYST_BLOCK_HIT,
                    SoundSource.PLAYERS, 0.5F, 1.6F);
            return InteractionResult.CONSUME;
        }

        // Otherwise place a new glyph above the clicked face if it's UP.
        BlockPos placeAt = (face == Direction.UP) ? clicked.above() : clicked.relative(face);
        BlockState atPlace = level.getBlockState(placeAt);
        if (!atPlace.isAir() && !atPlace.canBeReplaced()) return InteractionResult.PASS;

        BlockState floor = level.getBlockState(placeAt.below());
        if (!floor.isFaceSturdy(level, placeAt.below(), Direction.UP)) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.SUCCESS;

        int sign = level.random.nextInt(ChalkGlyphBlock.MAX_SIGN + 1);
        level.setBlockAndUpdate(placeAt,
                ModBlocks.CHALK_GLYPH.get().defaultBlockState()
                        .setValue(ChalkGlyphBlock.SIGN, sign));
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    placeAt.getX() + 0.5, placeAt.getY() + 0.05, placeAt.getZ() + 0.5,
                    8, 0.3, 0.05, 0.3, 0.01);
        }
        level.playSound(null, placeAt, SoundEvents.GRAVEL_PLACE,
                SoundSource.PLAYERS, 0.5F, 1.4F);

        ItemStack stack = ctx.getItemInHand();
        if (!player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(ctx.getHand()));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level l, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal("§7Desenha um glifo de sangue no chão."));
        tip.add(Component.literal("§7Clique em um glifo existente para alternar o desenho."));
        tip.add(Component.literal("§oUsado nos pentacles dos rituais."));
    }
}
