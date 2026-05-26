package br.com.murilo.liberthia.item;

import br.com.murilo.liberthia.registry.ModCapabilities;
import br.com.murilo.liberthia.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clear Matter Sword — versão v1 channel + death explosion.
 *
 * <ul>
 *   <li><b>Left-click (hit)</b>: dano normal + acumula LifestealCharge (legado).</li>
 *   <li><b>Right-click hold</b>: channel drain — segura o botão para drenar HP
 *       de entidades em cone à frente (raio 5 blocos), curando o player.
 *       Tomar dano interrompe o channel.</li>
 *   <li><b>Shift + right-click</b>: dash flamejante (legado).</li>
 *   <li><b>Death explosion</b>: ao morrer com a espada equipada, libera uma onda
 *       que adiciona 10% do max profile de white matter em players próximos
 *       e aplica Weakness III em mobs sem profile.</li>
 * </ul>
 */
public class ClearMatterSwordItem extends SwordItem {

    private static final String TAG_CHARGE = "LifestealCharge";
    private static final int MAX_CHARGE = 100;
    private static final int CHARGE_PER_HIT = 5;

    // r37 NERF MASSIVO — espada estava com imortalidade via Absorption infinita,
    // multi-hit accumulation via invulnerableTime=0, e damage charge stacking.
    // Tudo nerfado em 50-70% pra ser viável mas não apelona.
    private static final int DASH_COOLDOWN_TICKS = 400;     // 20s (era 10s)
    private static final double DASH_RANGE = 4.0D;          // 4b (era 6b)
    private static final int DASH_HITS = 1;                 // 1 hit (era 3)
    private static final float DASH_HIT_MULT = 1.2F;        // 1.2x (era 1.5x)
    private static final int DASH_FIRE_SECONDS = 3;
    private static final float BASE_ATTACK_DAMAGE = 6.0F;

    // Channel parameters — drain MUITO menor
    private static final double CHANNEL_RANGE = 4.0D;        // 4b (era 5b)
    private static final float CHANNEL_DAMAGE_PER_TICK = 0.25F; // 0.25 HP/tick (era 0.5)
    private static final float CHANNEL_HEAL_PER_TICK = 0.10F;   // 0.10 HP/tick (era 0.25)

    /**
     * Players actively channeling — tracked here so {@code interrupted on hurt}
     * can be implemented via {@link br.com.murilo.liberthia.event.ClearSwordChannelHandler}.
     */
    public static final Set<UUID> CHANNELING_PLAYERS = ConcurrentHashMap.newKeySet();
    /** Flag for current-tick interruption (cleared each tick). */
    public static final Set<UUID> INTERRUPTED_THIS_TICK = ConcurrentHashMap.newKeySet();

    public ClearMatterSwordItem(Properties properties) {
        super(ClearMatterToolMaterial.INSTANCE, 6, -2.4F, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);

        if (result) {
            attacker.addEffect(new MobEffectInstance(ModEffects.CLEAR_SHIELD.get(), 60, 0));

            if (target instanceof Player targetPlayer) {
                targetPlayer.getCapability(ModCapabilities.INFECTION).ifPresent(data -> {
                    data.reduceInfection(5);
                    data.setDirty(true);
                });
            }

            // r37 NERF: REMOVIDO damage charge accumulation. Antes cada hit
            // somava charge que aplicava dano bonus mágico — virava arma de
            // 1-shot kill após várias hits. Charge agora SÓ pra visual/tooltip.
            int charge = getCharge(stack);
            setCharge(stack, Math.min(MAX_CHARGE, charge + CHARGE_PER_HIT));
        }

        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift + right-click = fire dash (legacy behavior)
        if (player.isShiftKeyDown()) {
            if (player.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.pass(stack);
            }
            if (level.isClientSide()) {
                return InteractionResultHolder.success(stack);
            }
            if (!(level instanceof ServerLevel sl)) {
                return InteractionResultHolder.pass(stack);
            }
            performDash(sl, player, stack);
            return InteractionResultHolder.success(stack);
        }

        // Right-click HOLD = start channeling drain
        player.startUsingItem(hand);
        if (!level.isClientSide()) {
            CHANNELING_PLAYERS.add(player.getUUID());
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(user instanceof Player player)) return;
        if (!(level instanceof ServerLevel sl)) return;

        UUID id = player.getUUID();
        // v0.1.35: user pediu drain INFINITO — não interrompe mais por dano.
        // Antes: if (INTERRUPTED_THIS_TICK.remove(id)) → stopUsingItem.
        // Agora: limpa a flag silenciosamente, continua canalizando.
        INTERRUPTED_THIS_TICK.remove(id);

        // Find living entities in cone in front of player
        Vec3 look = player.getLookAngle();
        Vec3 origin = player.position().add(0, player.getEyeHeight() * 0.5, 0);
        AABB scan = new AABB(
                origin.x - CHANNEL_RANGE, origin.y - CHANNEL_RANGE, origin.z - CHANNEL_RANGE,
                origin.x + CHANNEL_RANGE, origin.y + CHANNEL_RANGE, origin.z + CHANNEL_RANGE
        );
        List<LivingEntity> candidates = sl.getEntitiesOfClass(LivingEntity.class, scan,
                e -> e != player && e.isAlive());

        int hitCount = 0;
        for (LivingEntity target : candidates) {
            Vec3 toTarget = target.position().subtract(origin).normalize();
            double dot = toTarget.dot(look);
            if (dot < 0.5) continue; // cone ~60deg
            double dist = target.position().distanceTo(origin);
            if (dist > CHANNEL_RANGE) continue;

            // r37 NERF: REMOVIDO invulnerableTime=0 — agora respeita damage cooldown
            // vanilla (0.5s entre hits). Antes era multi-hit por tick = OP.
            target.hurt(player.damageSources().playerAttack(player), CHANNEL_DAMAGE_PER_TICK);
            // r37 NERF: REMOVIDO Absorption auto-refresh. Antes ficava com 10HP
            // de Absorption infinitamente enquanto canalizava = imortalidade.
            // Agora só heal pequeno.
            player.heal(CHANNEL_HEAL_PER_TICK);
            // v0.1.40: se a sword NÃO está purificada (NBT "Purified"=true),
            // aplica WM no alvo. Purificada não causa infecção.
            boolean purified = stack.getTag() != null && stack.getTag().getBoolean("Purified");
            if (!purified && target instanceof Player victim) {
                victim.getCapability(br.com.murilo.liberthia.matter.MatterProfileProvider.CAP)
                        .ifPresent(vp -> {
                            vp.addWhite(0.5f);
                            br.com.murilo.liberthia.matter.MatterProfileEvents.syncTo(
                                    (net.minecraft.server.level.ServerPlayer) victim);
                        });
            }
            hitCount++;

            // Particle trail player -> target
            Vec3 diff = target.position().add(0, target.getBbHeight() * 0.5, 0)
                    .subtract(player.position().add(0, player.getEyeHeight(), 0));
            int particles = 5;
            for (int i = 0; i < particles; i++) {
                double t = (double) i / particles;
                double px = player.getX() + diff.x * t;
                double py = player.getY() + player.getEyeHeight() + diff.y * t;
                double pz = player.getZ() + diff.z * t;
                sl.sendParticles(ParticleTypes.END_ROD,
                        px, py, pz, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }

        if (hitCount > 0 && player.tickCount % 4 == 0) {
            sl.playSound(null, player.blockPosition(),
                    SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.PLAYERS, 0.4F, 1.6F);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        super.releaseUsing(stack, level, entity, timeLeft);
        if (level.isClientSide()) return;
        if (entity instanceof Player p) {
            CHANNELING_PLAYERS.remove(p.getUUID());
        }
    }

    private void performDash(ServerLevel sl, Player player, ItemStack stack) {
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * 2.0, 0.3, look.z * 2.0);
        player.hurtMarked = true;

        Vec3 origin = player.position();
        Vec3 endPos = origin.add(look.scale(DASH_RANGE));
        AABB scan = new AABB(origin, endPos).inflate(2.0);
        List<LivingEntity> hits = sl.getEntitiesOfClass(LivingEntity.class, scan,
                e -> e != player && e.isAlive());

        // r37 NERF: 1 hit por mob (era 3) + sem invulnerableTime=0 = sem
        // multi-stack damage = sem 1-shot kill.
        float baseDamage = BASE_ATTACK_DAMAGE * DASH_HIT_MULT;
        for (LivingEntity target : hits) {
            target.setSecondsOnFire(DASH_FIRE_SECONDS);
            target.hurt(player.damageSources().magic(), baseDamage);
        }

        setCharge(stack, 0);
        player.getCooldowns().addCooldown(this, DASH_COOLDOWN_TICKS);

        sl.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getY() + 1.0, player.getZ(),
                50, 0.5, 0.6, 0.5, 0.1);
        sl.sendParticles(ParticleTypes.GLOW,
                player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.5, 0.6, 0.5, 0.05);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 1.4F);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        // Right-click on entity now goes into channel via use() — pass through
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int charge = getCharge(stack);
        tooltip.add(Component.literal("Carga Lifesteal: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(charge + "/" + MAX_CHARGE)
                        .withStyle(charge >= MAX_CHARGE ? ChatFormatting.AQUA : ChatFormatting.WHITE)));
        tooltip.add(Component.literal("Segurar Right-click: dreno em cone (5 blocos)")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift+Right-click: dash flamejante (3 hits)")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Ao morrer: explosão de matéria branca")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        // v0.1.40: status da purificação. Sword purificada não infecta com WM.
        boolean purified = stack.getTag() != null && stack.getTag().getBoolean("Purified");
        if (purified) {
            tooltip.add(Component.literal("✦ PURIFICADA — não infecta o alvo")
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.literal("⚠ Infecta o alvo com WM — purifique no Matter Purifier (600k FE)")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static int getCharge(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        return tag.getInt(TAG_CHARGE);
    }

    public static void setCharge(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(TAG_CHARGE, Math.max(0, Math.min(MAX_CHARGE, value)));
    }

    /** Marca o player como interrompido — chamado quando ele toma dano durante channel. */
    public static void interrupt(UUID playerId) {
        if (CHANNELING_PLAYERS.contains(playerId)) {
            INTERRUPTED_THIS_TICK.add(playerId);
        }
    }
}
