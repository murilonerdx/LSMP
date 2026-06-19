package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.item.MassPossessionCrownItem;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * v0.1.22 r30: C2S — possessor do Crown of Mass Possession seta o "voice target"
 * (UUID do puppet pra quem o áudio SVC vai ser direcionado).
 *
 * <p>Actions:
 * <ul>
 *   <li>0 = CLEAR (zera voice target — broadcast normal)</li>
 *   <li>1 = SET (target = uuid no payload)</li>
 *   <li>2 = END_POSSESS (force-remove puppet do pool)</li>
 * </ul>
 *
 * <p>Quando voice target setado E player holding crown E falando via SVC,
 * {@link br.com.murilo.liberthia.voice.LiberthiaVoicePlugin} roteia o áudio
 * como LocationalAudioChannel na CABEÇA do puppet com distance=3 (só ele
 * ouve, ninguém em volta).
 */
public class CrownVoiceTargetC2SPacket {

    public static final byte ACTION_CLEAR = 0;
    public static final byte ACTION_SET = 1;
    public static final byte ACTION_END_POSSESS = 2;

    /** NBT key na crown que armazena o UUID do voice target atual. */
    public static final String NBT_VOICE_TARGET = "VoiceTarget";

    private final byte action;
    private final UUID payload;

    public CrownVoiceTargetC2SPacket(byte action, UUID payload) {
        this.action = action;
        this.payload = payload == null ? new UUID(0, 0) : payload;
    }

    public static void encode(CrownVoiceTargetC2SPacket p, FriendlyByteBuf buf) {
        buf.writeByte(p.action);
        buf.writeUUID(p.payload);
    }

    public static CrownVoiceTargetC2SPacket decode(FriendlyByteBuf buf) {
        return new CrownVoiceTargetC2SPacket(buf.readByte(), buf.readUUID());
    }

    public static void handle(CrownVoiceTargetC2SPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            ItemStack crown = findCrown(sp);
            if (crown.isEmpty()) {
                sp.displayClientMessage(Component.literal(
                        "§cVocê precisa segurar a Coroa."), true);
                return;
            }
            switch (p.action) {
                case ACTION_CLEAR -> {
                    if (crown.hasTag()) crown.getTag().remove(NBT_VOICE_TARGET);
                    sp.displayClientMessage(Component.literal(
                            "§d✦ Voice target limpo.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                }
                case ACTION_SET -> {
                    var puppets = MassPossessionCrownItem.getPuppets(crown);
                    if (!puppets.contains(p.payload)) {
                        sp.displayClientMessage(Component.literal(
                                "§cAlvo não está no pool de puppets."), true);
                        return;
                    }
                    crown.getOrCreateTag().putUUID(NBT_VOICE_TARGET, p.payload);
                    ServerPlayer tgt = sp.server.getPlayerList().getPlayer(p.payload);
                    String name = tgt != null ? tgt.getName().getString() : "?";
                    sp.displayClientMessage(Component.literal(
                            "§d✦ Sua voz agora chega só a §a" + name).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                }
                case ACTION_END_POSSESS -> {
                    MassPossessionCrownItem.togglePuppet(crown, p.payload);
                    // Se era voice target, limpa
                    if (crown.hasTag() && crown.getTag().hasUUID(NBT_VOICE_TARGET)) {
                        if (crown.getTag().getUUID(NBT_VOICE_TARGET).equals(p.payload)) {
                            crown.getTag().remove(NBT_VOICE_TARGET);
                        }
                    }
                    ServerPlayer tgt = sp.server.getPlayerList().getPlayer(p.payload);
                    String name = tgt != null ? tgt.getName().getString() : "?";
                    sp.displayClientMessage(Component.literal(
                            "§d✦ Posse encerrada em §c" + name).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                    if (tgt != null) {
                        tgt.displayClientMessage(Component.literal(
                                "§a✦ A presença em sua mente §lse retira§r§a."), false);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /** Retorna a Crown que o player está segurando (mainhand/offhand) ou EMPTY. */
    public static ItemStack findCrown(ServerPlayer sp) {
        var crown = ModItems.MASS_POSSESSION_CROWN.get();
        if (sp.getMainHandItem().is(crown)) return sp.getMainHandItem();
        if (sp.getOffhandItem().is(crown)) return sp.getOffhandItem();
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(crown)) return s;
        }
        return ItemStack.EMPTY;
    }
}
