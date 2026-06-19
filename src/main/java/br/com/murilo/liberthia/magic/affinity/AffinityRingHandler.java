package br.com.murilo.liberthia.magic.affinity;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.magic.school.SchoolResistanceHandler;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * r179: Handler central dos Affinity Rings passivos.
 *
 * <p>Substitui o {@code inventoryTick} do {@link AffinityRingItem} (que não
 * rodava em slots Curios). Roda 1×/tick no servidor e aplica os efeitos
 * dos anéis que o player {@link AffinityRings#isWorn estiver usando}
 * (inventário OU equipado).
 *
 * <ul>
 *   <li><b>FIREWARP</b> — +30 resist FIRE, imunidade a queimar, Fire Resistance.</li>
 *   <li><b>LURKER</b> — parado (sem mexer x/z) por 3s → Invisibilidade.</li>
 * </ul>
 *
 * <p>CAPACITY é tratado em {@code SourceData.recomputeMax} e BLOODBORN em
 * {@link BloodbornHandler} — ambos via {@link AffinityRings#isWorn}.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class AffinityRingHandler {

    private static final String K_STILL_SINCE = "liberthia.lurker_still_since";
    private static final String K_LAST_X = "liberthia.lurker_x";
    private static final String K_LAST_Z = "liberthia.lurker_z";
    /** Ticks parado necessários (3s). */
    private static final long STILL_TICKS = 60;
    /** Distância² mínima pra contar como "movimento". */
    private static final double MOVE_EPS_SQR = 1.0E-4;

    private AffinityRingHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = event.player;
        if (p == null || p.level().isClientSide) return;

        long now = p.level().getGameTime();

        // r180 FIX: recomputa o max de Source 1×/s — pega o ring CAPACITY (+50) ao
        // equipar/guardar/remover. Antes o recomputeMax nunca era chamado por aqui,
        // então o anel de capacidade não dava nada (report #67).
        if (now % 20 == 0) {
            br.com.murilo.liberthia.observation.source.SourceData.recomputeMax(p);
        }

        // ── FIREWARP ──
        if (AffinityRings.isWorn(p, AffinityRingItem.Type.FIREWARP)) {
            if (now % 20 == 0) {
                SchoolResistanceHandler.setResistance(p, SpellSchool.FIRE, 30);
            }
            if (p.getRemainingFireTicks() > 0) p.setRemainingFireTicks(0);
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false));
        }

        // ── LURKER (invisível parado 3s) ──
        if (AffinityRings.isWorn(p, AffinityRingItem.Type.LURKER)) {
            handleLurker(p, now);
        } else {
            CompoundTag pd = p.getPersistentData();
            pd.remove(K_STILL_SINCE);
            pd.remove(K_LAST_X);
            pd.remove(K_LAST_Z);
        }
    }

    private static void handleLurker(Player p, long now) {
        CompoundTag pd = p.getPersistentData();
        double curX = p.getX();
        double curZ = p.getZ();

        boolean moving;
        if (!pd.contains(K_LAST_X)) {
            moving = true; // primeira amostra — assume movimento (reseta o timer)
        } else {
            double dx = curX - pd.getDouble(K_LAST_X);
            double dz = curZ - pd.getDouble(K_LAST_Z);
            moving = (dx * dx + dz * dz) > MOVE_EPS_SQR || p.isSprinting();
        }
        pd.putDouble(K_LAST_X, curX);
        pd.putDouble(K_LAST_Z, curZ);

        if (moving) {
            pd.putLong(K_STILL_SINCE, now);
            return;
        }
        if (!pd.contains(K_STILL_SINCE)) {
            pd.putLong(K_STILL_SINCE, now);
            return;
        }
        if (now - pd.getLong(K_STILL_SINCE) >= STILL_TICKS) {
            MobEffectInstance cur = p.getEffect(MobEffects.INVISIBILITY);
            if (cur == null || cur.getDuration() < 20) {
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false));
            }
        }
    }
}
