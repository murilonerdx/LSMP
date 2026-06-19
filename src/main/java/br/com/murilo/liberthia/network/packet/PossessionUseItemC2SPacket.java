package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.event.PossessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * v0.1.49: C2S — possessor manda "usar o item da mainhand". Server executa
 * {@code stack.getItem().use(level, possessor, MAIN_HAND)} no próprio possessor
 * (não no possuído). Funciona pra:
 * <ul>
 *   <li><b>Consumíveis</b>: food, pílulas (dark/clear/yellow_matter_pill), potions —
 *       consome stack do possessor, aplica efeitos no possessor.</li>
 *   <li><b>Projéteis</b>: ender pearl, snowball, throwable, etc. — lança da
 *       posição do possessor (não da entidade possuída).</li>
 *   <li><b>Right-click items</b>: tools com special actions (Pulso de Astaron, etc.)</li>
 * </ul>
 *
 * <p>User pediu "quero usar items sendo monstro" — esse packet é o caminho.
 * Limitação: items posicionais (block placement) acabam ficando na pos do
 * possessor invisível, não onde a entity possuída tá vendo. Pra V2 dá pra
 * fazer raycast da entidade possuída e passar BlockHitResult sintético.
 */
public class PossessionUseItemC2SPacket {

    public PossessionUseItemC2SPacket() {}

    public static void encode(PossessionUseItemC2SPacket pkt, FriendlyByteBuf buf) {
        // sem payload
    }

    public static PossessionUseItemC2SPacket decode(FriendlyByteBuf buf) {
        return new PossessionUseItemC2SPacket();
    }

    public static void handle(PossessionUseItemC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer possessor = context.getSender();
            if (possessor == null) return;
            // r21: rate-limited recovery em vez de spam a cada packet.
            br.com.murilo.liberthia.event.PossessionSession session =
                    PossessionManager.getSession(possessor.getUUID());
            if (session == null) {
                PossessionManager.requestClientRecoverySync(possessor);
                return;
            }

            ItemStack stack = possessor.getMainHandItem();
            if (stack.isEmpty()) return;

            // Dispara use() do item no possessor. O resultado já trata:
            //   - food: aplica nutrition / saturation, decrementa stack
            //   - pill: dispara use() que mexe no MatterProfile
            //   - ender pearl: spawn projectile, decrementa stack
            InteractionResultHolder<ItemStack> result =
                    stack.getItem().use(possessor.level(), possessor, InteractionHand.MAIN_HAND);
            // Aplica o stack resultante (já pode ter shrink). Vanilla pattern.
            if (result.getResult().consumesAction()) {
                possessor.setItemInHand(InteractionHand.MAIN_HAND, result.getObject());
            }
        });
        context.setPacketHandled(true);
    }
}
