package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Comportamento de terror: mobs perto de um player com <b>Paranoia</b> alta
 * ficam ENCARANDO ele. Os hostis então <b>tremem</b>, dão uma <b>investida
 * rápida</b> e causam um dano leve — "mobs te observando e correndo atrás
 * tremendo". Quanto mais paranoia, mais mobs reagem, maior o raio e mais forte.
 *
 * <p>Otimizado: só roda pra players paranoicos (raro), a cada 10 ticks, com
 * raio e nº de mobs limitados.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ParanoiaMobBehavior {

    private ParanoiaMobBehavior() {}

    /** Abaixo disso, nada acontece (paranoia leve não atrai mobs). */
    private static final int THRESHOLD = 15;
    /** Teto de mobs afetados por player por ciclo (anti-lag). */
    private static final int MAX_MOBS = 6;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if ((sp.tickCount % 10) != 0) return;

        int paranoia = InsanityData.getParanoia(sp);
        if (paranoia < THRESHOLD) return;
        if (sp.isCreative() || sp.isSpectator()) return;

        ServerLevel sl = sp.serverLevel();
        float intensity = Math.min(1f, paranoia / 60f);     // 0..1
        double radius = 8.0 + intensity * 8.0;               // 8..16 blocos
        AABB box = sp.getBoundingBox().inflate(radius);

        // Players NÃO são Mob, então getEntitiesOfClass(Mob.class) já os exclui.
        List<Mob> mobs = sl.getEntitiesOfClass(Mob.class, box, Mob::isAlive);
        if (mobs.isEmpty()) return;

        int handled = 0;
        for (Mob mob : mobs) {
            if (handled >= MAX_MOBS) break;
            handled++;

            // Todos os mobs perto ENCARAM o player paranoico (creepy).
            mob.getLookControl().setLookAt(sp, 60f, 60f);

            // Só os HOSTIS tremem/investem/dão dano, com chance crescente.
            if (mob instanceof Monster
                    && sl.random.nextFloat() < (0.15f + intensity * 0.40f)) {
                Vec3 toPlayer = sp.position().subtract(mob.position()).normalize();
                // investida rápida
                mob.setDeltaMovement(mob.getDeltaMovement()
                        .add(toPlayer.x * 0.45, 0.12, toPlayer.z * 0.45));
                mob.hurtMarked = true;
                // tremor — jitter de rotação (cliente interpola como tremida)
                mob.setYRot(mob.getYRot() + (sl.random.nextFloat() - 0.5f) * 45f);
                // dano leve quando bem perto
                if (mob.distanceToSqr(sp) < 6.0) {
                    sp.hurt(sp.damageSources().mobAttack(mob), 1.0f + intensity * 2.0f);
                }
            }
        }
    }
}
