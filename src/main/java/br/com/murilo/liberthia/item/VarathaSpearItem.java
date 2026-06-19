package br.com.murilo.liberthia.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * r180: <b>Lança de Varatha, a Lança do Submundo</b> — arma lendária.
 * <ul>
 *   <li>Dano base ~10, velocidade de ataque alta, <b>+5 de alcance</b> (atributo Forge).</li>
 *   <li><b>Perfuração Infernal:</b> +25% de dano que ignora armadura (mágico).</li>
 *   <li><b>Dança de Sangue:</b> acertos consecutivos somam +1 (até +5).</li>
 *   <li><b>Golpe Giratório:</b> segure o botão direito ~1,5s → dano em área (6b) + empurrão.</li>
 * </ul>
 * <p><b>Retorno Espiritual:</b> solte o botão direito RÁPIDO (&lt;1,5s) pra ARREMESSAR a
 * lança (bumerangue) — voa, causa dano (10 + 3 perfuração) e volta sozinha pro inventário
 * ({@link br.com.murilo.liberthia.entity.ThrownVarathaEntity}).
 */
public class VarathaSpearItem extends SwordItem {

    private static final UUID REACH_UUID = UUID.fromString("da4a7000-0001-4eee-9a01-0e0e0e0e0001");
    private static final int SPIN_HOLD = 30; // 1.5s

    private Multimap<Attribute, AttributeModifier> cachedMainhand;

    public VarathaSpearItem(Properties props) {
        super(Tiers.NETHERITE, 5, -1.6F, props); // ~10 de dano, velocidade alta
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);
        if (cachedMainhand == null) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
            b.putAll(super.getAttributeModifiers(slot, stack)); // ATK + SPEED
            try {
                Attribute reach = net.minecraftforge.common.ForgeMod.ENTITY_REACH.get();
                if (reach != null) {
                    b.put(reach, new AttributeModifier(REACH_UUID, "Varatha reach", 5.0,
                            AttributeModifier.Operation.ADDITION));
                }
            } catch (Throwable ignored) {
                // atributo de alcance ausente nesta versão — ignora (melee normal)
            }
            cachedMainhand = b.build();
        }
        return cachedMainhand;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean ok = super.hurtEnemy(stack, target, attacker);
        if (ok && !target.level().isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();
            long now = target.level().getGameTime();
            int combo = (now - tag.getLong("VarathaLastHit") < 60) ? tag.getInt("VarathaCombo") : 0;
            combo = Math.min(5, combo + 1);
            tag.putInt("VarathaCombo", combo);
            tag.putLong("VarathaLastHit", now);

            // Dança de Sangue (+1..+5) + Perfuração Infernal (25% ignorando armadura)
            float bonus = combo + 2.5F;
            target.hurt(target.damageSources().magic(), bonus);
            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIMSON_SPORE,
                        target.getX(), target.getY() + 1.0, target.getZ(), 6 + combo, 0.3, 0.4, 0.3, 0.02);
            }
        }
        return ok;
    }

    // ── Golpe Giratório: segura o botão direito ──
    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.SPEAR; }
    @Override public int getUseDuration(ItemStack s) { return 72000; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        int held = getUseDuration(stack) - timeLeft;
        if (held < SPIN_HOLD) { throwSpear(stack, level, player, held); return; } // soltar rápido → arremesso
        if (level.isClientSide || !(level instanceof ServerLevel sl)) return;

        Vec3 c = player.position();
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(6.0))) {
            if (e == player || !e.isAlive()) continue;
            e.hurt(e.damageSources().playerAttack(player), 9.0F);
            Vec3 away = e.position().subtract(c);
            if (away.lengthSqr() > 0.01) {
                away = away.normalize().scale(1.2);
                e.push(away.x, 0.45, away.z);
                e.hurtMarked = true;
            }
        }
        // VFX do giro
        for (int i = 0; i < 36; i++) {
            double a = i * (Math.PI / 18);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                    c.x + Math.cos(a) * 3, c.y + 1.0, c.z + Math.sin(a) * 3, 1, 0, 0, 0, 0);
        }
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.7F);
        stack.hurtAndBreak(3, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        player.getCooldowns().addCooldown(this, 60);
    }

    /** Retorno Espiritual — arremessa a lança como bumerangue (volta sozinha). */
    private void throwSpear(ItemStack stack, Level level, Player player, int held) {
        if (held < 4) return;                                  // ignora toque acidental
        if (player.getCooldowns().isOnCooldown(this)) return;
        if (level.isClientSide) return;
        br.com.murilo.liberthia.entity.ThrownVarathaEntity t =
                new br.com.murilo.liberthia.entity.ThrownVarathaEntity(level, player, stack);
        t.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.6F, 1.0F);
        level.addFreshEntity(t);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.9F);
        // a lança vira o projétil (volta sozinha) — tira da mão pra não duplicar
        player.setItemInHand(player.getUsedItemHand(), ItemStack.EMPTY);
        player.getCooldowns().addCooldown(this, 10);
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("§4Lança do Submundo").withStyle(ChatFormatting.DARK_RED));
        tip.add(Component.literal("§7Alcance §c+5§7 · Perfuração §c25%§7 (ignora armadura)"));
        tip.add(Component.literal("§7Dança de Sangue: acertos seguidos §c+1→+5"));
        tip.add(Component.literal("§7Solte §erápido§7: §cArremesso §7(bumerangue, volta sozinha)"));
        tip.add(Component.literal("§7Segure §edireito ~1.5s§7: §cGolpe Giratório §7(área 6b + empurrão)"));
    }
}
