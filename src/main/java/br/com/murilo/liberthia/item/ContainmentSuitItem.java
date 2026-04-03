package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.InfectionLogic;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ContainmentSuitItem extends ArmorItem {
    private static final int MAX_CHARGE = 6000; // 5 minutes at 20 tps
    private static final String TAG_SUIT_CHARGE = "suit_charge";

    public ContainmentSuitItem(ArmorItem.Type type, Properties properties) {
        super(ContainmentSuitArmorMaterial.INSTANCE, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide || !(entity instanceof Player player)) return;

        // Only process logic on the chestplate to avoid running 4x
        if (this.getType() != Type.CHESTPLATE) return;

        if (!isFullSetWorn(player)) return;

        // Initialize charge if not present
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(TAG_SUIT_CHARGE)) {
            tag.putInt(TAG_SUIT_CHARGE, MAX_CHARGE);
        }

        int charge = tag.getInt(TAG_SUIT_CHARGE);
        float density = InfectionLogic.getChunkInfectionDensity(level, player.blockPosition());
        boolean inInfectionZone = density > 0.0F;

        if (inInfectionZone && charge > 0) {
            // Deplete 1 charge per tick in infection zones
            charge--;
            tag.putInt(TAG_SUIT_CHARGE, charge);

            // Grant near immunity via CLEAR_SHIELD effect
            player.addEffect(new MobEffectInstance(ModEffects.CLEAR_SHIELD.get(), 40, 0, false, false, true));
        } else if (charge <= 0 && inInfectionZone) {
            // No protection remaining, play warning sound every 100 ticks
            if (player.tickCount % 100 == 0) {
                level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND,
                        SoundSource.PLAYERS, 0.5F, 2.0F);
            }
        }
    }

    private boolean isFullSetWorn(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack armorStack = player.getItemBySlot(slot);
            if (!(armorStack.getItem() instanceof ContainmentSuitItem)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        CompoundTag tag = stack.getOrCreateTag();
        int charge = tag.contains(TAG_SUIT_CHARGE) ? tag.getInt(TAG_SUIT_CHARGE) : MAX_CHARGE;

        int totalSeconds = charge / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        tooltipComponents.add(Component.literal(
                String.format("\u00a7bCharge: %d:%02d", minutes, seconds)
        ));

        if (charge <= 0) {
            tooltipComponents.add(Component.literal("\u00a7c[DEPLETED] No protection active"));
        }
    }
}
