package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.network.KirikoBookNetworking;
import br.com.murilo.liberthia.packet.OpenKirikoBookScreenPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BookRedKirikoItem extends Item {

    private static final String ALLOWED_PLAYER = "Kiriko";

    private static final int INVISIBILITY_DURATION_TICKS = 8 * 20;
    private static final int SPEED_DURATION_TICKS = 8 * 20;
    private static final int TRAVEL_PROTECTION_DURATION_TICKS = 12 * 20;
    private static final int REGENERATION_DURATION_TICKS = 6 * 20;

    public BookRedKirikoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!canUseBook(serverPlayer)) {
            serverPlayer.sendSystemMessage(
                    Component.literal("O Livro Vermelho não responde ao seu nome.")
                            .withStyle(ChatFormatting.DARK_RED)
            );
            return InteractionResultHolder.fail(stack);
        }

        if (serverPlayer.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        applyTravelProtection(serverPlayer);

        MinecraftServer server = serverPlayer.server;

        List<OpenKirikoBookScreenPacket.DimensionEntry> dimensions = new ArrayList<>();

        for (ServerLevel serverLevel : server.getAllLevels()) {
            ResourceKey<Level> key = serverLevel.dimension();
            String dimensionId = key.location().toString();

            OpenKirikoBookScreenPacket.TeleportPos defaultPos = defaultPositionForDimension(dimensionId);

            OpenKirikoBookScreenPacket.TeleportPos safePos = findSafeTeleportPosition(
                    serverLevel,
                    defaultPos.x(),
                    defaultPos.y(),
                    defaultPos.z()
            );

            dimensions.add(new OpenKirikoBookScreenPacket.DimensionEntry(
                    dimensionId,
                    safePos.x(),
                    safePos.y(),
                    safePos.z()
            ));
        }

        List<OpenKirikoBookScreenPacket.PlayerEntry> players = new ArrayList<>();

        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            String name = target.getGameProfile().getName();

            if (target.getUUID().equals(serverPlayer.getUUID())) {
                continue;
            }

            if (startsWithLNPC(name)) {
                continue;
            }

            players.add(new OpenKirikoBookScreenPacket.PlayerEntry(name));
        }

        KirikoBookNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new OpenKirikoBookScreenPacket(dimensions, players)
        );

        serverPlayer.getCooldowns().addCooldown(this, 40);

        return InteractionResultHolder.success(stack);
    }

    public static void applyTravelProtection(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.INVISIBILITY,
                INVISIBILITY_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                SPEED_DURATION_TICKS,
                2,
                false,
                false,
                true
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                TRAVEL_PROTECTION_DURATION_TICKS,
                4,
                false,
                false,
                true
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE,
                TRAVEL_PROTECTION_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.SLOW_FALLING,
                TRAVEL_PROTECTION_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                REGENERATION_DURATION_TICKS,
                2,
                false,
                false,
                true
        ));

        player.fallDistance = 0.0F;
    }

    private static boolean canUseBook(ServerPlayer player) {
        return isKiriko(player) || isOp(player);
    }

    private static boolean isKiriko(ServerPlayer player) {
        return player.getGameProfile().getName().equalsIgnoreCase(ALLOWED_PLAYER);
    }

    private static boolean isOp(ServerPlayer player) {
        return player.server.getPlayerList().isOp(player.getGameProfile());
    }

    private static boolean startsWithLNPC(String name) {
        return name != null && name.toUpperCase().startsWith("LNPC");
    }

    private static OpenKirikoBookScreenPacket.TeleportPos defaultPositionForDimension(String dimensionId) {
        return switch (dimensionId) {
            case "minecraft:overworld" -> new OpenKirikoBookScreenPacket.TeleportPos(0, 100, 0);
            case "minecraft:the_nether" -> new OpenKirikoBookScreenPacket.TeleportPos(0, 80, 0);
            case "minecraft:the_end" -> new OpenKirikoBookScreenPacket.TeleportPos(0, 80, 0);

            case "twilightforest:twilight_forest" -> new OpenKirikoBookScreenPacket.TeleportPos(0, 100, 0);
            case "blue_skies:everbright" -> new OpenKirikoBookScreenPacket.TeleportPos(0, 120, 0);
            case "blue_skies:everdawn" -> new OpenKirikoBookScreenPacket.TeleportPos(0, 120, 0);

            default -> new OpenKirikoBookScreenPacket.TeleportPos(0, 100, 0);
        };
    }

    public static OpenKirikoBookScreenPacket.TeleportPos findSafeTeleportPosition(
            ServerLevel level,
            double wantedX,
            double wantedY,
            double wantedZ
    ) {
        int baseX = (int) Math.floor(wantedX);
        int baseY = clampY(level, (int) Math.floor(wantedY));
        int baseZ = (int) Math.floor(wantedZ);

        BlockPos base = new BlockPos(baseX, baseY, baseZ);

        BlockPos exact = findSafeAround(level, base, 4, 16, 16);
        if (exact != null) {
            return toTeleportPos(exact);
        }

        BlockPos wider = findSafeAround(level, base, 16, 32, 32);
        if (wider != null) {
            return toTeleportPos(wider);
        }

        BlockPos spawn = level.getSharedSpawnPos();

        BlockPos safeSpawn = findSafeAround(level, spawn, 16, 32, 32);
        if (safeSpawn != null) {
            return toTeleportPos(safeSpawn);
        }

        BlockPos emergency = new BlockPos(
                spawn.getX(),
                clampY(level, spawn.getY() + 8),
                spawn.getZ()
        );

        return toTeleportPos(emergency);
    }

    private static BlockPos findSafeAround(
            ServerLevel level,
            BlockPos center,
            int horizontalRadius,
            int scanDown,
            int scanUp
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int radius = 0; radius <= horizontalRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {

                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    for (int dy = scanUp; dy >= -scanDown; dy--) {
                        int x = center.getX() + dx;
                        int y = clampY(level, center.getY() + dy);
                        int z = center.getZ() + dz;

                        mutable.set(x, y, z);

                        if (isSafeTeleportSpot(level, mutable)) {
                            return mutable.immutable();
                        }
                    }
                }
            }
        }

        return null;
    }

    private static boolean isSafeTeleportSpot(ServerLevel level, BlockPos feetPos) {
        BlockPos headPos = feetPos.above();
        BlockPos groundPos = feetPos.below();

        if (!level.getWorldBorder().isWithinBounds(feetPos)) {
            return false;
        }

        if (feetPos.getY() <= level.getMinBuildHeight() + 1) {
            return false;
        }

        if (headPos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }

        BlockState feetState = level.getBlockState(feetPos);
        BlockState headState = level.getBlockState(headPos);
        BlockState groundState = level.getBlockState(groundPos);

        FluidState feetFluid = level.getFluidState(feetPos);
        FluidState headFluid = level.getFluidState(headPos);
        FluidState groundFluid = level.getFluidState(groundPos);

        boolean feetFree = feetState.getCollisionShape(level, feetPos).isEmpty();
        boolean headFree = headState.getCollisionShape(level, headPos).isEmpty();

        if (!feetFree || !headFree) {
            return false;
        }

        if (!feetFluid.isEmpty() || !headFluid.isEmpty()) {
            return false;
        }

        boolean solidGround = groundState.isFaceSturdy(level, groundPos, Direction.UP);

        if (!solidGround) {
            return false;
        }

        if (groundFluid.is(FluidTags.LAVA)) {
            return false;
        }

        if (groundState.is(Blocks.LAVA)
                || groundState.is(Blocks.MAGMA_BLOCK)
                || groundState.is(Blocks.CACTUS)
                || groundState.is(Blocks.CAMPFIRE)
                || groundState.is(Blocks.SOUL_CAMPFIRE)
                || groundState.is(Blocks.FIRE)
                || groundState.is(Blocks.SOUL_FIRE)
                || groundState.is(Blocks.POWDER_SNOW)) {
            return false;
        }

        return true;
    }

    private static int clampY(ServerLevel level, int y) {
        int min = level.getMinBuildHeight() + 2;
        int max = level.getMaxBuildHeight() - 3;

        if (y < min) {
            return min;
        }

        return Math.min(y, max);
    }

    private static OpenKirikoBookScreenPacket.TeleportPos toTeleportPos(BlockPos pos) {
        return new OpenKirikoBookScreenPacket.TeleportPos(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5
        );
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Livro Vermelho de Kiriko").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("Abre uma interface de travessia entre mundos.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Permite escolher dimensões, players e coordenadas.").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("Protege contra lava, queda, sufocamento e dano após a travessia.").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Apenas Kiriko e operadores conseguem usar.").withStyle(ChatFormatting.DARK_PURPLE));
    }
}