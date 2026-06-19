package br.com.murilo.liberthia.cosmic.observatory.console;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r50: <b>Caretaker Console</b> — admin item que abre uma GUI
 * pra configurar mensagens fake injetadas no chat de UM player específico.
 *
 * <h2>Uso</h2>
 * <ol>
 *   <li>Right-click no AR: abre {@code CaretakerConsoleScreen}</li>
 *   <li>GUI tem: input do nome do target, checkbox de templates, custom msg
 *       input, slider intervalo, slider duração, botão ATIVAR</li>
 *   <li>Quando ativa, server inicia {@link CaretakerSession} pro target</li>
 *   <li>Target recebe mensagens random só pra ele a cada N segundos</li>
 * </ol>
 */
public class CaretakerConsoleItem extends Item {

    public CaretakerConsoleItem(Properties p) {
        super(p.rarity(Rarity.EPIC).stacksTo(1));
    }

    @Override public boolean isFoil(ItemStack s) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (!(user instanceof ServerPlayer sp) && level.isClientSide) {
            // Client-side só abre GUI se for admin local
            openConsole();
            return InteractionResultHolder.success(stack);
        }
        if (!(user instanceof ServerPlayer ssp)) return InteractionResultHolder.pass(stack);
        // Server-side: valida permissão
        if (!ssp.hasPermissions(2) && !ssp.isCreative()) {
            ssp.displayClientMessage(Component.literal(
                    "§cVocê não é um Caretaker."), true);
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    @OnlyIn(Dist.CLIENT)
    private void openConsole() {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new br.com.murilo.liberthia.cosmic.observatory.console.client.CaretakerConsoleScreen());
    }

    @Override public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§4§l§oConsole do Caretaker").withStyle(ChatFormatting.DARK_RED));
        t.add(Component.literal("§7§oFerramenta dos administradores do Observatório."));
        t.add(Component.empty());
        t.add(Component.literal("§7Right-click: abre §lconsole§r§7 de configuração"));
        t.add(Component.literal("§7Define mensagens fake injetadas no §dchat§r§7 de"));
        t.add(Component.literal("§7um player target específico — §lapenas ele vê§r§7."));
        t.add(Component.empty());
        t.add(Component.literal("§7§lTemplates incluídos:"));
        t.add(Component.literal("§8• join/leave fake"));
        t.add(Component.literal("§8• fake death messages"));
        t.add(Component.literal("§8• /spawn /home failed"));
        t.add(Component.literal("§8• admin warnings"));
        t.add(Component.literal("§8• sistema/conexão fake"));
        t.add(Component.literal("§8• whispers (§o*você ouve...*§r§8)"));
        t.add(Component.empty());
        t.add(Component.literal("§c§oADMIN ONLY (op level 2+)"));
        t.add(Component.empty());
        t.add(Component.literal("§8§o\"A mensagem que falta. A que sobra. A que nunca veio.\""));
    }
}
