package br.com.murilo.liberthia.magic.spell.hotbar;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.client.KeyBindings;
import br.com.murilo.liberthia.magic.grimoire.GrimoireBookItem;
import br.com.murilo.liberthia.magic.grimoire.client.GrimoireWheelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r164 (rewritten): handler client-side dos keybinds de feitiço.
 *
 * <p>Sistema simplificado — <b>removido o "bind" Z/B/N</b> a pedido do user.
 * Agora os keybinds funcionam assim:
 * <ul>
 *   <li><b>X</b>: se o player segura um Grimoire (main ou off-hand), abre o
 *       {@link GrimoireWheelScreen} radial com os 9 slots do livro. Click
 *       (ou solta X em cima) seleciona o slot ativo. Depois é só right-click
 *       no mundo pra castar.</li>
 *   <li><b>R</b>: abre o wheel de Custom Spells (sistema separado da spell
 *       factory antiga — preservado pra não quebrar quem usa).</li>
 *   <li><b>Z / B / N</b>: <b>no-op</b>. O sistema antigo de bindar feitiços
 *       nos hotbar slots foi desativado.</li>
 * </ul>
 *
 * <p>O {@link SpellHotbarActionC2SPacket}/{@link SpellHotbarSyncS2CPacket}
 * ainda existem no projeto mas não são mais invocados pelo handler. Mantemos
 * registrados pra que conexões antigas não quebrem (clients/servers desincronos).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID, value = Dist.CLIENT)
public final class SpellHotbarKeyHandler {

    private SpellHotbarKeyHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // r164: X é DEDICADA ao grimoire. Sem grimoire na mão = sem wheel.
        // (User frustrou-se com 2 HUDs abrindo — agora é exclusivo do grimoire.)
        if (KeyBindings.SPELL_WHEEL_KEY.consumeClick()) {
            if (holdsGrimoire(mc.player)) {
                GrimoireWheelScreen.openFromHeldGrimoire();
            }
            // sem grimoire → no-op (sem mais fallback pra Custom Spell Wheel)
        }

        // r155: R continua abrindo o Custom Spell Wheel (sistema independente
        // pra quem criou feitiços via SpellFactory — não conflita com X).
        if (KeyBindings.SPELL_WHEEL_RADIAL.isDown() && mc.screen == null) {
            mc.setScreen(new br.com.murilo.liberthia.magic.custom.client.SpellWheelScreen());
        }

        // r164: Z / B / N — consume clicks pra não vazar pro vanilla, mas no-op.
        // (Mantemos pra futuras features sem ter que reativar os bindings.)
        KeyBindings.SPELL_HOTBAR_1.consumeClick();
        KeyBindings.SPELL_HOTBAR_2.consumeClick();
        KeyBindings.SPELL_HOTBAR_3.consumeClick();
    }

    private static boolean holdsGrimoire(Player p) {
        return p.getMainHandItem().getItem() instanceof GrimoireBookItem
                || p.getOffhandItem().getItem() instanceof GrimoireBookItem;
    }
}
