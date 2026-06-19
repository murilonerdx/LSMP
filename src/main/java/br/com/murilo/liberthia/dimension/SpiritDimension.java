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
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * v0.1.22 r24: utility class do mundo espiritual — chaves, NBT keys, helpers
 * de transição (overworld ↔ spirit).
 *
 * <h2>NBT keys no player.persistentData</h2>
 * <ul>
 *   <li>{@code liberthia.spirit_body} (UUID): UUID do {@link br.com.murilo.liberthia.entity.SoulBodyEntity}
 *       que ficou parado no overworld. Usado pra encontrar e despawnar
 *       quando o player volta.</li>
 *   <li>{@code liberthia.spirit_return_dim} (string): dimensão de origem
 *       (geralmente "minecraft:overworld"). Usado pra teleportar de volta.</li>
 *   <li>{@code liberthia.spirit_return_x|y|z} (double): coordenadas onde o
 *       body ficou. Player volta exatamente nessa posição.</li>
 *   <li>{@code liberthia.sanity} (int 0-100): sanidade atual. Decresce em
 *       spirit, recupera em overworld.</li>
 * </ul>
 */
public final class SpiritDimension {

    /** Chave da dimensão espiritual. */
    public static final ResourceKey<Level> SPIRIT_WORLD = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation(LiberthiaMod.MODID, "spirit_world"));

    // NBT keys
    public static final String NBT_BODY_UUID = "liberthia.spirit_body";
    public static final String NBT_RETURN_DIM = "liberthia.spirit_return_dim";
    public static final String NBT_RETURN_X = "liberthia.spirit_return_x";
    public static final String NBT_RETURN_Y = "liberthia.spirit_return_y";
    public static final String NBT_RETURN_Z = "liberthia.spirit_return_z";
    public static final String NBT_SANITY = "liberthia.sanity";
    /** r32: timestamp do enterSpirit pra grace period antes de body-link checks. */
    public static final String NBT_ENTER_TICK = "liberthia.spirit_enter_tick";
    /**
     * r38: flag setada pelo Tomo Proibido quando expira o efeito de 4 min.
     * Bloqueia {@link #returnToBody} normal — só sai via Prayer Book
     * ({@link #returnViaPrayer}).
     */
    public static final String NBT_TOME_CURSED = "liberthia.tome_cursed";
    /**
     * r180b (report #73): deadline (overworld game-time) até quando o player é
     * IMUNE a ser re-capturado por uma {@code DimensionalRiftEntity}. Evita o
     * ping-pong de voltar, cair EM CIMA do rift e ser sugado de novo na hora.
     */
    public static final String NBT_RIFT_IMMUNE = "liberthia.rift_immune_until";

    /** Sanidade inicial / máxima de qualquer player. */
    public static final int MAX_SANITY = 100;
    /** Threshold abaixo do qual alucinações começam. */
    public static final int LOW_SANITY_THRESHOLD = 30;
    /** Threshold crítico — sofrer dano + slowness. */
    public static final int CRITICAL_SANITY_THRESHOLD = 10;
    /** Ticks pra perder 1 ponto de sanidade no spirit world. */
    public static final int SANITY_DRAIN_INTERVAL = 100; // 5s
    /** Ticks pra regen 1 ponto na overworld. */
    public static final int SANITY_REGEN_INTERVAL = 200; // 10s

    private SpiritDimension() {}

    /** True se o player está atualmente na dimensão espiritual. */
    public static boolean isInSpiritWorld(Player player) {
        return player.level().dimension().equals(SPIRIT_WORLD);
    }

    /** Lê sanidade atual do player (default MAX se nunca foi tocada). */
    public static int getSanity(Player player) {
        var data = player.getPersistentData();
        if (!data.contains(NBT_SANITY)) return MAX_SANITY;
        return data.getInt(NBT_SANITY);
    }

    /** Seta sanidade clampada a [0, MAX_SANITY]. */
    public static void setSanity(Player player, int value) {
        int clamped = Math.max(0, Math.min(MAX_SANITY, value));
        player.getPersistentData().putInt(NBT_SANITY, clamped);
    }

    public static void addSanity(Player player, int delta) {
        setSanity(player, getSanity(player) + delta);
    }

    /**
     * Move o player pra spirit world preservando coords X/Y/Z. Salva NBT de
     * volta. NÃO spawna body — quem chama (item/ritual) faz isso.
     *
     * @return true se teleportou, false se já estava em spirit
     */
    public static boolean enterSpiritWorld(ServerPlayer sp) {
        if (isInSpiritWorld(sp)) return false;
        MinecraftServer server = sp.server;
        ServerLevel spirit = server.getLevel(SPIRIT_WORLD);
        if (spirit == null) return false;

        // Salva coords de retorno + dim + ENTER_TICK pra grace period
        var data = sp.getPersistentData();
        data.putString(NBT_RETURN_DIM, sp.level().dimension().location().toString());
        data.putDouble(NBT_RETURN_X, sp.getX());
        data.putDouble(NBT_RETURN_Y, sp.getY());
        data.putDouble(NBT_RETURN_Z, sp.getZ());
        // r32: grace period - bloqueia body-link checks por 10s após enter
        data.putLong(NBT_ENTER_TICK, sp.level().getGameTime());

        // r32: força chunk do body permanecer carregado enquanto player no spirit
        if (sp.level() instanceof ServerLevel originLvl) {
            var chunkPos = new net.minecraft.world.level.ChunkPos(
                    (int) sp.getX() >> 4, (int) sp.getZ() >> 4);
            originLvl.setChunkForced(chunkPos.x, chunkPos.z, true);
        }

        sp.teleportTo(spirit, sp.getX(), sp.getY(), sp.getZ(),
                EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                sp.getYRot(), sp.getXRot());
        // r180b: imune a re-captura por rift logo após entrar (evita bounce).
        grantRiftImmunity(sp, 100);
        return true;
    }

    /**
     * Move player de spirit world pra coords salvas no NBT. Quem chama deve
     * despawnar o body antes/depois conforme apropriado.
     *
     * <p>v0.1.22 r25 BUGFIX: se NBT está faltando (caso de player stuck
     * em spirit sem registro de origem — server crash, etc), agora faz
     * fallback EMERGENCIAL pro overworld spawn em vez de retornar false
     * silenciosamente. Previne player ficar preso pra sempre.
     *
     * @return true se voltou (mesmo emergencialmente), false só se já não
     *         estava em spirit
     */
    /**
     * r38: true se o player está com tome_cursed (foi exilado pro spirit world
     * pelo Tomo Proibido depois de 4 min de horror). Só sai com Prayer Book.
     */
    public static boolean isTomeCursed(Player player) {
        return player.getPersistentData().getBoolean(NBT_TOME_CURSED);
    }

    /**
     * r38: variante de {@link #returnToBody} chamada PELO PRAYER BOOK quando
     * player tomeCursed quer escapar. Remove a curse, limpa cosmic horror,
     * teleporta de volta. Aplica buff de regen pós-escape.
     */
    public static boolean returnViaPrayer(ServerPlayer sp) {
        if (!isInSpiritWorld(sp)) return false;
        var data = sp.getPersistentData();
        // Remove curse antes de fazer returnToBody (senão bloquearia de novo)
        data.remove(NBT_TOME_CURSED);
        // Limpa cosmic horror também (sessão pode estar persistente)
        try {
            br.com.murilo.liberthia.cosmic.CosmicHorrorManager.reset(sp);
        } catch (Throwable ignored) {}
        boolean ok = returnToBodyInternal(sp, true);
        if (ok) {
            sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.REGENERATION, 200, 1, true, true));
            sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SATURATION, 100, 0, true, true));
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§e§l✦ §r§eA oração quebra a maldição. Você retorna ao seu corpo."), false);
        }
        return ok;
    }

    public static boolean returnToBody(ServerPlayer sp) {
        // r38: tome_cursed bloqueia return normal — precisa do Prayer Book
        if (isTomeCursed(sp)) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§4§o§lA maldição do Tomo te prende ao Outro Lado. "
                            + "§r§4§oUse um §6Livro do Êxodo§r§4§o pra escapar."), true);
            return false;
        }
        return returnToBodyInternal(sp, false);
    }

    private static boolean returnToBodyInternal(ServerPlayer sp, boolean viaPrayer) {
        if (!isInSpiritWorld(sp)) return false;
        var data = sp.getPersistentData();

        ServerLevel target;
        double x, y, z;
        if (data.contains(NBT_RETURN_DIM)) {
            // Caminho normal
            String dimStr = data.getString(NBT_RETURN_DIM);
            // r26 BUGFIX: ResourceLocation.tryParse não crasha se string
            // está malformada (vazio, null, etc). Antes ResourceLocation(dimStr)
            // throw IllegalArgumentException → travava o player no spirit.
            ResourceLocation rl = ResourceLocation.tryParse(dimStr);
            target = null;
            if (rl != null) {
                ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, rl);
                target = sp.server.getLevel(dimKey);
            }
            if (target == null) target = sp.server.overworld();
            x = data.getDouble(NBT_RETURN_X);
            y = data.getDouble(NBT_RETURN_Y);
            z = data.getDouble(NBT_RETURN_Z);
        } else {
            // Fallback emergencial — overworld spawn
            target = sp.server.overworld();
            net.minecraft.core.BlockPos spawn = target.getSharedSpawnPos();
            x = spawn.getX() + 0.5;
            y = spawn.getY();
            z = spawn.getZ() + 0.5;
            br.com.murilo.liberthia.LiberthiaMod.LOGGER.warn(
                    "[SpiritWorld] {} sem NBT_RETURN_DIM — fallback pro overworld spawn",
                    sp.getName().getString());
        }

        // r32: solta o chunk forçado que estava grudado
        if (data.contains(NBT_RETURN_X)) {
            try {
                var chunkPos = new net.minecraft.world.level.ChunkPos(
                        (int) data.getDouble(NBT_RETURN_X) >> 4,
                        (int) data.getDouble(NBT_RETURN_Z) >> 4);
                target.setChunkForced(chunkPos.x, chunkPos.z, false);
            } catch (Throwable ignored) {}
        }

        sp.teleportTo(target, x, y, z,
                EnumSet.noneOf(net.minecraft.world.entity.RelativeMovement.class),
                sp.getYRot(), sp.getXRot());
        // r180b (report #73): imune a re-captura por rift ao voltar — sem isto o
        // player cai no rift de origem e é sugado de volta pra dimensão na hora.
        grantRiftImmunity(sp, 100);

        // Limpa NBT
        data.remove(NBT_BODY_UUID);
        data.remove(NBT_RETURN_DIM);
        data.remove(NBT_RETURN_X);
        data.remove(NBT_RETURN_Y);
        data.remove(NBT_RETURN_Z);
        data.remove(NBT_ENTER_TICK);
        return true;
    }

    /** Retorna posição salva ou null se não há. */
    public static Vec3 getSavedReturnPos(Player player) {
        var data = player.getPersistentData();
        if (!data.contains(NBT_RETURN_X)) return null;
        return new Vec3(data.getDouble(NBT_RETURN_X),
                data.getDouble(NBT_RETURN_Y),
                data.getDouble(NBT_RETURN_Z));
    }

    /** r180b: marca o player imune a re-captura por rift por {@code ticks} ticks. */
    public static void grantRiftImmunity(ServerPlayer sp, int ticks) {
        long now = sp.server.overworld().getGameTime();
        sp.getPersistentData().putLong(NBT_RIFT_IMMUNE, now + ticks);
    }

    /** r180b: true se o player ainda está no período de imunidade a rift. */
    public static boolean isRiftImmune(ServerPlayer sp) {
        return sp.getPersistentData().getLong(NBT_RIFT_IMMUNE) > sp.server.overworld().getGameTime();
    }
}
