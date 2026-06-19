package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.cosmic.horror.entity.VoidManifestationEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <b>Olho do Vazio</b> — invoca uma {@link VoidManifestationEntity}: uma sombra negra
 * com olhos vermelhos girando e tentáculos, igual à invocação do Underzealot/Gloomoth.
 *
 * <p>Use no chão/bloco (botão direito) → a manifestação surge logo acima da face clicada
 * e paira por ~11s antes de dissipar. Cooldown de 6s pra não floodar.
 */
public class VoidEyeItem extends Item {

    public VoidEyeItem(Properties props) {
        super(props.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedFace() != null
                ? ctx.getClickedPos().relative(ctx.getClickedFace())
                : ctx.getClickedPos().above();

        if (!level.isClientSide && level instanceof ServerLevel sl) {
            VoidManifestationEntity ent = ModEntities.VOID_MANIFESTATION.get().create(sl);
            if (ent != null) {
                ent.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                        level.random.nextFloat() * 360F, 0F);
                sl.addFreshEntity(ent);
                sl.playSound(null, pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.7F, 0.6F);
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 40, 0.6, 0.8, 0.6, 0.1);
                if (player != null) player.getCooldowns().addCooldown(this, 120);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Use no chão para invocar a presença.").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("Uma sombra de muitos olhos se ergue do nada.")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tip.add(Component.literal("✦ Manifestação do Vazio").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
    }
}
