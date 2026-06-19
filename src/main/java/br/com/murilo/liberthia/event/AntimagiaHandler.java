package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.compat.mna.AntiMagicWardHandler;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * r180: comportamento do efeito <b>Antimagia</b> (o "selo" do sistema anti-magia).
 * Enquanto um player tem o efeito:
 * <ul>
 *   <li><b>Não voa</b> (cancela voo mágico) — cobre os "itens contra voo".</li>
 *   <li><b>A magia que ele lança é quase anulada</b> (-75% de dano mágico/M&A).</li>
 *   <li>Fica lento + partículas cianas (identidade visual da linha).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AntimagiaHandler {

    private static final DustParticleOptions CYAN =
            new DustParticleOptions(new Vector3f(0.17F, 0.84F, 0.84F), 1.0F);

    private AntimagiaHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!sp.hasEffect(ModEffects.ANTIMAGIA.get())) return;

        // 1) bloqueia voo mágico (não mexe em quem está em creative-mode de verdade)
        if (sp.getAbilities().flying && !sp.isCreative() && !sp.isSpectator()) {
            sp.getAbilities().flying = false;
            sp.onUpdateAbilities();
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal("§b✦ Antimagia: voo selado"), true);
        }
        // 2) disrupção
        if (sp.tickCount % 40 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
            sp.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 1, false, false));
        }
        // 3) partículas cianas (visual)
        if (sp.level() instanceof ServerLevel sl && sp.tickCount % 8 == 0) {
            sl.sendParticles(CYAN, sp.getX(), sp.getY() + 1.0, sp.getZ(), 5, 0.4, 0.6, 0.4, 0.0);
        }
    }

    /** Magia que um player COM Antimagia lança é anulada (-75%). */
    @SubscribeEvent
    public static void onMagicCastWhileSealed(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker
                && attacker.hasEffect(ModEffects.ANTIMAGIA.get())) {
            DamageSource src = event.getSource();
            boolean magic = src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC)
                    || AntiMagicWardHandler.isMnaSource(src);
            if (magic) {
                event.setAmount(event.getAmount() * 0.25F);
            }
        }
    }
}
