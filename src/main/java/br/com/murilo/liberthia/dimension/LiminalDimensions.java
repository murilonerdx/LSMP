package br.com.murilo.liberthia.dimension;

import br.com.murilo.liberthia.LiberthiaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * v0.1.22 r57: <b>LIMINAL DIMENSIONS</b> — 3 dimensões cuja proposta NÃO é
 * combate, é dread, confusão, e desconexão da realidade.
 *
 * <h2>As Três</h2>
 * <ol>
 *   <li><b>The Upside Sea</b> ({@code liberthia:upside_sea}) — mundo invertido
 *       acima do céu. Oceano flutuante. Gravidade quebrada.</li>
 *   <li><b>The Folded City</b> ({@code liberthia:folded_city}) — cidade
 *       dobrada em si mesma. Arquitetura impossível. Pedestres sem rosto.</li>
 *   <li><b>The Wooden Place</b> ({@code liberthia:wooden_place}) — floresta
 *       viva. Criaturas que só se movem quando você não olha.</li>
 * </ol>
 *
 * <h2>NBT keys</h2>
 * <ul>
 *   <li>{@code liberthia.liminal_origin_dim/x/y/z}: coordenadas e dim de origem
 *       pra return. Setadas no enter.</li>
 *   <li>{@code liberthia.liminal_entered_tick}: tick em que entrou (grace).</li>
 * </ul>
 *
 * <p>NÃO há "exit portal" — só formas misteriosas de sair que o player descobre.
 */
public final class LiminalDimensions {

    public static final ResourceKey<Level> UPSIDE_SEA = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(LiberthiaMod.MODID, "upside_sea"));

    public static final ResourceKey<Level> FOLDED_CITY = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(LiberthiaMod.MODID, "folded_city"));

    public static final ResourceKey<Level> WOODEN_PLACE = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(LiberthiaMod.MODID, "wooden_place"));

    // NBT keys
    public static final String NBT_ORIGIN_DIM = "liberthia.liminal_origin_dim";
    public static final String NBT_ORIGIN_X = "liberthia.liminal_origin_x";
    public static final String NBT_ORIGIN_Y = "liberthia.liminal_origin_y";
    public static final String NBT_ORIGIN_Z = "liberthia.liminal_origin_z";
    public static final String NBT_ENTERED_TICK = "liberthia.liminal_entered_tick";
    public static final String NBT_CURRENT_DIM_TAG = "liberthia.liminal_current_dim";

    private LiminalDimensions() {}

    /** Checa se o player está em qualquer das 3 dimensões liminais. */
    public static boolean isInLiminal(Player p) {
        ResourceKey<Level> d = p.level().dimension();
        return d.equals(UPSIDE_SEA) || d.equals(FOLDED_CITY) || d.equals(WOODEN_PLACE);
    }

    public static boolean isUpsideSea(Player p) { return p.level().dimension().equals(UPSIDE_SEA); }
    public static boolean isFoldedCity(Player p) { return p.level().dimension().equals(FOLDED_CITY); }
    public static boolean isWoodenPlace(Player p) { return p.level().dimension().equals(WOODEN_PLACE); }

    /**
     * Teleporta player para uma das 3 dimensões liminais, salvando coords de
     * retorno no NBT. NÃO marca o player de forma óbvia — ele descobre.
     */
    public static boolean enterLiminal(ServerPlayer sp, ResourceKey<Level> destKey) {
        if (sp.level().dimension().equals(destKey)) return false;
        MinecraftServer server = sp.server;
        ServerLevel dest = server.getLevel(destKey);
        if (dest == null) {
            LiberthiaMod.LOGGER.warn("[Liminal] dimension {} not found", destKey.location());
            return false;
        }

        // Save return coords/dim
        var data = sp.getPersistentData();
        data.putString(NBT_ORIGIN_DIM, sp.level().dimension().location().toString());
        data.putDouble(NBT_ORIGIN_X, sp.getX());
        data.putDouble(NBT_ORIGIN_Y, sp.getY());
        data.putDouble(NBT_ORIGIN_Z, sp.getZ());
        data.putLong(NBT_ENTERED_TICK, sp.level().getGameTime());
        data.putString(NBT_CURRENT_DIM_TAG, destKey.location().toString());

        // Force-load origin chunk pra return funcionar
        if (sp.level() instanceof ServerLevel orig) {
            var chunkPos = new net.minecraft.world.level.ChunkPos(
                    (int) sp.getX() >> 4, (int) sp.getZ() >> 4);
            orig.setChunkForced(chunkPos.x, chunkPos.z, true);
        }

        // Teleport — escolhe Y baseado na dimensão
        double targetY;
        if (destKey.equals(UPSIDE_SEA)) targetY = 200.0;       // alto pra parecer que está no céu
        else if (destKey.equals(FOLDED_CITY)) targetY = 80.0;  // chão da cidade
        else targetY = 90.0;                                    // floresta surface

        sp.teleportTo(dest, sp.getX(), targetY, sp.getZ(),
                EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                sp.getYRot(), sp.getXRot());
        return true;
    }

    /**
     * Retorna o player pra dim de origem se ele está numa liminal.
     */
    public static boolean returnToOrigin(ServerPlayer sp) {
        if (!isInLiminal(sp)) return false;
        var data = sp.getPersistentData();
        if (!data.contains(NBT_ORIGIN_DIM)) {
            // Fallback overworld spawn
            ServerLevel ow = sp.server.overworld();
            var spawn = ow.getSharedSpawnPos();
            sp.teleportTo(ow, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                    sp.getYRot(), sp.getXRot());
            return true;
        }
        String dimStr = data.getString(NBT_ORIGIN_DIM);
        ResourceLocation rl = ResourceLocation.tryParse(dimStr);
        ServerLevel target = null;
        if (rl != null) {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, rl);
            target = sp.server.getLevel(dimKey);
        }
        if (target == null) target = sp.server.overworld();
        double x = data.getDouble(NBT_ORIGIN_X);
        double y = data.getDouble(NBT_ORIGIN_Y);
        double z = data.getDouble(NBT_ORIGIN_Z);

        // Unforce origin chunk
        try {
            var chunkPos = new net.minecraft.world.level.ChunkPos((int)x >> 4, (int)z >> 4);
            target.setChunkForced(chunkPos.x, chunkPos.z, false);
        } catch (Throwable ignored) {}

        sp.teleportTo(target, x, y, z,
                EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                sp.getYRot(), sp.getXRot());

        data.remove(NBT_ORIGIN_DIM);
        data.remove(NBT_ORIGIN_X);
        data.remove(NBT_ORIGIN_Y);
        data.remove(NBT_ORIGIN_Z);
        data.remove(NBT_ENTERED_TICK);
        data.remove(NBT_CURRENT_DIM_TAG);
        return true;
    }
}
