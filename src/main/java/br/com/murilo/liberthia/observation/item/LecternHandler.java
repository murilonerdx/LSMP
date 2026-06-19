package br.com.murilo.liberthia.observation.item;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.observation.api.ObservationRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * v0.1.22 r74: <b>Lectern Handler</b> — quando player right-click no Lectern
 * segurando um Spell Parchment, mostra o recipe completo no chat.
 *
 * <p>Não placa o parchment no lectern (lectern só aceita Book). Apenas
 * exibe o recipe pra leitura.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class LecternHandler {

    private LecternHandler() {}

    @SubscribeEvent
    public static void onLecternRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        var state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof LecternBlock)) return;
        var held = event.getItemStack();
        if (!(held.getItem() instanceof SpellParchmentItem)) return;

        var recipe = SpellParchmentItem.getRecipe(held);
        if (recipe.isEmpty()) {
            sp.displayClientMessage(Component.literal("§7§oPergaminho em branco."), false);
            return;
        }

        // Show recipe expanded
        sp.displayClientMessage(Component.literal(
            "§5§l✦ Lectern: Pergaminho aberto ✦"), false);
        var spell = SpellParchmentItem.buildSpell(held);
        if (spell != null) {
            sp.displayClientMessage(Component.literal(
                "§dNome: §e" + spell.name() + " §7| Custo: §c" + spell.totalSourceCost() + " Source"), false);
        }
        sp.displayClientMessage(Component.literal("§7§oReceita:"), false);
        int idx = 1;
        for (String id : recipe) {
            var part = ObservationRegistry.get(new ResourceLocation(id));
            String name = part != null ? part.displayComponent().getString() : id;
            String type = part != null ? typeIcon(part.typeIndex()) : "?";
            sp.displayClientMessage(Component.literal(
                "  §7" + (idx++) + ". " + type + " §f" + name), false);
        }
        // Block placing on lectern (prevent vanilla logic)
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static String typeIcon(int typeIndex) {
        return switch (typeIndex) {
            case 1 -> "§b★";
            case 5 -> "§d✦";
            case 10 -> "§e◆";
            default -> "§7?";
        };
    }
}
