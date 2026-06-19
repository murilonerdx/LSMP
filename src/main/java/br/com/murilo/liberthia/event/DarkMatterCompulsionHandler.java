package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.matter.MatterProfile;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * r180: <b>Sintoma da Matéria Escura — Compulsão Homicida.</b> Players com Matéria Escura
 * alta (≥{@value #THRESHOLD}) periodicamente veem a tela ficar vermelha (título "MATE-OS")
 * e ouvem vozes mandando matar quem está por perto. Quanto MAIOR a matéria, mais frequente.
 *
 * <p>Suprimido se o player tiver a <b>Âncora Mental</b> ({@code mind_anchor}) na mão,
 * inventário ou num slot Curios. (A vinheta vermelha persistente é client-side no
 * {@code ScreenEffectsOverlay}.)
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class DarkMatterCompulsionHandler {

    public static final float THRESHOLD = 55.0F;
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    private static final String[] URGES = {
            "Mate-os. Eles não merecem viver.",
            "Eles te olham torto... acabe com eles.",
            "O sangue deles vai te acalmar.",
            "Ninguém vai sentir falta deles.",
            "Faça agora, antes que percam a utilidade.",
            "A matéria exige. Obedeça."
    };

    private DarkMatterCompulsionHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return; // 1×/s
        if (sp.isCreative() || sp.isSpectator()) return;

        float dark = darkOf(sp);
        if (dark < THRESHOLD) return;
        if (hasSuppressor(sp)) return;

        Long next = NEXT.get(sp.getUUID());
        if (next != null && sp.tickCount < next) return;

        // intervalo encurta com a matéria: 55% → ~30s, 100% → ~12s
        int interval = (int) Mth.clamp(600 - (dark - THRESHOLD) * 8F, 240, 600);
        interval += sp.getRandom().nextInt(120) - 60;
        NEXT.put(sp.getUUID(), (long) sp.tickCount + Math.max(120, interval));

        triggerCompulsion(sp, dark);
    }

    private static void triggerCompulsion(ServerPlayer sp, float dark) {
        String urge = URGES[sp.getRandom().nextInt(URGES.length)];
        sp.connection.send(new ClientboundSetTitlesAnimationPacket(4, 36, 12));
        sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§lMATE-OS")));
        sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c" + urge)));
        sp.sendSystemMessage(Component.literal("§4§o" + urge));
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.9F, 0.5F);
        if (dark > 80F) {
            sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
        }
    }

    private static float darkOf(ServerPlayer sp) {
        MatterProfile prof = sp.getCapability(
                br.com.murilo.liberthia.matter.MatterProfileProvider.CAP).orElse(null);
        return prof != null ? prof.getDark() : 0F;
    }

    private static boolean hasSuppressor(Player p) {
        if (p.getInventory().contains(new ItemStack(ModItems.MIND_ANCHOR.get()))) return true;
        try {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                    .findFirstCurio(p, s -> s.is(ModItems.MIND_ANCHOR.get())).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
