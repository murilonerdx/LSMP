package br.com.murilo.liberthia.magic.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r172: <b>Orbe Arcano (consumível)</b> — 5 tipos. Right-click concede um
 * conjunto de buffs REAIS por 30s e consome o orbe (cooldown 5s). Também é
 * aceito no slot ORB do Arcane Workbench (compõe feitiços).
 */
public class ArcaneOrbConsumable extends Item {

    public enum Type {
        HEALING("Orbe da Cura", 0xFF66DD66, new Eff[]{
                new Eff(MobEffects.REGENERATION, 1), new Eff(MobEffects.ABSORPTION, 1)}, "§a+Regeneração II + Absorção"),
        WARDING("Orbe da Proteção", 0xFFCCAA44, new Eff[]{
                new Eff(MobEffects.DAMAGE_RESISTANCE, 1), new Eff(MobEffects.FIRE_RESISTANCE, 0)}, "§e+Resistência II + Resist. Fogo"),
        SWIFTNESS("Orbe da Rapidez", 0xFF66CCFF, new Eff[]{
                new Eff(MobEffects.MOVEMENT_SPEED, 1), new Eff(MobEffects.JUMP, 1), new Eff(MobEffects.DOLPHINS_GRACE, 0)}, "§b+Velocidade II + Salto + Golfinho"),
        INSIGHT("Orbe da Percepção", 0xFFAA88FF, new Eff[]{
                new Eff(MobEffects.NIGHT_VISION, 0), new Eff(MobEffects.WATER_BREATHING, 0)}, "§d+Visão Noturna + Resp. Aquática"),
        MIGHT("Orbe do Poder", 0xFFFF6644, new Eff[]{
                new Eff(MobEffects.DAMAGE_BOOST, 1), new Eff(MobEffects.DIG_SPEED, 1)}, "§c+Força II + Pressa II");

        public final String label; public final int color; public final Eff[] effs; public final String desc;
        Type(String l, int c, Eff[] e, String d) { this.label = l; this.color = c; this.effs = e; this.desc = d; }
    }

    public record Eff(MobEffect effect, int amp) {}

    private final Type type;
    public ArcaneOrbConsumable(Properties p, Type type) { super(p.stacksTo(16).rarity(Rarity.RARE)); this.type = type; }

    public Type type() { return type; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        for (Eff e : type.effs) player.addEffect(new MobEffectInstance(e.effect(), 600, e.amp(), false, true, true));
        player.getCooldowns().addCooldown(this, 100);
        if (!player.isCreative()) stack.shrink(1);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.3F);
        player.displayClientMessage(Component.literal("§5✦ §r§5" + type.label + " ativado."), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§5§o" + type.label));
        tip.add(Component.literal(type.desc + " §7(30s)"));
        tip.add(Component.literal("§8§oRight-click pra usar • aceito no slot Orb"));
    }
}
