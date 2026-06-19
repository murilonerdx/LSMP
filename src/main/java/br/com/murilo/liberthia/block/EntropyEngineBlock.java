package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.block.entity.EntropyEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * r180: <b>Motor de Entropia</b> — máquina que consome a realidade pra gerar FE +
 * corrupção. Clique direito mostra status (energia / corrupção / geração). Veja a
 * mecânica em {@link EntropyEngineBlockEntity}.
 */
public class EntropyEngineBlock extends Block implements EntityBlock {

    public EntropyEngineBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EntropyEngineBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof EntropyEngineBlockEntity e) EntropyEngineBlockEntity.tick(lvl, pos, st, e);
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EntropyEngineBlockEntity be) {
            int c = be.getLastCorruption();
            player.displayClientMessage(Component.literal("§5§l═ Motor de Entropia ═"), false);
            player.displayClientMessage(Component.literal("§7Energia: §b" + be.getEnergyStored() + " §7/ " + be.getMaxEnergy() + " FE"), false);
            player.displayClientMessage(Component.literal("§7Corrupção local: " + bar(c) + " §f" + c + "%"), false);
            player.displayClientMessage(Component.literal("§7Geração (último ciclo): §a" + be.getLastGen() + " FE"), false);
            if (c >= 80) {
                player.displayClientMessage(Component.literal("§4§l⚠ Corrupção crítica — risco de colapso/buraco negro!"), false);
            } else if (c >= 40) {
                player.displayClientMessage(Component.literal("§6Corrupção subindo — a energia escala, mas o perigo também."), false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static String bar(int pct) {
        int filled = Math.round(pct / 10F);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(i < filled ? "§c█" : "§8█");
        return sb.toString();
    }
}
