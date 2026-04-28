package br.com.murilo.liberthia.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Boss artifact — purely a tag/data class. The actual aura logic lives in
 * {@link br.com.murilo.liberthia.event.BossArtifactEvents}, which scans
 * online players' inventories every second and applies the configured
 * effect to nearby OTHER players (never the holder).
 *
 * <p>Carrying multiple different artifacts stacks the auras and grants a
 * "Trinity Wrath" damage pulse via the events handler.
 */
public class BossArtifactItem extends Item {
    public final Supplier<MobEffect> effect;
    public final int amplifier;
    public final int durationTicks;
    public final double radius;
    public final String label;

    public BossArtifactItem(Properties props,
                            String label,
                            Supplier<MobEffect> effect,
                            int amplifier,
                            int durationTicks,
                            double radius) {
        super(props.stacksTo(1).rarity(Rarity.RARE));
        this.label = label;
        this.effect = effect;
        this.amplifier = amplifier;
        this.durationTicks = durationTicks;
        this.radius = radius;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§4§o" + label));
        tip.add(Component.literal("§7Carregar: pulsa o efeito em outros jogadores"));
        tip.add(Component.literal("§7em raio §c" + (int) radius + " blocos§7."));
        tip.add(Component.literal("§7Combine os 3 artefatos para a §4§lFúria da Mãe§7."));
    }
}
