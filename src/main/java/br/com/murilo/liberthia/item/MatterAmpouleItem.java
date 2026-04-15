package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.logic.AmpouleEffectManager;
import br.com.murilo.liberthia.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ampola de Matéria — 3 tipos (Escura/Clara/Amarela).
 * Shift+clique: cicla tipo.
 * Clique em entidade: injeta efeitos.
 * Clique no ar olhando pra entidade: raycast.
 * Sem alvo: injeta em si mesmo.
 */
public class MatterAmpouleItem extends Item {
    private static final String TAG_TYPE = "matter_type";
    private static final String[] TYPE_NAMES = {"Escura", "Clara", "Amarela"};
    private static final ChatFormatting[] TYPE_COLORS = {ChatFormatting.DARK_PURPLE, ChatFormatting.AQUA, ChatFormatting.GOLD};

    public MatterAmpouleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(player instanceof ServerPlayer admin)) {
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        if (player.isShiftKeyDown()) {
            CompoundTag tag = stack.getOrCreateTag();
            int type = (tag.getInt(TAG_TYPE) + 1) % 3;
            tag.putInt(TAG_TYPE, type);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        // Raycast: achar entidade no cone de visão (15 blocos, ~30 graus)
        Vec3 eye = admin.getEyePosition();
        Vec3 look = admin.getLookAngle();
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity le : admin.serverLevel().getEntitiesOfClass(LivingEntity.class,
                admin.getBoundingBox().inflate(15), e -> e != admin && e.isAlive())) {
            Vec3 toTarget = le.position().add(0, le.getBbHeight() * 0.5, 0).subtract(eye);
            double dist = toTarget.length();
            if (dist > 15) continue;
            double dot = toTarget.normalize().dot(look.normalize());
            if (dot > 0.85 && dist < closestDist) {
                closestDist = dist;
                closest = le;
            }
        }

        if (closest != null) {
            inject(admin, stack, closest);
        } else {
            // Sem alvo: injeta em si mesmo
            inject(admin, stack, admin);
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer admin)) return InteractionResult.PASS;
        inject(admin, stack, target);
        return InteractionResult.SUCCESS;
    }

    private void inject(ServerPlayer admin, ItemStack stack, LivingEntity target) {
        int type = stack.getOrCreateTag().getInt(TAG_TYPE);
        switch (type) {
            case 0 -> injectDark(target);
            case 1 -> injectClear(target);
            case 2 -> injectYellow(target);
        }
        target.level().playSound(null, target.blockPosition(),
                ModSounds.DARK_PULSE.get(), SoundSource.PLAYERS, 0.5F, 1.5F);
        if (!admin.isCreative()) {
            stack.shrink(1);
        }
    }

    private void injectDark(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 1, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 300, 0, false, false, true));
    }

    private void injectClear(LivingEntity target) {
        if (target instanceof ServerPlayer sp) {
            AmpouleEffectManager.applyClear(sp, 20 * 20);
        } else {
            // Em mobs: levitation + slowness
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 1, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, 2, false, true, true));
        }
    }

    private void injectYellow(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0, false, true, true));
        if (target instanceof ServerPlayer sp) {
            AmpouleEffectManager.applyYellow(sp, 20 * 20);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int type = stack.getOrCreateTag().getInt(TAG_TYPE);
        tooltip.add(Component.literal("Ampola: " + TYPE_NAMES[type]).withStyle(TYPE_COLORS[type]));
        tooltip.add(Component.literal("Shift: trocar tipo").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Clique: injetar (sem alvo = em si)").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public Component getName(ItemStack stack) {
        int type = stack.getOrCreateTag().getInt(TAG_TYPE);
        return Component.literal("Ampola: " + TYPE_NAMES[type]).withStyle(TYPE_COLORS[type]);
    }
}
