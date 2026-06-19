package br.com.murilo.liberthia.observation.block;

import br.com.murilo.liberthia.observation.api.ObservationResolver;
import br.com.murilo.liberthia.observation.api.ObservationSpell;
import br.com.murilo.liberthia.observation.item.SpellParchmentItem;
import br.com.murilo.liberthia.observation.source.SourceData;
import br.com.murilo.liberthia.observation.source.SourceJarTile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * v0.1.22 r72: <b>Rune Block</b> — bloco achatado no chão que dispara feitiço
 * quando entity passa em cima.
 *
 * <p>Pattern AN's RuneBlock:
 * <ul>
 *   <li>Sem collision, achatado 1/16 do block</li>
 *   <li>NBT armazena recipe de spell</li>
 *   <li>onStep: cast spell, consume Source de Source Jar vizinho (8b)</li>
 * </ul>
 *
 * <h2>Interação</h2>
 * <ul>
 *   <li>Sneak+rclick com Parchment offhand: imprinta spell na runa</li>
 *   <li>Rclick: mostra info da runa</li>
 *   <li>Walk on: trigger</li>
 * </ul>
 */
public class RuneBlock extends BaseEntityBlock {

    /** Cooldown entre triggers (mesmo entity não pode triggar 2x seguidos). */
    public static final int TRIGGER_COOLDOWN = 60;

    private static final VoxelShape SHAPE =
        Block.box(0, 0, 0, 16, 1, 16);

    public RuneBlock(Properties props) {
        super(props);
    }

    @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RuneBlockEntity(pos, state);
    }

    /** Sneak+rclick com Parchment: imprinta. Senão: mostra info. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof RuneBlockEntity rune)) return InteractionResult.PASS;

        ItemStack offhand = player.getOffhandItem();

        // Sneak + parchment offhand = imprint
        if (player.isShiftKeyDown() && offhand.getItem() instanceof SpellParchmentItem) {
            var recipe = SpellParchmentItem.getRecipe(offhand);
            if (recipe.isEmpty()) {
                sp.displayClientMessage(Component.literal("§c⚠ Pergaminho vazio."), true);
                return InteractionResult.FAIL;
            }
            var spell = SpellParchmentItem.buildSpell(offhand);
            if (spell == null || spell.validate() != null) {
                sp.displayClientMessage(Component.literal("§c⚠ Recipe inválida."), true);
                return InteractionResult.FAIL;
            }
            rune.setSpellRecipe(recipe);
            rune.setSpellName(spell.name());
            rune.setSpellColor(spell.color());
            sp.displayClientMessage(Component.literal(
                "§5§l✦ Runa inscrita: §r§e" + spell.name()), true);
            return InteractionResult.CONSUME;
        }

        // Show info
        if (rune.hasSpell()) {
            sp.displayClientMessage(Component.literal(
                "§5Runa: §e" + rune.getSpellName() + " §7(" + rune.getSpellRecipe().size() + " glyphs)"), true);
        } else {
            sp.displayClientMessage(Component.literal(
                "§7§oRuna vazia. Shift+rclick com Pergaminho pra inscrever."), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** Step on rune: trigger spell. */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer sp)) return;
        if (!(level.getBlockEntity(pos) instanceof RuneBlockEntity rune)) return;
        if (!rune.hasSpell()) return;
        // Cooldown per entity per rune
        if (rune.isOnCooldownFor(entity.getUUID(), level.getGameTime())) return;
        rune.setTriggeredBy(entity.getUUID(), level.getGameTime());

        // Build spell
        ObservationSpell spell = rune.buildSpell();
        if (spell == null) return;

        // Consome Source: 1) procura SourceJar em 8b, 2) fallback player Source
        int cost = Math.max(1, spell.totalSourceCost() / 2);
        boolean paid = false;
        // Look for jar in 8 blocks
        for (BlockPos jarPos : BlockPos.betweenClosed(pos.offset(-8, -2, -8), pos.offset(8, 2, 8))) {
            if (level.getBlockEntity(jarPos) instanceof SourceJarTile jar && jar.getStored() >= cost) {
                if (jar.tryExtract(cost)) {
                    paid = true;
                    break;
                }
            }
        }
        if (!paid) {
            // Fallback: player source
            if (SourceData.consume(sp, cost)) {
                paid = true;
            }
        }
        if (!paid) {
            sp.displayClientMessage(Component.literal(
                "§c⚠ Runa sem Source pra ativar."), true);
            return;
        }

        // Cast spell — usa o player como caster
        ObservationResolver.cast(sp, spell);
        sp.displayClientMessage(Component.literal(
            "§5§l✦ Runa ativada §r§e" + spell.name()), true);

        // VFX
        if (level instanceof ServerLevel sl) {
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI * 2 / 16;
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5 + Math.cos(a) * 0.5,
                    pos.getY() + 0.2,
                    pos.getZ() + 0.5 + Math.sin(a) * 0.5,
                    1, 0.0, 0.05, 0.0, 0.0);
            }
        }
    }
}
