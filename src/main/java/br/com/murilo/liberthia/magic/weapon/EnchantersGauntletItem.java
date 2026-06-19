package br.com.murilo.liberthia.magic.weapon;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * v0.1.24 r97 / fix r179: <b>Enchanter's Gauntlet</b> — soco místico boomerang
 * + dano de melee próprio.
 *
 * <p><b>Melee:</b> +6 ATTACK_DAMAGE (7 total) — bater direto num mob agora dói.
 *
 * <p><b>Right-click (habilidade):</b> raio místico até 12 blocos. Particles
 * percorrem até o alvo, causam 7 de dano ELDRITCH. Cooldown 30t.
 *
 * <p>r179 FIX: a habilidade usava {@code sp.pick(12,0,false)} que SÓ detecta
 * blocos ({@code Entity.pick()} nunca retorna EntityHitResult) → a habilidade
 * NUNCA acertava ninguém. Agora usa {@link br.com.murilo.liberthia.util.EntityRaycast}.
 * E como era um {@link Item} puro sem modificadores, o golpe melee dava dano de
 * mão vazia — adicionados ATTACK_DAMAGE/ATTACK_SPEED (padrão do StaffItem).
 */
public class EnchantersGauntletItem extends Item {

    private static final UUID DAMAGE_UUID = UUID.fromString("b7f3a1c4-9d2e-4a6b-8c1f-3e5a7b9d2c4e");
    private static final UUID SPEED_UUID = UUID.fromString("b7f3a1c4-9d2e-4a6b-8c1f-3e5a7b9d2c4f");
    private static final float MELEE_DAMAGE = 6.0F;   // +6 → 7 total
    private static final float ABILITY_DAMAGE = 7.0F;
    private static final double RANGE = 12.0;

    private final Multimap<Attribute, AttributeModifier> attribs;

    public EnchantersGauntletItem(Properties props) {
        super(props.stacksTo(1).durability(150));
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(DAMAGE_UUID,
                "Gauntlet damage", MELEE_DAMAGE, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(SPEED_UUID,
                "Gauntlet speed", -2.0F, AttributeModifier.Operation.ADDITION)); // 2.0 atk/s
        this.attribs = b.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? attribs : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);

        ServerLevel sl = sp.serverLevel();
        Vec3 from = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();

        // Particle trail FORWARD
        for (int i = 1; i <= RANGE; i++) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    from.x + look.x * i,
                    from.y + look.y * i,
                    from.z + look.z * i,
                    2, 0.1, 0.1, 0.1, 0.02);
        }

        // r179 FIX: raycast de entidade REAL (Entity.pick() só pega blocos)
        LivingEntity target = br.com.murilo.liberthia.util.EntityRaycast.pickLiving(sp, RANGE);
        if (target != null) {
            target.hurt(SchoolDamageSource.eldritch().toVanilla(sl, sp), ABILITY_DAMAGE);
            sl.sendParticles(ParticleTypes.SOUL,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    15, 0.3, 0.5, 0.3, 0.05);
            sl.playSound(null, target.blockPosition(),
                    net.minecraft.sounds.SoundEvents.SOUL_ESCAPE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
        }

        // Particle trail BACK (boomerang return)
        sl.getServer().tell(new net.minecraft.server.TickTask(sl.getServer().getTickCount() + 5, () -> {
            for (int i = (int) RANGE; i >= 1; i--) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        from.x + look.x * i,
                        from.y + look.y * i,
                        from.z + look.z * i,
                        1, 0.05, 0.05, 0.05, 0);
            }
        }));

        sp.getCooldowns().addCooldown(this, 30);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§5Manopla do Encantador"));
        tooltip.add(Component.literal("§7Equipe nas §dmãos§7 (Curios):"));
        tooltip.add(Component.literal("§7 golpe §lforte§r§7 → §5fraqueza + lentidão + eldritch §7no alvo"));
        tooltip.add(Component.literal("§8Na mão: soco boomerang (raio 12) + 7 de melee"));
    }
}
