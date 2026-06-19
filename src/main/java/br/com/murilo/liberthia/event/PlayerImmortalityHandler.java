package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.1.41: gerencia "immortal mode" pra players via comando
 * {@code /liberthia immortal on|off}. Player fica imortal em SURVIVAL —
 * cancela TODO dano + tela de morte, mesmo magia/kill/etc.
 *
 * <p>3 listeners pra defesa em profundidade:
 * <ul>
 *   <li>{@code LivingHurtEvent} (HIGHEST): cancela ANTES do dano ser aplicado.</li>
 *   <li>{@code LivingDamageEvent} (HIGHEST): fallback — cancela DEPOIS do hurt.</li>
 *   <li>{@code LivingDeathEvent} (HIGHEST): safety net — se algo passar (ex:
 *       hp=0 via setHealth direto), restaura HP e cancela morte.</li>
 * </ul>
 *
 * <p>Estado VOLÁTIL — não persiste entre restarts (intencional, comando admin).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class PlayerImmortalityHandler {

    private static final Set<UUID> IMMORTAL = ConcurrentHashMap.newKeySet();

    private PlayerImmortalityHandler() {}

    /** Liga modo imortal pro player. Retorna true se mudou estado. */
    public static boolean enable(UUID uuid) {
        return IMMORTAL.add(uuid);
    }

    /** Desliga. Retorna true se estava ligado. */
    public static boolean disable(UUID uuid) {
        return IMMORTAL.remove(uuid);
    }

    public static boolean isImmortal(UUID uuid) {
        return IMMORTAL.contains(uuid);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!IMMORTAL.contains(sp.getUUID())) return;
        event.setCanceled(true);
        // Garante HP cheio se algo dropou antes
        if (sp.getHealth() < sp.getMaxHealth()) {
            sp.setHealth(sp.getMaxHealth());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!IMMORTAL.contains(sp.getUUID())) return;
        event.setCanceled(true);
        if (sp.getHealth() < sp.getMaxHealth()) {
            sp.setHealth(sp.getMaxHealth());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!IMMORTAL.contains(sp.getUUID())) return;
        event.setCanceled(true);
        // Restaura HP — sem isso fica preso em 0 e o próximo tick dá kick.
        sp.setHealth(sp.getMaxHealth());
        sp.hurtTime = 0;
        sp.invulnerableTime = 60; // 3s de invulnerabilidade extra de cobertura
    }
}
