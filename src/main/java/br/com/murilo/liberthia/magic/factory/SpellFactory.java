package br.com.murilo.liberthia.magic.factory;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import br.com.murilo.liberthia.magic.spell.CastContext;
import br.com.murilo.liberthia.magic.spell.MagicSounds;
import br.com.murilo.liberthia.magic.spell.SpellDef;
import br.com.murilo.liberthia.magic.spell.SpellProjectileEntity;
import br.com.murilo.liberthia.magic.spell.particle.SpellTrailParticleData;
import br.com.murilo.liberthia.magic.spell.vfx.ScheduledVfx;
import br.com.murilo.liberthia.particle.engine.ConfigurableParticleOptions;
import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * r148: <b>SpellFactory</b> — converte {@link SpellRecipe} (declarativo) em
 * {@link br.com.murilo.liberthia.magic.spell.SpellCast} (executável).
 *
 * <p>Esta é a peça-chave do sistema: cada {@link SpellType} tem um handler
 * que sabe traduzir os params de {@link SpellBehavior} + {@link SpellVfxProfile}
 * + {@link SpellEffectSpec} em ações concretas no mundo (spawn projétil,
 * apply effect, particles, sons).
 *
 * <h2>Patterns reusados</h2>
 * <ul>
 *   <li>{@link SpellProjectileEntity} pros tipos PROJECTILE/HOMING/MULTI_SHOT/ARC</li>
 *   <li>{@link ScheduledVfx} pra AURA, NOVA, BEAM, RAIN (efeitos sustentados)</li>
 *   <li>{@link MagicSounds} pra áudio por escola</li>
 *   <li>{@link SchoolDamageSource} pra damage source customizado</li>
 * </ul>
 */
public final class SpellFactory {

    private SpellFactory() {}

    /** Constrói um SpellDef pronto pra uso a partir de uma recipe. */
    public static SpellDef toSpellDef(SpellRecipe r) {
        return SpellDef.builder(r.id)
                .name(r.name)
                .school(r.school)
                .rarity(r.rarity)
                .mana(r.mana)
                .cooldown(r.cooldown)
                .damage(r.damage)
                .range(r.range)
                .lore(r.lore.isEmpty() ? "Feitiço da Spell Factory" : r.lore)
                .cast(toCastLambda(r))
                .build();
    }

    /** Constrói a lambda de cast — dispatch por SpellType. */
    public static br.com.murilo.liberthia.magic.spell.SpellCast toCastLambda(SpellRecipe r) {
        return ctx -> {
            switch (r.type) {
                case PROJECTILE -> castProjectile(r, ctx, false);
                case HOMING     -> castProjectile(r, ctx, true);
                case MULTI_SHOT -> castMultiShot(r, ctx);
                case ARC        -> castProjectile(r, ctx, false); // gravity handled by entity
                case BEAM       -> castBeam(r, ctx);
                case EXPLOSION  -> castExplosion(r, ctx);
                case NOVA       -> castNova(r, ctx);
                case CONE       -> castCone(r, ctx);
                case RAIN       -> castRain(r, ctx);
                case AURA       -> castAura(r, ctx);
                case TOUCH      -> castTouch(r, ctx);
                case SELF       -> castSelf(r, ctx);
                case DASH       -> castDash(r, ctx);
            }
            return true;
        };
    }

    // ═══════════════════════════════════════════════════════════════
    //  PROJECTILE-like
    // ═══════════════════════════════════════════════════════════════

    /** r150: spawn position = posição da MÃO dominante do player. Vem do CastContext. */
    private static Vec3 spawnPos(ServerPlayer p) {
        // Calcula offset lateral (right hand) baseado no yaw
        float yaw = p.getYRot();
        double yawRad = Math.toRadians(yaw);
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);
        double frontX = -Math.sin(yawRad);
        double frontZ = Math.cos(yawRad);
        // Lado dominante: RIGHT = positivo lateral, LEFT = negativo
        boolean rightHand = p.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        double lateral = rightHand ? 0.35 : -0.35;
        double front = 0.5;
        return new Vec3(
                p.getX() + rightX * lateral + frontX * front,
                p.getEyeY() - 0.25, // ligeiramente abaixo do olho — não bloqueia view
                p.getZ() + rightZ * lateral + frontZ * front);
    }

    private static void castProjectile(SpellRecipe r, CastContext ctx, boolean homing) {
        ServerPlayer p = ctx.caster;
        ServerLevel sl = ctx.level;
        Vec3 look = p.getLookAngle();
        Vec3 motion = look.scale(r.behavior.speed);

        SpellProjectileEntity proj = new SpellProjectileEntity(sl, p, motion, r.school, r.damage);
        proj.withConfig(r.behavior.lifetimeTicks, r.behavior.pierce, r.behavior.aoeRadius, r.behavior.igniteBlocks);
        // r150: spawn na MÃO (não na cara) — não obstrui visão do player
        Vec3 from = spawnPos(p);
        proj.setPos(from.x, from.y, from.z);
        sl.addFreshEntity(proj);

        applyVfxOverridesToProjectile(proj, r);
    }

    private static void castMultiShot(SpellRecipe r, CastContext ctx) {
        ServerPlayer p = ctx.caster;
        ServerLevel sl = ctx.level;
        Vec3 look = p.getLookAngle();
        Vec3 from = spawnPos(p);

        int count = Math.max(1, r.behavior.projectileCount);
        float spread = r.behavior.spreadDegrees;

        for (int i = 0; i < count; i++) {
            float angleOffset = count == 1 ? 0F : -spread / 2 + spread * i / (count - 1);
            Vec3 rotated = rotateYaw(look, angleOffset);
            Vec3 motion = rotated.scale(r.behavior.speed);

            SpellProjectileEntity proj = new SpellProjectileEntity(sl, p, motion, r.school,
                    r.damage / count * 1.5F);
            proj.withConfig(r.behavior.lifetimeTicks, r.behavior.pierce, r.behavior.aoeRadius, r.behavior.igniteBlocks);
            proj.setPos(from.x, from.y, from.z);
            sl.addFreshEntity(proj);
        }
    }

    private static Vec3 rotateYaw(Vec3 v, float degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    // ═══════════════════════════════════════════════════════════════
    //  BEAM
    // ═══════════════════════════════════════════════════════════════

    private static void castBeam(SpellRecipe r, CastContext ctx) {
        ServerPlayer p = ctx.caster;
        ServerLevel sl = ctx.level;
        // r150: beam sai da MÃO em vez da cara
        Vec3 from = spawnPos(p);
        Vec3 dir = p.getLookAngle();
        Vec3 to = from.add(dir.scale(r.behavior.beamRange));

        // Raycast pra achar onde o beam para
        HitResult hit = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 endPoint = hit.getLocation();

        // VFX sustentado entre from→endPoint
        ScheduledVfx.spawnBeam(sl, from, endPoint, r.school, r.behavior.beamMaxTicks);

        // r157 FIX: damage TODAS entidades dentro do caminho do beam (não só
        // crosshair target). Constrói uma AABB cobrindo o segmento, depois
        // filtra por distância perpendicular ao raio (thickness = 1.0 bloco).
        Vec3 actualEnd = endPoint; // beam para no bloco; só hit antes disso
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(from, actualEnd).inflate(1.0);
        Vec3 beamVec = actualEnd.subtract(from);
        double beamLenSq = beamVec.lengthSqr();
        if (beamLenSq < 1e-6) beamLenSq = 1.0;
        double thickness = Math.max(0.6, r.behavior.beamRange > 0 ? 1.0 : 0.6);
        java.util.List<LivingEntity> hits = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != p && e.isAlive() && !e.isSpectator());
        for (LivingEntity target : hits) {
            // Projeta target.position sobre o segmento; calcula distância perpendicular
            Vec3 rel = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from);
            double t = rel.dot(beamVec) / beamLenSq;
            if (t < 0 || t > 1) continue; // fora do segmento
            Vec3 closest = from.add(beamVec.scale(t));
            double dist = target.position().add(0, target.getBbHeight() * 0.5, 0).distanceTo(closest);
            if (dist <= thickness) {
                applyDamageAndEffects(r, target, p, sl);
            }
        }
        playImpactSound(r, sl, endPoint);
    }

    // ═══════════════════════════════════════════════════════════════
    //  EXPLOSION (instant AOE at crosshair)
    // ═══════════════════════════════════════════════════════════════

    private static void castExplosion(SpellRecipe r, CastContext ctx) {
        ServerPlayer p = ctx.caster;
        ServerLevel sl = ctx.level;
        Vec3 from = new Vec3(p.getX(), p.getEyeY(), p.getZ());
        Vec3 dir = p.getLookAngle();
        Vec3 to = from.add(dir.scale(r.range));
        HitResult hit = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 center = hit.getLocation();

        applyAoeDamage(r, sl, center, r.behavior.radius, p);
        spawnImpactBurst(r, sl, center);
        playImpactSound(r, sl, center);
    }

    // ═══════════════════════════════════════════════════════════════
    //  NOVA (expanding wave from caster)
    // ═══════════════════════════════════════════════════════════════

    private static void castNova(SpellRecipe r, CastContext ctx) {
        ServerPlayer p = ctx.caster;
        ServerLevel sl = ctx.level;
        Vec3 center = p.position();

        applyAoeDamage(r, sl, center, r.behavior.radius, p);

        // VFX: ground decal expandindo
        ScheduledVfx.spawnGroundDecal(sl, center, r.school, r.behavior.radius, 25);
        spawnImpactBurst(r, sl, center);
        playImpactSound(r, sl, center);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONE (frontal cone)
    // ═══════════════════════════════════════════════════════════════

    private static void castCone(SpellRecipe r, CastContext ctx) {
        ServerPlayer p = ctx.caster;
        ServerLevel sl = ctx.level;
        Vec3 origin = p.position().add(0, p.getEyeHeight() * 0.5, 0);
        Vec3 dir = p.getLookAngle();

        float radius = r.behavior.radius;
        float halfAngleRad = (float) Math.toRadians(r.behavior.coneAngle / 2);
        float cosHalfAngle = (float) Math.cos(halfAngleRad);

        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().inflate(radius))) {
            if (e == p) continue;
            Vec3 toEntity = e.position().subtract(origin).normalize();
            double dot = toEntity.dot(dir);
            if (dot >= cosHalfAngle) {
                applyDamageAndEffects(r, e, p, sl);
            }
        }

        // VFX: spray de partículas em cone
        spawnConeParticles(r, sl, origin, dir, radius);
        playImpactSound(r, sl, origin);
    }

    // ═══════════════════════════════════════════════════════════════
    //  RAIN (strikes caindo do céu)
    // ═══════════════════════════════════════════════════════════════

    private static void castRain(SpellRecipe r, CastContext ctx) {
        ServerPlayer p = ctx.caster;
        ServerLevel sl = ctx.level;
        Vec3 from = new Vec3(p.getX(), p.getEyeY(), p.getZ());
        Vec3 dir = p.getLookAngle();
        Vec3 to = from.add(dir.scale(r.range));
        HitResult hit = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 center = hit.getLocation();

        ScheduledVfx.spawnGroundDecal(sl, center, r.school, r.behavior.radius, r.behavior.rainDurationTicks);

        // Agenda N strikes ao longo da duração
        int strikes = r.behavior.rainStrikes;
        int duration = r.behavior.rainDurationTicks;
        int interval = Math.max(1, duration / strikes);

        for (int i = 0; i < strikes; i++) {
            int delay = i * interval;
            ScheduledVfx.schedule(sl, delay, () -> {
                double angle = sl.random.nextDouble() * Math.PI * 2;
                double dist = sl.random.nextDouble() * r.behavior.radius;
                Vec3 strikePos = new Vec3(
                        center.x + Math.cos(angle) * dist,
                        center.y,
                        center.z + Math.sin(angle) * dist);
                applyAoeDamage(r, sl, strikePos, 1.5F, p);
                spawnImpactBurst(r, sl, strikePos);
                playImpactSound(r, sl, strikePos);
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  AURA (sustained around caster)
    // ═══════════════════════════════════════════════════════════════

    private static void castAura(SpellRecipe r, CastContext ctx) {
        ServerPlayer p = ctx.caster;
        ServerLevel sl = ctx.level;
        int duration = r.behavior.auraDurationTicks;
        int interval = r.behavior.auraTickIntervalTicks;
        int pulses = duration / interval;

        ScheduledVfx.spawnAura(sl, p, r.school, r.behavior.radius, duration);

        for (int i = 0; i < pulses; i++) {
            int delay = i * interval;
            ScheduledVfx.schedule(sl, delay, () -> {
                if (!p.isAlive()) return;
                applyAoeDamage(r, sl, p.position(), r.behavior.radius, p);
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  TOUCH (single melee)
    // ═══════════════════════════════════════════════════════════════

    private static void castTouch(SpellRecipe r, CastContext ctx) {
        ServerPlayer p = ctx.caster;
        ServerLevel sl = ctx.level;
        LivingEntity target = ctx.pickTarget(r.behavior.touchRange);
        if (target == null) return;
        applyDamageAndEffects(r, target, p, sl);
        spawnImpactBurst(r, sl, target.position().add(0, target.getBbHeight() * 0.5, 0));
        playImpactSound(r, sl, target.position());
    }

    // ═══════════════════════════════════════════════════════════════
    //  SELF (buff caster)
    // ═══════════════════════════════════════════════════════════════

    private static void castSelf(SpellRecipe r, CastContext ctx) {
        ServerPlayer p = ctx.caster;
        ServerLevel sl = ctx.level;
        // Aplica todos os effects no próprio caster
        for (SpellEffectSpec eff : r.effects) {
            eff.applyTo(p, p, r.damage);
        }
        spawnImpactBurst(r, sl, p.position().add(0, p.getBbHeight() * 0.5, 0));
        playImpactSound(r, sl, p.position());
    }

    // ═══════════════════════════════════════════════════════════════
    //  DASH (move caster)
    // ═══════════════════════════════════════════════════════════════

    private static void castDash(SpellRecipe r, CastContext ctx) {
        ServerPlayer p = ctx.caster;
        ServerLevel sl = ctx.level;
        Vec3 dir = p.getLookAngle();
        // r157: BUFF — dash mais forte (velocidade 4× anterior + ignora gravity drag)
        Vec3 dash = dir.scale(Math.max(2.5F, r.behavior.dashDistance / 3F));
        p.setDeltaMovement(dash.x, Math.max(dash.y, 0.45), dash.z); // garante uplift mínimo
        p.hurtMarked = true;
        p.fallDistance = 0;  // r157: zera fall damage durante dash

        // r157: dash agora ATROPELA inimigos no caminho — dano em AABB do dash
        Vec3 endPos = p.position().add(dir.scale(r.behavior.dashDistance));
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(p.position(), endPos).inflate(1.5);
        java.util.List<LivingEntity> hits = sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != p && e.isAlive());
        for (LivingEntity t : hits) {
            applyDamageAndEffects(r, t, p, sl);
        }

        // Trail visual mais denso
        SpellTrailParticleData trail = new SpellTrailParticleData(r.school, 1.8F, 22);
        sl.sendParticles(trail, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.6, 0.6, 0.08);
        // Spawn trail nos próximos 3 ticks também
        for (int i = 1; i <= 3; i++) {
            final int delay = i * 2;
            ScheduledVfx.schedule(sl, delay, () -> {
                Vec3 pos = p.position();
                sl.sendParticles(trail, pos.x, pos.y + 1, pos.z, 20, 0.5, 0.5, 0.5, 0.05);
            });
        }
        playImpactSound(r, sl, p.position());
    }

    // ═══════════════════════════════════════════════════════════════
    //  Shared helpers
    // ═══════════════════════════════════════════════════════════════

    private static void applyDamageAndEffects(SpellRecipe r, LivingEntity target, LivingEntity caster, ServerLevel sl) {
        float finalDamage = r.damage;
        // r162: Mage Class bonus — multiplica damage + aplica effect class-specific
        if (caster instanceof net.minecraft.world.entity.player.Player p) {
            float classMult = br.com.murilo.liberthia.magic.mageclass.MageClassBonus.apply(p, target);
            finalDamage *= classMult;
        }
        // r151: crit roll
        boolean isCrit = false;
        if (r.behavior.critChance > 0F && sl.random.nextFloat() < r.behavior.critChance) {
            finalDamage *= r.behavior.critMultiplier;
            isCrit = true;
            // VFX feedback de crit — particles douradas extras
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    20, 0.3, 0.5, 0.3, 0.5);
        }
        if (finalDamage > 0) {
            SchoolDamageSource sds = SchoolDamageSource.of(r.school);
            target.hurt(sds.toVanilla(sl, caster), finalDamage);
            // r151: knockback configurável
            if (r.behavior.knockbackStrength > 0F && caster != null) {
                double dx = target.getX() - caster.getX();
                double dz = target.getZ() - caster.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.001) {
                    double kb = r.behavior.knockbackStrength;
                    target.setDeltaMovement(target.getDeltaMovement().add(
                            dx / len * kb, kb * 0.3, dz / len * kb));
                    target.hurtMarked = true;
                }
            }
            if (isCrit) {
                sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                        net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.5F);
            }
        }
        for (SpellEffectSpec eff : r.effects) {
            eff.applyTo(target, caster, finalDamage);
        }
    }

    private static void applyAoeDamage(SpellRecipe r, ServerLevel sl, Vec3 center, float radius, ServerPlayer caster) {
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(
                        center.x - radius, center.y - radius, center.z - radius,
                        center.x + radius, center.y + radius, center.z + radius))) {
            if (e == caster) continue;
            if (e.distanceToSqr(center) <= radius * radius) {
                applyDamageAndEffects(r, e, caster, sl);
            }
        }
        // Ignite blocks no centro se FIRE + ignite_blocks
        if (r.behavior.igniteBlocks && r.school == SpellSchool.FIRE) {
            BlockPos bp = BlockPos.containing(center);
            if (sl.getBlockState(bp).isAir()) {
                sl.setBlock(bp, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }

    private static void spawnImpactBurst(SpellRecipe r, ServerLevel sl, Vec3 pos) {
        float scale = r.vfx.impactScale;
        int count = r.vfx.impactParticles;

        // Big particles
        SpellTrailParticleData big = new SpellTrailParticleData(r.school, 2.0F * scale, 22);
        sl.sendParticles(big, pos.x, pos.y, pos.z, count / 4, 0.4 * scale, 0.4 * scale, 0.4 * scale, 0.2);

        // Small
        SpellTrailParticleData small = new SpellTrailParticleData(r.school, 0.7F * scale, 16);
        sl.sendParticles(small, pos.x, pos.y, pos.z, (int)(count * 0.55), 0.9 * scale, 0.9 * scale, 0.9 * scale, 0.3);

        // Emissive sparks
        int hex = colorOrSchool(r);
        float rC = ((hex >> 16) & 0xFF) / 255F;
        float gC = ((hex >> 8) & 0xFF) / 255F;
        float bC = (hex & 0xFF) / 255F;
        ConfigurableParticleOptions spark = new ConfigurableParticleOptions(
                ModParticles.ENGINE_PARTICLE.get(),
                Math.min(1F, rC + 0.2F), Math.min(1F, gC + 0.2F), Math.min(1F, bC + 0.2F), 0.9F,
                0.35F * scale, 0.02F, 24,
                0.06F, 0.82F, 0.9F,
                false, true, true);
        sl.sendParticles(spark, pos.x, pos.y, pos.z, count / 2, 0.7 * scale, 0.7 * scale, 0.7 * scale, 0.35);

        // Ground decal
        ScheduledVfx.spawnGroundDecal(sl, pos, r.school, 1.5 + scale, 25);

        // Screen shake
        if (r.vfx.screenShake > 0.01F) {
            for (var sp : sl.getPlayers(p -> p.distanceToSqr(pos.x, pos.y, pos.z) < 64)) {
                br.com.murilo.liberthia.network.ModNetwork.sendToPlayer(sp,
                        new br.com.murilo.liberthia.magic.spell.ScreenShakeS2CPacket(
                                r.vfx.screenShake, r.vfx.screenShakeTicks));
            }
        }
    }

    private static void spawnConeParticles(SpellRecipe r, ServerLevel sl, Vec3 origin, Vec3 dir, float radius) {
        SpellTrailParticleData spark = new SpellTrailParticleData(r.school, 1.0F, 14);
        for (int i = 0; i < 40; i++) {
            float t = i / 40F;
            Vec3 along = origin.add(dir.scale(radius * t));
            // Spread em volta de 'along' proporcional a t (cone abre)
            double spread = radius * t * 0.3;
            sl.sendParticles(spark,
                    along.x + (sl.random.nextDouble() - 0.5) * spread * 2,
                    along.y + (sl.random.nextDouble() - 0.5) * spread,
                    along.z + (sl.random.nextDouble() - 0.5) * spread * 2,
                    1, 0, 0, 0, 0.05);
        }
    }

    private static void playImpactSound(SpellRecipe r, ServerLevel sl, Vec3 pos) {
        sl.playSound(null, pos.x, pos.y, pos.z,
                MagicSounds.impact(r.school), SoundSource.PLAYERS, 1.2F, 0.8F);
        sl.playSound(null, pos.x, pos.y, pos.z,
                MagicSounds.impactSecondary(r.school), SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    private static int colorOrSchool(SpellRecipe r) {
        return r.vfx.colorPrimary != -1 ? r.vfx.colorPrimary : r.school.colorHex();
    }

    /** Aplica overrides de VFX no projétil (cor primária se setada). */
    private static void applyVfxOverridesToProjectile(SpellProjectileEntity proj, SpellRecipe r) {
        // Tag metadata pra rich impact handlers (futuro)
        net.minecraft.nbt.CompoundTag tag = proj.getPersistentData();
        tag.putString("liberthia.spell_id", r.id);
        if (r.vfx.colorPrimary != -1) tag.putInt("liberthia.color_primary", r.vfx.colorPrimary);
        if (r.vfx.colorSecondary != -1) tag.putInt("liberthia.color_secondary", r.vfx.colorSecondary);
    }
}
