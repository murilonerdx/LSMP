package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Poderes especiais do set completo de Dark Matter Armor.
 *
 * <h3>Mecânicas (set completo):</h3>
 * <ul>
 *   <li><b>Armor charge</b> — Cada vez que o player toma dano, 80% vai pra
 *       "carga" da armadura (salvo em NBT do chestplate, key {@code DarkCharge}),
 *       só 20% atinge o player de fato. Carga max = 500.</li>
 *   <li><b>Próximo ataque</b> — Quando o player ATACA, a carga é consumida +
 *       adicionada ao dano (1:1, até 500 bonus). Carga zera após uso.</li>
 *   <li><b>Modo combate (shift)</b> — Segurando shift com o set, ganha
 *       Strength II + Speed I a cada tick.</li>
 *   <li><b>Liberação explosiva</b> — Quando a carga está em 500 (max) E o
 *       player segura shift, explosão circular de 500 de dano em raio 5
 *       blocos. Consome toda a carga.</li>
 * </ul>
 *
 * <p>Tem um cooldown de 60 ticks entre uses de "liberação" pra evitar
 * detonações múltiplas no mesmo segundo.
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class DarkMatterPowersHandler {

    /** Key NBT pra carga armazenada no chestplate. */
    private static final String NBT_CHARGE = "DarkCharge";

    /** Carga máxima. */
    public static final int MAX_CHARGE = 500;

    /** Raio da explosão de liberação em blocos. */
    private static final double EXPLOSION_RADIUS = 5.0;

    /** Fração do dano absorvido pela armadura (0.0 = nada, 1.0 = tudo). */
    private static final float ABSORB_FRACTION = 0.80f;

    /** Cooldown entre detonations de liberação (server ticks). */
    private static final int RELEASE_COOLDOWN = 60;
    private static final java.util.Map<java.util.UUID, Long> releaseCooldowns =
            new java.util.concurrent.ConcurrentHashMap<>();

    private DarkMatterPowersHandler() {}

    // ──────────────────────────────────────────────────── Dano recebido

    /**
     * v0.1.22 r20 (Bug #28): MUDADO de LivingHurtEvent pra LivingDamageEvent.
     *
     * <p>LivingHurtEvent roda ANTES de armor reduction e Protection enchant
     * absorber. Quando o handler setamount(20%) pré-armor, o Protection
     * enchant aplicava sobre 20%, dava redução insignificante — usuário
     * (Bug #28) percebeu como "Protection não funciona".
     *
     * <p>Agora rola em LivingDamageEvent, que dispara DEPOIS de armor +
     * Protection. Comportamento: Protection reduz dano normalmente, e
     * SOBRE o resultado nossa armor absorve 80%. Player VÊ Protection
     * funcionando + ganha bônus da Dark Matter armor.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (!(victim instanceof ServerPlayer sp)) return;
        if (!hasFullDarkSet(sp)) return;

        float dmg = event.getAmount();
        if (dmg <= 0) return;

        // Dano absorvido pela armadura → vira carga
        int absorbed = Math.round(dmg * ABSORB_FRACTION);
        int currentCharge = getCharge(sp);
        int newCharge = Math.min(MAX_CHARGE, currentCharge + absorbed);
        int actuallyAbsorbed = newCharge - currentCharge;
        setCharge(sp, newCharge);

        // Reduz o dano que de fato atinge o player.
        // amount efetivo = dmg - absorbed (mas só conta o que efetivamente
        // foi absorvido — se a carga já estava em MAX, o dano passa todo)
        float passedDamage = dmg - actuallyAbsorbed;
        event.setAmount(Math.max(0.1f, passedDamage));

        // Particles + feedback visual (a cada 5+ pontos absorvidos)
        if (actuallyAbsorbed >= 1) {
            ServerLevel sl = sp.serverLevel();
            sl.sendParticles(ParticleTypes.SCULK_SOUL,
                    sp.getX(), sp.getY() + 1.2, sp.getZ(),
                    Math.min(10, actuallyAbsorbed),
                    0.4, 0.6, 0.4, 0.05);
        }

        // Notifica via actionbar quando carga atinge MAX (pra avisar o player
        // que pode detonar)
        if (newCharge == MAX_CHARGE && currentCharge < MAX_CHARGE) {
            sp.displayClientMessage(Component.literal(
                    "§4☢ Armadura CARREGADA — segure SHIFT pra liberar"), true);
        }
    }

    // ──────────────────────────────────────────────────── Ataque desferido

    /**
     * Quando o player com set completo ATACA outra entidade, a carga atual
     * é adicionada ao dano (1 charge = +1 damage). Carga zera após o ataque.
     *
     * <p>Usa AttackEntityEvent pra modificar o dano via {@code attacker.attack()}
     * mas como esse evento é fired ANTES do ataque, não consegue modificar
     * o dano diretamente. Solução: aplicar dano adicional manualmente DEPOIS
     * via LivingDamageEvent no target.
     */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player attacker = event.getEntity();
        if (attacker.level().isClientSide) return;
        if (!(attacker instanceof ServerPlayer sp)) return;
        if (!hasFullDarkSet(sp)) return;

        int charge = getCharge(sp);
        if (charge <= 0) return;

        // Aplica dano extra direto no alvo
        if (event.getTarget() instanceof LivingEntity target) {
            // damageSource = player attack mas com tipo magic pra atravessar
            // armor (charge representa dano de matter, não físico)
            DamageSource src = sp.damageSources().playerAttack(sp);
            target.hurt(src, charge);

            // Reset charge
            setCharge(sp, 0);

            ServerLevel sl = sp.serverLevel();
            sl.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                    target.getX(), target.getY() + 1, target.getZ(),
                    20, 0.5, 0.5, 0.5, 0.2);
            sl.playSound(null, target.blockPosition(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 1.2f);

            sp.displayClientMessage(Component.literal(
                    "§4◆ Liberou " + charge + " de carga!").withStyle(ChatFormatting.DARK_RED),
                    true);
        }
    }

    // ──────────────────────────────────────────────────── Modo combate (shift) + Liberação

    /**
     * Tick periódico. Checa:
     * - Se player tem set + shift: aplica Strength II + Speed I (refresh a cada 1s)
     * - Se carga MAX + shift segurado: detona explosão (com cooldown)
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!hasFullDarkSet(sp)) return;
        if (!sp.isShiftKeyDown()) return;

        // Buff passivo enquanto crouching com o set: Strength + Speed
        if (sp.tickCount % 20 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1,
                    true, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0,
                    true, false, true));
        }

        // Carga MAX + shift segurado por TODO o tick → libera explosão
        int charge = getCharge(sp);
        if (charge < MAX_CHARGE) return;

        long now = sp.level().getGameTime();
        Long cdEnd = releaseCooldowns.get(sp.getUUID());
        if (cdEnd != null && now < cdEnd) return;

        // Detona
        detonate(sp);
        setCharge(sp, 0);
        releaseCooldowns.put(sp.getUUID(), now + RELEASE_COOLDOWN);
    }

    /**
     * Liberação explosiva: 500 dano em todos os mobs/players em raio
     * EXPLOSION_RADIUS blocos. O próprio user NÃO é afetado.
     */
    private static void detonate(ServerPlayer sp) {
        ServerLevel sl = sp.serverLevel();

        // Particles + som dramático
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                sp.getX(), sp.getY() + 1, sp.getZ(), 200,
                EXPLOSION_RADIUS * 0.4, 1.0, EXPLOSION_RADIUS * 0.4, 0.3);
        sl.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                sp.getX(), sp.getY() + 1, sp.getZ(), 80,
                EXPLOSION_RADIUS * 0.5, 1.0, EXPLOSION_RADIUS * 0.5, 0.5);
        sl.playSound(null, sp.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 0.7f);
        sl.playSound(null, sp.blockPosition(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.5f);

        // Dano em todos os LivingEntity ao redor (exceto o player)
        var bb = sp.getBoundingBox().inflate(EXPLOSION_RADIUS);
        DamageSource src = sp.damageSources().playerAttack(sp);
        for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, bb,
                e -> e != sp && e.isAlive())) {
            double dist = target.distanceTo(sp);
            if (dist > EXPLOSION_RADIUS) continue;
            // Falloff: dano cai linear com distância
            float damage = (float) (MAX_CHARGE * (1.0 - dist / EXPLOSION_RADIUS));
            target.hurt(src, damage);
            // Knockback radial
            net.minecraft.world.phys.Vec3 away = target.position()
                    .subtract(sp.position()).normalize();
            target.setDeltaMovement(away.x * 1.5, 0.5, away.z * 1.5);
            target.hurtMarked = true;
        }

        sp.displayClientMessage(Component.literal(
                "§4☠ LIBERAÇÃO! 500 dano em volta").withStyle(ChatFormatting.DARK_RED),
                false);
    }

    // ──────────────────────────────────────────────────── Charge storage (NBT)

    /** Lê a carga atual do chestplate equipado. 0 se sem chestplate ou sem tag. */
    public static int getCharge(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || chest.getItem() != ModItems.DARK_MATTER_CHESTPLATE.get())
            return 0;
        CompoundTag tag = chest.getTag();
        return tag == null ? 0 : tag.getInt(NBT_CHARGE);
    }

    /** Salva a carga no NBT do chestplate. Clamp [0, MAX_CHARGE]. */
    public static void setCharge(Player player, int value) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || chest.getItem() != ModItems.DARK_MATTER_CHESTPLATE.get())
            return;
        int clamped = Math.max(0, Math.min(MAX_CHARGE, value));
        CompoundTag tag = chest.getOrCreateTag();
        tag.putInt(NBT_CHARGE, clamped);
    }

    // ──────────────────────────────────────────────────── Helpers

    public static boolean hasFullDarkSet(Player player) {
        return is(player.getItemBySlot(EquipmentSlot.HEAD),  ModItems.DARK_MATTER_HELMET.get())
            && is(player.getItemBySlot(EquipmentSlot.CHEST), ModItems.DARK_MATTER_CHESTPLATE.get())
            && is(player.getItemBySlot(EquipmentSlot.LEGS),  ModItems.DARK_MATTER_LEGGINGS.get())
            && is(player.getItemBySlot(EquipmentSlot.FEET),  ModItems.DARK_MATTER_BOOTS.get());
    }

    private static boolean is(ItemStack stack, net.minecraft.world.item.Item item) {
        return !stack.isEmpty() && stack.getItem() == item;
    }
}
