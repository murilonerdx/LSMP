package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.entity.ClonePlayerEntity;
import br.com.murilo.liberthia.item.MaddeningGazeItem;
import br.com.murilo.liberthia.item.MadnessAuraItem;
import br.com.murilo.liberthia.item.MassPossessionCrownItem;
import br.com.murilo.liberthia.item.MirrorOfInsanityItem;
import br.com.murilo.liberthia.network.ModNetwork;
import br.com.murilo.liberthia.network.packet.MadnessHallucinationS2CPacket;
import br.com.murilo.liberthia.network.packet.MaddenedTargetS2CPacket;
import br.com.murilo.liberthia.network.packet.MirrorInsanityS2CPacket;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Random;

/**
 * v0.1.22 r23: Handler central dos 4 itens cósmicos passivos.
 *
 * <h2>Mecânicas</h2>
 * <ul>
 *   <li><b>Madness Aura</b>: tick em players com o item, gera alucinações em
 *       players num raio.</li>
 *   <li><b>Maddening Gaze</b>: tick em holders, checa raycast no olhar →
 *       aplica Nausea/Wither em alvo + S2C RUN overlay.</li>
 *   <li><b>Mass Possession Crown</b>: chat com prefix "!" envia actionbar
 *       pra todos puppets do pool.</li>
 *   <li><b>Mirror of Insanity</b>: tick em holders, envia S2C pra players
 *       próximos exibirem "QUEM SOU EU?" overlay + render override.</li>
 *   <li><b>Soul Cloner cleanup</b>: tick em ClonePlayerEntity, despawna
 *       após CLONE_TTL_TICKS.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class MadnessEvents {

    private static final Random RNG = new Random();
    /** Próximo tick em que cada player pode receber outra alucinação. */
    private static final Map<UUID, Long> NEXT_HALLUCINATION = new ConcurrentHashMap<>();
    /** Tick do último update de Mirror pra cada holder. */
    private static final Map<UUID, Long> LAST_MIRROR_TICK = new ConcurrentHashMap<>();

    private MadnessEvents() {}

    /**
     * Helper: true se player tem o item no inv/mainhand/offhand.
     */
    private static boolean hasItem(Player p, Item item) {
        if (p.getMainHandItem().is(item)) return true;
        if (p.getOffhandItem().is(item)) return true;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(item)) return true;
        }
        return false;
    }

    /**
     * Tick principal — roda em todos os players online a cada tick.
     * Otimização: faz check de item rápido antes de qualquer trabalho.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        long now = sp.level().getGameTime();

        // (1) MADNESS AURA — emite alucinações em players próximos
        if (sp.tickCount % 40 == 0 && hasItem(sp, ModItems.MADNESS_AURA.get())) {
            tickMadnessAura(sp, now);
        }

        // (2) MADDENING GAZE — checa raycast no olhar
        if (sp.tickCount % 10 == 0 && hasItem(sp, ModItems.MADDENING_GAZE.get())) {
            tickMaddeningGaze(sp);
        }

        // (3) MIRROR OF INSANITY — efeito perceptual em players próximos
        Long lastMirror = LAST_MIRROR_TICK.get(sp.getUUID());
        if ((lastMirror == null || now - lastMirror >= MirrorOfInsanityItem.UPDATE_INTERVAL)
                && hasItem(sp, ModItems.MIRROR_OF_INSANITY.get())) {
            tickMirror(sp);
            LAST_MIRROR_TICK.put(sp.getUUID(), now);
        }

        // (4) MASS POSSESSION CROWN — efeito passivo nos puppets
        if (sp.tickCount % 60 == 0) {
            ItemStack mainHand = sp.getMainHandItem();
            ItemStack offHand = sp.getOffhandItem();
            ItemStack crown = mainHand.is(ModItems.MASS_POSSESSION_CROWN.get()) ? mainHand
                    : offHand.is(ModItems.MASS_POSSESSION_CROWN.get()) ? offHand
                    : ItemStack.EMPTY;
            if (!crown.isEmpty()) {
                tickCrownPassive(sp, crown);
            }
        }
    }

    /** Madness Aura: gera alucinação aleatória em cada player próximo. */
    private static void tickMadnessAura(ServerPlayer holder, long now) {
        AABB box = new AABB(holder.blockPosition()).inflate(MadnessAuraItem.RADIUS);
        List<ServerPlayer> nearby = holder.serverLevel().getEntitiesOfClass(
                ServerPlayer.class, box,
                p -> p != holder && !p.isCreative() && !p.isSpectator());
        for (ServerPlayer target : nearby) {
            Long nextTick = NEXT_HALLUCINATION.get(target.getUUID());
            if (nextTick != null && now < nextTick) continue;
            // Schedule next ~3-6s
            int delay = MadnessAuraItem.MIN_INTERVAL
                    + RNG.nextInt(MadnessAuraItem.MAX_INTERVAL - MadnessAuraItem.MIN_INTERVAL);
            NEXT_HALLUCINATION.put(target.getUUID(), now + delay);
            // Tipo aleatório
            int type = RNG.nextInt(4);
            ModNetwork.sendToPlayer(target, new MadnessHallucinationS2CPacket(type, RNG.nextInt()));
        }
    }

    /** Maddening Gaze: raycast no olhar, aplica efeito no alvo. */
    private static void tickMaddeningGaze(ServerPlayer holder) {
        Vec3 eye = holder.getEyePosition();
        Vec3 look = holder.getViewVector(1.0F).normalize();
        AABB scanBox = new AABB(eye, eye.add(look.scale(MaddeningGazeItem.MAX_DISTANCE)))
                .inflate(2.0);
        List<ServerPlayer> candidates = holder.serverLevel().getEntitiesOfClass(
                ServerPlayer.class, scanBox,
                p -> p != holder && p.isAlive() && !p.isCreative() && !p.isSpectator());
        for (ServerPlayer target : candidates) {
            Vec3 toTarget = target.position().add(0, 1, 0).subtract(eye);
            double dist = toTarget.length();
            if (dist > MaddeningGazeItem.MAX_DISTANCE) continue;
            double dot = toTarget.normalize().dot(look);
            if (dot < MaddeningGazeItem.GAZE_DOT_THRESHOLD) continue;
            // GAZE acertou — aplica efeitos
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 1, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0, false, true, true));
            target.hurt(target.damageSources().magic(), 1.0F);
            // S2C RUN overlay
            ModNetwork.sendToPlayer(target, new MaddenedTargetS2CPacket());
        }
    }

    /** Mirror: envia packet pros players próximos pra trigger overlay + render override. */
    private static void tickMirror(ServerPlayer holder) {
        AABB box = new AABB(holder.blockPosition()).inflate(MirrorOfInsanityItem.RADIUS);
        List<ServerPlayer> nearby = holder.serverLevel().getEntitiesOfClass(
                ServerPlayer.class, box,
                p -> p != holder && !p.isCreative() && !p.isSpectator());
        for (ServerPlayer target : nearby) {
            ModNetwork.sendToPlayer(target,
                    new MirrorInsanityS2CPacket(MirrorOfInsanityItem.UPDATE_INTERVAL));
        }
    }

    /** Crown passivo: aplica Slowness I + partículas nos puppets. */
    private static void tickCrownPassive(ServerPlayer holder, ItemStack crown) {
        List<UUID> puppets = MassPossessionCrownItem.getPuppets(crown);
        if (puppets.isEmpty()) return;
        for (UUID id : puppets) {
            ServerPlayer puppet = holder.server.getPlayerList().getPlayer(id);
            if (puppet == null) continue;
            puppet.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, false, false, false));
            if (puppet.level() instanceof ServerLevel psl) {
                psl.sendParticles(ParticleTypes.WITCH,
                        puppet.getX(), puppet.getY() + 1.8, puppet.getZ(),
                        3, 0.2, 0.1, 0.2, 0.01);
            }
        }
    }

    /**
     * Chat hook: se mensagem começa com "!" E sender tem Mass Possession Crown
     * com puppets:
     * <ul>
     *   <li>{@code !!whisper NAME} — seta voice target SVC (só ele ouve via voz)</li>
     *   <li>{@code !!end NAME} — encerra posse desse puppet</li>
     *   <li>{@code !!clear} — limpa voice target</li>
     *   <li>{@code !!list} — lista puppets atuais</li>
     *   <li>{@code !mensagem} — manda como actionbar pros puppets (legacy)</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        String msg = event.getMessage().getString();
        if (!msg.startsWith(MassPossessionCrownItem.CHAT_PREFIX)) return;

        ItemStack crown = ItemStack.EMPTY;
        if (sender.getMainHandItem().is(ModItems.MASS_POSSESSION_CROWN.get())) {
            crown = sender.getMainHandItem();
        } else if (sender.getOffhandItem().is(ModItems.MASS_POSSESSION_CROWN.get())) {
            crown = sender.getOffhandItem();
        } else {
            return;
        }
        List<UUID> puppets = MassPossessionCrownItem.getPuppets(crown);

        // r30: comandos !! (dois !) — antes de chegar no broadcast normal
        if (msg.startsWith("!!")) {
            event.setCanceled(true);
            handleCommand(sender, crown, msg.substring(2).trim(), puppets);
            return;
        }
        if (puppets.isEmpty()) return;

        String thought = msg.substring(MassPossessionCrownItem.CHAT_PREFIX.length()).trim();
        if (thought.isEmpty()) return;

        event.setCanceled(true);
        Component thoughtMsg = Component.literal("§4§o" + thought);
        int delivered = 0;
        for (UUID id : puppets) {
            ServerPlayer puppet = sender.server.getPlayerList().getPlayer(id);
            if (puppet == null) continue;
            puppet.displayClientMessage(thoughtMsg, true);
            delivered++;
        }
        sender.displayClientMessage(Component.literal(
                "§d✦ Pensamento enviado a §a" + delivered + "§7/§7" + puppets.size() + " puppets."), true);
    }

    /** r30: dispatcher de comandos !! da crown. */
    private static void handleCommand(ServerPlayer sender, ItemStack crown,
                                       String body, List<UUID> puppets) {
        String[] parts = body.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "clear" -> {
                if (crown.hasTag()) crown.getTag().remove(
                        br.com.murilo.liberthia.network.packet.CrownVoiceTargetC2SPacket.NBT_VOICE_TARGET);
                sender.displayClientMessage(Component.literal(
                        "§d✦ Voice target §climpo§7 — falando em broadcast normal."), true);
            }
            case "list" -> {
                sender.displayClientMessage(Component.literal(
                        "§d✦ Puppets §a(" + puppets.size() + ")§7:"), false);
                for (UUID id : puppets) {
                    ServerPlayer p = sender.server.getPlayerList().getPlayer(id);
                    String name = p != null ? p.getName().getString() : "§8(offline)";
                    sender.displayClientMessage(Component.literal("  §7- §e" + name), false);
                }
            }
            case "whisper", "w" -> {
                if (arg.isEmpty()) {
                    sender.displayClientMessage(Component.literal(
                            "§cUso: !!whisper <player>"), true);
                    return;
                }
                ServerPlayer target = findPuppetByName(sender, puppets, arg);
                if (target == null) {
                    sender.displayClientMessage(Component.literal(
                            "§cPlayer não encontrado no pool: " + arg), true);
                    return;
                }
                crown.getOrCreateTag().putUUID(
                        br.com.murilo.liberthia.network.packet.CrownVoiceTargetC2SPacket.NBT_VOICE_TARGET,
                        target.getUUID());
                sender.displayClientMessage(Component.literal(
                        "§d✦ §lWhisper§r§d ativo: sua voz vai só pra §a" + target.getName().getString()), true);
                sender.displayClientMessage(Component.literal(
                        "§7Use §e!!clear§7 pra parar."), false);
            }
            case "end", "kill" -> {
                if (arg.isEmpty()) {
                    sender.displayClientMessage(Component.literal(
                            "§cUso: !!end <player>"), true);
                    return;
                }
                ServerPlayer target = findPuppetByName(sender, puppets, arg);
                if (target == null) {
                    sender.displayClientMessage(Component.literal(
                            "§cPlayer não encontrado no pool: " + arg), true);
                    return;
                }
                MassPossessionCrownItem.togglePuppet(crown, target.getUUID());
                if (crown.hasTag() && crown.getTag().hasUUID(
                        br.com.murilo.liberthia.network.packet.CrownVoiceTargetC2SPacket.NBT_VOICE_TARGET)
                        && crown.getTag().getUUID(
                                br.com.murilo.liberthia.network.packet.CrownVoiceTargetC2SPacket.NBT_VOICE_TARGET)
                                .equals(target.getUUID())) {
                    crown.getTag().remove(
                            br.com.murilo.liberthia.network.packet.CrownVoiceTargetC2SPacket.NBT_VOICE_TARGET);
                }
                sender.displayClientMessage(Component.literal(
                        "§d✦ Posse encerrada em §c" + target.getName().getString()), true);
                target.displayClientMessage(Component.literal(
                        "§a✦ A presença em sua mente §lse retira§r§a."), false);
            }
            default -> sender.displayClientMessage(Component.literal(
                    "§cComandos: §e!!whisper <name>§c | §e!!end <name>§c | §e!!clear§c | §e!!list"), true);
        }
    }

    private static ServerPlayer findPuppetByName(ServerPlayer sender, List<UUID> puppets, String name) {
        for (UUID id : puppets) {
            ServerPlayer p = sender.server.getPlayerList().getPlayer(id);
            if (p != null && p.getName().getString().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Server tick — limpa clones que passaram do TTL (5 min).
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % 100 != 0) return; // a cada 5s
        long now = event.getServer().overworld().getGameTime();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (var e : level.getAllEntities()) {
                if (!(e instanceof ClonePlayerEntity clone)) continue;
                long despawn = clone.getPersistentData().getLong("liberthia.clone_despawn");
                if (despawn > 0 && now >= despawn) {
                    level.sendParticles(ParticleTypes.SMOKE,
                            clone.getX(), clone.getY() + 1.0, clone.getZ(),
                            15, 0.3, 0.5, 0.3, 0.05);
                    clone.discard();
                }
            }
        }
    }
}
