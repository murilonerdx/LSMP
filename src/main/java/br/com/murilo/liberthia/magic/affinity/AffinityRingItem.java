package br.com.murilo.liberthia.magic.affinity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * v0.1.24 r93 / fix r179: <b>Affinity Ring</b> — anel com bônus de school +
 * efeito passivo único. Funciona equipado num slot Curios <b>ou</b> no inventário.
 *
 * <h2>5 variantes</h2>
 * <ul>
 *   <li>{@code FIREWARP} — +30 FIRE resist, imune a queimar, Fire Resistance.</li>
 *   <li>{@code LURKER} — parado por 3s → invisível.</li>
 *   <li>{@code TELEPORT} — clique direito teletransporta 8 blocos na direção do
 *       olhar (anti-parede). Cooldown 30s.</li>
 *   <li>{@code CAPACITY} — +50 max Source.</li>
 *   <li>{@code BLOODBORN} — ataques físicos curam 10% do dano.</li>
 * </ul>
 *
 * <p>r179: os efeitos passivos saíram do {@code inventoryTick} (que não roda em
 * slots Curios) e passaram para handlers centrais que consultam
 * {@link AffinityRings#isWorn}: {@link AffinityRingHandler} (FIREWARP/LURKER),
 * {@link BloodbornHandler} (BLOODBORN) e {@code SourceData.recomputeMax} (CAPACITY).
 * TELEPORT é ativo via {@link #use}.
 */
public class AffinityRingItem extends Item {

    public enum Type { FIREWARP, LURKER, TELEPORT, CAPACITY, BLOODBORN }

    /** Distância do blink do anel de teleporte. */
    private static final double TELEPORT_DIST = 8.0;
    /** Cooldown do blink (30s). */
    private static final int TELEPORT_COOLDOWN = 30 * 20;

    private final Type type;

    public AffinityRingItem(Properties props, Type type) {
        super(props.stacksTo(1));
        this.type = type;
    }

    public Type getType() { return type; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (type != Type.TELEPORT) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (level instanceof ServerLevel sl) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getViewVector(1.0F).normalize();
            Vec3 end = eye.add(look.scale(TELEPORT_DIST));

            // Raycast: não atravessa paredes — recua um pouco do bloco atingido.
            BlockHitResult hit = level.clip(new ClipContext(
                    eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 dest = (hit.getType() == HitResult.Type.MISS)
                    ? end
                    : hit.getLocation().subtract(look.scale(0.7));
            double feetY = dest.y - player.getEyeHeight();

            // VFX/som na origem
            sl.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                    32, 0.4, 0.6, 0.4, 0.15);
            sl.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);

            // Teleporte sincronizado (ServerPlayer) — fallback pro genérico fora isso.
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.teleportTo(sl, dest.x, feetY, dest.z, sp.getYRot(), sp.getXRot());
            } else {
                player.teleportTo(dest.x, feetY, dest.z);
            }
            player.fallDistance = 0.0F;

            // VFX/som no destino
            sl.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                    32, 0.4, 0.6, 0.4, 0.15);
            sl.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
        }

        player.getCooldowns().addCooldown(this, TELEPORT_COOLDOWN);
        player.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        switch (type) {
            case FIREWARP -> {
                tooltip.add(Component.literal("§c+30 Resistência a Fogo"));
                tooltip.add(Component.literal("§cImune a dano de fogo"));
                tooltip.add(Component.literal("§c+20% dano de feitiços de Fogo"));
            }
            case LURKER -> tooltip.add(Component.literal("§7Parado por 3s → §finvisível"));
            case TELEPORT -> {
                tooltip.add(Component.literal("§5Clique direito: teleporta 8 blocos"));
                tooltip.add(Component.literal("§8Cooldown 30s"));
            }
            case CAPACITY -> tooltip.add(Component.literal("§b+50 de Source máximo"));
            case BLOODBORN -> tooltip.add(Component.literal("§4Ataques físicos curam 10% do dano"));
        }
        tooltip.add(Component.literal("§8§oFunciona equipado §nou§r§8§o no inventário."));
    }
}
