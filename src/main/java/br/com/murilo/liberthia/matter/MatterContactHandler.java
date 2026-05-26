package br.com.murilo.liberthia.matter;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Detecta itens com conteúdo de matéria nas mãos do jogador e adiciona
 * matéria ao perfil gradativamente.
 *
 * <p>Ex: segurar uma {@code DARK_MATTER_SWORD} (DM:40 no registry) gera
 * {@code 40 × 0.005 = 0.2 DM} a cada tick processado (60 ticks = 12 DM/min).
 *
 * <p>Aplica também pra qualquer item registrado em
 * {@link MatterContentRegistry} — segurar uma matéria amarela na mão também
 * cresce o YM, etc.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MatterContactHandler {

    /** A cada N ticks processa o contato. */
    public static final int PERIOD = 60;
    /** Multiplicador: matter * RATE = pontos adicionados por período. */
    public static final float RATE = 0.005f;

    private MatterContactHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % PERIOD != 0) return;
        if (player.isCreative() || player.isSpectator()) return;

        // v0.1.16 bug fix: CLEAR_SHIELD (aplicado pelas 3 pílulas por 60s) bloqueia
        // o ganho de matter por contato. Sem isso, player tomava pílula e logo
        // depois ganhava matter de novo porque o item na mão (espada de matter,
        // bucket de matter) ainda contava. Shield ativo = imunidade temporária.
        if (player.hasEffect(br.com.murilo.liberthia.registry.ModEffects.CLEAR_SHIELD.get())) {
            return;
        }

        // v0.1.30: Refined Containment Glove bloqueia ganho via CONTATO direto
        // (segurar shard/ingot na mão). Drena 1 durab por ativação.
        if (br.com.murilo.liberthia.compat.CuriosCompat.isRefinedGloveActive(player)) {
            // Só drena durab se de fato HAVERIA ganho (player segurando matter).
            MatterContent peekMain = MatterContentRegistry.of(player.getMainHandItem());
            MatterContent peekOff  = MatterContentRegistry.of(player.getOffhandItem());
            if (peekMain.dark() + peekMain.white() + peekMain.yellow() > 0
                    || peekOff.dark() + peekOff.white() + peekOff.yellow() > 0) {
                br.com.murilo.liberthia.compat.CuriosCompat.damageRefinedGlove(player, 1);
            }
            return;
        }
        // v0.1.30: Refined Containment Pendant também bloqueia contato
        // (cobertura ampla — pendant + glove juntos = imunidade total).
        if (br.com.murilo.liberthia.compat.CuriosCompat.isRefinedPendantActive(player)) {
            MatterContent peekMain = MatterContentRegistry.of(player.getMainHandItem());
            MatterContent peekOff  = MatterContentRegistry.of(player.getOffhandItem());
            if (peekMain.dark() + peekMain.white() + peekMain.yellow() > 0
                    || peekOff.dark() + peekOff.white() + peekOff.yellow() > 0) {
                br.com.murilo.liberthia.compat.CuriosCompat.damageRefinedPendant(player, 1);
            }
            return;
        }

        // Soma conteúdo de matéria de main hand + off hand
        MatterContent main  = matterOfHand(player.getMainHandItem());
        MatterContent off   = matterOfHand(player.getOffhandItem());

        float dm = (main.dark()   + off.dark())   * RATE * PERIOD;
        float wm = (main.white()  + off.white())  * RATE * PERIOD;
        float ym = (main.yellow() + off.yellow()) * RATE * PERIOD;
        if (dm <= 0 && wm <= 0 && ym <= 0) return;

        // v0.1.51: respeita as pílulas — cada tipo de matter é zerado se o
        // player tem a resistência correspondente ativa.
        final float fdm = MatterResistance.blocked(player, MatterResistance.Type.DARK) ? 0f : dm;
        final float fwm = MatterResistance.blocked(player, MatterResistance.Type.CLEAR) ? 0f : wm;
        final float fym = MatterResistance.blocked(player, MatterResistance.Type.YELLOW) ? 0f : ym;
        if (fdm <= 0 && fwm <= 0 && fym <= 0) return;
        player.getCapability(MatterProfileProvider.CAP).ifPresent(profile -> {
            if (fdm > 0) profile.addDark(fdm);
            if (fwm > 0) profile.addWhite(fwm);
            if (fym > 0) profile.addYellow(fym);
            MatterProfileEvents.syncTo(player);
        });
    }

    /**
     * v0.1.42: wrapper de MatterContentRegistry.of() que respeita o NBT
     * "Purified" da Clear Matter Sword. Sword purificada (post Matter Purifier
     * com 600k FE) NÃO conta como source de matter. User pediu: "espada mesmo
     * apos purificada ainda continua dando infecção".
     */
    private static MatterContent matterOfHand(net.minecraft.world.item.ItemStack stack) {
        if (stack.getItem() == br.com.murilo.liberthia.registry.ModItems.CLEAR_MATTER_SWORD.get()
                && stack.getTag() != null && stack.getTag().getBoolean("Purified")) {
            return MatterContent.EMPTY;
        }
        return MatterContentRegistry.of(stack);
    }
}
