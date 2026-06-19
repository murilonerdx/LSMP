package br.com.murilo.liberthia.loom;

import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * v0.1.22 r38: <b>Dark Matter Laser Gun — HOLD-TO-FIRE REWORK</b>
 *
 * <h2>r38 CHANGES</h2>
 * <ul>
 *   <li><b>Hold-to-fire:</b> right-click HOLD pra disparar continuamente. Cada
 *       tick que segura, drena energia + dispara laser. Para quando soltar OU
 *       quando energia acabar.</li>
 *   <li><b>Muito mais dano:</b> 40 base (era 20) + EXPLOSION ao acertar
 *       qualquer entidade (raio 3, sem destruir blocos).</li>
 *   <li><b>Fire stack:</b> 12s de fogo (era 8).</li>
 *   <li><b>Energy drain por tick:</b> 50 FE/tick (3000 FE/sec) — laser segurado
 *       1s = 1000 FE drainados. Full 50k FE = ~16s segurando.</li>
 *   <li><b>Heat acumula RÁPIDO:</b> +1 heat/tick — após 100 ticks (5s) overheat
 *       e queima o usuário (15 HP fire dmg).</li>
 *   <li><b>Damage cooldown por entidade:</b> mesma entidade só pode ser atingida
 *       1x por 8 ticks (0.4s) — previne multi-hit por frame.</li>
 *   <li><b>Bug fix energia:</b> drain agora é por TICK consumido (não por uso).
 *       Tooltip mostra estado real.</li>
 * </ul>
 *
 * <h2>Controls</h2>
 * <ul>
 *   <li><b>Hold RClick:</b> fire contínuo até soltar ou acabar energia</li>
 *   <li><b>Shift+RClick:</b> RECARREGAR (consome 1 Active Dark Matter, +10k FE)</li>
 * </ul>
 */
public class DarkMatterLaserItem extends Item {

    public static final int MAX_ENERGY = 50_000;
    /** r38: energy drained per tick of firing (3000 FE/sec). */
    public static final int ENERGY_PER_TICK = 50;
    /** r38: max use duration — 72000 ticks (1h) so it never auto-stops; we control via energy. */
    public static final int MAX_USE_DURATION = 72000;
    public static final int RANGE = 32;
    /** r38: damage per hit (was 20 single shot, now 8 per tick * many ticks). */
    public static final float DAMAGE_PER_HIT = 8.0F;
    /** r38: heat acumula 1 por tick que segurar; threshold = 100. */
    public static final int OVERHEAT_THRESHOLD = 100;
    public static final int MAX_HEAT = 150;
    /** r38: cooldown de hit por entidade pra evitar damage spam (8 ticks = 0.4s entre hits). */
    public static final int HIT_COOLDOWN_TICKS = 8;
    /** r38: explosion radius on hit. */
    public static final float EXPLOSION_RADIUS = 3.0F;

    public DarkMatterLaserItem(Properties p) { super(p.stacksTo(1).durability(0)); }

    @Override public boolean isFoil(ItemStack s) { return true; }
    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.BOW; }
    @Override public int getUseDuration(ItemStack s) { return MAX_USE_DURATION; }

    public int getEnergy(ItemStack s) { return s.getOrCreateTag().getInt("Energy"); }
    public void setEnergy(ItemStack s, int v) { s.getOrCreateTag().putInt("Energy", Math.max(0, Math.min(MAX_ENERGY, v))); }
    public int getHeat(ItemStack s) { return s.getOrCreateTag().getInt("Heat"); }
    public void setHeat(ItemStack s, int v) { s.getOrCreateTag().putInt("Heat", Math.max(0, Math.min(MAX_HEAT, v))); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) {
            // Client-side: pede start
            if (player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        // r38: SHIFT+RCLICK = RELOAD
        if (sp.isShiftKeyDown()) {
            return tryReload(sp, stack);
        }

        // r38: chegar energia ANTES de começar
        if (getEnergy(stack) < ENERGY_PER_TICK) {
            sp.displayClientMessage(Component.literal(
                    "§c⚠ Sem energia — §6shift+rclick§r§c pra recarregar."), true);
            return InteractionResultHolder.fail(stack);
        }
        if (getHeat(stack) >= OVERHEAT_THRESHOLD) {
            sp.displayClientMessage(Component.literal(
                    "§c§lOVERHEATED §r§7— aguarde o canhão esfriar."), true);
            return InteractionResultHolder.fail(stack);
        }

        // Inicia hold-to-fire
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * r38: chamado a cada tick enquanto player segura right-click.
     * Dispara laser e drena energia. Para se acabar.
     */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingTicks) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer sp)) return;
        ServerLevel sl = (ServerLevel) level;

        int energy = getEnergy(stack);
        if (energy < ENERGY_PER_TICK) {
            sp.releaseUsingItem();
            sp.displayClientMessage(Component.literal(
                    "§c⚠ Energia esgotada."), true);
            return;
        }

        int heat = getHeat(stack);
        if (heat >= OVERHEAT_THRESHOLD) {
            sp.releaseUsingItem();
            sp.hurt(sp.damageSources().onFire(), 15.0F);
            sp.setSecondsOnFire(8);
            sp.displayClientMessage(Component.literal(
                    "§c§l⚠ CANHÃO SUPERAQUECEU — 15 HP perdidos!"), true);
            return;
        }

        // Drena energia + acumula heat
        setEnergy(stack, energy - ENERGY_PER_TICK);
        setHeat(stack, heat + 1);

        // Dispara o laser
        fireLaser(sp, sl, stack);

        // Som contínuo a cada 6 ticks (3x/sec)
        if (level.getGameTime() % 6 == 0) {
            level.playSound(null, sp.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 1.5F, 1.6F + (level.random.nextFloat() * 0.4F));
        }
    }

    /** r38: chamado quando player solta o right-click. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (level.isClientSide) return;
        // Não faz nada — drain já parou
    }

    private void fireLaser(ServerPlayer sp, ServerLevel level, ItemStack stack) {
        Vec3 start = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        Vec3 end = start.add(look.scale(RANGE));

        // Raycast block
        BlockHitResult blockHit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        Vec3 stopPos = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : end;

        // r38: cooldown por entidade — NBT key "HitCD_<uuid>" = tick atual
        // Coleta UUIDs já atingidos recentemente
        long now = level.getGameTime();
        var tag = stack.getOrCreateTag();
        Set<String> hitThisShot = new HashSet<>();
        boolean anyHit = false;

        for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(start, stopPos).inflate(1.5))) {
            if (le == sp) continue;
            String uuidKey = "HitCD_" + le.getUUID().toString();
            long lastHit = tag.getLong(uuidKey);
            if (now - lastHit < HIT_COOLDOWN_TICKS) continue;

            Vec3 toEntity = le.position().subtract(start);
            double along = toEntity.dot(look);
            if (along < 0 || along > RANGE) continue;
            Vec3 closestOnRay = start.add(look.scale(along));
            double dist = le.position().distanceTo(closestOnRay);
            if (dist < 2.0) {
                // Aplica dano + fogo + cegueira
                le.hurt(sp.damageSources().playerAttack(sp), DAMAGE_PER_HIT);
                le.setSecondsOnFire(12);
                le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
                tag.putLong(uuidKey, now);
                hitThisShot.add(uuidKey);
                anyHit = true;

                // r38: EXPLOSION on hit (raio 3, sem destruir blocos)
                level.explode(sp, le.getX(), le.getY() + le.getBbHeight() / 2, le.getZ(),
                        EXPLOSION_RADIUS, Level.ExplosionInteraction.NONE);
                // Particles extra de impacto
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        le.getX(), le.getY() + le.getBbHeight() / 2, le.getZ(),
                        1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        le.getX(), le.getY() + le.getBbHeight() / 2, le.getZ(),
                        20, 0.5, 0.5, 0.5, 0.1);
            }
        }

        // Cleanup NBT velho de entidades atingidas há muito tempo (evita NBT bloat)
        if (now % 200 == 0) {
            var keys = new HashSet<>(tag.getAllKeys());
            for (String k : keys) {
                if (k.startsWith("HitCD_")) {
                    long lastHit = tag.getLong(k);
                    if (now - lastHit > 100) tag.remove(k);
                }
            }
        }

        // Particles along laser (mantém visual)
        for (double d = 0; d <= start.distanceTo(stopPos); d += 0.5) {
            Vec3 p = start.add(look.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            if (level.getGameTime() % 4 == 0) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }

        // r38: impact particles MAIORES (blocks + ground impact)
        level.sendParticles(ParticleTypes.EXPLOSION, stopPos.x, stopPos.y, stopPos.z,
                5, 0.3, 0.3, 0.3, 0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, stopPos.x, stopPos.y, stopPos.z,
                8, 0.2, 0.2, 0.2, 0.05);

        // Block break tracking — NBT key: "TargetBlock" + tick count (mantém da r37)
        if (!anyHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = blockHit.getBlockPos();
            BlockPos prev = tag.contains("TargetX") ? new BlockPos(
                    tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ")) : null;
            if (prev != null && prev.equals(hitPos)) {
                int ticks = tag.getInt("TargetTicks") + 1;
                tag.putInt("TargetTicks", ticks);
                if (ticks >= 40) { // r38: 40 ticks = 2s de fire contínuo quebra
                    level.destroyBlock(hitPos, true);
                    tag.remove("TargetX");
                    tag.remove("TargetY");
                    tag.remove("TargetZ");
                    tag.remove("TargetTicks");
                }
            } else {
                tag.putInt("TargetX", hitPos.getX());
                tag.putInt("TargetY", hitPos.getY());
                tag.putInt("TargetZ", hitPos.getZ());
                tag.putInt("TargetTicks", 1);
            }
        }
    }

    private static ItemStack findActivatedDarkMatter(ServerPlayer sp) {
        var item = ModItems.ACTIVE_DARK_MATTER.get();
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack s = sp.getInventory().getItem(i);
            if (s.is(item)) return s;
        }
        return ItemStack.EMPTY;
    }

    /**
     * r34: RELOAD via shift+right-click. Consome 1 Activated Dark Matter,
     * recarrega 10.000 FE no laser. Mensagem clara + som distinto.
     */
    private InteractionResultHolder<ItemStack> tryReload(ServerPlayer sp, ItemStack stack) {
        int current = getEnergy(stack);
        if (current >= MAX_ENERGY) {
            sp.displayClientMessage(Component.literal(
                    "§e⚡ Laser já com energia máxima (" + MAX_ENERGY + " FE)."), true);
            return InteractionResultHolder.fail(stack);
        }
        ItemStack ammo = findActivatedDarkMatter(sp);
        if (ammo.isEmpty()) {
            sp.displayClientMessage(Component.literal(
                    "§c⚠ Sem Activated Dark Matter no inventário pra recarregar."), true);
            return InteractionResultHolder.fail(stack);
        }
        ammo.shrink(1);
        int recharge = 10_000;
        setEnergy(stack, Math.min(MAX_ENERGY, current + recharge));
        // Reset heat também (lubrificação)
        setHeat(stack, 0);
        sp.level().playSound(null, sp.blockPosition(),
                net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 0.8F);
        sp.displayClientMessage(Component.literal(
                "§e§l⚡ RELOAD §r§7+§a" + recharge + " FE§r§7 | total: §b"
                        + getEnergy(stack) + " §7/ " + MAX_ENERGY), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean sel) {
        if (level.isClientSide) return;
        // Heat dissipates 1/sec quando NÃO está sendo usado (segurar=acumula via onUseTick)
        if (entity instanceof ServerPlayer sp && !sp.isUsingItem()
                && level.getGameTime() % 20 == 0) {
            setHeat(stack, getHeat(stack) - 1);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack s) { return getEnergy(s) < MAX_ENERGY; }
    @Override
    public int getBarWidth(ItemStack s) { return Math.round(13.0F * getEnergy(s) / MAX_ENERGY); }
    @Override
    public int getBarColor(ItemStack s) {
        float r = getEnergy(s) / (float) MAX_ENERGY;
        if (r > 0.5) return 0x55AAFF;
        if (r > 0.2) return 0xFFAA00;
        return 0xFF5555;
    }

    @Override
    public void appendHoverText(ItemStack s, @Nullable Level l, List<Component> t, TooltipFlag f) {
        int e = getEnergy(s);
        int h = getHeat(s);
        t.add(Component.literal("§5§oCanhão Laser de Matéria Escura").withStyle(ChatFormatting.ITALIC));
        t.add(Component.literal("§7Energia: §b" + e + " / " + MAX_ENERGY + " FE"));
        t.add(Component.literal("§7Calor: " + (h > OVERHEAT_THRESHOLD ? "§c" : "§e") + h + "/" + MAX_HEAT + " °"));
        t.add(Component.empty());
        t.add(Component.literal("§7• Dano por tick: §c" + DAMAGE_PER_HIT));
        t.add(Component.literal("§7• Drenagem: §b" + ENERGY_PER_TICK + " FE/tick §8(3000/s)§r"));
        t.add(Component.literal("§7• Cada hit: §6Fogo 12s§r§7 + §dCegueira§r§7 + §c§lEXPLOSÃO raio " + EXPLOSION_RADIUS));
        t.add(Component.literal("§7• Quebra blocos com fire contínuo (2s)"));
        t.add(Component.literal("§c• Overheat (5s segurando) = §l15 HP de fogo!"));
        t.add(Component.empty());
        t.add(Component.literal("§e§lCONTROLES:"));
        t.add(Component.literal("§7• §eHOLD RClick§7: §c§lFIRE contínuo§r§7 até soltar ou acabar"));
        t.add(Component.literal("§7• §6Shift+RClick§7: §aRECARREGAR §7(+10.000 FE, consome 1 Active DM)"));
    }
}
