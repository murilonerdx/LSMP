package br.com.murilo.liberthia.block;

import br.com.murilo.liberthia.matter.MatterProfile;
import br.com.murilo.liberthia.matter.MatterProfileProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Bloco "variante infectada" — versão de dirt/sand/stone/grass impregnada
 * com matéria escura, clara ou amarela. v0.1.13.
 *
 * <p>Comportamento:
 * <ul>
 *   <li><b>stepOn:</b> ao pisar, aplica um efeito de status leve + ganha
 *       fração de matéria correspondente no perfil do player (se não tiver
 *       armadura/proteção apropriada).</li>
 *   <li><b>tick passivo (sem random ticks)</b> — efeito só ao pisar pra
 *       não causar lag.</li>
 * </ul>
 *
 * <p>Cada matéria tem 4 variantes (dirt/sand/stone/grass) com seu próprio
 * efeito e contribuição de matéria. Stack effect: pisar em vários blocos
 * acumula matéria mais rápido.
 */
public class InfectedVariantBlock extends Block {

    public enum MatterType {
        DARK,      // Dark Matter — Withering, Slowness, Blindness, XP drain
        WHITE,     // White Matter — memória/teleporte, levitate, glowing
        YELLOW     // Yellow Matter — hunger, nausea, weakness, descontrole
    }

    private final MatterType type;
    private final MobEffect onStepEffect;
    /** Quantos ticks dura o efeito quando pisa (default 80 = 4s). */
    private final int effectDurationTicks;
    /** Amplifier do efeito (0 = nível I, 1 = nível II). */
    private final int effectAmplifier;
    /** Quanto de matéria ganha no perfil ao pisar (0..1, ex: 0.5 = 0.5 de DM). */
    private final float matterGainPerStep;

    public InfectedVariantBlock(Properties props, MatterType type,
                                MobEffect onStepEffect,
                                int effectDurationTicks, int effectAmplifier,
                                float matterGainPerStep) {
        super(props);
        this.type = type;
        this.onStepEffect = onStepEffect;
        this.effectDurationTicks = effectDurationTicks;
        this.effectAmplifier = effectAmplifier;
        this.matterGainPerStep = matterGainPerStep;
    }

    public MatterType getType() { return type; }

    /**
     * Helper pra criar Properties base — dirt-like, sand-like, etc. variam a
     * dureza e som. Map color reflete o tipo de matéria pra renderizar bem no
     * minimapa.
     */
    public static Properties propsFor(MatterType type, Properties base) {
        MapColor color = switch (type) {
            case DARK   -> MapColor.COLOR_PURPLE;
            case WHITE  -> MapColor.SNOW;
            case YELLOW -> MapColor.COLOR_YELLOW;
        };
        return base.mapColor(color);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living)) return;
        // Server-side: aplica efeito + ganha matéria a cada 20 ticks (1x/s
        // mesmo se andar — Entity.stepOn é chamado a cada movimento de bloco)
        if (level.getGameTime() % 20 != 0) return;

        // Aplica efeito de status
        if (onStepEffect != null) {
            living.addEffect(new MobEffectInstance(
                    onStepEffect, effectDurationTicks, effectAmplifier, false, true));
        }

        // Ganha matéria no perfil (apenas players — mobs não têm perfil)
        if (entity instanceof Player player && matterGainPerStep > 0) {
            player.getCapability(MatterProfileProvider.CAP).ifPresent(this::applyMatterGain);
        }
    }

    private void applyMatterGain(MatterProfile profile) {
        switch (type) {
            case DARK   -> profile.setDark(Math.min(100f, profile.getDark() + matterGainPerStep));
            case WHITE  -> profile.setWhite(Math.min(100f, profile.getWhite() + matterGainPerStep));
            case YELLOW -> profile.setYellow(Math.min(100f, profile.getYellow() + matterGainPerStep));
        }
    }
}
