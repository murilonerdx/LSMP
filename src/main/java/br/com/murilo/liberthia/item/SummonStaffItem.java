package br.com.murilo.liberthia.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Summon Staff — summons stored entity.
 * Right-click air → opens chat instruction.
 * Right-click player → summon at player.
 * Right-click block (shift) → summon at block.
 * Use /liberthia summonstaff set <entityid> [tag] [name] to configure.
 */
public class SummonStaffItem extends Item {

    public SummonStaffItem(Properties properties) {
        super(properties);
    }

    // Right-click on player → summon at them
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(target instanceof Player victim)) return InteractionResult.PASS;
        return summon(stack, user, victim.getX(), victim.getY(), victim.getZ());
    }

    // Shift+right-click on block → summon there
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player user = ctx.getPlayer();
        if (user == null || !user.isShiftKeyDown()) return InteractionResult.PASS;
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        BlockPos p = ctx.getClickedPos().above();
        return summon(ctx.getItemInHand(), user, p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
    }

    // Right-click air without shift → show instructions
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (user.isShiftKeyDown()) return InteractionResultHolder.pass(stack);

        // Show config
        if (!stack.hasTag() || !stack.getTag().contains("entity")) {
            user.sendSystemMessage(Component.translatable("chat.liberthia.summon_unset").withStyle(ChatFormatting.RED));
            user.sendSystemMessage(Component.literal("§7Use: §e/liberthia summonstaff set <entityid> [tag]"));
            user.sendSystemMessage(Component.literal("§7Ex: §a/liberthia summonstaff set backrooms:duller black_matter"));
        } else {
            CompoundTag tag = stack.getTag();
            String id = tag.getString("entity");
            String t = tag.getString("tag");
            user.sendSystemMessage(Component.literal("§6Summon Staff §7→ §f" + id +
                    (t.isEmpty() ? "" : " §8[" + t + "]")));
            Component clr = Component.literal("§c[Limpar]").withStyle(s -> s.withClickEvent(
                    new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/liberthia summonstaff clear")));
            user.sendSystemMessage(clr);
        }
        return InteractionResultHolder.success(stack);
    }

    private InteractionResult summon(ItemStack stack, Player user, double x, double y, double z) {
        if (!stack.hasTag() || !stack.getTag().contains("entity")) {
            user.sendSystemMessage(Component.translatable("chat.liberthia.summon_unset").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        CompoundTag tag = stack.getTag();
        String entityId = tag.getString("entity");
        String customTag = tag.getString("tag");
        String customName = tag.getString("name");

        try {
            Optional<EntityType<?>> opt = ForgeRegistries.ENTITY_TYPES.getHolder(new ResourceLocation(entityId))
                    .map(h -> h.value());
            if (opt.isEmpty()) {
                user.sendSystemMessage(Component.literal("§cEntidade não encontrada: " + entityId).withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            EntityType<?> type = opt.get();
            if (!(user.level() instanceof ServerLevel sl)) return InteractionResult.FAIL;

            Entity e = type.create(sl);
            if (e == null) return InteractionResult.FAIL;
            e.moveTo(x, y, z, user.getYRot(), 0);

            if (!customTag.isEmpty()) e.addTag(customTag);
            if (!customName.isEmpty()) {
                e.setCustomName(Component.literal(customName));
                e.setCustomNameVisible(true);
            }

            if (e instanceof Mob mob) {
                mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.COMMAND, null, null);
            }
            sl.addFreshEntity(e);

            sl.sendParticles(ParticleTypes.SOUL, x, y + 1, z, 40, 0.5, 1, 0.5, 0.1);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 20, 0.5, 0.3, 0.5, 0.05);
            sl.playSound(null, BlockPos.containing(x, y, z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5F, 0.5F);

            user.sendSystemMessage(Component.translatable("chat.liberthia.summon_ok", entityId)
                    .withStyle(ChatFormatting.GOLD));
            user.getCooldowns().addCooldown(this, 40);
        } catch (Exception ex) {
            user.sendSystemMessage(Component.literal("§cErro: " + ex.getMessage()).withStyle(ChatFormatting.RED));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.liberthia.summon_staff.desc1").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("item.liberthia.summon_staff.desc2").withStyle(ChatFormatting.GRAY));
        if (stack.hasTag() && stack.getTag().contains("entity")) {
            tooltip.add(Component.literal("§8→ §a" + stack.getTag().getString("entity")));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("entity");
    }
}
