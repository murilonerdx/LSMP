package br.com.murilo.liberthia.item;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pulso — artifact que dispara um pulso sônico cone-direcional similar ao
 * Warden. Cooldown 20s.
 *
 * <h3>Mecânica</h3>
 * <ul>
 *   <li>Direção: olhar do player.</li>
 *   <li>Alcance: ~15 blocos em linha reta.</li>
 *   <li>Cone: largura ~3 blocos em torno do eixo do olhar.</li>
 *   <li>Dano: 20 por target.</li>
 *   <li>Debuffs: Weakness II + Mining Fatigue III + Slowness III por 8s.</li>
 *   <li>Knockback radial pra fora do player.</li>
 *   <li>Particles SONIC_BOOM + som WARDEN_SONIC_BOOM.</li>
 *   <li>Cooldown 20s (400 ticks) no item.</li>
 * </ul>
 *
 * <p>Slot Curios: charm (tag genérica). Pode também ser segurado na mão e
 * usado por right-click sem precisar equipar.
 */
public class PulsoItem extends Item {

    /** Alcance do pulso em blocos. */
    private static final double RANGE = 15.0;
    /** Raio do cone em torno do eixo (efetivamente largura/2). */
    private static final double CONE_RADIUS = 2.5;
    /** Dano fixo por target. */
    private static final float DAMAGE = 20.0f;
    /** Cooldown em ticks (20s). */
    private static final int COOLDOWN_TICKS = 400;

    public PulsoItem(Properties props) {
        super(props);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.consume(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel sl = sp.serverLevel();
        Vec3 origin = sp.position().add(0, sp.getEyeHeight() * 0.5, 0);
        Vec3 look = sp.getLookAngle();
        Vec3 end = origin.add(look.scale(RANGE));

        // Particles SONIC_BOOM ao longo do feixe (10 pontos)
        for (int i = 1; i <= 10; i++) {
            double t = i / 10.0;
            Vec3 p = origin.add(look.scale(RANGE * t));
            sl.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        sl.playSound(null, sp.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 1.0F);

        // Procura targets num bounding box em volta do feixe
        AABB area = new AABB(origin, end).inflate(CONE_RADIUS);
        for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != sp && e.isAlive())) {
            // Filtra por distância ao eixo do feixe (cone test)
            Vec3 toTarget = target.position().subtract(origin);
            double along = toTarget.dot(look);
            if (along < 0 || along > RANGE) continue;
            Vec3 perp = toTarget.subtract(look.scale(along));
            if (perp.length() > CONE_RADIUS) continue;

            // Hit!
            target.hurt(sp.damageSources().sonicBoom(sp), DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 2));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 2));
            // Knockback radial leve pra fora
            Vec3 away = toTarget.normalize();
            target.setDeltaMovement(away.x * 1.2, 0.4, away.z * 1.2);
            target.hurtMarked = true;
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        sp.displayClientMessage(Component.literal("✦ Pulso disparado")
                .withStyle(ChatFormatting.AQUA), true);

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("✦ Pulso Sônico")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Right-click: dispara onda sônica em cone")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§b• Dano: 20 por alvo"));
        tooltip.add(Component.literal("§b• Alcance: 15 blocos"));
        tooltip.add(Component.literal("§b• Debuffs: Weakness II + Fatigue III + Slowness III (8s)"));
        tooltip.add(Component.literal("§b• Cooldown: 20s"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Slot Curios: charm")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
