package br.com.murilo.liberthia.cosmic.idol;

import br.com.murilo.liberthia.cosmic.scare.ScareS2CPacket;
import br.com.murilo.liberthia.cosmic.scare.ScareType;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.CapturePhotoS2CPacket;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * r178: <b>Câmera</b> — tira uma "foto" (flash branco + clique de obturador). A imagem
 * REAL capturada da tela é salva e vira uma {@link PhotographItem} no inventário, que
 * mostra a miniatura da foto no slot ({@code PhotographDecorator}). Às vezes a foto "sai
 * estranha" (cursed): um rosto pisca e a foto ganha moldura vermelha. Quanto mais coisas
 * de terror perto (EMF alto), maior a chance.
 */
public class CameraItem extends Item {

    public CameraItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            // flash + clique de obturador
            ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.SCREENSHOT, 12, 0, sp.getGameProfile().getName()));
            level.playSound(null, sp.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8F, 2.0F);

            // chance de "foto amaldiçoada" sobe com a intensidade de terror na região
            double chance = 0.22 + br.com.murilo.liberthia.cosmic.emf.EmfSource.intensityAt(level, sp.position(), sp) * 0.6;
            boolean cursed = sp.getRandom().nextDouble() < chance;
            if (cursed) {
                ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.FLASH, 14, sp.getRandom().nextInt(11), ""));
                sp.sendSystemMessage(Component.literal("§8a foto saiu estranha... tinha algo atrás de você."));
            }

            // id único da foto (server-side): uuid curto + gameTime
            String photoId = sp.getStringUUID().substring(0, 8) + "_" + level.getGameTime();
            // manda o CLIENTE capturar a tela e salvar como esse id
            ModNetwork.sendToPlayer(sp, new CapturePhotoS2CPacket(photoId));

            // legenda com coords + dia
            long day = level.getDayTime() / 24000L;
            String caption = "Dia " + day + " - " + sp.blockPosition().getX() + ", " + sp.blockPosition().getZ();

            // entrega a Fotografia ao jogador (cai no chão se inventário cheio)
            ItemStack photo = PhotographItem.create(ModItems.PHOTOGRAPH.get(), photoId, cursed, caption);
            if (!sp.getInventory().add(photo)) {
                sp.drop(photo, false);
            }

            player.getCooldowns().addCooldown(this, 30);
        }
        return InteractionResultHolder.success(stack);
    }
}
