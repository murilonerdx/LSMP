package br.com.murilo.liberthia.cosmic.relic;

import br.com.murilo.liberthia.dimension.SpiritDimension;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * r178: <b>Vela da Sanidade</b> — acende uma chama protetora. Restaura um pouco de sanidade
 * e marca você como "protegido" por 60s: nesse tempo a escuridão não apaga suas tochas
 * (ver {@code DarkFearManager}). Conforto raro num mundo que te observa.
 */
public class SanityCandleItem extends Item {

    /** Chave NBT lida pelo DarkFearManager: gameTime até quando o jogador está protegido. */
    public static final String WARDED_KEY = "liberthia.warded_until";

    public SanityCandleItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            SpiritDimension.addSanity(sp, 8);
            ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
            sp.getPersistentData().putLong(WARDED_KEY, sp.serverLevel().getGameTime() + 1200L);

            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLAME, sp.getX(), sp.getY() + 1.2, sp.getZ(), 8, 0.15, 0.25, 0.15, 0.005);
                sl.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 1.3, sp.getZ(), 5, 0.2, 0.3, 0.2, 0.002);
            }
            level.playSound(null, sp.blockPosition(), ModSounds.CLEAR_HUM.get(), SoundSource.PLAYERS, 0.5F, 1.2F);
            sp.sendSystemMessage(Component.literal("§eA chama te acalma. Por um instante, você não está sozinho... do jeito bom."));
            player.getCooldowns().addCooldown(this, 200);
        }
        return InteractionResultHolder.success(stack);
    }
}
