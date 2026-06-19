package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.entity.RiftPortalEntity;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.OpenRiftCutterCoordScreenS2CPacket;
import br.com.murilo.liberthia.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * r185 — <b>Adaga Corta-Fendas</b> (3 níveis via {@link RiftTier}). SHIFT+direito abre a GUI
 * de coordenadas (salvas no NBT da adaga). Direito abre um PORTAL VERDE ({@link RiftPortalEntity})
 * à frente — atravessá-lo teleporta para as coords salvas, na dimensão permitida pelo nível.
 * Quem está perto SEM segurar uma adaga recebe Radiação Dimensional ({@code RiftRadiationHandler}).
 */
public class RiftCutterItem extends SwordItem {
    public static final String KEY_X = "RiftTargetX";
    public static final String KEY_Y = "RiftTargetY";
    public static final String KEY_Z = "RiftTargetZ";
    public static final String KEY_DIM = "RiftTargetDim";

    private final RiftTier tier;

    public RiftCutterItem(RiftTier tier, Properties props) {
        super(DarkMatterToolMaterial.INSTANCE, tier.attackDamage, tier.attackSpeed, props);
        this.tier = tier;
    }

    public RiftTier riftTier() { return tier; }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // SHIFT+direito → abre a GUI de coordenadas
        if (player.isShiftKeyDown()) {
            CompoundTag tag = stack.getOrCreateTag();
            String dim = tag.contains(KEY_DIM) ? tag.getString(KEY_DIM) : sp.level().dimension().location().toString();
            ModNetwork.sendToPlayer(sp, new OpenRiftCutterCoordScreenS2CPacket(
                    tag.getInt(KEY_X), tag.getInt(KEY_Y), tag.getInt(KEY_Z), dim));
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        // direito → abre o portal verde
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(KEY_DIM)) {
            sp.displayClientMessage(Component.literal("§cNenhuma coordenada salva. SHIFT+clique para configurar."), true);
            return InteractionResultHolder.fail(stack);
        }
        int x = tag.getInt(KEY_X), y = tag.getInt(KEY_Y), z = tag.getInt(KEY_Z);
        String dimStr = tag.getString(KEY_DIM);
        ResourceLocation dimLoc = ResourceLocation.tryParse(dimStr);
        if (dimLoc == null) {
            sp.displayClientMessage(Component.literal("§cAlvo corrompido. SHIFT+clique para reconfigurar."), true);
            tag.remove(KEY_DIM);
            return InteractionResultHolder.fail(stack);
        }
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
        if (!tier.canTeleportTo(dimKey)) {
            sp.displayClientMessage(Component.literal("§cEste Corta-Fendas não alcança esta dimensão."), true);
            return InteractionResultHolder.fail(stack);
        }
        if (sp.getServer() == null || sp.getServer().getLevel(dimKey) == null) {
            sp.displayClientMessage(Component.literal("§cDimensão indisponível."), true);
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel origin = sp.serverLevel();
        Vec3 look = sp.getLookAngle();
        double px = sp.getX() + look.x * 2.0, py = sp.getY() + 0.2, pz = sp.getZ() + look.z * 2.0;
        RiftPortalEntity portal = ModEntities.RIFT_PORTAL.get().create(origin);
        if (portal == null) {
            sp.displayClientMessage(Component.literal("§cFalha ao abrir o portal."), true);
            return InteractionResultHolder.fail(stack);
        }
        portal.setPos(px, py, pz);
        portal.setTarget(dimStr, x + 0.5, y + 0.5, z + 0.5);
        portal.setYRot(sp.getYRot());
        origin.addFreshEntity(portal);
        origin.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py + 1.0, pz, 30, 0.4, 0.8, 0.4, 0.08);
        origin.playSound(null, sp.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.8F, 0.7F);
        sp.getCooldowns().addCooldown(this, 60);
        stack.hurtAndBreak(5, sp, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        tip.add(Component.literal("§5Adaga dimensional").withStyle(ChatFormatting.ITALIC));
        if (tag != null && tag.contains(KEY_DIM)) {
            tip.add(Component.literal("§7Alvo: §f" + tag.getInt(KEY_X) + ", " + tag.getInt(KEY_Y) + ", " + tag.getInt(KEY_Z)));
            tip.add(Component.literal("§7Dimensão: §f" + tag.getString(KEY_DIM)));
        } else {
            tip.add(Component.literal("§8Sem alvo — SHIFT+direito p/ configurar"));
        }
        String acesso = switch (tier) {
            case OVERWORLD_ONLY -> "Overworld";
            case OVERWORLD_NETHER -> "Overworld + Nether";
            case ALL_DIMENSIONS -> "Todas as dimensões";
        };
        tip.add(Component.literal("§7Acesso: §b" + acesso));
        tip.add(Component.literal("§cEmite Radiação Dimensional a quem está perto sem uma adaga."));
    }
}
