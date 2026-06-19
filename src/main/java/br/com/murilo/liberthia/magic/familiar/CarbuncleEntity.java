package br.com.murilo.liberthia.magic.familiar;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.1.24 r109: <b>Carbuncle</b> — variante Starbuncle especializado em
 * coletar items VALIOSOS (ores, gems, magic items). Filtro automático
 * baseado em rarity.
 *
 * <p>Picks up apenas items com rarity UNCOMMON+ ou cujo nome contém
 * "ore"/"gem"/"crystal". Dropa pra próprio inventário (sem chest linkado).
 */
public class CarbuncleEntity extends PathfinderMob {

    private final ItemStack[] internalInventory = new ItemStack[6];

    public CarbuncleEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        for (int i = 0; i < internalInventory.length; i++) internalInventory[i] = ItemStack.EMPTY;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel sl)) return;

        // Sparkle
        if (tickCount % 6 == 0) {
            sl.sendParticles(ParticleTypes.GLOW,
                    getX(), getY() + 0.4, getZ(),
                    1, 0.2, 0.2, 0.2, 0.02);
        }

        // A cada 20t, scan items próximos
        if (tickCount % 20 == 0) {
            var items = sl.getEntitiesOfClass(ItemEntity.class,
                    getBoundingBox().inflate(8));
            for (ItemEntity ie : items) {
                if (ie.hasPickUpDelay()) continue;
                ItemStack stack = ie.getItem();
                if (!isValuable(stack)) continue;
                if (tryInsertItem(stack)) {
                    ie.discard();
                    if (isInventoryFull()) break;
                }
            }
        }

        // Right-click player gives items? Future. Por enquanto, drop tudo na morte.
    }

    private boolean isValuable(ItemStack stack) {
        String key = stack.getDescriptionId().toLowerCase();
        if (stack.getRarity() != net.minecraft.world.item.Rarity.COMMON) return true;
        return key.contains("ore") || key.contains("gem") || key.contains("crystal")
                || key.contains("ingot") || key.contains("diamond")
                || key.contains("source") || key.contains("matter");
    }

    private boolean tryInsertItem(ItemStack stack) {
        for (int i = 0; i < internalInventory.length; i++) {
            if (internalInventory[i].isEmpty()) {
                internalInventory[i] = stack.copy();
                return true;
            }
            if (ItemStack.isSameItemSameTags(internalInventory[i], stack)
                    && internalInventory[i].getCount() + stack.getCount() <= 64) {
                internalInventory[i].grow(stack.getCount());
                return true;
            }
        }
        return false;
    }

    private boolean isInventoryFull() {
        for (ItemStack s : internalInventory) if (s.isEmpty()) return false;
        return true;
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        // Dropa inventário na morte
        for (ItemStack stack : internalInventory) {
            if (!stack.isEmpty()) {
                spawnAtLocation(stack);
            }
        }
        super.die(source);
    }
}
