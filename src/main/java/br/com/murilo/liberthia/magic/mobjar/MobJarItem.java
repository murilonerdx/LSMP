package br.com.murilo.liberthia.magic.mobjar;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

/**
 * v0.1.160 r135: <b>Mob Jar</b> — captura mobs vivos.
 *
 * <p>Inspirado no Ars Nouveau:
 * <ul>
 *   <li>Right-click em mob → captura no jar (NBT salva tipo + dados)</li>
 *   <li>Right-click em bloco com jar cheio → libera o mob</li>
 *   <li>Players e bosses NÃO podem ser capturados</li>
 * </ul>
 */
public class MobJarItem extends Item {

    public static final String NBT_ENTITY_TYPE = "captured_entity";
    public static final String NBT_ENTITY_DATA = "entity_data";
    public static final String NBT_ENTITY_NAME = "entity_name";

    public MobJarItem(Properties props) {
        super(props.stacksTo(1));
    }

    public static boolean hasMob(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(NBT_ENTITY_TYPE);
    }

    public static String getMobName(ItemStack stack) {
        if (!hasMob(stack)) return "";
        return stack.getTag().getString(NBT_ENTITY_NAME);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                    LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (hasMob(stack)) return InteractionResult.FAIL;

        // Players e bosses não podem ser capturados
        if (target instanceof Player) return InteractionResult.FAIL;
        if (target.isInvulnerable()) return InteractionResult.FAIL;
        // Bosses tem max health > 100 — bloqueio
        if (target.getMaxHealth() > 100F) {
            player.displayClientMessage(
                Component.literal("§c⚠ Este ser é poderoso demais pra ser capturado"), true);
            return InteractionResult.FAIL;
        }

        // Salva entity em NBT
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (type == null) return InteractionResult.FAIL;

        CompoundTag entityData = new CompoundTag();
        target.saveWithoutId(entityData);

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(NBT_ENTITY_TYPE, type.toString());
        tag.put(NBT_ENTITY_DATA, entityData);
        tag.putString(NBT_ENTITY_NAME, target.getDisplayName().getString());

        // Remove o mob vivo do mundo
        target.discard();

        player.displayClientMessage(
            Component.literal("§a✓ Capturado: §e" + target.getDisplayName().getString()), true);
        player.swing(hand);

        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        ItemStack stack = ctx.getItemInHand();
        if (!hasMob(stack)) return InteractionResult.FAIL;
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.FAIL;

        // Libera o mob no bloco onde clickou (lado de cima)
        CompoundTag tag = stack.getTag();
        String typeStr = tag.getString(NBT_ENTITY_TYPE);
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(typeStr));
        if (type == null) {
            player.displayClientMessage(
                Component.literal("§c⚠ Entity type inválido: " + typeStr), true);
            return InteractionResult.FAIL;
        }

        Entity entity = type.create(sl);
        if (entity == null) return InteractionResult.FAIL;

        var pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                player.getYRot(), 0F);

        // Restaura NBT do mob (HP, equipamentos, etc.)
        try {
            entity.load(tag.getCompound(NBT_ENTITY_DATA));
        } catch (Exception e) {
            // r137 fix #6: log corrupcao ao inves de fail silenciosa
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.warn(
                "MobJarItem NBT corrupted for type {} — releasing fresh entity: {}",
                typeStr, e.getMessage());
        }
        // r137 fix #6: regenera UUID — se 2 jars do mesmo mob existem (dupe via inventory copy),
        // liberar ambas dava 2 entities com mesma UUID — uma sumia da despawn dedup.
        entity.setUUID(UUID.randomUUID());
        // Garante position fresh
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        sl.addFreshEntity(entity);

        // Limpa o jar
        stack.removeTagKey(NBT_ENTITY_TYPE);
        stack.removeTagKey(NBT_ENTITY_DATA);
        stack.removeTagKey(NBT_ENTITY_NAME);

        player.displayClientMessage(
            Component.literal("§a✓ Liberado: §e" + entity.getDisplayName().getString()), true);
        player.swing(ctx.getHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (hasMob(stack)) {
            return Component.literal("Jarra de Captura ")
                .append(Component.literal("(" + getMobName(stack) + ")")
                    .withStyle(ChatFormatting.AQUA));
        }
        return super.getName(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasMob(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasMob(stack)) {
            tooltip.add(Component.literal("§7Contém: §e" + getMobName(stack)));
            tooltip.add(Component.literal("§7§oRight-click em um bloco para liberar"));
        } else {
            tooltip.add(Component.literal("§7Vazia — right-click em mob pra capturar"));
            tooltip.add(Component.literal("§8Bosses não podem ser capturados"));
        }
    }
}
