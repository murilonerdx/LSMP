package br.com.murilo.liberthia.cosmic.emf;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r174: <b>Irregularidade Dimensional</b> — item de terror (admin).
 *
 * <p>Quem carrega este item <b>É</b> uma irregularidade dimensional ambulante:
 * os {@link EmfMeterItem EMF Meters} dos players próximos reagem (acendem,
 * piscam pro vermelho, apitam) conforme o portador se aproxima. Em intensidade
 * máxima, drena a sanidade de quem está perto demais.
 *
 * <p>O portador também é detectado por {@link EmfSource}. Solta uma fumaça
 * sutil pra dar presença (visível só de perto).
 */
public class DimensionalIrregularityItem extends Item {

    public DimensionalIrregularityItem(Properties props) {
        super(props.rarity(Rarity.EPIC).stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer sp)) return;
        if (level.getGameTime() % 10 != 0) return;
        if (level instanceof ServerLevel sl) {
            // presença sutil — leve distorção ao redor do portador
            sl.sendParticles(ParticleTypes.PORTAL,
                    sp.getX(), sp.getY() + 1.0, sp.getZ(), 2, 0.35, 0.6, 0.35, 0.02);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Uma dobra na realidade colada a você.")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("EMF Meters próximos reagem à sua presença.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Perto demais, a sanidade alheia se desfaz.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
