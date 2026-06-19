package br.com.murilo.liberthia.cosmic.horror.entity;

import br.com.murilo.liberthia.cosmic.scare.ScareS2CPacket;
import br.com.murilo.liberthia.cosmic.scare.ScareType;
import br.com.murilo.liberthia.network.ModNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * r180: <b>Arquivista do Fim</b> — registra tudo que fazes e tenta "corrigir".
 * Segue-te À DISTÂNCIA (teleporta pra longe se chegas perto). Escreve "profecias"
 * sobre ti (actionbar). Às vezes te EDITA: teleporta, troca itens da hotbar, te puxa.
 * Ao morrer, <b>o mundo para por ~1s</b> (entidades congelam + flash + som).
 */
public class EndArchivistEntity extends Monster {

    private int prophecyCd = 100;
    private int editCd = 180;

    private static final String[] PROPHECIES = {
            "§8§o…ele vai virar à esquerda em 3 segundos.",
            "§8§o…anotado: morrerá aqui. Já está escrito.",
            "§8§o…essa escolha foi um erro. Corrigindo.",
            "§8§o…você não devia existir nesta página.",
            "§8§o…reescrevendo o que você acha que fez."
    };

    public EndArchivistEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 45.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new MoveTowardsTargetGoal(this, 0.95D, 40.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl)) return;

        // glifos flutuantes do livro
        if (this.tickCount % 5 == 0) {
            sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 1.8, getZ(), 3, 0.4, 0.4, 0.4, 0.6);
        }

        LivingEntity tgt = this.getTarget();
        if (!(tgt instanceof ServerPlayer p) || !p.isAlive()) return;

        // Mantém DISTÂNCIA: se o player chega perto, teleporta pra longe (observa de fora)
        if (this.distanceToSqr(p) < 25.0 && this.tickCount % 10 == 0) {
            teleportAway(sl, p);
        }
        if (--prophecyCd <= 0) {
            prophecyCd = 120 + random.nextInt(120);
            p.displayClientMessage(Component.literal(PROPHECIES[random.nextInt(PROPHECIES.length)]), true);
            sl.playSound(null, blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.HOSTILE, 0.8F, 0.6F);
        }
        if (--editCd <= 0) {
            editCd = 180 + random.nextInt(160);
            editPlayer(sl, p);
        }
    }

    private void teleportAway(ServerLevel sl, Player p) {
        double ang = random.nextDouble() * Math.PI * 2;
        double dist = 9 + random.nextDouble() * 4;
        double x = p.getX() + Math.cos(ang) * dist;
        double z = p.getZ() + Math.sin(ang) * dist;
        int y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
        sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 12, 0.3, 0.6, 0.3, 0.3);
        this.teleportTo(x + 0.5, y, z + 0.5);
    }

    /** "Edita" o player: teleporta / troca itens / puxa. */
    private void editPlayer(ServerLevel sl, ServerPlayer p) {
        int kind = random.nextInt(3);
        p.displayClientMessage(Component.literal("§5§o*o Arquivista te edita*"), true);
        ModNetwork.sendToPlayer(p, new ScareS2CPacket(ScareType.FLASH, 8, 0, ""));
        switch (kind) {
            case 0 -> { // teleporta o player alguns blocos
                double ang = random.nextDouble() * Math.PI * 2;
                double x = p.getX() + Math.cos(ang) * 4, z = p.getZ() + Math.sin(ang) * 4;
                int y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
                p.teleportTo(x + 0.5, y, z + 0.5);
            }
            case 1 -> { // troca dois slots da hotbar
                int a = random.nextInt(9), b = random.nextInt(9);
                ItemStack tmp = p.getInventory().getItem(a);
                p.getInventory().setItem(a, p.getInventory().getItem(b));
                p.getInventory().setItem(b, tmp);
                p.inventoryMenu.broadcastChanges();
            }
            default -> { // puxa o player na direção do arquivista
                net.minecraft.world.phys.Vec3 pull = this.position().subtract(p.position()).normalize().scale(0.9);
                p.push(pull.x, 0.2, pull.z);
                p.hurtMarked = true;
            }
        }
        sl.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.7F, 0.7F);
    }

    /** Ao morrer: o mundo "para" por ~1s. */
    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) {
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(24.0))) {
                if (le instanceof Player) continue;
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 250, false, false));
                le.setDeltaMovement(0, le.getDeltaMovement().y, 0);
            }
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32.0))) {
                ModNetwork.sendToPlayer(p, new ScareS2CPacket(ScareType.FLASH, 20, 0, ""));
                p.displayClientMessage(Component.literal("§7§oO mundo para por um instante..."), true);
            }
            sl.playSound(null, blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.4F, 0.4F);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 40, 0.6, 1.0, 0.6, 0.1);
        }
        super.die(source);
    }
}
