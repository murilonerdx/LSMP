package br.com.murilo.liberthia.cosmic.relic;

import br.com.murilo.liberthia.cosmic.scare.ScareS2CPacket;
import br.com.murilo.liberthia.cosmic.scare.ScareType;
import br.com.murilo.liberthia.loom.entity.PeripheralObserverEntity;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.registry.ModSounds;
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
import net.minecraft.world.phys.Vec3;

/**
 * r178: <b>Boneca</b> — segure-a e ela "vira a cabeça para você". Ao usar, um Observador
 * aparece logo atrás de você (no canto do olho) e some quando você olha. Ter a boneca
 * por perto convida coisas a te observar.
 */
public class CursedDollItem extends Item implements br.com.murilo.liberthia.cosmic.HorrorUsableOnOther {

    public CursedDollItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public void useOnOther(ServerPlayer user, ServerPlayer target) {
        if (!(target.level() instanceof ServerLevel sl)) return;
        target.sendSystemMessage(Component.literal("§8A boneca virou a cabeça para você."));
        ModNetwork.sendToPlayer(target, new ScareS2CPacket(ScareType.EYES, 90, 0, ""));
        PeripheralObserverEntity obs = ModEntities.LOOM_PERIPHERAL.get().create(sl);
        if (obs != null) {
            Vec3 look = target.getLookAngle();
            obs.moveTo(target.getX() - look.x * 8, target.getY(), target.getZ() - look.z * 8,
                    sl.random.nextFloat() * 360F, 0);
            obs.startHaunt(target.getUUID(), 2);
            sl.addFreshEntity(obs);
        }
        sl.playSound(null, target.blockPosition(), ModSounds.WATCHER_BREATH.get(), SoundSource.HOSTILE, 0.5F, 0.7F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            sp.sendSystemMessage(Component.literal("§8A boneca virou a cabeça para você."));
            ModNetwork.sendToPlayer(sp, new ScareS2CPacket(ScareType.EYES, 90, 0, ""));

            PeripheralObserverEntity obs = ModEntities.LOOM_PERIPHERAL.get().create(sl);
            if (obs != null) {
                Vec3 look = sp.getLookAngle();
                obs.moveTo(sp.getX() - look.x * 8, sp.getY(), sp.getZ() - look.z * 8,
                        sl.random.nextFloat() * 360F, 0);
                obs.startHaunt(sp.getUUID(), 2); // some sozinho em ~2 min
                sl.addFreshEntity(obs);
            }
            sl.playSound(null, sp.blockPosition(), ModSounds.WATCHER_BREATH.get(), SoundSource.HOSTILE, 0.5F, 0.7F);
            player.getCooldowns().addCooldown(this, 100);
        }
        return InteractionResultHolder.success(stack);
    }
}
