package br.com.murilo.liberthia.magic.spell;

import br.com.murilo.liberthia.magic.school.SpellSchool;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * r146: Helper centralizado de sons mágicos por escola.
 *
 * <p>Antes cada {@link SpellProjectileEntity#discardWithBurst()} mapeava
 * inline sons vanilla genéricos. Agora todo som mágico (charge start,
 * cast finish, impact) passa por aqui — fica fácil substituir por sons
 * customizados no futuro sem mexer em N arquivos.
 *
 * <p>Cada escola tem 3 fases:
 * <ul>
 *   <li>{@code playCastStart()} — começa o channel (player segura right-click)</li>
 *   <li>{@code playCastFinish()} — channel completou, projétil sai (ou cast self)</li>
 *   <li>{@code playImpact()} — projétil bate em alvo/bloco</li>
 * </ul>
 *
 * <p>Sons mapeados criativamente com vanilla pra cada escola ter caráter próprio.
 * Substituir esse arquivo é o ÚNICO ponto necessário pra adicionar OGGs custom.
 */
public final class MagicSounds {

    private MagicSounds() {}

    /** Som de início de channel (player começou a segurar o item). */
    public static SoundEvent castStart(SpellSchool school) {
        return switch (school) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case ICE -> SoundEvents.GLASS_PLACE;
            case LIGHTNING -> SoundEvents.BEACON_POWER_SELECT;
            case BLOOD -> SoundEvents.WITHER_AMBIENT;
            case ELDRITCH -> SoundEvents.PORTAL_AMBIENT;
            case HOLY -> SoundEvents.BEACON_AMBIENT;
            case NATURE -> SoundEvents.AZALEA_PLACE;
        };
    }

    /** Som de fim de channel (cast finalizou — projétil disparou). */
    public static SoundEvent castFinish(SpellSchool school) {
        return switch (school) {
            case FIRE -> SoundEvents.FIRECHARGE_USE;
            case ICE -> SoundEvents.SNOWBALL_THROW;
            case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case BLOOD -> SoundEvents.WITHER_SHOOT;
            case ELDRITCH -> SoundEvents.ENDERMAN_TELEPORT;
            case HOLY -> SoundEvents.ALLAY_ITEM_GIVEN;
            case NATURE -> SoundEvents.BAMBOO_PLACE;
        };
    }

    /** Som de impacto do projétil. */
    public static SoundEvent impact(SpellSchool school) {
        return switch (school) {
            case FIRE -> SoundEvents.GENERIC_EXPLODE;
            case ICE -> SoundEvents.GLASS_BREAK;
            case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case BLOOD -> SoundEvents.WITHER_HURT;
            case ELDRITCH -> SoundEvents.PORTAL_TRIGGER;
            case HOLY -> SoundEvents.BEACON_ACTIVATE;
            case NATURE -> SoundEvents.AZALEA_LEAVES_BREAK;
        };
    }

    /** Som secundário em pitch alto pra dar "shimmer" no impacto. */
    public static SoundEvent impactSecondary(SpellSchool school) {
        return switch (school) {
            case FIRE -> SoundEvents.FIRE_EXTINGUISH;
            case ICE -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case LIGHTNING -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case BLOOD -> SoundEvents.GHAST_HURT;
            case ELDRITCH -> SoundEvents.ENDERMAN_STARE;
            case HOLY -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case NATURE -> SoundEvents.AZALEA_PLACE;
        };
    }

    // ─── Helpers de tocar sons direto ──────────────────────────────────

    /** Toca o som de início de cast (canal aberto) — leve, em pitch médio. */
    public static void playCastStart(ServerPlayer player, SpellSchool school) {
        player.serverLevel().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                castStart(school), SoundSource.PLAYERS,
                0.8F, 0.9F + player.getRandom().nextFloat() * 0.2F);
    }

    /** Toca o som de fim de cast (projétil saiu) — em pitch normal alto. */
    public static void playCastFinish(ServerPlayer player, SpellSchool school) {
        player.serverLevel().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                castFinish(school), SoundSource.PLAYERS,
                1.0F, 1.0F + player.getRandom().nextFloat() * 0.1F);
    }

    /** Toca duas camadas no impacto (uma grave + uma aguda). */
    public static void playImpact(Level level, Vec3 pos, SpellSchool school, Entity src) {
        level.playSound(null, pos.x, pos.y, pos.z,
                impact(school), SoundSource.PLAYERS, 1.2F, 0.8F);
        level.playSound(null, pos.x, pos.y, pos.z,
                impactSecondary(school), SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    /** Toca som de cancel/fizzle (channel interrompido cedo). */
    public static void playCastCancel(ServerPlayer player) {
        player.serverLevel().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 0.7F);
    }
}
