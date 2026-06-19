package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.entity.ClonePlayerEntity;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * v0.1.22 r23: Soul Cloner — Frasco do Clonador.
 *
 * <p>Right-click em outro player spawna um {@link ClonePlayerEntity} EXATAMENTE
 * idêntico ao alvo (mesma skin/profile/nome). O clone:
 * <ul>
 *   <li>Aparece com a SKIN do target (renderer usa OWNER_UUID pra fetchar
 *       game profile do client)</li>
 *   <li>NoAi=true — fica parado onde foi spawnado</li>
 *   <li>Invulnerable=true por 30s — não pode ser despertado fácil</li>
 *   <li>Despawna após 5 minutos (ou /kill)</li>
 *   <li>Não tem inventário visível, não causa dano</li>
 * </ul>
 *
 * <p>Uso narrativo: clone aparece em frente ao target = jumpscare. Caster
 * vê em terceira pessoa o "irmão gêmeo" do target em qualquer local.
 *
 * <h2>Cooldown</h2>
 * 60 segundos por uso. Stack 1, EPIC.
 */
public class SoulClonerItem extends Item {

    public static final int COOLDOWN_TICKS = 1200; // 60s
    /** Despawn automático em 5 min (6000t). */
    public static final int CLONE_TTL_TICKS = 6000;

    public SoulClonerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(target instanceof ServerPlayer targetPlayer)) {
            // Só clona players
            return InteractionResult.PASS;
        }
        if (!(user instanceof ServerPlayer caster)) return InteractionResult.PASS;
        if (caster.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
        if (!(caster.level() instanceof ServerLevel sl)) return InteractionResult.FAIL;

        // Spawna o clone na posição do target
        ClonePlayerEntity clone = ModEntities.CLONE_PLAYER.get().create(sl);
        if (clone == null) return InteractionResult.FAIL;
        clone.moveTo(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                targetPlayer.getYRot(), targetPlayer.getXRot());
        clone.setOwnerUuid(targetPlayer.getUUID());
        clone.setOwnerName(targetPlayer.getName().getString());
        clone.setCustomName(Component.literal(targetPlayer.getName().getString()));
        clone.setCustomNameVisible(true);
        clone.setNoAi(true);
        clone.setInvulnerable(true);
        sl.addFreshEntity(clone);

        // Efeitos visuais + som
        sl.sendParticles(ParticleTypes.PORTAL,
                clone.getX(), clone.getY() + 1.0, clone.getZ(),
                40, 0.4, 1.0, 0.4, 0.2);
        sl.sendParticles(ParticleTypes.SOUL,
                clone.getX(), clone.getY() + 1.0, clone.getZ(),
                15, 0.3, 0.5, 0.3, 0.05);
        sl.playSound(null, clone.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2F, 0.7F);

        // Schedule despawn after 5 min via tick counter (simples — armazena
        // tick limit no NBT do clone, MadnessEvents.onServerTick remove)
        clone.getPersistentData().putLong("liberthia.clone_despawn",
                sl.getGameTime() + CLONE_TTL_TICKS);

        caster.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        caster.displayClientMessage(Component.literal(
                "§5✦ Você criou um clone de §d" + targetPlayer.getName().getString()
                        + "§5. Durará 5 minutos."), true);

        // Notifica o target (jumpscare aviso)
        targetPlayer.displayClientMessage(Component.literal(
                "§4§oAlgo apareceu... §4§lcom seu rosto.").withStyle(ChatFormatting.DARK_RED), true);

        if (!user.getAbilities().instabuild) stack.hurtAndBreak(1, user,
                p -> p.broadcastBreakEvent(hand));
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        tip.add(Component.literal("§b§oFrasco do Clonador").withStyle(ChatFormatting.ITALIC));
        tip.add(Component.literal("§7Right-click em outro player: spawna um §dclone exato§r§7"));
        tip.add(Component.literal("§7com a §emesma skin e nome§r§7. Dura §65 minutos§r§7."));
        tip.add(Component.literal("§7Cooldown: §c60s§r§7. Durabilidade limitada."));
        tip.add(Component.empty());
        tip.add(Component.literal("§8§oUma cópia perfeita... ou era você o original?"));
    }
}
