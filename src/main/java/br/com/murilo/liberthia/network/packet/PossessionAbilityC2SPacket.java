package br.com.murilo.liberthia.network.packet;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.event.PossessionManager;
import br.com.murilo.liberthia.event.PossessionSession;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * v0.1.22 r30: C2S — possessor pressiona tecla "ability" (G) → server executa
 * habilidade específica baseada no tipo do mob possuído.
 *
 * <h2>Habilidades por mob</h2>
 * <ul>
 *   <li><b>Creeper</b> → ignita (countdown 30 ticks pra explosão; 2ª pressão cancela)</li>
 *   <li><b>Enderman</b> → teleporta 16 blocos na direção do olhar</li>
 *   <li><b>Ghast</b> → atira fireball grande</li>
 *   <li><b>Blaze</b> → 3 small fireballs em rajada</li>
 *   <li><b>Skeleton</b> → atira flecha (sem custo de inventário)</li>
 *   <li><b>Wolf</b> → mordida + Bleeding 5s + speed I</li>
 *   <li><b>Spider</b> → pula 3 blocos pra frente</li>
 *   <li><b>Phantom</b> → mergulho rápido</li>
 *   <li><b>Witch</b> → joga potion de Harming</li>
 *   <li><b>Warden</b> → sonic boom no alvo olhado (10 dano + knockback)</li>
 *   <li><b>Wither</b> → wither skull em projétil</li>
 *   <li><b>Bee</b> → sting + poison no alvo olhado</li>
 *   <li><b>Default LivingEntity</b> → rugido + intimidação (slow IIs nos próximos)</li>
 * </ul>
 *
 * <h2>Cooldown</h2>
 * 2s por habilidade. Registrado por possessor.
 */
public class PossessionAbilityC2SPacket {

    private static final java.util.Map<java.util.UUID, Long> COOLDOWNS = new java.util.HashMap<>();
    private static final long COOLDOWN_TICKS = 40;

    public PossessionAbilityC2SPacket() {}

    public static void encode(PossessionAbilityC2SPacket pkt, FriendlyByteBuf buf) {}
    public static PossessionAbilityC2SPacket decode(FriendlyByteBuf buf) {
        return new PossessionAbilityC2SPacket();
    }

    public static void handle(PossessionAbilityC2SPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            PossessionSession session = PossessionManager.getSession(sender.getUUID());
            if (session == null) return;

            long now = sender.level().getGameTime();
            Long last = COOLDOWNS.get(sender.getUUID());
            if (last != null && now - last < COOLDOWN_TICKS) {
                sender.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§7Habilidade em cooldown..."), true);
                return;
            }

            Entity target = session.resolveTarget(sender.server);
            if (!(target instanceof LivingEntity le)) return;
            if (!(le.level() instanceof ServerLevel sl)) return;

            boolean ok = executeAbility(le, sl);
            if (ok) {
                COOLDOWNS.put(sender.getUUID(), now);
            }
        });
        context.setPacketHandled(true);
    }

    private static boolean executeAbility(LivingEntity le, ServerLevel sl) {
        try {
            // === CREEPER ===
            if (le instanceof Creeper creeper) {
                if (creeper.getSwellDir() > 0) {
                    creeper.setSwellDir(-1); // cancela
                    sl.playSound(null, creeper.blockPosition(), SoundEvents.CREEPER_PRIMED,
                            SoundSource.HOSTILE, 0.5F, 1.5F);
                } else {
                    creeper.ignite();
                    sl.playSound(null, creeper.blockPosition(), SoundEvents.CREEPER_PRIMED,
                            SoundSource.HOSTILE, 1.0F, 0.5F);
                }
                return true;
            }
            // === ENDERMAN — teleporta na direção do olhar ===
            if (le instanceof EnderMan em) {
                Vec3 look = em.getLookAngle();
                double tx = em.getX() + look.x * 16;
                double ty = em.getY() + look.y * 16;
                double tz = em.getZ() + look.z * 16;
                em.teleportTo(tx, ty, tz);
                sl.sendParticles(ParticleTypes.PORTAL,
                        em.getX(), em.getY() + 1, em.getZ(), 30, 0.5, 1, 0.5, 0.2);
                sl.playSound(null, em.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.HOSTILE, 1.0F, 1.0F);
                return true;
            }
            // === GHAST — fireball ===
            if (le instanceof Ghast ghast) {
                Vec3 look = ghast.getLookAngle().scale(16);
                LargeFireball fb = new LargeFireball(sl, ghast,
                        look.x, look.y, look.z, 2);
                fb.setPos(ghast.getX() + look.x * 0.1, ghast.getY() + 1,
                        ghast.getZ() + look.z * 0.1);
                sl.addFreshEntity(fb);
                sl.playSound(null, ghast.blockPosition(), SoundEvents.GHAST_SHOOT,
                        SoundSource.HOSTILE, 1.0F, 1.0F);
                return true;
            }
            // === BLAZE — 3 fireballs ===
            if (le instanceof Blaze blaze) {
                Vec3 look = blaze.getLookAngle();
                for (int i = -1; i <= 1; i++) {
                    SmallFireball fb = new SmallFireball(sl, blaze,
                            look.x + i * 0.1, look.y, look.z + i * 0.1);
                    fb.setPos(blaze.getX(), blaze.getEyeY() - 0.5, blaze.getZ());
                    sl.addFreshEntity(fb);
                }
                sl.playSound(null, blaze.blockPosition(), SoundEvents.BLAZE_SHOOT,
                        SoundSource.HOSTILE, 1.5F, 1.0F);
                return true;
            }
            // === SKELETON — flecha ===
            if (le instanceof Skeleton skel) {
                Vec3 look = skel.getLookAngle();
                AbstractArrow arrow = new net.minecraft.world.entity.projectile.Arrow(sl, skel);
                arrow.setPos(skel.getX(), skel.getEyeY(), skel.getZ());
                arrow.shoot(look.x, look.y + 0.1, look.z, 2.5F, 1F);
                sl.addFreshEntity(arrow);
                sl.playSound(null, skel.blockPosition(), SoundEvents.SKELETON_SHOOT,
                        SoundSource.HOSTILE, 1.0F, 1.0F);
                return true;
            }
            // === WOLF — bite forward ===
            if (le instanceof Wolf wolf) {
                Vec3 look = wolf.getLookAngle();
                AABB box = wolf.getBoundingBox().inflate(2);
                for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != wolf && e.isAlive())) {
                    t.hurt(wolf.damageSources().mobAttack(wolf), 4.0F);
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                    wolf.heal(2);
                }
                wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
                sl.playSound(null, wolf.blockPosition(), SoundEvents.WOLF_GROWL,
                        SoundSource.HOSTILE, 1.5F, 1.0F);
                return true;
            }
            // === SPIDER — jump forward ===
            if (le instanceof Spider spider) {
                Vec3 look = spider.getLookAngle();
                spider.setDeltaMovement(look.x * 1.5, 0.6, look.z * 1.5);
                spider.hasImpulse = true;
                sl.playSound(null, spider.blockPosition(), SoundEvents.SPIDER_AMBIENT,
                        SoundSource.HOSTILE, 1.0F, 1.5F);
                return true;
            }
            // === PHANTOM — dive ===
            if (le instanceof Phantom phantom) {
                Vec3 look = phantom.getLookAngle();
                phantom.setDeltaMovement(look.x * 2, look.y * 2, look.z * 2);
                phantom.hasImpulse = true;
                sl.playSound(null, phantom.blockPosition(), SoundEvents.PHANTOM_SWOOP,
                        SoundSource.HOSTILE, 1.0F, 1.0F);
                return true;
            }
            // === WITCH — throw harming potion ===
            if (le instanceof Witch witch) {
                Vec3 look = witch.getLookAngle();
                ThrownPotion pot = new ThrownPotion(sl, witch);
                pot.setItem(PotionUtils.setPotion(new net.minecraft.world.item.ItemStack(
                        Items.SPLASH_POTION), Potions.STRONG_HARMING));
                pot.setPos(witch.getX(), witch.getEyeY(), witch.getZ());
                pot.shoot(look.x, look.y + 0.2, look.z, 0.75F, 0.5F);
                sl.addFreshEntity(pot);
                sl.playSound(null, witch.blockPosition(), SoundEvents.WITCH_THROW,
                        SoundSource.HOSTILE, 1.0F, 1.0F);
                return true;
            }
            // === WARDEN — sonic boom ===
            if (le instanceof Warden warden) {
                Vec3 look = warden.getLookAngle().scale(20);
                Vec3 origin = warden.position().add(0, 1.6, 0);
                Vec3 endPos = origin.add(look);
                HitResult hit = sl.clip(new net.minecraft.world.level.ClipContext(origin, endPos,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, warden));
                AABB box = new AABB(origin, endPos).inflate(2);
                int hit_count = 0;
                for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != warden && e.isAlive())) {
                    Vec3 toTarget = t.position().subtract(origin).normalize();
                    if (toTarget.dot(look.normalize()) > 0.7) {
                        t.hurt(warden.damageSources().sonicBoom(warden), 10.0F);
                        Vec3 push = look.normalize().scale(2);
                        t.setDeltaMovement(push.x, 0.5, push.z);
                        t.hurtMarked = true;
                        hit_count++;
                    }
                }
                sl.sendParticles(ParticleTypes.SONIC_BOOM,
                        warden.getX(), warden.getEyeY(), warden.getZ(), 1, 0, 0, 0, 0);
                sl.playSound(null, warden.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                        SoundSource.HOSTILE, 3.0F, 1.0F);
                return true;
            }
            // === WITHER — wither skull ===
            if (le instanceof WitherBoss wither) {
                Vec3 look = wither.getLookAngle();
                net.minecraft.world.entity.projectile.WitherSkull skull =
                        new net.minecraft.world.entity.projectile.WitherSkull(sl, wither,
                                look.x, look.y, look.z);
                skull.setPos(wither.getX() + look.x, wither.getEyeY() - 0.5,
                        wither.getZ() + look.z);
                sl.addFreshEntity(skull);
                sl.playSound(null, wither.blockPosition(), SoundEvents.WITHER_SHOOT,
                        SoundSource.HOSTILE, 1.5F, 1.0F);
                return true;
            }
            // === BEE — sting ===
            if (le instanceof Bee bee) {
                AABB box = bee.getBoundingBox().inflate(2);
                for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != bee && e.isAlive())) {
                    t.hurt(bee.damageSources().mobAttack(bee), 2.0F);
                    t.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
                }
                sl.playSound(null, bee.blockPosition(), SoundEvents.BEE_STING,
                        SoundSource.HOSTILE, 1.0F, 1.0F);
                return true;
            }
            // === Default LivingEntity — rugido + slow nos próximos ===
            AABB box = le.getBoundingBox().inflate(8);
            for (LivingEntity other : sl.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != le && e.isAlive())) {
                other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            }
            sl.playSound(null, le.blockPosition(), SoundEvents.HOSTILE_HURT,
                    SoundSource.HOSTILE, 2.0F, 0.5F);
            sl.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    le.getX(), le.getEyeY(), le.getZ(), 8, 0.3, 0.2, 0.3, 0);
            return true;
        } catch (Throwable t) {
            LiberthiaMod.LOGGER.warn("[Possession Ability] failed: {}", t.toString());
            return false;
        }
    }
}
