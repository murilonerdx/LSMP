package br.com.murilo.liberthia.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * r92: Potion Jar — armazena 10k mB de 1 potion.
 */
public class PotionJarBlock extends Block implements EntityBlock {

    public PotionJarBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PotionJarBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof PotionJarBlockEntity be)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);

        // ─── r173: SOURCE (líquido roxo) é o uso principal do jarro ───
        // Source Gem → deposita Source no jarro (+50).
        if (held.is(br.com.murilo.liberthia.registry.ModItems.SOURCE_GEM.get())) {
            if (be.getAmount() > 0) {
                player.displayClientMessage(Component.literal("§c⚠ Jarro contém poção — esvazie primeiro."), true);
                return InteractionResult.FAIL;
            }
            int added = be.addSource(50);
            if (added > 0) {
                if (!player.isCreative()) held.shrink(1);
                player.displayClientMessage(Component.literal(
                        "§5§l+" + added + " Source §r§7→ §dJarro§r §7(" + be.getSource() + "/" + be.getSourceCapacity() + ")"), true);
                return InteractionResult.CONSUME;
            }
            player.displayClientMessage(Component.literal("§c⚠ Jarro de Source cheio."), true);
            return InteractionResult.FAIL;
        }

        // Mão vazia + jarro tem Source → transfere pro player (que tenha Source < max).
        if (held.isEmpty() && be.getSource() > 0
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            int cur = br.com.murilo.liberthia.observation.source.SourceData.get(sp);
            int max = br.com.murilo.liberthia.observation.source.SourceData.getMax(sp);
            if (cur < max) {
                int want = Math.min(50, max - cur);
                int drawn = be.drawSource(want);
                if (drawn > 0) {
                    br.com.murilo.liberthia.observation.source.SourceData.add(sp, drawn);
                    player.displayClientMessage(Component.literal(
                            "§e§l✦ §r§5+" + drawn + " Source§r §7→ §eVocê§r §7(" + br.com.murilo.liberthia.observation.source.SourceData.get(sp) + "/" + max + ")"), true);
                    return InteractionResult.CONSUME;
                }
            }
        }

        // INSERT potion (só se não tiver Source dentro)
        if (held.getItem() == Items.POTION && be.getSource() == 0) {
            var pot = PotionUtils.getPotion(held);
            if (be.addPotion(pot)) {
                if (!player.isCreative()) {
                    held.shrink(1);
                    player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
                }
                player.displayClientMessage(Component.literal(
                        "§5✦ Potion armazenado §7(" + be.getAmount() + "/" + PotionJarBlockEntity.CAPACITY + ")"), true);
                return InteractionResult.CONSUME;
            }
        }

        // EXTRACT potion
        if (held.getItem() == Items.GLASS_BOTTLE && be.getAmount() >= PotionJarBlockEntity.POTION_AMOUNT) {
            var pot = be.drawPotion();
            if (pot != null) {
                ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.POTION), pot);
                if (!player.isCreative()) held.shrink(1);
                player.getInventory().placeItemBackInInventory(potion);
                player.displayClientMessage(Component.literal(
                        "§5✦ Potion extraído §7(" + be.getAmount() + "/" + PotionJarBlockEntity.CAPACITY + ")"), true);
                return InteractionResult.CONSUME;
            }
        }

        // INFO
        if (be.getSource() > 0) {
            player.displayClientMessage(Component.literal(
                    "§d✦ §5" + be.getSource() + "§7/§5" + be.getSourceCapacity() + " §dSource"), true);
        } else {
            player.displayClientMessage(Component.literal(
                    "§5✦ §b" + be.getAmount() + "/" + PotionJarBlockEntity.CAPACITY + " §5mB §7(use Source Gem pra encher de Source)"), true);
        }
        return InteractionResult.CONSUME;
    }
}
