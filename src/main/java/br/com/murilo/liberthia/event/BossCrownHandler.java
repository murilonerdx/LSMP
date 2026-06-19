package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.item.BossCrownItem;
import br.com.murilo.liberthia.logic.LiberthiaMob;
import br.com.murilo.liberthia.registry.ModEffects;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.22 (revisão 2): toda a lógica server-side da Boss Crown.
 *
 * <p>Ampliações sobre v1: regen IV constante, aura damage 4 HP, blood orbital
 * attack (skip mobs do mod), armor crit 25% em atacantes, shift+gaze blindness
 * 10s, blood explosion pulse 40s, panic mode com Regen V + teleport players
 * pra longe, e bossbar com debounce (não some no primeiro hit).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class BossCrownHandler {

    private static final UUID HEALTH_MOD_UUID = UUID.fromString("f7b1a4d2-8e3c-4b9e-9c5d-2a8f7e1b6d3a");
    private static final String HEALTH_MOD_NAME = "liberthia.boss_crown.health";
    private static final double HEALTH_BONUS = 3580.0;

    // ---------- Intervalos ----------
    private static final int REGEN_REFRESH = 60;          // 3s
    private static final int AURA_INTERVAL = 20;          // 1s
    private static final int ORBITAL_INTERVAL = 10;       // 0.5s — sangue orbital
    private static final int PULSE_INTERVAL = 200;        // 10s (blindness+poison)
    private static final int GAZE_INTERVAL = 20;          // 1s
    private static final int PARTICLE_INTERVAL = 2;       // 0.1s
    private static final int BLOOD_EXPLOSION_INTERVAL = 800; // 40s

    // ---------- Panic ----------
    private static final String NBT_PANIC_COOLDOWN = "liberthia.boss_crown.panic_cd";
    private static final int PANIC_COOLDOWN_TICKS = 6000;
    private static final float PANIC_HP_THRESHOLD = 0.25f;

    /**
     * Bossbar state por player. Inclui timer de "fora da mão" pra evitar que
     * a bar suma no primeiro hit (debounce 60 ticks = 3s).
     */
    private static class BarState {
        ServerBossEvent bar;
        int notInHandTicks;
    }
    private static final Map<UUID, BarState> BOSS_BARS = new HashMap<>();
    private static final int BAR_REMOVE_GRACE = 60;

    private BossCrownHandler() {}

    /**
     * v0.1.22 r8: cria/mostra a bossbar IMEDIATAMENTE. Chamado pelo packet
     * handler quando o player ativa via tela. Evita esperar o próximo tick
     * (que pode ter delay perceptível).
     */
    public static void showBossBarNow(ServerPlayer sp, String customName) {
        UUID id = sp.getUUID();
        BarState st = BOSS_BARS.get(id);
        Component title = (customName == null || customName.isEmpty())
                ? Component.literal("§4§l⊕ ").append(sp.getName()).append(Component.literal(" §c§l⊕"))
                : Component.literal(customName);
        if (st == null) {
            st = new BarState();
            st.bar = new ServerBossEvent(title,
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.NOTCHED_20);
            st.bar.setDarkenScreen(false);
            st.bar.setPlayBossMusic(false);
            st.bar.setCreateWorldFog(false);
            st.bar.setVisible(true);
            BOSS_BARS.put(id, st);
            LiberthiaMod.LOGGER.info("[BossCrown bar] showBossBarNow CREATE for {} title='{}'",
                    sp.getName().getString(), title.getString());
        } else {
            st.bar.setName(title);
            st.bar.setVisible(true);
            LiberthiaMod.LOGGER.info("[BossCrown bar] showBossBarNow UPDATE for {}",
                    sp.getName().getString());
        }
        st.notInHandTicks = 0;
        float pct = Math.max(0f, Math.min(1f, sp.getHealth() / sp.getMaxHealth()));
        st.bar.setProgress(pct);
        // FORÇA a adição do próprio dono primeiro
        st.bar.addPlayer(sp);
        if (sp.level() instanceof ServerLevel sl) {
            for (ServerPlayer viewer : sl.players()) {
                if (viewer == sp) continue;
                st.bar.addPlayer(viewer);
            }
        }
        LiberthiaMod.LOGGER.info("[BossCrown bar] {} viewers, progress={}",
                st.bar.getPlayers().size(), st.bar.getProgress());
    }

    /** v0.1.22 r8: remove imediatamente. */
    public static void hideBossBarNow(ServerPlayer sp) {
        BarState st = BOSS_BARS.remove(sp.getUUID());
        if (st != null) {
            st.bar.removeAllPlayers();
            LiberthiaMod.LOGGER.info("[BossCrown bar] hideBossBarNow REMOVE for {}",
                    sp.getName().getString());
        }
        // v0.1.22 r10: FORCE remove HP modifier IMEDIATAMENTE também — não
        // espera o tick. User reportou que vida continuava em 1800 corações
        // após desativar.
        applyOrRemoveHpModifier(sp, false);
        LiberthiaMod.LOGGER.info("[BossCrown] hideBossBarNow — HP modifier removido pra {}",
                sp.getName().getString());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        ItemStack stack = findActiveCrown(sp);
        boolean hasActive = !stack.isEmpty();
        applyOrRemoveHpModifier(sp, hasActive);

        // ─── Bossbar — aparece QUANDO CROW ATIVA EM QUALQUER SLOT ───
        // v0.1.22 r7: antes só mostrava se mainhand. User pediu mostrar sempre
        // que coroa estiver ativa. Agora usa o findActiveCrown — qualquer slot.
        updateBossBar(sp, hasActive, stack);

        if (!hasActive) return;
        long tick = sp.tickCount;

        // 1) Regen IV constante (refresh 3s, duration 200t)
        if (tick % REGEN_REFRESH == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                    200, 3, true, false, true)); // amp 3 = Regen IV
        }

        // 2) Particles esfera de sangue (rápidas)
        if (tick % PARTICLE_INTERVAL == 0 && sp.level() instanceof ServerLevel sl) {
            spawnAuraParticles(sl, sp);
        }

        // 3) Aura damage tick
        if (tick % AURA_INTERVAL == 0) {
            applyAura(sp);
        }

        // 4) Sangue orbital — particles + damage aos players/mobs (skip mod mobs)
        if (tick % ORBITAL_INTERVAL == 0) {
            applyBloodOrbital(sp);
        }

        // 5) Pulso 10s — blindness + poison + infection raio 8
        if (tick % PULSE_INTERVAL == 0) {
            applyPulse(sp);
        }

        // 6) Gaze normal (1s) — só blindness curta se olha pra mim
        if (tick % GAZE_INTERVAL == 0) {
            applyPassiveGaze(sp);
        }

        // 7) Blood explosion massiva a cada 40s
        if (tick % BLOOD_EXPLOSION_INTERVAL == 0 && tick > 0) {
            applyBloodExplosion(sp);
        }

        // 8) Panic mode — HP ≤ 25% libera teleport repulsivo + Regen V
        applyPanicMode(sp);
    }

    /** Reflect 30% + armor crit 25% nos atacantes do dono. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (findActiveCrown(victim).isEmpty()) return;

        var srcEntity = event.getSource().getEntity();
        if (!(srcEntity instanceof LivingEntity attacker)) return;
        if (attacker == victim) return;

        // Reflect 30%
        float reflected = event.getAmount() * 0.30f;
        if (reflected > 0) {
            attacker.hurt(victim.damageSources().thorns(victim), reflected);
        }

        // Armor crit — drena 25% da dur restante de cada peça do atacante
        if (attacker instanceof Player p) {
            for (EquipmentSlot slot : new EquipmentSlot[]{
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack armor = p.getItemBySlot(slot);
                if (armor.isEmpty() || !armor.isDamageableItem()) continue;
                int remaining = armor.getMaxDamage() - armor.getDamageValue();
                int crit = Math.max(1, remaining / 4);
                armor.hurtAndBreak(crit, p, pl -> pl.broadcastBreakEvent(slot));
            }
        }

        if (victim.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.5, attacker.getZ(),
                    8, 0.3, 0.3, 0.3, 0.05);
        }
    }

    /** Shift + olhar pra player = cega ele 10s + infecção. */
    @SubscribeEvent
    public static void onShiftGaze(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (findActiveCrown(sp).isEmpty()) return;
        if (!sp.isShiftKeyDown()) return;
        if (!(event.getTarget() instanceof Player target)) return;
        if (target == sp) return;
        if (target.isCreative() || target.isSpectator()) return;
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0, true, false, true));
        applyBloodInfection(target, 200);
        if (sp.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SQUID_INK,
                    target.getX(), target.getY() + 1.6, target.getZ(),
                    20, 0.3, 0.3, 0.3, 0.05);
        }
        event.setCanceled(true);
    }

    // ─────────────────────── helpers ───────────────────────

    private static ItemStack findActiveCrown(Player p) {
        var crown = ModItems.BOSS_CROWN.get();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(crown) && BossCrownItem.isActive(s)) return s;
        }
        return ItemStack.EMPTY;
    }

    private static void applyOrRemoveHpModifier(ServerPlayer sp, boolean shouldHave) {
        AttributeInstance attr = sp.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        AttributeModifier existing = attr.getModifier(HEALTH_MOD_UUID);
        if (shouldHave && existing == null) {
            attr.addPermanentModifier(new AttributeModifier(
                    HEALTH_MOD_UUID, HEALTH_MOD_NAME, HEALTH_BONUS,
                    AttributeModifier.Operation.ADDITION));
            sp.setHealth((float) attr.getValue());
            LiberthiaMod.LOGGER.info("[BossCrown HP] APPLY mod for {} — maxHP agora {}",
                    sp.getName().getString(), attr.getValue());
        } else if (!shouldHave && existing != null) {
            attr.removeModifier(HEALTH_MOD_UUID);
            sp.setHealth(Math.min(sp.getHealth(), (float) attr.getValue()));
            LiberthiaMod.LOGGER.info("[BossCrown HP] REMOVE mod for {} — maxHP agora {}",
                    sp.getName().getString(), attr.getValue());
        }
    }

    private static void spawnAuraParticles(ServerLevel sl, ServerPlayer sp) {
        double radius = 1.6;
        double t = (sp.tickCount % 80) / 80.0 * Math.PI * 2;
        for (int i = 0; i < 8; i++) {
            double angle = t + i * Math.PI / 4;
            double px = sp.getX() + Math.cos(angle) * radius;
            double pz = sp.getZ() + Math.sin(angle) * radius;
            double py = sp.getY() + 1.2 + Math.sin(t + i) * 0.3;
            sl.sendParticles(ParticleTypes.CRIT, px, py, pz, 1, 0, 0, 0, 0.01);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, px, py, pz, 1, 0, 0, 0, 0.01);
        }
        sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                sp.getX(), sp.getY() + 1.0, sp.getZ(),
                1, 0.2, 0.2, 0.2, 0.01);
    }

    /** Aura damage raio 3 — players próximos levam dano + debuffs. */
    private static void applyAura(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel sl)) return;
        AABB box = new AABB(sp.blockPosition()).inflate(3.0);
        for (Player other : sl.getEntitiesOfClass(Player.class, box)) {
            if (other == sp) continue;
            if (other.isCreative() || other.isSpectator()) continue;
            other.hurt(sp.damageSources().magic(), 4.0f);
            other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 2, true, false, true));
            applyBloodInfection(other, 120);
        }
    }

    /**
     * Sangue orbital — particles densas + damage AoE em raio 5 a cada 0.5s.
     * Atinge players + mobs EXCETO mobs do mod Liberthia.
     */
    private static void applyBloodOrbital(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel sl)) return;
        // Particles orbitais densas em volta da cabeça
        double t = (sp.tickCount % 40) / 40.0 * Math.PI * 2;
        for (int i = 0; i < 5; i++) {
            double angle = t + i * (Math.PI * 2 / 5);
            double r = 0.7;
            double px = sp.getX() + Math.cos(angle) * r;
            double pz = sp.getZ() + Math.sin(angle) * r;
            double py = sp.getY() + sp.getEyeHeight() + 0.2;
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, px, py, pz, 2, 0.05, 0.05, 0.05, 0.05);
            sl.sendParticles(ParticleTypes.CRIT, px, py, pz, 2, 0.05, 0.05, 0.05, 0.05);
        }
        // AoE damage tick: raio 5, 1.5 HP de dano em players + mobs (skip mod mobs)
        AABB box = new AABB(sp.blockPosition()).inflate(5.0);
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (le == sp) continue;
            if (LiberthiaMob.isMob(le)) continue; // skip mobs do mod
            if (le instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            le.hurt(sp.damageSources().magic(), 1.5f);
        }
    }

    /** Pulso 10s — Blindness + Poison II + Infection raio 8 (só players). */
    private static void applyPulse(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel sl)) return;
        sl.playSound(null, sp.blockPosition(), SoundEvents.WITHER_AMBIENT,
                SoundSource.PLAYERS, 1.5F, 0.4F);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                sp.getX(), sp.getY() + 1.0, sp.getZ(),
                40, 1.0, 1.0, 1.0, 0.1);
        AABB box = new AABB(sp.blockPosition()).inflate(8.0);
        for (Player other : sl.getEntitiesOfClass(Player.class, box)) {
            if (other == sp) continue;
            if (other.isCreative() || other.isSpectator()) continue;
            other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, true, false, true));
            other.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1, true, false, true));
            applyBloodInfection(other, 200);
        }
    }

    /** Gaze passiva — player olhando pra mim ganha blindness curta. */
    private static void applyPassiveGaze(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel sl)) return;
        AABB box = new AABB(sp.blockPosition()).inflate(12.0);
        Vec3 meEye = sp.getEyePosition();
        for (Player other : sl.getEntitiesOfClass(Player.class, box)) {
            if (other == sp) continue;
            if (other.isCreative() || other.isSpectator()) continue;
            Vec3 look = other.getLookAngle().normalize();
            Vec3 toMe = meEye.subtract(other.getEyePosition()).normalize();
            double dot = look.dot(toMe);
            if (dot < 0.92) continue;
            other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false, true));
            applyBloodInfection(other, 200);
        }
    }

    /**
     * Blood explosion a cada 40s — pulse massivo raio 12 com Wither II +
     * Poison III + Blood Infection longa. Visual: 3 explosion emitters + 80
     * dust particles.
     */
    private static void applyBloodExplosion(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel sl)) return;
        sl.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 2.5F, 0.5F);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                sp.getX(), sp.getY() + 1.0, sp.getZ(),
                3, 0.5, 0.5, 0.5, 0.0);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                sp.getX(), sp.getY() + 1.0, sp.getZ(),
                80, 3.0, 2.0, 3.0, 0.3);

        AABB box = new AABB(sp.blockPosition()).inflate(12.0);
        for (Player other : sl.getEntitiesOfClass(Player.class, box)) {
            if (other == sp) continue;
            if (other.isCreative() || other.isSpectator()) continue;
            other.hurt(sp.damageSources().magic(), 8.0f);
            other.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1, true, false, true));
            other.addEffect(new MobEffectInstance(MobEffects.POISON, 300, 2, true, false, true));
            other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2, true, false, true));
            applyBloodInfection(other, 600);
        }
        sp.displayClientMessage(
                Component.literal("§4§l☠ EXPLOSÃO DE SANGUE ☠"), true);
    }

    private static void applyBloodInfection(Player p, int durationTicks) {
        if (ModEffects.BLOOD_INFECTION.get() != null) {
            p.addEffect(new MobEffectInstance(
                    ModEffects.BLOOD_INFECTION.get(),
                    durationTicks, 0, true, false, true));
        }
    }

    /**
     * Panic mode (HP ≤ 25%): aplica Regen V continuo + teleporta players a
     * ≤ 6 blocos pra ~20 blocos longe + Slowness IV + Hunger III. Repete a
     * cada tick enquanto HP baixo (não tem cooldown — é defesa contínua).
     */
    private static void applyPanicMode(ServerPlayer sp) {
        float hpPct = sp.getHealth() / sp.getMaxHealth();
        if (hpPct > PANIC_HP_THRESHOLD) return;
        if (!(sp.level() instanceof ServerLevel sl)) return;

        // Regen V refresh a cada 20t (1s) enquanto em panic
        if (sp.tickCount % 20 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                    60, 4, true, false, true)); // amp 4 = Regen V (2.5 HP/s)
        }

        // Teleport players próximos a cada 20t (1s)
        if (sp.tickCount % 20 != 0) return;

        AABB nearBox = new AABB(sp.blockPosition()).inflate(6.0);
        for (Player other : sl.getEntitiesOfClass(Player.class, nearBox)) {
            if (other == sp) continue;
            if (other.isCreative() || other.isSpectator()) continue;
            teleportAway(sl, sp, other);
        }
    }

    /** Teleporta o player pra um spot random a ~20 blocos de distância do dono. */
    private static void teleportAway(ServerLevel sl, ServerPlayer source, Player target) {
        Vec3 away = target.position().subtract(source.position());
        if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
        else away = away.normalize();
        double distance = 18.0 + sl.random.nextDouble() * 6.0;
        double tx = source.getX() + away.x * distance;
        double tz = source.getZ() + away.z * distance;
        double ty = target.getY();
        sl.sendParticles(ParticleTypes.PORTAL,
                target.getX(), target.getY() + 1.0, target.getZ(),
                30, 0.5, 0.8, 0.5, 0.2);
        target.teleportTo(tx, ty, tz);
        sl.sendParticles(ParticleTypes.PORTAL, tx, ty + 1.0, tz,
                30, 0.5, 0.8, 0.5, 0.2);
        sl.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0F, 0.7F);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, true, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 2, true, false, true));
    }

    // ─────────────────── bossbar ───────────────────

    /**
     * v0.1.22 r2: usa BarState com debounce — bar só some após 60 ticks
     * consecutivos sem crown na mainhand. Fix user bug "bossbar some após
     * primeiro hit".
     */
    private static void updateBossBar(ServerPlayer sp, boolean show, ItemStack activeCrown) {
        UUID id = sp.getUUID();
        BarState st = BOSS_BARS.get(id);

        if (!show) {
            if (st == null) return;
            st.notInHandTicks++;
            if (st.notInHandTicks >= BAR_REMOVE_GRACE) {
                LiberthiaMod.LOGGER.info("[BossCrown bar] REMOVE for {} (no active crown after grace)",
                        sp.getName().getString());
                st.bar.removeAllPlayers();
                BOSS_BARS.remove(id);
            }
            return;
        }

        // Custom name lido do stack ATIVO (qualquer slot). Fallback nome do player.
        String custom = activeCrown != null && !activeCrown.isEmpty()
                ? BossCrownItem.getBossBarName(activeCrown)
                : "";
        Component title = custom.isEmpty()
                ? Component.literal("§4§l⊕ ").append(sp.getName()).append(Component.literal(" §c§l⊕"))
                : Component.literal(custom);

        if (st == null) {
            st = new BarState();
            st.bar = new ServerBossEvent(title,
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.NOTCHED_20);
            st.bar.setDarkenScreen(false);
            st.bar.setPlayBossMusic(false);
            st.bar.setCreateWorldFog(false);
            st.bar.setVisible(true); // garantia explícita
            BOSS_BARS.put(id, st);
            LiberthiaMod.LOGGER.info("[BossCrown bar] CREATE for {} title='{}'",
                    sp.getName().getString(), title.getString());
        } else {
            st.bar.setName(title);
        }
        st.notInHandTicks = 0;

        float pct = Math.max(0f, Math.min(1f, sp.getHealth() / sp.getMaxHealth()));
        st.bar.setProgress(pct);

        // v0.1.22 r7: adicionar o PRÓPRIO dono primeiro (forma idempotente —
        // re-add é no-op se já é viewer). Depois itera level.players() pra os
        // demais. Antes só iterava level.players() — em alguns casos o próprio
        // player não estava na lista durante o tick (ex: respawn, dimension shift).
        st.bar.addPlayer(sp);
        if (sp.level() instanceof ServerLevel sl) {
            for (ServerPlayer viewer : sl.players()) {
                if (viewer == sp) continue;
                st.bar.addPlayer(viewer);
            }
        }
        // Log periódico pra confirmar funcionamento
        if (sp.tickCount % 100 == 0) {
            LiberthiaMod.LOGGER.info("[BossCrown bar] tick {} hp={}/{} progress={} viewers={}",
                    sp.getName().getString(),
                    String.format("%.1f", sp.getHealth()),
                    String.format("%.1f", sp.getMaxHealth()),
                    String.format("%.3f", st.bar.getProgress()),
                    st.bar.getPlayers().size());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        BarState st = BOSS_BARS.remove(sp.getUUID());
        if (st != null) st.bar.removeAllPlayers();
    }

    @SubscribeEvent
    public static void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        BarState st = BOSS_BARS.remove(sp.getUUID());
        if (st != null) st.bar.removeAllPlayers();
    }
}
