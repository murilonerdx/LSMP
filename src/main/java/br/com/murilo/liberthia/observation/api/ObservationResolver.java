package br.com.murilo.liberthia.observation.api;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;

/**
 * v0.1.22 r60: <b>ObservationResolver</b> — orchestrator do cast. Inspired
 * by AN's {@code SpellResolver}.
 *
 * <h2>Pipeline</h2>
 * <ol>
 *   <li>Valida recipe</li>
 *   <li>Cobra sanity</li>
 *   <li>Executa WatchMethod → obtém HitResult</li>
 *   <li>Itera Manifestations:
 *       <ul>
 *         <li>Coleta Distortions imediatas que seguem</li>
 *         <li>Acumula ObservationStats via builder</li>
 *         <li>Chama manifestation.manifest()</li>
 *         <li>Pula índices das Distortions já consumidas</li>
 *       </ul>
 *   </li>
 * </ol>
 */
public final class ObservationResolver {

    private ObservationResolver() {}

    public static boolean cast(ServerPlayer caster, ObservationSpell spell) {
        if (!(caster.level() instanceof ServerLevel level)) return false;

        // Validação
        String err = spell.validate();
        if (err != null) {
            caster.displayClientMessage(Component.literal("§c⚠ Observação inválida: " + err), true);
            return false;
        }

        // r165 FIX: Calcula custo final com costMult ANTES do check — evita
        // "Source insuficiente" falso quando o player tem source suficiente pra
        // o custo reduzido (mas não pro custo base).
        int sourceCost = spell.totalSourceCost();
        float costMult = br.com.murilo.liberthia.observation.source.MagicLevelData.getCostMult(caster);
        int finalCost = Math.max(1, (int)(sourceCost * costMult));

        int currentSource = br.com.murilo.liberthia.observation.source.SourceData.get(caster);
        if (currentSource < finalCost) {
            caster.displayClientMessage(Component.literal(
                    "§c⚠ Source insuficiente §7(" + currentSource + "/" + finalCost + ")"), true);
            return false;
        }

        // r165: Sanidade NÃO é gate de cast. É recurso do Spirit World apenas.

        // Cria context
        ObservationContext ctx = new ObservationContext(spell, level, caster);

        // Executa WatchMethod (sempre é o primeiro)
        ObservationPart first = ctx.nextPart();
        if (!(first instanceof WatchMethod method)) {
            return false; // já validado, mas safety
        }
        HitResult hit = method.observe(caster, level, ctx);
        if (hit == null || ctx.isCanceled()) {
            caster.displayClientMessage(Component.literal(
                    "§7§oA observação se desfaz."), true);
            return false;
        }

        // r61: VFX pre-cast — spiral in
        br.com.murilo.liberthia.observation.vfx.CastVfx.spiralIn(caster, caster.position(), spell.color());

        // Itera resto do recipe
        try {
            while (ctx.hasNextPart()) {
                ObservationPart part = ctx.nextPart();
                if (!(part instanceof Manifestation manifest)) {
                    // Distortion órfã ou tipo inesperado, pula
                    continue;
                }

                // Coleta Distortions que seguem essa Manifestation
                var distortions = spell.distortionsAfter(ctx.currentIndex());

                // Constrói stats
                ObservationStats.Builder statBuilder = ObservationStats.builder()
                        .sanityCost(manifest.sanityCost());
                for (Distortion d : distortions) {
                    d.applyToStats(statBuilder, manifest);
                }
                ObservationStats stats = statBuilder.build();

                // r61: VFX burst at hit before manifest
                br.com.murilo.liberthia.observation.vfx.CastVfx.burstOut(
                    level, hit.getLocation(), spell.color(), (int)stats.intensity);

                // Executa manifestation
                manifest.manifest(hit, level, caster, stats, ctx);

                // r61: VFX linger after
                if (stats.duration > 60) {
                    br.com.murilo.liberthia.observation.vfx.CastVfx.lingerAt(
                        level, hit.getLocation(), spell.color(), stats.duration);
                }

                // Cancel check
                if (ctx.isCanceled()) break;

                // Pula índices das Distortions já consumidas
                for (int i = 0; i < distortions.size(); i++) {
                    ctx.nextPart();
                }
            }

            // Consome source (só depois de execução bem-sucedida)
            // r165: costMult já calculado antes do check — usa finalCost direto
            br.com.murilo.liberthia.observation.source.SourceData.consume(caster, finalCost);
            // r165: sanidade NÃO é consumida por feitiços — só drena no Spirit World
            // r71: ganha 1 XP por cast bem-sucedido
            br.com.murilo.liberthia.observation.source.MagicLevelEvents.awardXp(caster, 1, "cast");
            return true;
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[ObservationResolver] error in spell {}: {}",
                    spell.name(), t.toString());
            return false;
        }
    }
}
