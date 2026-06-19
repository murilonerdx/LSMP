package br.com.murilo.liberthia.cosmic.relic;

import br.com.murilo.liberthia.cosmic.scare.ScareS2CPacket;
import br.com.murilo.liberthia.cosmic.scare.ScareType;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * r178: <b>Rádio Quebrado</b> — sintoniza estática e capta "transmissões" que não deviam
 * existir: sussurros distantes, passos falsos, gritos. De vez em quando, uma voz na
 * estática fala diretamente com você (narrador). Nunca toca a mesma coisa.
 */
public class BrokenRadioItem extends Item implements br.com.murilo.liberthia.cosmic.HorrorUsableOnOther {

    private static final List<Supplier<SoundEvent>> STATIONS = List.of(
            () -> ModSounds.COSMIC_RADIO_BROADCAST.get(),
            () -> ModSounds.COSMIC_DISTANT_WHISPERS.get(),
            () -> ModSounds.COSMIC_FALSE_FOOTSTEPS.get(),
            () -> ModSounds.COSMIC_DISTANT_SCREAM.get(),
            () -> ModSounds.COSMIC_VOID_BREATHING.get(),
            () -> ModSounds.AMBIENCE_DISTANT_WHISPERS.get()
    );

    private static final String[] VOICES = {
            "§7...você está aí?... eu sei que está...",
            "§7...não desligue... ainda não...",
            "§7...ele está bem atrás de você...",
            "§7...nós te encontramos...",
            "§7...socorro... tem alguém na sua casa...",
            "§7...continue cavando... mais fundo..."
    };

    public BrokenRadioItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            SoundEvent station = STATIONS.get(sp.getRandom().nextInt(STATIONS.size())).get();
            level.playSound(null, sp.blockPosition(), station, SoundSource.PLAYERS, 0.8F,
                    0.85F + sp.getRandom().nextFloat() * 0.3F);

            if (sp.getRandom().nextFloat() < 0.40F) {
                String voice = VOICES[sp.getRandom().nextInt(VOICES.length)];
                ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.NARRATOR, 90, 0, voice));
            }
            player.getCooldowns().addCooldown(this, 60);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void useOnOther(ServerPlayer user, ServerPlayer target) {
        SoundEvent station = STATIONS.get(target.getRandom().nextInt(STATIONS.size())).get();
        target.level().playSound(null, target.blockPosition(), station, SoundSource.PLAYERS, 0.8F,
                0.85F + target.getRandom().nextFloat() * 0.3F);
        // sempre fala uma voz no alvo (mais impactante quando "imposto")
        String voice = VOICES[target.getRandom().nextInt(VOICES.length)];
        ModNetwork.sendToPlayer(target, new ScareS2CPacket(ScareType.NARRATOR, 90, 0, voice));
    }
}
