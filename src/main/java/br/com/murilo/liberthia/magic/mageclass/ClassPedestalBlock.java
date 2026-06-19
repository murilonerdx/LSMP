package br.com.murilo.liberthia.magic.mageclass;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * r162: Class Pedestal — right-click pra escolher classe de mago (13 opções).
 *
 * <p>Server gera lista de 13 classes; client side renderiza menu via packet.
 * Por simplicidade, este bloco envia o player nome da classe selecionada via
 * chat clickable (não precisa de GUI custom).
 */
public class ClassPedestalBlock extends Block {

    public ClassPedestalBlock(BlockBehaviour.Properties p) {
        super(p);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        // Sneak = clear class, normal = open UI screen
        if (player.isShiftKeyDown()) {
            MageClassData.set(sp, null);
            sp.sendSystemMessage(Component.literal("§7Classe de mago §lremovida§r§7."));
            return InteractionResult.CONSUME;
        }
        // r164: abre a Screen UI em vez do chat menu antigo
        MageClass current = MageClassData.get(sp);
        int curLevel = current != null ? MageClassData.getLevel(sp) : 0;
        br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                new OpenClassPedestalS2CPacket(
                        current != null ? current.name() : "",
                        curLevel));
        return InteractionResult.CONSUME;
    }

    /** Envia uma "tela" de seleção via chat com componentes clicáveis. */
    private void showClassMenu(ServerPlayer sp) {
        sp.sendSystemMessage(Component.literal("§l§5═══ Pedestal das Classes ═══§r"));
        MageClass current = MageClassData.get(sp);
        if (current != null) {
            int lv = MageClassData.getLevel(sp);
            sp.sendSystemMessage(Component.literal(
                    "§7Atual: " + current.colorCode + "§l" + current.displayName + "§r §7nível §e" + lv + "§7/10  "
                            + "§7(§a+" + current.dmgBonusAt(lv) + "%§7 dano)"));
        } else {
            sp.sendSystemMessage(Component.literal("§7Atual: §8nenhuma classe selecionada"));
        }
        sp.sendSystemMessage(Component.literal("§7Click para escolher:"));
        for (MageClass cls : MageClass.values()) {
            net.minecraft.network.chat.MutableComponent line = Component.literal(
                    "  " + cls.colorCode + "▶ §l" + cls.displayName + "§r"
                            + " §7(+" + cls.baseDmgPct + "% base, +" + (int)cls.stepPct + "%/lv)");
            net.minecraft.network.chat.Style style = line.getStyle()
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                            net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                            "/liberthia mageclass " + cls.name()))
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                            net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Click pra escolher " + cls.displayName)));
            line.setStyle(style);
            sp.sendSystemMessage(line);
        }
        sp.sendSystemMessage(Component.literal("§7§o(shift+right-click pra remover classe)"));
    }
}
