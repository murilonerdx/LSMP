package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.cosmic.insanity.InsanityData;
import br.com.murilo.liberthia.data.ChunkInfectionData;
import br.com.murilo.liberthia.dimension.SpiritDimension;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * r180b — <b>Cristal de Foco</b> (parte do trio de contenção de matéria escura).
 * Estabilizador PORTÁTIL: right-click "foca a realidade" — reduz a corrupção
 * ({@link ChunkInfectionData}) nos 3×3 chunks à volta, limpa Escuridão/Náusea/Wither,
 * dá regen + sanidade e acalma a mente. Cooldown de 30s; gasta durabilidade. Também
 * é o núcleo de craft do {@code Raio Estabilizador}.
 */
public class FocusCrystalItem extends Item {

    private static final int COOLDOWN = 600; // 30s
    private static final int REDUCE = 3;

    public FocusCrystalItem(Properties props) {
        super(props.stacksTo(1).durability(64).rarity(Rarity.EPIC));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!(level instanceof ServerLevel sl)) return InteractionResultHolder.pass(stack);

        ChunkInfectionData data = ChunkInfectionData.get(sl);
        ChunkPos cp = new ChunkPos(player.blockPosition());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos c = new ChunkPos(cp.x + dx, cp.z + dz);
                int v = data.getContamination(c);
                if (v > 0) data.setContamination(c, Math.max(0, v - REDUCE));
            }
        }

        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.WITHER);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, true));
        if (player instanceof ServerPlayer sp) {
            SpiritDimension.addSanity(sp, 8);
            try {
                br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                        new br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
                InsanityData.addParanoia(sp, -4);
                InsanityData.addInsanity(sp, -2);
            } catch (Throwable ignored) {}
        }

        sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(),
                40, 0.6, 0.8, 0.6, 0.05);
        sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
        player.displayClientMessage(Component.literal("§b✦ A realidade se foca ao seu redor."), true);

        player.getCooldowns().addCooldown(this, COOLDOWN);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("✦ Cristal de Foco").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("§7Usar: §bpurifica §7corrupção (3×3 chunks) + acalma a mente"));
        tip.add(Component.literal("§8§oRecarrega em 30s"));
    }
}
