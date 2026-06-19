package br.com.murilo.liberthia.cosmic.artifacts;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.cosmic.sound.CosmicSoundManager;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.1.22 r56: Event handlers passivos pros 15 cosmic artifacts.
 *
 * <h2>Triggers</h2>
 * <ul>
 *   <li><b>PulledString</b>: LivingAttackEvent — knockback reverso</li>
 *   <li><b>QuietMark</b>: LivingHurtEvent — dano +50% se target marked</li>
 *   <li><b>LonelyEcho</b>: PlayerTickEvent — som fake atrás de quem te olha</li>
 *   <li><b>SoftWound</b>: LivingAttackEvent — agendar dano triplo em 3s</li>
 *   <li><b>LookingGlass</b>: ProjectileImpactEvent — refletir projétil</li>
 *   <li><b>HalfStep</b>: LivingAttackEvent — cancela 1º hit (cd)</li>
 *   <li><b>BentIron</b>: LivingHurtEvent — degrada arma do atacante</li>
 *   <li><b>PaleCoin</b>: LivingDeathEvent — drop +XP ao morrer</li>
 *   <li><b>SunkenRing</b>: PlayerTickEvent — water breathing + slow fall</li>
 *   <li><b>HandOnGlass</b>: LivingDropsEvent — preserva 1 item</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class CosmicArtifactsR56Events {

    /** Cooldown do HalfStep — player UUID → next tick. */
    private static final Map<UUID, Long> HALF_STEP_CD = new HashMap<>();
    /** Cooldown do Echo — player UUID → next tick. */
    private static final Map<UUID, Long> ECHO_CD = new HashMap<>();

    private CosmicArtifactsR56Events() {}

    /** Helper: tem o item em alguma slot do inventário? */
    private static boolean has(Player p, net.minecraft.world.item.Item item) {
        for (ItemStack s : p.getInventory().items) if (s.getItem() == item) return true;
        for (ItemStack s : p.getInventory().armor) if (s.getItem() == item) return true;
        if (p.getOffhandItem().getItem() == item) return true;
        return false;
    }

    // ───────── PulledString ─────────
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (!has(target, ModItems.PULLED_STRING.get())) return;
        var src = event.getSource();
        if (src == null || !(src.getEntity() instanceof LivingEntity attacker)) return;
        if (attacker == target) return;
        // Pull attacker closer (in reverse — they come TO the player)
        Vec3 dirToPlayer = target.position().subtract(attacker.position()).normalize().scale(0.6);
        attacker.setDeltaMovement(dirToPlayer.x, 0.2, dirToPlayer.z);
        attacker.hurtMarked = true;
        if (target.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SQUID_INK,
                    attacker.getX(), attacker.getY() + 1, attacker.getZ(),
                    10, 0.2, 0.4, 0.2, 0.05);
        }
    }

    // ───────── QuietMark damage bonus ─────────
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onHurtMark(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (!victim.getPersistentData().contains("liberthia.quiet_marked_until")) return;
        long until = victim.getPersistentData().getLong("liberthia.quiet_marked_until");
        if (victim.tickCount > until) {
            victim.getPersistentData().remove("liberthia.quiet_marked_until");
            return;
        }
        // +50% dano enquanto marcado
        event.setAmount(event.getAmount() * 1.5F);
    }

    // ───────── BentIron — atacante perde durability ─────────
    @SubscribeEvent
    public static void onBentIron(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (!has(target, ModItems.BENT_IRON.get())) return;
        var src = event.getSource();
        if (src == null || !(src.getEntity() instanceof LivingEntity attacker)) return;
        // FIX (#47): antes só tirava 2 de durabilidade (50%) — imperceptível.
        // Agora SEMPRE aplica Weakness no atacante (você VÊ as partículas) e morde
        // bem mais a durabilidade da arma dele ("armas em volta ficam cansadas").
        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true));
        ItemStack mainHand = attacker.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
            int chip = Math.max(6, mainHand.getMaxDamage() / 16); // ~6% ou mín 6
            mainHand.setDamageValue(Math.min(mainHand.getMaxDamage() - 1,
                    mainHand.getDamageValue() + chip));
        }
        if (target.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    attacker.getX(), attacker.getY() + 1, attacker.getZ(),
                    12, 0.2, 0.3, 0.2, 0.02);
        }
    }

    // ───────── HalfStep — primeira esquiva ─────────
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHalfStep(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!has(p, ModItems.HALF_STEP.get())) return;
        UUID id = p.getUUID();
        Long cd = HALF_STEP_CD.get(id);
        if (cd != null && p.tickCount < cd) return;
        HALF_STEP_CD.put(id, (long)p.tickCount + 12000); // 10 min cd
        event.setCanceled(true);
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.PORTAL,
                    p.getX(), p.getY() + 1, p.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
        }
        if (p instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(
                    "§5§o✦ você não estava ali."), true);
        }
    }

    // ───────── PaleCoin — XP boost on death ─────────
    @SubscribeEvent
    public static void onPaleCoin(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!has(p, ModItems.PALE_COIN.get())) return;
        // Boost XP que o player dropa ao morrer
        if (p.level() instanceof ServerLevel sl) {
            // Spawn XP orbs adicionais
            int xpAmount = p.totalExperience + 50;
            net.minecraft.world.entity.ExperienceOrb orb = new net.minecraft.world.entity.ExperienceOrb(
                    sl, p.getX(), p.getY(), p.getZ(), xpAmount);
            sl.addFreshEntity(orb);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 1, 0.5, 0.1);
        }
    }

    // ───────── LookingGlass — reflect projectile ─────────
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        var hit = event.getRayTraceResult();
        if (!(hit instanceof net.minecraft.world.phys.EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof Player target)) return;
        if (!has(target, ModItems.LOOKING_GLASS.get())) return;

        Projectile proj = event.getProjectile();
        // Inverte velocidade
        Vec3 v = proj.getDeltaMovement();
        proj.setDeltaMovement(v.scale(-1.2));
        proj.setOwner(target);
        event.setCanceled(true);
        if (target.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    proj.getX(), proj.getY(), proj.getZ(), 12, 0.2, 0.2, 0.2, 0.05);
        }
    }

    // ───────── SunkenRing — water breathing + slow fall ─────────
    @SubscribeEvent
    public static void onSunkenRing(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 40 != 0) return;
        if (!has(sp, ModItems.SUNKEN_RING.get())) return;
        sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 80, 0, true, false));
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false));
    }

    // ───────── LonelyEcho — som fake atrás de quem te olha ─────────
    @SubscribeEvent
    public static void onEcho(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer owner)) return;
        if (owner.tickCount % 40 != 0) return; // 2s
        if (!has(owner, ModItems.LONELY_ECHO.get())) return;

        UUID id = owner.getUUID();
        Long cd = ECHO_CD.get(id);
        if (cd != null && owner.tickCount < cd) return;

        if (!(owner.level() instanceof ServerLevel level)) return;
        for (Player nearby : level.players()) {
            if (nearby == owner) continue;
            if (!(nearby instanceof ServerPlayer obs)) continue;
            if (obs.distanceTo(owner) > 20) continue;
            Vec3 toOwner = owner.position().subtract(obs.position()).normalize();
            Vec3 look = obs.getLookAngle();
            if (toOwner.dot(look) > 0.93) {
                // Player obs está olhando pra owner — toca passos fake atrás de obs
                CosmicSoundManager.playFalseFootsteps(obs, true);
                ECHO_CD.put(id, (long)owner.tickCount + 200); // 10s cd
                break;
            }
        }
    }

    // ───────── HandOnGlass — preserva 1 item (best one) ao morrer ─────────
    @SubscribeEvent
    public static void onHandOnGlass(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!has(p, ModItems.HAND_ON_GLASS.get())) return;
        // Acha o item mais "valioso" (mais durability/maior stack) e remove do drops
        net.minecraft.world.entity.item.ItemEntity best = null;
        int bestScore = -1;
        for (var ie : event.getDrops()) {
            ItemStack s = ie.getItem();
            int score = s.getCount() + (s.isDamageableItem() ? s.getMaxDamage() : 0);
            if (score > bestScore) {
                bestScore = score;
                best = ie;
            }
        }
        if (best != null) {
            ItemStack preserved = best.getItem().copy();
            event.getDrops().remove(best);
            // Salva no NBT do player pra restore on respawn
            p.getPersistentData().put("liberthia.hand_on_glass_preserve",
                    preserved.save(new net.minecraft.nbt.CompoundTag()));
        }
    }

    @SubscribeEvent
    public static void onHandOnGlassRespawn(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        Player oldP = event.getOriginal();
        Player newP = event.getEntity();
        if (oldP.getPersistentData().contains("liberthia.hand_on_glass_preserve")) {
            ItemStack preserved = ItemStack.of(
                    oldP.getPersistentData().getCompound("liberthia.hand_on_glass_preserve"));
            if (!preserved.isEmpty()) {
                if (!newP.getInventory().add(preserved)) {
                    newP.drop(preserved, false);
                }
                if (newP instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.literal(
                            "§5§o✦ Uma mão te entrega algo de volta."), false);
                }
            }
            oldP.getPersistentData().remove("liberthia.hand_on_glass_preserve");
        }
    }

    // ───────── Cleanup despawn Wood creatures ─────────
    @SubscribeEvent
    public static void onWoodCreatureTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;
        if (sl.getGameTime() % 40 != 0) return;
        for (var ent : sl.getAllEntities()) {
            if (!ent.getPersistentData().getBoolean("liberthia.wood_creature")) continue;
            long despawnAt = ent.getPersistentData().getLong("liberthia.wood_despawn_at");
            if (sl.getGameTime() >= despawnAt - 100000 /* sane bound */
                    && (despawnAt < ent.tickCount || ent.tickCount > 800)) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        ent.getX(), ent.getY() + 1, ent.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
                ent.discard();
            }
        }
    }

    // ───────── Cleanup Marrow Whistle skel despawn ─────────
    @SubscribeEvent
    public static void onMarrowTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;
        if (sl.getGameTime() % 100 != 0) return;
        for (var ent : sl.getAllEntities()) {
            if (!ent.getPersistentData().hasUUID("liberthia.marrow_master")) continue;
            long despawnAt = ent.getPersistentData().getLong("liberthia.marrow_despawn_at");
            if (sl.getGameTime() >= despawnAt) {
                sl.sendParticles(ParticleTypes.SOUL,
                        ent.getX(), ent.getY() + 1, ent.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
                ent.discard();
            }
        }
    }
}
