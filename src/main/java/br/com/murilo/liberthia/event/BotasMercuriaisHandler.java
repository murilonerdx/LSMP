package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.compat.CuriosCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Habilita voo creative-like enquanto Botas Mercuriais estão equipadas.
 * Drena durab por tick voando + por hit causado.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class BotasMercuriaisHandler {

    /** Players cujo mayfly foi habilitado por nós — pra revogar quando tirar. */
    private static final Set<UUID> ENABLED_BY_US = new HashSet<>();

    private BotasMercuriaisHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        boolean active = CuriosCompat.isBotasMercuriaisActive(sp);
        if (active && !sp.getAbilities().instabuild) {
            if (!sp.getAbilities().mayfly) {
                sp.getAbilities().mayfly = true;
                sp.onUpdateAbilities();
                ENABLED_BY_US.add(sp.getUUID());
            }
            if (sp.getAbilities().flying) {
                CuriosCompat.damageBotasMercuriais(sp, 1);
                // Re-check active after damage (item might be consumed)
                if (!CuriosCompat.isBotasMercuriaisActive(sp)) {
                    sp.getAbilities().flying = false;
                    sp.getAbilities().mayfly = false;
                    sp.onUpdateAbilities();
                    ENABLED_BY_US.remove(sp.getUUID());
                }
            }
        } else if (ENABLED_BY_US.contains(sp.getUUID()) && !sp.getAbilities().instabuild) {
            sp.getAbilities().flying = false;
            sp.getAbilities().mayfly = false;
            sp.onUpdateAbilities();
            ENABLED_BY_US.remove(sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void onHit(LivingHurtEvent event) {
        if (event.getSource() == null) return;
        if (!(event.getSource().getEntity() instanceof Player p)) return;
        if (!(p instanceof ServerPlayer sp)) return;
        if (CuriosCompat.isBotasMercuriaisActive(sp)) {
            CuriosCompat.damageBotasMercuriais(sp, 1);
        }
    }
}
