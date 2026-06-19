package br.com.murilo.liberthia.cosmic.horror.entity;

import br.com.murilo.liberthia.registry.ModParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * <b>Manifestação do Vazio</b> — uma sombra de fumaça negra com olhos vermelhos
 * girando e tentáculos, inspirada na invocação do Underzealot/Gloomoth (Alex's Caves).
 *
 * <p>NÃO ataca nem dá dano por padrão — é um VFX assombroso que paira no lugar por
 * {@value #LIFETIME} ticks: cresce, pulsa, "olha" pro player mais próximo e some.
 * Toda a aparência é client-side ({@code VoidManifestationRenderer}); aqui só
 * controlamos ciclo de vida, partículas-server e som.
 */
public class VoidManifestationEntity extends Entity {

    public static final int LIFETIME = 220; // ~11s

    /** progresso 0..1 sincronizado pro cliente (envelope de escala/alpha). */
    private static final EntityDataAccessor<Float> PROGRESS =
            SynchedEntityData.defineId(VoidManifestationEntity.class, EntityDataSerializers.FLOAT);

    private int age = 0;

    public VoidManifestationEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true; // VFX grande — não some por culling de bbox
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(PROGRESS, 0.0F);
    }

    /** 0 → sobe → platô → desce → 0 (envelope suave pra escala e opacidade). */
    public float getProgress() {
        return this.entityData.get(PROGRESS);
    }

    /** Ângulo base de giro (em graus) — usado pelos olhos/fumaça no renderer. */
    public float getSpin(float partial) {
        return (age + partial) * 4.0F;
    }

    public int getAge() { return age; }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            spawnClientParticles();
            age++;
            return;
        }

        age++;

        // envelope: sobe em 30t, platô, desce nos últimos 40t
        float p;
        if (age < 30) p = age / 30.0F;
        else if (age > LIFETIME - 40) p = Math.max(0.0F, (LIFETIME - age) / 40.0F);
        else p = 1.0F;
        this.entityData.set(PROGRESS, p);

        // som ressonante grave de invocação, periódico
        if (age % 24 == 0) {
            level().playSound(null, blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.HOSTILE, 1.1F, 0.5F);
        }
        if (age == 6) {
            level().playSound(null, blockPosition(), SoundEvents.WARDEN_EMERGE,
                    SoundSource.HOSTILE, 0.9F, 0.7F);
        }

        // pequena pressão psicológica: escuridão em quem fica encarando perto (sem dano)
        if (age % 40 == 0) {
            Player near = level().getNearestPlayer(this, 8.0);
            if (near != null && !near.isCreative() && !near.isSpectator()) {
                near.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false, true));
            }
        }

        // VFX CUSTOM (não-vanilla) server-side pra todos verem:
        // fumaça-sombra dá corpo à massa; olhos vermelhos VOAM saindo dela.
        if (level() instanceof ServerLevel sl) {
            // sombra: várias wisps por tick, espalhadas em volta do núcleo
            // (count=0 + os 3 "Dist" viram o vetor velocidade; speed=1 multiplicador)
            if (age % 2 == 0) {
                for (int i = 0; i < 4; i++) {
                    double ax = (sl.random.nextDouble() - 0.5) * 1.6;
                    double az = (sl.random.nextDouble() - 0.5) * 1.6;
                    sl.sendParticles(ModParticles.VOID_SHADOW_WISP.get(),
                            getX() + ax, getY() + 0.7 + sl.random.nextDouble() * 1.2, getZ() + az,
                            0, ax * 0.3, 0.04, az * 0.3, 1.0);
                }
            }
            // olhos: nascem do núcleo e voam pra fora (vetor radial)
            if (age % 7 == 0 && age > 8) {
                int eyes = 1 + sl.random.nextInt(2);
                for (int i = 0; i < eyes; i++) {
                    double ang = sl.random.nextDouble() * Math.PI * 2;
                    double vx = Math.cos(ang), vz = Math.sin(ang);
                    sl.sendParticles(ModParticles.VOID_FLYING_EYE.get(),
                            getX() + vx * 0.5, getY() + 0.9 + sl.random.nextDouble() * 0.8, getZ() + vz * 0.5,
                            0, vx, 0.0, vz, 1.0);
                }
            }
        }

        if (age >= LIFETIME) {
            if (level() instanceof ServerLevel sl) {
                // dissipação: jato final de fumaça-sombra
                for (int i = 0; i < 16; i++) {
                    double ax = (sl.random.nextDouble() - 0.5) * 1.4;
                    double az = (sl.random.nextDouble() - 0.5) * 1.4;
                    sl.sendParticles(ModParticles.VOID_SHADOW_WISP.get(),
                            getX() + ax, getY() + 1.0, getZ() + az, 0, ax, 0.06, az, 1.0);
                }
            }
            discard();
        }
    }

    private void spawnClientParticles() {
        // (partículas custom são emitidas server-side via sendParticles pra todos verem;
        //  client-side fica vazio pra não duplicar)
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { age = tag.getInt("Age"); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putInt("Age", age); }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    /** Sempre renderiza (VFX grande), independente da distância de frustum. */
    @Override public boolean shouldRenderAtSqrDistance(double dist) { return dist < 16384.0; }
}
