package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.fog.FogZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>Incensário da Névoa</b> — item emissor de neblina (estilo terror).
 *
 * <ul>
 *   <li>Click no chão: invoca uma zona de névoa densa (preto-azulada) ali — persiste.</li>
 *   <li>Shift+Click: dissipa a névoa mais próxima.</li>
 * </ul>
 *
 * <p>Usa o mesmo sistema do comando ({@code FogZoneData}), então some/aparece pra
 * todo mundo e sobrevive a reinícios. Pra cor/raio custom, use {@code /liberthia fog}.
 */
public class MistCenserItem extends Item {

    private static final double RAIO = 14.0;
    private static final int COR = 0x0A0A12;   // preto-azulado sufocante
    private static final float DENS = 0.92f;
    private static final float OPAC = 0.22f;   // translúcida (haze sufocante, não bola)
    private static final float INTEN = 1.5f;   // puffs pequenos que se sobrepõem
    private static final double DIST = 44.0;   // distância de render

    public MistCenserItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level lvl = ctx.getLevel();
        if (!(lvl instanceof ServerLevel sl)) {
            return InteractionResult.SUCCESS; // cliente: deixa a animação de uso rolar
        }
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos().above();
        String dim = sl.dimension().location().toString();
        FogZoneData data = FogZoneData.get(sl);

        boolean shift = player != null && player.isShiftKeyDown();
        if (shift) {
            boolean removed = data.removeNnear(sl.getServer(), dim,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 18.0);
            if (player != null) {
                player.displayClientMessage(Component.literal(removed
                        ? "§5Névoa dissipada." : "§7Nenhuma névoa por perto."), true);
            }
        } else {
            int id = data.add(sl.getServer(), dim,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    RAIO, COR, DENS, OPAC, INTEN, DIST, true);
            if (player != null) {
                player.displayClientMessage(Component.literal(
                        "§5Incensário: §fnévoa invocada §8(#" + id + ", raio " + (int) RAIO + ")"), true);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        t.add(Component.literal("§8§oExala uma névoa densa e sufocante."));
        t.add(Component.literal("§7Click no chão: §dinvoca névoa"));
        t.add(Component.literal("§7Shift+Click: §ddissipa a névoa próxima"));
        t.add(Component.literal("§8Cor/raio custom: §7/liberthia fog"));
    }
}
