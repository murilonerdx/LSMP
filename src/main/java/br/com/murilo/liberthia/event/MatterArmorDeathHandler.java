package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.matter.MatterProfile;
import br.com.murilo.liberthia.matter.MatterProfileProvider;
import br.com.murilo.liberthia.registry.ModBlocks;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Outbreak on player death when wearing matter armor + matter level ≥ 95.
 *
 * <p>When a player dies wearing any piece of a given matter type AND the
 * corresponding matter profile value is ≥ 95, an outbreak triggers at the
 * death position: a matter block is placed (if air), a matter bucket item
 * is dropped, particles are spawned, a sound plays, and nearby living
 * entities (except the dead player) receive thematic debuffs.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MatterArmorDeathHandler {

    private MatterArmorDeathHandler() {}

    private enum MatterType { DARK, YELLOW, CLEAR }

    private enum OutbreakType { DARK, YELLOW, CLEAR }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;

        MatterProfile profile = sp.getCapability(MatterProfileProvider.CAP).orElse(null);
        if (profile == null) return;

        int dark = countMatterArmor(sp, MatterType.DARK);
        int yellow = countMatterArmor(sp, MatterType.YELLOW);
        int clear = countMatterArmor(sp, MatterType.CLEAR);

        if (dark > 0 && profile.getDark() >= 95f) outbreak(sp, OutbreakType.DARK);
        if (yellow > 0 && profile.getYellow() >= 95f) outbreak(sp, OutbreakType.YELLOW);
        if (clear > 0 && profile.getWhite() >= 95f) outbreak(sp, OutbreakType.CLEAR);
    }

    private static void outbreak(ServerPlayer sp, OutbreakType type) {
        Level level = sp.level();
        if (!(level instanceof ServerLevel sl)) return;
        BlockPos pos = sp.blockPosition();

        // 1) Place matter block at death pos (if air)
        if (sl.getBlockState(pos).isAir()) {
            switch (type) {
                case DARK -> sl.setBlockAndUpdate(pos, ModBlocks.DARK_MATTER_BLOCK.get().defaultBlockState());
                case YELLOW -> sl.setBlockAndUpdate(pos, ModBlocks.YELLOW_MATTER_BLOCK.get().defaultBlockState());
                case CLEAR -> sl.setBlockAndUpdate(pos, ModBlocks.CLEAR_MATTER_BLOCK.get().defaultBlockState());
            }
        }

        // 2) Drop a matter bucket as item entity
        ItemStack bucket = switch (type) {
            case DARK -> new ItemStack(ModItems.DARK_MATTER_BUCKET.get());
            case YELLOW -> new ItemStack(ModItems.YELLOW_MATTER_BUCKET.get());
            case CLEAR -> new ItemStack(ModItems.CLEAR_MATTER_BUCKET.get());
        };
        ItemEntity itemEntity = new ItemEntity(sl,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                bucket);
        itemEntity.setDefaultPickUpDelay();
        sl.addFreshEntity(itemEntity);

        // 3) Particles in radius 3
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        switch (type) {
            case DARK -> {
                sl.sendParticles(ParticleTypes.SCULK_CHARGE_POP, cx, cy, cz, 60,
                        3.0, 1.0, 3.0, 0.05);
                sl.sendParticles(ParticleTypes.SCULK_SOUL, cx, cy, cz, 40,
                        3.0, 1.0, 3.0, 0.05);
            }
            case YELLOW -> {
                sl.sendParticles(ParticleTypes.FLAME, cx, cy, cz, 60,
                        3.0, 1.0, 3.0, 0.05);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 40,
                        3.0, 1.0, 3.0, 0.05);
            }
            case CLEAR -> {
                sl.sendParticles(ParticleTypes.SOUL, cx, cy, cz, 60,
                        3.0, 1.0, 3.0, 0.05);
                sl.sendParticles(ParticleTypes.GLOW, cx, cy, cz, 40,
                        3.0, 1.0, 3.0, 0.05);
            }
        }

        // 4) Sound
        switch (type) {
            case DARK -> sl.playSound(null, pos,
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5F, 0.9F);
            case YELLOW -> sl.playSound(null, pos,
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 1.0F);
            case CLEAR -> sl.playSound(null, pos,
                    SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.PLAYERS, 1.5F, 0.8F);
        }

        // 5) Apply effects in radius 4 (except the dead player)
        AABB area = new AABB(pos).inflate(4.0);
        List<LivingEntity> nearby = sl.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != sp && e.isAlive());

        for (LivingEntity entity : nearby) {
            switch (type) {
                case DARK -> entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
                case YELLOW -> {
                    entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 2));
                    entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 2));
                }
                case CLEAR -> {
                    entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 2));
                    entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
                }
            }
        }
    }

    /** Counts how many pieces of matter armor of the given type are equipped. */
    private static int countMatterArmor(ServerPlayer sp, MatterType type) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = sp.getItemBySlot(slot);
            if (matches(stack.getItem(), type)) count++;
        }
        return count;
    }

    private static boolean matches(Item item, MatterType type) {
        return switch (type) {
            case DARK -> item == ModItems.DARK_MATTER_HELMET.get()
                    || item == ModItems.DARK_MATTER_CHESTPLATE.get()
                    || item == ModItems.DARK_MATTER_LEGGINGS.get()
                    || item == ModItems.DARK_MATTER_BOOTS.get();
            case YELLOW -> item == ModItems.YELLOW_MATTER_HELMET.get()
                    || item == ModItems.YELLOW_MATTER_CHESTPLATE.get()
                    || item == ModItems.YELLOW_MATTER_LEGGINGS.get()
                    || item == ModItems.YELLOW_MATTER_BOOTS.get();
            case CLEAR -> item == ModItems.CLEAR_MATTER_HELMET.get()
                    || item == ModItems.CLEAR_MATTER_CHESTPLATE.get()
                    || item == ModItems.CLEAR_MATTER_LEGGINGS.get()
                    || item == ModItems.CLEAR_MATTER_BOOTS.get();
        };
    }
}
