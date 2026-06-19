package br.com.murilo.liberthia.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * v0.1.22 r24: corpo físico do player enquanto a consciência está no mundo
 * espiritual. Renderizado como o player (mesma skin/nome via
 * {@code OWNER_UUID}/{@code OWNER_NAME} herdado de
 * {@link ClonePlayerEntity}).
 *
 * <h2>Comportamento</h2>
 * <ul>
 *   <li>{@code NoAi=true} — não anda, não pathfinda</li>
 *   <li>{@code Invulnerable=true} por default (anti-troll), MAS aceita:
 *     <ul>
 *       <li>OUT_OF_WORLD (void) — não dá pra evitar</li>
 *       <li>damage de magic source explícito (mods de RP)</li>
 *     </ul>
 *   </li>
 *   <li>Sound de respiração leve a cada 5s (player.breath / 0.5 vol)</li>
 *   <li>NÃO despawna automaticamente. Só some quando o player volta ou via
 *       /liberthia spirit reset</li>
 *   <li>HP igual ao do player original quando spawnado. Se HP chega a 0
 *       (mesmo invulnerável geralmente), o player ESPIRITUAL morre — link
 *       de vida.</li>
 * </ul>
 */
public class SoulBodyEntity extends ClonePlayerEntity {

    /** True se o player original está MORRENDO via esse body (sync). */
    private static final EntityDataAccessor<Boolean> DYING =
            SynchedEntityData.defineId(SoulBodyEntity.class, EntityDataSerializers.BOOLEAN);

    public SoulBodyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // Por padrão é invulnerable, mas pode ser desligado por NBT/admin
        this.setInvulnerable(true);
        this.setNoAi(true);
        this.setNoGravity(false); // cai se desafiamos a gravidade
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DYING, false);
    }

    public void setDying(boolean dying) {
        this.entityData.set(DYING, dying);
    }

    public boolean isDying() {
        return this.entityData.get(DYING);
    }

    /**
     * v0.1.22 r25 BUGFIX: ClonePlayerEntity (parent) usa SynchedEntityData
     * pra OWNER_UUID/OWNER_NAME mas NÃO persiste em NBT. Se server reinicia
     * com body ativo, vira "corpo sem dono" e o renderer quebra. Aqui
     * salvamos manualmente em NBT.
     */
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        java.util.UUID owner = this.getOwnerUuid();
        if (owner != null) tag.putUUID("LiberthiaBodyOwner", owner);
        tag.putString("LiberthiaBodyName", this.getOwnerName() == null ? "" : this.getOwnerName());
        tag.putBoolean("LiberthiaBodyDying", this.isDying());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("LiberthiaBodyOwner")) {
            this.setOwnerUuid(tag.getUUID("LiberthiaBodyOwner"));
        }
        if (tag.contains("LiberthiaBodyName")) {
            this.setOwnerName(tag.getString("LiberthiaBodyName"));
        }
        if (tag.getBoolean("LiberthiaBodyDying")) {
            this.setDying(true);
        }
    }

    /**
     * Override: aceita void damage e magic source (ex: /kill via admin)
     * mas ignora dano vanilla normal (mob attack, queda, etc).
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Sempre aceita void/kill admin
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            this.setInvulnerable(false);
            boolean result = super.hurt(source, amount);
            this.setInvulnerable(true);
            if (this.getHealth() <= 0) {
                this.setDying(true);
            }
            return result;
        }
        // Defesa contra outros tipos: ignora
        return false;
    }

    /** Tick — respiração leve, partículas de soul subtle. */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        // Respiração a cada 5s (100t)
        if (this.tickCount % 100 == 0) {
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.PLAYER_BREATH,
                    net.minecraft.sounds.SoundSource.NEUTRAL,
                    0.3F, 0.7F + this.random.nextFloat() * 0.3F);
        }
        // Partícula de soul subtle a cada 40t (2s)
        if (this.tickCount % 40 == 0 && this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                    this.getX(), this.getY() + 1.8, this.getZ(),
                    1, 0.1, 0.1, 0.1, 0.005);
        }
    }
}
