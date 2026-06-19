package br.com.murilo.liberthia.cosmic.horror.item;

import br.com.murilo.liberthia.cosmic.framework.HorrorFramework;
import br.com.murilo.liberthia.cosmic.framework.HorrorType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * r81 — <b>Veinbound Chestplate</b> (Flesh Horror armor).
 *
 * <p>Peitoral orgânico. Regenera 1 HP a cada 60 ticks (3s) MAS acumula
 * exposure FLESH passivamente. "Sussurra" — particle DAMAGE_INDICATOR
 * sobre o player.
 *
 * <p>Em troca: +5 max HP enquanto equipada.
 */
public class VeinboundChestplateItem extends ArmorItem {

    public VeinboundChestplateItem(Properties props) {
        super(ArmorMaterials.NETHERITE, ArmorItem.Type.CHESTPLATE,
                props.stacksTo(1).durability(800).fireResistant());
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        super.onArmorTick(stack, level, player);
        if (level.isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;

        // r164 FIX (bug #40): só checa se realmente está no slot CHEST.
        // Antes usava reference equality (chest != stack) — agora compara por item.
        ItemStack chest = sp.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || !chest.is(this)) return;

        long tick = level.getGameTime();

        // r164 FIX: regen agora SEMPRE funciona — se HP cheia, dá absorption
        // (corações amarelos) ao invés de só ignorar.
        if (tick % 60 == 0) {
            if (sp.getHealth() < sp.getMaxHealth()) {
                sp.heal(1.0F);
            } else {
                // HP cheia → dá absorption (cap 4 corações ext) pra player VER que tá funcionando
                float curAbs = sp.getAbsorptionAmount();
                if (curAbs < 8.0F) {
                    sp.setAbsorptionAmount(Math.min(8.0F, curAbs + 1.0F));
                }
            }
            // Particle damage indicator simulando "carne crescendo"
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        sp.getX(), sp.getY() + 1, sp.getZ(),
                        3, 0.3, 0.4, 0.3, 0.02);
                sl.sendParticles(ParticleTypes.HEART,
                        sp.getX(), sp.getY() + 2.2, sp.getZ(),
                        1, 0.1, 0.1, 0.1, 0.01);
            }
        }

        // Cada 100t, expose FLESH
        if (tick % 100 == 0) {
            HorrorFramework.getState(sp).addExposure(HorrorType.FLESH, 0.8F);
        }

        // Sussurro audível ocasional (Sound, não chat)
        if (tick % 600 == 0 && Math.random() < 0.3) {
            sp.level().playSound(null, sp.blockPosition(),
                    net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    0.4F, 0.6F + (float)Math.random() * 0.3F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§4§oCarne viva costurada em armadura."));
        tooltip.add(Component.literal("§c+1 HP §c§oa cada 3s (ou +1 absorption se HP cheia)"));
        tooltip.add(Component.literal("§eAbsorption máx: §6+8§e (4 corações)"));
        tooltip.add(Component.literal("§5§oExposição FLESH passiva. Ouve-se respirar."));
    }
}
