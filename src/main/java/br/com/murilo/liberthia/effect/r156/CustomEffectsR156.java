package br.com.murilo.liberthia.effect.r156;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * r156: <b>20 Custom Mob Effects</b> — efeitos divertidos/assustadores/criativos
 * que exploram o mod inteiro.
 *
 * <p>Cada efeito é uma inner class para registro compacto em {@link br.com.murilo.liberthia.registry.ModEffects}.
 */
public final class CustomEffectsR156 {

    private CustomEffectsR156() {}

    // 1. DIMENSIONAL_BLINDNESS — A cada 10s teleporta câmera para outro player por 4s
    public static class DimensionalBlindness extends MobEffect {
        public DimensionalBlindness() { super(MobEffectCategory.HARMFUL, 0x6B2DC4); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e instanceof ServerPlayer sp)) return;
            long t = e.level().getGameTime();
            if (t % 200 != 0) return;  // cada 10s
            List<ServerPlayer> players = ((ServerLevel)e.level()).players();
            ServerPlayer other = null;
            for (ServerPlayer p : players) if (p != sp) { other = p; break; }
            if (other == null) return;
            // Marca pra cliente trocar visão (4s)
            sp.getPersistentData().putLong("liberthia.dim_blind_until", t + 80);
            sp.getPersistentData().putString("liberthia.dim_blind_target", other.getStringUUID());
            sp.level().playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 0.7F);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 2. DEVIL_FOOTSTEPS — pés deixam magic_fire_blood
    public static class DevilFootsteps extends MobEffect {
        public DevilFootsteps() { super(MobEffectCategory.HARMFUL, 0xCC1100); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (!e.onGround()) return;
            if (e.level().getGameTime() % 5 != 0) return;
            BlockPos under = e.blockPosition();
            if (sl.getBlockState(under).isAir() && sl.getBlockState(under.below()).isSolidRender(sl, under.below())) {
                var magicFire = br.com.murilo.liberthia.registry.ModBlocks.MAGIC_FIRE_BLOOD.get();
                sl.setBlock(under, magicFire.defaultBlockState(), 3);
            }
            sl.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY()+0.05, e.getZ(), 3, 0.2, 0.05, 0.2, 0.01);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 3. BLOOD_MOON — dá blood_infection a inimigos próximos
    public static class BloodMoon extends MobEffect {
        public BloodMoon() { super(MobEffectCategory.BENEFICIAL, 0xAA0033); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (sl.getGameTime() % 40 != 0) return;
            AABB box = new AABB(e.blockPosition()).inflate(6);
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, box)) {
                if (t == e) continue;
                if (t instanceof Player p && p == e) continue;
                t.addEffect(new MobEffectInstance(br.com.murilo.liberthia.registry.ModEffects.BLOOD_INFECTION.get(), 100, amp));
            }
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getY()+1, e.getZ(), 6, 1.5, 1.5, 1.5, 0.05);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 4. SHADOW_DOUBLE — particles atrás do player tipo clone
    public static class ShadowDouble extends MobEffect {
        public ShadowDouble() { super(MobEffectCategory.NEUTRAL, 0x111111); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (sl.getGameTime() % 4 != 0) return;
            float yaw = e.getYRot() * (float)Math.PI / 180F;
            double dx = -Math.sin(yaw) * 2.5;
            double dz = Math.cos(yaw) * 2.5;
            sl.sendParticles(ParticleTypes.SQUID_INK, e.getX()+dx, e.getY()+1, e.getZ()+dz, 4, 0.3, 0.8, 0.3, 0.01);
            sl.sendParticles(ParticleTypes.SMOKE, e.getX()+dx, e.getY()+0.5, e.getZ()+dz, 2, 0.2, 0.2, 0.2, 0.0);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 5. WHISPERS — som ambiente aleatório no ouvido
    public static class Whispers extends MobEffect {
        public Whispers() { super(MobEffectCategory.HARMFUL, 0x4B0082); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (e.level().random.nextInt(80) != 0) return;
            sl.playSound(null, e.blockPosition(),
                    SoundEvents.WARDEN_AMBIENT, SoundSource.AMBIENT, 0.3F, 0.6F + e.level().random.nextFloat() * 0.4F);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 6. VERTIGO — particles tontas + slowness leve
    public static class Vertigo extends MobEffect {
        public Vertigo() { super(MobEffectCategory.HARMFUL, 0x66FF99); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (e.level().getGameTime() % 100 == 0) {
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
            }
            sl.sendParticles(ParticleTypes.NAUTILUS, e.getX(), e.getY()+1.6, e.getZ(), 2, 0.5, 0.2, 0.5, 0.05);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 7. HUNGRY_VOID — items próximos voam pro player (magnet)
    public static class HungryVoid extends MobEffect {
        public HungryVoid() { super(MobEffectCategory.BENEFICIAL, 0x000033); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (sl.getGameTime() % 5 != 0) return;
            AABB box = new AABB(e.blockPosition()).inflate(8);
            for (ItemEntity it : sl.getEntitiesOfClass(ItemEntity.class, box)) {
                double dx = e.getX() - it.getX();
                double dy = e.getY() + 0.5 - it.getY();
                double dz = e.getZ() - it.getZ();
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (dist < 0.5) continue;
                double speed = 0.3;
                it.setDeltaMovement(dx/dist*speed, dy/dist*speed, dz/dist*speed);
            }
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 8. GHOST_TOUCH — player atravessa entidades + leveza
    public static class GhostTouch extends MobEffect {
        public GhostTouch() { super(MobEffectCategory.BENEFICIAL, 0xCCEEFF); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (sl.getGameTime() % 20 == 0) {
                e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false));
            }
            sl.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY()+1, e.getZ(), 1, 0.5, 0.5, 0.5, 0.01);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 9. SOUL_LINK — dano sofrido envia 25% pra um aliado próximo (via player-data marker)
    public static class SoulLink extends MobEffect {
        public SoulLink() { super(MobEffectCategory.NEUTRAL, 0xFF66AA); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            // marker para event handler externo. Aqui só visualiza.
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (sl.getGameTime() % 10 != 0) return;
            sl.sendParticles(ParticleTypes.HEART, e.getX(), e.getY()+1.5, e.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 10. HAUNTED_INVENTORY — swap items aleatórios no hotbar
    public static class HauntedInventory extends MobEffect {
        public HauntedInventory() { super(MobEffectCategory.HARMFUL, 0x9933CC); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e instanceof Player p)) return;
            if (p.level().getGameTime() % 200 != 0) return;  // cada 10s
            var inv = p.getInventory();
            int a = p.level().random.nextInt(9);
            int b = p.level().random.nextInt(9);
            if (a == b) return;
            ItemStack ia = inv.getItem(a);
            ItemStack ib = inv.getItem(b);
            inv.setItem(a, ib);
            inv.setItem(b, ia);
            if (p.level() instanceof ServerLevel sl)
                sl.playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4F, 1.5F);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 11. MIRROR_WALK — dano ao redor refletido em particles atrás
    public static class MirrorWalk extends MobEffect {
        public MirrorWalk() { super(MobEffectCategory.BENEFICIAL, 0xCCFFFF); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (sl.getGameTime() % 3 != 0) return;
            sl.sendParticles(ParticleTypes.GLOW, e.getX(), e.getY()+1, e.getZ(), 1, 0.3, 0.5, 0.3, 0.05);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 12. TIME_DILATION — mobs próximos ficam lentos
    public static class TimeDilation extends MobEffect {
        public TimeDilation() { super(MobEffectCategory.BENEFICIAL, 0x8B7FBF); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (sl.getGameTime() % 20 != 0) return;
            AABB box = new AABB(e.blockPosition()).inflate(6);
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, box)) {
                // pula a própria fonte E qualquer player — a aura lenta MOBS, nunca
                // deve aleijar jogadores (Fadiga de Mineração invisível = "bug").
                if (t == e || t instanceof net.minecraft.world.entity.player.Player) continue;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
                t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, false, false));
            }
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 13. REVERSE_GRAVITY — empurra player pra cima periodicamente
    public static class ReverseGravity extends MobEffect {
        public ReverseGravity() { super(MobEffectCategory.NEUTRAL, 0xFFAA00); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            long t = e.level().getGameTime();
            long phase = t % 200;
            if (phase < 80) {  // 4s de gravidade reversa, depois 6s normal
                if (e.level() instanceof ServerLevel) {
                    var v = e.getDeltaMovement();
                    e.setDeltaMovement(v.x, Math.max(v.y, 0.06), v.z);
                    e.fallDistance = 0;
                }
            }
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 14. MAGNET_FIST — items voam para o player (alias de hungry void mas raio maior)
    public static class MagnetFist extends MobEffect {
        public MagnetFist() { super(MobEffectCategory.BENEFICIAL, 0xFF6633); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (sl.getGameTime() % 4 != 0) return;
            AABB box = new AABB(e.blockPosition()).inflate(12);
            for (ItemEntity it : sl.getEntitiesOfClass(ItemEntity.class, box)) {
                double dx = e.getX() - it.getX();
                double dy = e.getY() + 0.5 - it.getY();
                double dz = e.getZ() - it.getZ();
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (dist < 0.5) continue;
                double speed = 0.5;
                it.setDeltaMovement(dx/dist*speed, dy/dist*speed, dz/dist*speed);
                it.setUnlimitedLifetime();
            }
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 15. POX_SWARM — particles ao redor + blindness em inimigos próximos
    public static class PoxSwarm extends MobEffect {
        public PoxSwarm() { super(MobEffectCategory.HARMFUL, 0x668833); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            sl.sendParticles(ParticleTypes.SCULK_SOUL, e.getX(), e.getY()+1.5, e.getZ(), 3, 1.2, 0.8, 1.2, 0.02);
            if (sl.getGameTime() % 60 == 0) {
                AABB box = new AABB(e.blockPosition()).inflate(4);
                for (Mob t : sl.getEntitiesOfClass(Mob.class, box)) {
                    if (t == e) continue;
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false));
                }
            }
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 16. NIGHTMARE — periodicamente dá Confusion + som assustador
    public static class Nightmare extends MobEffect {
        public Nightmare() { super(MobEffectCategory.HARMFUL, 0x4B0080); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (sl.getGameTime() % 100 != 0) return;
            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
            sl.playSound(null, e.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.AMBIENT, 0.4F, 0.5F);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 17. LIBERTHIA_BLESSING — heal + regen + glow continuo
    public static class LiberthiaBlessing extends MobEffect {
        public LiberthiaBlessing() { super(MobEffectCategory.BENEFICIAL, 0xFFE066); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (e.level().getGameTime() % 40 == 0) {
                e.heal(2F + amp);
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
            }
            if (e.level() instanceof ServerLevel sl && sl.getGameTime() % 10 == 0) {
                sl.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY()+1, e.getZ(), 2, 0.4, 0.6, 0.4, 0.01);
            }
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 18. CRYSTAL_BLOOM — flores spawnam nos blocos pisados
    public static class CrystalBloom extends MobEffect {
        public CrystalBloom() { super(MobEffectCategory.BENEFICIAL, 0xFF99CC); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (!e.onGround()) return;
            if (sl.getGameTime() % 8 != 0) return;
            BlockPos below = e.blockPosition().below();
            if (sl.getBlockState(below).is(Blocks.GRASS_BLOCK)
                    && sl.getBlockState(e.blockPosition()).isAir()
                    && sl.getRandom().nextFloat() < 0.5F) {
                sl.setBlock(e.blockPosition(),
                        sl.getRandom().nextBoolean() ? Blocks.DANDELION.defaultBlockState() : Blocks.POPPY.defaultBlockState(), 3);
            }
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, e.getX(), e.getY()+0.2, e.getZ(), 2, 0.3, 0.1, 0.3, 0.01);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 19. OMINOUS_AURA — taunt: mobs vêm pra cima do player
    public static class OminousAura extends MobEffect {
        public OminousAura() { super(MobEffectCategory.HARMFUL, 0x550000); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            if (sl.getGameTime() % 20 != 0) return;
            AABB box = new AABB(e.blockPosition()).inflate(16);
            for (Monster m : sl.getEntitiesOfClass(Monster.class, box)) {
                if (m.getTarget() == null) {
                    m.setTarget(e);
                }
            }
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }

    // 20. STARDUST — trail de partículas em movimento
    public static class Stardust extends MobEffect {
        public Stardust() { super(MobEffectCategory.NEUTRAL, 0xAAFFFF); }
        @Override public void applyEffectTick(LivingEntity e, int amp) {
            if (!(e.level() instanceof ServerLevel sl)) return;
            sl.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY()+0.5+e.level().random.nextFloat(),
                    e.getZ(), 3, 0.5, 0.5, 0.5, 0.03);
            sl.sendParticles(ParticleTypes.GLOW, e.getX(), e.getY()+1, e.getZ(), 1, 0.4, 0.4, 0.4, 0.02);
        }
        @Override public boolean isDurationEffectTick(int dur, int amp) { return true; }
    }
}
