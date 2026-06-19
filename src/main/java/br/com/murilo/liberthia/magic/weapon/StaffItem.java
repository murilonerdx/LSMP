package br.com.murilo.liberthia.magic.weapon;

import br.com.murilo.liberthia.magic.school.SchoolDamageSource;
import br.com.murilo.liberthia.magic.school.SpellSchool;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * v0.1.171 r143: <b>Staff</b> — arma channeled (beam sustentado).
 *
 * <h2>Right-click HOLD</h2>
 * Mantém botão pressionado pra disparar um RAIO contínuo da cor da escola.
 * Cada tick (20× por segundo) ele acerta o que estiver mirando + AOE 2.5b.
 *
 * <h2>Damage scaling</h2>
 * Base damage 12 melee. Channeled tick damage = 4 por tick (~80 dmg/sec sustained).
 * AOE secundário em 2.5b de raio com 50% dano.
 *
 * <h2>Cost</h2>
 * 2 Source por tick de channel (40 Source/segundo).
 */
public class StaffItem extends Item {

    private static final UUID DAMAGE_UUID = UUID.fromString("a3e8b9a2-3f4a-4f8b-9a1c-7d2e3f4a5b6c");
    public static final int MAX_USE_TICKS = 200; // 10s max channel
    public static final int SOURCE_PER_TICK = 2;
    public static final float BEAM_DAMAGE_PER_TICK = 4F;
    public static final float AOE_RADIUS = 2.5F;
    public static final double BEAM_RANGE = 24.0;

    private final SpellSchool school;
    private final Multimap<Attribute, AttributeModifier> attribs;

    public StaffItem(Properties props, SpellSchool school, int castCooldown, float meleeDamage) {
        super(props.stacksTo(1).durability(1500));
        this.school = school;
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(DAMAGE_UUID,
                "Staff damage", meleeDamage, AttributeModifier.Operation.ADDITION));
        b.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                UUID.fromString("a3e8b9a2-3f4a-4f8b-9a1c-7d2e3f4a5b6d"),
                "Staff speed", -2.2F, AttributeModifier.Operation.ADDITION));
        this.attribs = b.build();
    }

    public SpellSchool school() { return school; }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? attribs : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // Hold pose like bow
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return MAX_USE_TICKS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // r143: começa channeling — startUsingItem ativa tick callbacks
        player.startUsingItem(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            sp.serverLevel().playSound(null, sp.blockPosition(),
                    getStartSound(), SoundSource.PLAYERS, 0.7F, 1.2F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (level.isClientSide || !(entity instanceof ServerPlayer sp)) return;
        ServerLevel sl = sp.serverLevel();

        // r143: gasta Source por tick
        int sourceCur = br.com.murilo.liberthia.observation.source.SourceData.get(sp);
        if (sourceCur < SOURCE_PER_TICK) {
            sp.releaseUsingItem();
            sp.displayClientMessage(Component.literal(
                "§c⚠ Source esgotado"), true);
            return;
        }
        br.com.murilo.liberthia.observation.source.SourceData.consume(sp, SOURCE_PER_TICK);

        // Cast beam
        castBeam(sp, sl, stack);
    }

    /** Beam channeled: a cada tick, raycast + AOE damage + sprites grandes. */
    private void castBeam(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 from = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        Vec3 to = from.add(look.scale(BEAM_RANGE));

        // Raycast pra entity
        net.minecraft.world.phys.HitResult hit = sp.pick(BEAM_RANGE, 0F, false);
        LivingEntity primary = null;

        // Entity ray search
        var box = new AABB(from, to).inflate(1.0);
        double closest = Double.MAX_VALUE;
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != sp && e.isAlive())) {
            Vec3 toEntity = le.position().add(0, le.getBbHeight() / 2, 0).subtract(from);
            double t = toEntity.dot(look);
            if (t < 0 || t > BEAM_RANGE) continue;
            Vec3 projected = from.add(look.scale(t));
            double distPerp = projected.distanceTo(le.position().add(0, le.getBbHeight() / 2, 0));
            if (distPerp < 1.2 && t < closest) {
                closest = t;
                primary = le;
            }
        }

        Vec3 hitPos = primary != null
                ? primary.position().add(0, primary.getBbHeight() / 2, 0)
                : to;

        // Beam particles — denso, espessura visual grande
        Vec3 dir = hitPos.subtract(from);
        double dist = dir.length();
        Vec3 norm = dir.normalize();
        int steps = (int) (dist * 4); // 4 partículas por bloco
        for (int i = 0; i < steps; i++) {
            double f = i / (double) steps;
            double x = from.x + norm.x * dist * f;
            double y = from.y + norm.y * dist * f;
            double z = from.z + norm.z * dist * f;
            // Core particles (densas)
            sl.sendParticles(getParticleForSchool(),
                    x, y, z, 2, 0.05, 0.05, 0.05, 0);
            // Wider corona pra parecer raio grosso
            if (i % 2 == 0) {
                sl.sendParticles(getCoronaParticle(),
                        x + (sl.random.nextDouble() - 0.5) * 0.3,
                        y + (sl.random.nextDouble() - 0.5) * 0.3,
                        z + (sl.random.nextDouble() - 0.5) * 0.3,
                        1, 0.02, 0.02, 0.02, 0);
            }
        }

        // Damage primary target
        if (primary != null) {
            final LivingEntity primaryFinal = primary;
            applyDamageToTarget(sp, sl, primaryFinal, BEAM_DAMAGE_PER_TICK);
            // AOE around primary target
            AABB aoeBox = new AABB(primaryFinal.position(), primaryFinal.position()).inflate(AOE_RADIUS);
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, aoeBox,
                    e -> e != sp && e != primaryFinal && e.isAlive())) {
                applyDamageToTarget(sp, sl, le, BEAM_DAMAGE_PER_TICK * 0.5F);
            }
            // Burst no impact
            for (int i = 0; i < 8; i++) {
                sl.sendParticles(getCoronaParticle(),
                        hitPos.x + (sl.random.nextDouble() - 0.5) * 0.8,
                        hitPos.y + (sl.random.nextDouble() - 0.5) * 0.8,
                        hitPos.z + (sl.random.nextDouble() - 0.5) * 0.8,
                        1, 0.1, 0.1, 0.1, 0.05);
            }
        }

        // Durability
        if (sp.tickCount % 20 == 0) {
            stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(sp.getUsedItemHand()));
        }

        // Cast sound a cada 5 ticks pra não saturar
        if (sp.tickCount % 5 == 0) {
            sl.playSound(null, sp.blockPosition(), getTickSound(),
                    SoundSource.PLAYERS, 0.3F, 1.0F + (sp.tickCount % 10) * 0.05F);
        }
    }

    private void applyDamageToTarget(ServerPlayer sp, ServerLevel sl, LivingEntity target, float dmg) {
        // Apply school-specific effects
        switch (school) {
            case FIRE -> {
                target.setRemainingFireTicks(target.getRemainingFireTicks() + 40);
                target.hurt(SchoolDamageSource.fire(80).toVanilla(sl, sp), dmg);
            }
            case ICE -> {
                target.setTicksFrozen(Math.min(160, target.getTicksFrozen() + 40));
                target.hurt(SchoolDamageSource.ice(60).toVanilla(sl, sp), dmg);
            }
            case LIGHTNING -> {
                target.hurt(SchoolDamageSource.of(SpellSchool.LIGHTNING).toVanilla(sl, sp), dmg * 1.2F);
                // Chain lightning effect
                if (target.tickCount % 10 == 0) {
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            target.getX(), target.getY() + 1, target.getZ(),
                            12, 0.5, 1, 0.5, 0.2);
                }
            }
            case BLOOD -> {
                target.hurt(SchoolDamageSource.blood(0.3F).toVanilla(sl, sp), dmg);
                if (sl.random.nextFloat() < 0.2F) sp.heal(0.5F);
            }
            case ELDRITCH -> {
                target.hurt(SchoolDamageSource.eldritch().toVanilla(sl, sp), dmg * 0.9F);
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DARKNESS, 40, 0, true, false));
            }
            case HOLY -> {
                float holyDmg = target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD ? dmg * 2F : dmg;
                target.hurt(SchoolDamageSource.of(SpellSchool.HOLY).toVanilla(sl, sp), holyDmg);
                if (sp.tickCount % 20 == 0) sp.heal(1F);
            }
            case NATURE -> {
                target.hurt(SchoolDamageSource.of(SpellSchool.NATURE).toVanilla(sl, sp), dmg);
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 2, true, false));
            }
        }
    }

    private net.minecraft.core.particles.ParticleOptions getParticleForSchool() {
        return switch (school) {
            case FIRE -> ParticleTypes.FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case BLOOD -> ParticleTypes.DAMAGE_INDICATOR;
            case ELDRITCH -> ParticleTypes.SOUL_FIRE_FLAME;
            case HOLY -> ParticleTypes.END_ROD;
            case NATURE -> ParticleTypes.HAPPY_VILLAGER;
        };
    }

    /** Particle de "corona" pra dar look de raio grosso. */
    private net.minecraft.core.particles.ParticleOptions getCoronaParticle() {
        return switch (school) {
            case FIRE -> ParticleTypes.LAVA;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.END_ROD;
            case BLOOD -> ParticleTypes.CRIT;
            case ELDRITCH -> ParticleTypes.PORTAL;
            case HOLY -> ParticleTypes.GLOW;
            case NATURE -> ParticleTypes.COMPOSTER;
        };
    }

    private net.minecraft.sounds.SoundEvent getStartSound() {
        return switch (school) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case ICE -> SoundEvents.GLASS_BREAK;
            case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case BLOOD -> SoundEvents.WITHER_SHOOT;
            case ELDRITCH -> SoundEvents.PORTAL_TRIGGER;
            case HOLY -> SoundEvents.BEACON_ACTIVATE;
            case NATURE -> SoundEvents.AMETHYST_BLOCK_BREAK;
        };
    }

    private net.minecraft.sounds.SoundEvent getTickSound() {
        return switch (school) {
            case FIRE -> SoundEvents.FIRE_AMBIENT;
            case ICE -> SoundEvents.AMETHYST_BLOCK_HIT;
            case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case BLOOD -> SoundEvents.AMETHYST_BLOCK_FALL;
            case ELDRITCH -> SoundEvents.PORTAL_AMBIENT;
            case HOLY -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case NATURE -> SoundEvents.AMETHYST_BLOCK_PLACE;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Escola: ").append(school.label()));
        tooltip.add(Component.literal("§b§lRAIO CANALIZADO"));
        tooltip.add(Component.literal("§7Dano: §c" + BEAM_DAMAGE_PER_TICK + "/tick §8(~80/s)"));
        tooltip.add(Component.literal("§7AOE: §a" + AOE_RADIUS + "b §8(50% dano)"));
        tooltip.add(Component.literal("§7Source: §b" + SOURCE_PER_TICK + "/tick §8(40/s)"));
        tooltip.add(Component.literal("§8§oSegure o botão pra raio contínuo"));
    }
}
