package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.capture.CapturedPlayerManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SealingSealItem extends Item {

    private static final String TAG_HAS_CAPTURE = "HasCapture";
    private static final String TAG_CAPTURED_TYPE = "CapturedType";
    private static final String TAG_CAPTURED_ENTITY = "CapturedEntity";
    private static final String TAG_CAPTURED_NAME = "CapturedName";
    private static final String TAG_CAPTURED_PLAYER = "CapturedPlayer";
    private static final String TAG_CAPTURED_CATEGORY = "CapturedCategory";

    private static final int LEVEL_ANIMAL = 0;
    private static final int LEVEL_FRIENDLY = 1;
    private static final int LEVEL_MONSTER = 2;
    private static final int LEVEL_PLAYER = 3;

    private final SealTier tier;

    public SealingSealItem(SealTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public SealTier getTier() {
        return tier;
    }

    /**
     * Shift + botão direito no ar.
     * Libera o que estiver capturado.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        boolean released = releaseCaptured(stack, serverPlayer);

        return released
                ? InteractionResultHolder.consume(stack)
                : InteractionResultHolder.pass(stack);
    }

    /**
     * Shift + botão direito em bloco.
     * Libera o que estiver capturado.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        boolean released = releaseCaptured(stack, serverPlayer);

        return released ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    /**
     * Clique direto na entidade.
     * Também existe evento global para reforçar isso.
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        return tryCaptureWithSeal(stack, serverPlayer, target);
    }

    public InteractionResult tryCaptureWithSealPlayer(ItemStack stack, ServerPlayer player, LivingEntity target) {
        if (hasCapturedSomething(stack)) {
            return InteractionResult.FAIL;
        }

        CaptureCategory category = getCaptureCategory(target);

        if (!tier.canCaptureRequiredLevel(category.requiredLevel())) {
            player.displayClientMessage(
                    Component.literal("Este selo não é forte o suficiente. Necessário: ")
                            .append(Component.literal(category.displayName()).withStyle(ChatFormatting.YELLOW)),
                    false
            );
            return InteractionResult.FAIL;
        }

        if (target instanceof ServerPlayer targetPlayer) {
            return capturePlayer(stack, player, targetPlayer, category);
        }

        return captureEntity(stack, player, target, category);
    }

    public InteractionResult tryCaptureWithSeal(ItemStack stack, ServerPlayer player, LivingEntity target) {
        if (hasCapturedSomething(stack)) {
            player.displayClientMessage(
                    Component.literal("Este selo já contém uma entidade.")
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return InteractionResult.FAIL;
        }

        CaptureCategory category = getCaptureCategory(target);

        if (!tier.canCaptureRequiredLevel(category.requiredLevel())) {
            player.displayClientMessage(
                    Component.literal("Este selo não é forte o suficiente. Necessário: ")
                            .append(Component.literal(category.displayName()).withStyle(ChatFormatting.YELLOW)),
                    false
            );
            return InteractionResult.FAIL;
        }

        if (target instanceof ServerPlayer targetPlayer) {
            return capturePlayer(stack, player, targetPlayer, category);
        }

        return captureEntity(stack, player, target, category);
    }

    private InteractionResult captureEntity(
            ItemStack stack,
            ServerPlayer player,
            LivingEntity target,
            CaptureCategory category
    ) {
        CompoundTag entityTag = new CompoundTag();

        if (!target.save(entityTag)) {
            player.displayClientMessage(
                    Component.literal("Falha ao salvar a entidade no selo.")
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return InteractionResult.FAIL;
        }

        ResourceLocation typeId = EntityType.getKey(target.getType());

        CompoundTag sealTag = stack.getOrCreateTag();
        sealTag.putBoolean(TAG_HAS_CAPTURE, true);
        sealTag.putString(TAG_CAPTURED_TYPE, typeId.toString());
        sealTag.put(TAG_CAPTURED_ENTITY, entityTag);
        sealTag.putString(TAG_CAPTURED_CATEGORY, category.name());

        if (target.hasCustomName()) {
            sealTag.putString(TAG_CAPTURED_NAME, target.getCustomName().getString());
        } else {
            sealTag.putString(TAG_CAPTURED_NAME, target.getDisplayName().getString());
        }

        target.discard();

        player.displayClientMessage(
                Component.literal("Entidade capturada: ")
                        .append(Component.literal(typeId.toString()).withStyle(tier.color()))
                        .append(Component.literal(" [" + category.displayName() + "]").withStyle(ChatFormatting.GRAY)),
                false
        );

        return InteractionResult.CONSUME;
    }

    private InteractionResult capturePlayer(
            ItemStack stack,
            ServerPlayer captor,
            ServerPlayer target,
            CaptureCategory category
    ) {
        if (target.getUUID().equals(captor.getUUID())) {
            captor.displayClientMessage(
                    Component.literal("Você não pode capturar a si mesmo.")
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return InteractionResult.FAIL;
        }

        CompoundTag tag = stack.getOrCreateTag();

        tag.putBoolean(TAG_HAS_CAPTURE, true);
        tag.putUUID(TAG_CAPTURED_PLAYER, target.getUUID());
        tag.putString(TAG_CAPTURED_NAME, target.getGameProfile().getName());
        tag.putString(TAG_CAPTURED_CATEGORY, category.name());

        CapturedPlayerManager.capture(target, captor);

        captor.displayClientMessage(
                Component.literal("Player capturado: ")
                        .append(Component.literal(target.getGameProfile().getName()).withStyle(ChatFormatting.DARK_PURPLE)),
                false
        );

        target.displayClientMessage(
                Component.literal("Você foi aprisionado em um selo.")
                        .withStyle(ChatFormatting.RED),
                false
        );

        return InteractionResult.CONSUME;
    }

    private boolean releaseCaptured(ItemStack stack, ServerPlayer holder) {
        if (!hasCapturedSomething(stack)) {
            holder.displayClientMessage(
                    Component.literal("Este selo está vazio.")
                            .withStyle(ChatFormatting.GRAY),
                    true
            );
            return false;
        }

        CompoundTag tag = stack.getTag();

        if (tag == null) {
            return false;
        }

        if (tag.hasUUID(TAG_CAPTURED_PLAYER)) {
            return releaseCapturedPlayer(stack, holder, tag);
        }

        if (tag.contains(TAG_CAPTURED_ENTITY)) {
            return releaseCapturedEntity(stack, holder, tag);
        }

        clearCapture(stack);

        holder.displayClientMessage(
                Component.literal("O selo estava corrompido e foi limpo.")
                        .withStyle(ChatFormatting.YELLOW),
                false
        );

        return true;
    }

    private boolean releaseCapturedPlayer(ItemStack stack, ServerPlayer holder, CompoundTag tag) {
        UUID playerId = tag.getUUID(TAG_CAPTURED_PLAYER);
        ServerPlayer captured = holder.server.getPlayerList().getPlayer(playerId);

        if (captured == null) {
            holder.displayClientMessage(
                    Component.literal("O player capturado não está online.")
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return false;
        }

        CapturedPlayerManager.release(captured);
        teleportReleasedPlayerNearHolder(holder, captured);

        clearCapture(stack);

        holder.displayClientMessage(
                Component.literal("Player liberado do selo.")
                        .withStyle(ChatFormatting.GREEN),
                false
        );

        captured.displayClientMessage(
                Component.literal("Você foi liberado do selo.")
                        .withStyle(ChatFormatting.GREEN),
                false
        );

        return true;
    }

    private boolean releaseCapturedEntity(ItemStack stack, ServerPlayer holder, CompoundTag tag) {
        if (!(holder.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        CompoundTag entityTag = tag.getCompound(TAG_CAPTURED_ENTITY);

        Optional<Entity> optionalEntity = EntityType.create(entityTag, serverLevel);

        if (optionalEntity.isEmpty()) {
            holder.displayClientMessage(
                    Component.literal("Falha ao recriar a entidade capturada.")
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return false;
        }

        Entity entity = optionalEntity.get();

        BlockPos spawnPos = getReleasePos(holder);

        entity.moveTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                holder.getYRot(),
                0.0F
        );

        serverLevel.addFreshEntity(entity);

        clearCapture(stack);

        holder.displayClientMessage(
                Component.literal("Entidade liberada do selo.")
                        .withStyle(ChatFormatting.GREEN),
                false
        );

        return true;
    }

    private BlockPos getReleasePos(ServerPlayer holder) {
        ServerLevel level = holder.serverLevel();

        BlockHitResult hit = getPlayerPOVHitResult(level, holder, ClipContext.Fluid.NONE);

        BlockPos basePos;

        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            basePos = hit.getBlockPos().relative(hit.getDirection());
        } else {
            basePos = holder.blockPosition().relative(holder.getDirection(), 2);
        }

        if (!level.getBlockState(basePos).isAir()) {
            basePos = holder.blockPosition().relative(holder.getDirection(), 2);
        }

        return basePos;
    }

    private void teleportReleasedPlayerNearHolder(ServerPlayer holder, ServerPlayer captured) {
        BlockPos pos = holder.blockPosition().relative(holder.getDirection(), 2);

        captured.teleportTo(
                holder.serverLevel(),
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                holder.getYRot(),
                holder.getXRot()
        );
    }

    private void clearCapture(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag == null) {
            return;
        }

        tag.remove(TAG_HAS_CAPTURE);
        tag.remove(TAG_CAPTURED_TYPE);
        tag.remove(TAG_CAPTURED_ENTITY);
        tag.remove(TAG_CAPTURED_NAME);
        tag.remove(TAG_CAPTURED_PLAYER);
        tag.remove(TAG_CAPTURED_CATEGORY);

        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    private boolean hasCapturedSomething(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        return tag != null && tag.getBoolean(TAG_HAS_CAPTURE);
    }

    private CaptureCategory getCaptureCategory(LivingEntity target) {
        if (target instanceof Player) {
            return CaptureCategory.PLAYER;
        }

        if (isMonsterOrHostile(target)) {
            return CaptureCategory.MONSTER;
        }

        if (isAnimal(target)) {
            return CaptureCategory.ANIMAL;
        }

        if (isFriendlyEntity(target)) {
            return CaptureCategory.FRIENDLY;
        }

        /*
         * Entidade viva desconhecida.
         * Por segurança, tratamos como monstro.
         * Assim só diamante/netherite captura mobs estranhos de mod.
         */
        return CaptureCategory.MONSTER;
    }

    private boolean isAnimal(LivingEntity target) {
        return target instanceof Animal
                || target.getType().getCategory() == MobCategory.CREATURE
                || target.getType().getCategory() == MobCategory.WATER_CREATURE
                || target.getType().getCategory() == MobCategory.AMBIENT;
    }

    private boolean isMonsterOrHostile(LivingEntity target) {
        return target instanceof Monster
                || target.getType().getCategory() == MobCategory.MONSTER;
    }

    private boolean isFriendlyEntity(LivingEntity target) {
        return target instanceof Villager
                || target instanceof AbstractVillager
                || target instanceof TamableAnimal;
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag != null && tag.getBoolean(TAG_HAS_CAPTURE)) {
            String name = tag.getString(TAG_CAPTURED_NAME);

            if (!name.isBlank()) {
                return tier.displayName()
                        .copy()
                        .append(Component.literal(" [" + name + "]").withStyle(ChatFormatting.GRAY));
            }
        }

        return tier.displayName();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.literal("Hierarquia de captura:")
                .withStyle(ChatFormatting.DARK_GRAY));

        switch (tier) {
            case BONE -> tooltip.add(Component.literal("Captura animais.")
                    .withStyle(ChatFormatting.GRAY));
            case GOLD -> tooltip.add(Component.literal("Captura animais e entidades amigáveis.")
                    .withStyle(ChatFormatting.GOLD));
            case DIAMOND -> tooltip.add(Component.literal("Captura animais, amigáveis e monstros.")
                    .withStyle(ChatFormatting.AQUA));
            case NETHERITE -> tooltip.add(Component.literal("Captura tudo abaixo e também players.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }

        if (hasCapturedSomething(stack)) {
            CompoundTag tag = stack.getTag();
            String name = tag != null ? tag.getString(TAG_CAPTURED_NAME) : "";

            tooltip.add(Component.literal("Contém: " + (name.isBlank() ? "Entidade desconhecida" : name))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

            tooltip.add(Component.literal("Shift + botão direito para liberar.")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.literal("Vazio.")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private enum CaptureCategory {
        ANIMAL(LEVEL_ANIMAL, "Animal"),
        FRIENDLY(LEVEL_FRIENDLY, "Amigável"),
        MONSTER(LEVEL_MONSTER, "Monstro"),
        PLAYER(LEVEL_PLAYER, "Player");

        private final int requiredLevel;
        private final String displayName;

        CaptureCategory(int requiredLevel, String displayName) {
            this.requiredLevel = requiredLevel;
            this.displayName = displayName;
        }

        public int requiredLevel() {
            return requiredLevel;
        }

        public String displayName() {
            return displayName;
        }
    }
}