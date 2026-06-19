package br.com.murilo.liberthia.matter;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Helper pra tocar sons de matter (Dark/White/Yellow) APENAS pra players
 * infectados na vizinhança. Quem não tem matter no profile não escuta nada —
 * é como se os sons vivessem na "frequência" do infectado.
 *
 * <p>Mecânica narrativa: a matter "fala" com quem a carrega. Sussurros, pulsos,
 * estática — só audíveis pra quem foi exposto. Player limpo passa do lado de
 * um Dark Matter Block e só ouve o ambiente normal do jogo. Player com 30+ de
 * DM no profile ouve o bloco pulsando, sussurros, etc.
 *
 * <p>Implementação: {@link ServerLevel#playSound} broadcasta pra todos no raio
 * sound — usar {@code null} como playerExcluded faz TODOS ouvirem. Aqui a
 * gente IGNORA o broadcast e itera os ServerPlayers manualmente, chamando
 * {@link ServerPlayer#playNotifySound} (envia ClientboundSoundPacket SÓ pro
 * player alvo). Outros nem sabem que houve som.
 *
 * <p>Critério de "infectado": qualquer profile.dark/white/yellow > {@link #THRESHOLD}.
 * Threshold baixo (5) garante que mesmo exposição leve já habilita os sons —
 * faz parte da curva de "agora você ouve coisas que antes não escutava".
 */
public final class MatterSoundUtil {

    /** Threshold mínimo de matter pra começar a ouvir os sons. */
    public static final float THRESHOLD = 5.0f;

    /** Raio em blocos do alcance do som — fora disso ninguém escuta. */
    public static final double RADIUS = 32.0;

    private MatterSoundUtil() {}

    public enum MatterChannel { DARK, WHITE, YELLOW, ANY }

    /**
     * Toca {@code sound} em {@code pos} apenas pros players infectados com
     * matter do canal especificado (ou qualquer matter se {@link MatterChannel#ANY}).
     *
     * <p>Se nenhum player no raio tá infectado, o som NÃO toca pra ninguém —
     * é a degradação esperada (efeito "ninguém sente um pulso vazio").
     */
    public static void playToInfected(ServerLevel level, BlockPos pos,
                                       SoundEvent sound, SoundSource source,
                                       float volume, float pitch,
                                       MatterChannel channel) {
        if (level == null || sound == null) return;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        double r2 = RADIUS * RADIUS;
        for (ServerPlayer sp : level.players()) {
            if (sp.distanceToSqr(x, y, z) > r2) continue;
            if (!isInfected(sp, channel)) continue;
            sp.playNotifySound(sound, source, volume, pitch);
        }
    }

    /** Versão de conveniência — toca pra todos infectados com QUALQUER matter. */
    public static void playToAnyInfected(ServerLevel level, BlockPos pos,
                                          SoundEvent sound, SoundSource source,
                                          float volume, float pitch) {
        playToInfected(level, pos, sound, source, volume, pitch, MatterChannel.ANY);
    }

    /**
     * True se o player tem matter do canal especificado acima do threshold.
     * Lê o profile via capability — se a cap não existe (player novo, antes
     * do attach), retorna false.
     */
    public static boolean isInfected(ServerPlayer sp, MatterChannel channel) {
        return sp.getCapability(MatterProfileProvider.CAP).map(profile -> switch (channel) {
            case DARK   -> profile.getDark()   >= THRESHOLD;
            case WHITE  -> profile.getWhite()  >= THRESHOLD;
            case YELLOW -> profile.getYellow() >= THRESHOLD;
            case ANY    -> profile.getDark()   >= THRESHOLD
                        || profile.getWhite()  >= THRESHOLD
                        || profile.getYellow() >= THRESHOLD;
        }).orElse(false);
    }
}
