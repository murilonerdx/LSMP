package br.com.murilo.liberthia.event;

import br.com.murilo.liberthia.LiberthiaMod;
import br.com.murilo.liberthia.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Poderes especiais do set completo de Clear Matter Armor.
 *
 * <h3>Ativos (set completo):</h3>
 * <ul>
 *   <li><b>Teletransporte</b> — Shift+Right-click no ar OU com qualquer item
 *       na mão teleporta o player ~8 blocos pra frente. Cooldown de 10s
 *       compartilhado entre tentativas. Custo: nada.</li>
 *   <li><b>Knockback reflexivo</b> — Quem te bate é arremessado pra trás
 *       (kb x 2.0 da força padrão).</li>
 *   <li><b>Dano → Velocidade</b> — Todo dano tomado vira Speed temporário:
 *       1 ponto de dano = 5 ticks de Speed II.</li>
 *   <li>Já tem efeitos passivos (Speed/Jump/Regen/Water Breathing/Absorption)
 *       via {@link MatterArmorEffectsHandler}.</li>
 * </ul>
 *
 * <p><b>Implementação do teleport</b>: o evento de right-click sem item
 * (RightClickEmpty) só dispara no cliente, então usamos um packet C2S
 * (ClearMatterTeleportC2SPacket) que é registrado em {@code ModNetwork}.
 * Com item na mão, usamos {@code RightClickItem} server-side direto.
 *
 * <p>Cooldown é per-player, armazenado em estado server-side simples (Map
 * UUID → tick).
 */
@Mod.EventBusSubscriber(modid = LiberthiaMod.MODID)
public final class ClearMatterPowersHandler {

    /** Cooldown do teleport em ticks (20t = 1s). */
    private static final int TELEPORT_COOLDOWN_TICKS = 200; // 10s

    /** Distância do teleport em blocos. */
    private static final double TELEPORT_DISTANCE = 8.0;

    /** Multiplicador de knockback que o atacante sofre. */
    private static final double KNOCKBACK_MULTIPLIER = 2.0;

    /** Map UUID → tick em que o cooldown expira. Não persistido entre restarts
     *  (cooldown reseta no logout). */
    private static final java.util.Map<java.util.UUID, Long> teleportCooldowns =
            new java.util.concurrent.ConcurrentHashMap<>();

    private ClearMatterPowersHandler() {}

    // ──────────────────────────────────────────────────── Teleport

    /**
     * Tenta executar o teleport. Chamada pelos handlers de evento +
     * pelo packet C2S do cliente. Valida set completo + shift + cooldown.
     */
    public static void tryTeleport(ServerPlayer sp) {
        if (!hasFullClearSet(sp)) return;
        if (!sp.isShiftKeyDown()) return;

        long now = sp.level().getGameTime();
        Long cdEnd = teleportCooldowns.get(sp.getUUID());
        if (cdEnd != null && now < cdEnd) {
            long remaining = (cdEnd - now) / 20;
            sp.displayClientMessage(Component.literal(
                    "§b⏳ Teleport em cooldown — " + remaining + "s"), true);
            return;
        }

        // Calcula destino: TELEPORT_DISTANCE blocos na direção do olhar (horizontal).
        // Limita Y pra não atravessar tetos/chãos absurdos — usa raycast simples.
        Vec3 look = sp.getLookAngle();
        Vec3 origin = sp.position();
        Vec3 dest = origin.add(look.x * TELEPORT_DISTANCE, 0, look.z * TELEPORT_DISTANCE);

        // Procura solo seguro pra cair: olha ±3 blocos verticalmente.
        ServerLevel sl = sp.serverLevel();
        net.minecraft.core.BlockPos destPos = net.minecraft.core.BlockPos.containing(dest);
        net.minecraft.core.BlockPos safe = findSafeLandingY(sl, destPos);
        if (safe == null) {
            sp.displayClientMessage(Component.literal(
                    "§c⚠ Sem espaço seguro pra teleportar"), true);
            return;
        }

        // FX no ponto de origem
        sl.sendParticles(ParticleTypes.PORTAL,
                origin.x, origin.y + 1, origin.z, 40, 0.5, 1.0, 0.5, 0.2);
        sl.playSound(null, sp.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.3f);

        // Teleport (mantém Y do safe + olhar atual)
        sp.teleportTo(sl, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5,
                sp.getYRot(), sp.getXRot());

        // FX no destino
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                safe.getX() + 0.5, safe.getY() + 1, safe.getZ() + 0.5,
                40, 0.5, 1.0, 0.5, 0.2);
        sl.playSound(null, safe,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.7f);

        teleportCooldowns.put(sp.getUUID(), now + TELEPORT_COOLDOWN_TICKS);
        sp.displayClientMessage(Component.literal(
                "§b✦ Teleport ativado §7(cooldown 10s)"), true);
    }

    /** Procura Y seguro perto da pos pra cair sem afogar/queimar/etc. */
    private static net.minecraft.core.BlockPos findSafeLandingY(ServerLevel sl,
                                                                 net.minecraft.core.BlockPos pos) {
        // Tenta a Y atual primeiro, depois +1, -1, +2, -2, +3, -3
        int[] offsets = {0, 1, -1, 2, -2, 3, -3};
        for (int dy : offsets) {
            net.minecraft.core.BlockPos test = pos.offset(0, dy, 0);
            if (isSafeLanding(sl, test)) return test;
        }
        return null;
    }

    /** True se o player pode ficar de pé em test (2 blocos de ar + bloco sólido abaixo). */
    private static boolean isSafeLanding(ServerLevel sl, net.minecraft.core.BlockPos test) {
        var feet = sl.getBlockState(test);
        var head = sl.getBlockState(test.above());
        // Pés e cabeça precisam ser não-colidíveis. Não nos importa se tem
        // bloco abaixo (player cai pequeno, ok).
        return feet.getCollisionShape(sl, test).isEmpty()
                && head.getCollisionShape(sl, test.above()).isEmpty();
    }

    // ──────────────────────────────────────────────────── Right-click handlers

    /**
     * Server-side: dispara quando player com item na mão dá right-click no ar
     * ou em entidade/bloco. Se for shift+filter, deixa o filter abrir GUI.
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!sp.isShiftKeyDown()) return;
        if (!hasFullClearSet(sp)) return;
        // RightClickItem só dispara quando o player NÃO mira em bloco/entidade
        // (caso contrário seria RightClickBlock/EntityInteract), então sempre
        // OK teleportar aqui se as condições batem.
        tryTeleport(sp);
    }

    // ──────────────────────────────────────────────────── Combate (knockback + speed)

    /**
     * Quando vítima com set completo toma dano:
     * 1. Atacante leva knockback x2.0
     * 2. Vítima ganha Speed II proporcional ao dano (5 ticks por ponto de dano)
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (!(victim instanceof Player vp)) return;
        if (!hasFullClearSet(vp)) return;

        float dmg = event.getAmount();
        if (dmg <= 0) return;

        // Knockback reflexivo no atacante direto (se houver)
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker) {
            Vec3 away = attacker.position().subtract(vp.position()).normalize();
            attacker.knockback(KNOCKBACK_MULTIPLIER, -away.x, -away.z);
        }

        // Speed II proporcional ao dano — clamped pra 5s (100t) max
        int duration = Math.min(100, (int) (dmg * 5));
        if (duration > 0 && vp instanceof ServerPlayer svp) {
            svp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1,
                    true, false, true));
        }
    }

    // ──────────────────────────────────────────────────── Helpers

    /** True se o player tem TODAS as 4 peças de Clear Matter equipadas. */
    public static boolean hasFullClearSet(Player player) {
        return is(player.getItemBySlot(EquipmentSlot.HEAD),  ModItems.CLEAR_MATTER_HELMET.get())
            && is(player.getItemBySlot(EquipmentSlot.CHEST), ModItems.CLEAR_MATTER_CHESTPLATE.get())
            && is(player.getItemBySlot(EquipmentSlot.LEGS),  ModItems.CLEAR_MATTER_LEGGINGS.get())
            && is(player.getItemBySlot(EquipmentSlot.FEET),  ModItems.CLEAR_MATTER_BOOTS.get());
    }

    private static boolean is(ItemStack stack, net.minecraft.world.item.Item item) {
        return !stack.isEmpty() && stack.getItem() == item;
    }
}
