package br.com.murilo.liberthia.logic;

import br.com.murilo.liberthia.faction.Faction;
import br.com.murilo.liberthia.faction.FactionReputation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Right-click to pray — grants +10 Order reputation, once every 30 minutes. */
public class OrderShrineBlock extends Block {

    private static final long COOLDOWN_TICKS = 36_000L; // 30 min @ 20 tps
    private static final String TAG_LAST_PRAY = "liberthia_last_pray";

    public OrderShrineBlock(Properties p) { super(p); }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.CONSUME;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.CONSUME;

        long now = level.getGameTime();
        long last = sp.getPersistentData().getLong(TAG_LAST_PRAY);
        if (now - last < COOLDOWN_TICKS) {
            long remaining = (COOLDOWN_TICKS - (now - last)) / 20L;
            player.displayClientMessage(
                    Component.literal("§7Você deve esperar " + remaining + "s para rezar de novo."), true);
            return InteractionResult.CONSUME;
        }

        sp.getPersistentData().putLong(TAG_LAST_PRAY, now);
        FactionReputation rep = FactionReputation.forLevel(sl);
        rep.add(sp.getUUID(), Faction.ORDER, +10);
        rep.add(sp.getUUID(), Faction.BLOOD, -2);

        int orderRep = rep.get(sp.getUUID(), Faction.ORDER);
        player.displayClientMessage(
                Component.literal("§bA Ordem ouve. §f(Ordem: " + orderRep + ")"), false);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 1.5F);
        return InteractionResult.CONSUME;
    }
}
