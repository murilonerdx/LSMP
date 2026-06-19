package br.com.murilo.liberthia.cosmic.relic;

import br.com.murilo.liberthia.cosmic.emf.EmfSource;
import br.com.murilo.liberthia.cosmic.scare.ScareS2CPacket;
import br.com.murilo.liberthia.cosmic.scare.ScareType;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * r178: <b>Espelho Negro</b> — ao usar, a tela inteira espelha (inverte). Às vezes algo
 * "se mexe no reflexo": um rosto pisca e a sanidade cai. Quanto mais coisas de terror
 * por perto (EMF alto), maior a chance de o reflexo trair você.
 */
public class BlackMirrorItem extends Item implements br.com.murilo.liberthia.cosmic.HorrorUsableOnOther {

    public BlackMirrorItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public void useOnOther(ServerPlayer user, ServerPlayer target) {
        ModNetwork.sendToPlayer(target, new ScareS2CPacket(ScareType.MIRROR, 80, 0, ""));
        target.level().playSound(null, target.blockPosition(), ModSounds.COSMIC_GLITCH.get(), SoundSource.PLAYERS, 0.6F, 0.8F);
        double chance = 0.25 + EmfSource.intensityAt(target.level(), target.position(), target) * 0.5;
        if (target.getRandom().nextDouble() < chance) {
            ModNetwork.sendToPlayer(target, new ScareS2CPacket(ScareType.FLASH, 12, target.getRandom().nextInt(11), ""));
            target.sendSystemMessage(Component.literal("§8algo se mexeu no reflexo..."));
            SpiritDimension.addSanity(target, -3);
            ModNetwork.sendToPlayer(target, new SanitySyncS2CPacket(SpiritDimension.getSanity(target)));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.MIRROR, 80, 0, ""));
            level.playSound(null, sp.blockPosition(), ModSounds.COSMIC_GLITCH.get(), SoundSource.PLAYERS, 0.6F, 0.8F);

            double chance = 0.25 + EmfSource.intensityAt(level, sp.position(), sp) * 0.5;
            if (sp.getRandom().nextDouble() < chance) {
                ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.FLASH, 12, sp.getRandom().nextInt(11), ""));
                sp.sendSystemMessage(Component.literal("§8algo se mexeu no reflexo..."));
                SpiritDimension.addSanity(sp, -3);
                ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
            }
            player.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.success(stack);
    }
}
