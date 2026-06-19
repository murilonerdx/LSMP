package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * v0.1.22 r23: Mass Possession Crown — Coroa da Posse em Massa.
 *
 * <h2>Modo de uso</h2>
 * <ul>
 *   <li><b>Right-click em outro player</b>: adiciona/remove ele do "puppet pool"
 *       (lista de UUIDs no NBT). Toggle.</li>
 *   <li><b>Right-click no AR</b>: mostra lista de puppets atuais.</li>
 *   <li><b>Shift+Right-click no AR</b>: limpa pool inteiro.</li>
 *   <li><b>Chat com prefixo "!" enquanto segura</b>: mensagem vai como
 *       §oactionbar§r pra TODOS os puppets — fingem que sentiram um pensamento
 *       intrusivo. Ex: "!corre pro nether" aparece como §dactionbar§r§7 vermelho na tela deles.</li>
 *   <li><b>Passive tick</b>: puppets recebem Slowness I + partículas roxas
 *       constantes pra denunciar visualmente.</li>
 * </ul>
 *
 * <p>Implementação do chat-as-actionbar em {@link br.com.murilo.liberthia.event.MadnessEvents}.
 */
public class MassPossessionCrownItem extends Item {

    private static final String NBT_PUPPETS = "PuppetPool";
    public static final int MAX_PUPPETS = 8;
    /** Prefixo no chat pra broadcast aos puppets. Ex: "!fuja agora". */
    public static final String CHAT_PREFIX = "!";

    public MassPossessionCrownItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    /** Retorna lista de UUIDs dos puppets atuais. Nunca null. */
    public static List<UUID> getPuppets(ItemStack stack) {
        List<UUID> out = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_PUPPETS)) return out;
        ListTag list = tag.getList(NBT_PUPPETS, 8); // STRING
        for (int i = 0; i < list.size(); i++) {
            try {
                out.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {}
        }
        return out;
    }

    private static void savePuppets(ItemStack stack, List<UUID> puppets) {
        ListTag list = new ListTag();
        for (UUID id : puppets) list.add(StringTag.valueOf(id.toString()));
        stack.getOrCreateTag().put(NBT_PUPPETS, list);
    }

    /** Adiciona/remove puppet. Retorna true se adicionou, false se removeu. */
    public static boolean togglePuppet(ItemStack stack, UUID puppet) {
        List<UUID> list = getPuppets(stack);
        boolean added;
        if (list.contains(puppet)) {
            list.remove(puppet);
            added = false;
        } else {
            if (list.size() >= MAX_PUPPETS) {
                added = false; // cheio
            } else {
                list.add(puppet);
                added = true;
            }
        }
        savePuppets(stack, list);
        return added;
    }

    public static void clearPuppets(ItemStack stack) {
        if (stack.hasTag()) stack.getTag().remove(NBT_PUPPETS);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(target instanceof ServerPlayer victim)) return InteractionResult.PASS;
        if (!(user instanceof ServerPlayer holder)) return InteractionResult.PASS;
        if (victim.getUUID().equals(holder.getUUID())) {
            holder.displayClientMessage(Component.literal(
                    "§7Você não pode possuir a si mesmo."), true);
            return InteractionResult.FAIL;
        }
        // Mind Ward bloqueia
        if (MindWardItem.hasWard(victim)) {
            holder.displayClientMessage(Component.literal(
                    "§5✦ §d" + victim.getName().getString() + "§5 está protegido."), true);
            return InteractionResult.FAIL;
        }
        boolean added = togglePuppet(stack, victim.getUUID());
        int total = getPuppets(stack).size();
        if (added) {
            holder.displayClientMessage(Component.literal(
                    "§d+ §a" + victim.getName().getString()
                            + "§7 adicionado aos puppets (§e" + total + "/" + MAX_PUPPETS + "§7)"), true);
            victim.displayClientMessage(Component.literal(
                    "§4§oVocê sente uma presença em sua mente...").withStyle(ChatFormatting.DARK_RED), true);
        } else {
            holder.displayClientMessage(Component.literal(
                    "§d- §c" + victim.getName().getString()
                            + "§7 removido (§e" + total + "/" + MAX_PUPPETS + "§7)"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(user instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);

        if (user.isShiftKeyDown()) {
            clearPuppets(stack);
            sp.displayClientMessage(Component.literal(
                    "§d✦ Pool de puppets §climpo§7.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResultHolder.success(stack);
        }
        List<UUID> puppets = getPuppets(stack);
        if (puppets.isEmpty()) {
            sp.displayClientMessage(Component.literal(
                    "§7Nenhum puppet. Right-click em players pra adicionar (max " + MAX_PUPPETS + ")."), false);
            sp.displayClientMessage(Component.literal(
                    "§7Use §e!sua_mensagem§7 no chat pra mandar pensamento intrusivo."), false);
        } else {
            sp.displayClientMessage(Component.literal(
                    "§d✦ Puppets §a(" + puppets.size() + ")§7:"), false);
            for (UUID id : puppets) {
                ServerPlayer p = sp.server.getPlayerList().getPlayer(id);
                String name = p != null ? p.getName().getString() : "§8(offline)";
                sp.displayClientMessage(Component.literal("  §7- §e" + name), false);
            }
            sp.displayClientMessage(Component.literal(
                    "§7Use §e!mensagem§7 no chat pra mandar como actionbar pra todos."), false);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§d§oCoroa da Posse em Massa").withStyle(ChatFormatting.ITALIC));
        tip.add(Component.literal("§7Right-click em players: §a+/-§r§7 do pool"));
        tip.add(Component.literal("§7Right-click no ar: §elista§r§7 puppets"));
        tip.add(Component.literal("§7Shift+Right-click no ar: §climpa§r§7 pool"));
        tip.add(Component.literal("§7Chat §e!mensagem§r§7: §dactionbar§r§7 pra todos puppets"));
        int count = getPuppets(stack).size();
        if (count > 0) {
            tip.add(Component.empty());
            tip.add(Component.literal("§d§l✦ §r§e" + count + "§7/§7" + MAX_PUPPETS + "§7 puppets ativos"));
        }
    }
}
