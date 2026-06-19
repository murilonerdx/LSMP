package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.BossCrownItem;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r3: C2S — player confirma nome custom + estado ativo. Server
 * encontra a Boss Crown no inventário e salva ambos os NBTs.
 */
public class SetBossCrownNameC2SPacket {
    /** NBT key — string com o nome custom da bossbar. */
    public static final String NBT_BOSS_BAR_NAME = "BossBarName";

    private final String name;
    private final boolean active;

    public SetBossCrownNameC2SPacket(String name, boolean active) {
        this.name = name == null ? "" : name;
        this.active = active;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(name, 64);
        buf.writeBoolean(active);
    }

    public static SetBossCrownNameC2SPacket decode(FriendlyByteBuf buf) {
        return new SetBossCrownNameC2SPacket(buf.readUtf(64), buf.readBoolean());
    }

    public static void handle(SetBossCrownNameC2SPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) {
                LiberthiaMod.LOGGER.warn("[BossCrown C2S] save received but sender is null");
                return;
            }
            LiberthiaMod.LOGGER.info("[BossCrown C2S] save received from {}: name='{}' active={}",
                    sender.getName().getString(), msg.name, msg.active);

            // v0.1.22 r3: scan inventário INTEIRO (não só mainhand/offhand).
            // Permite renomear mesmo se o item foi movido entre o abrir tela
            // e o salvar.
            ItemStack stack = findCrown(sender);
            if (stack.isEmpty()) {
                LiberthiaMod.LOGGER.warn("[BossCrown C2S] {} doesn't have a Boss Crown in inventory",
                        sender.getName().getString());
                sender.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§cVocê não tem a Coroa do Boss no inventário."),
                        true);
                return;
            }

            String safe = msg.name.trim();
            if (safe.length() > 32) safe = safe.substring(0, 32);
            if (safe.isEmpty()) {
                if (stack.hasTag()) stack.getTag().remove(NBT_BOSS_BAR_NAME);
                LiberthiaMod.LOGGER.info("[BossCrown C2S] cleared boss bar name");
            } else {
                stack.getOrCreateTag().putString(NBT_BOSS_BAR_NAME, safe);
                LiberthiaMod.LOGGER.info("[BossCrown C2S] saved boss bar name '{}'", safe);
            }
            boolean wasActive = BossCrownItem.isActive(stack);
            BossCrownItem.setActive(stack, msg.active);
            if (wasActive != msg.active) {
                sender.level().playSound(null, sender.blockPosition(),
                        msg.active ? SoundEvents.WITHER_SPAWN : SoundEvents.WITHER_DEATH,
                        SoundSource.PLAYERS,
                        msg.active ? 1.0F : 0.6F,
                        msg.active ? 0.6F : 1.2F);
                LiberthiaMod.LOGGER.info("[BossCrown C2S] toggled active: {} → {}", wasActive, msg.active);
            }
            // v0.1.22 r8: chama IMEDIATAMENTE a bossbar (sem esperar o tick).
            // User reclamou que bossbar nunca aparecia.
            if (msg.active) {
                br.com.murilo.liberthia.event.BossCrownHandler.showBossBarNow(sender, safe);
            } else {
                br.com.murilo.liberthia.event.BossCrownHandler.hideBossBarNow(sender);
            }
            sender.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(msg.active
                            ? "§4§l⊕ Coroa DESPERTA§r§7 — bossbar: §f"
                                    + (safe.isEmpty() ? sender.getName().getString() : safe)
                            : "§7§l⊖ Coroa adormecida"),
                    true);
        });
        ctx.get().setPacketHandled(true);
    }

    /** Procura a Boss Crown no inventário inteiro do player. */
    private static ItemStack findCrown(ServerPlayer p) {
        var crown = ModItems.BOSS_CROWN.get();
        ItemStack mainHand = p.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHand.is(crown)) return mainHand;
        ItemStack offHand = p.getItemInHand(InteractionHand.OFF_HAND);
        if (offHand.is(crown)) return offHand;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(crown)) return s;
        }
        return ItemStack.EMPTY;
    }
}
