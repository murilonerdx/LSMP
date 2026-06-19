package br.com.murilo.liberthia.dimension;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entity.SoulBodyEntity;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.SanitySyncS2CPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * v0.1.22 r24 (r32 BUG FIX): handlers do sistema espiritual.
 *
 * <h2>r32 BUG FIX — auto-return causava ejeção em 3s</h2>
 * Bug reportado: "vai pro espiritual, em poucos segundos volta sozinho ao
 * mundo normal, corpo fica parado, não consigo voltar pro corpo clicando".
 *
 * <p><b>Causa raiz:</b> {@code onPlayerTick} fazia body-link check a cada
 * 60 ticks (3s). Quando o body estava em chunk descarregado (player teleportou
 * pra spirit world, body's chunk unloaded), {@code getEntity(bodyUuid)}
 * retornava null → handler interpretava como "body destruído" → forçava
 * return + 50% dano. Player era ejetado em 3s.
 *
 * <p><b>Fix:</b>
 * <ul>
 *   <li>Adiciona NBT_ENTER_TICK — timestamp de quando entrou em spirit</li>
 *   <li>GRACE PERIOD de 200 ticks (10s) antes de body-link checks</li>
 *   <li>Requer 3 checks consecutivos falhados (60s) antes de auto-return</li>
 *   <li>Body é {@code setPersistenceRequired} no spawn (não pickup vanilla)</li>
 *   <li>Force-load do chunk do body com {@code setChunkForced(true)} pra
 *       garantir que ele NUNCA descarregue enquanto player no spirit</li>
 *   <li>Remove emergência "sem NBT_BODY_UUID → force return" — agora apenas
 *       loga warning e deixa o player no spirit (clique pra voltar)</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class SpiritWorldEvents {

    /** Ticks após enterSpirit em que body-link checks ficam desabilitados. */
    public static final int GRACE_PERIOD_TICKS = 200; // 10s
    /** Checks consecutivos falhados necessários pra forçar return (1 check / 3s). */
    public static final int CONSECUTIVE_FAILS_FOR_RETURN = 5; // 15s de falha contínua

    /** Mapa em-memória de fails consecutivos por player UUID. */
    private static final java.util.Map<UUID, Integer> CONSECUTIVE_FAILS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private SpiritWorldEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        boolean inSpirit = SpiritDimension.isInSpiritWorld(sp);
        int sanity = SpiritDimension.getSanity(sp);

        // (1) Sanity drain em spirit
        if (inSpirit && sp.tickCount % SpiritDimension.SANITY_DRAIN_INTERVAL == 0) {
            SpiritDimension.addSanity(sp, -1);
            sanity = SpiritDimension.getSanity(sp);
            ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(sanity));
        }

        // (2) Sanity regen em overworld — r179: MUITO mais rápido sob o SOL
        // (dia, céu visível, sem chuva). Ficar na luz do sol restaura lucidez.
        if (!inSpirit && sanity < SpiritDimension.MAX_SANITY) {
            boolean sunlit = sp.level().isDay() && !sp.level().isRaining()
                    && sp.level().canSeeSky(sp.blockPosition().above());
            int interval = sunlit ? Math.max(1, SpiritDimension.SANITY_REGEN_INTERVAL / 2)
                                  : SpiritDimension.SANITY_REGEN_INTERVAL;
            if (sp.tickCount % interval == 0) {
                SpiritDimension.addSanity(sp, sunlit ? 2 : 1);
                sanity = SpiritDimension.getSanity(sp);
                ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(sanity));
            }
        }

        // (3) Sanity effects
        if (sanity < SpiritDimension.CRITICAL_SANITY_THRESHOLD) {
            if (sp.tickCount % 60 == 0) {
                sp.hurt(sp.damageSources().magic(), 1.0F);
                sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false));
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, true, false));
            }
        } else if (sanity < SpiritDimension.LOW_SANITY_THRESHOLD) {
            if (sp.tickCount % 100 == 0) {
                sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, true, false));
            }
        }

        // (4) Body health link — r32: GRACE PERIOD + CONSECUTIVE FAILS
        if (inSpirit && sp.tickCount % 60 == 0) {
            var data = sp.getPersistentData();
            // r32: respeita grace period
            long enterTick = data.getLong(SpiritDimension.NBT_ENTER_TICK);
            long now = sp.level().getGameTime();
            if (enterTick > 0 && (now - enterTick) < GRACE_PERIOD_TICKS) {
                // Ainda em grace period — skip check
                return;
            }

            if (data.hasUUID(SpiritDimension.NBT_BODY_UUID)) {
                UUID bodyUuid = data.getUUID(SpiritDimension.NBT_BODY_UUID);
                String returnDim = data.getString(SpiritDimension.NBT_RETURN_DIM);
                double bodyX = data.getDouble(SpiritDimension.NBT_RETURN_X);
                double bodyZ = data.getDouble(SpiritDimension.NBT_RETURN_Z);

                ServerLevel originLevel = sp.server.getLevel(
                        net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION,
                                net.minecraft.resources.ResourceLocation.tryParse(returnDim)));
                if (originLevel != null) {
                    // r32: força chunk loaded permanently enquanto player no spirit
                    var chunkPos = new net.minecraft.world.level.ChunkPos(
                            (int) bodyX >> 4, (int) bodyZ >> 4);
                    originLevel.setChunkForced(chunkPos.x, chunkPos.z, true);
                    var entity = originLevel.getEntity(bodyUuid);

                    if (entity == null || entity.isRemoved()) {
                        // r32: contagem de falhas consecutivas
                        int fails = CONSECUTIVE_FAILS.getOrDefault(sp.getUUID(), 0) + 1;
                        CONSECUTIVE_FAILS.put(sp.getUUID(), fails);
                        LiberthiaMod.LOGGER.debug(
                                "[SpiritWorld] body check fail {}/{} for {}",
                                fails, CONSECUTIVE_FAILS_FOR_RETURN, sp.getName().getString());

                        if (fails >= CONSECUTIVE_FAILS_FOR_RETURN) {
                            // De fato sumiu — força retorno após 15s de falhas
                            sp.displayClientMessage(Component.literal(
                                    "§4§l✦ §r§4Seu corpo foi destruído!"
                            ).withStyle(ChatFormatting.DARK_RED), false);
                            originLevel.setChunkForced(chunkPos.x, chunkPos.z, false);
                            SpiritDimension.returnToBody(sp);
                            sp.hurt(sp.damageSources().magic(), sp.getMaxHealth() * 0.5F);
                            CONSECUTIVE_FAILS.remove(sp.getUUID());
                        }
                    } else {
                        // Body found — reseta contador
                        CONSECUTIVE_FAILS.remove(sp.getUUID());
                        if (entity instanceof SoulBodyEntity body && body.isDying()) {
                            sp.kill();
                        }
                    }
                }
            } else {
                // r32: REMOVIDO o auto-return emergencial. Antes, se NBT_BODY_UUID
                // sumisse o player era ejetado. Agora só loga; player pode clicar
                // pra voltar (returnToBody usa fallback emergencial pra overworld).
                LiberthiaMod.LOGGER.warn(
                        "[SpiritWorld] {} em spirit sem NBT_BODY_UUID — sem auto-return, " +
                        "use Soul Sever pra voltar.",
                        sp.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        CONSECUTIVE_FAILS.remove(sp.getUUID());
        if (SpiritDimension.isInSpiritWorld(sp)) {
            LiberthiaMod.LOGGER.info("[SpiritWorld] {} desconectou em spirit world — body permanece",
                    sp.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        // r165: SEMPRE sincroniza sanidade no login — garante HUD correto mesmo
        // quando player está no overworld com sanidade parcial.
        ModNetwork.sendToPlayer(sp, new SanitySyncS2CPacket(SpiritDimension.getSanity(sp)));
        if (!SpiritDimension.isInSpiritWorld(sp)) return;
        // r32: re-graceperiod no login pra player que reconectou em spirit
        sp.getPersistentData().putLong(SpiritDimension.NBT_ENTER_TICK,
                sp.level().getGameTime());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof SoulBodyEntity body)) return;
        UUID owner = body.getOwnerUuid();
        if (owner == null) return;
        if (body.level() instanceof ServerLevel sl) {
            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(owner);
            if (sp != null) {
                sp.displayClientMessage(Component.literal(
                        "§4§l✦ Seu corpo foi morto. Você é um espírito errante."), false);
                if (SpiritDimension.isInSpiritWorld(sp)) {
                    sp.kill();
                }
            }
            sl.sendParticles(ParticleTypes.SOUL,
                    body.getX(), body.getY() + 1.0, body.getZ(),
                    30, 0.5, 1.0, 0.5, 0.2);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        var oldData = event.getOriginal().getPersistentData();
        if (!oldData.hasUUID(SpiritDimension.NBT_BODY_UUID)) return;
        UUID bodyUuid = oldData.getUUID(SpiritDimension.NBT_BODY_UUID);
        var newData = event.getEntity().getPersistentData();
        newData.remove(SpiritDimension.NBT_BODY_UUID);
        newData.remove(SpiritDimension.NBT_RETURN_DIM);
        newData.remove(SpiritDimension.NBT_RETURN_X);
        newData.remove(SpiritDimension.NBT_RETURN_Y);
        newData.remove(SpiritDimension.NBT_RETURN_Z);
        newData.remove(SpiritDimension.NBT_ENTER_TICK);
        newData.putInt(SpiritDimension.NBT_SANITY, SpiritDimension.MAX_SANITY);

        if (event.getEntity() instanceof ServerPlayer sp) {
            CONSECUTIVE_FAILS.remove(sp.getUUID());
            for (var lvl : sp.server.getAllLevels()) {
                var e = lvl.getEntity(bodyUuid);
                if (e != null) {
                    e.discard();
                    break;
                }
            }
        }
    }
}
