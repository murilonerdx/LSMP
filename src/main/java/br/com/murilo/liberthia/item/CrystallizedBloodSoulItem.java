package br.com.murilo.liberthia.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Soul Shard — Occultism-inspired pet respawn item.
 *
 * <p>When a tamed Blood-kin pet dies, it drops one of these with the full
 * entity NBT serialised inside. Right-click on a block with the shard to
 * spawn an identical pet at that position (same name, same variants, same
 * owner, full HP).
 *
 * <p>Generic enough to work for any LivingEntity type — currently used by
 * BloodHound but reusable for future tamed kin.
 */
public class CrystallizedBloodSoulItem extends Item {
    private static final String NBT_ENTITY_ID = "EntityId";
    private static final String NBT_ENTITY_DATA = "EntityData";
    private static final String NBT_ENTITY_NAME = "EntityName";

    public CrystallizedBloodSoulItem(Properties props) {
        super(props.stacksTo(1));
    }

    public static void writeEntity(ItemStack stack, EntityType<?> type, CompoundTag entityNbt) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(NBT_ENTITY_ID, BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        tag.put(NBT_ENTITY_DATA, entityNbt);
        Component label = type.getDescription();
        if (label != null) tag.putString(NBT_ENTITY_NAME, label.getString());
    }

    public static Optional<EntityType<?>> readType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_ENTITY_ID)) return Optional.empty();
        ResourceLocation id = new ResourceLocation(tag.getString(NBT_ENTITY_ID));
        return Optional.ofNullable(BuiltInRegistries.ENTITY_TYPE.get(id));
    }

    public static Optional<CompoundTag> readEntityData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_ENTITY_DATA)) return Optional.empty();
        return Optional.of(tag.getCompound(NBT_ENTITY_DATA));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;

        ItemStack stack = ctx.getItemInHand();
        Optional<EntityType<?>> typeOpt = readType(stack);
        Optional<CompoundTag> nbtOpt = readEntityData(stack);
        if (typeOpt.isEmpty() || nbtOpt.isEmpty()) {
            if (ctx.getPlayer() != null) {
                ctx.getPlayer().displayClientMessage(
                        Component.literal("§cAlma corrompida — sem dados de entidade."), true);
            }
            return InteractionResult.FAIL;
        }

        BlockPos pos = ctx.getClickedPos().above();
        Entity ent = typeOpt.get().create(sl);
        if (ent == null) return InteractionResult.FAIL;
        ent.load(nbtOpt.get());
        ent.moveTo(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5,
                level.getRandom().nextFloat() * 360F, 0F);
        if (ent instanceof Mob mob) {
            mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos),
                    net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null, null);
        }
        if (ent instanceof LivingEntity le) {
            le.setHealth(le.getMaxHealth());
        }
        sl.addFreshEntity(ent);

        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                30, 0.4, 0.5, 0.4, 0.04);
        sl.playSound(null, pos, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0F, 0.6F);

        if (ctx.getPlayer() != null && !ctx.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tip, flag);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_ENTITY_NAME)) {
            tip.add(Component.literal("§7Contém: §c" + tag.getString(NBT_ENTITY_NAME)));
        } else {
            tip.add(Component.literal("§7Vazio — ainda não capturou ninguém."));
        }
        tip.add(Component.literal("§7Right-click num bloco para reviver o pet."));
    }
}
