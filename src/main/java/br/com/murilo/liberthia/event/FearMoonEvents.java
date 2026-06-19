package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager;
import br.com.murilo.liberthia.fog.PersonalFogManager;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.FearMoonSyncS2CPacket;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModEntities;
import br.com.murilo.liberthia.world.FearMoonData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * <b>A Lua do Medo</b> — evento de noite raro onde as regras de segurança
 * quebram. Orquestra: sorteio ao anoitecer (5%), aviso (tambor/heartbeat),
 * efeito {@code MEDO} (Fraqueza V + Velocidade IV) em quem está na rua, monstros
 * mais fortes, mobs que entram em casa somem, proibição de dormir, nevoeiro
 * vermelho, sons, doppelgangers e fendas.
 *
 * <p>Registrado manualmente em {@code LiberthiaMod}. Estado em {@link FearMoonData}.
 */
public final class FearMoonEvents {

    private FearMoonEvents() {}

    /** Chance por noite de a Lua do Medo nascer. */
    public static final float FEAR_CHANCE = 0.05F;

    // ───────────────────────── tick principal ─────────────────────────

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;

        MinecraftServer server = sl.getServer();
        FearMoonData data = FearMoonData.get(server);
        long dayTime = sl.getDayTime() % 24000L;
        long day = sl.getDayTime() / 24000L;

        // SORTEIO ao anoitecer (~13000) — uma vez por noite
        if (dayTime >= 13000 && dayTime < 13400 && data.getLastRolledDay() != day) {
            data.setLastRolledDay(day);
            boolean fear = data.isForceNext() || sl.random.nextFloat() < FEAR_CHANCE;
            data.setForceNext(false);
            if (fear && !data.isActive()) startFearMoon(server, sl);
        }

        // FIM ao amanhecer
        if (data.isActive() && sl.isDay()) {
            endFearMoon(server, sl);
            return;
        }
        if (!data.isActive()) return;

        // ── DURANTE a Lua do Medo ──
        long t = sl.getGameTime();

        if (t % 20 == 0) {
            int fog = darken(data.getColor());
            for (ServerPlayer p : sl.players()) {
                PersonalFogManager.apply(p, fog, 0.55F, 3);
                boolean outdoor = sl.isNight() && sl.canSeeSky(p.blockPosition());
                if (outdoor) {
                    // Efeito Medo: Fraqueza V + Velocidade IV (pânico) + marker
                    p.addEffect(new MobEffectInstance(ModEffects.MEDO.get(), 60, 0, true, true));
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 4, true, false));
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 3, true, false));
                } else {
                    p.removeEffect(ModEffects.MEDO.get());
                }
            }
        }

        // sons de terror
        if (t % 70 == 0) {
            for (ServerPlayer p : sl.players()) {
                if (sl.random.nextFloat() < 0.5F) CosmicSoundManager.playSkyHum(p);
                if (sl.random.nextFloat() < 0.25F) CosmicSoundManager.playDistantScream(p);
            }
        }
        // tambor/batimento lento
        if (t % 80 == 0) {
            for (ServerPlayer p : sl.players()) {
                CosmicSoundManager.playPositional(p, SoundEvents.WARDEN_HEARTBEAT,
                        p.getX(), p.getY(), p.getZ(), 1.2F, 0.5F);
            }
        }
        // mobs que entraram em casa somem (qualquer um na rua é alvo)
        if (t % 20 == 10) {
            antiHouseDespawn(sl);
        }
        // doppelgangers + fendas raros
        if (t % 600 == 0) {
            spawnHorrors(sl);
        }
    }

    // ───────────────────────── start / end ─────────────────────────

    /** Inicia a Lua do Medo AGORA (também chamado pelo Tambor da Lua do Medo). */
    public static void startFearMoon(MinecraftServer server, ServerLevel sl) {
        FearMoonData.get(server).setActive(true);
        int fog = darken(FearMoonData.get(server).getColor());
        for (ServerPlayer p : sl.players()) {
            p.displayClientMessage(Component.literal("§4§l☾ A LUA DO MEDO SE ERGUE ☾"), false);
            p.displayClientMessage(Component.literal("§cNão durma. Não confie em ninguém. Fique abrigado."), false);
            CosmicSoundManager.playPositional(p, SoundEvents.WARDEN_HEARTBEAT,
                    p.getX(), p.getY(), p.getZ(), 1.6F, 0.35F);
            PersonalFogManager.apply(p, fog, 0.55F, 10);
        }
        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§8§oUm tambor distante ecoa na escuridão..."), false);
        syncAll(server);
        LiberthiaMod.LOGGER.info("[FearMoon] A Lua do Medo começou.");
    }

    public static void endFearMoon(MinecraftServer server, ServerLevel sl) {
        FearMoonData.get(server).setActive(false);
        for (ServerPlayer p : sl.players()) {
            PersonalFogManager.clear(p);
            p.removeEffect(ModEffects.MEDO.get());
            p.displayClientMessage(Component.literal("§7O amanhecer dissipa o medo. A lua some."), true);
        }
        syncAll(server);
        LiberthiaMod.LOGGER.info("[FearMoon] A Lua do Medo terminou.");
    }

    /** Reenvia o estado (ativa + cor) pra todos os clientes (lua/céu vermelhos). */
    public static void syncAll(MinecraftServer server) {
        FearMoonData d = FearMoonData.get(server);
        ModNetwork.sendToAll(server, new FearMoonSyncS2CPacket(d.isActive(), d.getColor()));
    }

    /** Escurece uma cor pra usar como névoa (mantém o matiz, reduz brilho). */
    private static int darken(int rgb) {
        int r = (int) (((rgb >> 16) & 0xFF) * 0.35);
        int g = (int) (((rgb >> 8) & 0xFF) * 0.35);
        int b = (int) ((rgb & 0xFF) * 0.35);
        return (r << 16) | (g << 8) | b;
    }

    /** Sincroniza o estado pra quem entra (vê a lua/céu se já estiver ativa). */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            FearMoonData d = FearMoonData.get(sp.server);
            ModNetwork.sendToPlayer(sp, new FearMoonSyncS2CPacket(d.isActive(), d.getColor()));
        }
    }

    // ───────────────────────── sub-sistemas ─────────────────────────

    /** Mobs sob teto (em casa) perto de um player somem. Rua = alvo fácil. */
    private static void antiHouseDespawn(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            AABB box = p.getBoundingBox().inflate(28);
            for (Monster m : sl.getEntitiesOfClass(Monster.class, box, e -> e.isAlive())) {
                BlockPos pos = m.blockPosition();
                if (!sl.canSeeSky(pos) && hasRoof(sl, pos)) {
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                            m.getX(), m.getY() + 0.6, m.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
                    sl.sendParticles(ParticleTypes.SMOKE,
                            m.getX(), m.getY() + 0.6, m.getZ(), 8, 0.25, 0.4, 0.25, 0.01);
                    m.discard();
                }
            }
        }
    }

    private static boolean hasRoof(ServerLevel sl, BlockPos pos) {
        for (int dy = 1; dy <= 5; dy++) {
            BlockPos a = pos.above(dy);
            if (sl.getBlockState(a).isSolidRender(sl, a)) return true;
        }
        return false;
    }

    /** Doppelgangers hostis + fendas pra um mundo que não existe (raros, só na rua). */
    private static void spawnHorrors(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (!sl.canSeeSky(p.blockPosition())) continue; // só pra quem está exposto
            if (sl.random.nextFloat() < 0.35F) spawnDoppelganger(sl, p);
            if (sl.random.nextFloat() < 0.15F) spawnRift(sl, p);
        }
    }

    private static void spawnDoppelganger(ServerLevel sl, ServerPlayer p) {
        var clone = ModEntities.REFLECTION_ENTITY.get().create(sl);
        if (clone == null) return;
        double a = sl.random.nextDouble() * Math.PI * 2;
        double dist = 8 + sl.random.nextDouble() * 8;
        double x = p.getX() + Math.cos(a) * dist;
        double z = p.getZ() + Math.sin(a) * dist;
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
        clone.setOwnerUuid(p.getUUID());
        clone.setOwnerName(p.getName().getString());
        clone.copyEquipmentFrom(p);
        clone.setHostile(true); // não confie em ninguém
        clone.moveTo(x + 0.5, y, z + 0.5, sl.random.nextFloat() * 360, 0);
        sl.addFreshEntity(clone);
    }

    private static void spawnRift(ServerLevel sl, ServerPlayer p) {
        var rift = ModEntities.DIMENSIONAL_RIFT.get().create(sl);
        if (rift == null) return;
        double a = sl.random.nextDouble() * Math.PI * 2;
        double dist = 6 + sl.random.nextDouble() * 10;
        double x = p.getX() + Math.cos(a) * dist;
        double z = p.getZ() + Math.sin(a) * dist;
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
        rift.moveTo(x + 0.5, y + 1, z + 0.5, 0F, 0F);
        sl.addFreshEntity(rift);
    }

    // ───────────────────────── outros hooks ─────────────────────────

    /** Monstros que nascem na Lua do Medo são mais fortes. */
    @SubscribeEvent
    public static void onMobJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        if (!(event.getEntity() instanceof Monster m)) return;
        if (!FearMoonData.get(sl.getServer()).isActive()) return;
        m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 24000, 1, true, false));   // Força II
        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 24000, 0, true, false)); // Velocidade I
        m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 24000, 0, true, false)); // Resistência I
    }

    /** Ninguém dorme sob a Lua do Medo. */
    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player.level() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;
        if (FearMoonData.get(sl.getServer()).isActive()) {
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
            player.displayClientMessage(
                    Component.literal("§4Você não consegue dormir sob a Lua do Medo."), true);
        }
    }
}
